package com.securebiz.outbox;

import com.securebiz.notification.repository.NotificationRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.*;

class OutboxDispatcherTest {
    @Test
    void completedOrderCreatesNotificationAndMarksEventPublishedOnce() {
        var repository=mock(OutboxRepository.class);var notifications=mock(NotificationRepository.class);
        UUID eventId=UUID.randomUUID(),orderId=UUID.randomUUID(),userId=UUID.randomUUID();
        var event=new OutboxRepository.OutboxEvent(eventId,"order",orderId,"ORDER_COMPLETED","{}",0);
        when(repository.lockNext(10)).thenReturn(Optional.of(event),Optional.empty());
        when(repository.orderRecipient(orderId)).thenReturn(Optional.of(new OutboxRepository.OrderRecipient(userId,"ORD-001")));

        new OutboxDispatcher(repository,notifications,20,10).dispatch();

        verify(notifications).create(userId,"ORDER_COMPLETED","Order completed",
                "Order ORD-001 was completed successfully.","order",orderId);
        verify(repository).published(eventId);
        verify(repository,never()).failed(any(),any());
    }

    @Test
    void unsupportedEventRecordsFailureAndStopsBatchForLaterRetry() {
        var repository=mock(OutboxRepository.class);var notifications=mock(NotificationRepository.class);
        UUID eventId=UUID.randomUUID();
        var event=new OutboxRepository.OutboxEvent(eventId,"unknown",UUID.randomUUID(),"UNKNOWN","{}",2);
        when(repository.lockNext(10)).thenReturn(Optional.of(event));

        new OutboxDispatcher(repository,notifications,20,10).dispatch();

        verify(repository).failed(eq(eventId),contains("Unsupported outbox event type"));
        verify(repository,never()).published(any());
        verifyNoInteractions(notifications);
        verify(repository,times(1)).lockNext(10);
    }
}
