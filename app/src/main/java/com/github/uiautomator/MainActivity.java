package com.github.uiautomator;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.github.uiautomator.handler.CoorHandler;
import com.github.uiautomator.screenrecorder.AudioEncodeConfig;
import com.github.uiautomator.screenrecorder.RecoderActivity;
import com.github.uiautomator.screenrecorder.RecorderManager;
import com.github.uiautomator.screenrecorder.VideoEncodeConfig;
import com.github.uiautomator.screenrecorder.http.CoordinateServer;
import com.github.uiautomator.view.FloatView;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static android.Manifest.permission.RECORD_AUDIO;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static android.os.Build.VERSION_CODES.M;

public class MainActivity extends Activity implements View.OnClickListener{
    private final String TAG = "ATXMainActivity";

    private Button btnFinish;
    private Button btnIdentify;
    private Button btnAccessibility;
    private Button btnDevelopmentSettings;
    private Button btnStopUiautomator;
    private Button btnStopAtxAgent;
    private Button btnCoorLocate;
    private Button btnRecord;
    private Button btnRecordSetting;
    private TextView tvIP;

    private WindowManager mWindowManager;
    private WindowManager.LayoutParams param;
    private FloatView mLayout;
    private CoorHandler mHandler;

    private MediaProjectionManager mMediaProjectionManager;
    private RecorderManager mRecorderManager;

    private static final int REQUEST_MEDIA_PROJECTION = 1;
    private static final int REQUEST_PERMISSIONS = 2;


    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            Log.i(TAG, "service connected");
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.i(TAG, "service disconnected");

            // restart service
            Intent intent = new Intent(MainActivity.this, Service.class);
            startService(intent);
//            bindService(intent, connection, BIND_IMPORTANT | BIND_AUTO_CREATE);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent serviceIntent = new Intent(this, Service.class);
        startService(serviceIntent);
        bindService(serviceIntent, connection, BIND_IMPORTANT | BIND_AUTO_CREATE);
        initView();
        Intent intent = getIntent();
        boolean isHide = intent.getBooleanExtra("hide", false);
        if (isHide) {
            Log.i(TAG, "launch args hide:true, move to background");
            moveTaskToBack(true);
        }
        handleMessage();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mMediaProjectionManager = (MediaProjectionManager) getApplicationContext().getSystemService(MEDIA_PROJECTION_SERVICE);
            mRecorderManager = new RecorderManager(this);
        }
    }

    private void startServer(Handler mHandler) {
        try {
            CoordinateServer mServer = new CoordinateServer(9000, mHandler);
            mServer.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleMessage() {
        mHandler = new CoorHandler(mLayout);
        startServer(mHandler);
    }

    private void initView() {
        btnFinish = (Button) findViewById(R.id.btn_finish);
        btnIdentify = (Button) findViewById(R.id.btn_identify);
        btnAccessibility = ((Button) findViewById(R.id.accessibility));
        btnDevelopmentSettings = ((Button) findViewById(R.id.development_settings));
        btnStopUiautomator = ((Button) findViewById(R.id.stop_uiautomator));
        btnStopAtxAgent = ((Button) findViewById(R.id.stop_atx_agent));
        btnCoorLocate = (Button) findViewById(R.id.btn_coor_locate);
        btnRecord = (Button) findViewById(R.id.btn_screen_record);
        btnRecordSetting = (Button) findViewById(R.id.btn_record_setting);
        btnFinish.setOnClickListener(this);
        btnIdentify.setOnClickListener(this);
        btnAccessibility.setOnClickListener(this);
        btnDevelopmentSettings.setOnClickListener(this);
        btnStopUiautomator.setOnClickListener(this);
        btnStopAtxAgent.setOnClickListener(this);
        btnCoorLocate.setOnClickListener(this);
        btnRecord.setOnClickListener(this);
        btnRecordSetting.setOnClickListener(this);

        tvIP = (TextView) findViewById(R.id.ip_address);
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        int ip = wifiManager.getConnectionInfo().getIpAddress();
        String ipStr = (ip & 0xFF) + "." + ((ip >> 8) & 0xFF) + "." + ((ip >> 16) & 0xFF) + "." + ((ip >> 24) & 0xFF);
        tvIP.setText("IP: " + ipStr);
        tvIP.setTextColor(Color.BLUE);

        mWindowManager=(WindowManager)getApplicationContext().getSystemService(Context.WINDOW_SERVICE);     //获取WindowManager
        param = ((UApplication)getApplication()).getMywmParams();
        param.type= WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;     // 系统提示类型,重要
        param.format=1;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            param.flags = WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS |
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS |
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH |
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL; // 不能抢占聚焦点
        }else {
            param.flags = WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS |
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH |
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL; // 不能抢占聚焦点
        }
        param.alpha = 1.0f;
        param.gravity= Gravity.LEFT| Gravity.TOP;   //调整悬浮窗口至左上角
        param.systemUiVisibility = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
    }

    private void showFloatView() {
        if (mLayout == null){
            mLayout=new FloatView(getApplicationContext());
            mLayout.setFocusable(false);
        }
        mHandler.updateFloatView(mLayout);
        mWindowManager.addView(mLayout, param);
        btnCoorLocate.setText(getString(R.string.stop_coor_location));
    }
    private void closeFloatView(){
        if (mLayout.isShown()){
            mWindowManager.removeView(mLayout);
            btnCoorLocate.setText(getString(R.string.coor_switch));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(mLayout != null && mLayout.isShown()){
            btnCoorLocate.setText(R.string.stop_coor_location);
        }else {
            btnCoorLocate.setText(getString(R.string.coor_switch));
        }
        if (mRecorderManager.mRecorder != null) {
            btnRecord.setText(getString(R.string.stop_record));
        }else{
            btnRecord.setText(getString(R.string.screen_record));
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mRecorderManager.mRecorder != null) {
            mRecorderManager.stopRecorder();
        }
        if (mLayout != null && mLayout.isShown()){
            mWindowManager.removeView(mLayout);
        }
        System.out.println("activity is destroy ------------------");
    }

    @TargetApi(M)
    private void requestPermissions() {
        final String[] permissions = new String[]{WRITE_EXTERNAL_STORAGE, RECORD_AUDIO};
        boolean showRationale = false;
        for (String perm : permissions) {
            showRationale |= shouldShowRequestPermissionRationale(perm);
        }
        if (!showRationale) {
            requestPermissions(permissions, REQUEST_PERMISSIONS);
            return;
        }
        new AlertDialog.Builder(this)
                .setMessage("Using your mic to record audio and your sd card to save video file")
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        MainActivity.this.requestPermissions(permissions, REQUEST_PERMISSIONS);
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .create()
                .show();
    }

    @Override
    public void onClick(View view) {
        Request request;
        Intent intent;
        switch (view.getId()){
            case R.id.btn_finish:
                unbindService(connection);
                stopService(new Intent(MainActivity.this, Service.class));
                finish();
                break;
            case R.id.btn_identify:
                intent = new Intent(MainActivity.this, IdentifyActivity.class);
                Bundle bundle = new Bundle();
                bundle.putString("theme", "RED");
                intent.putExtras(bundle);
                startActivity(intent);
                break;
            case R.id.accessibility:
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
                break;
            case R.id.development_settings:
                startActivity(new Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS));
                break;
            case R.id.stop_uiautomator:
                request = new Request.Builder()
                        .url("http://127.0.0.1:7912/uiautomator")
                        .delete()
                        .build();
                new OkHttpClient().newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        e.printStackTrace();
                        Looper.prepare();
                        Toast.makeText(MainActivity.this, "Uiautomator already stopped ", Toast.LENGTH_SHORT).show();
                        Looper.loop();
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        Looper.prepare();
                        Toast.makeText(MainActivity.this, "Uiautomator stopped", Toast.LENGTH_SHORT).show();
                        Looper.loop();
                    }
                });
                break;
            case R.id.stop_atx_agent:
                request = new Request.Builder()
                        .url("http://127.0.0.1:7912/stop")
                        .get()
                        .build();
                new OkHttpClient().newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        e.printStackTrace();
                        Looper.prepare();
                        Toast.makeText(MainActivity.this, "server already stopped", Toast.LENGTH_SHORT).show();
                        Looper.loop();
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        Looper.prepare();
                        Toast.makeText(MainActivity.this, "server stopped", Toast.LENGTH_SHORT).show();
                        Looper.loop();
                    }
                });
                break;
            case R.id.btn_coor_locate:
                if (mLayout == null || !mLayout.isShown()){
                    showFloatView();
                }else {
                    closeFloatView();
                }
                break;
            case R.id.btn_screen_record:
                if (mRecorderManager.mRecorder != null) {
                    mRecorderManager.stopRecorder();
                    btnRecord.setText(getString(R.string.screen_record));
                } else if (hasPermissions()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        startCaptureIntent();
                        btnRecord.setText(getString(R.string.stop_record));
                    }
                } else if (Build.VERSION.SDK_INT >= M) {
                    requestPermissions();
                } else {
                    Toast.makeText(MainActivity.this, R.string.no_permission_tip, Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.btn_record_setting:
                Toast.makeText(this, "功能暂未实现！", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean hasPermissions() {
        PackageManager pm = getPackageManager();
        String packageName = getPackageName();
        int granted = pm.checkPermission(RECORD_AUDIO, packageName) | pm.checkPermission(WRITE_EXTERNAL_STORAGE, packageName);
        return granted == PackageManager.PERMISSION_GRANTED;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void startCaptureIntent() {
        Intent captureIntent = null;
        captureIntent = mMediaProjectionManager.createScreenCaptureIntent();
        startActivityForResult(captureIntent, REQUEST_MEDIA_PROJECTION);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQUEST_PERMISSIONS) {
            int granted = PackageManager.PERMISSION_GRANTED;
            for (int r : grantResults) {
                granted |= r;
            }
            if (granted == PackageManager.PERMISSION_GRANTED) {
                startCaptureIntent();
            } else {
                Toast.makeText(this, "No Permission!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_MEDIA_PROJECTION) {
            MediaProjection mediaProjection = null;
            mediaProjection = mMediaProjectionManager.getMediaProjection(resultCode, data);
            if (mediaProjection == null) {
                Log.e("@@", "media projection is null");
                return;
            }
            mRecorderManager.recorderResult(mediaProjection);
        }
    }

}
