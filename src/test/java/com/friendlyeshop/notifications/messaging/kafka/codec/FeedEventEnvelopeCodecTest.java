package com.friendlyeshop.notifications.messaging.kafka.codec;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.friendlyeshop.notifications.exception.InvalidFeedEventException;
import com.friendlyeshop.notifications.messaging.kafka.model.FeedEventEnvelope;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class FeedEventEnvelopeCodecTest {

    private static final UUID EVENT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private static final UUID ORDER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    private static final UUID TENANT_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    private static final Instant OCCURRED_AT = Instant.parse("2026-01-15T10:15:30Z");

    private final FeedEventEnvelopeCodec codec =
            new FeedEventEnvelopeCodec(JsonMapper.builder().build());

    @Test
    void decodesAValidEnvelope() {
        String payload = validPayload();

        FeedEventEnvelope envelope = codec.decode(payload);

        assertThat(envelope.eventId()).isEqualTo(EVENT_ID);
        assertThat(envelope.orderId()).isEqualTo(ORDER_ID);
        assertThat(envelope.tenantId()).isEqualTo(TENANT_ID);
        assertThat(envelope.eventType()).isEqualTo("order.placed");
        assertThat(envelope.occurredAt()).isEqualTo(OCCURRED_AT);
        assertThat(envelope.payload()).isEqualTo(payload);
    }

    @Test
    void rejectsAnEnvelopeWithoutTenant() {
        String payload = "{\"eventId\":\"" + EVENT_ID + "\",\"orderId\":\"" + ORDER_ID + "\","
                + "\"eventType\":\"order.placed\",\"occurredAt\":\"" + OCCURRED_AT + "\"}";

        assertThatThrownBy(() -> codec.decode(payload))
                .isInstanceOf(InvalidFeedEventException.class)
                .hasMessageContaining("tenantId");
    }

    @Test
    void rejectsMalformedJson() {
        assertThatThrownBy(() -> codec.decode("{not-json"))
                .isInstanceOf(InvalidFeedEventException.class);
    }

    @Test
    void rejectsInvalidIdentifiers() {
        String payload = validPayload().replace(EVENT_ID.toString(), "not-a-uuid");

        assertThatThrownBy(() -> codec.decode(payload))
                .isInstanceOf(InvalidFeedEventException.class)
                .hasMessageContaining("eventId");
    }

    private static String validPayload() {
        return "{\"eventId\":\"" + EVENT_ID + "\",\"orderId\":\"" + ORDER_ID + "\","
                + "\"tenantId\":\"" + TENANT_ID + "\",\"eventType\":\"order.placed\","
                + "\"occurredAt\":\"" + OCCURRED_AT + "\"}";
    }
}
