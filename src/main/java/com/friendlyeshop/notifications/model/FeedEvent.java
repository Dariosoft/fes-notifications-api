package com.friendlyeshop.notifications.model;

import com.friendlyeshop.notifications.messaging.kafka.model.FeedEventEnvelope;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "feed_events")
@Getter
public class FeedEvent {

    @Id
    @Column(name = "event_id")
    private UUID eventId;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    private String payload;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected FeedEvent() {
    }

    public static FeedEvent from(FeedEventEnvelope envelope) {
        FeedEvent event = new FeedEvent();
        event.eventId = envelope.eventId();
        event.orderId = envelope.orderId();
        event.tenantId = envelope.tenantId();
        event.eventType = envelope.eventType();
        event.occurredAt = envelope.occurredAt();
        event.payload = envelope.payload();
        event.createdAt = Instant.now();
        return event;
    }
}
