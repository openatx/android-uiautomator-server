package com.github.uiautomator.screenrecorder.http;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

public class CoordinateServer extends NanoHTTPD {

    private Handler mHandler;

    public CoordinateServer(int port, Handler mHandler) {
        super(port);
        this.mHandler = mHandler;
    }

    @Override
    public Response serve(String uri, Method method,
                          Map<String, String> headers, Map<String, String> params,
                          Map<String, String> files) {
        Message message = new Message();
        message.what = 0;
        Bundle bundle = new Bundle();
        bundle.putString("coor", files.get("postData"));
        message.setData(bundle);
        mHandler.sendMessage(message);
        StringBuilder builder = new StringBuilder();
        builder.append("<!DOCTYPE html><html><body>");
        builder.append("good");
        builder.append("</body></html>\n");
        return newFixedLengthResponse(builder.toString());
    }
}
