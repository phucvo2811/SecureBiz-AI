package com.securebiz.outbox;

import com.securebiz.notification.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OutboxDispatcher {
    private static final Logger log = LoggerFactory.getLogger(OutboxDispatcher.class);
    private final OutboxRepository outbox;
    private final NotificationRepository notifications;
    private final int batchSize;
    private final int maxAttempts;

    public OutboxDispatcher(OutboxRepository outbox, NotificationRepository notifications,
                            @Value("${securebiz.outbox.batch-size:20}") int batchSize,
                            @Value("${securebiz.outbox.max-attempts:10}") int maxAttempts) {
        this.outbox = outbox;
        this.notifications = notifications;
        this.batchSize = Math.max(1, batchSize);
        this.maxAttempts = Math.max(1, maxAttempts);
    }

    @Scheduled(fixedDelayString = "${securebiz.outbox.poll-delay-ms:1000}")
    @Transactional
    public void dispatch() {
        for (int handled = 0; handled < batchSize; handled++) {
            var event = outbox.lockNext(maxAttempts);
            if (event.isEmpty()) return;
            try {
                handle(event.get());
                outbox.published(event.get().id());
            } catch (RuntimeException failure) {
                outbox.failed(event.get().id(), failure.getMessage());
                log.warn("Outbox event {} dispatch failed on attempt {}", event.get().id(),
                        event.get().attemptCount() + 1, failure);
                return;
            }
        }
    }

    private void handle(OutboxRepository.OutboxEvent event) {
        if (!"order".equals(event.aggregateType()) || !"ORDER_COMPLETED".equals(event.eventType())) {
            throw new IllegalArgumentException("Unsupported outbox event type: " + event.eventType());
        }
        var recipient = outbox.orderRecipient(event.aggregateId())
                .orElseThrow(() -> new IllegalStateException("Outbox order no longer exists"));
        notifications.create(recipient.userId(), "ORDER_COMPLETED", "Order completed",
                "Order " + recipient.orderNumber() + " was completed successfully.",
                "order", event.aggregateId());
    }
}
