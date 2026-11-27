package com.friendlyeshop.notifications.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.friendlyeshop.notifications.messaging.kafka.model.FeedEventEnvelope;
import com.friendlyeshop.notifications.model.FeedEvent;
import com.friendlyeshop.notifications.repository.FeedEventRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class FeedServiceTest {

    private static final UUID EVENT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private static final UUID ORDER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    private static final UUID TENANT_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    private static final String PAYLOAD = "{\"eventId\":\"11111111-1111-1111-1111-111111111111\"}";

    @Mock
    private FeedEventRepository feedEventRepository;

    private FeedService feedService;

    @BeforeEach
    void setUp() {
        feedService = new FeedService(feedEventRepository, 100);
    }

    @Test
    void recordsANewEvent() {
        FeedEvent event = feedEvent();
        when(feedEventRepository.insertIfAbsent(
                eq(EVENT_ID),
                eq(ORDER_ID),
                eq(TENANT_ID),
                eq("order.placed"),
                any(Instant.class),
                eq(PAYLOAD))).thenReturn(1);

        assertThat(feedService.record(event)).isTrue();
    }

    @Test
    void doesNotReportADuplicateEventAsRecorded() {
        FeedEvent event = feedEvent();
        when(feedEventRepository.insertIfAbsent(
                any(), any(), any(), any(), any(), any())).thenReturn(0);

        assertThat(feedService.record(event)).isFalse();
    }

    @Test
    void returnsTheMostRecentEventsUsingTheReplaySize() {
        FeedEvent event = feedEvent();
        when(feedEventRepository.findRecent(eq(TENANT_ID), any(Pageable.class)))
                .thenReturn(List.of(event));

        assertThat(feedService.recentEvents(TENANT_ID)).containsExactly(event);

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(feedEventRepository).findRecent(eq(TENANT_ID), pageable.capture());
        assertThat(pageable.getValue().getPageSize()).isEqualTo(100);
    }

    private static FeedEvent feedEvent() {
        return FeedEvent.from(new FeedEventEnvelope(
                EVENT_ID,
                ORDER_ID,
                TENANT_ID,
                "order.placed",
                Instant.parse("2026-01-15T10:15:30Z"),
                PAYLOAD));
    }
}
