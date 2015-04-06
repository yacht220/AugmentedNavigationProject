/*
 * 文件名：	MapNavigation.java
 * 日期：	2010-3-23
 * 修改历史：
 * [时间]		[修改者]			[修改内容]
 */
package com.lenovo.map;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

import com.lenovo.arfindertest.ARFinderTest;
import com.lenovo.arfindertest.R;
import com.lenovo.minimap.HttpClientUtil;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SoundEffectConstants;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnTouchListener;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * 版权所有(c)联想集团有限公司 1998-2010 保留所有权利.	<br />
 * 项目：	<br />
 * 描述：	<br />
 * @author	zhangguojun<br />
 * @version	1.0
 * @since	JDK1.6
 */
public class MapNavigation extends Activity {
	private static String TAG = "MapNavigation";
	private static String[] URLS = {"http://wap.mapabc.com/wap/show.jsp?", "&extra=png&picw=248&pich=350&key=LeNoVo&uid="};
	private OnTouchListener touchListener;
	private ImageView mapView;
	private ImageView zoom_deflate;
	private ImageView zoom_blow;
	private ImageButton back;
//	private ImageView compass_pointer;
	private CompassPointerView compassPointerView;
	private TextView text;
	private LocationManager locationManager;
	private LocationListener locationListener;
	private int locationUpdateTime = 3000;
	private int zoom = 5;//地图缩放级别（1到9，值越小地图越详细）
	private double longitude;
	private double latitude;
	private int picLength;
	private Handler updateMapHandler;
	/* 传感器 */
	private float prevals = 0; // 前一个罗盘方向读数
	private int orientationCounter = 0; // 水平旋转次数，[0, 359]为0，增加360度加一，减少360度减一
	private static volatile float direction = 0;
	private static volatile float kFilteringFactor = (float)0.05;
	private SensorManager sensorManager;
	private SensorEventListener sensorEventListener;
	private Handler updateCompassPointerHandler;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,  WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,  WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		setContentView(R.layout.map);
		
		mapView = (ImageView) findViewById(R.id.mapView);
		zoom_deflate = (ImageView) findViewById(R.id.zoom_deflate);
		zoom_blow = (ImageView) findViewById(R.id.zoom_blow);
		back = (ImageButton) findViewById(R.id.back);
//		compass_pointer = (ImageView) findViewById(R.id.compass_pointer);
		compassPointerView = (CompassPointerView) findViewById(R.id.compassPointerView);
		text = (TextView) findViewById(R.id.text);
		
		touchListener = new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				switch (v.getId()) {
				case R.id.back:
					startActivity(new Intent(MapNavigation.this, ARFinderTest.class));
					break;
				case R.id.zoom_deflate://缩小
					if(zoom < 9) {zoom++;}
					updateMap();
					zoom_deflate.playSoundEffect(SoundEffectConstants.CLICK);// 播放声音需要在layout/main.xml中的该View中设置android:soundEffectsEnabled="true"
					break;
				case R.id.zoom_blow://放大
					if(zoom > 1) {zoom--;}
					updateMap();
					zoom_blow.playSoundEffect(SoundEffectConstants.CLICK);// 播放声音需要在layout/main.xml中的该View中设置android:soundEffectsEnabled="true"
					break;
				default:
					break;
				}
				return false;
			}			
		};
		zoom_deflate.setOnTouchListener(touchListener);
		zoom_blow.setOnTouchListener(touchListener);
		back.setOnTouchListener(touchListener);
		
		locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		locationListener = new LocationListener(){
	    	public void onLocationChanged(Location location){
	    		if(location != null) {
	    			longitude = location.getLongitude();
		    		latitude = location.getLatitude();
		    		text.setText("经度 = " + longitude + "\n纬度 = " + latitude + "\n图片大小 = " + picLength);
		    		Log.e(TAG, "longitude = " + longitude + ", latitude + " + latitude);
		    		updateMap();
	    		}
	    	}
	    	
	    	public void onProviderDisabled(String provider){// Provider被disable时触发此函数，比如GPS被关闭
	    	}
	    	
	    	public void onProviderEnabled(String provider){ //  Provider被enable时触发此函数，比如GPS被打开
	    	}
	    	
	    	public void onStatusChanged(String provider, int status, Bundle extras){// Provider的转态在可用、暂时不可用和无服务三个状态直接切换时触发此函数
	    	}
	    };
	    
	    sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
	    sensorEventListener = new SensorEventListener() {
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
					 * filter，减少抖动。direction的范围和rawDirection一样。
					 * 还有其它的一些filter方法，但效果不理想。例如间隔若干时间
					 * 读取罗盘度数；前方向值和罗盘比较，大则++，小则--；360度分
					 * 成n块，每块只对应一种度数，即floor(vals[0]/n)*n；求若干时
					 * 间段内罗盘读书的平均值。
					 * direction的范围为正负无穷
					 */
					//direction:经过处理后的罗盘方向数值，单位：度
					//direction:值范围[-∞, +∞]，0.0表示朝正北，以360度为周期，读数按顺时针递增，也就是90.0为正东，270.0为正西
					direction =(float)((rawDirection * kFilteringFactor) + (direction * (1.0 - kFilteringFactor)));
					updateCompassPointer();
				}
			}
			
			@Override
			public void onAccuracyChanged(Sensor sensor, int accuracy) {
			}
		};
		
	    registerListener();
	    updateCompassPointer();
	    updateMap();
	}
	
	private void updateCompassPointer() {
		updateCompassPointerHandler = new Handler() {
            @Override
			public void handleMessage(Message msg) {
				super.handleMessage(msg);
				// 旋转图片
				compassPointerView.update();
				Log.e(TAG, "updateCompassPointer OK, direction=" + direction);
			}
       };
       updateCompassPointerHandler.sendEmptyMessage(0);
	}
	
	private synchronized void updateMap() {
		updateMapHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
//        		longitude = 116.29200518131256;//联想研究院西门
//        		latitude = 40.05170524120331;
        		if(longitude == 0 || latitude == 0) {
        			return ;
        		}
        		String picUrl = getLocationMapPicUrl(longitude, latitude, zoom);
        		Log.e(TAG, "picUrl = " + picUrl);
        		InputStream is = null;
        		try {
        			URL url = new URL(picUrl);
        			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        			conn.setDoInput(true);
        			conn.connect();
        			is = conn.getInputStream();
        			picLength = conn.getContentLength();
        			Log.e(TAG, "conn.getContentLength() = " + picLength);
        			Bitmap bm = BitmapFactory.decodeStream(is);
//        			bm = BitmapFactory.decodeResource(getResources(), R.drawable.map);
        			mapView.setImageBitmap(bm);
        			mapView.setScaleType(ImageView.ScaleType.FIT_XY);
        			text.setText("经度 = " + longitude + "\n纬度 = " + latitude + "\n图片大小 = " + picLength);
        		} catch (Exception e) {
        			e.printStackTrace();
				} finally {
					try {
						if (is != null) {
							is.close();
							is = null;
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
        		Log.e(TAG, "updateMap OK");
            }
       };
       updateMapHandler.sendEmptyMessage(0);
	}
	
	private String getLocationMapPicUrl(double longitude, double latitude, int zoom) {
		String result = null;
		try {
			String url = URLS[0] + "cenx=" + longitude + "&ceny=" + latitude + "&tiplabel=" + URLEncoder.encode("我在这", "GBK")+ "&x=&y=&name=&zoom=" + zoom + URLS[1];
			System.out.println("url = " + url);
			result = HttpClientUtil.getStringResultForHttpGet(url);//"10.99.60.201:8080"
			if(result == null || result.indexOf("error") > -1) {
				System.out.println("Input date error Or HttpClient Exception!");
				return null;
			}
			result = result.substring(result.indexOf("<mapurl>") + "<mapurl>".length(), result.indexOf("</mapurl>")).trim();
			System.out.println("result = " + result);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}
	
	private static class CompassPointerView extends View {
		public CompassPointerView(Context context, AttributeSet attrs) {
			super(context, attrs);
		}
		public void update() {
			postInvalidate();
		}
		protected void onDraw(Canvas canvas) {
			super.onDraw(canvas);
			Matrix compassPointerMatrix = new Matrix();
			
//			compassPointerMatrix.postRotate(400, 12, 22);
//			compassPointerMatrix.postTranslate(423, 25);
			
			compassPointerMatrix.postRotate(-direction, 12, 21);
			compassPointerMatrix.postTranslate(262, 21);
			Paint paint = new Paint();
			paint.setAntiAlias(true);
			canvas.drawBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.map_compass_pointer), compassPointerMatrix, paint);
			compassPointerMatrix.postTranslate(-262, -21);
			compassPointerMatrix.postRotate(direction, 12, 21);
			
		}
	}
	
	/**
	 * 注册地理位置和传感器监听
	 * @param locationUpdateTime 地理位置更新时间
	 */
	private void registerListener() {
		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, locationUpdateTime, 1, locationListener);
		sensorManager.registerListener(sensorEventListener, sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION), SensorManager.SENSOR_DELAY_FASTEST);
		sensorManager.registerListener(sensorEventListener, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_FASTEST);
	}
	
	/**
	 * 销毁地理位置和传感器监听
	 */
	private void unRegisterListener() {
		locationManager.removeUpdates(locationListener);
	}
	
	/**
	 * 在activity已经停止，重新开始的时候调用
	 */
	public void onRestart() {
		super.onRestart();
		registerListener();
	}
	
	/**
	 * 在activity要停止的时候调用
	 */
	public void onStop() {
		super.onStop();
		unRegisterListener();
	}
	
	/**
	 * 结束当前的这个应用程序(有多个Activity)<br>
	 * 注意：在AndroidManifest.xml中添加权限 &lt;uses-permission android:name="android.permission.RESTART_PACKAGES" /&gt;
	 */
	public void onDestroy() {
		super.onDestroy();
		((ActivityManager) getSystemService(Context.ACTIVITY_SERVICE)).restartPackage(getPackageName());
	}
}