package com.friendlyeshop.notifications.controller;

import com.friendlyeshop.notifications.config.FeedBroker;
import com.friendlyeshop.notifications.model.FeedEvent;
import com.friendlyeshop.notifications.service.FeedService;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
public class FeedController {

    private final FeedService feedService;

    private final FeedBroker feedBroker;

    private final long timeoutMs;

    public FeedController(
            FeedService feedService,
            FeedBroker feedBroker,
            @Value("${app.feed.timeout-ms:1800000}") long timeoutMs) {
        this.feedService = feedService;
        this.feedBroker = feedBroker;
        this.timeoutMs = timeoutMs;
    }

    @GetMapping(path = "/feed", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter feed(@RequestParam("tenantId") UUID tenantId) {
        SseEmitter emitter = feedBroker.register(tenantId, timeoutMs);
        for (FeedEvent event : feedService.recentEvents(tenantId)) {
            feedBroker.send(tenantId, emitter, event);
        }
        return emitter;
    }
}
