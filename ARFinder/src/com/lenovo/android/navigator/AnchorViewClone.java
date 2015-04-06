package com.lenovo.android.navigator;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;

import android.view.MotionEvent;
import android.widget.ImageView;

/*
 * AnchorViewClone 和 AnchorView功能类似， 显示气泡和场景图片
 */
public class AnchorViewClone extends ImageView {

    private static final float SCALE_FACTOR = 0.79f; // 380/480=0.79

    private static final int offset = (int) ((81 + 25) * SCALE_FACTOR); //和信息卡的高度相等    
    private List<Data> anchorInfos = new ArrayList<Data>();
    
    private int touchX;
    private int touchY;

    private boolean isTouch = false;
	private int currentSelectedIndex;
	private boolean hasManualSelection = false;//记录有手动选择的标志    

    public static class UriTimestamp {
        public String uri;
        public long timestamp;

        public UriTimestamp(String uri, long timestamp) {
            this.uri = uri;
            this.timestamp = timestamp;
        }
    }

    public static class Data {
    	public double distance;
    	public float winX;
    	public float winY;
    	public String name;
    	public String addr;
    	public String phone;
    	public boolean isSelected;
    	public Rect rect;
    	public long timestamp;
    	
    	public Data() {
    	}
    	
    	public Data(Data data) {
    		this.distance = data.distance;
    		this.winX = data.winX;
    		this.winY = data.winY;
    		this.name = data.name;
    		this.addr = data.addr;
    		this.phone = data.phone;
    		isSelected = false;
    	}
    	
    	public Data(String d, String winX, String winY, String name, 
    			String addr, String phone) {
    		this.distance = Double.valueOf(d);
    		this.winX = Float.valueOf(winX) * SCALE_FACTOR;
    		this.winY = Float.valueOf(winY) * SCALE_FACTOR + offset;
    		this.name = name;
    		this.addr = addr;
    		this.phone = phone;
    		isSelected = false;
    	}
    	
    	public Data(String d, float winX, float winY, String name, 
    			String addr, String phone) {
    		this.distance = Double.valueOf(d);
    		this.winX = winX;
    		this.winY = winY;
    		this.name = name;
    		this.addr = addr;
    		this.phone = phone;
    		isSelected = false;
    	}
    	
    	public static ArrayList<Data> analysis(String[] buffer) {
    		if (buffer == null)
    			return null;
    		int length = buffer.length;
    		ArrayList<Data> result = new ArrayList<Data>();    		
    		for (int i = 0; i < length; i += 6) {
    			Data data = new Data(buffer[i], buffer[i + 1], buffer[i + 2],
    					buffer[i + 3], buffer[i + 4], buffer[i + 5]);
    			result.add(data);
    		}
    		return result;
    	}
    	
    	public void setSelected(boolean b) {
    		this.isSelected = b;
    	}
    	
    	public boolean getSelected() {
    		return this.isSelected;
    	}
    	
    	public void setRegion(Rect r) {
    		this.rect = r;
    	}
    	
    	public boolean inInRegion(int px, int py) {
    		return rect.contains(px, py);
    	}
        
        public void serialize(ImageRecord ir, long timestamp) {
        	ir.insertRecord(this, timestamp);        	
        }
        
        public void deserialize(ImageRecord ir) {
        	ir.insertRecord(this, timestamp);
        }
        
        public void truncate(ImageRecord ir) {
        	ir.insertRecord(this, timestamp);
        }
    }
    
	public AnchorViewClone(Context context) {
        super(context);
	}

    public AnchorViewClone(Context context, AttributeSet attrs) {
        super(context,attrs);
    }
    
    public void setAnchorInfo(List<Data> infos) {
        anchorInfos.clear();
        if (infos == null) {
            return;
        }
        
        for (int i = 0; i < infos.size(); i++) {
			Data anchor = new Data(infos.get(i));
            anchorInfos.add(anchor);
        }
    }
    
    private void updateSelection(int index) {
    	setCurrentSelectedIndex(index);
    	anchorInfos.get(index).setSelected(true);
		updateInfoCard(anchorInfos.get(index));
    }
    
	public int getCurrentSelectedIndex() {
		return currentSelectedIndex;
	}
	
	public boolean hasSelection() {
		return hasManualSelection;
	}
	
	public void setCurrentSelectedIndex(int index) {
		if (index > anchorInfos.size() - 1)
			index = anchorInfos.size() - 1;
		if (index < 0)
			index = 0;
		currentSelectedIndex = index;
	}
	
	public List<Data> getAnchorInfos() {
		return anchorInfos;
	}
    
    private Rect calculateRegion(Drawable d, float windowX, float windowY) {
    	int xx = (int) windowX;
    	int yy = (int) windowY;
    	int w = d.getIntrinsicWidth();
    	int h = d.getIntrinsicHeight();
    	return new Rect(xx - w / 2, yy - h, xx + w / 2, yy);
    }
     
    public void unSelectAll() {
    	if (hasSelection()) {
    		anchorInfos.get(currentSelectedIndex).setSelected(false);
    		this.invalidate();
    	}
    }
    
	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);		

		if (anchorInfos == null)
			return;
		
		int i = anchorInfos.size() - 1;
		
		while (i >= 0) {
			Data anchor = anchorInfos.get(i);
			Drawable drawable = Const.getDrawableByDistance(anchor.distance, anchor.getSelected());
			Rect r = calculateRegion(drawable, anchor.winX, anchor.winY);//绘制的矩形区域
			anchor.setRegion(r);
			i--;
		}
		
		int catalog[][] = Const.getOverlapBubbleInfo(anchorInfos);
		for (i = 0; i < catalog[1].length; i++) {
			if (catalog[1][i] > 0) {
    			Rect r = calculateRegion(Const.bgNumber, 
						anchorInfos.get(i).rect.right + 4,
						anchorInfos.get(i).rect.top + 10);
	    		Const.draw(canvas, Const.bgNumber, r);
				canvas.drawText(catalog[1][i]+"", 
						anchorInfos.get(i).rect.right,
						anchorInfos.get(i).rect.top + 4, Const.getPaint());
			}
		}
		
		if (isTouch) {
			isTouch = false;
			touchSelect(catalog[0]);
		}
		
		i = anchorInfos.size() - 1;
		while (i >= 0) {
			Data anchor = anchorInfos.get(i);
			Drawable drawable = Const.getDrawableByDistance(anchor.distance, anchor.getSelected());	    	
	    	Const.draw(canvas, drawable, anchorInfos.get(i).rect);
			i--;
		}
	}
	
	private void touchSelect(int catalog[]) {	
		hasManualSelection = false;
		((SnapShot) getContext()).showInfoCard(false);//信息卡消失
		for (int i = 0; i < anchorInfos.size(); i++) {		
			if (anchorInfos.get(i).isSelected) {
				anchorInfos.get(i).setSelected(false);//之前的退选
				break;
			}
		}
		
		for (int i = 0; i < anchorInfos.size(); i++) {
			if (anchorInfos.get(i).inInRegion(touchX, touchY)) {
				int value = catalog[i];
				if (value == 0) { // 0 means this bubble is single one
					updateSelection(i);
				} else {
					updateSelection(i);
				}
				hasManualSelection = true;
				break;
			}
		}
		invalidate();
	}
	
	public void updateInfoCard(Data data) {
		List<Data> l = new ArrayList<Data>();
		l.add(data);
		((SnapShot)getContext()).updateInfoCard(l);
	}

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int x = (int) event.getX();
        int y = (int) event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
            	touchX = x;
            	touchY = y;
            	isTouch = true;
            	invalidate();
            case MotionEvent.ACTION_MOVE:
                break;
            case MotionEvent.ACTION_UP:
                break;
        }
        return false;
    }

    public void serialize(ImageRecord ir, long timestamp) {
    	if (anchorInfos == null) {
    		return;
    	}
    	for (int i = 0; i < anchorInfos.size(); i++) {
    		anchorInfos.get(i).serialize(ir, timestamp);
    	}
    }
}

