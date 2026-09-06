package com.training.oms.notification;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * In-memory store kept intentionally simple - this service has no database.
 * In a real system this would call an email/SMS/push provider instead.
 */
@Component
public class NotificationStore {

    private final List<NotificationRecord> notifications = new CopyOnWriteArrayList<>();

    public void add(NotificationRecord record) {
        notifications.add(record);
    }

    public List<NotificationRecord> getAll() {
        return List.copyOf(notifications);
    }
}
