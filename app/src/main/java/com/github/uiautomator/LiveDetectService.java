package com.github.uiautomator;

import android.app.IntentService;
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

import com.github.uiautomator.util.OkhttpManager;

import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class LiveDetectService extends IntentService {
    public static final String ACTION_START = "com.github.uiautomator.liveDetect.ACTION_START";
    public static final String ACTION_STOP = "com.github.uiautomator.liveDetect.ACTION_STOP";

    private static final String TAG = "LiveDetectService";
    private static final int NOTIFICATION_ID = 0x2;

    private NotificationCompat.Builder builder;

    public LiveDetectService() {
        super("LiveDetectService");
    }

    @Override
    public IBinder onBind(Intent intent) {
        // We don't support binding to this service
        return null;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        while(true) {
            try {
                Thread.sleep(10 * 1000);
                final String url = "http://127.0.0.1:7912/ping";
                OkhttpManager.getSingleton().post(url, new JSONObject().toString(), new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        String str = getString(R.string.livedetect_service_text) + getString(R.string.agent_die);
                        setNotificationContentText(str);
                        Log.e(TAG, "call url:" + url + " is failed");
                        Log.e(TAG, "exception:" + e);
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        String result = response.body().string();
                        if (result.equals("pong")) {
                            setNotificationContentText(getString(R.string.livedetect_service_text) + getString(R.string.agent_live));
                        }
                        Log.i(TAG, result);
                    }
                });
            } catch (InterruptedException e) {
                e.printStackTrace();
                Log.e(TAG, e.getMessage());
            }
        }
    }

    public void setNotificationContentText(String text) {
        builder.setContentText(text);
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(NOTIFICATION_ID, builder.build());
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
            channelId = createNotificationChannel("live detect service", "Live Detect Service");
        } else {
            // If earlier version channel ID is not used
            // https://developer.android.com/reference/android/support/v4/app/NotificationCompat.Builder.html#NotificationCompat.Builder(android.content.Context)
        }
        builder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.ic_notification)
                .setTicker(getString(R.string.livedetect_service_ticker))
                .setContentTitle(getString(R.string.livedetect_service_title))
                .setContentText(getString(R.string.livedetect_service_text))
                .setContentIntent(PendingIntent.getActivity(this, NOTIFICATION_ID, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT))
                .setWhen(System.currentTimeMillis());
        Notification notification = builder.build();
        startForeground(NOTIFICATION_ID, notification);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "Stopping service");
        stopForeground(true);
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
}
