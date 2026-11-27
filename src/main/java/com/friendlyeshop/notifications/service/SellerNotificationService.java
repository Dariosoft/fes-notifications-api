package com.friendlyeshop.notifications.service;

import com.friendlyeshop.notifications.client.OutOfBandNotification;
import com.friendlyeshop.notifications.client.OutOfBandSender;
import com.friendlyeshop.notifications.repository.DeliveryRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Service
public class SellerNotificationService {

    private static final String CHANNEL = "OUT_OF_BAND";

    private static final String STATUS = "SENT";

    private final DeliveryRepository deliveryRepository;

    private final OutOfBandSender outOfBandSender;

    private final JsonMapper jsonMapper;

    public SellerNotificationService(
            DeliveryRepository deliveryRepository,
            OutOfBandSender outOfBandSender,
            JsonMapper jsonMapper) {
        this.deliveryRepository = deliveryRepository;
        this.outOfBandSender = outOfBandSender;
        this.jsonMapper = jsonMapper;
    }

    @Transactional
    public void process(String messageIdHeader, String body) {
        JsonNode payload = parse(body);
        String messageId = resolveMessageId(messageIdHeader, payload);
        if (deliveryRepository.existsByMessageId(messageId)) {
            return;
        }
        UUID orderId = optionalUuid(payload, "orderId");
        UUID tenantId = optionalUuid(payload, "tenantId");
        int inserted = deliveryRepository.insertIfAbsent(messageId, orderId, tenantId, CHANNEL, STATUS);
        if (inserted == 0) {
            return;
        }
        outOfBandSender.send(new OutOfBandNotification(messageId, orderId, tenantId, body));
    }

    private JsonNode parse(String body) {
        JsonNode payload;
        try {
            payload = jsonMapper.readTree(body);
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("Malformed seller notification payload", exception);
        }
        if (payload == null || !payload.isObject()) {
            throw new IllegalArgumentException("Seller notification payload is not a JSON object");
        }
        return payload;
    }

    private String resolveMessageId(String messageIdHeader, JsonNode payload) {
        if (messageIdHeader != null && !messageIdHeader.isBlank()) {
            return messageIdHeader;
        }
        JsonNode eventId = payload.get("eventId");
        if (eventId != null && eventId.isString() && !eventId.asString().isBlank()) {
            return eventId.asString();
        }
        throw new IllegalArgumentException("Seller notification has no stable message identifier");
    }

    private UUID optionalUuid(JsonNode payload, String field) {
        JsonNode value = payload.get(field);
        if (value == null || value.isNull() || !value.isString() || value.asString().isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(value.asString());
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}
