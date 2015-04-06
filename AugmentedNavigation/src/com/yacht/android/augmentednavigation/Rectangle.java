package com.yacht.android.augmentednavigation;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.opengles.GL10;

/**
 * This class is an object representation of 
 * a Rectangle containing the vertex information
 * and drawing functionality, which is called 
 * by the renderer.
 * 
 * @author Savas Ziplies (nea/INsanityDesign)
 */
public class Rectangle {		
	/** The buffer holding the vertices */
	private FloatBuffer vertexBuffer;
	
	/** The buffer holding the normal colors */
	private FloatBuffer colorBufferNormal;
	
	/** The buffer holding the shadow colors */
	private FloatBuffer colorBufferShadow;

	/** 
	 * The initial vertex declaration 
	 */
	private float vertices[];
	
	/** 
	 * The initial color definition 
	 * alpha值用于设置透明程度
	 */
	private float colorsNormal[] = {
		    					0.0f, 1.0f, 0.0f, 0.7f, 
		    					0.0f, 1.0f, 0.0f, 0.7f, 
		    					0.0f, 1.0f, 0.0f, 0.7f,	
		    					0.0f, 1.0f, 0.0f, 0.7f
	                            };
	
	/** 
	 * The initial color definition 
	 * alpha值用于设置透明程度
	 */
	private float colorsShadow[] = {
		    					0.0f, 0.0f, 0.0f, 0.7f, 
		    					0.0f, 0.0f, 0.0f, 0.7f, 
		    					0.0f, 0.0f, 0.0f, 0.7f,	
		    					0.0f, 0.0f, 0.0f, 0.7f
		    					};	
	
	/**
	 * The Rectangle constructor.
	 * 
	 * Initiate the buffers.
	 */
	public Rectangle() {		
		ByteBuffer byteBuf = ByteBuffer.allocateDirect(colorsNormal.length * 4);
		byteBuf.order(ByteOrder.nativeOrder());
		colorBufferNormal = byteBuf.asFloatBuffer();
		colorBufferNormal.put(colorsNormal);
		colorBufferNormal.position(0);
		
		byteBuf = ByteBuffer.allocateDirect(colorsShadow.length * 4);
		byteBuf.order(ByteOrder.nativeOrder());
		colorBufferShadow = byteBuf.asFloatBuffer();
		colorBufferShadow.put(colorsShadow);
		colorBufferShadow.position(0);			
	}

	/**
	 * The object own drawing function.
	 * Called from the renderer to redraw this instance
	 * with possible changes in values.
	 * 
	 * @param gl - The GL Context
	 * @param render - GLRender对象
	 * @param isDrawShadow - 是否渲染阴影，true为渲染阴影
	 */
	public void draw(GL10 gl, GLRender render, boolean isDrawShadow) {	
		// 初始化vertices，宽度为2米
		vertices = new float[]{
				-1.0f, 0.0f, 0.0f, 	//Bottom Left
				1.0f, 0.0f, 0.0f, 		//Bottom Right
				-1.0f, 0.0f, -render.getRouteDistance(),	 	//Top Left
				1.0f, 0.0f, -render.getRouteDistance() 		//Top Right
				};
		
		ByteBuffer byteBuf = ByteBuffer.allocateDirect(vertices.length * 4);
		byteBuf.order(ByteOrder.nativeOrder());
		vertexBuffer = byteBuf.asFloatBuffer();
		vertexBuffer.put(vertices);
		vertexBuffer.position(0);
		
		//Set the face rotation
		gl.glFrontFace(GL10.GL_CW);
		
		//Point to our vertex and color buffer
		gl.glVertexPointer(3, GL10.GL_FLOAT, 0, vertexBuffer);
		if(isDrawShadow){
			gl.glColorPointer(4, GL10.GL_FLOAT, 0, colorBufferShadow);
		}else{
			gl.glColorPointer(4, GL10.GL_FLOAT, 0, colorBufferNormal);
		}
		
		//Enable the vertex and color state
		gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
		gl.glEnableClientState(GL10.GL_COLOR_ARRAY);
		
		//Draw the vertices as triangle strip
		gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, vertices.length / 3);
		
		//Disable the client state before leaving
		gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
		gl.glDisableClientState(GL10.GL_COLOR_ARRAY);
	}
}
