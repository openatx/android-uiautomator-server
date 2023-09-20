package com.github.uiautomator.stub.helper.core;

import android.view.accessibility.AccessibilityNodeInfo;

import com.github.uiautomator.stub.exceptions.UiAutomator2Exception;

import static com.github.uiautomator.stub.helper.ReflectionUtils.invoke;
import static com.github.uiautomator.stub.helper.ReflectionUtils.method;

public class QueryController {

    private static final String CLASS_QUERY_CONTROLLER = "android.support.test.uiautomator.QueryController";
    private static final String METHOD_GET_ACCESSIBILITY_ROOT_NODE = "getRootNode";

    private final Object queryController;

    public QueryController(Object queryController) {
        this.queryController = queryController;
    }

    public AccessibilityNodeInfo getAccessibilityRootNode() throws UiAutomator2Exception {
        return (AccessibilityNodeInfo) invoke(method(CLASS_QUERY_CONTROLLER, METHOD_GET_ACCESSIBILITY_ROOT_NODE), queryController);
    }

}
