package com.github.uiautomator;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.github.uiautomator.util.Permissons4App;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class IdentifyActivity extends Activity {
    private static final String TAG = "IdentityActivity";
    public static final String ACTION_IDENTITY = "com.github.uiautomator.ACTION_IDENTIFY";
    public static final String EXTRA_SERIAL = "serial";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();

        LinearLayout layout = new LinearLayout(this);
        layout.setKeepScreenOn(true);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
        ));

        Bundle extras = intent.getExtras();
        String activityTheme = "BLACK";
        if (extras != null && extras.getString("theme") != null) {
            activityTheme = extras.getString("theme").toUpperCase();
        }
        Log.i(TAG, "theme " + activityTheme);
        Float brightness = 0.1f;
        Integer backgroundColor = Color.BLACK;
        if (activityTheme != null && "RED".equals(activityTheme)) {
            backgroundColor = Color.RED;
            brightness = 1.0f;
        }

        layout.setBackgroundColor(backgroundColor);
        layout.setPadding(16, 16, 16, 16);
        layout.setGravity(Gravity.CENTER);

        TelephonyManager tm = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);

        String serial = intent.getStringExtra(EXTRA_SERIAL);

        if (serial == null) {
            serial = getProperty("ro.serialno", "unknown");
        }

        layout.addView(createLabel("SERIAL"));
        layout.addView(createData(serial));
        layout.addView(createLabel("MODEL"));
        layout.addView(createData(getProperty("ro.product.model", "unknown")));
        layout.addView(createLabel("VERSION"));
        layout.addView(createData(Build.VERSION.RELEASE + " (SDK " + Build.VERSION.SDK_INT + ")"));
        layout.addView(createLabel("OPERATOR"));
        layout.addView(createData(tm.getSimOperatorName()));
        layout.addView(createLabel("PHONE"));
        layout.addView(createData(tm.getLine1Number()));
        layout.addView(createLabel("IMEI"));
        layout.addView(createData(tm.getDeviceId()));
        layout.addView(createLabel("ICCID"));
        layout.addView(createData(tm.getSimSerialNumber()));

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        ensureVisibility(brightness);
        setContentView(layout);
    }

    private View createLabel(String text) {
        TextView titleView = new TextView(this);
        titleView.setGravity(Gravity.CENTER);
        titleView.setTextColor(Color.parseColor("#c1272d"));
        titleView.setTextSize(16f);
        titleView.setText(text);
        return titleView;
    }

    private View createData(String text) {
        TextView dataView = new TextView(this);
        dataView.setGravity(Gravity.CENTER);
        dataView.setTextColor(Color.WHITE);
        dataView.setTextSize(24f);
        dataView.setText(text);
        return dataView;
    }

    private void ensureVisibility(Float brightness) {
        Window window = getWindow();

        window.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        window.addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);

        unlock();

        WindowManager.LayoutParams params = window.getAttributes();
        params.screenBrightness = brightness;
        window.setAttributes(params);
    }

    private String getProperty(String name, String defaultValue) {
        try {
            Class<?> SystemProperties = Class.forName("android.os.SystemProperties");
            Method get = SystemProperties.getMethod("get", String.class, String.class);
            return (String) get.invoke(SystemProperties, name, defaultValue);
        } catch (ClassNotFoundException e) {
            Log.e(TAG, "Class.forName() failed", e);
            return defaultValue;
        } catch (NoSuchMethodException e) {
            Log.e(TAG, "getMethod() failed", e);
            return defaultValue;
        } catch (InvocationTargetException e) {
            Log.e(TAG, "invoke() failed", e);
            return defaultValue;
        } catch (IllegalAccessException e) {
            Log.e(TAG, "invoke() failed", e);
            return defaultValue;
        }
    }

    @SuppressWarnings("deprecation")
    private void unlock() {
        KeyguardManager keyguardManager = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
        keyguardManager.newKeyguardLock("InputService/Unlock").disableKeyguard();
    }
}
