package com.yacht.android.augmentednavigation;

import java.util.List;

import android.content.Context;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class CameraView extends SurfaceView {
	/** Camera实例 */
	Camera camera;
	/** SurfaceHolder实例 */
	SurfaceHolder previewHolder;
	/** SurfaceHolder监听器 */
	SurfaceHolder.Callback surfaceHolderListener = new SurfaceHolder.Callback() {
		@Override
		public void surfaceCreated(SurfaceHolder holder) {
			camera = Camera.open();
			
			/* 使在portrait模式下预览图像方向正常 */
			Parameters params = camera.getParameters();
//			params.set("orientation", "portrait"); //1.x有效
			params.setSceneMode("portrait");//2.0、2.1有效			
//			params.setRotation(90);
			camera.setParameters(params); 

			try{
				camera.setPreviewDisplay(previewHolder);
			}catch(Throwable t){}
		}
		
		@Override
		public void surfaceChanged(SurfaceHolder holder, int format, int width,
				int height) {
			Parameters params = camera.getParameters();
			List<Size> sizes = params.getSupportedPreviewSizes();
			Size optimalSize = getOptimalPreviewSize(sizes, width, height);
			params.setPreviewSize(optimalSize.width, optimalSize.height );
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
	
	private Size getOptimalPreviewSize(List<Size> sizes, int w, int h) {
        final double ASPECT_TOLERANCE = 0.05;
        double targetRatio = (double) w / h;
        if (sizes == null) return null;

        Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        int targetHeight = h;

        // Try to find an size match aspect ratio and size
        for (Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        // Cannot find the one match the aspect ratio, ignore the requirement
        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        return optimalSize;
    }
	
	/**
	 * 构造函数
	 * @param context Context实例
	 */
	public CameraView(Context context/*, AttributeSet attrs*/) {
		super(context/*, attrs*/);
		
		previewHolder = this.getHolder();
		previewHolder.addCallback(surfaceHolderListener);
		previewHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);				
	}
}