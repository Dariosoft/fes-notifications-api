package com.friendlyeshop.notifications.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
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
import org.mockito.ArgumentCaptor;
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

    @Test
    void fallsBackToTheEventIdWhenTheHeaderIsBlank() {
        when(deliveryRepository.existsByMessageId(EVENT_ID.toString())).thenReturn(true);

        sellerNotificationService.process("   ", BODY);

        verify(deliveryRepository).existsByMessageId(EVENT_ID.toString());
        verifyNoInteractions(outOfBandSender);
    }

    @Test
    void rejectsPayloadWithoutStableIdentifier() {
        assertThatThrownBy(() -> sellerNotificationService.process(null, "{}"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("stable message identifier");
    }

    @Test
    void rejectsPayloadWithBlankEventId() {
        assertThatThrownBy(() -> sellerNotificationService.process(null, "{\"eventId\":\"  \"}"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("stable message identifier");
    }

    @Test
    void rejectsNonStringEventIdWhenTheHeaderIsMissing() {
        assertThatThrownBy(() -> sellerNotificationService.process(null, "{\"eventId\":123}"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("stable message identifier");
    }

    @Test
    void rejectsMalformedPayload() {
        assertThatThrownBy(() -> sellerNotificationService.process("amqp-message-id", "{not-json"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNonObjectPayload() {
        assertThatThrownBy(() -> sellerNotificationService.process("amqp-message-id", "[]"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void ignoresInvalidOptionalIdentifiers() {
        String body = "{\"eventId\":\"" + EVENT_ID + "\",\"orderId\":123,"
                + "\"tenantId\":\"not-a-uuid\",\"eventType\":\"order.placed\"}";
        when(deliveryRepository.existsByMessageId("amqp-message-id")).thenReturn(false);
        when(deliveryRepository.insertIfAbsent(
                eq("amqp-message-id"), isNull(), isNull(), eq("OUT_OF_BAND"), eq("SENT")))
                .thenReturn(1);
        ArgumentCaptor<OutOfBandNotification> captor =
                ArgumentCaptor.forClass(OutOfBandNotification.class);

        sellerNotificationService.process("amqp-message-id", body);

        verify(outOfBandSender).send(captor.capture());
        assertThat(captor.getValue().orderId()).isNull();
        assertThat(captor.getValue().tenantId()).isNull();
    }

    @Test
    void ignoresNullOptionalIdentifiers() {
        String body = "{\"eventId\":\"" + EVENT_ID + "\",\"orderId\":null,"
                + "\"tenantId\":null,\"eventType\":\"order.placed\"}";
        when(deliveryRepository.existsByMessageId("amqp-message-id")).thenReturn(false);
        when(deliveryRepository.insertIfAbsent(
                eq("amqp-message-id"), isNull(), isNull(), eq("OUT_OF_BAND"), eq("SENT")))
                .thenReturn(1);

        sellerNotificationService.process("amqp-message-id", body);

        verify(outOfBandSender).send(any(OutOfBandNotification.class));
    }
}
