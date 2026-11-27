package com.friendlyeshop.notifications.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class DeliveryTest {

    @Test
    void createsDeliveryWithStableFields() {
        UUID orderId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();

        Delivery delivery = Delivery.of("message-1", orderId, tenantId, "OUT_OF_BAND", "SENT");

        assertEquals("message-1", delivery.getMessageId());
        assertEquals(orderId, delivery.getOrderId());
        assertEquals(tenantId, delivery.getTenantId());
        assertEquals("OUT_OF_BAND", delivery.getChannel());
        assertEquals("SENT", delivery.getStatus());
        assertNotNull(delivery.getCreatedAt());
    }
}
