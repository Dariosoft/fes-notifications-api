package com.friendlyeshop.notifications.messaging.rabbit.publisher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class DeadLetterPublisher {

    private static final Logger LOG = LoggerFactory.getLogger(DeadLetterPublisher.class);

    private static final String DEFAULT_EXCHANGE = "";

    private static final String SELLER_NOTIFY_RETRY = "seller.notify.retry";

    private static final String SELLER_NOTIFY_DLQ = "seller.notify.dlq";

    private final RabbitTemplate rabbitTemplate;

    public DeadLetterPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void moveToRetry(Message original) {
        send(original, SELLER_NOTIFY_RETRY);
    }

    public void moveToDeadLetter(Message original) {
        send(original, SELLER_NOTIFY_DLQ);
        LOG.warn(
                "Moved exhausted seller notification to {} messageId={}",
                SELLER_NOTIFY_DLQ,
                original.getMessageProperties().getMessageId());
    }

    private void send(Message original, String routingKey) {
        Message copy = MessageBuilder.fromClonedMessage(original).build();
        rabbitTemplate.send(DEFAULT_EXCHANGE, routingKey, copy);
    }
}
