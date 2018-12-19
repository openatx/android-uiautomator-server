package com.github.uiautomator.monitor;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import com.github.uiautomator.R;
import com.github.uiautomator.Service;

/**
 * Created by hzsunshx on 2018/1/16.
 */

public class WifiMonitor extends AbstractMonitor {
    private static final String TAG = "WifiMonitor";
    private BroadcastReceiver receiver;

    public WifiMonitor(Context context, HttpPostNotifier notifier) {
        super(context, notifier);
    }

    @Override
    public void register() {
        Log.i(TAG, "Wifi monitor starting");
        this.receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(intent.getAction())) {// 监听wifi的打开与关闭，与wifi的连接无关
                    int wifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, 0);
                    switch (wifiState) {
                        case WifiManager.WIFI_STATE_DISABLED:
                        case WifiManager.WIFI_STATE_DISABLING:
                            report(notifier, "wifi");
                            report(notifier, new WifiInfos(false, "").toString());
                            break;
                    }
                }
                // 监听网络连接，包括wifi和移动数据的打开和关闭,以及连接上可用的连接都会接到监听
                if (ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())) {
                    //获取联网状态的NetworkInfo对象
                    NetworkInfo info = intent.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);
                    if (info != null) {
                        //如果当前的网络连接成功并且网络连接可用
                        if (NetworkInfo.State.CONNECTED == info.getState() && info.isAvailable()) {
                            WifiManager wifi = (WifiManager)((Service) context).getSystemService(Context.WIFI_SERVICE);
                            WifiInfo wInfo = wifi.getConnectionInfo();
                            report(notifier, new WifiInfos(true, wInfo.getSSID()).toString());
                        }
                    }
                }
                WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
                int ip = wifiManager.getConnectionInfo().getIpAddress();
                String ipStr = (ip & 0xFF) + "." + ((ip >> 8) & 0xFF) + "." + ((ip >> 16) & 0xFF) + "." + ((ip >> 24) & 0xFF);
                ((Service) context).setNotificationContentText(context.getString(R.string.monitor_service_text) + " on " + ipStr);
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        context.registerReceiver(receiver, filter);
    }

    @Override
    public void unregister() {
        if (receiver != null) {
            context.unregisterReceiver(receiver);
        }
    }

    private void report(HttpPostNotifier notifier, String content) {
        notifier.Notify("/info/wifi", content);
    }

    class WifiInfos{

        private boolean wifiStatus = false;
        private String ssid = "";

        public WifiInfos(boolean wifiStatus, String ssid){
            this.wifiStatus = wifiStatus;
            this.ssid = ssid;
        }

        @Override
        public String toString() {
            return "{wifiStatus:" + wifiStatus +
                    ",ssid:" + ssid + "}";
        }
    }

}
