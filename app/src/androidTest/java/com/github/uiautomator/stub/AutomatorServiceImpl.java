/*
 * The MIT License (MIT)
 * Copyright (c) 2015 xiaocong@gmail.com
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE
 * OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.github.uiautomator.stub;

import android.app.UiAutomation;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.os.SystemClock;
import android.support.test.InstrumentationRegistry;
import android.support.test.uiautomator.Configurator;
import android.support.test.uiautomator.Direction;
import android.support.test.uiautomator.StaleObjectException;
import android.support.test.uiautomator.UiCollection;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObject;
import android.support.test.uiautomator.UiObject2;
import android.support.test.uiautomator.UiObjectNotFoundException;
import android.support.test.uiautomator.UiScrollable;
import android.support.test.uiautomator.UiSelector;
import android.support.test.uiautomator.Until;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;

import com.github.uiautomator.ToastHelper;
import com.github.uiautomator.stub.watcher.ClickUiObjectWatcher;
import com.github.uiautomator.stub.watcher.PressKeysWatcher;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;


public class AutomatorServiceImpl implements AutomatorService {

    private final HashSet<String> watchers = new HashSet<String>();
    private final ConcurrentHashMap<String, UiObject> uiObjects = new ConcurrentHashMap<String, UiObject>();
    private SoundPool soundPool = new SoundPool(100, AudioManager.STREAM_MUSIC,0);


    private UiDevice device;
    private UiAutomation uiAutomation;

    public AutomatorServiceImpl() {
        this.uiAutomation = InstrumentationRegistry.getInstrumentation().getUiAutomation();
        this.device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());


        // 音频加载完播放
        soundPool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
            @Override
            public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
                soundPool.play(sampleId,1,1,1,0,1);
            }
        });

        // Reset Configurator Wait Timeout
        Configurator configurator = Configurator.getInstance();
        configurator.setWaitForSelectorTimeout(0L); // Default 10000
        configurator.setWaitForIdleTimeout(0L); // Default 10000
        configurator.setActionAcknowledgmentTimeout(500); // Default 3000
        configurator.setScrollAcknowledgmentTimeout(200); // Default 200
        configurator.setKeyInjectionDelay(0); // Default 0

        // default uiAutomation serviceInfo.eventTypes is -1
        // I guess this might be watch all eventTypes
        uiAutomation.setOnAccessibilityEventListener(new AccessibilityEventListener(device, watchers));
    }

    /**
     * It's to play a section music to test
     * @return
     */
    @Override
    public boolean playSound(String path){
        try {
            soundPool.load(path, 1);
        } catch (Exception e)
        {
            return false;
        }
        return true;
    }

    /**
     * It's to test if the service is alive.
     *
     * @return 'pong'
     */
    @Override
    public String ping() {
        return "pong";
    }

    /**
     * Get the device info.
     *
     * @return device info.
     */
    @Override
    public DeviceInfo deviceInfo() {
        return DeviceInfo.getDeviceInfo();
    }

    /**
     * Trigger all watchers when TYPE_WINDOW_STATE_CHANGED or TYPE_WINDOW_CONTENT_CHANGED
     */
    @Override
    public void runWatchersOnWindowsChange(boolean enabled) {
        // Important: UiAutomator2 is based on accessibility. so we should keep the old eventTypes.
        // Default eventTypes contains many ...
        // and keep serviceInfo.packageNames unchanged.
        AccessibilityEventListener.getInstance().triggerWatchers = enabled;
    }

    @Override
    public boolean hasWatchedOnWindowsChange() {
        return AccessibilityEventListener.getInstance().triggerWatchers;
    }

    @Override
    public boolean makeToast(final String text, final int duration) {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                ToastHelper.makeText(InstrumentationRegistry.getTargetContext(), text, duration).show();
            }
        });
        return true;
    }

    @Override
    public String getLastToast(long cacheDuration) {
        AccessibilityEventListener instance = AccessibilityEventListener.getInstance();
        if (System.currentTimeMillis() < cacheDuration + instance.toastTime) {
            return instance.toastMessage;
        }
        return null;
    }

    @Override
    public boolean clearLastToast() {
        AccessibilityEventListener.getInstance().toastMessage = null;
        return true;
    }

    /**
     * Perform a click at arbitrary coordinates specified by the user.
     *
     * @param x coordinate
     * @param y coordinate
     * @return true if the click succeeded else false
     */
    @Override
    public boolean click(int x, int y) {
        return device.click(x, y);
    }

    /**
     * Performs a swipe from one coordinate to another coordinate. You can control the smoothness and speed of the swipe by specifying the number of steps. Each step execution is throttled to 5 milliseconds per step, so for a 100 steps, the swipe will take around 0.5 seconds to complete.
     *
     * @param startX X-axis value for the starting coordinate
     * @param startY Y-axis value for the starting coordinate
     * @param endX   X-axis value for the ending coordinate
     * @param endY   Y-axis value for the ending coordinate
     * @param steps  is the number of steps for the swipe action
     * @return true if swipe is performed, false if the operation fails or the coordinates are invalid
     * @throws com.github.uiautomator.stub.NotImplementedException
     */
    @Override
    public boolean drag(int startX, int startY, int endX, int endY, int steps) throws NotImplementedException {
        return device.drag(startX, startY, endX, endY, steps);
    }

    /**
     * Performs a swipe from one coordinate to another using the number of steps to determine smoothness and speed. Each step execution is throttled to 5ms per step. So for a 100 steps, the swipe will take about 1/2 second to complete.
     *
     * @param startX X-axis value for the starting coordinate
     * @param startY Y-axis value for the starting coordinate
     * @param endX   X-axis value for the ending coordinate
     * @param endY   Y-axis value for the ending coordinate
     * @param steps  is the number of move steps sent to the system
     * @return false if the operation fails or the coordinates are invalid
     */
    @Override
    public boolean swipe(int startX, int startY, int endX, int endY, int steps) {
        return device.swipe(startX, startY, endX, endY, steps);
    }

    @Override
    public boolean swipePoints(int[] segments, int segmentSteps) {
        android.graphics.Point[] points = new android.graphics.Point[segments.length / 2];
        for (int i = 0; i < segments.length / 2; i++) {
            points[i] = new android.graphics.Point(segments[2 * i], segments[2 * i + 1]);
        }
        return device.swipe(points, segmentSteps);
    }

    // Multi touch is a little complicated
    @Override
    public boolean injectInputEvent(int action, float x, float y, int metaState) {
        MotionEvent e = MotionEvent.obtain(SystemClock.uptimeMillis(),
                SystemClock.uptimeMillis(),
                action, x, y, metaState);
        e.setSource(InputDevice.SOURCE_TOUCHSCREEN);
        boolean b = uiAutomation.injectInputEvent(e, true);
        e.recycle();
        return b;
    }

    /**
     * Helper method used for debugging to dump the current window's layout hierarchy. The file root location is /data/local/tmp
     *
     * @param compressed use compressed layout hierarchy or not using setCompressedLayoutHeirarchy method. Ignore the parameter in case the API level lt 18.
     * @param filename   the filename to be stored. @deprecated
     * @return the absolute path name of dumped file.
     */
    @Deprecated
    @Override
    public String dumpWindowHierarchy(boolean compressed, String filename) {
        return dumpWindowHierarchy(compressed);
    }

    /**
     * Helper method used for debugging to dump the current window's layout hierarchy.
     *
     * @param compressed use compressed layout hierarchy or not using setCompressedLayoutHeirarchy method. Ignore the parameter in case the API level lt 18.
     * @return the absolute path name of dumped file.
     */
    @Override
    public String dumpWindowHierarchy(boolean compressed) {
        device.setCompressedLayoutHeirarchy(compressed);
        try {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            AccessibilityNodeInfoDumper.dumpWindowHierarchy(device, os);
//            device.dumpWindowHierarchy(os);
            os.close();
            return os.toString("UTF-8");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Take a screenshot of current window and store it as PNG The screenshot is adjusted per screen rotation
     *
     * @param filename where the PNG should be written to
     * @param scale    scale the screenshot down if needed; 1.0f for original size
     * @param quality  quality of the PNG compression; range: 0-100
     * @return the file name of the screenshot. null if failed.
     * @throws com.github.uiautomator.stub.NotImplementedException
     */
    @Override
    public String takeScreenshot(String filename, float scale, int quality) throws NotImplementedException {
        File f = new File(InstrumentationRegistry.getTargetContext().getFilesDir(), filename);
        device.takeScreenshot(f, scale, quality);
        if (f.exists()) return f.getAbsolutePath();
        return null;
    }

    /**
     * Disables the sensors and freezes the device rotation at its current rotation state, or enable it.
     *
     * @param freeze true to freeze the rotation, false to unfreeze the rotation.
     * @throws android.os.RemoteException
     */
    @Override
    public void freezeRotation(boolean freeze) throws RemoteException {
        if (freeze) device.freezeRotation();
        else device.unfreezeRotation();
    }

    /**
     * Simulates orienting the device to the left/right/natural and also freezes rotation by disabling the sensors.
     *
     * @param dir Left or l, Right or r, Natural or n, case insensitive
     * @throws android.os.RemoteException
     * @throws com.github.uiautomator.stub.NotImplementedException
     */
    @Override
    public void setOrientation(String dir) throws RemoteException, NotImplementedException {
        dir = dir.toLowerCase();
        if ("left".equals(dir) || "l".equals(dir)) device.setOrientationLeft();
        else if ("right".equals(dir) || "r".equals(dir)) device.setOrientationRight();
        else if ("natural".equals(dir) || "n".equals(dir)) device.setOrientationNatural();
    }

    /**
     * Retrieves the text from the last UI traversal event received.
     *
     * @return the text from the last UI traversal event received.
     */
    @Override
    public String getLastTraversedText() {
        return device.getLastTraversedText();
    }

    /**
     * Clears the text from the last UI traversal event.
     */
    @Override
    public void clearLastTraversedText() {
        device.clearLastTraversedText();
    }

    /**
     * Opens the notification shade.
     *
     * @return true if successful, else return false
     * @throws com.github.uiautomator.stub.NotImplementedException
     */
    @Override
    public boolean openNotification() throws NotImplementedException {
        return device.openNotification();
    }

    /**
     * Opens the Quick Settings shade.
     *
     * @return true if successful, else return false
     * @throws com.github.uiautomator.stub.NotImplementedException
     */
    @Override
    public boolean openQuickSettings() throws NotImplementedException {
        return device.openQuickSettings();
    }

    /**
     * Checks if a specific registered UiWatcher has triggered. See registerWatcher(String, UiWatcher). If a UiWatcher runs and its checkForCondition() call returned true, then the UiWatcher is considered triggered. This is helpful if a watcher is detecting errors from ANR or crash dialogs and the test needs to know if a UiWatcher has been triggered.
     *
     * @param watcherName the name of registered watcher.
     * @return true if triggered else false
     */
    @Override
    public boolean hasWatcherTriggered(String watcherName) {
        return device.hasWatcherTriggered(watcherName);
    }

    /**
     * Checks if any registered UiWatcher have triggered.
     *
     * @return true if any UiWatcher have triggered else false.
     */
    @Override
    public boolean hasAnyWatcherTriggered() {
        return device.hasAnyWatcherTriggered();
    }

    /**
     * Register a ClickUiObjectWatcher
     *
     * @param name       Watcher name
     * @param conditions If all UiObject in the conditions match, the watcher should be triggered.
     * @param target     The target UiObject should be clicked if all conditions match.
     */
    @Override
    public void registerClickUiObjectWatcher(String name, Selector[] conditions, Selector target) {
        synchronized (watchers) {
            if (watchers.contains(name)) {
                device.removeWatcher(name);
                watchers.remove(name);
            }

            UiSelector[] selectors = new UiSelector[conditions.length];
            for (int i = 0; i < conditions.length; i++) {
                selectors[i] = conditions[i].toUiSelector();
            }
            device.registerWatcher(name, new ClickUiObjectWatcher(selectors, target.toUiSelector()));
            watchers.add(name);
        }
    }

    /**
     * Register a PressKeysWatcher
     *
     * @param name       Watcher name
     * @param conditions If all UiObject in the conditions match, the watcher should be triggered.
     * @param keys       All keys will be pressed in sequence.
     */
    @Override
    public void registerPressKeyskWatcher(String name, Selector[] conditions, String[] keys) {
        synchronized (watchers) {
            if (watchers.contains(name)) {
                device.removeWatcher(name);
                watchers.remove(name);
            }

            UiSelector[] selectors = new UiSelector[conditions.length];
            for (int i = 0; i < conditions.length; i++) {
                selectors[i] = conditions[i].toUiSelector();
            }
            device.registerWatcher(name, new PressKeysWatcher(selectors, keys));
            watchers.add(name);
        }
    }

    /**
     * Removes a previously registered UiWatcher.
     *
     * @param name Watcher name
     */
    @Override
    public void removeWatcher(String name) {
        synchronized (watchers) {
            if (watchers.contains(name)) {
                device.removeWatcher(name);
                watchers.remove(name);
            }
        }
    }

    /**
     * Resets a UiWatcher that has been triggered. If a UiWatcher runs and its checkForCondition() call returned true, then the UiWatcher is considered triggered.
     */
    @Override
    public void resetWatcherTriggers() {
        device.resetWatcherTriggers();
    }

    /**
     * Force to run all watchers.
     */
    @Override
    public void runWatchers() {
        device.runWatchers();
    }

    /**
     * Get all registered UiWatchers
     *
     * @return UiWatcher names
     */
    @Override
    public String[] getWatchers() {
        synchronized (watchers) {
            return watchers.toArray(new String[watchers.size()]);
        }
    }

    /**
     * Simulates a short press using key name.
     *
     * @param key possible key name is home, back, left, right, up, down, center, menu, search, enter, delete(or del), recent(recent apps), volume_up, volume_down, volume_mute, camera, power
     * @return true if successful, else return false
     * @throws android.os.RemoteException
     */
    @Override
    public boolean pressKey(String key) throws RemoteException {
        boolean result;
        key = key.toLowerCase();
        if ("home".equals(key)) result = device.pressHome();
        else if ("back".equals(key)) result = device.pressBack();
        else if ("left".equals(key)) result = device.pressDPadLeft();
        else if ("right".equals(key)) result = device.pressDPadRight();
        else if ("up".equals(key)) result = device.pressDPadUp();
        else if ("down".equals(key)) result = device.pressDPadDown();
        else if ("center".equals(key)) result = device.pressDPadCenter();
        else if ("menu".equals(key)) result = device.pressMenu();
        else if ("search".equals(key)) result = device.pressSearch();
        else if ("enter".equals(key)) result = device.pressEnter();
        else if ("delete".equals(key) || "del".equals(key)) result = device.pressDelete();
        else if ("recent".equals(key)) result = device.pressRecentApps();
        else if ("volume_up".equals(key)) result = device.pressKeyCode(KeyEvent.KEYCODE_VOLUME_UP);
        else if ("volume_down".equals(key))
            result = device.pressKeyCode(KeyEvent.KEYCODE_VOLUME_DOWN);
        else if ("volume_mute".equals(key))
            result = device.pressKeyCode(KeyEvent.KEYCODE_VOLUME_MUTE);
        else if ("camera".equals(key)) result = device.pressKeyCode(KeyEvent.KEYCODE_CAMERA);
        else result = "power".equals(key) && device.pressKeyCode(KeyEvent.KEYCODE_POWER);

        return result;
    }

    /**
     * Simulates a short press using a key code. See KeyEvent.
     *
     * @param keyCode the key code of the event.
     * @return true if successful, else return false
     */
    @Override
    public boolean pressKeyCode(int keyCode) {
        return device.pressKeyCode(keyCode);
    }

    /**
     * Simulates a short press using a key code. See KeyEvent.
     *
     * @param keyCode   the key code of the event.
     * @param metaState an integer in which each bit set to 1 represents a pressed meta key
     * @return true if successful, else return false
     */
    @Override
    public boolean pressKeyCode(int keyCode, int metaState) {
        return device.pressKeyCode(keyCode, metaState);
    }

    /**
     * This method simulates pressing the power button if the screen is OFF else it does nothing if the screen is already ON. If the screen was OFF and it just got turned ON, this method will insert a 500ms delay to allow the device time to wake up and accept input.
     *
     * @throws android.os.RemoteException
     */
    @Override
    public void wakeUp() throws RemoteException {
        device.wakeUp();
    }

    /**
     * This method simply presses the power button if the screen is ON else it does nothing if the screen is already OFF.
     *
     * @throws android.os.RemoteException
     */
    @Override
    public void sleep() throws RemoteException {
        device.sleep();
    }

    /**
     * Checks the power manager if the screen is ON.
     *
     * @return true if the screen is ON else false
     * @throws android.os.RemoteException
     */
    @Override
    public boolean isScreenOn() throws RemoteException {
        return device.isScreenOn();
    }

    /**
     * Waits for the current application to idle.
     *
     * @param timeout in milliseconds
     */
    @Override
    public void waitForIdle(long timeout) {
        device.waitForIdle(timeout);
    }

    /**
     * Waits for a window content update event to occur. If a package name for the window is specified, but the current window does not have the same package name, the function returns immediately.
     *
     * @param packageName the specified window package name (can be null). If null, a window update from any front-end window will end the wait.
     * @param timeout     the timeout for the wait
     * @return true if a window update occurred, false if timeout has elapsed or if the current window does not have the specified package name
     */
    @Override
    public boolean waitForWindowUpdate(String packageName, long timeout) {
        return device.waitForWindowUpdate(packageName, timeout);
    }

    /**
     * Clears the existing text contents in an editable field. The UiSelector of this object must reference a UI element that is editable. When you call this method, the method first sets focus at the start edge of the field. The method then simulates a long-press to select the existing text, and deletes the selected text. If a "Select-All" option is displayed, the method will automatically attempt to use it to ensure full text selection. Note that it is possible that not all the text in the field is selected; for example, if the text contains separators such as spaces, slashes, at symbol etc. Also, not all editable fields support the long-press functionality.
     *
     * @param obj the selector of the UiObject.
     * @throws android.support.test.uiautomator.UiObjectNotFoundException
     */
    @Override
    public void clearTextField(Selector obj) throws UiObjectNotFoundException {
        try {
            obj.toUiObject2().clear();
        } catch (NullPointerException | StaleObjectException e) {
            device.findObject(obj.toUiSelector()).clearTextField();
        }

    }

    /**
     * Reads the text property of the UI element
     *
     * @param obj the selector of the UiObject.
     * @return text value of the current node represented by this UiObject
     * @throws android.support.test.uiautomator.UiObjectNotFoundException
     */
    @Override
    public String getText(Selector obj) throws UiObjectNotFoundException {
        if (obj.toUiObject2() == null) {
            return device.findObject(obj.toUiSelector()).getText();
        } else {
            return obj.toUiObject2().getText();
        }
    }

    /**
     * Sets the text in an editable field, after clearing the field's content. The UiSelector selector of this object must reference a UI element that is editable. When you call this method, the method first simulates a click() on editable field to set focus. The method then clears the field's contents and injects your specified text into the field. If you want to capture the original contents of the field, call getText() first. You can then modify the text and use this method to update the field.
     *
     * @param obj  the selector of the UiObject.
     * @param text string to set
     * @return true if operation is successful
     * @throws android.support.test.uiautomator.UiObjectNotFoundException
     */
    @Override
    public boolean setText(Selector obj, String text) throws UiObjectNotFoundException {
        try {
            obj.toUiObject2().click();
            obj.toUiObject2().setText(text);
            return true;
        } catch (NullPointerException | StaleObjectException e) {
            return device.findObject(obj.toUiSelector()).setText(text);
        }
    }

    /**
     * Performs a click at the center of the visible bounds of the UI element represented by this UiObject.
     *
     * @param obj the target ui object.
     * @return true id successful else false
     * @throws android.support.test.uiautomator.UiObjectNotFoundException
     */
    @Override
    public boolean click(Selector obj) throws UiObjectNotFoundException {
        if (obj.toUiObject2() == null) {
            return device.findObject(obj.toUiSelector()).click();
        } else {
            obj.toUiObject2().click();
            return true;
        }
    }

    /**
     * Clicks the bottom and right corner or top and left corner of the UI element
     *
     * @param obj    the target ui object.
     * @param corner "br"/"bottomright" means BottomRight, "tl"/"topleft" means TopLeft, "center" means Center.
     * @return true on success
     * @throws android.support.test.uiautomator.UiObjectNotFoundException
     */
    @Override
    public boolean click(Selector obj, String corner) throws UiObjectNotFoundException {
        return click(device.findObject(obj.toUiSelector()), corner);
    }

    private boolean click(UiObject obj, String corner) throws UiObjectNotFoundException {
        if (corner == null) corner = "center";
        corner = corner.toLowerCase();
        if ("br".equals(corner) || "bottomright".equals(corner)) return obj.clickBottomRight();
        else if ("tl".equals(corner) || "topleft".equals(corner)) return obj.clickTopLeft();
        else if ("c".equals(corner) || "center".equals(corner)) return obj.click();
        return false;
    }

    /**
     * Performs a click at the center of the visible bounds of the UI element represented by this UiObject and waits for window transitions. This method differ from click() only in that this method waits for a a new window transition as a result of the click. Some examples of a window transition:
     * - launching a new activity
     * - bringing up a pop-up menu
     * - bringing up a dialog
     *
     * @param obj     the target ui object.
     * @param timeout timeout before giving up on waiting for a new window
     * @return true if the event was triggered, else false
     * @throws android.support.test.uiautomator.UiObjectNotFoundException
     */
    @Override
    public boolean clickAndWaitForNewWindow(Selector obj, long timeout) throws UiObjectNotFoundException {
        if (obj.toUiObject2() == null) {
            return device.findObject(obj.toUiSelector()).clickAndWaitForNewWindow(timeout);
        } else {
            return obj.toUiObject2().clickAndWait(Until.newWindow(), timeout);
        }
    }

    /**
     * Long clicks the center of the visible bounds of the UI element
     *
     * @param obj the target ui object.
     * @return true if operation was successful
     * @throws android.support.test.uiautomator.UiObjectNotFoundException
     */
    @Override
    public boolean longClick(Selector obj) throws UiObjectNotFoundException {
        if (obj.toUiObject2() == null) {
            return device.findObject(obj.toUiSelector()).longClick();
        } else {
            obj.toUiObject2().longClick();
            return true;
        }
    }

    /**
     * Long clicks bottom and right corner of the UI element
     *
     * @param obj    the target ui object.
     * @param corner "br"/"bottomright" means BottomRight, "tl"/"topleft" means TopLeft, "center" means Center.
     * @return true if operation was successful
     * @throws android.support.test.uiautomator.UiObjectNotFoundException
     */
    @Override
    public boolean longClick(Selector obj, String corner) throws UiObjectNotFoundException {
        return longClick(device.findObject(obj.toUiSelector()), corner);
    }

    private boolean longClick(UiObject obj, String corner) throws UiObjectNotFoundException {
        if (corner == null) corner = "center";

        corner = corner.toLowerCase();
        if ("br".equals(corner) || "bottomright".equals(corner)) return obj.longClickBottomRight();
        else if ("tl".equals(corner) || "topleft".equals(corner)) return obj.longClickTopLeft();
        else if ("c".equals(corner) || "center".equals(corner)) return obj.longClick();

        return false;
    }

    /**
     * Drags this object to a destination UiObject. The number of steps specified in your input parameter can influence the drag speed, and varying speeds may impact the results. Consider evaluating different speeds when using this method in your tests.
     *
     * @param obj     the ui object to be dragged.
     * @param destObj the ui object to be dragged to.
     * @param steps   usually 40 steps. You can increase or decrease the steps to change the speed.
     * @return true if successful
     * @throws android.support.test.uiautomator.UiObjectNotFoundException
     * @throws com.github.uiautomator.stub.NotImplementedException
     */
    @Override
    public boolean dragTo(Selector obj, Selector destObj, int steps) throws UiObjectNotFoundException, NotImplementedException {
        return dragTo(device.findObject(obj.toUiSelector()), destObj, steps);
    }

    private boolean dragTo(UiObject obj, Selector destObj, int steps) throws UiObjectNotFoundException, NotImplementedException {
        return obj.dragTo(device.findObject(destObj.toUiSelector()), steps);
    }

    /**
     * Drags this object to arbitrary coordinates. The number of steps specified in your input parameter can influence the drag speed, and varying speeds may impact the results. Consider evaluating different speeds when using this method in your tests.
     *
     * @param obj   the ui object to be dragged.
     * @param destX the X-axis coordinate of destination.
     * @param destY the Y-axis coordinate of destination.
     * @param steps usually 40 steps. You can increase or decrease the steps to change the speed.
     * @return true if successful
     * @throws android.support.test.uiautomator.UiObjectNotFoundException
     * @throws com.github.uiautomator.stub.NotImplementedException
     */
    @Override
    public boolean dragTo(Selector obj, int destX, int destY, int steps) throws UiObjectNotFoundException, NotImplementedException {
        return dragTo(device.findObject(obj.toUiSelector()), destX, destY, steps);
    }

    private boolean dragTo(UiObject obj, int destX, int destY, int steps) throws UiObjectNotFoundException, NotImplementedException {
        return obj.dragTo(destX, destY, steps);
    }

    /**
     * Check if view exists. This methods performs a waitForExists(long) with zero timeout. This basically returns immediately whether the view represented by this UiObject exists or not.
     *
     * @param obj the ui object.
     * @return true if the view represented by this UiObject does exist
     */
    @Override
    public boolean exist(Selector obj) {
        if (obj.getChildOrSibling().length == 0 && obj.toBySelector() != null)
            return device.wait(Until.hasObject(obj.toBySelector()), 0L);
        return device.findObject(obj.toUiSelector()).exists();
    }

    /**
     * Get the object info.
     *
     * @param obj the target ui object.
     * @return object info.
     * @throws android.support.test.uiautomator.UiObjectNotFoundException
     */
    @Override
    public ObjInfo objInfo(Selector obj) throws UiObjectNotFoundException {
        try {
             final UiObject2 obj2 = obj.toUiObject2(); // to avoid a race condition
             if (obj2 != null) {
                 return ObjInfo.getObjInfo(obj2);
             }
        } catch(StaleObjectException e){
            Log.d("objInfo got StaleObjectException " + e);
            // HotFix(ssx): Here always raise StaleObjectException
            // Refs: https://github.com/openatx/uiautomator2/issues/138
        }
        return ObjInfo.getObjInfo(device.findObject(obj.toUiSelector()));
    }

    /**
     * Get the count of the UiObject instances by the selector
     *
     * @param obj the selector of the ui object
     * @return the count of instances.
     */
    @Override
    public int count(Selector obj) {
        if ((obj.deepSelector().getMask() & Selector.MASK_INSTANCE) > 0) {
            if (device.findObject(obj.toUiSelector()).exists()) return 1;
            else return 0;
        } else {
            UiSelector sel = obj.toUiSelector();
            if (!device.findObject(sel).exists()) return 0;
            int low = 1;
            int high = 2;

            // Note: can not use `sel = sel.instance(high -1)`
            // because this will change first selector in chain not last.
            sel = obj.toUiSelector(high - 1);
            while (device.findObject(sel).exists()) {
                low = high;
                high = high * 2;
                sel = obj.toUiSelector(high - 1);
            }
            while (high > low + 1) {
                int mid = (low + high) / 2;
                sel = obj.toUiSelector(mid - 1);
                if (device.findObject(sel).exists()) low = mid;
                else high = mid;
            }
            return low;
        }
    }

    /**
     * Get the info of all instance by the selector.
     *
     * @param obj the selector of ui object.
     * @return array of object info.
     */
    @Override
    public ObjInfo[] objInfoOfAllInstances(Selector obj) {
        int total = count(obj);
        ObjInfo objs[] = new ObjInfo[total];
        if ((obj.getMask() & Selector.MASK_INSTANCE) > 0 && total > 0) {
            try {
                objs[0] = objInfo(obj);
            } catch (UiObjectNotFoundException e) {
            }
        } else {
            UiSelector sel = obj.toUiSelector();
            for (int i = 0; i < total; i++) {
                try {
                    objs[i] = ObjInfo.getObjInfo(sel.instance(i));
                } catch (UiObjectNotFoundException e) {
                }
            }
        }
        return objs;
    }

    /**
     * Generates a two-pointer gesture with arbitrary starting and ending points.
     *
     * @param obj         the target ui object. ??
     * @param startPoint1 start point of pointer 1
     * @param startPoint2 start point of pointer 2
     * @param endPoint1   end point of pointer 1
     * @param endPoint2   end point of pointer 2
     * @param steps       the number of steps for the gesture. Steps are injected about 5 milliseconds apart, so 100 steps may take around 0.5 seconds to complete.
     * @return true if all touch events for this gesture are injected successfully, false otherwise
     * @throws android.support.test.uiautomator.UiObjectNotFoundException
     */
    @Override
    public boolean gesture(Selector obj, Point startPoint1, Point startPoint2, Point endPoint1, Point endPoint2, int steps) throws UiObjectNotFoundException, NotImplementedException {
        return gesture(device.findObject(obj.toUiSelector()), startPoint1, startPoint2, endPoint1, endPoint2, steps);
    }

    private boolean gesture(UiObject obj, Point startPoint1, Point startPoint2, Point endPoint1, Point endPoint2, int steps) throws UiObjectNotFoundException, NotImplementedException {
        return obj.performTwoPointerGesture(startPoint1.toPoint(), startPoint2.toPoint(), endPoint1.toPoint(), endPoint2.toPoint(), steps);
    }

    /**
     * Performs a two-pointer gesture, where each pointer moves diagonally toward the other, from the edges to the center of this UiObject .
     *
     * @param obj     the target ui object.
     * @param percent percentage of the object's diagonal length for the pinch gesture
     * @param steps   the number of steps for the gesture. Steps are injected about 5 milliseconds apart, so 100 steps may take around 0.5 seconds to complete.
     * @return true if all touch events for this gesture are injected successfully, false otherwise
     * @throws android.support.test.uiautomator.UiObjectNotFoundException
     * @throws com.github.uiautomator.stub.NotImplementedException
     */
    @Override
    public boolean pinchIn(Selector obj, int percent, int steps) throws UiObjectNotFoundException, NotImplementedException {
        return pinchIn(device.findObject(obj.toUiSelector()), percent, steps);
    }

    private boolean pinchIn(UiObject obj, int percent, int steps) throws UiObjectNotFoundException, NotImplementedException {
        return obj.pinchIn(percent, steps);
    }

    /**
     * Performs a two-pointer gesture, where each pointer moves diagonally opposite across the other, from the center out towards the edges of the this UiObject.
     *
     * @param obj     the target ui object.
     * @param percent percentage of the object's diagonal length for the pinch gesture
     * @param steps   the number of steps for the gesture. Steps are injected about 5 milliseconds apart, so 100 steps may take around 0.5 seconds to complete.
     * @return true if all touch events for this gesture are injected successfully, false otherwise
     * @throws android.support.test.uiautomator.UiObjectNotFoundException
     * @throws com.github.uiautomator.stub.NotImplementedException
     */
    @Override
    public boolean pinchOut(Selector obj, int percent, int steps) throws UiObjectNotFoundException, NotImplementedException {
        return pinchOut(device.findObject(obj.toUiSelector()), percent, steps);
    }

    private boolean pinchOut(UiObject obj, int percent, int steps) throws UiObjectNotFoundException, NotImplementedException {
        return obj.pinchOut(percent, steps);
    }

    /**
     * Performs the swipe up/down/left/right action on the UiObject
     *
     * @param obj   the target ui object.
     * @param dir   "u"/"up", "d"/"down", "l"/"left", "r"/"right"
     * @param steps indicates the number of injected move steps into the system. Steps are injected about 5ms apart. So a 100 steps may take about 1/2 second to complete.
     * @return true of successful
     * @throws android.support.test.uiautomator.UiObjectNotFoundException
     */
    @Override
    public boolean swipe(Selector obj, String dir, int steps) throws UiObjectNotFoundException {
        return swipe(device.findObject(obj.toUiSelector()), dir, steps);
    }

    private boolean swipe(UiObject item, String dir, int steps) throws UiObjectNotFoundException {
        dir = dir.toLowerCase();
        boolean result = false;
        if ("u".equals(dir) || "up".equals(dir)) result = item.swipeUp(steps);
        else if ("d".equals(dir) || "down".equals(dir)) result = item.swipeDown(steps);
        else if ("l".equals(dir) || "left".equals(dir)) result = item.swipeLeft(steps);
        else if ("r".equals(dir) || "right".equals(dir)) result = item.swipeRight(steps);
        return result;
    }

    /**
     * Performs the swipe up/down/left/right action on the UiObject
     *
     * @param obj     the target ui object.
     * @param dir     "u"/"up", "d"/"down", "l"/"left", "r"/"right"
     * @param percent expect value: percent >= 0.0F && percent <= 1.0F,The length of the swipe as a percentage of this object's size.
     * @param steps   indicates the number of injected move steps into the system. Steps are injected about 5ms apart. So a 100 steps may take about 1/2 second to complete.
     * @return true of successful
     * @throws android.support.test.uiautomator.UiObjectNotFoundException
     */
    @Override
    public boolean swipe(Selector obj, String dir, float percent, int steps) throws UiObjectNotFoundException {
        if (obj.toUiObject2() == null) {
            return swipe(device.findObject(obj.toUiSelector()), dir, steps);
        }
        return swipe(obj.toUiObject2(), dir, percent, steps);
    }

    private boolean swipe(UiObject2 item, String dir, float percent, int steps) throws UiObjectNotFoundException {
        dir = dir.toLowerCase();
        if ("u".equals(dir) || "up".equals(dir)) item.swipe(Direction.UP, percent, steps);
        else if ("d".equals(dir) || "down".equals(dir)) item.swipe(Direction.DOWN, percent, steps);
        else if ("l".equals(dir) || "left".equals(dir)) item.swipe(Direction.LEFT, percent, steps);
        else if ("r".equals(dir) || "right".equals(dir))
            item.swipe(Direction.RIGHT, percent, steps);
        return true;
    }

    /**
     * Waits a specified length of time for a view to become visible. This method waits until the view becomes visible on the display, or until the timeout has elapsed. You can use this method in situations where the content that you want to select is not immediately displayed.
     *
     * @param obj     the target ui object
     * @param timeout time to wait (in milliseconds)
     * @return true if the view is displayed, else false if timeout elapsed while waiting
     */
    @Override
    public boolean waitForExists(Selector obj, long timeout) {
        try {
            if (obj.getChildOrSibling().length == 0 && obj.checkBySelectorNull(obj) == false)
                return device.wait(Until.hasObject(obj.toBySelector()), timeout);
        } catch (ClassCastException e) {
            Log.d("waitForExists got ClassCastException "+ e);
            // Hotfix because of https://github.com/openatx/uiautomator2/issues/140
        }
        // https://developer.android.com/reference/android/support/test/uiautomator/UiObject#waitforexists
        return device.findObject(obj.toUiSelector()).waitForExists(timeout);
    }

    /**
     * Waits a specified length of time for a view to become undetectable. This method waits until a view is no longer matchable, or until the timeout has elapsed. A view becomes undetectable when the UiSelector of the object is unable to find a match because the element has either changed its state or is no longer displayed. You can use this method when attempting to wait for some long operation to compete, such as downloading a large file or connecting to a remote server.
     *
     * @param obj     the target ui object
     * @param timeout time to wait (in milliseconds)
     * @return true if the element is gone before timeout elapsed, else false if timeout elapsed but a matching element is still found.
     */
    @Override
    public boolean waitUntilGone(Selector obj, long timeout) {
        try {
            if (obj.getChildOrSibling().length == 0 && obj.checkBySelectorNull(obj) == false)
                return device.wait(Until.gone(obj.toBySelector()), timeout);
        } catch (ClassCastException e) {
            Log.d("waitUntilGone got ClassCastException "+ e);
        }
        return device.findObject(obj.toUiSelector()).waitUntilGone(timeout);
    }

    /**
     * Performs a backwards fling action with the default number of fling steps (5). If the swipe direction is set to vertical, then the swipe will be performed from top to bottom. If the swipe direction is set to horizontal, then the swipes will be performed from left to right. Make sure to take into account devices configured with right-to-left languages like Arabic and Hebrew.
     *
     * @param obj        the selector of the scrollable object
     * @param isVertical vertical or horizontal
     * @return true if scrolled, and false if can't scroll anymore
     * @throws android.support.test.uiautomator.UiObjectNotFoundException
     */
    @Override
    public boolean flingBackward(Selector obj, boolean isVertical) throws UiObjectNotFoundException {
        UiScrollable scrollable = new UiScrollable(obj.toUiSelector());
        if (isVertical) scrollable.setAsVerticalList();
        else scrollable.setAsHorizontalList();
        return scrollable.flingBackward();
    }

    /**
     * Performs a forward fling with the default number of fling steps (5). If the swipe direction is set to vertical, then the swipes will be performed from bottom to top. If the swipe direction is set to horizontal, then the swipes will be performed from right to left. Make sure to take into account devices configured with right-to-left languages like Arabic and Hebrew.
     *
     * @param obj        the selector of the scrollable object
     * @param isVertical vertical or horizontal
     * @return true if scrolled, and false if can't scroll anymore
     * @throws android.support.test.uiautomator.UiObjectNotFoundException
     */
    @Override
    public boolean flingForward(Selector obj, boolean isVertical) throws UiObjectNotFoundException {
        UiScrollable scrollable = new UiScrollable(obj.toUiSelector());
        if (isVertical) scrollable.setAsVerticalList();
        else scrollable.setAsHorizontalList();
        return scrollable.flingForward();
    }

    /**
     * Performs a fling gesture to reach the beginning of a scrollable layout element. The beginning can be at the top-most edge in the case of vertical controls, or the left-most edge for horizontal controls. Make sure to take into account devices configured with right-to-left languages like Arabic and Hebrew.
     *
     * @param obj        the selector of the scrollable object
     * @param isVertical vertical or horizontal
     * @param maxSwipes  max swipes to achieve beginning.
     * @return true on scrolled, else false
     * @throws android.support.test.uiautomator.UiObjectNotFoundException
     */
    @Override
    public boolean flingToBeginning(Selector obj, boolean isVertical, int maxSwipes) throws UiObjectNotFoundException {
        UiScrollable scrollable = new UiScrollable(obj.toUiSelector());
        if (isVertical) scrollable.setAsVerticalList();
        else scrollable.setAsHorizontalList();
        return scrollable.flingToBeginning(maxSwipes);
    }

    /**
     * Performs a fling gesture to reach the end of a scrollable layout element. The end can be at the bottom-most edge in the case of vertical controls, or the right-most edge for horizontal controls. Make sure to take into account devices configured with right-to-left languages like Arabic and Hebrew.
     *
     * @param obj        the selector of the scrollable object
     * @param isVertical vertical or horizontal
     * @param maxSwipes  max swipes to achieve end.
     * @return true on scrolled, else false
     * @throws android.support.test.uiautomator.UiObjectNotFoundException
     */
    @Override
    public boolean flingToEnd(Selector obj, boolean isVertical, int maxSwipes) throws UiObjectNotFoundException {
        UiScrollable scrollable = new UiScrollable(obj.toUiSelector());
        if (isVertical) scrollable.setAsVerticalList();
        else scrollable.setAsHorizontalList();
        return scrollable.flingToEnd(maxSwipes);
    }

    /**
     * Performs a backward scroll. If the swipe direction is set to vertical, then the swipes will be performed from top to bottom. If the swipe direction is set to horizontal, then the swipes will be performed from left to right. Make sure to take into account devices configured with right-to-left languages like Arabic and Hebrew.
     *
     * @param obj        the selector of the scrollable object
     * @param isVertical vertical or horizontal
     * @param steps      number of steps. Use this to control the speed of the scroll action.
     * @return true if scrolled, false if can't scroll anymore
     * @throws android.support.test.uiautomator.UiObjectNotFoundException
     */
    @Override
    public boolean scrollBackward(Selector obj, boolean isVertical, int steps) throws UiObjectNotFoundException {
        UiScrollable scrollable = new UiScrollable(obj.toUiSelector());
        if (isVertical) scrollable.setAsVerticalList();
        else scrollable.setAsHorizontalList();
        return scrollable.scrollBackward(steps);
    }

    /**
     * Performs a forward scroll with the default number of scroll steps (55). If the swipe direction is set to vertical, then the swipes will be performed from bottom to top. If the swipe direction is set to horizontal, then the swipes will be performed from right to left. Make sure to take into account devices configured with right-to-left languages like Arabic and Hebrew.
     *
     * @param obj        the selector of the scrollable object
     * @param isVertical vertical or horizontal
     * @param steps      number of steps. Use this to control the speed of the scroll action.
     * @return true on scrolled, else false
     * @throws android.support.test.uiautomator.UiObjectNotFoundException
     */
    @Override
    public boolean scrollForward(Selector obj, boolean isVertical, int steps) throws UiObjectNotFoundException {
        UiScrollable scrollable = new UiScrollable(obj.toUiSelector());
        if (isVertical) scrollable.setAsVerticalList();
        else scrollable.setAsHorizontalList();
        return scrollable.scrollForward(steps);
    }

    /**
     * Scrolls to the beginning of a scrollable layout element. The beginning can be at the top-most edge in the case of vertical controls, or the left-most edge for horizontal controls. Make sure to take into account devices configured with right-to-left languages like Arabic and Hebrew.
     *
     * @param obj        the selector of the scrollable object
     * @param isVertical vertical or horizontal
     * @param maxSwipes  max swipes to be performed.
     * @param steps      use steps to control the speed, so that it may be a scroll, or fling
     * @return true on scrolled else false
     * @throws android.support.test.uiautomator.UiObjectNotFoundException
     */
    @Override
    public boolean scrollToBeginning(Selector obj, boolean isVertical, int maxSwipes, int steps) throws UiObjectNotFoundException {
        UiScrollable scrollable = new UiScrollable(obj.toUiSelector());
        if (isVertical) scrollable.setAsVerticalList();
        else scrollable.setAsHorizontalList();
        return scrollable.scrollToBeginning(maxSwipes, steps);
    }

    /**
     * Scrolls to the end of a scrollable layout element. The end can be at the bottom-most edge in the case of vertical controls, or the right-most edge for horizontal controls. Make sure to take into account devices configured with right-to-left languages like Arabic and Hebrew.
     *
     * @param obj        the selector of the scrollable object
     * @param isVertical vertical or horizontal
     * @param maxSwipes  max swipes to be performed.
     * @param steps      use steps to control the speed, so that it may be a scroll, or fling
     * @return true on scrolled, else false
     * @throws android.support.test.uiautomator.UiObjectNotFoundException
     */
    @Override
    public boolean scrollToEnd(Selector obj, boolean isVertical, int maxSwipes, int steps) throws UiObjectNotFoundException {
        UiScrollable scrollable = new UiScrollable(obj.toUiSelector());
        if (isVertical) scrollable.setAsVerticalList();
        else scrollable.setAsHorizontalList();
        return scrollable.scrollToEnd(maxSwipes, steps);
    }

    /**
     * Perform a scroll forward action to move through the scrollable layout element until a visible item that matches the selector is found.
     *
     * @param obj        the selector of the scrollable object
     * @param targetObj  the item matches the selector to be found.
     * @param isVertical vertical or horizontal
     * @return true on scrolled, else false
     * @throws android.support.test.uiautomator.UiObjectNotFoundException
     */
    @Override
    public boolean scrollTo(Selector obj, Selector targetObj, boolean isVertical) throws UiObjectNotFoundException {
        UiScrollable scrollable = new UiScrollable(obj.toUiSelector());
        if (isVertical) scrollable.setAsVerticalList();
        else scrollable.setAsHorizontalList();
        return scrollable.scrollIntoView(targetObj.toUiSelector());
    }


    /**
     * Name an UiObject and cache it.
     *
     * @param obj UiObject
     * @return the name of the UiObject
     */
    private String addUiObject(UiObject obj) {
        String key = UUID.randomUUID().toString();
        uiObjects.put(key, obj);
        // schedule the clear timer.
        Timer clearTimer = new Timer();
        clearTimer.schedule(new ClearUiObjectTimerTask(key), 60000);
        return key;
    }

    class ClearUiObjectTimerTask extends TimerTask {
        String name;

        public ClearUiObjectTimerTask(String name) {
            this.name = name;
        }

        @Override
        public void run() {
            uiObjects.remove(name);
        }
    }

    /**
     * Searches for child UI element within the constraints of this UiSelector selector. It looks for any child matching the childPattern argument that has a child UI element anywhere within its sub hierarchy that has a text attribute equal to text. The returned UiObject will point at the childPattern instance that matched the search and not at the identifying child element that matched the text attribute.
     *
     * @param collection Selector of UiCollection or UiScrollable.
     * @param text       String of the identifying child contents of of the childPattern
     * @param child      UiSelector selector of the child pattern to match and return
     * @return A string ID represent the returned UiObject.
     */
    @Override
    public String childByText(Selector collection, Selector child, String text) throws UiObjectNotFoundException {
        UiObject obj;
        if (exist(collection) && objInfo(collection).isScrollable()) {
            obj = new UiScrollable(collection.toUiSelector()).getChildByText(child.toUiSelector(), text);
        } else {
            obj = new UiCollection(collection.toUiSelector()).getChildByText(child.toUiSelector(), text);
        }
        return addUiObject(obj);
    }

    @Override
    public String childByText(Selector collection, Selector child, String text, boolean allowScrollSearch) throws UiObjectNotFoundException {
        UiObject obj = new UiScrollable(collection.toUiSelector()).getChildByText(child.toUiSelector(), text, allowScrollSearch);
        return addUiObject(obj);
    }

    /**
     * Searches for child UI element within the constraints of this UiSelector selector. It looks for any child matching the childPattern argument that has a child UI element anywhere within its sub hierarchy that has content-description text. The returned UiObject will point at the childPattern instance that matched the search and not at the identifying child element that matched the content description.
     *
     * @param collection Selector of UiCollection or UiScrollable
     * @param child      UiSelector selector of the child pattern to match and return
     * @param text       String of the identifying child contents of of the childPattern
     * @return A string ID represent the returned UiObject.
     */
    @Override
    public String childByDescription(Selector collection, Selector child, String text) throws UiObjectNotFoundException {
        UiObject obj;
        if (exist(collection) && objInfo(collection).isScrollable()) {
            obj = new UiScrollable(collection.toUiSelector()).getChildByDescription(child.toUiSelector(), text);
        } else {
            obj = new UiCollection(collection.toUiSelector()).getChildByDescription(child.toUiSelector(), text);
        }
        return addUiObject(obj);
    }

    @Override
    public String childByDescription(Selector collection, Selector child, String text, boolean allowScrollSearch) throws UiObjectNotFoundException {
        UiObject obj = new UiScrollable(collection.toUiSelector()).getChildByDescription(child.toUiSelector(), text, allowScrollSearch);
        return addUiObject(obj);
    }

    /**
     * Searches for child UI element within the constraints of this UiSelector. It looks for any child matching the childPattern argument that has a child UI element anywhere within its sub hierarchy that is at the instance specified. The operation is performed only on the visible items and no scrolling is performed in this case.
     *
     * @param collection Selector of UiCollection or UiScrollable
     * @param child      UiSelector selector of the child pattern to match and return
     * @param instance   int the desired matched instance of this childPattern
     * @return A string ID represent the returned UiObject.
     */
    @Override
    public String childByInstance(Selector collection, Selector child, int instance) throws UiObjectNotFoundException {
        UiObject obj;
        if (exist(collection) && objInfo(collection).isScrollable()) {
            obj = new UiScrollable(collection.toUiSelector()).getChildByInstance(child.toUiSelector(), instance);
        } else {
            obj = new UiCollection(collection.toUiSelector()).getChildByInstance(child.toUiSelector(), instance);
        }
        return addUiObject(obj);
    }

    /**
     * Creates a new UiObject for a child view that is under the present UiObject.
     *
     * @param obj      The ID string represent the parent UiObject.
     * @param selector UiSelector selector of the child pattern to match and return
     * @return A string ID represent the returned UiObject.
     */
    @Override
    public String getChild(String obj, Selector selector) throws UiObjectNotFoundException {
        UiObject ui = uiObjects.get(obj);
        if (ui != null) {
            return addUiObject(ui.getChild(selector.toUiSelector()));
        }
        return null;
    }

    /**
     * Creates a new UiObject for a sibling view or a child of the sibling view, relative to the present UiObject.
     *
     * @param obj      The ID string represent the source UiObject.
     * @param selector for a sibling view or children of the sibling view
     * @return A string ID represent the returned UiObject.
     */
    @Override
    public String getFromParent(String obj, Selector selector) throws UiObjectNotFoundException {
        UiObject ui = uiObjects.get(obj);
        if (ui != null) {
            return addUiObject(ui.getFromParent(selector.toUiSelector()));
        }
        return null;
    }

    /**
     * Get a new UiObject from the selector.
     *
     * @param selector Selector of the UiObject
     * @return A string ID represent the returned UiObject.
     * @throws android.support.test.uiautomator.UiObjectNotFoundException
     */
    @Override
    public String getUiObject(Selector selector) throws UiObjectNotFoundException {
        return addUiObject(device.findObject(selector.toUiSelector()));
    }

    /**
     * Remove the UiObject from memory.
     */
    @Override
    public void removeUiObject(String obj) {
        uiObjects.remove(obj);
    }

    /**
     * Get all named UiObjects.
     *
     * @return all names
     */
    @Override
    public String[] getUiObjects() {
        Set<String> strings = uiObjects.keySet();
        return strings.toArray(new String[strings.size()]);
    }

    private UiObject getUiObject(String name) throws UiObjectNotFoundException {
        if (uiObjects.containsKey(name)) {
            return uiObjects.get(name);
        } else {
            throw new UiObjectNotFoundException("UiObject " + name + " not found!");
        }
    }

    /**
     * Clears the existing text contents in an editable field. The UiSelector of this object must reference a UI element that is editable. When you call this method, the method first sets focus at the start edge of the field. The method then simulates a long-press to select the existing text, and deletes the selected text. If a "Select-All" option is displayed, the method will automatically attempt to use it to ensure full text selection. Note that it is possible that not all the text in the field is selected; for example, if the text contains separators such as spaces, slashes, at symbol etc. Also, not all editable fields support the long-press functionality.
     *
     * @param obj the id of the UiObject.
     * @throws android.support.test.uiautomator.UiObjectNotFoundException
     */
    @Override
    public void clearTextField(String obj) throws UiObjectNotFoundException {
        getUiObject(obj).clearTextField();
    }

    /**
     * Reads the text property of the UI element
     *
     * @param obj the id of the UiObject.
     * @return text value of the current node represented by this UiObject
     * @throws android.support.test.uiautomator.UiObjectNotFoundException
     */
    @Override
    public String getText(String obj) throws UiObjectNotFoundException {
        return getUiObject(obj).getText();
    }

    /**
     * Sets the text in an editable field, after clearing the field's content. The UiSelector selector of this object must reference a UI element that is editable. When you call this method, the method first simulates a click() on editable field to set focus. The method then clears the field's contents and injects your specified text into the field. If you want to capture the original contents of the field, call getText() first. You can then modify the text and use this method to update the field.
     *
     * @param obj  the id of the UiObject.
     * @param text string to set
     * @return true if operation is successful
     * @throws android.support.test.uiautomator.UiObjectNotFoundException
     */
    @Override
    public boolean setText(String obj, String text) throws UiObjectNotFoundException {
        return getUiObject(obj).setText(text);
    }

    /**
     * Performs a click at the center of the visible bounds of the UI element represented by this UiObject.
     *
     * @param obj the id of target ui object.
     * @return true id successful else false
     * @throws android.support.test.uiautomator.UiObjectNotFoundException
     */
    @Override
    public boolean click(String obj) throws UiObjectNotFoundException {
        return getUiObject(obj).click();
    }

    /**
     * Clicks the bottom and right corner or top and left corner of the UI element
     *
     * @param obj    the id of target ui object.
     * @param corner "br"/"bottomright" means BottomRight, "tl"/"topleft" means TopLeft, "center" means Center.
     * @return true on success
     * @throws android.support.test.uiautomator.UiObjectNotFoundException
     */
    @Override
    public boolean click(String obj, String corner) throws UiObjectNotFoundException {
        return click(getUiObject(obj), corner);
    }

    /**
     * Performs a click at the center of the visible bounds of the UI element represented by this UiObject and waits for window transitions. This method differ from click() only in that this method waits for a a new window transition as a result of the click. Some examples of a window transition:
     * - launching a new activity
     * - bringing up a pop-up menu
     * - bringing up a dialog
     *
     * @param obj     the id of target ui object.
     * @param timeout timeout before giving up on waiting for a new window
     * @return true if the event was triggered, else false
     * @throws android.support.test.uiautomator.UiObjectNotFoundException
     */
    @Override
    public boolean clickAndWaitForNewWindow(String obj, long timeout) throws UiObjectNotFoundException {
        return getUiObject(obj).clickAndWaitForNewWindow(timeout);
    }

    /**
     * Long clicks the center of the visible bounds of the UI element
     *
     * @param obj the id of target ui object.
     * @return true if operation was successful
     * @throws android.support.test.uiautomator.UiObjectNotFoundException
     */
    @Override
    public boolean longClick(String obj) throws UiObjectNotFoundException {
        return getUiObject(obj).longClick();
    }

    /**
     * Long clicks bottom and right corner of the UI element
     *
     * @param obj    the id of target ui object.
     * @param corner "br"/"bottomright" means BottomRight, "tl"/"topleft" means TopLeft, "center" means Center.
     * @return true if operation was successful
     * @throws android.support.test.uiautomator.UiObjectNotFoundException
     */
    @Override
    public boolean longClick(String obj, String corner) throws UiObjectNotFoundException {
        return longClick(getUiObject(obj), corner);
    }

    /**
     * Drags this object to a destination UiObject. The number of steps specified in your input parameter can influence the drag speed, and varying speeds may impact the results. Consider evaluating different speeds when using this method in your tests.
     *
     * @param obj     the id of ui object to be dragged.
     * @param destObj the ui object to be dragged to.
     * @param steps   usually 40 steps. You can increase or decrease the steps to change the speed.
     * @return true if successful
     * @throws android.support.test.uiautomator.UiObjectNotFoundException
     * @throws com.github.uiautomator.stub.NotImplementedException
     */
    @Override
    public boolean dragTo(String obj, Selector destObj, int steps) throws UiObjectNotFoundException, NotImplementedException {
        return dragTo(getUiObject(obj), destObj, steps);
    }

    /**
     * Drags this object to arbitrary coordinates. The number of steps specified in your input parameter can influence the drag speed, and varying speeds may impact the results. Consider evaluating different speeds when using this method in your tests.
     *
     * @param obj   the id of ui object to be dragged.
     * @param destX the X-axis coordinate of destination.
     * @param destY the Y-axis coordinate of destination.
     * @param steps usually 40 steps. You can increase or decrease the steps to change the speed.
     * @return true if successful
     * @throws android.support.test.uiautomator.UiObjectNotFoundException
     * @throws com.github.uiautomator.stub.NotImplementedException
     */
    @Override
    public boolean dragTo(String obj, int destX, int destY, int steps) throws UiObjectNotFoundException, NotImplementedException {
        return dragTo(getUiObject(obj), destX, destY, steps);
    }

    /**
     * Check if view exists. This methods performs a waitForExists(long) with zero timeout. This basically returns immediately whether the view represented by this UiObject exists or not.
     *
     * @param obj the id of ui object.
     * @return true if the view represented by this UiObject does exist
     */
    @Override
    public boolean exist(String obj) {
        try {
            return getUiObject(obj).exists();
        } catch (UiObjectNotFoundException e) {
            return false;
        }
    }

    /**
     * Get the object info.
     *
     * @param obj the id of target ui object.
     * @return object info.
     * @throws android.support.test.uiautomator.UiObjectNotFoundException
     */
    @Override
    public ObjInfo objInfo(String obj) throws UiObjectNotFoundException {
        return ObjInfo.getObjInfo(getUiObject(obj));
    }

    /**
     * Generates a two-pointer gesture with arbitrary starting and ending points.
     *
     * @param obj         the id of target ui object. ??
     * @param startPoint1 start point of pointer 1
     * @param startPoint2 start point of pointer 2
     * @param endPoint1   end point of pointer 1
     * @param endPoint2   end point of pointer 2
     * @param steps       the number of steps for the gesture. Steps are injected about 5 milliseconds apart, so 100 steps may take around 0.5 seconds to complete.
     * @return true if all touch events for this gesture are injected successfully, false otherwise
     * @throws android.support.test.uiautomator.UiObjectNotFoundException
     */
    @Override
    public boolean gesture(String obj, Point startPoint1, Point startPoint2, Point endPoint1, Point endPoint2, int steps) throws UiObjectNotFoundException, NotImplementedException {
        return gesture(getUiObject(obj), startPoint1, startPoint2, endPoint1, endPoint2, steps);
    }

    /**
     * Performs a two-pointer gesture, where each pointer moves diagonally toward the other, from the edges to the center of this UiObject .
     *
     * @param obj     the id of target ui object.
     * @param percent percentage of the object's diagonal length for the pinch gesture
     * @param steps   the number of steps for the gesture. Steps are injected about 5 milliseconds apart, so 100 steps may take around 0.5 seconds to complete.
     * @return true if all touch events for this gesture are injected successfully, false otherwise
     * @throws android.support.test.uiautomator.UiObjectNotFoundException
     * @throws com.github.uiautomator.stub.NotImplementedException
     */
    @Override
    public boolean pinchIn(String obj, int percent, int steps) throws UiObjectNotFoundException, NotImplementedException {
        return pinchIn(getUiObject(obj), percent, steps);
    }

    /**
     * Performs a two-pointer gesture, where each pointer moves diagonally opposite across the other, from the center out towards the edges of the this UiObject.
     *
     * @param obj     the id of target ui object.
     * @param percent percentage of the object's diagonal length for the pinch gesture
     * @param steps   the number of steps for the gesture. Steps are injected about 5 milliseconds apart, so 100 steps may take around 0.5 seconds to complete.
     * @return true if all touch events for this gesture are injected successfully, false otherwise
     * @throws android.support.test.uiautomator.UiObjectNotFoundException
     * @throws com.github.uiautomator.stub.NotImplementedException
     */
    @Override
    public boolean pinchOut(String obj, int percent, int steps) throws UiObjectNotFoundException, NotImplementedException {
        return pinchOut(getUiObject(obj), percent, steps);
    }

    /**
     * Performs the swipe up/down/left/right action on the UiObject
     *
     * @param obj   the id of target ui object.
     * @param dir   "u"/"up", "d"/"down", "l"/"left", "r"/"right"
     * @param steps indicates the number of injected move steps into the system. Steps are injected about 5ms apart. So a 100 steps may take about 1/2 second to complete.
     * @return true of successful
     * @throws android.support.test.uiautomator.UiObjectNotFoundException
     */
    @Override
    public boolean swipe(String obj, String dir, int steps) throws UiObjectNotFoundException {
        return swipe(getUiObject(obj), dir, steps);
    }

    /**
     * Waits a specified length of time for a view to become visible. This method waits until the view becomes visible on the display, or until the timeout has elapsed. You can use this method in situations where the content that you want to select is not immediately displayed.
     *
     * @param obj     the id of target ui object
     * @param timeout time to wait (in milliseconds)
     * @return true if the view is displayed, else false if timeout elapsed while waiting
     */
    @Override
    public boolean waitForExists(String obj, long timeout) throws UiObjectNotFoundException {
        return getUiObject(obj).waitForExists(timeout);
    }

    /**
     * Waits a specified length of time for a view to become undetectable. This method waits until a view is no longer matchable, or until the timeout has elapsed. A view becomes undetectable when the UiSelector of the object is unable to find a match because the element has either changed its state or is no longer displayed. You can use this method when attempting to wait for some long operation to compete, such as downloading a large file or connecting to a remote server.
     *
     * @param obj     the id of target ui object
     * @param timeout time to wait (in milliseconds)
     * @return true if the element is gone before timeout elapsed, else false if timeout elapsed but a matching element is still found.
     */
    @Override
    public boolean waitUntilGone(String obj, long timeout) throws UiObjectNotFoundException {
        return getUiObject(obj).waitUntilGone(timeout);
    }

    /**
     * Get Configurator
     *
     * @return Configurator information.
     * @throws com.github.uiautomator.stub.NotImplementedException
     */
    @Override
    public ConfiguratorInfo getConfigurator() throws NotImplementedException {
        return new ConfiguratorInfo();
    }

    /**
     * Set Configurator.
     *
     * @param info the configurator information to be set.
     * @throws com.github.uiautomator.stub.NotImplementedException
     */
    @Override
    public ConfiguratorInfo setConfigurator(ConfiguratorInfo info) throws NotImplementedException {
        ConfiguratorInfo.setConfigurator(info);
        return new ConfiguratorInfo();
    }
}
