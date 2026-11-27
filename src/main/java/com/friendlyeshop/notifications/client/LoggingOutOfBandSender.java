package com.friendlyeshop.notifications.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LoggingOutOfBandSender implements OutOfBandSender {

    private static final Logger LOG = LoggerFactory.getLogger(LoggingOutOfBandSender.class);

    @Override
    public void send(OutOfBandNotification notification) {
        LOG.info(
                "Out-of-band seller notification delivered messageId={} orderId={} tenantId={}",
                notification.messageId(),
                notification.orderId(),
                notification.tenantId());
    }
}
