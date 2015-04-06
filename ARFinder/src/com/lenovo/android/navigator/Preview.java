package com.lenovo.android.navigator;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;

import android.view.MotionEvent;
import android.view.View;

import com.lenovo.android.navigator.AnchorView.AnchorInfo;

/*
 * 点击重合信息点， 进入散点的界面
 */
public class Preview extends View {
	
    private int touchX;
    private int touchY;
    
	private int value;
    private int[] catalog;
    private List<Rect> rects = new ArrayList<Rect>();
    private List<AnchorInfo> anchorInfos;

	private Paint paint;
	
    public Preview(Context context, AttributeSet attrs) {
        super(context, attrs);
        
        paint = new Paint();
    	paint.setStyle(Paint.Style.FILL);
    	paint.setColor(Color.WHITE);
    }
    
    public void updateView(int value, int catalog[], List<AnchorInfo> anchorInfos) {
    	this.value = value;
    	this.catalog = catalog;
    	this.anchorInfos = anchorInfos;
    	rects.clear();
    	
    	for (int i = 0; i < anchorInfos.size(); i++) {
    		rects.add(anchorInfos.get(i).rect);
    	}
    	disperse();
    	invalidate();
    }
    
	private static final int centerX = 60;
	private int centerY = 0;
	
	private static final int offsets[] = {0, 220, 160, 130, 80};//第一个元素没用
	private static final int steps[] = {0, 0, 110, 110, 110};//第一个元素没用
	
	/*
	 * 根据重合的信息点的数目，计算出散点的各个位置
	 */
    private void disperse() {
        centerY = 0;
    	int count = 0;
		for (int j = 0; j < catalog.length; j++) {
			if (catalog[j] == value) {
				count++;
			}
		}
		if (count == 0 || count == 1) {
			return;
		}
    	for (int i = 0; i < anchorInfos.size(); i++) {
    		rects.get(i).set(centerX - rects.get(i).width() / 2, 
    				centerY - rects.get(i).height() / 2, 
    				centerX + rects.get(i).width() / 2, 
    				centerY + rects.get(i).height() / 2);
    	}
    	
    	int k = (int) Math.round((count + 0.5) / 2);    	
    	int m = 0;
    	for (int i = 0; i < anchorInfos.size(); i++) {
			if (catalog[i] == value) {				
				int dx = (m % 2) * 180 ;				
				int dy = (m / 2) * steps[k] + offsets[k];
				rects.get(i).offset(dx, dy);
				m++;
			}
    	}
    }

    @Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		if (catalog != null) {
			for (int j = 0; j < catalog.length; j++) {
				if (catalog[j] == value) {
			        Drawable drawable = Const.getDrawableByDistance(anchorInfos.get(j).getData().getRealTimeDistance(), false);
			        Const.draw(canvas, drawable, rects.get(j));			        
			        canvas.drawText(anchorInfos.get(j).data.getName(), rects.get(j).left, rects.get(j).bottom + 20, paint);
				}
			}
		}
	}
	
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int x = (int) event.getX();
        int y = (int) event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
            	touchX = x;
            	touchY = y;
            	int index = touchSelect();
            	
            	CameraActivity act = ((CameraActivity) getContext());
            	if (index >= 0) {			//有选择
            		act.anchorView.updateSelection(index);
            	}
            	act.backfromPreview();
    			setVisibility(View.INVISIBLE);
            	return true;
            case MotionEvent.ACTION_MOVE:
                break;
            case MotionEvent.ACTION_UP:
                break;
        }
        return false;
    }
    
    private int touchSelect() {
		for (int i = 0; i < rects.size(); i++) {
			if (rects.get(i).contains(touchX, touchY)) {
				return i;
			}
		}
		return -1;
	}
}