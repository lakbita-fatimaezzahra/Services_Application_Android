package com.example.servicechronometrejava;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ChronometreService extends Service {

    private final IBinder monBinder = new ServiceBinder();

    private int tempsTotal = 0;
    private boolean actif = false;

    private ScheduledExecutorService scheduler;
    private static final int ID_NOTIFICATION = 2002;
    private NotificationManager notifManager;

    public class ServiceBinder extends Binder {
        public ChronometreService getInstance() {
            return ChronometreService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        notifManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        initialiserChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        String cmd = (intent != null) ? intent.getAction() : null;

        if ("STOP_SERVICE".equals(cmd)) {
            stopSelf();
            return START_NOT_STICKY;
        }

        if (!actif) {
            actif = true;
            startForeground(ID_NOTIFICATION, construireNotification());
            lancerCompteur();
        }

        return START_STICKY;
    }

    private void lancerCompteur() {
        scheduler = Executors.newSingleThreadScheduledExecutor();

        scheduler.scheduleAtFixedRate(() -> {
            tempsTotal++;
            rafraichirNotif();
        }, 0, 1, TimeUnit.SECONDS);
    }

    private void initialiserChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel canal = new NotificationChannel(
                    "timer_channel_id",
                    "Service Timer",
                    NotificationManager.IMPORTANCE_LOW
            );
            notifManager.createNotificationChannel(canal);
        }
    }

    private Notification construireNotification() {
        return new NotificationCompat.Builder(this, "timer_channel_id")
                .setContentTitle("Service actif")
                .setContentText("Durée : " + convertirTemps(tempsTotal))
                .setSmallIcon(android.R.mipmap.sym_def_app_icon)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    private void rafraichirNotif() {
        notifManager.notify(ID_NOTIFICATION, construireNotification());
    }

    private String convertirTemps(int t) {
        int min = t / 60;
        int sec = t % 60;
        return String.format("%02d:%02d", min, sec);
    }

    // 🔥 IMPORTANT : permet à l'Activity de récupérer le temps
    public int getTempsActuel() {
        return tempsTotal;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return monBinder;
    }

    @Override
    public void onDestroy() {
        actif = false;

        if (scheduler != null) {
            scheduler.shutdown();
        }

        stopForeground(true);
        super.onDestroy();
    }
}