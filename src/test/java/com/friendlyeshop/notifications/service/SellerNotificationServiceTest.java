package com.friendlyeshop.notifications.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.friendlyeshop.notifications.client.OutOfBandNotification;
import com.friendlyeshop.notifications.client.OutOfBandSender;
import com.friendlyeshop.notifications.repository.DeliveryRepository;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
class SellerNotificationServiceTest {

    private static final UUID EVENT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private static final UUID ORDER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    private static final UUID TENANT_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    private static final String BODY = "{\"eventId\":\"" + EVENT_ID + "\",\"orderId\":\"" + ORDER_ID + "\","
            + "\"tenantId\":\"" + TENANT_ID + "\",\"eventType\":\"order.placed\"}";

    @Mock
    private DeliveryRepository deliveryRepository;

    @Mock
    private OutOfBandSender outOfBandSender;

    private SellerNotificationService sellerNotificationService;

    @BeforeEach
    void setUp() {
        sellerNotificationService = new SellerNotificationService(
                deliveryRepository,
                outOfBandSender,
                JsonMapper.builder().build());
    }

    @Test
    void usesTheAmqpMessageIdFirst() {
        when(deliveryRepository.existsByMessageId("amqp-message-id")).thenReturn(true);

        sellerNotificationService.process("amqp-message-id", BODY);

        verify(deliveryRepository).existsByMessageId("amqp-message-id");
        verifyNoInteractions(outOfBandSender);
    }

    @Test
    void fallsBackToTheEventIdWhenTheHeaderIsMissing() {
        when(deliveryRepository.existsByMessageId(EVENT_ID.toString())).thenReturn(true);

        sellerNotificationService.process(null, BODY);

        verify(deliveryRepository).existsByMessageId(EVENT_ID.toString());
        verifyNoInteractions(outOfBandSender);
    }

    @Test
    void registersAndDelegatesANewDelivery() {
        when(deliveryRepository.existsByMessageId("amqp-message-id")).thenReturn(false);
        when(deliveryRepository.insertIfAbsent(
                eq("amqp-message-id"), eq(ORDER_ID), eq(TENANT_ID), eq("OUT_OF_BAND"), eq("SENT")))
                .thenReturn(1);

        sellerNotificationService.process("amqp-message-id", BODY);

        verify(outOfBandSender).send(any(OutOfBandNotification.class));
    }

    @Test
    void skipsDelegationWhenTheDeliveryWasInsertedConcurrently() {
        when(deliveryRepository.existsByMessageId("amqp-message-id")).thenReturn(false);
        when(deliveryRepository.insertIfAbsent(
                eq("amqp-message-id"), eq(ORDER_ID), eq(TENANT_ID), eq("OUT_OF_BAND"), eq("SENT")))
                .thenReturn(0);

        sellerNotificationService.process("amqp-message-id", BODY);

        verify(outOfBandSender, never()).send(any());
    }
}
