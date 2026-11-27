package com.friendlyeshop.notifications.messaging.kafka.model;

import java.time.Instant;
import java.util.UUID;

public record FeedEventEnvelope(
        UUID eventId,
        UUID orderId,
        UUID tenantId,
        String eventType,
        Instant occurredAt,
        String payload) {
}
