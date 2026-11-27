package com.friendlyeshop.notifications.client;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class LoggingOutOfBandSenderTest {

    @Test
    void sendsNotificationThroughTheDefaultAdapter() {
        LoggingOutOfBandSender sender = new LoggingOutOfBandSender();

        sender.send(new OutOfBandNotification(
                "message-1", UUID.randomUUID(), UUID.randomUUID(), "{}"));
    }
}
