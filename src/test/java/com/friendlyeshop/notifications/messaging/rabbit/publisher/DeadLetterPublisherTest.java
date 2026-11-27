package com.friendlyeshop.notifications.messaging.rabbit.publisher;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

class DeadLetterPublisherTest {

    @Test
    void movesMessageToTheRetryQueue() {
        RabbitTemplate template = mock(RabbitTemplate.class);
        DeadLetterPublisher publisher = new DeadLetterPublisher(template);
        Message message = new Message("{}".getBytes(StandardCharsets.UTF_8), new MessageProperties());

        publisher.moveToRetry(message);

        verify(template).send(eq(""), eq("seller.notify.retry"), any(Message.class));
    }

    @Test
    void movesExhaustedMessageToTheDeadLetterQueue() {
        RabbitTemplate template = mock(RabbitTemplate.class);
        DeadLetterPublisher publisher = new DeadLetterPublisher(template);
        Message message = new Message("{}".getBytes(StandardCharsets.UTF_8), new MessageProperties());

        publisher.moveToDeadLetter(message);

        verify(template).send(eq(""), eq("seller.notify.dlq"), any(Message.class));
    }
}
