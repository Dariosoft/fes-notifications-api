package com.friendlyeshop.notifications.config;

import com.friendlyeshop.notifications.model.FeedEvent;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Component
public class FeedBroker {

    private static final Logger LOG = LoggerFactory.getLogger(FeedBroker.class);

    private final Map<UUID, Set<SseEmitter>> emittersByTenant = new ConcurrentHashMap<>();

    public SseEmitter register(UUID tenantId, long timeoutMs) {
        SseEmitter emitter = new SseEmitter(timeoutMs);
        subscribe(tenantId, emitter);
        return emitter;
    }

    void subscribe(UUID tenantId, SseEmitter emitter) {
        emittersByTenant.computeIfAbsent(tenantId, id -> ConcurrentHashMap.newKeySet()).add(emitter);
        emitter.onCompletion(() -> remove(tenantId, emitter));
        emitter.onTimeout(() -> {
            remove(tenantId, emitter);
            emitter.complete();
        });
        emitter.onError(error -> remove(tenantId, emitter));
    }

    public void publish(FeedEvent event) {
        Set<SseEmitter> tenantEmitters = emittersByTenant.get(event.getTenantId());
        if (tenantEmitters == null) {
            return;
        }
        for (SseEmitter emitter : tenantEmitters) {
            send(event.getTenantId(), emitter, event);
        }
    }

    public void send(UUID tenantId, SseEmitter emitter, FeedEvent event) {
        try {
            emitter.send(SseEmitter.event()
                    .id(event.getEventId().toString())
                    .name(event.getEventType())
                    .data(event.getPayload()));
        } catch (IOException | IllegalStateException exception) {
            LOG.warn(
                    "Removing feed subscriber for tenant {} after send failure",
                    tenantId,
                    exception);
            remove(tenantId, emitter);
            emitter.completeWithError(exception);
        }
    }

    public void remove(UUID tenantId, SseEmitter emitter) {
        emittersByTenant.computeIfPresent(tenantId, (id, tenantEmitters) -> {
            tenantEmitters.remove(emitter);
            return tenantEmitters.isEmpty() ? null : tenantEmitters;
        });
    }
}
