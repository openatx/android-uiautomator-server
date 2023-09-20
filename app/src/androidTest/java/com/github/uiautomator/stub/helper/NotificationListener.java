package com.github.uiautomator.stub.helper;

import android.app.UiAutomation;
import android.view.accessibility.AccessibilityEvent;
import com.github.uiautomator.stub.Device;
import java.util.ArrayList;
import java.util.List;
import static java.lang.System.currentTimeMillis;

/**
 * 监听toast信息
 */
public final class NotificationListener {
    private static List<CharSequence> toastMessages = new ArrayList<CharSequence>();
    private final static NotificationListener INSTANCE = new NotificationListener();
    private Thread toastThread = null;
    private boolean stopLooping = false;

    private NotificationListener(){

    }

    public static NotificationListener getInstance(){
        return INSTANCE;
    }

    /**
     * Listens for Notification Messages
     */
    public void start(){
        if(toastThread == null){
            toastThread = new Thread(new Listener());
            toastThread.setDaemon(true);
            toastThread.start();
            stopLooping = false;
        }
    }

    public void stop(){
        stopLooping = true;
        try{
            if(toastThread.isAlive()){
                toastThread.stop();
            }
        }catch (Exception e){

        }
        toastThread = null;
    }

    public List<CharSequence> getToastMSGs() {
        List<CharSequence> result = new ArrayList<CharSequence>();
        result.addAll(toastMessages);
        toastMessages.clear();
        return result;
    }

    private class Listener implements Runnable{
        @Override
        public void run() {
            while (!stopLooping) {
                AccessibilityEvent accessibilityEvent = null;
                //return true if the AccessibilityEvent type is NOTIFICATION type
                UiAutomation.AccessibilityEventFilter eventFilter = new UiAutomation.AccessibilityEventFilter() {
                    @Override
                    public boolean accept(AccessibilityEvent event) {
                        return event.getEventType() == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED;
                    }
                };
                Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        // Not performing any event.
                    }
                };
                try {
                    //wait for AccessibilityEvent filter
                    accessibilityEvent = Device.getInstance().getUiAutomation()
                            .executeAndWaitForEvent(runnable /*executable event*/, eventFilter /* event to filter*/, 500 /*time out in ms*/);
                } catch (Exception ignore) {}

                if (accessibilityEvent != null) {
                    toastMessages.addAll(accessibilityEvent.getText());
                }
            }
        }
    }
}
