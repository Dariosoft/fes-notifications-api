package com.friendlyeshop.notifications.service;

import com.friendlyeshop.notifications.model.FeedEvent;
import com.friendlyeshop.notifications.repository.FeedEventRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FeedService {

    private final FeedEventRepository feedEventRepository;

    private final int replaySize;

    public FeedService(
            FeedEventRepository feedEventRepository,
            @Value("${app.feed.replay-size:100}") int replaySize) {
        this.feedEventRepository = feedEventRepository;
        this.replaySize = replaySize;
    }

    @Transactional
    public boolean record(FeedEvent event) {
        int inserted = feedEventRepository.insertIfAbsent(
                event.getEventId(),
                event.getOrderId(),
                event.getTenantId(),
                event.getEventType(),
                event.getOccurredAt(),
                event.getPayload());
        return inserted > 0;
    }

    @Transactional(readOnly = true)
    public List<FeedEvent> recentEvents(UUID tenantId) {
        return feedEventRepository.findRecent(tenantId, PageRequest.of(0, replaySize));
    }
}
