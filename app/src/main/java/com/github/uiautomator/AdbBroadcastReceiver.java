package com.github.uiautomator;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.util.Log;

public class AdbBroadcastReceiver extends BroadcastReceiver {

    private MockLocationProvider mockGPS;
    private MockLocationProvider mockWifi;
    private static final String TAG = "MockGPSReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals("stop.mock")) {
            if (mockGPS != null) {
                mockGPS.shutdown();
            }
            if (mockWifi != null) {
                mockWifi.shutdown();
            }
        } else {
            mockGPS = new MockLocationProvider(LocationManager.GPS_PROVIDER, context);
            mockWifi = new MockLocationProvider(LocationManager.NETWORK_PROVIDER, context);

            double lat = Double.parseDouble(intent.getStringExtra("lat") != null ? intent.getStringExtra("lat") : "0");
            double lon = Double.parseDouble(intent.getStringExtra("lon") != null ? intent.getStringExtra("lon") : "0");
            double alt = Double.parseDouble(intent.getStringExtra("alt") != null ? intent.getStringExtra("alt") : "0");
            float accurate = Float.parseFloat(intent.getStringExtra("accurate") != null ? intent.getStringExtra("accurate") : "0");
            Log.i(TAG, String.format("setting mock to Latitude=%f, Longitude=%f Altitude=%f Accuracy=%f", lat, lon, alt, accurate));
            mockGPS.pushLocation(lat, lon, alt, accurate);
            mockWifi.pushLocation(lat, lon, alt, accurate);
        }
    }
}
