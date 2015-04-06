
/*
 * 文件名：	Coord.java
 * 日期：	2010-1-25
 * 修改历史：
 * [时间]		[修改者]			[修改内容]
 */
package com.lenovo.minimap.dto;


/**
 * 
 * 版权所有(c)联想集团有限公司 1998-2010 保留所有权利.	<br />
 * 项目：	<br />
 * 描述：	坐标经纬度（20级像素坐标）<br />
 * @author	zhangguojun<br />
 * @version	1.0
 * @since	JDK1.6, HttpClient4.0
 */
public class Coord20 {
	/** 坐标经度（20级像素坐标） */
	private int x;
	/** 坐标纬度（20级像素坐标） */
	private int y;
	
	/**
	 * 构造一个新的 Coord20
	 */
	public Coord20() {}
	
	/**
	 * 通过指定的x， y坐标（20级像素坐标），构造一个新的 Coord20
	 */
	public Coord20(int x, int y) {
		this.x = x;
		this.y = y;
	}
	
	/**
	 * 返回坐标经度（20级像素坐标）
	 * @return 坐标经度（20级像素坐标）
	 */
	public int getX() {
		return x;
	}
	
	/**
	 * 设置坐标经度（20级像素坐标）
	 * @param x 坐标经度（20级像素坐标）
	 */
	public void setX(int x) {
		this.x = x;
	}
	
	/**
	 * 返回坐标纬度（20级像素坐标）
	 * @return 坐标纬度（20级像素坐标）
	 */
	public int getY() {
		return y;
	}
	
	/**
	 * 设置坐标纬度（20级像素坐标）
	 * @param y 坐标纬度（20级像素坐标）
	 */
	public void setY(int y) {
		this.y = y;
	}
}