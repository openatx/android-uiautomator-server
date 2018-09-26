package com.github.uiautomator.handler;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;

import com.github.uiautomator.view.FloatView;

public class CoorHandler extends Handler {
    private FloatView mLayout;

    public CoorHandler(FloatView mLayout){
        this.mLayout = mLayout;
    }

    @Override
    public void handleMessage(Message msg) {
        super.handleMessage(msg);
        Bundle bundle;
        String data;
        String[] coor;
        if (this.mLayout != null && this.mLayout.getVisibility() == View.VISIBLE){
            switch (msg.what){
                case 0:
                    bundle = msg.getData();
                    data = bundle.getString("coor");
                    coor = data.substring(1, data.length() - 1).split(", ");
                    if (coor.length == 5){
                        int fX = Integer.parseInt(coor[0]);
                        int fY = Integer.parseInt(coor[1]);
                        int tX = Integer.parseInt(coor[2]);
                        int tY = Integer.parseInt(coor[3]);
                        mLayout.setVisibility(View.VISIBLE);
                        mLayout.updateView(fX, fY, tX, tY);
                        Message message = new Message();
                        message.what = 1;
                        sendMessageDelayed(message, 600);
                    }else if (coor[0].equals("0")){
                        int x = Integer.parseInt(coor[1]);
                        int y = Integer.parseInt(coor[2]);
                        mLayout.setVisibility(View.VISIBLE);
                        mLayout.updateView(x, y, x, y);
                        Message message = new Message();
                        message.what = 1;
                        sendMessageDelayed(message, 600);
                    }
                    break;
                case 1:
                    mLayout.clearView();
                    break;
            }
        }
    }

    public void updateFloatView(FloatView mLayout){
        this.mLayout = mLayout;
    }
}
