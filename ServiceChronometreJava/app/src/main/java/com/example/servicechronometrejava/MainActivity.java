package com.example.servicechronometrejava;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Handler;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private TextView txtAffichage;
    private Button btnLancer, btnArreter;

    private ChronometreService serviceTimer;
    private boolean connecte = false;

    private Handler handler = new Handler();

    private final ServiceConnection liaison = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            ChronometreService.ServiceBinder binder =
                    (ChronometreService.ServiceBinder) service;

            serviceTimer = binder.getInstance();
            connecte = true;

            mettreAJourTemps(); // 🔥 lancement affichage
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            connecte = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txtAffichage = findViewById(R.id.tvTemps);
        btnLancer = findViewById(R.id.btnStart);
        btnArreter = findViewById(R.id.btnStop);

        btnLancer.setOnClickListener(v -> demarrerService());
        btnArreter.setOnClickListener(v -> arreterService());
    }

    private void demarrerService() {
        Intent intent = new Intent(this, ChronometreService.class);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }

        bindService(intent, liaison, Context.BIND_AUTO_CREATE);
    }

    private void arreterService() {
        Intent intent = new Intent(this, ChronometreService.class);
        intent.setAction("STOP_SERVICE");

        stopService(intent);

        if (connecte) {
            unbindService(liaison);
            connecte = false;
        }

        handler.removeCallbacksAndMessages(null);

        txtAffichage.setText("00:00");
    }

    private void mettreAJourTemps() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {

                if (connecte && serviceTimer != null) {

                    int t = serviceTimer.getTempsActuel();

                    int min = t / 60;
                    int sec = t % 60;

                    txtAffichage.setText(String.format("%02d:%02d", min, sec));
                }

                handler.postDelayed(this, 1000);
            }
        }, 1000);
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);

        if (connecte) {
            unbindService(liaison);
        }

        super.onDestroy();
    }
}