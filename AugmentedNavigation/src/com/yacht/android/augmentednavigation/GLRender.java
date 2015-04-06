/*
 * 需要求取GPS经纬度、实际距离和OpenGL逻辑长度单位之间的换算关系，通过GPS经纬度
 * 算距离的公式测出：
 * 1000米 = 0.01018473125gps。
 * 由于OpenGL逻辑长度单位本身是一种相对长度，而非绝对长度，单位可以为mm、cm、m
 * 等，只要保证所设定的单位在以后的计算中保持一致就行。则假设OpenGL逻辑长度
 * 单位1gl = 1米，则三者对应关系为：
 * 1gl = 1米 = 0.00001018473125gps。 
 * 
 * *************忽略***********************************************************
 * HTC G2手机的屏幕大小为3.2英寸，该值表示手机屏幕对角线的长度，等于81.28mm。宽高为
 * 320*480。根据三角定理可以推算出手机屏幕的宽度和高度分别为：
 * 81.28*cos(arctan(480/320)) = 45.09mm，81.28*sin(arctan(480/320)) = 67.63mm。
 * 当OpenGL的摄像机透视垂直视角设置为45度，在OpenGL世界坐标系z=0的平面上画一平面，其
 * 宽高刚好覆盖整个屏幕的逻辑长度单位均为2。即2glx = 0.04509m，2gly = 0.06763m。
 * 由于二者比值不同，则取前者，即比值为2gl = 0.04509m。
 * 最后得到三者的转换关系为：
 * 44.355732978487469505433577289865gl = 1米 = 0.00001018473125gps。
 * 在实际计算中，由于数值太大导致如果兴趣点过远则无法显示，可以在程序中设定，如果超过
 * 100米，则按100米所对应的OpenGL逻辑长度单位来计算。
 * *************忽略***********************************************************
 * 
 * 另外，若要考虑海拔，就不能采用平面阴影的计算方法，为了简化计算，在本程序中规定，海拔与阴影计算
 * 不允许共存。 
 */



package com.yacht.android.augmentednavigation;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.opengl.GLU;
import android.opengl.GLSurfaceView.Renderer;

public class GLRender implements Renderer {
	/** Cube instance */
	private Cube cube;	
	/** Rectangle instance */
	private Rectangle rectangle;	
	/** CanvasView实例 */
	private CanvasView canvasView;	
	/** AugmentedNavigation实例 */
	private AugmentedNavigation an;	
	/** Angle For The Cube */
	private float rquad;
	/** 阴影投射变换矩阵 */
	private float[] shadowMatrix; 
	/** 路径总索引大小，即路径点总数 */
	private int routeIndexSize;
	/** 路径段的长度 */
	private float routeLength;
	/** 离用户最近的路径点索引 */
	private int routeCurIndex;
	/** 提取用于渲染的路径总索引大小，即提取的路径点总数 */
	private int routeSectionIndexSize; 
	/** 从已规划的路径提取当前路径点坐标后若干路径点坐标 */
	private double[][] routeSection;
	/** 转换后的用于渲染的各点坐标 */
	private double[][] routeSectionGL; 
	/** 是否渲染阴影，要渲染就不能考虑海拔 */
	private boolean isShadow;
	
	/**
	 * 构造函数
	 * @param an AugmentedNavigation实例
	 * @param canvasView CanvasView实例
	 */
	public GLRender(AugmentedNavigation an, CanvasView canvasView){
		// 初始化
		this.an = an;
		this.canvasView = canvasView;
		cube = new Cube();
		rectangle = new Rectangle();
		
		rquad = 0.0f;
		shadowMatrix = new float[16];

		routeIndexSize = 8;
		routeLength = 0.0f;	
		
		routeCurIndex = 0;		
		routeSectionIndexSize = 0;
		routeSection = new double[3][3]; // 注意数组第一个下标，表示一次性提取的路径点最大个数
		routeSectionGL = new double[3][3]; // 注意数组第一个下标，需和一次性提取的路径点最大个数一样
		
		isShadow = true;
	}
	
	@Override
	public void onSurfaceCreated(GL10 gl, EGLConfig config) {
		gl.glShadeModel(GL10.GL_SMOOTH); 			//Enable Smooth Shading
		gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f); 	//背景颜色为黑色，alpha值必须为0才能和camerapreview正常叠加
		gl.glClearDepthf(1.0f); 					//Depth Buffer Setup
		gl.glDepthFunc(GL10.GL_LEQUAL); 			//The Type Of Depth Testing To Do
		
		// Clear stencil buffer with zero, increment by one whenever anybody
	    // draws into it. When stencil function is enabled, only write where
	    // stencil value is zero. This prevents the transparent shadow from drawing
	    // over itself
	    gl.glStencilOp(GL10.GL_INCR, GL10.GL_INCR, GL10.GL_INCR);
	    gl.glClearStencil(0);
	    gl.glStencilFunc(GL10.GL_EQUAL, 0, 0xff);
	    
        gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA); // 设置颜色混合模式，产生虚拟物体间的半透明效果
        gl.glEnable(GL10.GL_BLEND); // 启用颜色混合
        
		float[] lightPos = { 0.0f, 100.0f, 0.0f }; // 光源位置
		float[][] points = {{ -30.0f, 0.0f, -20.0f },
                            { 40.0f, 0.0f, 20.0f },
                            { -30.0f, 0.0f, 20.0f }}; // 平面上的任意三点，顺时针方向
		float[] planeEquation = new float[4]; // 平面方程
		//planeEquation = Math3d.m3dGetPlaneEquation(points[0], points[1], points[2]); // 通过平面上的任意三点获取该平面的方程
		//shadowMatrix = Math3d.m3dMakePlanarShadowMatrix(planeEquation, lightPos); // 获取阴影投射变换矩阵
		Math3d.m3dGetPlaneEquationJni(planeEquation, points[0], points[1], points[2]); // 通过平面上的任意三点获取该平面的方程
		Math3d.m3dMakePlanarShadowMatrixJni(shadowMatrix, planeEquation, lightPos); // 获取阴影投射变换矩阵
		
		//Really Nice Perspective Calculations
		gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT, GL10.GL_NICEST); 
	}
	
	@Override
	public void onDrawFrame(GL10 gl) {
		if(an.isDrawEnabled()){			
			float[] xRot = {1.0f, 0.0f, 0.0f, 0.0f,
			                0.0f, (float) Math.cos(Math.toRadians(an.getInclination())), (float) Math.sin(Math.toRadians(an.getInclination())), 0.0f,
                            0.0f, -(float) Math.sin(Math.toRadians(an.getInclination())), (float) Math.cos(Math.toRadians(an.getInclination())), 0.0f,
                            0.0f, 0.0f, 0.0f, 1.0f}; // 俯仰旋转矩阵
	
	        float[] yRot = {(float) Math.cos(Math.toRadians(an.getDirection())), 0.0f, -(float) Math.sin(Math.toRadians(an.getDirection())), 0.0f,
			                0.0f, 1.0f, 0.0f, 0.0f,
                            (float) Math.sin(Math.toRadians(an.getDirection())), 0.0f, (float) Math.cos(Math.toRadians(an.getDirection())), 0.0f,
                            0.0f, 0.0f, 0.0f, 1.0f}; // 方向旋转矩阵
	        
	        float[] zRot = {(float) Math.cos(Math.toRadians(-an.getRoll())), (float) Math.sin(Math.toRadians(-an.getRoll())), 0.0f, 0.0f,
			        -(float) Math.sin(Math.toRadians(-an.getRoll())), (float) Math.cos(Math.toRadians(-an.getRoll())), 0.0f, 0.0f,
			        0.0f, 0.0f, 1.0f, 0.0f,
			        0.0f, 0.0f, 0.0f, 1.0f}; // 左右摇摆旋转矩阵
	
	        //Clear Screen, Depth Buffer And Stencil Buffer
	        gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT | GL10.GL_STENCIL_BUFFER_BIT);	
	        gl.glLoadIdentity();					//Reset The Current Modelview Matrix
	
	        //Drawing	        
	        // 随罗盘和加速度计旋转场景。旋转轴顺序为z、x、y轴，一定注意顺序，否则会有问题！！
	        gl.glMultMatrixf(zRot, 0);
	        gl.glMultMatrixf(xRot, 0);
	        gl.glMultMatrixf(yRot, 0);   
	        
	        // 垂直向下平移场景1.5米，相当于贴在地面上
	        gl.glTranslatef(0, -1.5f, 0);
	
	        /** 
	         * 画兴趣点标识
	         */
	        int i = 0; // 计数器        
	        while(i<5){
	        	// 当兴趣点离用户的距离小于50米时才渲染
	        	if(canvasView.getPOIDistanceWithoutAltitude()[i] <= 50){
	        		if(isShadow){
		        		/* 
			        	 * 先画阴影，采用平面阴影方法。
			        	 * 注意矩阵变换的顺序，先在y=0的平面上平移，这样相当于光源在实体
			        	 * 正上方随实体在y=0的平面上平移，再进行阴影投射矩阵的变换，固定
			        	 * 光源和投射平面的位置，然后在作与实体余下的同样的矩阵变换。这样做的
			        	 * 目的是将光源始终固定在实体的正上方。
			        	 */
			        	gl.glDisable(GL10.GL_DEPTH_TEST); // 取消深度检测，仅用于兴趣点标识的阴影
			        	gl.glEnable(GL10.GL_STENCIL_TEST); // 启用模板检测，仅用于兴趣点标识的阴影
			        	gl.glPushMatrix();	        	
			        	// 计算兴趣点在OpenGL以用户为原点坐标系下的坐标值，注意z轴的方向
					    gl.glTranslatef(
					    		(float)((an.getPOILocation()[i][1]-an.getMyLocation()[1])/0.00001018473125), 
					    		0, 
							    -(float)((an.getPOILocation()[i][0]-an.getMyLocation()[0])/0.00001018473125));	
					    
					    // 阴影投射变换
			         	gl.glMultMatrixf(shadowMatrix, 0);
					    
			         	// 作与实体余下的同样的矩阵变换
					    gl.glTranslatef(0.0f, 10.0f, 0.0f);	        	
					    gl.glScalef(10.0f, 10.0f, 10.0f);
					    gl.glRotatef(rquad, 1.0f, 1.0f, 1.0f);
			        	cube.draw(gl, true);	        	
			        	gl.glPopMatrix();	
			        	gl.glDisable(GL10.GL_STENCIL_TEST); // 取消模板检测
			        	gl.glEnable(GL10.GL_DEPTH_TEST); // 启用深度检测
			        	
			        	/*
			        	 * 再画实体
			        	 */
			        	gl.glPushMatrix();       	
			        	
					    // 计算兴趣点在OpenGL以用户为原点坐标系下的坐标值，注意z轴的方向
					    gl.glTranslatef(
					    		(float)((an.getPOILocation()[i][1]-an.getMyLocation()[1])/0.00001018473125), 
							    10.0f, // 不考虑海拔因素
							    -(float)((an.getPOILocation()[i][0]-an.getMyLocation()[0])/0.00001018473125));
					    gl.glScalef(10.0f, 10.0f, 10.0f);
					    gl.glRotatef(rquad, 1.0f, 1.0f, 1.0f);	
					    cube.draw(gl, false);							
					    gl.glPopMatrix();	        	
		        	}
	        		else{
	        			/*
			        	 * 仅画实体，考虑海拔因素
			        	 */
			        	gl.glPushMatrix();       	
			        	
					    // 计算兴趣点在OpenGL以用户为原点坐标系下的坐标值，注意z轴的方向
					    gl.glTranslatef(
					    		(float)((an.getPOILocation()[i][1]-an.getMyLocation()[1])/0.00001018473125), 
							    (float)(an.getPOILocation()[i][2]-an.getMyLocation()[2]), // 考虑海拔因素
							    -(float)((an.getPOILocation()[i][0]-an.getMyLocation()[0])/0.00001018473125));
					    gl.glScalef(10.0f, 10.0f, 10.0f);
					    gl.glRotatef(rquad, 1.0f, 1.0f, 1.0f);	
					    cube.draw(gl, false);							
					    gl.glPopMatrix();	        			
	        		}	        		        		
	        	}        			
			    i++;	    	    
		    }	
	
	        rquad -= 1.0f;
	        
	        /** 
	         * 画路径
	         */        
	        findNearestPosition(an.getMyLocation(), routeIndexSize);
	        extractRoute(routeIndexSize);
	        gps2GL(an.getMyLocation());
	        float angleX; // 每一段路径绕X轴需要旋转的角度，海拔因素
	        float angleY; // 每一段路径绕Y轴需要旋转的角度
	        
	        if(isShadow){
	        	/* 
	        	 * 先画阴影，采用平面阴影方法。
	        	 * 路径的阴影投射中，最开始就进行阴影投射的矩阵变换，固定光源和
	        	 * 投射平面的位置，这样做的效果相当于光源始终在用户当前位置的正
	        	 * 上方。
	        	 */	        
		        gl.glDisable(GL10.GL_DEPTH_TEST); // 取消深度检测，用于路径的阴影和实体	    
		        gl.glEnable(GL10.GL_STENCIL_TEST); // 启用模板检测，用于路径的阴影
	            gl.glPushMatrix();        
	            
	             // 阴影投射变换
	         	gl.glMultMatrixf(shadowMatrix, 0);
	            
	            gl.glTranslatef(0.0f, 0.5f, 0.0f); // 将路径朝上平移0.5米
		        
		        // 先画用户当前位置到离用户最近的规划路径坐标点的线段
	            routeLength = (float) Math.sqrt((Math.pow(routeSectionGL[0][0] - 0.0, 2) + Math.pow(routeSectionGL[0][2] - 0.0, 2))); // 计算距离
	            if(routeSectionGL[0][0] > 0.0 && routeSectionGL[0][2] == 0.0) // 坐标落在x正轴上
		    		angleY = 90.0f; 
		    	else if (routeSectionGL[0][0] < 0.0 && routeSectionGL[0][2] == 0.0) // 坐标落在x负轴上
		    		angleY = -90.0f;
		    	else 
		    	{
		    		angleY = (float) Math.atan(routeSectionGL[0][0]/-routeSectionGL[0][2]); // 计算弧度，注意场景坐标系中z轴方向是朝内的，-routeSectionGL[0][2]用来转换成朝外
		    	    angleY = (float) Math.toDegrees(angleY); // 转换成角度
		            if(routeSectionGL[0][0] >= 0.0 && routeSectionGL[0][2] > 0.0) // 坐标落在z正轴上或落入的象限为第四象限
		    		    angleY = 180.0f + angleY;
		    	    else if(routeSectionGL[0][0] < 0.0 && routeSectionGL[0][2] > 0.0) // 坐标落入的象限为第三象限
		    		    angleY = -180.0f + angleY;
		    	}
	            gl.glRotatef(-angleY, 0.0f, 1.0f, 0.0f); // 绕Y轴旋转路径段到指定的方向
		    	rectangle.draw(gl, this, true);
		    	
		    	// 再画规划路径
		        i = 0; 
		    	while(i < routeSectionIndexSize - 1)
		    	{
		    		gl.glTranslatef(0.0f, 0.0f, -routeLength); // 平移到下一个规划路径坐标点
		            gl.glRotatef(angleY, 0.0f, 1.0f, 0.0f); // 绕Y轴旋转回正方向，朝北
		    		float dx = (float) (routeSectionGL[i+1][0] - routeSectionGL[i][0]); // 两坐标的x差值
		    		float dz = (float) (routeSectionGL[i+1][2] - routeSectionGL[i][2]); // 两坐标的z差值

		    		routeLength = (float) Math.sqrt((Math.pow(dx, 2) + Math.pow(dz, 2))); // 计算距离
		    		if(dx > 0.0 && dz == 0.0) // 坐标落在x正轴上
		    		    angleY = 90.0f; 
		    	    else if (dx < 0.0 && dz == 0.0) // 坐标落在x负轴上
		    		    angleY = -90.0f;
		    	    else 
		    		{
		    			angleY = (float) Math.atan(dx/-dz); // 计算弧度，注意场景坐标系中z轴方向是朝内的，-dz用来转换成朝外
		    	        angleY = (float) Math.toDegrees(angleY); // 转换成角度
		    		    if(dx >= 0.0 && dz > 0.0) // 坐标落在z正轴上或落入的象限为第四象限
		    		        angleY = 180.0f + angleY;
		    	        else if(dx < 0.0 && dz > 0.0) // 坐标落入的象限为第三象限
		    		        angleY = -180.0f + angleY;
		    		}
		    		gl.glRotatef(-angleY, 0.0f, 1.0f, 0.0f); // 绕Y轴旋转路径段到指定的方向
		    		rectangle.draw(gl, this, true);

		    		i++;
		    	}	        		        
		        gl.glPopMatrix();  
		        gl.glDisable(GL10.GL_STENCIL_TEST); // 取消模板检测
		        
		        /*
	        	 * 再画实体
	        	 */
		        gl.glPushMatrix();
		        
		        gl.glTranslatef(0.0f, 0.5f, 0.0f); // 将路径朝上平移0.5米
		        
		        // 先画用户当前位置到离用户最近的规划路径坐标点的线段
		        routeLength = (float) Math.sqrt((Math.pow(routeSectionGL[0][0] - 0.0, 2) + Math.pow(routeSectionGL[0][2] - 0.0, 2))); // 计算距离
		        if(routeSectionGL[0][0] > 0.0 && routeSectionGL[0][2] == 0.0) // 坐标落在x正轴上
		    		angleY = 90.0f; 
		    	else if (routeSectionGL[0][0] < 0.0 && routeSectionGL[0][2] == 0.0) // 坐标落在x负轴上
		    		angleY = -90.0f;
		    	else 
		    	{
		    		angleY = (float) Math.atan(routeSectionGL[0][0]/-routeSectionGL[0][2]); // 计算弧度，注意场景坐标系中z轴方向是朝内的，-routeSectionGL[0][2]用来转换成朝外
		    	    angleY = (float) Math.toDegrees(angleY); // 转换成角度
		            if(routeSectionGL[0][0] >= 0.0 && routeSectionGL[0][2] > 0.0) // 坐标落在z正轴上或落入的象限为第四象限
		    		    angleY = 180.0f + angleY;
		    	    else if(routeSectionGL[0][0] < 0.0 && routeSectionGL[0][2] > 0.0) // 坐标落入的象限为第三象限
		    		    angleY = -180.0f + angleY;
		    	}
		        gl.glRotatef(-angleY, 0.0f, 1.0f, 0.0f); // 绕Y轴旋转路径段到指定的方向
		    	//rectangle = new Rectangle(this, false); // 由于distance的值在Rectangle类中需使用，且是变化的，所以需要每一次都进行实例化
		    	rectangle.draw(gl, this, false);
		    	
		    	// 再画规划路径
		        i = 0; 
		    	while(i < routeSectionIndexSize - 1)
		    	{
		    		gl.glTranslatef(0.0f, 0.0f, -routeLength); // 平移到下一个规划路径坐标点
		            gl.glRotatef(angleY, 0.0f, 1.0f, 0.0f); // 绕Y轴旋转回正方向，朝北
		    		float dx = (float) (routeSectionGL[i+1][0] - routeSectionGL[i][0]); // 两坐标的x差值
		    		float dz = (float) (routeSectionGL[i+1][2] - routeSectionGL[i][2]); // 两坐标的z差值

		    		routeLength = (float) Math.sqrt((Math.pow(dx, 2) + Math.pow(dz, 2))); // 计算距离
		    		if(dx > 0.0 && dz == 0.0) // 坐标落在x正轴上
		    		    angleY = 90.0f; 
		    	    else if (dx < 0.0 && dz == 0.0) // 坐标落在x负轴上
		    		    angleY = -90.0f;
		    	    else 
		    		{
		    			angleY = (float) Math.atan(dx/-dz); // 计算弧度，注意场景坐标系中z轴方向是朝内的，-dz用来转换成朝外
		    	        angleY = (float) Math.toDegrees(angleY); // 转换成角度
		    		    if(dx >= 0.0 && dz > 0.0) // 坐标落在z正轴上或落入的象限为第四象限
		    		        angleY = 180.0f + angleY;
		    	        else if(dx < 0.0 && dz > 0.0) // 坐标落入的象限为第三象限
		    		        angleY = -180.0f + angleY;
		    		}
		    		gl.glRotatef(-angleY, 0.0f, 1.0f, 0.0f); // 绕Y轴旋转路径段到指定的方向
		    		//rectangle = new Rectangle(this, false); // 由于distance的值在Rectangle类中需使用，且是变化的，所以需要每一次都进行实例化
		    		rectangle.draw(gl, this, false);

		    		i++;
		    	}	      		        
		        gl.glPopMatrix();	
	        	gl.glEnable(GL10.GL_DEPTH_TEST); // 启用深度检测        	
	        }
	        else{
	        	/*
	        	 * 仅画实体，考虑海拔
	        	 */
		        gl.glDisable(GL10.GL_DEPTH_TEST); // 取消深度检测，用于路径的实体
		        gl.glPushMatrix();
		        
		        gl.glTranslatef(0.0f, 0.5f, 0.0f); // 将路径朝上平移0.5米
		        
		        // 先画用户当前位置到离用户最近的规划路径坐标点的线段
		        routeLength = (float) Math.sqrt((Math.pow(routeSectionGL[0][0] - 0.0, 2) + Math.pow(routeSectionGL[0][1] - 0.0, 2) + Math.pow(routeSectionGL[0][2] - 0.0, 2))); // 计算距离
		        angleX = (float) Math.toDegrees(Math.asin(routeSectionGL[0][1]/routeLength));
		        if(routeSectionGL[0][0] > 0.0 && routeSectionGL[0][2] == 0.0) // 坐标落在x正轴上
		    		angleY = 90.0f; 
		    	else if (routeSectionGL[0][0] < 0.0 && routeSectionGL[0][2] == 0.0) // 坐标落在x负轴上
		    		angleY = -90.0f;
		    	else 
		    	{
		    		angleY = (float) Math.atan(routeSectionGL[0][0]/-routeSectionGL[0][2]); // 计算弧度，注意场景坐标系中z轴方向是朝内的，-routeSectionGL[0][2]用来转换成朝外
		    	    angleY = (float) Math.toDegrees(angleY); // 转换成角度
		            if(routeSectionGL[0][0] >= 0.0 && routeSectionGL[0][2] > 0.0) // 坐标落在z正轴上或落入的象限为第四象限
		    		    angleY = 180.0f + angleY;
		    	    else if(routeSectionGL[0][0] < 0.0 && routeSectionGL[0][2] > 0.0) // 坐标落入的象限为第三象限
		    		    angleY = -180.0f + angleY;
		    	}
		        gl.glRotatef(-angleY, 0.0f, 1.0f, 0.0f); // 绕Y轴旋转路径段到指定的方向
		    	gl.glRotatef(angleX, 1.0f, 0.0f, 0.0f); // 绕X轴旋转路径段到指定的方向
		    	//rectangle = new Rectangle(this, false); // 由于distance的值在Rectangle类中需使用，且是变化的，所以需要每一次都进行实例化
		    	rectangle.draw(gl, this, false);
		    	
		    	// 再画规划路径
		        i = 0; 
		    	while(i < routeSectionIndexSize - 1)
		    	{
		    		gl.glTranslatef(0.0f, 0.0f, -routeLength); // 平移到下一个规划路径坐标点
		    		gl.glRotatef(-angleX, 1.0f, 0.0f, 0.0f); // 绕X轴旋转回水平方向
		            gl.glRotatef(angleY, 0.0f, 1.0f, 0.0f); // 绕Y轴旋转回正方向，朝北
		    		float dx = (float) (routeSectionGL[i+1][0] - routeSectionGL[i][0]); // 两坐标的x差值
		    		float dy = (float) (routeSectionGL[i+1][1] - routeSectionGL[i][1]); // 两坐标的y差值
		    		float dz = (float) (routeSectionGL[i+1][2] - routeSectionGL[i][2]); // 两坐标的z差值

		    		routeLength = (float) Math.sqrt((Math.pow(dx, 2) + Math.pow(dy, 2) + Math.pow(dz, 2))); // 计算距离
		    		angleX = (float) Math.toDegrees(Math.asin(dy/routeLength));
		    		if(dx > 0.0 && dz == 0.0) // 坐标落在x正轴上
		    		    angleY = 90.0f; 
		    	    else if (dx < 0.0 && dz == 0.0) // 坐标落在x负轴上
		    		    angleY = -90.0f;
		    	    else 
		    		{
		    			angleY = (float) Math.atan(dx/-dz); // 计算弧度，注意场景坐标系中z轴方向是朝内的，-dz用来转换成朝外
		    	        angleY = (float) Math.toDegrees(angleY); // 转换成角度
		    		    if(dx >= 0.0 && dz > 0.0) // 坐标落在z正轴上或落入的象限为第四象限
		    		        angleY = 180.0f + angleY;
		    	        else if(dx < 0.0 && dz > 0.0) // 坐标落入的象限为第三象限
		    		        angleY = -180.0f + angleY;
		    		}
		    		gl.glRotatef(-angleY, 0.0f, 1.0f, 0.0f); // 绕Y轴旋转路径段到指定的方向
			    	gl.glRotatef(angleX, 1.0f, 0.0f, 0.0f); // 绕X轴旋转路径段到指定的方向
		    		//rectangle = new Rectangle(this, false); // 由于distance的值在Rectangle类中需使用，且是变化的，所以需要每一次都进行实例化
		    		rectangle.draw(gl, this, false);

		    		i++;
		    	}	      		        
		        gl.glPopMatrix();	
	        	gl.glEnable(GL10.GL_DEPTH_TEST); // 启用深度检测	        	
	        }	                
	    }		 						
	}

	@Override
	public void onSurfaceChanged(GL10 gl, int width, int height) {
		if(height == 0) { 						//Prevent A Divide By Zero By
			height = 1; 						//Making Height Equal One
		}

		gl.glViewport(0, 0, width, height); 	//Reset The Current Viewport
		gl.glMatrixMode(GL10.GL_PROJECTION); 	//Select The Projection Matrix
		gl.glLoadIdentity(); 					//Reset The Projection Matrix

		//Calculate The Aspect Ratio Of The Window
		// 45-50度为标准镜头的垂直视角
		GLU.gluPerspective(gl, 45.0f, (float)width / (float)height, 0.1f, 5000.0f);

		gl.glMatrixMode(GL10.GL_MODELVIEW); 	//Select The Modelview Matrix
		gl.glLoadIdentity(); 					//Reset The Modelview Matrix
	}
	
	/**
	 * 寻找离当前用户位置最近的路径点坐标。routeCurIndex获取需要的值
	 * @param curPos 用户的当前GPS坐标
	 * @param routeIndexSize 整条规划路径点坐标总数
	 */
	public void findNearestPosition(double[] curPos, int routeIndexSize){
		float[] distances = new float[routeIndexSize]; // 各路径点离用户的距离
		float shortestDis; // 路径点离用户的最短距离
		int i = 0;
		while(i < (routeIndexSize - 1)) // 地图整条规划路径中最后一个坐标是目的地，不需要计算在内
		{
			if(isShadow)
				distances[i] = (float) an.getDistanceWithoutAltitude(curPos[0], curPos[1], an.getRoute()[i][0], an.getRoute()[i][1]);
					//(float) Math.sqrt((Math.pow(curPos[0] - an.getRoute()[i][0], 2) + Math.pow(curPos[1] - an.getRoute()[i][1], 2))); // 平面空间中两点直线距离计算公式
			else
				distances[i] = (float) an.getDistanceWithAltitude(curPos[0], curPos[1], curPos[2], an.getRoute()[i][0], an.getRoute()[i][1], an.getRoute()[i][2]);
					//(float) Math.sqrt((Math.pow(curPos[0] - an.getRoute()[i][0], 2) + Math.pow(curPos[1] - an.getRoute()[i][1], 2) + Math.pow(curPos[2] - an.getRoute()[i][2], 2))); // 三维空间中两点直线距离计算公式
			i++;
		}

		shortestDis = distances[0];
		
		i = 1;
		while(i < (routeIndexSize - 1))
		{
			if(shortestDis > distances[i])
			{
				shortestDis = distances[i];
				routeCurIndex = i; // 需获取的值
			}
			i++;
		}
	}
	
	/**
	 * 从整条路径中提取当前路径点坐标前若干距离内的路径各点坐标。routeSection和routeSectionIndexSize获取需要的值
	 * @param routeIndexSize 整条规划路径点坐标总数
	 */
	public void extractRoute(int routeIndexSize){
		int i = 0;
		while(i < 3 && i+routeCurIndex < routeIndexSize) // 每次最多提取3个路径点
		{
			routeSection[i][0] = an.getRoute()[i+routeCurIndex][0]; // 需获取的值，纬度
			routeSection[i][1] = an.getRoute()[i+routeCurIndex][1]; // 需获取的值，经度
			routeSection[i][2] = an.getRoute()[i+routeCurIndex][2]; // 需获取的值，海拔
			i++;
		}
		routeSectionIndexSize = i; // 需获取的值
	} 
	
	/**
	 * 提取的路径点坐标到OpenGL坐标的格式转换，用户当前位置为原点。routeSectionGL获取需要的值
	 * @param curPos 用户的当前GPS坐标
	 */
	public void gps2GL(double[] curPos)
	{
		int i = 0;
		while(i < routeSectionIndexSize)
		{
			routeSectionGL[i][0] = (routeSection[i][1] - curPos[1])/0.00001018473125; // 需获取的值，经度相当于OpenGL坐标系的x
			routeSectionGL[i][1] = (routeSection[i][2] - curPos[2]); // 需获取的值，海拔相当于OpenGL坐标系的y。这里海拔是米制单位，所以，不需要转换 /*0.0;  // 需获取的值，海拔相当于OpenGL坐标系的y，目前不考虑海拔，设置为0*/
			routeSectionGL[i][2] = - (routeSection[i][0] - curPos[0])/0.00001018473125; // 需获取的值，注意z轴的方向，纬度相当于OpenGL坐标系的z
			i++;
		}
	}
	
	/**
	 * 返回路径段长度
	 * @return 返回路径段长度
	 */
	public float getRouteDistance(){
		return routeLength;
	}
}
