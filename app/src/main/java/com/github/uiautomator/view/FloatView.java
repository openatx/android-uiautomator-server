package com.github.uiautomator.view;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.WindowManager;

import com.github.uiautomator.R;


public class FloatView extends View {
    private Paint mPaint;
    private int fX;
    private int fY;
    private int tX;
    private int tY;
    private boolean mSwipe = false;
    private int mRadius;
    private boolean drawView = false;
    private Resources mResources;
    private Bitmap mBitmap;
    private Rect mSrcRect;

    private WindowManager mW = (WindowManager)getContext().getApplicationContext().getSystemService(Context.WINDOW_SERVICE);

    public FloatView(Context context) {
        this(context, null);
    }

    public FloatView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FloatView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setColor(Color.RED);
        mPaint.setStrokeWidth(3);
        fX = 0;
        fY = 0;
        tX = 0;
        tY = 0;
        mRadius = 30;
        mResources = getResources();
        initBitmap();
    }

    private void initBitmap() {
        mBitmap = ((BitmapDrawable)mResources.getDrawable(R.drawable.ic_notification)).getBitmap();
        mSrcRect = new Rect(0, 0, 100, 100);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawIcon(canvas);
        if (drawView){
            mPaint.setAlpha(0xFF);
            if(mSwipe){
                drawSwipe(canvas);
                mSwipe = false;
            }else {
                drawClick(canvas);
            }
            drawView = false;
        }
    }

    private void drawIcon(Canvas canvas) {
        int mWidth = mW.getDefaultDisplay().getWidth();
        int mHeight = mW.getDefaultDisplay().getHeight();
        Rect mDestRect = new Rect(mWidth - 140, mHeight / 2 - 50, mWidth - 40, mHeight / 2 + 50);
        mPaint.setAlpha(0x88);
        canvas.drawBitmap(mBitmap, mSrcRect, mDestRect, mPaint);
    }

    private void drawClick(Canvas canvas) {
        int mWidth = mW.getDefaultDisplay().getWidth();
        int mHeight = mW.getDefaultDisplay().getHeight();
        mPaint.setColor(Color.RED);
        canvas.drawCircle(fX, fY, mRadius, mPaint);
        canvas.drawLine(0, fY, mWidth, fY, mPaint);
        canvas.drawLine(fX, 0, fX, mHeight, mPaint);
        mPaint.setColor(Color.WHITE);
        canvas.drawCircle(fX, fY, mRadius - 5, mPaint);
    }

    private void drawSwipe(Canvas canvas) {
        mPaint.setColor(Color.RED);
        canvas.drawCircle(fX, fY, mRadius, mPaint);
        canvas.drawCircle(tX, tY, mRadius-10, mPaint);
        canvas.drawLine(fX, fY, tX, tY, mPaint);
        mPaint.setColor(Color.WHITE);
        canvas.drawCircle(fX, fY, mRadius - 5, mPaint);
        canvas.drawCircle(tX, tY, mRadius - 15, mPaint);
    }

    public void updateView(int fX, int fY, int tX, int tY) {
        this.fX = fX;
        this.fY = fY;
        this.tX = tX;
        this.tY = tY;
        drawView = true;
        if (fX != tX || fY != tY){
            mSwipe = true;
        }
        invalidate();
    }

    public void clearView() {
        invalidate();
    }
}
