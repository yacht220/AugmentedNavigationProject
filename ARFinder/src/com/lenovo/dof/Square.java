package com.lenovo.dof;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.opengles.GL10;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLUtils;

import com.lenovo.android.navigator.R;
import com.lenovo.android.navigator.ServiceProxy;
import com.lenovo.minimap.MinimapService;

/**
 * This class is an object representation of 
 * a Square containing the vertex information
 * and drawing functionality, which is called 
 * by the renderer.
 * 
 * @author Savas Ziplies (nea/INsanityDesign)
 */
public class Square {
	/** The buffer holding the vertices */
	private FloatBuffer vertexBufferPoi;
	
	/** The buffer holding the vertices */
	private FloatBuffer vertexBufferBackground;
	
	/** The buffer holding the texture coordinates */
	private FloatBuffer textureBuffer;
	
	/** The buffer holding the colors */
	private FloatBuffer colorBuffer;	
	
	/** Our texture pointer */
	private int[] texturePointer;
	
	/** 纹理图片，纹理图片的长宽像素尺寸最好为32、64、128、256和512，否则可能无法显示 */
	private static int[] TEXTURE_IMAGE = {R.drawable.user, R.drawable.pointblue, R.drawable.piontyellow, R.drawable.background};
	
	/** 纹理数量 */
	private static int TEXTURE_NUM = 4;
	
	/** 
	 * The initial vertex declaration 
	 */
	private float verticesPoi[] = {
			-5, -5, 0, 	//Bottom Left
			5, -5, 0,     //Bottom Right
			-5, 5, 0,	 	//Top Left
			5, 5, 0 		//Top Right
			};
	
	/** 
	 * The initial vertex declaration 
	 */
	private float verticesBackground[];
	
	/** 
	 * The initial color definition 
	 * alpha值用于设置透明程度
	 */
	private float colors[] = {
			                    1.0f, 1.0f, 1.0f, 1.0f, 
			                    1.0f, 1.0f, 1.0f, 1.0f, 
			                    1.0f, 1.0f, 1.0f, 1.0f,
			                    1.0f, 1.0f, 1.0f, 1.0f, 
	                            };
	
	/** The initial texture coordinates (u, v) */	
    private float textureCoord[] = {    		
			//Mapping coordinates for the vertices    		
    		0.0f, 1.0f, //Top Left
    		1.0f, 1.0f, //Top Right
    		0.0f, 0.0f, //Bottom Left
    		1.0f, 0.0f //Bottom Right   		
            };
	
	/**
	 * The Square constructor.
	 * 
	 * Initiate the buffers.
	 */
	public Square(ServiceProxy ms) {
		/* 初始化 */		
		verticesBackground = new float[]{
				0, 0, 0, 	//Bottom Left
				ms.getScreenWidth(), 0, 0,     //Bottom Right
				0, ms.getScreenHeight(), 0,	 	//Top Left
				ms.getScreenWidth(), ms.getScreenHeight(), 0 		//Top Right
				};
		
		texturePointer = new int[TEXTURE_NUM];
		
		ByteBuffer byteBuf = ByteBuffer.allocateDirect(verticesPoi.length * 4);
		byteBuf.order(ByteOrder.nativeOrder());
		vertexBufferPoi = byteBuf.asFloatBuffer();
		vertexBufferPoi.put(verticesPoi);
		vertexBufferPoi.position(0);
		
		byteBuf = ByteBuffer.allocateDirect(verticesBackground.length * 4);
		byteBuf.order(ByteOrder.nativeOrder());
		vertexBufferBackground = byteBuf.asFloatBuffer();
		vertexBufferBackground.put(verticesBackground);
		vertexBufferBackground.position(0);
		
		byteBuf = ByteBuffer.allocateDirect(colors.length * 4);
		byteBuf.order(ByteOrder.nativeOrder());
		colorBuffer = byteBuf.asFloatBuffer();
		colorBuffer.put(colors);
		colorBuffer.position(0);	
		
		byteBuf = ByteBuffer.allocateDirect(textureCoord.length * 4);
		byteBuf.order(ByteOrder.nativeOrder());
		textureBuffer = byteBuf.asFloatBuffer();
		textureBuffer.put(textureCoord);
		textureBuffer.position(0);	
	}

	/**
	 * The object own drawing function.
	 * Called from the renderer to redraw this instance
	 * with possible changes in values.
	 * 
	 * @param gl - The GL Context
	 * @param mode - 0为渲染用户，1为渲染兴趣点（蓝），2为渲染兴趣点（黄），3为渲染背景
	 */
	public void draw(GL10 gl, int mode) {
		//Bind our generated texture
		gl.glBindTexture(GL10.GL_TEXTURE_2D, texturePointer[mode]);		
		
		//Set the face rotation
		gl.glFrontFace(GL10.GL_CW);
		
		//Point to our buffer
		// 判断，若渲染背景，就选择背景的顶点buffer，否则选择用户和兴趣点的顶点buffer
		if(mode == 3){
			gl.glVertexPointer(3, GL10.GL_FLOAT, 0, vertexBufferBackground);
		}else{
			gl.glVertexPointer(3, GL10.GL_FLOAT, 0, vertexBufferPoi);
		}		
		gl.glColorPointer(4, GL10.GL_FLOAT, 0, colorBuffer);
		gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, textureBuffer);
		
		//Enable the vertex, color and texture state
		gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
		gl.glEnableClientState(GL10.GL_COLOR_ARRAY);
		gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
		
		//Draw the vertices as triangle strip
		gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, verticesBackground.length / 3);
		
		//Disable the client state before leaving
		gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
		gl.glDisableClientState(GL10.GL_COLOR_ARRAY);
		gl.glDisableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
	}
	
	/**
	 * Load the textures
	 * 
	 * @param gl - The GL Context
	 * @param context - The Activity context
	 */
	public void loadGLTexture(GL10 gl, Context context) {		
		//Generate one texture pointer...
		gl.glGenTextures(TEXTURE_NUM, texturePointer, 0);
		int i = 0; // 计数器
		while(i < TEXTURE_NUM){
			//Get the texture from the Android resource directory		
			InputStream is = context.getResources().openRawResource(TEXTURE_IMAGE[i]);
			Bitmap bitmap = null;
			try {
				//BitmapFactory is an Android graphics utility for images
				bitmap = BitmapFactory.decodeStream(is);

			} finally {
				//Always clear and close
				try {
					is.close();
					is = null;
				} catch (IOException e) {
				}
			}			
			//...and bind it to our array
			gl.glBindTexture(GL10.GL_TEXTURE_2D, texturePointer[i]);
			
			//Create Nearest Filtered Texture
			gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_NEAREST);
			gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);

			//Different possible texture parameters, e.g. GL10.GL_CLAMP_TO_EDGE
			gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S, GL10.GL_REPEAT);
			gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T, GL10.GL_REPEAT);
			
			//Use the Android GLUtils to specify a two-dimensional texture image from our bitmap
			GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, bitmap, 0);
			
			//Clean up
			bitmap.recycle();		
			
			i++;
		}
	}
}
