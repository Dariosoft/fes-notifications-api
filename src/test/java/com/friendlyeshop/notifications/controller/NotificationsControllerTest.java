package com.friendlyeshop.notifications.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class NotificationsControllerTest {

    @Test
    void reportsThatTheServiceIsRunning() {
        NotificationsController controller = new NotificationsController();

        assertEquals("Notifications API is up and running.", controller.notifications());
    }
}
