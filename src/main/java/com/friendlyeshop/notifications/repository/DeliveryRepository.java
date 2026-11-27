package com.friendlyeshop.notifications.repository;

import com.friendlyeshop.notifications.model.Delivery;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DeliveryRepository extends JpaRepository<Delivery, String> {

    boolean existsByMessageId(String messageId);

    @Modifying
    @Query(value = "INSERT INTO deliveries "
            + "(message_id, order_id, tenant_id, channel, status) "
            + "VALUES (:messageId, :orderId, :tenantId, :channel, :status) "
            + "ON CONFLICT (message_id) DO NOTHING", nativeQuery = true)
    int insertIfAbsent(
            @Param("messageId") String messageId,
            @Param("orderId") UUID orderId,
            @Param("tenantId") UUID tenantId,
            @Param("channel") String channel,
            @Param("status") String status);
}
