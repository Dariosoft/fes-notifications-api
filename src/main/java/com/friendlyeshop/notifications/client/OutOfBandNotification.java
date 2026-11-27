package com.friendlyeshop.notifications.client;

import java.util.UUID;

public record OutOfBandNotification(
        String messageId,
        UUID orderId,
        UUID tenantId,
        String payload) {
}
