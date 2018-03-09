package com.github.uiautomator.stub;

import android.app.UiAutomation;
import android.text.TextUtils;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Button;

import java.util.HashMap;
import java.util.List;

/**
 * Used to skip apk auto install && permission popups
 * Called in method: setPermissionPatterns
 * <p>
 * Created by hzsunshx on 2018/3/7.
 */

public class EventListener implements UiAutomation.OnAccessibilityEventListener {
    //    private String[] buttonTexts; // For example: "确定", "安装", "继续安装", "下一步", "完成"
    private HashMap<String, String[]> patterns;

    public EventListener(HashMap<String, String[]> patterns) {
        this.patterns = patterns;
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        Log.d("Accessibility event " + event.toString());
        Log.d("Package name " + event.getPackageName());
        for (String packageName : this.patterns.keySet()) {
            if (event.getPackageName().equals(packageName)) {
                String[] labels = patterns.get(packageName);
                boolean r = performInstallation(event, labels);
                Log.d("Action perform: " + TextUtils.join(", ", labels) + " " + r);
            }
        }
    }

    private boolean performInstallation(AccessibilityEvent event, String[] texts) {
        List<AccessibilityNodeInfo> nodeInfoList;
        for (String text : texts) {
            nodeInfoList = event.getSource().findAccessibilityNodeInfosByText(text);
            // Note: findAccessibilityNodeInfosByText will return all node which contains text
            if (nodeInfoList != null && !nodeInfoList.isEmpty()) {
                boolean performed = performClick(nodeInfoList, text, Button.class.getName());
                if (performed) return true;
            }
        }
        return false;
    }

    private boolean performClick(List<AccessibilityNodeInfo> nodeInfoList, String text, String widgetType) {
        for (AccessibilityNodeInfo node : nodeInfoList) {
            Log.d("travel node: " + node);
            if (node.isClickable() && node.isEnabled() && text.equals(node.getText())) {
                if (widgetType == null || node.getClassName().equals(widgetType)) {
                    Log.d("click: " + node.getText());
                    return node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                }
            }
        }
        return false;
    }
}
