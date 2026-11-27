package com.friendlyeshop.notifications.messaging.kafka.codec;

import com.friendlyeshop.notifications.exception.InvalidFeedEventException;
import com.friendlyeshop.notifications.messaging.kafka.model.FeedEventEnvelope;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.UUID;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Component
public class FeedEventEnvelopeCodec {

    private final JsonMapper jsonMapper;

    public FeedEventEnvelopeCodec(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    public FeedEventEnvelope decode(String payload) {
        JsonNode root = parse(payload);
        return new FeedEventEnvelope(
                requiredUuid(root, "eventId"),
                requiredUuid(root, "orderId"),
                requiredUuid(root, "tenantId"),
                requiredText(root, "eventType"),
                requiredInstant(root, "occurredAt"),
                payload);
    }

    private JsonNode parse(String payload) {
        if (payload == null || payload.isBlank()) {
            throw new InvalidFeedEventException("Feed event payload is empty");
        }
        try {
            JsonNode root = jsonMapper.readTree(payload);
            if (root == null || !root.isObject()) {
                throw new InvalidFeedEventException("Feed event payload is not a JSON object");
            }
            return root;
        } catch (InvalidFeedEventException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new InvalidFeedEventException("Malformed feed event payload", exception);
        }
    }

    private String requiredText(JsonNode root, String field) {
        JsonNode value = root.get(field);
        if (value == null || value.isNull() || !value.isString() || value.asString().isBlank()) {
            throw new InvalidFeedEventException("Feed event is missing " + field);
        }
        return value.asString();
    }

    private UUID requiredUuid(JsonNode root, String field) {
        String value = requiredText(root, field);
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException exception) {
            throw new InvalidFeedEventException("Feed event has an invalid " + field, exception);
        }
    }

    private Instant requiredInstant(JsonNode root, String field) {
        String value = requiredText(root, field);
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException exception) {
            throw new InvalidFeedEventException("Feed event has an invalid " + field, exception);
        }
    }
}
