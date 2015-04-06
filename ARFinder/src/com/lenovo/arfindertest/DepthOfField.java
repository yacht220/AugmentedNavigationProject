package com.lenovo.arfindertest;

import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL;
import javax.microedition.khronos.opengles.GL10;

import com.lenovo.android.navigator.CameraActivity;
import com.lenovo.android.navigator.R;
import com.lenovo.android.navigator.ScalerView;
import com.lenovo.android.navigator.ServiceProxy;
import com.lenovo.android.navigator.Util;
import com.lenovo.dof.MatrixGrabber;
import com.lenovo.dof.MatrixTrackingGL;
import com.lenovo.dof.Square;
import com.lenovo.minimap.MinimapService;
import com.lenovo.minimap.search.AroundSearch;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.opengl.GLSurfaceView;
import android.opengl.GLU;
import android.opengl.GLSurfaceView.Renderer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
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

public class DepthOfField extends Activity {
	/** MinimapService对象 */
	private static ServiceProxy ms;
	/** Around实例链表 */
	private static List<AroundSearch.Around> arounds;
	/** MyGLSurfaceView实例，OpenGL对象 */
	private DofView dofView;	
	/** 景深模式中的距离拉杆 */
	private ImageView barViewDof;
	/** 景深模式退出按键 */
    private ImageButton backBtnDof;
    /** MatrixGrabber实例，用于获取当前OpenGL Matrix */
	private MatrixGrabber matrixGrabber;
	/** 存储景深模式中各兴趣点的屏幕坐标，有x,y两个坐标值 */
	private static float[][] PoiScreenCoordDof;
	/** 最多可获取的兴趣点数量 */
	private static int MAX_POI_SHOW_SIZE;
	/** 景深模式中场景向前移动Flag */
	private boolean isMoveForwardDof;
	/** 景深模式中场景向后移动Flag */
	private boolean isMoveBackwardDof;
    /** 景深模式中各兴趣点渲染纹理Flag，1为蓝色纹理，2为黄色纹理  */
    private static int[] PoiTextureModeDof;
    /** 触摸事件监听器 */
	private OnTouchListener touchListener;
	/** 罗盘 */
	private ScalerView scalerView;
	/** 用于刷新罗盘 */
	private Handler updateScalerHandler;
	/** 信息卡上的文字 */
	private TextView infoText;
	/** 信息卡布局 */
	private View infoCard;
	/** 详细信息布局 */
	private View infoDetailView;
	/** 详细信息中的返回按钮 */
	private Button closeDetailBtn;
	/** 信息卡中导航按钮 */
	private Button infoCardNavBtn;
	/** 详细信息中导航按钮 */
	private Button infoDetailNavBtn;
	/** 当前选中的兴趣点索引 */
	private int currentAroundIndex;
	/** 滑动距离临界值 */
	private static final int FLING_THRESHOLD = 30;
    /** 滑动中View里最后一个x值 */
	private float lastMotionX;
	
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	try{
    		Log.d("OPENGL","onCreate");
    		
    		super.onCreate(savedInstanceState);
    		// 隐藏抬头
//            requestWindowFeature(Window.FEATURE_NO_TITLE);
    		
    		// 全屏
            //getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
    		
            // 禁止休眠
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,  WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            
    		setContentView(R.layout.dof);
    		
    		scalerView = (ScalerView) findViewById(R.id.scaler);
    		// 接收消息后执行
    		updateScalerHandler = new Handler() {
                @Override
    			public void handleMessage(Message msg) {
    				super.handleMessage(msg);
    				// 调用ScalerView类的方法使其更新
    				scalerView.onDirectionChanged(ms.getSensorDirection());
    			}
            };
           
            infoCard = (View) findViewById(R.id.infocard_panel);
            infoDetailView = (View) findViewById(R.id.detail_panel);
            closeDetailBtn = (Button) findViewById(R.id.go_back);
            infoCardNavBtn = (Button) findViewById(R.id.info_card_to);
            infoCardNavBtn.setVisibility(View.INVISIBLE);
            infoDetailNavBtn = (Button) findViewById(R.id.info_to);
            infoDetailNavBtn.setVisibility(View.INVISIBLE);
            
            
    		isMoveForwardDof = false;
    		isMoveBackwardDof = false;
    		MAX_POI_SHOW_SIZE = MinimapService.SEARCH_SIZE;
    		PoiScreenCoordDof = new float[MAX_POI_SHOW_SIZE][2];
    		PoiTextureModeDof = new int[MAX_POI_SHOW_SIZE];
    		barViewDof = (ImageView)findViewById(R.id.bar);
    		backBtnDof = (ImageButton)findViewById(R.id.back);
    		
    		/* OpenGL的View用于景深模式 */ 
    		dofView = (DofView)findViewById(R.id.dofview);
    		// 用MatrixGrabber实例所必需的代码
    		dofView.setGLWrapper(new DofView.GLWrapper() {
                public GL wrap(GL gl) {
                    return new MatrixTrackingGL(gl);
                }});
    		// We want an 8888 pixel format because that's required for
            // a translucent window.
            // And we want a depth buffer.
    		// 在setRenderer()之前调用
    		// 在CameraView上叠加时，必须的代码
    		dofView.setEGLConfigChooser(8, 8, 8, 8, 16, 0); 
    		dofView.setRenderer(new DofRenderer(this)); 
    		// Use a surface format with an Alpha channel.
    		// 在CameraView上叠加时，必须的代码
    		dofView.getHolder().setFormat(PixelFormat.TRANSLUCENT);
    		
    		// 初始化MatrixGrabber实例
    		matrixGrabber = new MatrixGrabber(); 	
    		
    		/* 初始化时，景深模式中的兴趣点纹理都为蓝色 */
    		int i = 0; // 计数器
    		while(i<MAX_POI_SHOW_SIZE){
    			PoiTextureModeDof[i] = 1;
    			i++;
    		}
    		
    		// 设置触摸事件监听器
            touchListener = new OnTouchListener() {			
    			@Override
    			public boolean onTouch(View v, MotionEvent e) {
    				// 当前View坐标系下的触摸坐标，原点在当前View左上角
    				float x; 
    	            float y;
    	            // 触摸点的屏幕坐标与目标点屏幕坐标的距离。仅当用于兴趣点触摸时才使用整个数组，其它情况只使用d[0]
    	            double[] d = new double[MAX_POI_SHOW_SIZE];      	            
    	        	
    				switch(v.getId()){
    				// 距离拉杆
    				case R.id.bar:	
    					Log.d("touch", "barViewDof");
    					
    					// 获取当前View坐标系下的触摸坐标，原点在当前View左上角
    					x = e.getX(); 
    		            y = e.getY();	
    		        	switch(e.getAction()){
    		            case MotionEvent.ACTION_DOWN:
    		            	// 计算当前触摸点的View坐标与距离拉杆前进按钮View坐标(25, 5)的距离
    		            	d[0] = Math.sqrt(Math.pow(x-25, 2)+Math.pow(y-5, 2));
    		            	// 30像素距离范围内为点中
    		            	if(d[0] < 30){
    		            		isMoveBackwardDof = false;
    		            		isMoveForwardDof = true;
    		            		// 播放声音需要在layout/main.xml中的该View中设置android:soundEffectsEnabled="true"
    		            		barViewDof.playSoundEffect(SoundEffectConstants.CLICK);
    		            	}
    		            	// 计算当前触摸点的View坐标与距离拉杆后退按钮View坐标(50, 100)的距离
    		            	d[0] = Math.sqrt(Math.pow(x-50, 2)+Math.pow(y-100, 2));
    		            	// 30像素距离范围内为点中
    		            	if(d[0] < 30){
    		            		isMoveForwardDof = false;
    		            		isMoveBackwardDof = true;			            		
    		            		// 播放声音需要在layout/main.xml中的该View中设置android:soundEffectsEnabled="true"
    		            		barViewDof.playSoundEffect(SoundEffectConstants.CLICK);			            		                		 
    		            	}			            	
    		            	break;
    		            }			        	
    		        	break;
    		        
    				// 回到实景模式
    				case R.id.back:
    					Log.d("touch", "backBtnDof");
    					
    					switch(e.getAction()){
    		            case MotionEvent.ACTION_DOWN:
    		            	backBtnDof.setImageResource(R.drawable.go_back_pressed);			            	
    		            	break;
    		            case MotionEvent.ACTION_UP:
    		            	backBtnDof.setImageResource(R.drawable.go_back_normal);
    		            	
    		            	// 播放声音需要在layout/main.xml中的该View中设置android:soundEffectsEnabled="true"
    		            	backBtnDof.playSoundEffect(SoundEffectConstants.CLICK);
    		            	startActivity(new Intent(DepthOfField.this, CameraActivity.class));
    		            	break;
    					}               		
    					break;
    					
    				// 触摸兴趣点	
    				case R.id.dofview:	
    		     		Log.d("touch", "myGlSurface");

		     		    // 获取当前View坐标系下的触摸坐标，原点在当前View左上角
						x = e.getX(); 
			            y = e.getY();
		                switch(e.getAction()){
		                case MotionEvent.ACTION_DOWN:
		                	// 事件发生后首先将信息卡设置为INVISIBLE
		                	infoCard.setVisibility(View.INVISIBLE);
		                	
		                	int i = 0; // 计数器
		                	// 获取屏幕上触摸点和所有兴趣点的距离
		                	while(i<arounds.size()){
		                        // 计算当前触摸点的屏幕坐标与兴趣点当前屏幕坐标的距离
		                    	d[i] = Math.sqrt(Math.pow(x-PoiScreenCoordDof[i][0], 2)+Math.pow(y-PoiScreenCoordDof[i][1], 2));
		                	    i++;
		                	}
		                	// 找出最短距离及其兴趣点索引			                	
		                	int shortestPoiIndex = 0;
		                	double shortestD = d[0];
		                	i = 1;
		                	while(i<arounds.size()){
		                		if(shortestD > d[i]){
		                			shortestD = d[i];
		                			shortestPoiIndex = i;
		                		}
		                		i++;
		                	}
		                	// 点中和取消点中后需要触发的动作
		                	i = 0;
		                	while(i<arounds.size() ){
		                		// 若当前索引为最短距离的兴趣点且其距离小于30像素，则为点中，否则为点中取消
		                		if(i == shortestPoiIndex && d[shortestPoiIndex]<30){
			                		// TODO 被点中后需要做的事情 
		                    		PoiTextureModeDof[i] = 2; // 变换纹理
		                    		currentAroundIndex = i;
		                    		updateInfoCard(currentAroundIndex);
		                			
			                	}else{
			                		// TODO 点中取消后需要做的事情
		                    		PoiTextureModeDof[i] = 1; // 变换纹理		
			                	}
		                    	i++;
		                    }	                	
		                	break;
		                }		
    		     		break;  
    		     		
    		        // 点击信息卡，进入详细信息
    				case R.id.infocard_panel: 
    					backBtnDof.setVisibility(View.INVISIBLE);
    					barViewDof.setVisibility(View.INVISIBLE);
    					scalerView.setVisibility(View.INVISIBLE);
    					infoDetailView.setVisibility(View.VISIBLE); 
    					
    					setInfoDetailText(currentAroundIndex);
    					break;
    					
    				// 从详细信息返回到景深模式
    				case R.id.go_back:
    					infoDetailView.setVisibility(View.INVISIBLE);
    					backBtnDof.setVisibility(View.VISIBLE);
    					barViewDof.setVisibility(View.VISIBLE);
    					scalerView.setVisibility(View.VISIBLE);
    					break;
    					
    				// 滑动详细信息
    				case R.id.detail_panel:
    					x = e.getX();

    					switch (e.getAction()) {
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
//    						updateInfoCard(currentAnchorInAllIndex);//设置信息卡
    						break;
    					}    					
    					break;
    		        } 
    				return true;
    			}
    		};
    		
    		/* 
    		 * 每次触摸只能触发一个View，该View是其范围大小包含了触摸点，在最上层，
    		 * 并且设置为View.VISIBLE。
    		 */
    		backBtnDof.setOnTouchListener(touchListener);
    		barViewDof.setOnTouchListener(touchListener);
    		dofView.setOnTouchListener(touchListener);
    		infoCard.setOnTouchListener(touchListener);
    		closeDetailBtn.setOnTouchListener(touchListener);
    		infoDetailView.setOnTouchListener(touchListener);
    	}catch(Exception e){Log.d("OPENGL", "error"+e.getMessage());}   	   	
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
    
    /*
     * 自定义GLSurfaceView类
     */
    private static class DofView extends GLSurfaceView { 
    	/* 
    	 * 构造函数
    	 * 当采用res/layout中的xml文件来布局时，自定义的View类的
    	 * 构造函数就需要AttributeSet类型参数，且该类必须为static。
    	 */
        public DofView(Context context, AttributeSet attrs) {
        	super(context, attrs);  
        	Log.d("OPENGL","DofViewConstructor");
        }                
    }
    
    /*  
     * 景深模式根据UI设计应该在另一个Activity中，整合时需要考虑到这点。目前只是
     * 用于预研。
     */
    private class DofRenderer implements Renderer {
    	/** Square instance */
    	private Square square;	
    	/** The Activity Context ( NEW ) */
    	private Context context;
    	/** 前后移动参数 */
    	private float move;
    	/** 摄像机垂直视角 */
    	private float fovy;

    	/** 构造函数 */
    	public DofRenderer(Context context) {
    		Log.d("OPENGL","DofRendererConstructor");
    		
    		square = new Square(ms);
    		this.context = context;
    		move = -50;
    		fovy = 45;
    	}

    	@Override
    	public void onDrawFrame(GL10 gl) {
    		Log.d("OPENGL","onDrawFrame");
    		/*
    		 * Clear Screen And Depth Buffer。
    		 */
            gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT | GL10.GL_STENCIL_BUFFER_BIT);
 			
            float[] yRot = {(float) Math.cos(Math.toRadians(ms.getDirection())), 0.0f, -(float) Math.sin(Math.toRadians(ms.getDirection())), 0.0f,
	                0.0f, 1.0f, 0.0f, 0.0f,
                    (float) Math.sin(Math.toRadians(ms.getDirection())), 0.0f, (float) Math.cos(Math.toRadians(ms.getDirection())), 0.0f,
                    0.0f, 0.0f, 0.0f, 1.0f}; // 水平旋转矩阵
            
            // 判断场景是否进行向前后移动，逐步增加或减少是为了产生动画效果。同时变换摄像机的垂直视角。
			if(isMoveForwardDof == true){
				if(move >= 150){
					move = 150;
					isMoveForwardDof = false;
				}else{
					move += 5;
//					fovy += 0.5;
				}
			}else if(isMoveBackwardDof == true){
				if(move <= -50){
					move = -50;
					isMoveBackwardDof = false;
				}else{
					move -= 5;
//					fovy -= 0.5;
				}
			}
            	
            gl.glLoadIdentity();	//Reset The Current Modelview Matrix
            
            /* 渲染背景 */
			gl.glDisable(GL10.GL_DEPTH_TEST);
			gl.glMatrixMode(GL10.GL_PROJECTION); 	//Select The Projection Matrix
//			gl.glPushMatrix();
			gl.glLoadIdentity(); // 一定要有这行代码！！
			// 设置摄像机正投影效果，左手坐标系
			gl.glOrthof(0, ms.getScreenWidth(), 0, ms.getScreenHeight(), -1, 1); 
			gl.glMatrixMode(GL10.GL_MODELVIEW);
			square.draw(gl, 3);
			gl.glMatrixMode(GL10.GL_PROJECTION);
//			gl.glPopMatrix();
			gl.glLoadIdentity(); // 一定要有这行代码！！
			// 设置摄像机透视投影效果
			GLU.gluPerspective(gl, fovy, ms.getScreenWidth()/ ms.getScreenHeight(), 0.1f, 5000.0f);
			gl.glMatrixMode(GL10.GL_MODELVIEW); 	//Select The Modelview Matrix
			gl.glEnable(GL10.GL_DEPTH_TEST);   			
			
			// 设置场景相对于摄像机的位置
            gl.glRotatef(10, 1, 0, 0);
            gl.glTranslatef(0, -20, move); 
            
            // 渲染用户图标，不随场景旋转而旋转
//            gl.glPushMatrix();
//            gl.glTranslatef(0, 0, -180);
            square.draw(gl, 0);
//            gl.glPopMatrix();
            
            // 随罗盘旋转场景。旋转轴顺序为z、x、y轴，一定注意顺序，否则会有问题！！
	        gl.glMultMatrixf(yRot, 0);       
            
            // 渲染兴趣点图标
            int i =0; // 计数器
            while(i < arounds.size()){                	
            	gl.glPushMatrix(); 
            	
            	float[] PoiWindowCoordDof = new float[3]; // 存储景深模式中各兴趣点的Window坐标，用于gluProject()方法返回值临时存储，有x,y,z三个坐标值
            	float angle; // 兴趣点square相对于用户原点偏离正北方向的角度
            	/* 
            	 * 计算并平移相对于用户的兴趣点位置
            	 * 需要求取GPS经纬度、实际距离和OpenGL逻辑长度单位之间的换算关系，通过GPS经纬度
                 * 算距离的公式测出：
                 * 1000米 = 0.01018473125gps。
                 * 由于OpenGL逻辑长度单位本身是一种相对长度，而非绝对长度，单位可以为mm、cm、m
                 * 等，只要保证所设定的单位在以后的计算中保持一致就行。则假设OpenGL逻辑长度
                 * 单位1gl = 1米，则三者对应关系为：
                 * 1gl = 1米 = 0.00001018473125gps。
            	 */
            	// 经度相当于OpenGL坐标系的x
            	float dx = (float)((arounds.get(i).getCoordDeflectX()-ms.getMyLocationDeflect().getX())/0.00001018473125);
            	// 注意z轴的方向，纬度相当于OpenGL坐标系的z
            	float dz = -(float)((arounds.get(i).getCoordDeflectY()-ms.getMyLocationDeflect().getY())/0.00001018473125);
            	gl.glTranslatef(dx, 0, dz);	
            	
            	/*
            	 * 让所有兴趣点square都面向用户原点，所以需要计算兴趣点square相对于用户原点
            	 * 偏离正北方向的角度angle，然后按照该角度旋转兴趣点square，这样就使其面向用户原点。
            	 */
            	if(dx > 0.0 && dz == 0.0) // 坐标落在x正轴上
	    		    angle = 90.0f; 
	    	    else if (dx < 0.0 && dz == 0.0) // 坐标落在x负轴上
	    		    angle = -90.0f;
	    	    else 
	    		{
	    			angle = (float) Math.atan(dx/-dz); // 计算弧度，注意场景坐标系中z轴方向是朝内的，-dz用来转换成朝外
	    	        angle = (float) Math.toDegrees(angle); // 转换成角度
	    		    if(dx > 0.0 && dz > 0.0) // 坐标落入的象限为第四象限
	    		        angle = 180.0f + angle;
	    	        else if(dx < 0.0 && dz > 0.0) // 坐标落入的象限为第三象限
	    		        angle = -180.0f + angle;
	    		}
	    		gl.glRotatef(-angle, 0.0f, 1.0f, 0.0f); // 旋转兴趣点square，使其面向用户原点。
            	
            	square.draw(gl, PoiTextureModeDof[i]);
            	
            	/* 将兴趣点的OpenGL坐标转换成Window坐标（原点在左下角），3D rendering pipeline */
            	matrixGrabber.getCurrentState(gl);
            	if(GLU.gluProject(
            			0, 
        				0, 
        				0,
        				matrixGrabber.mModelView, 
        				0, 
        				matrixGrabber.mProjection, 
        				0, 
        				new int[]{0, 0, (int)ms.getScreenWidth(), (int)ms.getScreenHeight()}, // viewport布满整个屏幕，viewport的坐标在Window坐标系下，原点也在左下角
        				0, 
        				PoiWindowCoordDof, 
        				0
        				) == GL10.GL_TRUE){
            		/* 
            		 * 将Window坐标转换成屏幕坐标（原点在左上角），screenX=winX, screenY=viewportY-winY。
            		 * 这里需要注意，当获取的winZ>1时，其兴趣点已经在背后，但其屏幕坐标仍然在屏幕范围内，所以
            		 * 对这种情况的兴趣点的screenX和screenY作人为处理，使其不在屏幕上。
            		 */
            		if(PoiWindowCoordDof[2]>1){
            			PoiScreenCoordDof[i][0] = -50;
            			PoiScreenCoordDof[i][1] = -50;
            		}else{
            			PoiScreenCoordDof[i][0]=PoiWindowCoordDof[0];
            			PoiScreenCoordDof[i][1]=ms.getScreenHeight() - PoiWindowCoordDof[1]; 
            		}
            	}            	
            	
            	gl.glPopMatrix();                    
            	i++;
            }           
            
            // 发送消息通知Handler执行
            updateScalerHandler.sendEmptyMessage(0);		
        }

    	@Override
    	public void onSurfaceChanged(GL10 gl, int width, int height) {
    		Log.d("OPENGL","onSurfaceChanged");
    		if(height == 0) { 						//Prevent A Divide By Zero By
    			height = 1; 						//Making Height Equal One
    		}

    		gl.glViewport(0, 0, width, height); 	//Reset The Current Viewport
    		gl.glMatrixMode(GL10.GL_PROJECTION); 	//Select The Projection Matrix
    		gl.glLoadIdentity(); 					//Reset The Projection Matrix

    		//Calculate The Aspect Ratio Of The Window
    		GLU.gluPerspective(gl, fovy, (float)width / (float)height, 0.1f, 5000.0f);    		
    		gl.glMatrixMode(GL10.GL_MODELVIEW); 	//Select The Modelview Matrix
    		gl.glLoadIdentity(); 					//Reset The Modelview Matrix   		
    	}

    	@Override
    	public void onSurfaceCreated(GL10 gl, EGLConfig config) {  
    		Log.d("OPENGL","onSurfaceCreated");
    		square.loadGLTexture(gl, context); //Load the texture for the square once during Surface creation
    		gl.glEnable(GL10.GL_TEXTURE_2D);			//Enable Texture Mapping ( NEW )
    		
    		gl.glBlendFunc(GL10.GL_ONE, GL10.GL_ONE_MINUS_SRC_ALPHA); // 用于具有Alpha值的PNG texture
    		gl.glEnable(GL10.GL_BLEND); // 用于具有Alpha值的PNG texture
    		
    		gl.glAlphaFunc(GL10.GL_GREATER, 0.0f); // Alpha值大于0的像素才写入Z-BUFFER中，这样，在深度检测中，只比较Alpha值大于0的像素，等于0的不比较。这样就解决了全透明区域参与深度检测后出现透明遮挡的错误 
    		gl.glEnable(GL10.GL_ALPHA_TEST); // 启用Alpha检测   		
    		
    		gl.glShadeModel(GL10.GL_SMOOTH); 			//Enable Smooth Shading
    		
    		gl.glClearColor(0, 0, 0, 0); 	//背景颜色为黑色，Alpha值必须为0才能和CameraView正常叠加
    		
    		gl.glClearDepthf(1.0f); 					//Depth Buffer Setup
    		gl.glDepthFunc(GL10.GL_LEQUAL); 			//The Type Of Depth Testing To Do 深度值相等或小的通过深度检测	
    		gl.glEnable(GL10.GL_DEPTH_TEST); // 启动深度检测
    		
    		// Clear stencil buffer with zero, increment by one whenever anybody
    	    // draws into it. When stencil function is enabled, only write where
    	    // stencil value is zero. This prevents the transparent shadow from drawing
    	    // over itself
    	    gl.glStencilOp(GL10.GL_INCR, GL10.GL_INCR, GL10.GL_INCR);
    	    gl.glClearStencil(0);
    	    gl.glStencilFunc(GL10.GL_EQUAL, 0, 0xffffffff);
    	    gl.glEnable(GL10.GL_STENCIL_TEST); // 启用模板检测
    		
    	    //Really Nice Perspective Calculations
    		gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT, GL10.GL_NICEST); 
    	}   	
    }
    
    /**
     * 开启景深模式，传入主Activity中的两个对象，因为景深模式中需要主Activity的一些数据
     * @param ms MinimapService对象
     * @param arounds List<AroundSearch.Around>对象
     */
    public static void startDof(ServiceProxy ms, List<AroundSearch.Around> arounds){
    	Log.d("OPENGL","startDof");
    	DepthOfField.ms = ms;
    	DepthOfField.arounds = arounds;
    }
    
    @Override
    public void onStart(){
    	Log.d("OPENGL","onStart");
    	super.onStart();
    }
    
    @Override
    public void onRestart(){
    	Log.d("OPENGL","onRestart");
    	super.onRestart();
    }
    
    @Override
    public void onResume(){
    	Log.d("OPENGL","onResume");
    	super.onResume();   
    	/*
    	 * 解决当前activity退出又重新进入后Renderer不再重新渲染的方法是
    	 * 在AndroidManifest.xml中的该activity中加入android:launchMode="singleInstance"（多Activity时）
    	 */
    	dofView.onResume();
    }
    
    @Override
    public void onPause(){
    	Log.d("OPENGL","onPause");
    	super.onPause();
    	dofView.onPause(); 
    }
    
    @Override
    public void onStop(){
    	Log.d("OPENGL","onStop");
    	super.onStop();
    }
    
    /**
	 * 结束当前的这个应用程序(有多个Activity)<br>
	 * 注意：在AndroidManifest.xml中添加权限 &lt;uses-permission android:name="android.permission.RESTART_PACKAGES" /&gt;
	 */
    @Override
	public void onDestroy() {
    	Log.d("OPENGL","onDestroy");
		super.onDestroy();
		((ActivityManager) getSystemService(Context.ACTIVITY_SERVICE)).restartPackage(getPackageName());
	}
}