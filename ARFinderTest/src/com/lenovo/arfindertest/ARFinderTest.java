package com.lenovo.arfindertest;

import java.util.List;

import com.lenovo.minimap.MinimapService;
import com.lenovo.minimap.search.AroundSearch;
import com.lenovo.minimap.search.RouteSearch;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SoundEffectConstants;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnTouchListener;
import android.widget.ImageButton;

public class ARFinderTest extends Activity {
	/** MinimapService对象 */
	public static MinimapService ms;
//	/** PoiCanvasView实例，实景模式中兴趣点2D图形渲染对象 */
//	private PoiCanvasView poiCanvasDraw;
//	/** CameraView实例，摄像头对象 */
//	private CameraView cameraView;	
	/** 景深模式进入按键 */
	private ImageButton dofBtn;	
	private ImageButton googleBtn;
    /** 触摸事件监听器 */
	private OnTouchListener touchListener;	
	/** Around实例链表 */
	public static List<AroundSearch.Around> arounds;
	public static List<RouteSearch.Route> routes;
	/** 兴趣点是否已经获得 */
	private static boolean isPoiAvailable;    
	
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
            
    		setContentView(R.layout.main);
    		
    		ms = new MinimapService(this, 500);
    		
    		isPoiAvailable = false;     		
    		
//    		cameraView = (CameraView)findViewById(R.id.cameraview);
//    		poiCanvasDraw = (PoiCanvasView)findViewById(R.id.poicanvasview);
    		dofBtn = (ImageButton)findViewById(R.id.dof);
    		googleBtn = (ImageButton)findViewById(R.id.google);
            
            // 设置触摸事件监听器
            touchListener = new OnTouchListener() {			
				@Override
				public boolean onTouch(View v, MotionEvent e) {		            
					switch(v.getId()){			        	
			        // 进入景深模式
					case R.id.dof:
						Log.d("touch", "dofBtn");
						
						switch(e.getAction()){
			            case MotionEvent.ACTION_DOWN:
			            	dofBtn.setImageResource(R.drawable.dofselect);			            	
			            	break;
			            case MotionEvent.ACTION_UP:
			            	dofBtn.setImageResource(R.drawable.dof);
			            	
			            	// 播放声音需要在layout/main.xml中的该View中设置android:soundEffectsEnabled="true"
			            	dofBtn.playSoundEffect(SoundEffectConstants.CLICK);
			            	
			            	// 当兴趣点还未获取到时，无效
			                if(isPoiAvailable == false){
			                	break;
			                }
			            			
			                DepthOfField.startDof(ms, arounds);
			            	startActivity(new Intent(ARFinderTest.this, DepthOfField.class));
			            	break;
						} 
						break;
						
					case R.id.google : 
						startActivity(new Intent(ARFinderTest.this, GoogleMap.class));
						break;					
			        } 
					return true;
				}
			};
			
			/* 
			 * 每次触摸只能触发一个View，该View是其范围大小包含了触摸点，在最上层，
			 * 并且设置为View.VISIBLE。
			 */
			dofBtn.setOnTouchListener(touchListener);
			googleBtn.setOnTouchListener(touchListener);
    	}catch(Exception e){Log.d("Exception", e.getMessage());}    	
    }    
    
    /* 实景模式中兴趣点2D图形渲染类 */
    @SuppressWarnings("unused")
	private static class PoiCanvasView extends View{
    	/* 
    	 * 构造函数
    	 * 当采用res/layout中的xml文件来布局时，自定义的View类的
    	 * 构造函数就需要AttributeSet类型参数，且该类必须为static。
    	 */
    	public PoiCanvasView(Context context, AttributeSet attrs) {
    		super(context, attrs);	
    	}
    	    
    	@Override
    	protected void onDraw(Canvas canvas) {
    		super.onDraw(canvas);			
    		Paint paint = new Paint();
    		paint.setStyle(Paint.Style.FILL);
    		paint.setColor(Color.GREEN); 
    		
    		// 只有当GPS获取位置信息后才开始查询，并渲染
    		if(ms.isGPSDataAvailable()){
    			// 如果兴趣点还未获取，则查询
    			if(isPoiAvailable == false){
    				arounds = ms.searchAround(1);
    				while(arounds == null){
    					arounds = ms.searchAround(1);
    				}    				
    				isPoiAvailable = true;
    			}
    			
    			int n = 0; // 计数器
    			int i = 0; // 计数器
        		while(i < arounds.size()){
        			if(n == 4){
        				n = 0;
        			}
        			canvas.drawCircle(arounds.get(i).getScreenX(), arounds.get(i).getScreenY(), 10, paint);
        			canvas.drawText(arounds.get(i).getName()+",实时距离:"+Math.round(arounds.get(i).getRealTimeDistance())+"米,高德距离:"+arounds.get(i).getStaticDistance()+"米", arounds.get(i).getScreenX()+15, arounds.get(i).getScreenY(), paint);
        			// 画景深模式的兴趣点屏幕位置，测试用
//        			canvas.drawCircle(PoiScreenCoordDof[i][0], PoiScreenCoordDof[i][1], 10, paint); 
        			n++;
        			i++;			
    			}
    		}    		   					
    		invalidate();
    	}
    	
    	@Override
        protected void onAttachedToWindow() {
            super.onAttachedToWindow();
        }
        
        @Override
        protected void onDetachedFromWindow() {
            super.onDetachedFromWindow();
        }    	
    }    
    
    /* 摄像机类 */
    @SuppressWarnings("unused")
	private static class CameraView extends SurfaceView {
    	/* Camera实例 */
    	Camera camera;
    	/* SurfaceHolder实例 */
    	SurfaceHolder previewHolder;
    	/* SurfaceHolder监听器 */
    	SurfaceHolder.Callback surfaceHolderListener = new SurfaceHolder.Callback() {
    		@Override
    		public void surfaceCreated(SurfaceHolder holder) {
    			camera = Camera.open();
    			
    			/* 使在portrait模式下预览图像方向正常 */
    			Parameters params = camera.getParameters();
    			params.set("orientation", "portrait");
    			camera.setParameters(params);

    			try{
    				camera.setPreviewDisplay(previewHolder);
    			}catch(Throwable t){}
    		}
    		
    		@Override
    		public void surfaceChanged(SurfaceHolder holder, int format, int width,
    				int height) {
    			Parameters params = camera.getParameters();
    			params.setPreviewSize(width, height);
    			params.setPreviewFormat(PixelFormat.JPEG);
    			camera.setParameters(params);
    			camera.startPreview();
    		}
    		
    		@Override
    		public void surfaceDestroyed(SurfaceHolder holder) {
    			camera.stopPreview();
    			camera.release();			
    		}
    	};
    	
    	/* 
    	 * 构造函数
    	 * 当采用res/layout中的xml文件来布局时，自定义的View类的
    	 * 构造函数就需要AttributeSet类型参数，且该类必须为static。
    	 */
    	public CameraView(Context context, AttributeSet attrs) {
    		super(context, attrs);
    		
    		previewHolder = this.getHolder();
    		previewHolder.addCallback(surfaceHolderListener);
    		previewHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);				
    	}
    }
    
    @Override
    public void onStart(){
    	super.onStart();
    }
    
    @Override
    public void onRestart(){
    	super.onRestart();
//    	ms.onRestart();
    }
    
    @Override
    public void onResume(){
    	super.onResume();
    }
    
    @Override
    public void onPause(){
    	super.onPause();
    }
    
    @Override
    public void onStop(){
    	super.onStop(); 
//    	ms.onStop();
    }
    
    /*
     * 结束当前的这个应用程序(有多个Activity)<br>
	 * 注意：在AndroidManifest.xml中添加权限 &lt;uses-permission android:name="android.permission.RESTART_PACKAGES" /&gt;
     */
    @Override
    public void onDestroy(){
    	super.onDestroy();
    	ms.onDestroy();
    	((ActivityManager) getSystemService(Context.ACTIVITY_SERVICE)).restartPackage(getPackageName());
    }
}