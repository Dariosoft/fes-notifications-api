package com.friendlyeshop.notifications.messaging.kafka.listener;

import com.friendlyeshop.notifications.config.FeedBroker;
import com.friendlyeshop.notifications.exception.InvalidFeedEventException;
import com.friendlyeshop.notifications.messaging.kafka.codec.FeedEventEnvelopeCodec;
import com.friendlyeshop.notifications.messaging.kafka.model.FeedEventEnvelope;
import com.friendlyeshop.notifications.model.FeedEvent;
import com.friendlyeshop.notifications.service.FeedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class OrderFeedListener {

    private static final Logger LOG = LoggerFactory.getLogger(OrderFeedListener.class);

    private final FeedEventEnvelopeCodec envelopeCodec;

    private final FeedService feedService;

    private final FeedBroker feedBroker;

    public OrderFeedListener(
            FeedEventEnvelopeCodec envelopeCodec,
            FeedService feedService,
            FeedBroker feedBroker) {
        this.envelopeCodec = envelopeCodec;
        this.feedService = feedService;
        this.feedBroker = feedBroker;
    }

    @KafkaListener(topics = "orders.events", groupId = "notifications.feed")
    public void onOrderEvent(String payload) {
        FeedEventEnvelope envelope;
        try {
            envelope = envelopeCodec.decode(payload);
        } catch (InvalidFeedEventException exception) {
            LOG.warn("Discarding invalid order feed event: {}", exception.getMessage());
            return;
        }
        FeedEvent event = FeedEvent.from(envelope);
        if (feedService.record(event)) {
            feedBroker.publish(event);
        }
    }
}
