package com.lenovo.android.navigator;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.os.Handler;
import android.os.Vibrator;

public class Util {
	
	public static final int VIBARATION = 3000;

	public static final void decodeYUV(byte[] inputYUV420SP, int[] result,
			int width, int height) {
		final int frameSize = width * height;
		for (int j = 0, yp = 0; j < height; j++) {
			int uvp = frameSize + (j >> 1) * width, u = 0, v = 0;
			for (int i = 0; i < width; i++, yp++) {
				int y = (0xff & ((int) inputYUV420SP[yp])) - 16;
				if (y < 0)
					y = 0;
				if ((i & 1) == 0) {
					v = (0xff & inputYUV420SP[uvp++]) - 128;
					u = (0xff & inputYUV420SP[uvp++]) - 128;
				}
				int y1192 = 1192 * y;
				int r = (y1192 + 1634 * v);
				int g = (y1192 - 833 * v - 400 * u);
				int b = (y1192 + 2066 * u);
				if (r < 0)
					r = 0;
				else if (r > 262143)
					r = 262143;
				if (g < 0)
					g = 0;
				else if (g > 262143)
					g = 262143;
				if (b < 0)
					b = 0;
				else if (b > 262143)
					b = 262143;
				result[yp] = 0xe0000000 | ((r << 6) & 0xff0000)
						| ((g >> 2) & 0xff00) | ((b >> 10) & 0xff);
			}
		}
	}
	
    public static Bitmap setColorDatas(byte[] rawData,int width,int height) {    	
		int[] pickuppedColorData = new int[rawData.length];
		Util.decodeYUV(rawData, pickuppedColorData, width, height);    	
        Matrix rotation = new Matrix();
        rotation.setRotate(90, width / 2, height / 2);
        Bitmap bitmap = Bitmap.createBitmap(pickuppedColorData, width, height, Bitmap.Config.ARGB_8888);
        return Bitmap.createBitmap(bitmap, 0, 0, width, height, rotation, true);
    }    

    public static void asyncRequest(final Runnable r) {
        final Handler h = new Handler();
        final Thread t = new Thread(new Runnable() {
            public void run() {
                h.post(r);
            }
        });
        t.start();
    }
    
    public static String formatDistance(double distance) {
    	return ((int) distance) + "m";
    }
    
    public static void vibrate(Context context) {
        Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        vibrator.vibrate(VIBARATION);
    }
}
