package com.yacht.android.augmentednavigation;

import android.app.Activity;
import android.content.Context;
import android.graphics.PixelFormat;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;

public class AugmentedNavigation extends Activity {    
	/** CameraView实例，摄像头对象 */
	private CameraView cameraView;
	/** TextView实例，罗盘读数显示 */
	private TextView orientationValueText;
	/** TextView实例，加速度计读数显示 */
	private TextView accelerometerValueText;
	/** TextView实例，方向和俯仰数值显示 */
	private TextView poseText;
	/** TextView实例，GPS读数显示 */
	private TextView gpsValueText;
	/** GLSurfaceView实例，OpenGL对象 */
	private GLSurfaceView glSurface;
	/** CanvasView实例，2D图形渲染对象 */
	private CanvasView canvasDraw;	
	/** 屏幕像素宽度 */
	private static int SCREENWIDTH;
	/** 屏幕像素高度 */
	private static int SCREENHEIGHT;
	/** 通过左右摇摆进行修正了的方向数值，取值范围[-∞, +∞]，0.0表示朝正北，以360度为周期，读数按顺时针递增，逆时针递减 */
	private float directionWithRoll = 0;
	/** 未通过左右摇摆进行修正了的方向数值，取值范围[-∞, +∞]，0.0表示朝正北，以360度为周期，读数按顺时针递增，逆时针递减 */
	private float directionWithoutRoll = 0;
	/** 俯仰数值，取值范围[-90.0, +90.0]，0.0表示手机屏幕朝内垂直，-90.0表示手机屏幕朝下水平，+90.0表示手机屏幕朝上水平 */
	private float inclination = 0;
	/** 左右摆动数值，取值范围[-90.0, +270.0]，0.0表示手机竖直垂直，[-90.0, 0)表示手机朝右上摆动，(0, +90.0]表示手机朝左上摆动，(+90, +180]朝左下摆动，(+180, +270]朝右下摆动 */
	private float roll = 0;
	/** 过滤系数 */
    private static float KFILTERINGFACTOR = (float)0.05;
    /** 前一个罗盘方向读数 */
    private float prevals = 0;
    /** 水平旋转次数，[0, 359]为0，增加360度加一，减少360度减一 */
    private int orientationCounter = 0;
    /** 用户当前GPS位置信息，纬度、经度、海拔 */
    private double[] myLocation = {40.053150, 116.292311, 40.0};
    /** 兴趣点GPS位置信息 */
    private double[][] poiLocation = new double[5][3];
    /** 兴趣点名称 */
    private String[] poiName = new String[5];
    /** 路径点GPS位置信息 */
    private double[][] route = new double[8][3];
    /** 是否渲染标识 */
    private boolean isDraw = true;    
	/** 传感器监听器 */   
	private SensorEventListener sensorListener = new SensorEventListener() {
		
		@Override
		public void onSensorChanged(SensorEvent event) {
			float vals[] = event.values; // 传感器原始读数
			
			/*
			 *  保证对象canvasDraw的onDraw()方法循环渲染。若没有这语句，则仅进行第一次渲染，
			 *  之后不再渲染。
			 *  与传感器同步渲染。
			 */			
			if (canvasDraw != null) {
				canvasDraw.invalidate();
            }
			
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
				float rawDirection = orientationCounter*360+vals[0]; // 罗盘方向原始读数
				prevals = vals[0];
				
				/* 
				 * filter，减少抖动。directionWithoutRoll的范围和rawDirection一样。
				 * 还有其它的一些filter方法，但效果不理想。例如间隔若干时间
				 * 读取罗盘度数；前方向值和当前罗盘方向读数比较，大则++，小则
				 * --；360度分成n块，每块只对应一种度数，即floor(vals[0]/n)*n；
				 * 求若干时间段内罗盘读数的平均值。
				 * directionWithoutRoll的范围为正负无穷
				 */
				directionWithoutRoll =(float)((rawDirection * KFILTERINGFACTOR) + 
						(directionWithoutRoll * (1.0 - KFILTERINGFACTOR)));
				
				// 通过左右摇摆数值对方向数值进行修正
				directionWithRoll = directionWithoutRoll + roll;
				
				orientationValueText.setText("orientation:\nZ:"+vals[0]+
						" X:"+vals[1]+" Y:"+vals[2]+" rawDir:"+rawDirection);
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
				 * 当左右摇摆数值在(-45, 45)或(135, 225)之间时，俯仰角度采用手机竖直的方式进行计算，
				 * 当左右摇摆数值在[225, 270]，或[-90, -45]，或[45， 135]之间时，俯仰角度采用手机横向的方式
				 * 进行计算
				 */
				float rawInclination;
				if(rawRoll>-45&&rawRoll<45){ // 竖直
					rawInclination = (float) (Math.toDegrees(Math.atan(vals[2]/vals[1])));
				}else if(rawRoll>135&&rawRoll<225){ // 竖直
					rawInclination = -(float) (Math.toDegrees(Math.atan(vals[2]/vals[1])));
				}else if((rawRoll>=225&&rawRoll<=270)||(rawRoll>=-90&&rawRoll<=-45)){ // 横向
					rawInclination = -(float) (Math.toDegrees(Math.atan(vals[2]/vals[0])));
				}else{ // rawRoll在[45, 135]之间，横向
					rawInclination = (float) (Math.toDegrees(Math.atan(vals[2]/vals[0])));
				}
				/* 
				 * filter，减少抖动
				 * -90<=inclination<=90
				 */
				inclination = (float)((rawInclination * KFILTERINGFACTOR) + 
						(inclination * (1.0 - KFILTERINGFACTOR)));				
				
				accelerometerValueText.setText("\n\n\naccelerometer:\nx:"+
				vals[0]+" y:"+vals[1]+" z:"+vals[2]+" rawInc:"+rawInclination+" rawRol:"+rawRoll);
	         }
			
			poseText.setText("\n\n\n\n\n\n\ndir:"+directionWithRoll+" inc:"+inclination+" rol:"+roll);		
		}
		
		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {}
	};
    /** GPS监听器 */
	private final LocationListener gpsListener = new LocationListener(){
    	// Location发生变化时被调用
    	public void onLocationChanged(Location location){
    		String latLngAltString;
    		if(location != null){
    			myLocation[0] = location.getLatitude();
    			myLocation[1] = location.getLongitude();
    			myLocation[2] = location.getAltitude();
    			
    			latLngAltString = "latitude:"+myLocation[0]+"\nlongitude:"+myLocation[1]
    			                +"\naltitude:"+myLocation[2];
    			isDraw = true;
    		}else{
    			latLngAltString = "cannot obtain location info.";
    			isDraw = false;
    		}
    		gpsValueText.setText("\n\n\n\n\n\n\n\n\nposition:\n"+latLngAltString);
    	}
    	
    	public void onProviderDisabled(String provider){isDraw = false;}
    	
    	public void onProviderEnabled(String provider){isDraw = true;}
    	
    	public void onStatusChanged(String provider, int status, Bundle extras){}
    };
    /** SensorManager实例 */
	private SensorManager sensorMan;
	/** LocationManager实例 */
//	private LocationManager locMan;
	
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {    	
    	try{
    		super.onCreate(savedInstanceState); 
    		
            // 隐藏抬头
            requestWindowFeature(Window.FEATURE_NO_TITLE);
            
            // 全屏
            //getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
            
            // 禁止休眠
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,  WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            
    		FrameLayout rl = new FrameLayout(this.getApplicationContext()); 
    		setContentView(rl);  
//    		setContentView(R.layout.main);
    		
    		/* 获取手机屏幕像素大小 */
			Display display = getWindowManager().getDefaultDisplay();
			SCREENWIDTH = display.getWidth(); 
			SCREENHEIGHT = display.getHeight();

    		/*
    		 *  兴趣点
    		 */
    		// 上地地铁站
    		poiLocation[0][0] = 40.03150284290314; // 纬度
    		poiLocation[0][1] = 116.31354331970215; // 经度
    		poiLocation[0][2] = 57.0; // 海拔
    		poiName[0] = "上地地铁站";
    		// 西二旗地铁站
    		poiLocation[1][0] = 40.05035877227783; // 纬度
    		poiLocation[1][1] = 116.30049705505371; // 经度
    		poiLocation[1][2] = 39.0; // 海拔
    		poiName[1] = "西二旗地铁站";
    		// 辉煌国际路口
    		poiLocation[2][0] = 40.0519198179245; // 纬度
    		poiLocation[2][1] = 116.29707992076874; // 经度
    		poiLocation[2][2] = 33.0; // 海拔
    		poiName[2] = "辉煌国际路口";		
    		// 联想南门
    		poiLocation[3][0] = 40.05169987678528; // 纬度
    		poiLocation[3][1] = 116.29452109336853; // 经度
    		poiLocation[3][2] = 58.0; // 海拔
    		poiName[3] = "联想南门";
    	    // 联想西门
    		poiLocation[4][0] = 40.05170524120331; // 纬度
    		poiLocation[4][1] = 116.29200518131256; // 经度	
    		poiLocation[4][2] = 43.0; // 海拔
    		poiName[4] = "联想西门";
    		
    		/*
    		 *  路径
    		 */
    	    // 联想北门
    		route[0][0] = 40.054001212120056; // 纬度
    		route[0][1] = 116.29382908344269; // 经度	
    		route[0][2] = 44.0; // 海拔
    		
    		route[1][0] = 40.05365788936615; // 纬度
    		route[1][1] = 116.29333019256592; // 经度
    		route[0][2] = 43.0; // 海拔
    		
    		route[2][0] = 40.05337357521057; // 纬度
    		route[2][1] = 116.29286348819733; // 经度
    		route[0][2] = 41.0; // 海拔
    		
    		route[3][0] = 40.05313754081726; // 纬度
    		route[3][1] = 116.29228949546814; // 经度  
    		route[0][2] = 40.0; // 海拔
    		
    		route[4][0] = 40.05293369293213; // 纬度
    		route[4][1] = 116.29183351993561; // 经度  
    		route[0][2] = 38.0; // 海拔
    		
    		route[5][0] = 40.052729845047; // 纬度
    		route[5][1] = 116.29172623157501; // 经度  
    		route[0][2] = 36.0; // 海拔
    		
    		route[6][0] = 40.052273869514465; // 纬度
    		route[6][1] = 116.29188179969788; // 经度   
    		route[0][2] = 38.0; // 海拔
    		
    		// 联想西门
    		route[7][0] = 40.05170524120331; // 纬度
    		route[7][1] = 116.29200518131256; // 经度 
    		route[0][2] = 43.0; // 海拔   		
    		
    		/*locMan = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
    		locMan.requestLocationUpdates(LocationManager.GPS_PROVIDER, 
    				500, 1, gpsListener);*/
    		
        	sensorMan = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
            sensorMan.registerListener(sensorListener, 
            		sensorMan.getDefaultSensor(Sensor.TYPE_ORIENTATION),
            		SensorManager.SENSOR_DELAY_FASTEST);
            sensorMan.registerListener(sensorListener, 
            		sensorMan.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
            		SensorManager.SENSOR_DELAY_FASTEST);
    		
            orientationValueText = new TextView(this.getApplicationContext());
            accelerometerValueText = new TextView(this.getApplicationContext());
            poseText = new TextView(this.getApplicationContext());
            gpsValueText = new TextView(this.getApplicationContext());
            cameraView = new CameraView(this.getApplicationContext());
    		canvasDraw = new CanvasView(this.getApplicationContext(), this);
    		//canvasDraw = (CanvasView)findViewById(R.id.canvasview);
    		
    		glSurface = new GLSurfaceView(this.getApplicationContext());     
    		// We want an 8888 pixel format because that's required for
            // a translucent window.
            // And we want a depth buffer and a stencil buffer.
    		// 在setRenderer()之前调用
    		// 在其他View上叠加时，必须的代码
    		glSurface.setEGLConfigChooser(8, 8, 8, 8, 16, 4); 
    		glSurface.setRenderer(new GLRender(this, canvasDraw)); 
    		// Use a surface format with an Alpha channel.
    		// 在其他View上叠加时，必须的代码
    		glSurface.getHolder().setFormat(PixelFormat.TRANSLUCENT);

    		// 注意添加顺序，否则有的View会被覆盖
    	    rl.addView(glSurface);    		
    		rl.addView(cameraView);    
//    		rl.addView(canvasDraw);
//    		rl.addView(orientationValueText);
//    		rl.addView(accelerometerValueText);
//    		rl.addView(poseText);
//    		rl.addView(gpsValueText); 
            
            Log.d("Activity", "onCreate()");
    	}catch(Exception e){Log.d("Exception", e.getMessage());}
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
     * 返回是否可以进行渲染
     * @return 可以为true，不可以为false
     */
    public boolean isDrawEnabled(){
    	return isDraw;
    }
    
    /**
     * 返回兴趣点的经纬度及海拔
     * @return 返回兴趣点的经纬度及海拔
     */
    public double[][] getPOILocation(){
    	return poiLocation;
    }
    
    /**
     * 返回兴趣点名称
     * @return 返回兴趣点名称
     */
    public String[] getPOIName(){
    	return poiName;
    }
    /**
     * 返回用户当前位置的经纬度及海拔
     * @return 返回用户当前位置的经纬度及海拔
     */
    public double[] getMyLocation(){
    	return myLocation;
    }
    
    /**
     * 返回水平旋转次数
     * @return 返回水平旋转次数
     */
    public int getOrientationCounter(){
    	return orientationCounter;
    }
    
    /**
     * 返回方向数值，可进行左右摇摆，取值范围[-∞, +∞]
     * @return 返回方向数值
     */
    public float getDirection(){
    	return directionWithRoll;
    }
    
    /**
     * 返回俯仰数值，取值范围[-90.0, +90.0]
     * @return 返回俯仰数值
     */
    public float getInclination(){
    	return inclination;
    } 
    
    /**
     * 返回左右摆动数值，取值范围[-90.0, +270.0]
     * @return 返回左右摆动数值
     */
    public float getRoll(){
    	return roll;
    }
    
    /**
     * 返回路径
     * @return 返回路径
     */
    public double[][] getRoute(){
    	return route;
    }
    
    /** 
     * 根据两点经纬度计算距离，公里制单位
     * @param lat1 第一个点的纬度
     * @param lng1 第一个点的经度
     * @param lat2 第二个点的纬度
     * @param lng2 第二个点的经度
     * @return 返回距离，公里制单位
     */
    public double getDistanceWithoutAltitude(double lat1, double lng1, double lat2, double lng2)
    {
        return canvasDraw.getDistanceWithoutAltitude(lat1, lng1, lat2, lng2);
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
    public double getDistanceWithAltitude(double lat1, double lng1, double alt1, double lat2, double lng2, double alt2)
    {
        return canvasDraw.getDistanceWithAltitude(lat1, lng1, alt1, lat2, lng2, alt2);
    }    
    
    @Override
    public void onStart(){
    	super.onStart();
    	Log.d("Activity", "onStart()");
    }
    
    @Override
    public void onRestart(){
    	super.onRestart();
    	/*locMan.requestLocationUpdates(LocationManager.GPS_PROVIDER, 
				500, 1, gpsListener);*/
    	sensorMan.registerListener(sensorListener, 
        		sensorMan.getDefaultSensor(Sensor.TYPE_ORIENTATION),
        		SensorManager.SENSOR_DELAY_FASTEST);
        sensorMan.registerListener(sensorListener, 
        		sensorMan.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
        		SensorManager.SENSOR_DELAY_FASTEST);
    	Log.d("Activity", "onRestart()");
    }
    
    @Override
    public void onResume(){
    	super.onResume();
    	glSurface.onResume();
    	Log.d("Activity", "onResume()");
    }
    
    @Override
    public void onPause(){
    	super.onPause();
    	glSurface.onPause();
    	Log.d("Activity", "onPause()");
    }
    
    @Override
    public void onStop(){
    	super.onStop();
    	sensorMan.unregisterListener(sensorListener);
//    	locMan.removeUpdates(gpsListener);
    	Log.d("Activity", "onStop()");    	
    }
    
    @Override
    public void onDestroy(){
    	super.onDestroy();
    	Log.d("Activity", "onDestroy()"); 
    	// After this is called, your app process is no longer available in DDMS  
    	android.os.Process.killProcess(android.os.Process.myPid());  
    }
}