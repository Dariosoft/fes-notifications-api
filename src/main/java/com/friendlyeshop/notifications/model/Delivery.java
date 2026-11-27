package com.friendlyeshop.notifications.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;

@Entity
@Table(name = "deliveries")
@Getter
public class Delivery {

    @Id
    @Column(name = "message_id", nullable = false, length = 100)
    private String messageId;

    @Column(name = "order_id")
    private UUID orderId;

    @Column(name = "tenant_id")
    private UUID tenantId;

    @Column(name = "channel", nullable = false, length = 20)
    private String channel;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Delivery() {
    }

    public static Delivery of(
            String messageId, UUID orderId, UUID tenantId, String channel, String status) {
        Delivery delivery = new Delivery();
        delivery.messageId = messageId;
        delivery.orderId = orderId;
        delivery.tenantId = tenantId;
        delivery.channel = channel;
        delivery.status = status;
        delivery.createdAt = Instant.now();
        return delivery;
    }
}
