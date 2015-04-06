package com.lenovo.android.navigator;

import android.graphics.Bitmap.Config;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.PaintDrawable;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.graphics.Canvas;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.Paint;
import android.graphics.Rect;

import android.content.Context;

final class Utilities {
    private static int sIconWidth = 27;
    private static int sIconHeight = 40;

    private static final Rect sOldBounds = new Rect();
    private static Canvas sCanvas = new Canvas();
    private static final int TRANSPARENT = 0x80;

    static {
        sCanvas.setDrawFilter(new PaintFlagsDrawFilter(Paint.DITHER_FLAG,
                Paint.FILTER_BITMAP_FLAG));
    }

    private static final int ICON_WIDTH 	= 27;
    private static final int ICON_HEIGHT 	= 40;
    
    /*
     * 创建缩略图
     */
    static Drawable createIconThumbnail(Drawable icon, Context context) {
        sIconWidth = ICON_WIDTH;
        sIconHeight = ICON_HEIGHT;

        int width = sIconWidth;
        int height = sIconHeight;

        float scale = 1.0f;
        if (icon instanceof PaintDrawable) {
            PaintDrawable painter = (PaintDrawable) icon;
            painter.setIntrinsicWidth(width);
            painter.setIntrinsicHeight(height);
        } else if (icon instanceof BitmapDrawable) {
        }
        int iconWidth = icon.getIntrinsicWidth();
        int iconHeight = icon.getIntrinsicHeight();

        if (width > 0 && height > 0) {
            if (width < iconWidth || height < iconHeight || scale != 1.0f) {
                final float ratio = (float) iconWidth / iconHeight;

                if (iconWidth > iconHeight) {
                    height = (int) (width / ratio);
                } else if (iconHeight > iconWidth) {
                    width = (int) (height * ratio);
                }

                final Bitmap.Config c = icon.getOpacity() != PixelFormat.OPAQUE ?
                            Bitmap.Config.ARGB_8888 : Bitmap.Config.RGB_565;
                final Bitmap thumb = Bitmap.createBitmap(sIconWidth, sIconHeight, c);
                final Canvas canvas = sCanvas;
                canvas.setBitmap(thumb);
                // Copy the old bounds to restore them later
                // If we were to do oldBounds = icon.getBounds(),
                // the call to setBounds() that follows would
                // change the same instance and we would lose the
                // old bounds
                sOldBounds.set(icon.getBounds());
                final int x = (sIconWidth - width) / 2;
                final int y = (sIconHeight - height) / 2;
                icon.setBounds(x, y, x + width, y + height);
                icon.draw(canvas);
                icon.setBounds(sOldBounds);
                icon = new FastBitmapDrawable(thumb);
            } else if (iconWidth < width && iconHeight < height) {
                final Bitmap.Config c = Bitmap.Config.ARGB_8888;
                final Bitmap thumb = Bitmap.createBitmap(sIconWidth, sIconHeight, c);
                final Canvas canvas = sCanvas;
                canvas.setBitmap(thumb);
                sOldBounds.set(icon.getBounds());
                final int x = (width - iconWidth) / 2;
                final int y = (height - iconHeight) / 2;
                icon.setBounds(x, y, x + iconWidth, y + iconHeight);
                icon.draw(canvas);
                icon.setBounds(sOldBounds);
                icon = new FastBitmapDrawable(thumb);
            }
        }

        return icon;
    }
    
    /*
     * 设置图片的alpha值
     */
    public static Bitmap setAlpha(Bitmap sourceImg, int number) {
    	int[] argb = new int[sourceImg.getWidth() * sourceImg.getHeight()];
    	sourceImg.getPixels(argb, 0, sourceImg.getWidth(), 0, 0,sourceImg.getWidth(), sourceImg.getHeight());// 获得图片的ARGB值
    	number = number * 255 / 100;
    	for (int i = 0; i < argb.length; i++) {
    		argb[i] = (number << 24) | (argb[i] & 0x00FFFFFF);// 修改最高2位的值
    	}
    	sourceImg = Bitmap.createBitmap(argb, sourceImg.getWidth(), sourceImg.getHeight(), Config.ARGB_8888);  
    	return sourceImg;
	}

    /*
     * 创建缩略图, 相对于createIconThumbnail, createIconThumbnail2的透明度为
     */
    static Drawable createIconThumbnail2(Drawable icon, Context context) {
        sIconWidth = ICON_WIDTH;
        sIconHeight = ICON_HEIGHT;

        int width = sIconWidth;
        int height = sIconHeight;

        float scale = 1.0f;
        if (icon instanceof PaintDrawable) {
            PaintDrawable painter = (PaintDrawable) icon;
            painter.setIntrinsicWidth(width);
            painter.setIntrinsicHeight(height);
        } else if (icon instanceof BitmapDrawable) {

        }
        int iconWidth = icon.getIntrinsicWidth();
        int iconHeight = icon.getIntrinsicHeight();

        if (width > 0 && height > 0) {
            if (width < iconWidth || height < iconHeight || scale != 1.0f) {
                final float ratio = (float) iconWidth / iconHeight;

                if (iconWidth > iconHeight) {
                    height = (int) (width / ratio);
                } else if (iconHeight > iconWidth) {
                    width = (int) (height * ratio);
                }

                final Bitmap.Config c = icon.getOpacity() != PixelFormat.OPAQUE ?
                            Bitmap.Config.ARGB_8888 : Bitmap.Config.RGB_565;
                final Bitmap thumb = Bitmap.createBitmap(sIconWidth, sIconHeight, c);
                final Canvas canvas = sCanvas;
                canvas.setBitmap(thumb);

                sOldBounds.set(icon.getBounds());
                final int x = (sIconWidth - width) / 2;
                final int y = (sIconHeight - height) / 2;
                icon.setBounds(x, y, x + width, y + height);
                icon.draw(canvas);
                icon.setBounds(sOldBounds);
                icon = new FastBitmapDrawable(Utilities.setAlpha(thumb, TRANSPARENT));
            } else if (iconWidth < width && iconHeight < height) {
                final Bitmap.Config c = Bitmap.Config.ARGB_8888;
                final Bitmap thumb = Bitmap.createBitmap(sIconWidth, sIconHeight, c);
                final Canvas canvas = sCanvas;
                canvas.setBitmap(thumb);
                sOldBounds.set(icon.getBounds());
                final int x = (width - iconWidth) / 2;
                final int y = (height - iconHeight) / 2;
                icon.setBounds(x, y, x + iconWidth, y + iconHeight);
                icon.draw(canvas);
                icon.setBounds(sOldBounds);
                icon = new FastBitmapDrawable(Utilities.setAlpha(thumb, TRANSPARENT));
            }
        }

        return icon;
    }
}
