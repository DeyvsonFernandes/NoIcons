package com.noicons.launcher;

import android.app.Notification;
import android.service.notification.StatusBarNotification;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class NotificationStore {

    public interface Listener {
        void onChanged();
    }

    private static final Map<String, Integer> counts = new HashMap<>();
    private static Listener listener;

    public static void setListener(Listener l) {
        listener = l;
    }

    public static int getCount(String pkg) {
        Integer c = counts.get(pkg);
        return c == null ? 0 : c;
    }

    private static boolean ignorar(StatusBarNotification sbn) {
        int flags = sbn.getNotification().flags;
        return (flags & Notification.FLAG_ONGOING_EVENT) != 0
                || (flags & Notification.FLAG_GROUP_SUMMARY) != 0
                || sbn.getPackageName().equals("com.example.brainbreak");
    }

    // chamada a cada notificação nova que chega
    public static void onPosted(StatusBarNotification sbn) {
        if (ignorar(sbn)) return;
        counts.put(sbn.getPackageName(), getCount(sbn.getPackageName()) + 1);
        notificar();
    }

    // notificações que já estavam na barra quando o serviço conectou
    public static void initial(StatusBarNotification[] list) {
        if (list == null) return;
        for (StatusBarNotification sbn : list) {
            if (ignorar(sbn)) continue;
            String pkg = sbn.getPackageName();
            if (!counts.containsKey(pkg)) counts.put(pkg, 1);
        }
        notificar();
    }

    // se o app não tem mais nenhuma notificação ativa, zera o número
    public static void cleanup(StatusBarNotification[] list) {
        Set<String> ativos = new HashSet<>();
        if (list != null) {
            for (StatusBarNotification sbn : list) {
                if (!ignorar(sbn)) ativos.add(sbn.getPackageName());
            }
        }
        counts.keySet().retainAll(ativos);
        notificar();
    }

    public static void markSeen(String pkg) {
        counts.remove(pkg);
        notificar();
    }

    private static void notificar() {
        if (listener != null) listener.onChanged();
    }
}