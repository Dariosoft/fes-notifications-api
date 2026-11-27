package com.friendlyeshop.notifications.repository;

import com.friendlyeshop.notifications.model.FeedEvent;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FeedEventRepository extends JpaRepository<FeedEvent, UUID> {

    @Modifying
    @Query(value = "INSERT INTO feed_events "
            + "(event_id, order_id, tenant_id, event_type, occurred_at, payload) "
            + "VALUES (:eventId, :orderId, :tenantId, :eventType, :occurredAt, CAST(:payload AS jsonb)) "
            + "ON CONFLICT (event_id) DO NOTHING", nativeQuery = true)
    int insertIfAbsent(
            @Param("eventId") UUID eventId,
            @Param("orderId") UUID orderId,
            @Param("tenantId") UUID tenantId,
            @Param("eventType") String eventType,
            @Param("occurredAt") Instant occurredAt,
            @Param("payload") String payload);

    @Query("SELECT event FROM FeedEvent event WHERE event.tenantId = :tenantId "
            + "ORDER BY event.createdAt DESC")
    List<FeedEvent> findRecent(@Param("tenantId") UUID tenantId, Pageable pageable);
}
