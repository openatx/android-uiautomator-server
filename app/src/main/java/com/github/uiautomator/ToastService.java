package com.github.uiautomator;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import java.util.Timer;
import java.util.TimerTask;

public class ToastService {

    private static final String TAG = "ToastService";

    WindowManager windowManager;
    private View toastView;
    private WindowManager.LayoutParams params;
    private int duration;
    private Timer timer;
    private ToastService(Context context, String text, int duration){
        this.duration = duration;
        timer = new Timer();
        Toast toast = Toast.makeText(context, text, Toast.LENGTH_LONG);
        windowManager = (WindowManager)context.getSystemService(Context.WINDOW_SERVICE);
        toastView = toast.getView();
        params = new WindowManager.LayoutParams();
        params.height = WindowManager.LayoutParams.WRAP_CONTENT;
        params.width = WindowManager.LayoutParams.WRAP_CONTENT;
        params.format = PixelFormat.TRANSLUCENT;
        params.windowAnimations = toast.getView().getAnimation().INFINITE;
        params.type = WindowManager.LayoutParams.TYPE_TOAST;
        params.setTitle("Toast");
        params.flags = WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
        params.gravity = Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM;
        params.verticalMargin = 0.2f;
        params.alpha = 0.6f;
        params.y = -30;
    }


    public static ToastService makeText(Context context, String text, int duration){
        return new ToastService(context, text, duration);
    }

    public void show(){
        windowManager.addView(toastView, params);
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                windowManager.removeView(toastView);
            }
        }, 1000 * duration);
    }

    public void cancel(){
        windowManager.removeView(toastView);
        timer.cancel();
    }

}
