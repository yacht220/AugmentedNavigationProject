package com.lenovo.android.navigator;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Dialog;

import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.hardware.Camera;
import android.hardware.SensorListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.View.OnClickListener;

import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;


import com.lenovo.android.navigator.AnchorView.AnchorInfo;
import com.lenovo.android.navigator.NavigationView.NameAndDistance;
import com.lenovo.arfindertest.DepthOfField;
import com.lenovo.arfindertest.GoogleMap;
import com.lenovo.minimap.search.AroundSearch;

@SuppressWarnings("deprecation")
public class CameraActivity extends Activity implements LocationListener {
	
	public static final String KEY_PHOTO_INFO = "key_photo_info";
	public static final String KEY_PHOTO_DATA = "key_photo_data";
	public static final String KEY_PHOTO_WIDTH = "key_photo_width";
	public static final String KEY_PHOTO_HEIGHT = "key_photo_height";
	public static final String KEY_ANCHOR_INFO = "key_anchor_info";
	
	private static final int DIALOG_GPS_FAILED 	= 0;
	private static final int DIALOG_CATAGORY 	= 1;
	public static final int DIALOG_WAITING 		= 2;
	public static final int DIALOG_SEARCH_NAVIGATION_FAILED = 3;
	
    private static final int FLING_THRESHOLD = 30;
	private static final float THRESHOLD = 30f;
	private static final int TIMEOUT = 3000;

	//定义了程序的若干模式， 进入某种模式，就设为相应的模式
	public static final int MODE_SEARCH      = 0;
	public static final int MODE_CAMERA      = 1;
	public static final int MODE_LIST        = 2;
	public static final int MODE_MAP         = 3;
	public static final int MODE_MULTISELECT = 4;
	public static final int MODE_DEPTH       = 5;
	public static final int MODE_DETAIL      = 6;
	public static final int MODE_NAVIGATION  = 7;

	private static final long LOCATION_UPDATE_INTERVAL_MILLIS = 500L;

	private int mode;
	private boolean camera2Map = true;
	private boolean map2Camera = true;
	private Handler handler = new Handler();

    private long lastTime;

	private Runnable toNavigationTimer = new Runnable() {
		public void run() {
			setMode(MODE_NAVIGATION);
			showNavigation();
		}
	};

	private Runnable toCameraTimer = new Runnable() {
		public void run() {
			setMode(MODE_CAMERA);
			showCamera();
		}
	};

    private	Runnable toMapTimer = new Runnable() {
		public void run() {
			setMode(MODE_MAP);
			showMap();
		}
	};

    private static final int SEARCH_GPS_TIMEOUT = 3 * 60000;//查找GPS的超时
	private Runnable gpsSearchingFailedTimer = new Runnable() {
		public void run() {
		    searchingView.setVisibility(View.INVISIBLE);
            CameraActivity.this.showDialog(DIALOG_GPS_FAILED);
		}
	};

	public Preview preview;
	public void backfromPreview() {
		setMode(MODE_CAMERA);
		showCamera();
	}

	private SensorManager sensorManager;
	public CameraView cameraView;
	private TextView searchingView;
	
	private ScalerView scaler;
	private ImageView indicator;
	
	private View infoListPanel;
	private ListView infoList;
	private Button listClose;

	private FrameLayout buttonPanel;
	public  Button lockBtn;
	public  Button depthBtn;
	private Button listBtn;

	private FrameLayout mapView;
//	private CompassView compass;

	private LayoutInflater inflater;
	private LocationManager locationManager;

	private View cardAnchorView;
	private ListView infoCardView;

	public AnchorView anchorView;
	public ImageView focusView; 	

	//查看更多信息界面
	private float lastMotionX;
	private View infoDetailView;
	private Button infoDetailNavigateBtn ;
	private Button closeDetailBtn;
	private Button detailListBtn;
	
	private NavigationView navigationView;//实景导航的打标记的
	private FrameLayout navigation;
	
	public View navigationPrompt;
	public ImageView navigationPromptImage;
	public TextView navigationPromptText;

	public static List<PositionListener> listeners = new ArrayList<PositionListener>();

	/*
	 * 手势对应的Listener
	 * onDirectionChanged 是与东南西北方向相关的
	 * onGestureChanged 是手机垂直，平放相关的
	 */
	public interface PositionListener {
		void onDirectionChanged(float v);
		void onGestureChanged(float v);
	}

	public static void addPositionListener(PositionListener l) {
		listeners.add(l);
	}
	
	/*
     * Mini Service 邦定
     */
    private boolean mIsBound;
    public ServiceProxy mBoundService ;
    
    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            mBoundService = ((ServiceProxy.ServiceBinder)service).getService();
        }

        public void onServiceDisconnected(ComponentName className) {
            mBoundService = null;
        }
    };

    private void bindService() {            
    	bindService(new Intent(this, 
            ServiceProxy.class), mConnection, Context.BIND_AUTO_CREATE);
    	mIsBound = true;
    }
    
    private void unbindService() {
        if (mIsBound) {
            unbindService(mConnection);
            mIsBound = false;
        }
    }

	private final SensorListener sensorListener = new SensorListener() {
		public void onSensorChanged(int sensor, float[] values) {
            checkGPSAvailable();

			if (!(mode == MODE_CAMERA 
					|| mode == MODE_MAP
					|| mode == MODE_NAVIGATION
					|| mode == MODE_SEARCH))
				return;
            long now = System.currentTimeMillis();
            if (lastTime > now) //在不影响用户体验的同时，适当的减少调用次数
                return;
            lastTime = now + 50;
			for (int i = 0; i < listeners.size(); i++) {
				listeners.get(i).onDirectionChanged(values[0]);
				listeners.get(i).onGestureChanged(values[1]);
			}
			if(mBoundService != null){
				transfer(mBoundService.getInclination()-90);
			}			
		}

		public void onAccuracyChanged(int sensor, int accuracy) {
		}
	};

    /*
     * 检测GPS状态
     */
    private void checkGPSAvailable() {    	
    	if (hasGPS)//当已经找到gps无须在重试了
    		return;
		if (isGPSAvailable) {
            if (searchingView.isShown()) {
		        searchingView.setVisibility(View.INVISIBLE);
                handler.removeCallbacks(gpsSearchingFailedTimer);
            }
            
            hasGPS = true;
    		setMode(MODE_CAMERA);
    		showCamera();
        }
    }
    
    /*
     * 两种模式的转换
     */
    private void doTransfer(int mode, float angle, Runnable notMap, Runnable map) {
		if (!(mode == MODE_CAMERA || mode == MODE_MAP || mode == MODE_NAVIGATION))
			return;

		float v = Math.abs(angle);
		if (v > THRESHOLD) {
			map2Camera = true;
			if (camera2Map) {
				handler.removeCallbacks(map);
				handler.postDelayed(notMap, TIMEOUT);
				camera2Map = false;
			}
		} else {
			camera2Map = true;
			if (map2Camera) {
				handler.removeCallbacks(notMap);
				handler.postDelayed(map, TIMEOUT);
				map2Camera = false;
			}
		}
    }
    
    /*
     * 根据手机倾斜角度调用的转换函数
     */
	private void transfer(float angle) {
		if (anchorView.lock) {
			return;
		}
		if (navigationPromptDialog != null && navigationPromptDialog.isShowing()) {
			return;
		}
		
		if (mode == MODE_CAMERA) {
			doTransfer(mode, angle, toCameraTimer, toMapTimer);
			previewMode = MODE_CAMERA;
		} else if (mode == MODE_NAVIGATION) {
			doTransfer(mode, angle, toNavigationTimer, toMapTimer);
			previewMode = MODE_NAVIGATION;
		} else if (mode == MODE_MAP) {		//从map模式返回到原来正确的模式
			if (previewMode == MODE_CAMERA)
				doTransfer(mode, angle, toCameraTimer, toMapTimer);
			else
				doTransfer(mode, angle, toNavigationTimer, toMapTimer);
		}
	}
	
	private int previewMode;	
	
	private Thread checkGPSAvailableThread;
	public boolean isGPSAvailable;//当前的gps状态信息
	private boolean hasGPS = false;	
	
	@Override
	protected void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		
		// 禁止休眠
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,  WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		
		locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		bindService();
		
	    Const.loadDrawable(this);
	    
		final Handler gpsStatusHandler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				switch(msg.what) {
				case MSG_GPS_AVAILABLE:		
					isGPSAvailable = msg.getData().getBoolean(KEY_MSG_GPS_AVAILABLE);
					break;
				}
			}
		};
		
		Runnable waitServiceBound = new Runnable() {
			public void run() {
				if (mBoundService != null) {
					Log.d("____________", "checkGPSAvailable");
					checkGPSAvailableThread = new CheckGPSAvailableThread(gpsStatusHandler, mBoundService);
					checkGPSAvailableThread.start();
				} else {
					handler.postDelayed(this, 5000);
				}
			}
		};		
		handler.postDelayed(waitServiceBound, 5000);

		sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		setContentView(R.layout.camera);

		inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		
		mapView = (FrameLayout) findViewById(R.id.map_panel);
//		compass = (CompassView) findViewById(R.id.compass_panel);
//		addPositionListener(compass);

		scaler = (ScalerView) findViewById(R.id.scaler);
		addPositionListener(scaler);
		indicator = (ImageView) findViewById(R.id.indicator);

		preview = (Preview) findViewById(R.id.preview);
		
		cameraView = (CameraView) findViewById(R.id.camera_panel);
		searchingView = (TextView) findViewById(R.id.gps_search_view);

		focusView = (ImageView) findViewById(R.id.focus);
		cardAnchorView = (View) findViewById(R.id.card_anchor_panel);
		infoCardView = (ListView) findViewById(R.id.info_card);
		infoCardView.setAdapter(null);

		infoDetailView = (View) findViewById(R.id.detail_panel);
		infoDetailNavigateBtn = (Button) findViewById(R.id.info_to);
		infoDetailNavigateBtn.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {	
				AroundSearch.Around info = (AroundSearch.Around) v.getTag();
				enterNavigation(info);
			}
		});

		infoDetailView.setOnTouchListener(new View.OnTouchListener() {
			public boolean onTouch(View v, MotionEvent event) {

				final int action = event.getAction();
				final float x = event.getX();

				switch (action) {
				case MotionEvent.ACTION_DOWN:
					lastMotionX = x;
					break;
				case MotionEvent.ACTION_MOVE:
					break;
				case MotionEvent.ACTION_UP:					
					int flag = 0;
					final int deltaX = (int) (lastMotionX - x);
					if (Math.abs(deltaX) < FLING_THRESHOLD)
						return true;
					if (deltaX < 0) {
						--flag;						
					} else if (deltaX > 0) {
						++flag;
					}
					
					AroundSearch.Around point = (AroundSearch.Around) v.getTag();
					int currentAnchorInAllIndex = 0;					
					List<AroundSearch.Around> all = anchorView.getAllInfos();//所有信息点的列表
					
					for (int i = 0; i < all.size(); i++) {
						if (point.equals(all.get(i))) {
							currentAnchorInAllIndex = i;//获得信息点在列表中的下标
						}
					}
					//获得下个显示信息卡的index
					currentAnchorInAllIndex += flag;
					if (currentAnchorInAllIndex < 0) currentAnchorInAllIndex = 0;
					else if (currentAnchorInAllIndex >= all.size()) currentAnchorInAllIndex = all.size() - 1;
						
					AroundSearch.Around info = all.get(currentAnchorInAllIndex);					
					setInfoDetailText(info);//设置当前详细页
					anchorView.updateInfoCard(info);//设置信息卡
					break;
				}
				return true;
			}
		});
		
		closeDetailBtn = (Button) findViewById(R.id.go_back);
		closeDetailBtn.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				setMode(MODE_CAMERA);
				showCamera();
				
				int fromWhere = (Integer) v.getTag();
				if (fromWhere == R.id.info_list) {			//从列表框中进入
					showList();
				}
			}
		});
		
		anchorView = (AnchorView) findViewById(R.id.anchor_panel);
		anchorView.setCondition(CatalogAdapter.FULL); // default type
		addPositionListener(anchorView);
		
		navigationView = (NavigationView) findViewById(R.id.navigation);
		navigation = (FrameLayout) findViewById(R.id.navigation_panel);		
		
		navigationPrompt = (View) findViewById(R.id.prompt);
		navigationPromptText = (TextView) findViewById(R.id.prompt_text);
		navigationPromptImage = (ImageView) findViewById(R.id.prompt_image);
		navigationPrompt.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if (navigationView.isNavigationFinished()) {	//点击终点提示牌，完成导航，返回实景界面
					exitNavigation();
					return;
				}
				if (navigationView != null) {
					showNavigationDlg(navigationView.getNavigationList(), navigationView.navIndex);
				}
			}
		});

		buttonPanel = (FrameLayout) findViewById(R.id.button_panel);
		lockBtn = (Button) findViewById(R.id.lock_btn);
		listBtn = (Button) findViewById(R.id.list_btn);

		lockBtn.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				takePicture();								
			}
		});		
		
		listBtn.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				anchorView.unSelectAll();
				setMode(MODE_LIST);
				showList();
			}
		});
		setupList();
		
		depthBtn = (Button) findViewById(R.id.depth_btn);
		
		depthBtn.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				List<AroundSearch.Around> tArounds = anchorView.getAllInfos();
				if(tArounds == null || tArounds.size() == 0) {
					return ;
				} else {
					DepthOfField.startDof(mBoundService, tArounds);
					startActivity(new Intent(CameraActivity.this, DepthOfField.class));		
				}
			}
		});	
		
		// GPS searching
		setMode(MODE_SEARCH);
		showSearch();
	    handler.postDelayed(gpsSearchingFailedTimer , SEARCH_GPS_TIMEOUT);	
	}	
	
	/*
	 * 进入锁定场景界面
	 */
	private static class LockRunnable implements Runnable {
		private CameraActivity ca;
		
		public LockRunnable(CameraActivity c) {
			ca = new WeakReference<CameraActivity>(c).get();
		}
		
		public void run() {
			Intent i = new Intent(ca, SnapShot.class);
			Bundle b = new Bundle();
			
			Camera.Parameters parameter = ca.cameraView.camera.getParameters();				
			int width = parameter.getPreviewSize().width;
			int height = parameter.getPreviewSize().height;
			
			b.putInt(KEY_PHOTO_WIDTH, width);
			b.putInt(KEY_PHOTO_HEIGHT, height);
			b.putByteArray(KEY_PHOTO_DATA, ca.cameraView.snapShot);			
			b.putStringArray(KEY_ANCHOR_INFO, ca.anchorView.AnchorInfo2StringArray());
			i.putExtra(KEY_PHOTO_INFO, b);			

			ca.startActivity(i);
		}
	};
	
	public boolean startSnapShotActivity() {
		if (cameraView.snapShot == null)
			return false;
		cameraView.postDelayed(new LockRunnable(this), 10);
		return true;
	}
	
	public void showInfoCard(boolean show) {
		infoCardView.setVisibility(show ? View.VISIBLE : View.INVISIBLE);
	}

	public void updateInfoCard(List<AroundSearch.Around> list, Object obj) {
		//TODO updateInfoCard
		infoCardView.setAdapter(new InfoAdapter(this, list));
		showInfoCard(true);
		infoCardView.invalidate();
	}

	public void updateInfoList(List<AroundSearch.Around> list) {
		if (list == null)
			infoList.setAdapter(null);
		else
			infoList.setAdapter(new InfoAdapter(this, list));
		infoList.invalidate();
	}

	private void enterInfoDetailView(AroundSearch.Around info, int fromWhere) {
		setMode(MODE_DETAIL);
		if (fromWhere == R.id.info_list) {			//从列表框中进入
			showDetail(true);
		} else if (fromWhere == R.id.info_card) {
			showDetail(false);
		}
		closeDetailBtn.setTag(fromWhere);
		setInfoDetailText(info);
	}
	
	/*
	 * 设置详细页面的内容
	 */
	private void setInfoDetailText(AroundSearch.Around info) {
		TextView nameView = (TextView) findViewById(R.id.info_detail_name);
		TextView addrView = (TextView) findViewById(R.id.info_detail_addr);
		TextView distanceView = (TextView) findViewById(R.id.info_detail_distance);
		TextView timeView = (TextView) findViewById(R.id.info_detail_time);
		
		infoDetailView.setTag(info);			//为了计算当前信息点在整个信息点列表中的位置
		infoDetailNavigateBtn.setTag(info);		//为了导航时获取信息点
		
		nameView.setText(info.getName());
		addrView.setText(info.getAddress());
		distanceView.setText("距离 ：" + Util.formatDistance(info.getRealTimeDistance()));
		String time = "未知";
		if (info.getNeedTime() != -1)
			time = info.getNeedTime() + "";
		timeView.setText("时间 :" + time);
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case DIALOG_GPS_FAILED:
			TextView tv = (TextView) inflater.inflate(R.layout.gps_failed, null, false);
			return new AlertDialog.Builder(this).setView(tv).setPositiveButton(
					R.string.reconnect, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog,
								int whichButton) {
							setMode(CameraActivity.MODE_SEARCH);
							showSearch();
	                        handler.postDelayed(gpsSearchingFailedTimer , SEARCH_GPS_TIMEOUT);
						}
					}).setNegativeButton(R.string.cancel,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog,
								int whichButton) {
							dismissDialog(CameraActivity.DIALOG_GPS_FAILED);
							setMode(CameraActivity.MODE_CAMERA);
							showCamera();
						}
					}).create();
			case DIALOG_CATAGORY:
				return new CreateCatalog().createDialog();
			case DIALOG_WAITING:
				return createWaitingDialog();
			case DIALOG_SEARCH_NAVIGATION_FAILED:
				return createNavigationFailedDialog();
		}
		return null;
	}
	
    private Dialog createWaitingDialog() {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.navigation_searching);
        return dialog;
    }
    
    private Dialog createNavigationFailedDialog() {    	
    	return new AlertDialog.Builder(CameraActivity.this)
    	.setMessage(R.string.no_navigation)
        .setPositiveButton(R.string.reconnect, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
            	researchNavigation();
            }
        })
        .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
    			setMode(MODE_CAMERA);
    			showCamera();
            }
        })
        .create();
    }

    private class CreateCatalog implements DialogInterface.OnClickListener {
    	private CatalogAdapter adapter;

    	Dialog createDialog() {
    		adapter = new CatalogAdapter(CameraActivity.this);

		    final AlertDialog.Builder builder = new AlertDialog.Builder(CameraActivity.this);
		    builder.setAdapter(adapter, this);
		    builder.setInverseBackgroundForced(true);
		    AlertDialog dialog = builder.create();
		    return dialog;
		}
    	
        public void onClick(DialogInterface dialog, int which) {
        	anchorView.setCondition(which + 1);
        	anchorView.enableSearchAround();
        }
    }

	private void setupList() {
		infoListPanel = (View) findViewById(R.id.info_list_panel);
		infoList = (ListView) findViewById(R.id.info_list);
		infoList.setAdapter(null);

		listClose = (Button) findViewById(R.id.info_list_close);
		listClose.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				setMode(MODE_CAMERA);
				showCamera();
			}
		});
	}

	public class InfoAdapter extends BaseAdapter {
		List<AroundSearch.Around> list;

		public InfoAdapter(Context c, List<AroundSearch.Around> l) {
			list = l;
		}

		public int getCount() {
			return list.size();
		}

		public Object getItem(int position) {
			return list.get(position);
		}

		public long getItemId(int position) {
			return position;
		}
		
		public View getView(int position, View convertView, ViewGroup parent) {
			final AroundSearch.Around item = (AroundSearch.Around) getItem(position);
			
			if (convertView == null) {
				convertView = inflater.inflate(R.layout.info_item, parent, false);
			}

			final ListView listView = (ListView) parent;
			
			if (listView.getId() == R.id.info_card) {		//信息卡的点击
				listView.setOnTouchListener(new View.OnTouchListener() {
					public boolean onTouch(View v, MotionEvent event) {
						final float x = event.getX();    
						if (event.getAction() == MotionEvent.ACTION_DOWN) {
		    				if (x > 200) {	//点击信息卡的右侧，进入导航
		    					enterNavigation(item);
		    				} else {		    //点击信息卡进入详细页面					
		    					enterInfoDetailView(item, listView.getId());//从list列表中进入详细页面
		    				}				
						}
						return false;
					}
				});
			} else {
				LinearLayout ll = (LinearLayout) convertView.findViewById(R.id.info_display_area);
				ll.setOnClickListener(new OnClickListener() {
					public void onClick(View v) {
						enterInfoDetailView(item, listView.getId());//从list列表中进入详细页面
					}
				});
				
				Button b = (Button) convertView.findViewById(R.id.info_to);
				b.setOnClickListener(new OnClickListener() {
					public void onClick(View v) {
						enterNavigation(item);
					}
				});
			}
			
			TextView tv = (TextView) convertView.findViewById(R.id.info_name);
			tv.setText(item.getName());

			tv = (TextView) convertView.findViewById(R.id.info_addr);
			tv.setText(item.getAddress());

			tv = (TextView) convertView.findViewById(R.id.info_distance);
			tv.setText("距离: " + Util.formatDistance(item.getRealTimeDistance()));

			tv = (TextView) convertView.findViewById(R.id.info_phone);
			tv.setText("电话: " + item.getPhoneNumber());
			
			return convertView;
		}
	}

	@Override
	public void onRestart() {
		anchorView.lock = false;
		anchorView.isRunOneTime = false;
		super.onRestart();
	}

	@Override
	protected void onResume() {
		super.onResume();
		sensorManager.registerListener(sensorListener,
				SensorManager.SENSOR_ORIENTATION,
				SensorManager.SENSOR_DELAY_GAME);
		
		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
				LOCATION_UPDATE_INTERVAL_MILLIS, 1f, this);
		locationManager.requestLocationUpdates(
				LocationManager.NETWORK_PROVIDER,
				LOCATION_UPDATE_INTERVAL_MILLIS, 1f, this);
	}

	@Override
	protected void onPause() {
		sensorManager.unregisterListener(sensorListener);
		locationManager.removeUpdates(this);
		super.onPause();
	}

	@Override
	protected void onStop() {
		super.onStop();
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		if (checkGPSAvailableThread != null)
			checkGPSAvailableThread.interrupt();
		unbindService();		
//        android.os.Process.killProcess(android.os.Process.myPid());
		((ActivityManager) getSystemService(Context.ACTIVITY_SERVICE)).restartPackage(getPackageName());
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		MenuInflater menuInflater = getMenuInflater();
		menuInflater.inflate(R.menu.menu, menu);
		return true;
	}

	public void takePicture() {
		cameraView.takePicture();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_help:
			return true;
		case R.id.menu_catagory:
			showDialog(DIALOG_CATAGORY);
			return true;
		case R.id.menu_exit:
            finish();
			return true;
		}
		return false;
	}

	public void setMode(int mode) {
		this.mode = mode;
	}
	
	public int getMode() {
		return mode;
	}
	
	private void enterNavigation(int endX, int endY) {
		navigationView.setServiceProxy(mBoundService);
		setMode(MODE_NAVIGATION);
		navigationPrompt.setVisibility(View.INVISIBLE);
		showNavigation();
		navigationView.beginNavigate(mBoundService, endX, endY);		
	}
	
	private void enterNavigation(AroundSearch.Around info) {
		enterNavigation(info.getCoord20X(), info.getCoord20Y());
	}
	
	private void researchNavigation() {
		if (navigationView != null)
			enterNavigation(navigationView.endX, navigationView.endY);
		navigationPromptDialog.hide();//重新搜索路径时消失
	}
	
	private AlertDialog navigationPromptDialog;
	public void showNavigationDlg(ArrayList<NameAndDistance> list, int selection) {
		NavigationDlgAdapter dlgAdapter = new NavigationDlgAdapter(CameraActivity.this, list);
		AlertDialog.Builder builder = new AlertDialog.Builder(CameraActivity.this);
		
		View titleView = (View) inflater.inflate(R.layout.custom_title, null, false);
	    builder.setCustomTitle(titleView);
	    
		Button reset = (Button) titleView.findViewById(R.id.navigation_reset);
		reset.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {	
				researchNavigation();
			}
		});
		
	    builder.setPositiveButton(R.string.navigation_ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
            	navigationPrompt.setVisibility(View.VISIBLE);
            }
        })
        .setNegativeButton(R.string.navigation_cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
            	exitNavigation();
            }
        });
		builder.setAdapter(dlgAdapter, null);
		
		navigationPromptDialog = builder.create();		
		navigationPromptDialog.getListView().setTag(new Integer(selection));		
		navigationPromptDialog.show();
	}
	
	public void exitNavigation() {
		setMode(MODE_CAMERA);
		showCamera();
	}

	private void showSearch() {
		hideAllView();		
		scaler.setVisibility(View.VISIBLE);
		indicator.setVisibility(View.VISIBLE);
		searchingView.setVisibility(View.VISIBLE);
	}
	
	private void showScaler() {
		scaler.setVisibility(View.VISIBLE);
		indicator.setVisibility(View.VISIBLE);
	}
	
	private void showCamera() {
		hideAllView();
		showScaler();
		buttonPanel.setVisibility(View.VISIBLE);
		cardAnchorView.setVisibility(View.VISIBLE);
	}
	
	private void showMap() {
//		hideAllView();
//		mapView.setVisibility(View.VISIBLE);
		GoogleMap.arounds = anchorView.getAllInfos();
		if(GoogleMap.arounds == null || GoogleMap.arounds.size() == 0) {
			return;
		} else {
			GoogleMap.locationOverLayId = -2;
			if(GoogleMap.overlays != null) {
				GoogleMap.overlays.clear();
			}
			if(GoogleMap.updateLocationChangedGoogleMapHandler != null) {
				GoogleMap.updateLocationChangedGoogleMapHandler.sendEmptyMessage(0);
			}
			startActivity(new Intent(CameraActivity.this, GoogleMap.class));
		}
	}
	
	private void showList() {
		hideAllView();
		showScaler();
		infoListPanel.setVisibility(View.VISIBLE);
		updateInfoList(anchorView.getAllInfos());
	}

	private void showDetail(boolean isShowList) {
		hideAllView();
		if (isShowList)
			infoListPanel.setVisibility(View.VISIBLE);
		infoDetailView.setVisibility(View.VISIBLE);
	}
	
	private void showNavigation() {
		hideAllView();
		showScaler();
		navigation.setVisibility(View.VISIBLE);
	}
	
	public void showPreview() {
		hideAllView();
		showScaler();
		preview.setVisibility(View.VISIBLE);
	}
	
	private void hideAllView() {
		scaler.setVisibility(View.INVISIBLE);
		indicator.setVisibility(View.INVISIBLE);
		searchingView.setVisibility(View.INVISIBLE);
		
		cardAnchorView.setVisibility(View.INVISIBLE);
		
		buttonPanel.setVisibility(View.INVISIBLE);
		
		mapView.setVisibility(View.INVISIBLE);
		
		infoListPanel.setVisibility(View.INVISIBLE);
		
		infoDetailView.setVisibility(View.INVISIBLE);
		
		navigation.setVisibility(View.INVISIBLE);
		
		preview.setVisibility(View.INVISIBLE);
	}
	
	public void performMultiSelection(int value, int catalog[], List<AnchorInfo> anchorInfos) {
		setMode(MODE_MULTISELECT);
		preview.updateView(value, catalog, anchorInfos);		
		showPreview();
	}
	
	public static final int MSG_GPS_AVAILABLE = 0;
	public static final String KEY_MSG_GPS_AVAILABLE = "KEY_MSG_GPS_AVAILABLE";
	
    public static class CheckGPSAvailableThread extends Thread {
    	
        private Handler handler;
        private ServiceProxy proxy;
        
        CheckGPSAvailableThread(Handler handler, ServiceProxy proxy) {
        	this.handler = handler;        	
        	this.proxy = proxy;
        }
        
        private void sendDownloadData(boolean available) {
        	Message msg = Message.obtain();
            Bundle b = new Bundle();
            msg.what = MSG_GPS_AVAILABLE;            
            b.putBoolean(KEY_MSG_GPS_AVAILABLE, available);
            msg.setData(b);
            handler.sendMessage(msg);
        }
        
        public void run() {
            while (true) {
                try {
                	boolean b = proxy.isGPSDataAvailable();
                	sendDownloadData(b);                	
                    Thread.currentThread().sleep(2000);
                } catch(InterruptedException e) {
                }
            }
        }
	}    
    
	public void onLocationChanged(Location location) {
	}

	public void onProviderDisabled(String provider) {
	}

	public void onProviderEnabled(String provider) {
	}

	public void onStatusChanged(String provider, int status, Bundle extras) {
	}
}
