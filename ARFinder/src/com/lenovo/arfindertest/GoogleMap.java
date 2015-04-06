/*
 * 文件名：	GoogleMap.java
 * 日期：	2010-3-26
 * 修改历史：
 * [时间]		[修改者]			[修改内容]
 */
package com.lenovo.arfindertest;

import com.lenovo.android.navigator.CameraActivity;
import com.lenovo.android.navigator.CompassView;
import com.lenovo.android.navigator.R;
import com.lenovo.android.navigator.Util;
import com.lenovo.map.GeoItemizedOverlay;
import com.lenovo.map.MyOverLay;
import com.lenovo.minimap.MinimapService;
import com.lenovo.minimap.dto.Coord20;
import com.lenovo.minimap.dto.CoordDeflect;
import com.lenovo.minimap.dto.CoordGps;
import com.lenovo.minimap.search.AroundSearch;
import com.lenovo.minimap.search.RouteSearch;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import java.text.DecimalFormat;
import java.util.List;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SoundEffectConstants;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;
import com.google.android.maps.ItemizedOverlay.OnFocusChangeListener;

/**
 * 版权所有(c)联想集团有限公司 1998-2010 保留所有权利. <br />
 * 项目： <br />
 * 描述： <br />
 * 
 * @author zhangguojun<br />
 * @version 1.0
 * @since JDK1.6
 */
public class GoogleMap extends MapActivity {
	private static String TAG = "GoogleMap";
	private TextView text;
	private ImageView zoom;
	private ImageView zoom_deflate;
	private ImageView zoom_blow;
	private ImageButton back;
	private CompassView compassView;
	private OnTouchListener touchListener;
	/** 信息卡布局 */
	private View infoCard;
	/** 详细信息布局 */
	private View infoDetailView;
	/** 详细信息中的返回按钮 */
	private Button closeDetailBtn;
	/** 信息卡上的文字 */
	private TextView infoText;
	
	private MapView gMapView;
	private MapController mapController;
	private LocationManager locationManager;
	private LocationListener locationListener;
	private Location location;
	private int zoomLevel = 17;//设置默认的放大层级
	private double longitude;
	private double latitude;
	private OnFocusChangeListener onFocusChangeListener;
	private boolean isWalkRoute = false;
	private int currentOverlayIndex = -2;//被点击的Overlay
	private GeoPoint g3;
	
	public static List<Overlay> overlays;
	private GeoItemizedOverlay itemizedOverlay;
	public static int locationOverLayId  = -2;//用户坐标在List<Overlay>中的id
	
	private GeoPoint geoPointFirst;//初始化的第一个坐标点
	private GeoPoint geoPointCurrent;//不断更新的坐标点
	private double distance = 0;
	
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
	private SensorManager sensorManager;
	private SensorEventListener sensorEventListener;
//	private Handler updateCompassPointerHandler;
	public static Handler updateLocationChangedGoogleMapHandler;
	private int locationUpdateTime = 500;
	private SensorEvent sensorEvent;
	private MinimapService service = null;
	
//	private SearchAroundTask searchAroundTask;
	private SearchWalkRouteTask searchWalkRouteTask;
	private CoordGps2CoordDeflectTask coordGps2CoordDeflectTask;
	public static List<AroundSearch.Around> arounds;
	
	/** 滑动距离临界值 */
	private static final int FLING_THRESHOLD = 30;
	 /** 滑动中View里最后一个x值 */
	private float lastMotionX;
	/** 信息卡中导航按钮 */
	private Button infoCardNavBtn;
	/** 详细信息中导航按钮 */
	private Button infoDetailNavBtn;
	/** 用于地图模式到实景模式的切换 */
	private Handler mapToCameraHandler;
    /** 仰角是否大于30度的Flag */
	private boolean isGreaterThan30 = false;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        // 禁止休眠
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,  WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		setContentView(R.layout.map_google);
		service = new MinimapService(this);
		
		zoom = (ImageView) findViewById(R.id.zoom);
		zoom_deflate = (ImageView) findViewById(R.id.zoom_deflate);//缩小地图按钮
		zoom_blow = (ImageView) findViewById(R.id.zoom_blow);//放大地图按钮
		back = (ImageButton) findViewById(R.id.back);//返回按钮
		compassView = (CompassView) findViewById(R.id.compassView);//电子罗盘指针
		
		
		
		gMapView = (MapView) findViewById(R.id.gmapView);
		mapController = gMapView.getController();
		overlays = gMapView.getOverlays();
		mapController.setZoom(zoomLevel);
		text = (TextView) findViewById(R.id.text);
		
		infoCard = (View) findViewById(R.id.infocard_panel);
        infoDetailView = (View) findViewById(R.id.detail_panel);
        closeDetailBtn = (Button) findViewById(R.id.go_back);
        
        infoCardNavBtn = (Button) findViewById(R.id.info_card_to);
        infoCardNavBtn.setVisibility(View.INVISIBLE);
        infoDetailNavBtn = (Button) findViewById(R.id.info_to);
        infoDetailNavBtn.setVisibility(View.INVISIBLE);
		
		touchListener = new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				float x;
				switch (v.getId()) {
					case R.id.back:
						startActivity(new Intent(GoogleMap.this, CameraActivity.class));
						break;
					case R.id.zoom_deflate://缩小
						zoomLevel--;
						if (zoomLevel < 1) {
							zoomLevel = 1;
						}
						mapController.setZoom(zoomLevel);
						zoom_deflate.playSoundEffect(SoundEffectConstants.CLICK);// 播放声音需要在layout/main.xml中的该View中设置android:soundEffectsEnabled="true"
						break;
					case R.id.zoom_blow://放大
						zoomLevel++;
						if (zoomLevel > gMapView.getMaxZoomLevel()) {
							zoomLevel = gMapView.getMaxZoomLevel();
						}
						mapController.setZoom(zoomLevel);
						zoom_blow.playSoundEffect(SoundEffectConstants.CLICK);// 播放声音需要在layout/main.xml中的该View中设置android:soundEffectsEnabled="true"
						break;
					case R.id.infocard_panel: 
						infoDetailView.setVisibility(View.VISIBLE);
						zoom.setVisibility(View.INVISIBLE);
						zoom_blow.setVisibility(View.INVISIBLE);
						zoom_deflate.setVisibility(View.INVISIBLE);
						compassView.setVisibility(View.INVISIBLE);
						setInfoDetailText(currentOverlayIndex);
						gMapView.setVisibility(View.INVISIBLE);
						break;
					case R.id.go_back:
						infoDetailView.setVisibility(View.INVISIBLE);
						zoom.setVisibility(View.VISIBLE);
						zoom_blow.setVisibility(View.VISIBLE);
						zoom_deflate.setVisibility(View.VISIBLE);
						compassView.setVisibility(View.VISIBLE);
						gMapView.setVisibility(View.VISIBLE);
						break;
					case R.id.detail_panel:
						x = event.getX();
						switch (event.getAction()) {
							case MotionEvent.ACTION_DOWN:
								Log.d("detail", "ACTION_DOWN");
								lastMotionX = x;
								break;
							case MotionEvent.ACTION_MOVE:
								Log.d("detail", "ACTION_MOVE");
								break;
							case MotionEvent.ACTION_UP:	
								Log.d("detail", "ACTION_UP");
								int flag = 0;
								final int deltaX = (int) (lastMotionX - x);
								if (Math.abs(deltaX) < FLING_THRESHOLD){
									Log.d("detail", "FLING_THRESHOLD");
									return true;
								}							
								if (deltaX < 0) {
									Log.d("detail", "deltaX < 0");
									--flag;						
								} else if (deltaX > 0) {
									Log.d("detail", "deltaX > 0");
									++flag;
								}
								
								AroundSearch.Around point = (AroundSearch.Around) v.getTag();
								int currentAnchorInAllIndex = 0;					
								
								for (int i = 0; i < arounds.size(); i++) {
									if (point.equals(arounds.get(i))) {
										currentAnchorInAllIndex = i;//获得信息点在列表中的下标
									}
								}
								//获得下个显示信息卡的index
								currentAnchorInAllIndex += flag;
								if (currentAnchorInAllIndex < 0) currentAnchorInAllIndex = 0;
								else if (currentAnchorInAllIndex >= arounds.size()) currentAnchorInAllIndex = arounds.size() - 1;
									
								setInfoDetailText(currentAnchorInAllIndex);//设置当前详细页
								break;
						}    					
						break;
					case R.id.gmapView:
						if(currentOverlayIndex != -2) {
							updateOverlayDrawable(currentOverlayIndex, getResources().getDrawable(R.drawable.map_point_blue));
						}
						infoCard.setVisibility(View.INVISIBLE);
						compassView.setVisibility(View.VISIBLE);
					break;
				}
//				return GoogleMap.this.onTouchEvent(event);
				return false;
			}			
		};
		
		zoom_deflate.setOnTouchListener(touchListener);
		zoom_blow.setOnTouchListener(touchListener);
		back.setOnTouchListener(touchListener);
		gMapView.setOnTouchListener(touchListener);
        infoCard.setOnTouchListener(touchListener);
        infoDetailView.setOnTouchListener(touchListener);
        infoDetailView.setFocusable(true);
        infoDetailView.setLongClickable(true);
        closeDetailBtn.setOnTouchListener(touchListener);
        
		onFocusChangeListener = new OnFocusChangeListener() {
			@SuppressWarnings("unchecked")
			@Override
			public void onFocusChanged(ItemizedOverlay itemizedoverlay, OverlayItem overlayitem) {
				Log.e(TAG, "onFocusChanged overlayitem============ " + (overlayitem == null ? "null" : overlayitem.routableAddress()) + "");
				if(overlayitem != null) {
					for (int i = 0; i < overlays.size(); i++) {
						GeoItemizedOverlay temp = (GeoItemizedOverlay) overlays.get(i);
						if(temp.equals((GeoItemizedOverlay) itemizedoverlay)) {
							if(currentOverlayIndex != -2 && currentOverlayIndex != i) {
								updateOverlayDrawable(currentOverlayIndex, getResources().getDrawable(R.drawable.map_point_blue));
							}
							currentOverlayIndex = i;
							updateOverlayDrawable(currentOverlayIndex, getResources().getDrawable(R.drawable.map_point_yellow));
							
							updateInfoCard(currentOverlayIndex);
							
//							synchronized(new AlertDialog.Builder(GoogleMap.this)
//							.setTitle("提示")
//							.setMessage("坐标：" + overlayitem.routableAddress())
//							.setNegativeButton("开始导航", new DialogInterface.OnClickListener() {
//								public void onClick(DialogInterface dialog, int which) {
////									isWalkRoute = true;
////									updateWalkRoutes();
//								}
//							}).show()){};
							break;
						}
					}
				}
			}
		};
		

		//地理位置监听器
		locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		locationListener = new LocationListener() {
			@Override
			public void onLocationChanged(Location location) {
				GoogleMap.this.location = location;
//				onLocationChangedGoogleMap();
			}
			@Override
			public void onProviderDisabled(String provider) {}
			@Override
			public void onProviderEnabled(String provider) {}
			@Override
			public void onStatusChanged(String provider, int status, Bundle extras) {}
		};
		
		//感应器监听器
		sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
	    sensorEventListener = new SensorEventListener() {
			@Override
			public void onSensorChanged(SensorEvent event) {
				GoogleMap.this.sensorEvent = event;
				onSensorChangedGoogleMap();
			}
			@Override
			public void onAccuracyChanged(Sensor sensor, int accuracy) {}
		};
		registerListener();//注册locationListener和sensorEventListener
        
		// 更新罗盘
//		updateCompassPointerHandler = new Handler() {
//			@Override
//			public void handleMessage(Message msg) {
//				super.handleMessage(msg);
//				compassView.onDirectionChanged(direction); // 调用ComassVeiw的方法更新罗盘
//			}
//		};
		
		mapToCameraHandler = new Handler();
		
		updateLocationChangedGoogleMapHandler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				super.handleMessage(msg);
				onLocationChangedGoogleMap();
			}
		};

		location = getLocationPrivider();
		if (location != null) {
			geoPointCurrent = geoPointFirst = location2GeoPoint(location);
			onLocationChangedGoogleMap();
		} else {
			new AlertDialog.Builder(GoogleMap.this)
			.setTitle("系统信息")
			.setMessage(getResources().getString(R.string.str_message))
			.setNegativeButton("确定", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					GoogleMap.this.finish();
				}
			}).show();
		}
//		start();
	}

	/*
     * 更新信息卡内容
     */
    private void updateInfoCard(int currentAroundIndex) {
    	infoText = (TextView) findViewById(R.id.info_name);
		infoText.setText(arounds.get(currentAroundIndex).getName());
		infoText = (TextView) findViewById(R.id.info_addr);
		infoText.setText(arounds.get(currentAroundIndex).getAddress());
		infoText = (TextView) findViewById(R.id.info_distance);
		infoText.setText("距离: " + Util.formatDistance(arounds.get(currentAroundIndex).getRealTimeDistance()));
		infoText = (TextView) findViewById(R.id.info_phone);
		infoText.setText("电话: " + arounds.get(currentAroundIndex).getPhoneNumber());
		infoCard.setVisibility(View.VISIBLE);
//		compassView.setVisibility(View.INVISIBLE);
    }
    
    /*
	 * 设置详细页面的内容
	 */
	private void setInfoDetailText(int currentAroundIndex) {
		TextView nameView = (TextView) findViewById(R.id.info_detail_name);
		TextView addrView = (TextView) findViewById(R.id.info_detail_addr);
		TextView distanceView = (TextView) findViewById(R.id.info_detail_distance);
		TextView timeView = (TextView) findViewById(R.id.info_detail_time);
		
		infoDetailView.setTag(arounds.get(currentAroundIndex));			//为了计算当前信息点在整个信息点列表中的位置
		
		nameView.setText(arounds.get(currentAroundIndex).getName());
		addrView.setText(arounds.get(currentAroundIndex).getAddress());
		distanceView.setText("距离 ：" + Util.formatDistance(arounds.get(currentAroundIndex).getRealTimeDistance()));
		String time = "未知";
		if (arounds.get(currentAroundIndex).getNeedTime() != -1)
			time = arounds.get(currentAroundIndex).getNeedTime() + "";
		timeView.setText("时间 :" + time);
	}
    
	//兴趣点搜索和显示处理
//	private class SearchAroundTask extends AsyncTask<Integer, Void, List<AroundSearch.Around>> {
//		private MinimapService service;
//		SearchAroundTask(MinimapService service) {
//			this.service = service;
//		}
//		@Override
//		protected void onPreExecute() {
//            text.setText("开始下载兴趣点数据");
//        }
//		@Override
//		protected List<AroundSearch.Around> doInBackground(Integer... params) {
//			return this.service.searchAround(params[0], params[1], params[2]);
//		}
//		@Override
//        protected void onCancelled() {
//            super.onCancelled();
//        }
//        protected void onPostExecute(List<AroundSearch.Around> arounds) {
//        	for (int i = 0; i < overlays.size(); i++) {
//				if(i != locationOverLayId && !overlays.isEmpty()) {
//					overlays.remove(i);
//				}
//			}
//        	updateLocationOverlay();
//			onFocusChangeListener = new OnFocusChangeListener() {
//				@SuppressWarnings("unchecked")
//				@Override
//				public void onFocusChanged(ItemizedOverlay itemizedoverlay, OverlayItem overlayitem) {
//					Log.e(TAG, "onFocusChanged overlayitem============ " + (overlayitem == null ? "null" : overlayitem.routableAddress()) + "");
//					if(overlayitem != null) {
//						itemizedOverlay = new GeoItemizedOverlay(getResources().getDrawable(R.drawable.map_point_yellow));
//						itemizedOverlay.addOverlay(overlayitem);
//						itemizedOverlay.setOnFocusChangeListener(onFocusChangeListener);
//						addItemizedOverlay(itemizedOverlay);
//						overlayItemNav = overlayitem;
//						
//						synchronized(new AlertDialog.Builder(GoogleMap.this)
//						.setTitle("提示")
//						.setMessage("坐标：" + overlayitem.routableAddress())
//						.setNegativeButton("开始导航", new DialogInterface.OnClickListener() {
//							public void onClick(DialogInterface dialog, int which) {
//								isWalkRoute = true;
//								updateWalkRoutes();
//							}
//						}).show()){};
//					}
//				}
//			};
//			
//			if(arounds != null) {
//				int len = arounds.size();
//				for (int i = 0; i < len; i++) {
//					itemizedOverlay = new GeoItemizedOverlay(getResources().getDrawable(R.drawable.map_point_blue));
//					itemizedOverlay.addOverlay(new OverlayItem(new GeoPoint((int) (arounds.get(i).getCoordDeflectY() * 1E6), (int) (arounds.get(i).getCoordDeflectX() * 1E6)), "", ""));
//					itemizedOverlay.setOnFocusChangeListener(onFocusChangeListener);
//					addItemizedOverlay(itemizedOverlay);
//				}
//			}
//        }   
//	}
	
	//步行导航处理
	private class SearchWalkRouteTask extends AsyncTask<Double, Void, List<RouteSearch.Route>> {
		private MinimapService service;
		
		SearchWalkRouteTask(MinimapService service) {
			this.service = service;
		}
		@Override
		protected void onPreExecute() {
            text.setText("开始下载步行导航数据");
        }
		@Override
		protected List<RouteSearch.Route> doInBackground(Double... params) {
			Coord20 coord20 = service.coordGps2Coord20(new CoordGps(params[0], params[1]));
			return this.service.searchWalkRoute(coord20.getX(), coord20.getY(), 2);
		}
		@Override
        protected void onCancelled() {
            super.onCancelled();
        }
        protected void onPostExecute(List<RouteSearch.Route> routes) {
        	for (int i = 0; i < overlays.size(); i++) {
				if(i != locationOverLayId && !overlays.isEmpty()) {
					overlays.remove(i);
				}
			}
        	updateLocationOverlay();
			if(routes != null) {
				int len = routes.size();
				for (int i = 0; i < len; i++) {
					int len2 = routes.get(i).getNavigationSize();
					for (int j = 0; j < len2; j++) {
						int len3 = routes.get(i).getNavigations().get(j).getCoordDeflectSize();
						for (int k = 0; k < len3; k++) {
							GeoPoint g1 = new GeoPoint((int) (routes.get(i).getNavigations().get(j).getCoordDeflects().get(k).getY() * 1E6), (int) (routes.get(i).getNavigations().get(j).getCoordDeflects().get(k).getX() * 1E6));
							int l = k < len3 - 1 ? k + 1 : k;
							GeoPoint g2 = new GeoPoint((int) (routes.get(i).getNavigations().get(j).getCoordDeflects().get(l).getY() * 1E6), (int) (routes.get(i).getNavigations().get(j).getCoordDeflects().get(l).getX() * 1E6));
							addOverLay(new MyOverLay(g1, g2, MyOverLay.MODE_LINE));
							if(i == len - 1 && j == len2 - 1 && k == len3 - 1) {
								g3 = g2;
								addOverLay(new MyOverLay(g3, R.drawable.map_point_yellow, GoogleMap.this));//画终点
							}
						}
					}
				}
			}
        }   
	}
	
	//google GPS坐标转换成偏转坐标处理
	private class CoordGps2CoordDeflectTask extends AsyncTask<Integer, Void, CoordDeflect> {
		private MinimapService service;
		CoordGps2CoordDeflectTask(MinimapService service) {
			this.service = service;
		}
		@Override
		protected void onPreExecute() {
            text.setText("开始转换坐标");
        }
		@Override
		protected CoordDeflect doInBackground(Integer... params) {
			return this.service.coordGps2CoordDeflect(new CoordGps(params[0] / 1E6, params[1] / 1E6));
		}
		@Override
        protected void onCancelled() {
            super.onCancelled();
        }
        protected void onPostExecute(CoordDeflect coordDeflect) {
        	geoPointCurrent = new GeoPoint((int)(coordDeflect.getY() * 1E6), (int)(coordDeflect.getX() * 1E6));
			if(locationOverLayId == -2) {
				GeoItemizedOverlay gio = new GeoItemizedOverlay(getResources().getDrawable(R.drawable.map_point_user));
				gio.addOverlay(new OverlayItem(geoPointCurrent, "", ""));
				addItemizedOverlay(gio);
				locationOverLayId = gio.getId();
			} else if(overlays != null) {
				GeoItemizedOverlay gio = new GeoItemizedOverlay(getResources().getDrawable(R.drawable.map_point_blue));
				gio.addOverlay(new OverlayItem(geoPointCurrent, "", ""));
				overlays.set(locationOverLayId, gio);
			}
			refreshMapView();
        }   
	}
	
	private void updateOverlayDrawable(int id, Drawable drawable) {
		GeoItemizedOverlay temp = new GeoItemizedOverlay(drawable);
		temp.addOverlay(((GeoItemizedOverlay) overlays.get(id)).createItem());
		temp.setId(((GeoItemizedOverlay) overlays.get(id)).getId());
		temp.setOnFocusChangeListener(onFocusChangeListener);
		overlays.set(id, temp);
		temp = null;
	}
	
	//地理位置变动执行函数
	public synchronized void onLocationChangedGoogleMap() {
		Log.i(TAG, "location = " + location);
		geoPointCurrent = location2GeoPoint(location);//记下移动后的位置
		refreshMapView();//更新MapView
		distance += getDistance(geoPointFirst, geoPointCurrent);//取得移动距离
		geoPointFirst = geoPointCurrent;
		if(isWalkRoute) {
			updateLocationOverlay();
		} else {
			updateArounds();
		}
		showTag();
	}
	
	//更新自己的地理位置图标
	private synchronized void updateLocationOverlay() {
		Integer[] params = {geoPointCurrent.getLongitudeE6(), geoPointCurrent.getLatitudeE6()};
		coordGps2CoordDeflectTask = new CoordGps2CoordDeflectTask(service);
		coordGps2CoordDeflectTask.execute(params);
		coordGps2CoordDeflectTask = null;
		
//		double la = ((geoPointCurrent.getLatitudeE6() / 1E6) + 0.00125772440338) * 1E6;
//		double lo = ((geoPointCurrent.getLongitudeE6() / 1E6) + 0.00608586762238) * 1E6;
//		geoPointCurrent = new GeoPoint((int)la, (int)(lo));
//		if(locationOverLayId == -2) {
//			GeoItemizedOverlay gio = new GeoItemizedOverlay(getResources().getDrawable(R.drawable.map_point_user));
//			gio.addOverlay(new OverlayItem(geoPointCurrent, "", ""));
//			addItemizedOverlay(gio);
//			locationOverLayId = gio.getId();
//		} else if(overlays != null) {
//			GeoItemizedOverlay gio = new GeoItemizedOverlay(getResources().getDrawable(R.drawable.map_point_user));
//			gio.addOverlay(new OverlayItem(geoPointCurrent, "", ""));
//			overlays.set(locationOverLayId, gio);
//		}
//		refreshMapView();
	}
	
	//更新兴趣点列表
	private synchronized void updateArounds() {
//		Integer[] params = {1, 10, 3000};
//		searchAroundTask = new SearchAroundTask(service);
//		searchAroundTask.execute(params);
//		searchAroundTask = null;
		

    	for (int i = 0; i < overlays.size(); i++) {
			if(i != locationOverLayId && !overlays.isEmpty()) {
				overlays.remove(i);
			}
		}
    	updateLocationOverlay();
		if(arounds != null) {
			int len = arounds.size();
			for (int i = 0; i < len; i++) {
				itemizedOverlay = new GeoItemizedOverlay(getResources().getDrawable(R.drawable.map_point_blue));
				itemizedOverlay.addOverlay(new OverlayItem(new GeoPoint((int) (arounds.get(i).getCoordDeflectY() * 1E6), (int) (arounds.get(i).getCoordDeflectX() * 1E6)), "", ""));
				itemizedOverlay.setOnFocusChangeListener(onFocusChangeListener);
				addItemizedOverlay(itemizedOverlay);
			}
		}
    
	}
	
	//更新导航
//	private synchronized void updateWalkRoutes() {
//		double endX = overlayItemNav.getPoint().getLongitudeE6() / 1E6;
//		double endY = overlayItemNav.getPoint().getLatitudeE6() / 1E6;
//		Toast.makeText(this, "endX = " + endX + ", endY = " + endY, Toast.LENGTH_LONG).show();
//		Double[] params = {endX, endY};
//		searchWalkRouteTask = new SearchWalkRouteTask(service);
//		searchWalkRouteTask.execute(params);
//		searchWalkRouteTask = null;
//	}
	
	//添加气泡图层
	private void addItemizedOverlay(GeoItemizedOverlay geoItemizedOverlay) {
		int index = -1;
		overlays.add(geoItemizedOverlay);
		if(overlays != null && !overlays.isEmpty()) {
			index = overlays.lastIndexOf(geoItemizedOverlay);
		}
		geoItemizedOverlay.setId(index);
		System.out.println("geoItemizedOverlay.getId() = " + geoItemizedOverlay.getId());
	}
	
	//添加画线图层
	private void addOverLay(MyOverLay myOverLay) {
		int index = -1;
		overlays.add(myOverLay);
		if(overlays != null && !overlays.isEmpty()) {
			index = overlays.lastIndexOf(myOverLay);
		}
		myOverLay.setId(index);
		System.out.println("myOverLay.getId() = " + myOverLay.getId());
	}
	
	//左上角提示信息
	private synchronized void showTag() {
		longitude = geoPointCurrent.getLongitudeE6() / 1E6;
		latitude = geoPointCurrent.getLatitudeE6() / 1E6;
		distance = Double.parseDouble(new DecimalFormat("#.00").format(distance));
		text.setText("经度 = " + longitude + "\n纬度 = " + latitude + "\n总移动距离 = " + distance + "米");
	}
	
	private GeoPoint location2GeoPoint(Location location) {
		if(location == null) return null;
		return new GeoPoint((int) (location.getLatitude() * 1E6), (int) (location.getLongitude() * 1E6));
	}

	public Location getLocationPrivider() {//取得LocationProvider
		Criteria mCriteria01 = new Criteria();
		mCriteria01.setAccuracy(Criteria.ACCURACY_FINE);
		mCriteria01.setAltitudeRequired(false);
		mCriteria01.setBearingRequired(false);
		mCriteria01.setCostAllowed(true);
		mCriteria01.setPowerRequirement(Criteria.POWER_LOW);
		return locationManager.getLastKnownLocation(locationManager.getBestProvider(mCriteria01, true));
	}

	//刷新地图到用户的当前位置
	private synchronized void refreshMapView() {//更新MapView的方法
		gMapView.displayZoomControls(true);
		gMapView.setSatellite(false);
		mapController.animateTo(geoPointCurrent);
		mapController.setZoom(zoomLevel);
	}

	public double getDistance(GeoPoint gp1, GeoPoint gp2) {//取得两点间的距离的方法
		double Lat1r = (Math.PI / 180) * (gp1.getLatitudeE6() / 1E6);
		double Lat2r = (Math.PI / 180) * (gp2.getLatitudeE6() / 1E6);
		double Long1r = (Math.PI / 180) * (gp1.getLongitudeE6() / 1E6);
		double Long2r = (Math.PI / 180) * (gp2.getLongitudeE6() / 1E6);
		double R = 6371;//地球半径(KM)
		double d = Math.acos(Math.sin(Lat1r) * Math.sin(Lat2r) + Math.cos(Lat1r) * Math.cos(Lat2r) * Math.cos(Long2r - Long1r))	* R;
		return d * 1000;
	}

	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}
	
//	private void start() {
//		gp1 = gp2;
//		resetOverlay();
//		setStartPoint();
//		refreshMapView();
//		distance = 0;
//		_isRun = true;
//	}
	
//	private void end() {
//		/* 画终点 */
//		setEndPoint();
//		/* 更新MapView */
//		refreshMapView();
//		/* 终止画路线的机制 */
//		_isRun = false;
//	}
	
//	private void paint() {
//		/* 画终点 */
//		setEndPoint();
//		/* 更新MapView */
//		refreshMapView();
//		/* 终止画路线的机制 */
//		_run = false;
//	}
	
//	/* 设置起点的方法 */
//	private void setStartPoint() {
//		mapOverlays.add(new MyOverLay(gp1, gp2, 1));
//	}

	/* 设置路线的方法 */
//	private void setRoute() {
//		mapOverlays.add(new MyOverLay(gp1, gp2, 2));
//	}

//	/* 设置终点的方法 */
//	private void setEndPoint() {
//		mapOverlays.add(new MyOverLay(gp1, gp2, 3));
//	}
	
	//感应器更新处理
	private synchronized void onSensorChangedGoogleMap() {
		float vals[] = sensorEvent.values;
		if(sensorEvent.sensor.getType() == Sensor.TYPE_ORIENTATION){				
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
			directionWithoutRoll =(float)((rawDirection * KFILTERINGFACTOR) + (directionWithoutRoll * (1.0 - KFILTERINGFACTOR)));
			
			// 通过左右摇摆数值对方向数值进行修正
			directionWithRoll = directionWithoutRoll + roll;
		}
		if(sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER){									
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
			
			if(Math.abs(inclination-90) > 30.0f) {
				// 延迟3秒进入实景模式
				if(isGreaterThan30 == false){
				    isGreaterThan30 = true;
				    mapToCameraHandler.postDelayed(toCameraTimer, 3000); 				   
			    }    	      	   
		    }else{
		    	if(isGreaterThan30 == true){
		    		isGreaterThan30 = false;
				    mapToCameraHandler.removeCallbacks(toCameraTimer);		    		   
			    }    	  
		    }
		}
		
//		updateCompassPointerHandler.sendEmptyMessage(0);   // 发送消息更新罗盘    
	}
	
	private	Runnable toCameraTimer = new Runnable() {
		public void run() {
			Log.d("transfer","run");
			startActivity(new Intent(GoogleMap.this, CameraActivity.class));
		}
	};
	
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
		sensorManager.unregisterListener(sensorEventListener);
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
	public void onPause() {
		super.onPause();
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