package com.github.uiautomator.stub.helper.core;

import android.view.InputEvent;
import android.view.MotionEvent.PointerCoords;

import com.github.uiautomator.stub.exceptions.UiAutomator2Exception;
import com.github.uiautomator.stub.helper.ReflectionUtils;

public class InteractionController {

    public static final String METHOD_PERFORM_MULTI_POINTER_GESTURE = "performMultiPointerGesture";
    private static final String CLASS_INTERACTION_CONTROLLER = "android.support.test.uiautomator.InteractionController";
    private static final String METHOD_SEND_KEY = "sendKey";
    private static final String METHOD_SEND_TEXT = "sendText";
    private static final String METHOD_INJECT_EVENT_SYNC = "injectEventSync";
    private static final String METHOD_TOUCH_DOWN = "touchDown";
    private static final String METHOD_TOUCH_UP = "touchUp";
    private static final String METHOD_TOUCH_MOVE = "touchMove";
    private static final String METHOD_CLICK = "clickNoSync";
    private static final String METHOD_LONG_CLICK = "longTapNoSync";
    private final Object interactionController;

    public InteractionController(Object interactionController) {
        this.interactionController = interactionController;
    }

    public boolean sendKey(int keyCode, int metaState) throws UiAutomator2Exception {
        return (Boolean) ReflectionUtils.invoke(ReflectionUtils.method(CLASS_INTERACTION_CONTROLLER, METHOD_SEND_KEY, int.class, int.class), interactionController, keyCode, metaState);
    }

    public boolean sendText(String text) throws UiAutomator2Exception {
        return (Boolean) ReflectionUtils.invoke(ReflectionUtils.method(CLASS_INTERACTION_CONTROLLER, METHOD_SEND_TEXT, String.class), interactionController, text);
    }

    public boolean injectEventSync(InputEvent event) throws UiAutomator2Exception {
        return (Boolean) ReflectionUtils.invoke(ReflectionUtils.method(CLASS_INTERACTION_CONTROLLER, METHOD_INJECT_EVENT_SYNC, InputEvent.class), interactionController, event);
    }

    public boolean touchDown(int x, int y) throws UiAutomator2Exception {
        return (Boolean) ReflectionUtils.invoke(ReflectionUtils.method(CLASS_INTERACTION_CONTROLLER, METHOD_TOUCH_DOWN, int.class, int.class), interactionController, x, y);
    }

    public boolean touchUp(int x, int y) throws UiAutomator2Exception {
        return (Boolean) ReflectionUtils.invoke(ReflectionUtils.method(CLASS_INTERACTION_CONTROLLER, METHOD_TOUCH_UP, int.class, int.class), interactionController, x, y);
    }

    public boolean touchMove(int x, int y) throws UiAutomator2Exception {
        return (Boolean) ReflectionUtils.invoke(ReflectionUtils.method(CLASS_INTERACTION_CONTROLLER, METHOD_TOUCH_MOVE, int.class, int.class), interactionController, x, y);
    }
    public boolean clickNoSync(int x,int y) throws UiAutomator2Exception{
        return (Boolean) ReflectionUtils.invoke(ReflectionUtils.method(CLASS_INTERACTION_CONTROLLER, METHOD_CLICK, int.class, int.class), interactionController, x, y);
    }
    public boolean longTapNoSync(int x,int y) throws UiAutomator2Exception{
        return (Boolean) ReflectionUtils.invoke(ReflectionUtils.method(CLASS_INTERACTION_CONTROLLER, METHOD_LONG_CLICK, int.class, int.class), interactionController, x, y);
    }

    public Boolean performMultiPointerGesture(PointerCoords[][] pcs) throws UiAutomator2Exception {
        return (Boolean) ReflectionUtils.invoke(ReflectionUtils.method(CLASS_INTERACTION_CONTROLLER, METHOD_PERFORM_MULTI_POINTER_GESTURE, PointerCoords[][].class), interactionController, (Object) pcs);
    }
}
