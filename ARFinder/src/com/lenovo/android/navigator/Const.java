package com.lenovo.android.navigator;

import java.lang.ref.WeakReference;
import java.util.List;

import com.lenovo.android.navigator.AnchorViewClone.Data;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

/*
 * 公共函数类
 */

public final class Const {

	public static final int NEAR = 200;
	public static final int MID  = 1000;
	public static final int FAR  = 3000;
	
    private static Drawable far;
    private static Drawable farSelected;
    private static Drawable mid;
    private static Drawable midSelected;
    private static Drawable near;
    private static Drawable nearSelected;
    
    public static Drawable finish;
    public static Drawable back;
    public static Drawable right;
    public static Drawable left;    
    
    public static Drawable bgNumber;  
    
    public static Drawable stepLeft;
    public static Drawable stepRight;
    public static Drawable finishLeft;
    public static Drawable finishRight;    

    private static Paint paint;
    
    public static Paint getPaint() {
    	if (paint == null) {
    		paint = new Paint();
    		paint.setStyle(Paint.Style.FILL);
    		paint.setColor(Color.BLUE);
    	}
    	return paint;
    }
    
    /*
     * 将一些常用的资源载入， 方便其他类直接使用
     */
    public static void loadDrawable(Context context) {
        WeakReference<Context> cxt = new WeakReference<Context>(context);
        Resources res = cxt.get().getResources();   
        
        far = res.getDrawable(R.drawable.far);
        farSelected = res.getDrawable(R.drawable.far_selected);
        mid = res.getDrawable(R.drawable.mid);
        midSelected = res.getDrawable(R.drawable.mid_selected);
        near = res.getDrawable(R.drawable.near);
        nearSelected = res.getDrawable(R.drawable.near_selected); 
        
        finish = res.getDrawable(R.drawable.finish_flag);
        back = res.getDrawable(R.drawable.turn_back); 
        right = res.getDrawable(R.drawable.turn_right); 
        left = res.getDrawable(R.drawable.turn_left); 
        bgNumber = res.getDrawable(R.drawable.number_background);
        
        stepLeft = res.getDrawable(R.drawable.step_left);
        stepRight = res.getDrawable(R.drawable.step_right);
        finishLeft = res.getDrawable(R.drawable.finish_left);
        finishRight = res.getDrawable(R.drawable.finish_right);
    }
    
    /*
     * 根据距离远近返回不同的气泡图元
     */
	public static Drawable getDrawableByDistance(double d, boolean isSelected) {
		if (isSelected) {
			if (d >= 0 && d <= Const.NEAR)
				return nearSelected;
			else if (d > Const.NEAR && d <= Const.MID)
				return midSelected;
			else
				return farSelected;
		} else {
			if (d >= 0 && d <= Const.NEAR)
				return near;
			else if (d > Const.NEAR && d <= Const.MID)
				return mid;
			else
				return far;
		}
	}
	
	private static int square(Rect a) {
		return a.width() * a.height();
	}
	
	/*
	 * if two info bubbles' intersection area larger than half 
	 * size of the smaller one, return true.
	 * 
	 * else return false
	 */
	public static boolean isOverlap(Rect a, Rect b) {
		if (Rect.intersects(a, b)) {
			Rect tmp = new Rect();
			tmp.setIntersect(a, b);
			return (2 * square(tmp) > Math.min(square(a), square(b)));
		}
		return false;
	}
	
    public static void draw(Canvas canvas, Drawable d, Rect r) {
        d.setBounds(r);
        d.draw(canvas);
    }
    
	/*
	 * Calculate bubbles overlapping information.
	 * Returns int[0] The overlapped bubbles have the same
	 * value. For the single bubble, value is 0.
	 * int[1] index and count.
	 */	
	
	public static int[][] getOverlapBubbleInfo(List<Data> anchorInfos) {
		int size = anchorInfos.size();
		int catalog[][] = new int[2][size];
		int[][] result = new int[size][size];
		
		for (int i = size - 1; i >= 0; i--) {	//计算互为重叠的气泡， 标记为1
			for (int j = 0; j < i; j++) {
				if (Const.isOverlap(anchorInfos.get(i).rect, anchorInfos.get(j).rect)) {
					result[i][j] = 1;
				}
			}
            result[i][i] = 1;
		}
		for (int i = 0; i < size; i++) {			//按列扫描result
			for (int j = i + 1 ; j < size; j++) {   //从当前行的下个开始计算 j = i+1
				if (result[j][i] > 0) {				//表示找到一个重叠的气泡
					for (int k = 0; k < size; k++) {
						result[j][k] = 0;				//清除该行， 因为这个气泡被认为和当前气泡重叠
					}
					result[i][i] += 1;					//重叠的气泡数目，
					catalog[0][j] = i + 1;				//记录那些气泡和当前气泡重叠， 并付给只有重叠的气泡才相等的值； 不同的重叠群这个值是不一样的
					catalog[0][i] = i + 1;				//别忘了自己
				}
			}
		}
		for (int i = 0; i < size; i++) {
			if (result[i][i] > 1) {
				catalog[1][i] = result[i][i];		//重叠的数目
			}
		}
		return catalog;
	}
}
