package com.github.uiautomator.stub.exceptions;

public class UiAutomator2Exception extends RuntimeException {
    private static final long serialVersionUID = -1592305571101012889L;

    public UiAutomator2Exception(String message) {
        super(message);
    }

    public UiAutomator2Exception(Throwable t) {
        super(t);
    }

    public UiAutomator2Exception(String message, Throwable t) {
        super(message, t);
    }
}
