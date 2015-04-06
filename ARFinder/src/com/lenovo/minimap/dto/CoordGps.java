
/*
 * 文件名：	GPSCoord.java
 * 日期：	2010-1-25
 * 修改历史：
 * [时间]		[修改者]			[修改内容]
 */
package com.lenovo.minimap.dto;


/**
 * 
 * 版权所有(c)联想集团有限公司 1998-2010 保留所有权利.	<br />
 * 项目：	<br />
 * 描述：	坐标经纬度（GPS真实经纬度坐标）<br />
 * @author	zhangguojun<br />
 * @version	1.0
 * @since	JDK1.6, HttpClient4.0
 */
public class CoordGps {
	/** 坐标经度（GPS坐标） */
	private double x;
	/** 坐标纬度（GPS坐标） */
	private double y;
	
	/**
	 * 构造一个新的 CoordGps
	 */
	public CoordGps() {}
	
	/**
	 * 通过指定的x， y坐标（GPS坐标），构造一个新的 CoordGps
	 */
	public CoordGps(double x, double y) {
		this.x = x;
		this.y = y;
	}
	
	/**
	 * 返回坐标经度（GPS坐标）
	 * @return 坐标经度（GPS坐标）
	 */
	public double getX() {
	
		return x;
	}
	
	/**
	 * 设置坐标经度（GPS坐标）
	 * @param x 坐标经度（GPS坐标）
	 */
	public void setX(double x) {
	
		this.x = x;
	}
	
	/**
	 * 返回坐标纬度（GPS坐标）
	 * @return 坐标纬度（GPS坐标）
	 */
	public double getY() {
	
		return y;
	}
	
	/**
	 * 设置坐标纬度（GPS坐标）
	 * @param y 坐标纬度（GPS坐标）
	 */
	public void setY(double y) {
	
		this.y = y;
	}
	
}