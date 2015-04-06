package com.lenovo.android.navigator;

import java.io.IOException;


import android.content.Context;

import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.util.AttributeSet;

import android.view.SurfaceHolder;
import android.view.SurfaceView;

import android.view.SurfaceHolder.Callback;



/*
 * 实景模式的实时取景
 */
public class CameraView extends SurfaceView implements Callback {

	private SurfaceHolder holder;
	public Camera camera = null;
	private boolean inProgress = false;
	private boolean hasSurface = false;
	private boolean isPreviewing = false;
	public  byte[] snapShot;

	public void takePicture() {
		camera.setPreviewCallback(new RawPreviewCallback());
		if (!inProgress) {
			inProgress = true;
			camera.setPreviewCallback(new RawPreviewCallback());
		}
	}

	private final class RawPreviewCallback implements PreviewCallback {
		public void onPreviewFrame(byte[] rawData, Camera camera) {
			snapShot = rawData;			
			
			camera.setPreviewCallback(null);
			
			CameraActivity act = ((CameraActivity) CameraView.this.getContext());
			if (!act.anchorView.lock) {
				stopCameraPreview();
				((CameraActivity) CameraView.this.getContext()).startSnapShotActivity();
			}
		}
	}

	public CameraView(Context context, AttributeSet attrs) {
		super(context, attrs);
		holder = getHolder();
		initSurface();
	}

	private void initSurface() {
		holder.addCallback(this);
		holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
	}

	public void startCameraPreview() {
		if (!isPreviewing) {
			if (camera != null)
				camera.startPreview();
			isPreviewing = true;
		}
	}

	public void stopCameraPreview() {
		if (isPreviewing) {
			if (camera != null)
				camera.stopPreview();
			isPreviewing = false;
		}
	}

	private void cameraDestroy() {
		if (camera != null) {
			stopCameraPreview();
			camera.release();
			camera = null;
		}
	}

	public void onResume() {
		if (hasSurface) {
			surfaceCreated(holder);
			startCameraPreview();
		} else {
			initSurface();
		}
	}

	public void onPause() {
		cameraDestroy();
	}

	public void surfaceCreated(SurfaceHolder holder) {
		camera = Camera.open();
		Camera.Parameters parameter = camera.getParameters();
		parameter.set("orientation", "portrait"); // fix orientation error in Android 1.5. API level 5 has setRotation
		camera.setParameters(parameter);

		try {
			camera.setPreviewDisplay(holder);
		} catch (IOException exception) {
			camera.release();
			camera = null;
		}
		hasSurface = true;
	}

	public void surfaceDestroyed(SurfaceHolder holder) {
		cameraDestroy();
		hasSurface = false;
	}

	public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
		Camera.Parameters parameter = camera.getParameters();
		parameter.setPreviewSize(w, h);   // fix orientation error in Android 1.5. API level 5 has setRotation
		camera.setParameters(parameter);
		startCameraPreview();
	}
}
