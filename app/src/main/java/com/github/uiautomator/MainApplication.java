package com.github.uiautomator;

import android.app.Application;
import android.util.Log;

import com.tendcloud.tenddata.TCAgent;

public class MainApplication extends Application {
    private static final String TAG = ">>>>>MainApplication";
    @Override 
    public void onCreate() {
        super.onCreate();
//        JLibrary.InitEntry(this); //移动安全联盟统一SDK初始化
        TCAgent.LOG_ON = true;
        // App ID: 在TalkingData创建应用后，进入数据报表页中，在“系统设置”-“编辑应用”页面里查看App ID。  
        // 渠道 ID: 是渠道标识符，可通过不同渠道单独追踪数据。
        TCAgent.init(this, BuildConfig.buildTendId, BuildConfig.buildChannel);
        // 如果已经在AndroidManifest.xml配置了App ID和渠道ID，调用TCAgent.init(this)即可；或与AndroidManifest.xml中的对应参数保持一致。
        TCAgent.setReportUncaughtExceptions(true);
        Log.e(TAG, "TCAgent init over>>>>>");
    }
}