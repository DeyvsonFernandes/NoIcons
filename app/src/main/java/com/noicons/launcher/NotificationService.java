package com.noicons.launcher;

import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

public class NotificationService extends NotificationListenerService {

    @Override
    public void onListenerConnected() {
        try {
            NotificationStore.initial(getActiveNotifications());
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        NotificationStore.onPosted(sbn);
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        try {
            NotificationStore.cleanup(getActiveNotifications());
        } catch (Exception ignored) {
        }
    }
}