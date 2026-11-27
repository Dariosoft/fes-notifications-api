package com.friendlyeshop.notifications.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.friendlyeshop.notifications.messaging.kafka.model.FeedEventEnvelope;
import com.friendlyeshop.notifications.model.FeedEvent;
import java.io.IOException;
import java.time.Instant;
import java.util.UUID;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

class FeedBrokerTest {

    private final FeedBroker broker = new FeedBroker();

    @Test
    void ignoresPublishWithoutSubscribers() {
        assertDoesNotThrow(() -> broker.publish(event(UUID.randomUUID())));
    }

    @Test
    void publishesToSubscribedEmitters() throws IOException {
        UUID tenantId = UUID.randomUUID();
        SseEmitter emitter = mock(SseEmitter.class);
        broker.subscribe(tenantId, emitter);

        broker.publish(event(tenantId));

        verify(emitter).send(any(SseEmitter.SseEventBuilder.class));
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
    void removesEmitterOnCompletion() throws IOException {
        UUID tenantId = UUID.randomUUID();
        SseEmitter emitter = mock(SseEmitter.class);
        broker.subscribe(tenantId, emitter);
        ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
        verify(emitter).onCompletion(captor.capture());

        captor.getValue().run();

        broker.publish(event(tenantId));
        verify(emitter, never()).send(any(SseEmitter.SseEventBuilder.class));
    }

    @Test
    void completesEmitterOnTimeout() {
        UUID tenantId = UUID.randomUUID();
        SseEmitter emitter = mock(SseEmitter.class);
        broker.subscribe(tenantId, emitter);
        ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
        verify(emitter).onTimeout(captor.capture());

        captor.getValue().run();

        verify(emitter).complete();
    }

    @Test
    void removesEmitterOnError() throws IOException {
        UUID tenantId = UUID.randomUUID();
        SseEmitter emitter = mock(SseEmitter.class);
        broker.subscribe(tenantId, emitter);
        ArgumentCaptor<Consumer<Throwable>> captor = ArgumentCaptor.forClass(Consumer.class);
        verify(emitter).onError(captor.capture());

        captor.getValue().accept(new IllegalStateException("closed"));

        broker.publish(event(tenantId));
        verify(emitter, never()).send(any(SseEmitter.SseEventBuilder.class));
    }

    @Test
    void keepsOtherSubscribersWhenOneIsRemoved() throws IOException {
        UUID tenantId = UUID.randomUUID();
        SseEmitter removed = mock(SseEmitter.class);
        SseEmitter remaining = mock(SseEmitter.class);
        broker.subscribe(tenantId, removed);
        broker.subscribe(tenantId, remaining);

        broker.remove(tenantId, removed);
        broker.publish(event(tenantId));

        verify(remaining).send(any(SseEmitter.SseEventBuilder.class));
        verify(removed, never()).send(any(SseEmitter.SseEventBuilder.class));
    }

    @Test
    void registersAndRemovesEmitter() {
        UUID tenantId = UUID.randomUUID();

        SseEmitter emitter = broker.register(tenantId, 1000L);

        assertNotNull(emitter);
        assertDoesNotThrow(() -> broker.remove(tenantId, emitter));
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
