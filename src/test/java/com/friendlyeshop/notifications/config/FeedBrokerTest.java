package com.friendlyeshop.notifications.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.friendlyeshop.notifications.messaging.kafka.model.FeedEventEnvelope;
import com.friendlyeshop.notifications.model.FeedEvent;
import java.io.IOException;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

class FeedBrokerTest {

    private final FeedBroker broker = new FeedBroker();

    @Test
    void ignoresPublishWithoutSubscribers() {
        broker.publish(event(UUID.randomUUID()));
    }

    @Test
    void sendsEventToEmitter() throws IOException {
        UUID tenantId = UUID.randomUUID();
        SseEmitter emitter = mock(SseEmitter.class);

        broker.send(tenantId, emitter, event(tenantId));

        verify(emitter).send(any(SseEmitter.SseEventBuilder.class));
    }

    @Test
    void removesEmitterWhenSendFails() throws IOException {
        UUID tenantId = UUID.randomUUID();
        SseEmitter emitter = mock(SseEmitter.class);
        doThrow(new IOException("closed")).when(emitter).send(any(SseEmitter.SseEventBuilder.class));

        broker.send(tenantId, emitter, event(tenantId));

        verify(emitter).completeWithError(any(IOException.class));
    }

    @Test
    void registersAndRemovesEmitter() {
        UUID tenantId = UUID.randomUUID();
        SseEmitter emitter = broker.register(tenantId, 1000L);

        broker.remove(tenantId, emitter);
    }

    private static FeedEvent event(UUID tenantId) {
        FeedEventEnvelope envelope = new FeedEventEnvelope(
                UUID.randomUUID(),
                UUID.randomUUID(),
                tenantId,
                "order.placed",
                Instant.now(),
                "{}");
        return FeedEvent.from(envelope);
    }
}
