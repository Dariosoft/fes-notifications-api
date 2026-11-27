package com.friendlyeshop.notifications.messaging.rabbit.listener;

import com.friendlyeshop.notifications.messaging.rabbit.RetryAttempts;
import com.friendlyeshop.notifications.messaging.rabbit.publisher.DeadLetterPublisher;
import com.friendlyeshop.notifications.service.SellerNotificationService;
import com.rabbitmq.client.Channel;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class SellerNotificationListener {

    private static final Logger LOG = LoggerFactory.getLogger(SellerNotificationListener.class);

    private final SellerNotificationService sellerNotificationService;

    private final RetryAttempts retryAttempts;

    private final DeadLetterPublisher deadLetterPublisher;

    public SellerNotificationListener(
            SellerNotificationService sellerNotificationService,
            RetryAttempts retryAttempts,
            DeadLetterPublisher deadLetterPublisher) {
        this.sellerNotificationService = sellerNotificationService;
        this.retryAttempts = retryAttempts;
        this.deadLetterPublisher = deadLetterPublisher;
    }

    @RabbitListener(queues = "seller.notify")
    public void onSellerNotification(Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        try {
            String body = new String(message.getBody(), StandardCharsets.UTF_8);
            sellerNotificationService.process(message.getMessageProperties().getMessageId(), body);
            channel.basicAck(deliveryTag, false);
        } catch (RuntimeException exception) {
            handleFailure(message, channel, deliveryTag, exception);
        }
    }

    private void handleFailure(
            Message message, Channel channel, long deliveryTag, RuntimeException exception)
            throws IOException {
        int attempt = retryAttempts.currentAttempt(message);
        if (attempt < RetryAttempts.MAX_ATTEMPTS) {
            LOG.warn(
                    "Seller notification processing failed on attempt {} of {}; scheduling retry",
                    attempt,
                    RetryAttempts.MAX_ATTEMPTS,
                    exception);
            try {
                deadLetterPublisher.moveToRetry(message);
                channel.basicAck(deliveryTag, false);
            } catch (RuntimeException publishFailure) {
                channel.basicNack(deliveryTag, false, true);
            }
            return;
        }
        LOG.error(
                "Seller notification processing exhausted {} attempts; moving message to the dead letter queue",
                RetryAttempts.MAX_ATTEMPTS,
                exception);
        deadLetterPublisher.moveToDeadLetter(message);
        channel.basicAck(deliveryTag, false);
    }
}
