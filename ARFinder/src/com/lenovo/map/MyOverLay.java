package com.lenovo.map;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.RectF;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.Projection;

public class MyOverLay extends Overlay {
	public static final int MODE_PIC = 0;
	public static final int MODE_START = 1;
	public static final int MODE_LINE = 2;
	public static final int MODE_END = 3;
	private int mode = 0;//0图片，1起点，2直线， 3终点
	private GeoPoint geoPointStart;
	private GeoPoint geoPointEnd;
	private Bitmap bitmap;
	private int mRadius = 2;
	private int id;

	public MyOverLay(GeoPoint geoPoint) {//画起点
		this.geoPointStart = geoPoint;
		this.mode = MODE_START;
	}
	
	public MyOverLay(GeoPoint geoPoint, Bitmap bitmap) {//画图片
		this.geoPointStart = geoPoint;
		this.bitmap = bitmap;
		this.mode = MODE_PIC;
	}
	
	public MyOverLay(GeoPoint geoPoint, int resDrawableId, Context context) {//画图片
		this.geoPointStart = geoPoint;
		this.bitmap = BitmapFactory.decodeResource(context.getResources(), resDrawableId);
		this.mode = MODE_PIC;
	}
	
	public MyOverLay(GeoPoint geoPointStart, GeoPoint geoPointEnd, int mode) {//画直线，终点
		this.geoPointStart = geoPointStart;
		this.geoPointEnd = geoPointEnd;
		this.mode = mode;
	}

	@Override
	public boolean draw(Canvas canvas, MapView mapView, boolean shadow, long when) {
		Projection projection = mapView.getProjection();
		if (shadow == false) {
			Paint paint = new Paint();//设置笔刷
			paint.setAntiAlias(true);
			paint.setColor(Color.BLACK);

			Point point = new Point();
			projection.toPixels(geoPointStart, point);
			switch (mode) {
			case MODE_PIC:
				canvas.drawBitmap(bitmap, point.x, point.y, paint);//画图片
				break;
			case MODE_START://mode=1：创建起点
				RectF oval = new RectF(point.x - mRadius, point.y - mRadius, point.x + mRadius, point.y + mRadius);//定义RectF对象
				canvas.drawOval(oval, paint);//绘制起点的圆形
				break;
			case MODE_LINE://mode=2：画路线
				Point point2 = new Point();
				projection.toPixels(geoPointEnd, point2);
				paint.setColor(Color.BLACK);
				paint.setStrokeWidth(5);
				paint.setAlpha(120);
				canvas.drawLine(point.x, point.y, point2.x, point2.y, paint);//画线
				break;
			case MODE_END:// mode=3：创建终点
				Point point3 = new Point();//避免误差，先画最后一段的路线
				projection.toPixels(geoPointEnd, point3);
				paint.setColor(Color.BLACK);
				paint.setStrokeWidth(5);
				paint.setAlpha(120);
				canvas.drawLine(point.x, point.y, point3.x, point3.y, paint);

				RectF endOval = new RectF(point3.x - mRadius, point3.y - mRadius, point3.x + mRadius, point3.y + mRadius);//定义RectF对象
				paint.setAlpha(255);
				canvas.drawOval(endOval, paint);//绘制终点的圆形
				break;
			default:
				break;
			}
		}
		return super.draw(canvas, mapView, shadow, when);
	}
	
	public void setId(int id) {
		this.id = id;
	}
	
	public int getId() {
		return this.id;
	}
}