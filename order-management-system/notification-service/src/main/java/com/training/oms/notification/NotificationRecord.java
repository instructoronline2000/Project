package com.training.oms.notification;

import java.time.Instant;

public record NotificationRecord(Instant sentAt, Long orderId, String channel, String message) {
}
