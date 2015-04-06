package com.lenovo.arutil;

import com.lenovo.minimap.MinimapService;
import com.lenovo.minimap.dto.Coord20;
import com.lenovo.minimap.dto.CoordDeflect;
import com.lenovo.minimap.dto.CoordGps;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
//import android.os.SystemClock;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

public class ARUtil {
	private static final double EARTH_RADIUS = 6378.137;
	/* 手机屏幕像素大小 */
    private static float SCREENWIDTH;
	private static float SCREENHEIGHT;
	
	/* 对象实例化 */
	private MinimapService ms;
	private CoordGps myLocationCoordGps;
	private Coord20 myLocationCoord20;
	private CoordDeflect myLocationCoordDeflect;
	
	/* GPS*/
	private double[] myLocation; // 纬度、经度、海拔 
	private LocationManager locMan;
	//private long preTimeGPSUpdate; // 上一次GPS更新的时间
	private boolean isGPSAvailable;
	private int locationUpdateTime;//地理位置更新时间
	
	/* 传感器 */
	private float prevals = 0; // 前一个罗盘方向读数
	private int orientationCounter = 0; // 水平旋转次数，[0, 359]为0，增加360度加一，减少360度减一
	/* 通过左右摇摆进行修正了的方向数值，取值范围[-∞, +∞]，0.0表示朝正北，以360度为周期，读数按顺时针递增，逆时针递减 */
	private volatile float directionWithRoll = 0;
	/* 未通过左右摇摆进行修正了的方向数值，取值范围[-∞, +∞]，0.0表示朝正北，以360度为周期，读数按顺时针递增，逆时针递减 */
	private volatile float directionWithoutRoll = 0;
	private volatile float inclination = 0;
	/* 左右摆动数值，取值范围[-90.0, +270.0]，0.0表示手机竖直垂直，[-90.0, 0)表示手机朝右上摆动，(0, +90.0]表示手机朝左上摆动，(+90, +180]朝左下摆动，(+180, +270]朝右下摆动 */
	private volatile float roll = 0;
	private static float KFILTERINGFACTOR = (float)0.05;
	private SensorManager sensorMan;
//	private Context context;
	
	/**
	 * 在Location本地地理位置信息更新监听器里面，联网更新地理位置20级像素坐标和偏转坐标容易造成阻塞和手机屏幕卡死<br />
	 * 所以在该方法体内使用线程的方式联网更新，并且为防止多线程资源的耗费，采用同步保护
	 */
	private synchronized void updateLocationCoord() {
		myLocationCoord20 = ms.coordGps2Coord20(myLocationCoordGps);
		myLocationCoordDeflect = ms.coord202CoordDeflect(myLocationCoord20, false);
	}
	
	/* GPS监听器 */
	private final LocationListener gpsListener = new LocationListener(){
    	//Location发生变化时被调用
    	public void onLocationChanged(Location location){
    		if(location != null){
    			myLocation[0] = location.getLatitude();
    			myLocation[1] = location.getLongitude();
    			myLocation[2] = location.getAltitude();
    			
    			// 先将GPS真是经纬度转换成20级像素坐标，再将20级像素坐标转换成偏转后GPS坐标
    			myLocationCoordGps.setX(myLocation[1]);
    			myLocationCoordGps.setY(myLocation[0]);
    			
//    			new Thread() {
//					public void run() {
						updateLocationCoord();
//					};
//				}.start();
    			
    			// 再偏转一次
//    			myLocationCoordGps.setX(myLocationCoordDeflect.getX());
//    			myLocationCoordGps.setY(myLocationCoordDeflect.getY());
//    			myLocationCoord20 = ms.coordGps2Coord20(myLocationCoordGps);
//    			myLocationCoordDeflect = ms.coord202CoordDeflect(myLocationCoord20);
    			
    			/*ARFinderTest.textView.setText(
    					myLocationCoordDeflect.getX()+" "+myLocationCoordDeflect.getY()
    					//myLocation[1]+" "+ myLocation[0]
    					);*/
    			
    			//preTimeGPSUpdate = SystemClock.elapsedRealtime(); 
    			isGPSAvailable = true;
    		}else{
    			isGPSAvailable = false;
    		}
    	}
    	
    	public void onProviderDisabled(String provider){// Provider被disable时触发此函数，比如GPS被关闭
    		isGPSAvailable = false;
    	}
    	
    	public void onProviderEnabled(String provider){ //  Provider被enable时触发此函数，比如GPS被打开
    		isGPSAvailable = true;
    	}
    	
    	public void onStatusChanged(String provider, int status, Bundle extras){// Provider的转态在可用、暂时不可用和无服务三个状态直接切换时触发此函数

    	}
    };
    
    /* 传感器监听器 */
    private SensorEventListener sensorListener = new SensorEventListener() {
		
		@Override
		public void onSensorChanged(SensorEvent event) {
			float vals[] = event.values;
			
			if(event.sensor.getType() == Sensor.TYPE_ORIENTATION){				
                /*
                 * 需要将vals[0]进行平滑处理。因为vals[0]的范围是[0, 359]，在0和359之间会
                 * 出现数值突变，使得在该临界范围的计算错误。所以需要对359和0之间的突变进行
                 * 平滑，使数值的变化形式为-2、-1、0、1、2...、359、360、361...，平滑结果
                 * 传给rawDirection，则rawDirection的范围为正负无穷。
                 */
				if((prevals-vals[0])>180){    // 从359到0的突变，差值检测的范围自定义
					orientationCounter++;
				}else if((prevals-vals[0])<-180){    // 从0到359的突变，差值检测的范围自定义
					orientationCounter--;
				}
				float rawDirection = orientationCounter*360+vals[0];
				prevals = vals[0];
				
				/* 
				 * filter，减少抖动。directionWithoutRoll的范围和rawDirection一样。
				 * 还有其它的一些filter方法，但效果不理想。例如间隔若干时间
				 * 读取罗盘度数；前方向值和当前罗盘方向读数比较，大则++，小则
				 * --；360度分成n块，每块只对应一种度数，即floor(vals[0]/n)*n；
				 * 求若干时间段内罗盘读书的平均值。
				 * directionWithoutRoll的范围为正负无穷
				 */
				directionWithoutRoll =(float)((rawDirection * KFILTERINGFACTOR) + 
						(directionWithoutRoll * (1.0 - KFILTERINGFACTOR)));	
				
				// 通过左右摇摆数值对方向数值进行修正
				directionWithRoll = directionWithoutRoll + roll;
			}
			if(event.sensor.getType() == Sensor.TYPE_ACCELEROMETER){									
				/*
				 * 计算左右摆动原始角度，-pi/2<=Math.atan()<=pi/2，-90<=rawRoll<=270
				 * 根据手机坐标系的y轴指向朝上或朝下分别计算rawRoll，使其取值范围由原来
				 * 的[-90, +90]变成[-90, 270]，使得在对方向数值进行修正时，手机左右摆动
				 * 由-90到270度逆时针旋转时，其方向数值保持不变  
				 */
				float rawRoll;
				if(vals[1]<0){ // 手机坐标系的y轴朝下时
					rawRoll = (float) (180+Math.toDegrees(Math.atan(vals[0]/vals[1])));
				}else{
					rawRoll = (float) (Math.toDegrees(Math.atan(vals[0]/vals[1])));
				}	
				/* 
				 * filter，减少抖动
				 * -90<=roll<=270
				 */
				roll = (float)((rawRoll * KFILTERINGFACTOR) + 
						(roll * (1.0 - KFILTERINGFACTOR)));
				/*
				 * 计算俯仰原始角度，-pi/2<=Math.atan()<=pi/2，-90<=rawInclination<=90
				 * 当左右摇摆数值绝对值小于45度时，俯仰角度采用手机竖直的方式进行计算，
				 * 超过45度时，俯仰角度采用手机横向的方式（手机坐标系的x轴朝上或朝下两种情况）
				 * 进行计算
				 */
				float rawInclination;
				if(Math.abs(rawRoll) < 45){
					rawInclination = (float) (Math.toDegrees(Math.atan(vals[2]/vals[1])));
				}else if(vals[0]<0){ // 手机坐标系的x轴朝下时
					rawInclination = -(float) (Math.toDegrees(Math.atan(vals[2]/vals[0])));
				}else{
					rawInclination = (float) (Math.toDegrees(Math.atan(vals[2]/vals[0])));
				}
				/* 
				 * filter，减少抖动
				 * -90<=inclination<=90
				 */
				inclination = (float)((rawInclination * KFILTERINGFACTOR) + 
						(inclination * (1.0 - KFILTERINGFACTOR)));
	         }
		}
		
		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {
		}
	};
	
	/**
	 * 构造函数
	 * @param context 为外部传进的Context对象
	 */
	public ARUtil(MinimapService ms, Context context, int locationUpdateTime){
		try{
			this.ms = ms;
			this.locationUpdateTime = locationUpdateTime;
//			this.context = context;
			myLocationCoordGps = new CoordGps();
			myLocationCoordDeflect = new CoordDeflect();
			/* 获取手机屏幕像素大小 */
			Display display = ((WindowManager)context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
			SCREENWIDTH = display.getWidth(); 
			SCREENHEIGHT = display.getHeight();
			
			/* 初始化GPS */
			myLocation = new double[3];	
			//preTimeGPSUpdate = -10000;
			isGPSAvailable = false;
			
			locMan = (LocationManager)context.getSystemService(Context.LOCATION_SERVICE);
			/* 初始化传感器 */
			sensorMan = (SensorManager)context.getSystemService(Context.SENSOR_SERVICE);
			
			registerListener(this.locationUpdateTime);
		}catch(Exception e){Log.d("exception",e.getMessage());}	
	}	
	
	/**
	 * 返回当前设备的屏幕宽度
	 * @return 返回当前设备的屏幕宽度
	 */
	public float getScreenWidth(){
		return SCREENWIDTH;
	}	
	
	/**
	 * 返回当前设备的屏幕高度
	 * @return 返回当前设备的屏幕高度
	 */
	public float getScreenHeight(){
		return SCREENHEIGHT;
	}
	
	/**
	 * 返回当前位置GPS真实经纬度
	 * @return 返回当前位置GPS真实经纬度
	 */
	public CoordGps getMyLocationCoordGps(){
		return myLocationCoordGps;
	}
	
	/**
	 * 获取当前20级像素坐标
	 * @return 返回当前20级像素坐标
	 */
	public Coord20 getMyLocationCoord20() {
		return myLocationCoord20;	
	}

	/**
	 * 获取当前GPS偏转后位置
	 * @return 返回当前GPS偏转后位置信息
	 */
	public CoordDeflect getMyLocationDeflect() {
		return myLocationCoordDeflect;	
	}
	
	/**
	 * 当前GPS是否可用，GPS未更新时间超过10秒则不可用
	 * @return 可用为true，不可用为false
	 */
	public boolean isGPSDataAvailable() {
//		if(SystemClock.elapsedRealtime() - preTimeGPSUpdate > 10000){
//			return false;		
//		}else{
//			return true;
//		}		
		return isGPSAvailable;
	}

	/**
     * 返回方向数值，可进行左右摇摆
     * @return 返回方向数值
     */
	public float getDirection() {
		return directionWithRoll;
	}

	/**
	 * 返回经过处理后的垂直倾斜数值
	 * @return 返回经过处理后的垂直倾斜数值
	 */
	public float getInclination() {
		return inclination;
	}
	
//	/**
//	 * 获取屏幕坐标位置x坐标
//	 * @param poiCoordDeflectX 兴趣点GPS偏转经度
//	 * @param poiCoordDeflectY 兴趣点GPS偏转纬度
//	 * @return 返回屏幕x坐标
//	 */	 
//	public float getScreenX(double poiCoordDeflectX, double poiCoordDeflectY){
//		/*
//		 * 计算兴趣点以用户为原点且相对于正北方的偏移角度。平面坐标系中，经度相当
//		 * 于坐标x，纬度相当于坐标y。由于采用four-quadrant inverse tangent，
//		 * 计算的兴趣点偏移角度poiDirection的范围是[-180, 180]，则需要将该角度的
//		 * 范围先转换成[0, 359]，再根据当前用户水平旋转次数来增加或减少若干360度，
//		 * 使得兴趣点方向和用户方向的差值的绝对值范围保持在[0, 359]，以方便后面的计算。
//		 */	
//		float poiDirection = (float) Math.toDegrees(Math.atan2(
//				poiCoordDeflectX
//				-
////				myLocation[1],
//				myLocationCoordDeflect.getX(), 
//				poiCoordDeflectY
//				-
////				myLocation[0]
//				myLocationCoordDeflect.getY()
//				                            ));
//		if(poiDirection<0&&poiDirection>-180)
//			poiDirection = poiDirection+360;	
//		poiDirection = poiDirection + orientationCounter*360;
//		
//		/* 
//		 * 计算兴趣点在屏幕上的像素横坐标
//		 * 在计算兴趣点的屏幕像素横坐标时，需要求取兴趣点方向和用户朝向的差值
//		 * offsetDirection，正常情况下，该差值的绝对值范围为[0,359]，在屏幕像素横坐标
//		 * 的计算公式中，两者相互靠近， 即当差值缩小到一定范围内时，兴趣点在屏幕上的
//		 * 像素横坐标落在屏幕像素宽度的范围内，此时可见。 但当兴趣点的偏移角度为正北
//		 * 方向附近，且用户的旋转角度也为正北方向附近时，正常情况下兴趣点是可见的，但
//		 * 是在某些特殊情况下，会出问题。例如，当某兴趣点的偏移角度为0，即正北方，用户
//		 * 旋转角度若在-5度，正常情况下是应该可见的。但根据前面的公式：
//		 * poiDirection[i] = poiDirection[i] + an.getOrientationCounter()*360
//		 * ，此时兴趣点的偏移角度为-360，那么两者的差值就成了-360-(-5)=-355度，而该差
//		 * 代入屏幕像素横坐标的计算公式时，计算出来的结果为不可见。所以，在这种情况下就
//		 * 需要对差值处理，加上360，结果为5度，此时代入屏幕像素横坐标的计算公式，计算结果
//		 * 才正确。所以，需要对该差值的范围进行再次限定，这里将该范围的绝对值设定为
//		 * [0, 180]。若两者的差值超过该范围，则需对差值offsetDirection做360度增加或减
//		 * 少，以此限定其范围。
//		 * 屏幕像素横坐标的计算公式：
//		 * 角度差值*二分之一屏幕像素宽度与二分之一屏幕水平视角的比例+二分之一屏幕像素
//		 * 宽度。
//		 */
//		float offsetDirection = poiDirection-directionWithRoll;
//	    if(offsetDirection<-180){
//			offsetDirection += 360;
//		}else if(offsetDirection>180){
//			offsetDirection -= 360;
//		}
//		return offsetDirection*SCREENWIDTH/2/16+SCREENWIDTH/2;
//	}
//	
//	/**
//	 * 获取屏幕坐标位置y坐标
//	 * @return 返回屏幕y坐标
//	 */
//	public float getScreenY(){	
//		/*
//		 * 计算兴趣点在屏幕上的像素纵坐标
//		 * 根据俯仰的角度来计算，规定俯仰角度朝水平方向为0，朝上为正，朝下为负，
//		 * 取值范围[-90， 90]。公式为：
//		 * 俯仰角度*二分之一屏幕像素高度与二分之一屏幕垂直视角的比例+二分之一屏
//		 * 幕像素高度
//		 */
//		return -inclination*SCREENHEIGHT/2/25+SCREENHEIGHT/2;
//	}	
	
	/**
	 * 返回屏幕x、y坐标
	 * @param poiCoordDeflectX 兴趣点GPS偏转经度
	 * @param poiCoordDeflectY 兴趣点GPS偏转纬度
	 * @return 返回屏幕x、y坐标，数组[0]=x坐标，数组[1]=y坐标
	 */
	public float[] getScreenXY(double poiCoordDeflectX, double poiCoordDeflectY){
		/*
		 * 计算兴趣点以用户为原点且相对于正北方的偏移角度。平面坐标系中，经度相当
		 * 于坐标x，纬度相当于坐标y。由于采用four-quadrant inverse tangent，
		 * 计算的兴趣点偏移角度poiDirection的范围是[-180, 180]，则需要将该角度的
		 * 范围先转换成[0, 359]，再根据当前用户水平旋转次数来增加或减少若干360度，
		 * 使得兴趣点方向和用户方向的差值的绝对值范围保持在[0, 359]，以方便后面的计算。
		 */			
		float poiDirection = (float) Math.toDegrees(Math.atan2(
				poiCoordDeflectX - myLocationCoordDeflect.getX(), 
				poiCoordDeflectY - myLocationCoordDeflect.getY()));
		if(poiDirection<0&&poiDirection>-180)
			poiDirection = poiDirection+360;	
		poiDirection = poiDirection + orientationCounter*360;
		
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
		float offsetDirection = poiDirection-directionWithRoll;
	    if(offsetDirection<-180){
			offsetDirection += 360;
		}else if(offsetDirection>180){
			offsetDirection -= 360;
		}
	    float poiScreenXOriginal = offsetDirection*SCREENWIDTH/2/16+SCREENWIDTH/2;
	    /*
		 * 计算兴趣点在屏幕上的原始像素纵坐标
		 * 根据俯仰的角度来计算，规定俯仰角度朝水平方向为0，朝上为正，朝下为负，
		 * 取值范围[-90， 90]。公式为：
		 * 俯仰角度*二分之一屏幕像素高度与二分之一屏幕垂直视角的比例+二分之一屏
		 * 幕像素高度
		 */
	    float poiScreenYOriginal = -inclination*SCREENHEIGHT/2/25+SCREENHEIGHT/2;
		
	    // 转换成以屏幕中心为原点的坐标系
	    float x = poiScreenXOriginal - SCREENWIDTH/2;
	    float y = -(poiScreenYOriginal - SCREENHEIGHT/2);
	    // 兴趣点在屏幕上的坐标离屏幕原点的距离
	    float radius = (float) Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2));
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
	    float poiScreenAngleRoll = poiScreenAngleOriginal - roll;
	    
	    // 极坐标系与平面直角坐标系（笛卡尔坐标系）间转换
	    x = (float) (radius * Math.cos(Math.toRadians(poiScreenAngleRoll)));
	    y = (float) (radius * Math.sin(Math.toRadians(poiScreenAngleRoll)));
	    
	    // 转换成屏幕坐标系
	    float[] screenXY = {x + SCREENWIDTH/2, - (y - SCREENHEIGHT/2)};    
	    
	    return screenXY;		
	}
	
	/** 
     * 根据两点经纬度计算距离，单位：米
     * @param lngDeflect1 第一个点的偏转经度
     * @param latDeflect1 第一个点的偏转纬度
     * @param lngDeflect2 第二个点的偏转经度
     * @param latDeflect2 第二个点的偏转纬度
     * @return 返回距离，单位：米
     */
	public double getDistance(double lngDeflect1, double latDeflect1, double lngDeflect2, double latDeflect2)
    {
        double radLat1 = Math.toRadians(latDeflect1);
        double radLat2 = Math.toRadians(latDeflect2);
        double a = radLat1 - radLat2;
        double b = Math.toRadians(lngDeflect1) - Math.toRadians(lngDeflect2);
        double s = 2 * Math.asin(Math.sqrt(Math.pow(Math.sin(a/2),2) + 
        		Math.cos(radLat1)*Math.cos(radLat2)*Math.pow(Math.sin(b/2),2)));
       s = s * EARTH_RADIUS;
       return 1000*s;
    }

	/**
	 * 注册地理位置和传感器监听
	 * @param locationUpdateTime 地理位置更新时间
	 */
	private void registerListener(int locationUpdateTime) {
		locMan.requestLocationUpdates(LocationManager.GPS_PROVIDER, 
				locationUpdateTime, 1, gpsListener);
    	sensorMan.registerListener(sensorListener, 
        		sensorMan.getDefaultSensor(Sensor.TYPE_ORIENTATION),
        		SensorManager.SENSOR_DELAY_FASTEST);
        sensorMan.registerListener(sensorListener, 
        		sensorMan.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
        		SensorManager.SENSOR_DELAY_FASTEST);
	}
	
	/**
	 * 销毁地理位置和传感器监听
	 * 如果某了Activity中销毁了地理位置和传感器监听，那么传入的另一个Activity
	 * 中就无法使用
	 */
	private void unRegisterListener() {
		sensorMan.unregisterListener(sensorListener);
    	locMan.removeUpdates(gpsListener);
	}
	
	/**
	 * 在activity已经停止，重新开始的时候调用
	 * 会重新注册地理位置和传感器监听
	 */
	public void onRestart() {
		registerListener(locationUpdateTime);
	}

	/**
	 * 在activity要停止的时候调用
	 * 会销毁地理位置和传感器监听
	 * 如果某了Activity中销毁了地理位置和传感器监听，那么传入的另一个Activity
	 * 中就无法使用 
	 */
	public void onStop() {
		unRegisterListener();
	}
	
//	/**
//	 * 结束当前的这个应用程序(有多个Activity)<br>
//	 * 注意：在AndroidManifest.xml中添加权限 &lt;uses-permission android:name="android.permission.RESTART_PACKAGES" /&gt;
//	 */
//	public void onDestroy() {
//		((ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE)).restartPackage(context.getPackageName());
//	}
}