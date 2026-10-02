package com.securebiz.outbox;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class OutboxRepository {
    private final JdbcClient jdbc;

    public OutboxRepository(JdbcClient jdbc) { this.jdbc = jdbc; }

    public Optional<OutboxEvent> lockNext(int maxAttempts) {
        return jdbc.sql("""
                SELECT id, aggregate_type, aggregate_id, event_type, payload::text, attempt_count
                FROM outbox_events
                WHERE published_at IS NULL AND attempt_count < :maxAttempts
                ORDER BY created_at, id
                FOR UPDATE SKIP LOCKED
                LIMIT 1
                """).param("maxAttempts", maxAttempts)
                .query((row, number) -> new OutboxEvent(
                        row.getObject("id", UUID.class), row.getString("aggregate_type"),
                        row.getObject("aggregate_id", UUID.class), row.getString("event_type"),
                        row.getString("payload"), row.getInt("attempt_count")))
                .optional();
    }

    public Optional<OrderRecipient> orderRecipient(UUID orderId) {
        return jdbc.sql("SELECT created_by, order_number FROM orders WHERE id=:id")
                .param("id", orderId)
                .query((row, number) -> new OrderRecipient(
                        row.getObject("created_by", UUID.class), row.getString("order_number")))
                .optional();
    }

    public void published(UUID id) {
        jdbc.sql("UPDATE outbox_events SET published_at=now(),attempt_count=attempt_count+1,last_error=NULL WHERE id=:id")
                .param("id", id).update();
    }

    public void failed(UUID id, String error) {
        String safe = error == null ? "Unknown dispatch failure" : error.substring(0, Math.min(1000, error.length()));
        jdbc.sql("UPDATE outbox_events SET attempt_count=attempt_count+1,last_error=:error WHERE id=:id")
                .param("id", id).param("error", safe).update();
    }

    public record OutboxEvent(UUID id, String aggregateType, UUID aggregateId,
                              String eventType, String payload, int attemptCount) {}
    public record OrderRecipient(UUID userId, String orderNumber) {}
}
