package com.github.uiautomator.stub;

import static com.github.uiautomator.stub.helper.ReflectionUtils.getField;
import static com.github.uiautomator.stub.helper.ReflectionUtils.invoke;
import static com.github.uiautomator.stub.helper.ReflectionUtils.method;

import android.app.UiAutomation;
import androidx.test.uiautomator.UiDevice;
import android.view.Display;

import com.github.uiautomator.stub.helper.core.InteractionController;
import com.github.uiautomator.stub.helper.core.QueryController;

/**
 * device common method
 */
public class Device {
    private static volatile Device instance = null;
    private static final String FIELD_QUERY_CONTROLLER = "mQueryController";
    private static final String FIELD_INTERACTION_CONTROLLER = "mInteractionController";
    private static final String METHOD_GET_DEFAULT_DISPLAY = "getDefaultDisplay";
    private UiDevice uiDevice;
    private UiAutomation uiAutomation;
    private QueryController mQueryController;
    private InteractionController mInteractionController;

    private Device(){
    }

    public static Device getInstance(){
        if(instance == null){
            synchronized (Device.class){
                if(instance == null){
                    instance = new Device();
                }
            }
        }
        return instance;
    }

    public void init(UiDevice uiDevice,UiAutomation uiAutomation){
        this.uiDevice = uiDevice;
        this.uiAutomation = uiAutomation;
        try{
            mQueryController = new QueryController(getField(UiDevice.class, FIELD_QUERY_CONTROLLER, uiDevice));
        }catch (Exception e){
            Log.e("get query controller error", e);
        }
        try {
            mInteractionController =  new InteractionController(getField(UiDevice.class, FIELD_INTERACTION_CONTROLLER, uiDevice));
        } catch (Exception e) {
            Log.e("get query controller error", e);
        }

    }

    public UiDevice getUiDevice(){
        return uiDevice;
    }

    public UiAutomation getUiAutomation(){ return uiAutomation; }

    public Display getDefaultDisplay(){
        return (Display) invoke(method(uiDevice.getClass(),METHOD_GET_DEFAULT_DISPLAY), uiDevice);
    }

    public QueryController getQueryController(){
        return mQueryController;
    }

    public InteractionController getInteractionController() {
        return mInteractionController;
    }


}
