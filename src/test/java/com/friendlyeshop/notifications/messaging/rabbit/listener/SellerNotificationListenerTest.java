package com.friendlyeshop.notifications.messaging.rabbit.listener;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.friendlyeshop.notifications.messaging.rabbit.RetryAttempts;
import com.friendlyeshop.notifications.messaging.rabbit.publisher.DeadLetterPublisher;
import com.friendlyeshop.notifications.service.SellerNotificationService;
import com.rabbitmq.client.Channel;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

@ExtendWith(MockitoExtension.class)
class SellerNotificationListenerTest {

    private static final String BODY = "{}";

    private static final long DELIVERY_TAG = 7L;

    @Mock
    private SellerNotificationService service;

    @Mock
    private RetryAttempts retryAttempts;

    @Mock
    private DeadLetterPublisher publisher;

    @Mock
    private Channel channel;

    @Mock
    private Message message;

    @Mock
    private MessageProperties properties;

    private SellerNotificationListener listener;

    @BeforeEach
    void setUp() {
        listener = new SellerNotificationListener(service, retryAttempts, publisher);
        when(message.getMessageProperties()).thenReturn(properties);
        when(message.getBody()).thenReturn(BODY.getBytes(UTF_8));
        when(properties.getDeliveryTag()).thenReturn(DELIVERY_TAG);
        when(properties.getMessageId()).thenReturn("message-1");
    }

    @Test
    void acknowledgesDeliveredNotification() throws IOException {
        listener.onSellerNotification(message, channel);

        verify(service).process("message-1", BODY);
        verify(channel).basicAck(DELIVERY_TAG, false);
    }

    @Test
    void schedulesRetryBeforeExhaustion() throws IOException {
        doThrow(new IllegalStateException("boom")).when(service).process("message-1", BODY);
        when(retryAttempts.currentAttempt(message)).thenReturn(2);

        listener.onSellerNotification(message, channel);

        verify(publisher).moveToRetry(message);
        verify(publisher, never()).moveToDeadLetter(message);
        verify(channel).basicAck(DELIVERY_TAG, false);
    }

    @Test
    void movesToDeadLetterAfterExhaustion() throws IOException {
        doThrow(new IllegalStateException("boom")).when(service).process("message-1", BODY);
        when(retryAttempts.currentAttempt(message)).thenReturn(RetryAttempts.MAX_ATTEMPTS);

        listener.onSellerNotification(message, channel);

        verify(publisher).moveToDeadLetter(message);
        verify(publisher, never()).moveToRetry(message);
        verify(channel).basicAck(DELIVERY_TAG, false);
    }

    @Test
    void requeuesWhenTheRetryPublishFails() throws IOException {
        doThrow(new IllegalStateException("boom")).when(service).process("message-1", BODY);
        when(retryAttempts.currentAttempt(message)).thenReturn(2);
        doThrow(new IllegalStateException("rabbit down")).when(publisher).moveToRetry(message);

        listener.onSellerNotification(message, channel);

        verify(channel).basicNack(DELIVERY_TAG, false, true);
        verify(channel, never()).basicAck(DELIVERY_TAG, false);
    }
}
