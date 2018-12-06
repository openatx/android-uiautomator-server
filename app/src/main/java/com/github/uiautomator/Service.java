package com.github.uiautomator;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.github.uiautomator.monitor.AbstractMonitor;
import com.github.uiautomator.monitor.BatteryMonitor;
import com.github.uiautomator.monitor.HttpPostNotifier;
import com.github.uiautomator.monitor.RotationMonitor;
import com.github.uiautomator.monitor.WifiMonitor;

import java.util.ArrayList;
import java.util.List;

public class Service extends android.app.Service {
    public static final String ACTION_START = "com.github.uiautomator.ACTION_START";
    public static final String ACTION_STOP = "com.github.uiautomator.ACTION_STOP";

    private static final String TAG = "UIAService";
    private static final int NOTIFICATION_ID = 0x1;

    private NotificationCompat.Builder builder;
    private List<AbstractMonitor> monitors = new ArrayList<>();

    @Override
    public IBinder onBind(Intent intent) {
        // We don't support binding to this service
        return null;
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private String createNotificationChannel(String channelId, String channelName){
        NotificationChannel chan = new NotificationChannel(channelId,
                channelName, NotificationManager.IMPORTANCE_NONE);
        chan.setLightColor(Color.BLUE);
        chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        NotificationManager service = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        service.createNotificationChannel(chan);
        return channelId;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        Intent notificationIntent = new Intent(this, MainActivity.class);
        String channelId = "";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            channelId = createNotificationChannel("my_service", "My Background Service");
        } else {
            // If earlier version channel ID is not used
            // https://developer.android.com/reference/android/support/v4/app/NotificationCompat.Builder.html#NotificationCompat.Builder(android.content.Context)
        }
        builder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.ic_notification)
                .setTicker(getString(R.string.service_ticker))
                .setContentTitle(getString(R.string.service_title))
                .setContentText(getString(R.string.service_text))
                .setContentIntent(PendingIntent.getActivity(this, NOTIFICATION_ID, notificationIntent, 0))
                .setWhen(System.currentTimeMillis());
        Notification notification = builder.build();
        startForeground(NOTIFICATION_ID, notification);

        HttpPostNotifier notifier = new HttpPostNotifier("http://127.0.0.1:7912");
        addMonitor(new BatteryMonitor(this, notifier));
        addMonitor(new RotationMonitor(this, notifier));
        addMonitor(new WifiMonitor(this, notifier));
    }

    public void setNotificationContentText(String text) {
        builder.setContentText(text);
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(NOTIFICATION_ID, builder.build());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "Stopping service");
        stopForeground(true);
        removeAllMonitor();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "On StartCommand");
        super.onStartCommand(intent, flags, startId);
        String action = intent.getAction();

        if (ACTION_START.equals(action)) {
            Log.i(TAG, "Receive start-service action, but ignore it");
        } else if (ACTION_STOP.equals(action)) {
            stopSelf();
        } else {
            Log.w(TAG, "Unknown action " + action);
        }

        return START_NOT_STICKY; // not start again, when killed by system
    }

    @Override
    public void onLowMemory() {
        Log.w(TAG, "Low memory");
    }

    private void addMonitor(AbstractMonitor monitor) {
        monitors.add(monitor);
    }

    private void removeAllMonitor() {
        for (AbstractMonitor monitor : monitors) {
            monitor.unregister();
        }
    }
}
