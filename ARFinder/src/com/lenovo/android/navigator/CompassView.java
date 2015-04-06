package com.lenovo.android.navigator;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;

import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;

import android.view.View;

/*
 * 圆形的电子罗盘
 */
public class CompassView extends View 
    implements CameraActivity.PositionListener {
	
	private static final float THRESHOLD = 1f;
    private Bitmap background;
    private float lastDegree;
    private float degree;
    private Matrix rot = new Matrix();

    public CompassView(Context context, AttributeSet attrs) {
    	super(context,attrs);
    	Drawable drawable = context.getResources().getDrawable(R.drawable.north);        
        background = ((BitmapDrawable) drawable).getBitmap();
    }

    public void onDirectionChanged(float value) {    	
    	degree = value;
        invalidate();
    }
    
    public void onGestureChanged(float value) {
    	
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        if (Math.abs(lastDegree - degree) < THRESHOLD)
        	degree = lastDegree;
        lastDegree = degree;

        rot.reset();
        rot.setRotate(-degree, background.getWidth() / 2, background.getHeight() / 2);       
        canvas.drawBitmap(background, rot, null);
    }
}