package com.friendlyeshop.notifications.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.friendlyeshop.notifications.config.FeedBroker;
import com.friendlyeshop.notifications.messaging.kafka.model.FeedEventEnvelope;
import com.friendlyeshop.notifications.model.FeedEvent;
import com.friendlyeshop.notifications.service.FeedService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

class FeedControllerWebTest {

    private static final UUID EVENT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private static final UUID ORDER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    private static final UUID TENANT_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    private static final long TIMEOUT_MS = 1000L;

    private FeedService feedService;

    private FeedBroker feedBroker;

    private SseEmitter emitter;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        feedService = mock(FeedService.class);
        feedBroker = mock(FeedBroker.class);
        emitter = new SseEmitter(TIMEOUT_MS);
        when(feedBroker.register(TENANT_ID, TIMEOUT_MS)).thenReturn(emitter);
        FeedController controller = new FeedController(feedService, feedBroker, TIMEOUT_MS);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void opensAnEventStreamAndSendsTheReplayForTheTenant() throws Exception {
        FeedEvent event = FeedEvent.from(new FeedEventEnvelope(
                EVENT_ID,
                ORDER_ID,
                TENANT_ID,
                "order.placed",
                Instant.parse("2026-01-15T10:15:30Z"),
                "{\"eventId\":\"11111111-1111-1111-1111-111111111111\"}"));
        when(feedService.recentEvents(TENANT_ID)).thenReturn(List.of(event));

        MvcResult result = mockMvc.perform(get("/feed").param("tenantId", TENANT_ID.toString()))
                .andExpect(request().asyncStarted())
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM))
                .andReturn();

        verify(feedBroker).register(TENANT_ID, TIMEOUT_MS);
        verify(feedBroker).send(TENANT_ID, emitter, event);

        emitter.complete();
        mockMvc.perform(asyncDispatch(result)).andExpect(status().isOk());
    }
}
