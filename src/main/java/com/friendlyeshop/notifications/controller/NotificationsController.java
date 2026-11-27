package com.friendlyeshop.notifications.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class NotificationsController {

    /** Placeholder so /notifications keeps responding until the feed and delivery land. */
    @GetMapping
    public String notifications() {
        return "Notifications API is up and running.";
    }
}
