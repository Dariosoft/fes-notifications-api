package com.friendlyeshop.notifications.client;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class LoggingOutOfBandSenderTest {

    @Test
    void sendsNotificationThroughTheDefaultAdapter() {
        LoggingOutOfBandSender sender = new LoggingOutOfBandSender();

        assertDoesNotThrow(() -> sender.send(new OutOfBandNotification(
                "message-1", UUID.randomUUID(), UUID.randomUUID(), "{}")));
    }
}
