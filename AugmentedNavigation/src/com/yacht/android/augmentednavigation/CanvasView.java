package com.yacht.android.augmentednavigation;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class CanvasView extends View {
	/** AugmentedNavigation实例 */
	private AugmentedNavigation an;	
	/** 兴趣点方向 */
	private float[] poiDirection; 
	/** 地球半径 */
	private static double  EARTH_RADIUS = 6378.137;
	/** 兴趣点离用户的距离，不考虑海拔因素 */
	private double[] poiDistanceWithoutAltitude;
	/** 兴趣点离用户的距离，要考虑海拔因素 */
	private double[] poiDistanceWithAltitude;
	/** 兴趣点和用户的连线与水平面的夹角，取值范围[-90, +90] */
	private float[] poiAngle;
	
	/**
	 * 构造函数
	 * @param context Context实例
	 * @param an AugmentedNavigation实例
	 */
	public CanvasView(Context context/*, AttributeSet attrs*/, AugmentedNavigation an) {
		super(context/*, attrs*/);	
		
		// 初始化
		this.an = an;
		poiDirection = new float[5];
		poiDistanceWithoutAltitude = new double[5];
		poiDistanceWithAltitude = new double[5];
		poiAngle = new float[5];
	}
	    
	@Override
	protected void onDraw(Canvas canvas) {
		if(an.isDrawEnabled()){
			super.onDraw(canvas);
			
			// 屏幕像素横坐标
			float[] poiU = new float[5];			
			// 屏幕像素纵坐标 
			float[] poiV = new float[5];			
			// 兴趣点方向和用户方向的差值
			float[] offsetDirection = new float[5];			
		    // 实例化Paint对象
			Paint paint = new Paint();
			paint.setStyle(Paint.Style.FILL);
			
			int i = 0;
			while(i<5){
				/*
				 * 计算兴趣点以用户为原点且相对于正北方的偏移角度。平面坐标系中，经度相当
				 * 于坐标x，纬度相当于坐标y。由于采用four-quadrant inverse tangent，
				 * 计算的兴趣点偏移角度poiDirection的范围是[-180, 180]，则需要将该角度的
				 * 范围先转换成[0, 359]，再根据当前用户水平旋转次数来增加或减少若干360度，
				 * 使得兴趣点方向和用户方向的差值的绝对值范围保持在[0, 359]，以方便后面的计算。
				 */			
				poiDirection[i] = (float) Math.toDegrees(Math.atan2(
						an.getPOILocation()[i][1]-an.getMyLocation()[1], 
						an.getPOILocation()[i][0]-an.getMyLocation()[0]));
				if(poiDirection[i]<0&&poiDirection[i]>=-180)
					poiDirection[i] = poiDirection[i]+360;	
				poiDirection[i] = poiDirection[i] + an.getOrientationCounter()*360;
				
				// 获取距离，不考虑海拔因素，米制单位
				poiDistanceWithoutAltitude[i] = 1000*(getDistanceWithoutAltitude(an.getPOILocation()[i][0], an.getPOILocation()[i][1], 
						an.getMyLocation()[0], an.getMyLocation()[1]));
				// 获取距离，要考虑海拔因素，米制单位
				poiDistanceWithAltitude[i] = 1000*(getDistanceWithAltitude(an.getPOILocation()[i][0], an.getPOILocation()[i][1], an.getPOILocation()[i][2],
						an.getMyLocation()[0], an.getMyLocation()[1], an.getMyLocation()[2]));
				// 获取兴趣点和用户的连线与水平面的夹角
				poiAngle[i] = (float)(Math.toDegrees(Math.asin((an.getPOILocation()[i][2] - an.getMyLocation()[2])/poiDistanceWithAltitude[i])));
				
				/* 
				 * 计算兴趣点在屏幕上的原始像素横坐标。
				 * 在计算兴趣点的屏幕像素横坐标时，需要求取兴趣点方向和用户朝向的差值
				 * offsetDirection，正常情况下，该差值的绝对值范围为[0,359]，在屏幕像素横坐标
				 * 的计算公式中，两者相互靠近， 即当差值缩小到一定范围内时，兴趣点在屏幕上的
				 * 像素横坐标落在屏幕像素宽度的范围内，此时可见。 但当兴趣点的偏移角度为正北
				 * 方向附近，且用户的旋转角度也为正北方向附近时，正常情况下兴趣点是可见的，但
				 * 是在某些特殊情况下，会出问题。例如，当某兴趣点的偏移角度为0，即正北方，用户
				 * 旋转角度若在-5度，正常情况下是应该可见的。但根据前面的公式：
				 * poiDirection[i] = poiDirection[i] + an.getOrientationCounter()*360
				 * ，此时兴趣点的偏移角度为-360，那么两者的差值就成了-360-(-5)=-355度，而该差
				 * 代入屏幕像素横坐标的计算公式时，计算出来的结果为不可见。所以，在这种情况下就
				 * 需要对差值处理，加上360，结果为5度，此时代入屏幕像素横坐标的计算公式，计算结果
				 * 才正确。所以，需要对该差值的范围进行再次限定，这里将该范围的绝对值设定为
				 * [0, 180]。若两者的差值超过该范围，则需对差值offsetDirection做360度增加或减
				 * 少，以此限定其范围。
				 * 屏幕像素横坐标的计算公式：
				 * 角度差值*二分之一屏幕像素宽度与二分之一屏幕水平视角的比例+二分之一屏幕像素
				 * 宽度。
				 */
				offsetDirection[i] = poiDirection[i]-an.getDirection();
			    if(offsetDirection[i]<-180){
					offsetDirection[i] += 360;
				}else if(offsetDirection[i]>180){
					offsetDirection[i] -= 360;
				}
			    float poiScreenXOriginal = offsetDirection[i]*an.getScreenWidth()/2/15+an.getScreenWidth()/2;			
				
				/*
				 * 计算兴趣点在屏幕上的原始像素纵坐标
				 * 根据俯仰的角度来计算，规定俯仰角度朝水平方向为0，朝上为正，朝下为负，
				 * 取值范围[-90， 90]。公式为：
				 * (俯仰角度-兴趣点和用户的连线与水平面的夹角)*二分之一屏幕像素高度与二分之一屏幕垂直视角的比例+二分之一屏
				 * 幕像素高度
				 */
			    float poiScreenYOriginal = (-an.getInclination()-poiAngle[i])*an.getScreenHeight()/2/25+an.getScreenHeight()/2;
			    
			    // 转换成以屏幕中心为原点的坐标系
			    float x = poiScreenXOriginal-an.getScreenWidth()/2;
			    float y = -(poiScreenYOriginal-an.getScreenHeight()/2);
			    // 兴趣点在屏幕上的坐标离屏幕原点的距离
			    float radius = (float) Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2));
			    
//			    // 兴趣点在屏幕上的坐标与屏幕原点的连线和x轴正方向的角度，取值范围是[-90, +90]
//			    float slope = y/x;
//			    float poiScreenAngleOriginal = (float) Math.toDegrees(Math.atan(slope));
//			    // 将poiScreenAngleOriginal的取值范围变成[0, 359]，0为落在x轴正方向，同极坐标
//			    if(x>0&&y>=0){ // 第一象限或x轴正方向
//			    	// poiScreenAngleOriginal保持原来的值			     	
//			    }else if(x==0&&y>0){ // y轴正方向
//			    	poiScreenAngleOriginal = 90;
//			    }else if(x<0&&y>=0){ // 第二象限或x轴负方向
//			    	poiScreenAngleOriginal += 180;
//			    }else if(x<0&&y<0){ // 第三象限
//			    	poiScreenAngleOriginal += 180;
//			    }else if(x==0&&y<0){ // y轴负方向
//			    	poiScreenAngleOriginal = 270;
//			    }else if(x>0&&y<0){ // 第四象限
//			    	poiScreenAngleOriginal += 360;
//			    }else if(x==0&&y==0){ // 原点
//			    	// poiU[i] = poiScreenXOriginal;
//			    	// poiV[i] = poiScreenYOriginal;
//			    }
			    
			    /*
			     * 兴趣点在屏幕上的坐标与屏幕原点的连线和x轴正方向的角度，采用four-quadrant inverse tangent，
			     * 取值范围是[-180, +180]
			     */
			    float poiScreenAngleOriginal = (float) Math.toDegrees(Math.atan2(y, x));
			    // 将poiScreenAngleOriginal的取值范围变成[0, 359]，0为落在x轴正方向，同极坐标
			    if(poiScreenAngleOriginal<0&&poiScreenAngleOriginal>=-180){
			    	poiScreenAngleOriginal += 360;
			    }		    
			    
			    // 左右摆动会影响兴趣点在屏幕上的角度
			    float poiScreenAngleRoll = poiScreenAngleOriginal - an.getRoll();
			    
			    // 极坐标系与平面直角坐标系（笛卡尔坐标系）间转换
			    x = (float) (radius * Math.cos(Math.toRadians(poiScreenAngleRoll)));
			    y = (float) (radius * Math.sin(Math.toRadians(poiScreenAngleRoll)));
			    
			    // 转换成屏幕坐标系
			    poiU[i] = x + an.getScreenWidth()/2;
			    poiV[i] = - (y - an.getScreenHeight()/2);
				
				// 渲染，当进入50米距离范围时颜色变红，否则为绿
				if(poiDistanceWithoutAltitude[i] <= 50)
					paint.setColor(Color.RED);
				else 
					paint.setColor(Color.GREEN);
				canvas.drawText(an.getPOIName()[i]+":"+Math.round(poiDistanceWithoutAltitude[i])+"米", poiU[i]+15, poiV[i], paint);
				canvas.drawCircle(poiU[i], poiV[i], 10, paint);
				
				i++;			
			}

			/*
			 *  保证onDraw()方法循环渲染。若没有这语句，则仅进行第一次渲染，
			 *  之后不再渲染。
			 */
			//invalidate();
		}
	}
	
	@Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
    }
    
    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
    }
    
    /** 
     * 根据两点经纬度计算距离，公里制单位
     * @param lat1 第一个点的纬度
     * @param lng1 第一个点的经度
     * @param lat2 第二个点的纬度
     * @param lng2 第二个点的经度
     * @return 返回距离，公里制单位
     */
    protected double getDistanceWithoutAltitude(double lat1, double lng1, double lat2, double lng2)
    {
        double radLat1 = Math.toRadians(lat1);
        double radLat2 = Math.toRadians(lat2);
        double a = radLat1 - radLat2;
        double b = Math.toRadians(lng1) - Math.toRadians(lng2);
        double s = 2 * Math.asin(Math.sqrt(Math.pow(Math.sin(a/2),2) + 
        		Math.cos(radLat1)*Math.cos(radLat2)*Math.pow(Math.sin(b/2),2)));
        s = s * EARTH_RADIUS;
        return s;
    }
    
    /**
     * 根据两点经纬度和海拔计算距离，公里制单位
     * @param lat1 第一个点的纬度
     * @param lng1 第一个点的经度
     * @param alt1 第一个点的海拔
     * @param lat2 第二个点的纬度
     * @param lng2 第二个点的经度
     * @param alt2 第二个点的海拔
     * @return 返回距离，公里制单位
     */
    protected double getDistanceWithAltitude(double lat1, double lng1, double alt1, double lat2, double lng2, double alt2)
    {
        double radLat1 = Math.toRadians(lat1);
        double radLat2 = Math.toRadians(lat2);
        double a = radLat1 - radLat2;
        double b = Math.toRadians(lng1) - Math.toRadians(lng2);
        double s = 2 * Math.asin(Math.sqrt(Math.pow(Math.sin(a/2),2) + 
        		Math.cos(radLat1)*Math.cos(radLat2)*Math.pow(Math.sin(b/2),2)));
        s = s * (EARTH_RADIUS + Math.min(alt1, alt2)/1000.0); // 水平距离,考虑海拔修正
        double deltaHeight = Math.abs(alt1 - alt2)/1000.0; // 高差
        s = Math.sqrt(s * s + deltaHeight * deltaHeight); // 直线距离
        return s;
    }    
    
    /**
     * 返回兴趣点离用户的距离，不考虑海拔因素
     * @return
     */
    public double[] getPOIDistanceWithoutAltitude(){
    	return poiDistanceWithoutAltitude;
    }


}