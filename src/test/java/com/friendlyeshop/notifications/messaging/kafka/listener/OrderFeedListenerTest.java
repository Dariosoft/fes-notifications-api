package com.friendlyeshop.notifications.messaging.kafka.listener;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.friendlyeshop.notifications.config.FeedBroker;
import com.friendlyeshop.notifications.exception.InvalidFeedEventException;
import com.friendlyeshop.notifications.messaging.kafka.codec.FeedEventEnvelopeCodec;
import com.friendlyeshop.notifications.messaging.kafka.model.FeedEventEnvelope;
import com.friendlyeshop.notifications.model.FeedEvent;
import com.friendlyeshop.notifications.service.FeedService;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderFeedListenerTest {

    private static final String PAYLOAD = "{\"eventId\":\"00000000-0000-0000-0000-000000000001\"}";

    @Mock
    private FeedEventEnvelopeCodec codec;

    @Mock
    private FeedService feedService;

    @Mock
    private FeedBroker feedBroker;

    private OrderFeedListener listener;

    private FeedEventEnvelope envelope;

    @BeforeEach
    void setUp() {
        listener = new OrderFeedListener(codec, feedService, feedBroker);
        envelope = new FeedEventEnvelope(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "order.placed",
                Instant.now(),
                PAYLOAD);
    }

    @Test
    void publishesNewEventToTheFeed() {
        when(codec.decode(PAYLOAD)).thenReturn(envelope);
        when(feedService.record(any(FeedEvent.class))).thenReturn(true);

        listener.onOrderEvent(PAYLOAD);

        verify(feedBroker).publish(any(FeedEvent.class));
    }

    @Test
    void doesNotPublishDuplicates() {
        when(codec.decode(PAYLOAD)).thenReturn(envelope);
        when(feedService.record(any(FeedEvent.class))).thenReturn(false);

        listener.onOrderEvent(PAYLOAD);

        verify(feedBroker, never()).publish(any(FeedEvent.class));
    }

    @Test
    void discardsInvalidEvents() {
        when(codec.decode(PAYLOAD)).thenThrow(new InvalidFeedEventException("bad"));

        listener.onOrderEvent(PAYLOAD);

        verifyNoInteractions(feedService, feedBroker);
    }
}
