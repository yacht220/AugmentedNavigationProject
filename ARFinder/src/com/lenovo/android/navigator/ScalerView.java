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
 * 屏幕顶端的电子罗盘
 */

public class ScalerView extends View 
    implements CameraActivity.PositionListener {

    private static final float DENSITY = 40f;
    
    private Bitmap bmp;
    private float offset;
    private Drawable drawable;
    private float step;
    private Matrix trans = new Matrix();

    public ScalerView(Context context, AttributeSet attrs) {
    	super(context,attrs);
        drawable = context.getResources().getDrawable(R.drawable.compass);
        Bitmap tmp = ((BitmapDrawable) drawable).getBitmap();
        int h = tmp.getHeight();
        bmp = Bitmap.createBitmap(tmp, 100, 0, 370, h);   //(470 - 100)        
        step = bmp.getWidth() / DENSITY;
    }

    public void onDirectionChanged(float value) {
        if (value >= 0f && value < 279f) {              //360 - 81;
        	offset = (80f - (value / 9f * step));       // 80 is start position of north
        } else {            
            offset = (80f + ((360 - value) / 9f * step)); // 9 = 360/DESITY
        }
        invalidate();
    }

    public void onGestureChanged(float value) {
    }
    
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        trans.reset();
        trans.setTranslate(offset, 0);
        canvas.drawBitmap(bmp, trans, null);
        
        float r = offset + bmp.getWidth();  // right edge of center bmp        
        if (r < getWidth()) {
        	trans.reset();
        	trans.setTranslate(r, 0);
        	canvas.drawBitmap(bmp, trans, null);
        }
        if (offset > 0) {
        	trans.reset();
        	trans.setTranslate(-bmp.getWidth() + offset, 0);
        	canvas.drawBitmap(bmp, trans, null);
        }
    }
}