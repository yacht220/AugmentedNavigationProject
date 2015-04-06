/*
 * 文件名：	ViaPlatform.java
 * 日期：	2010-1-26
 * 修改历史：
 * [时间]		[修改者]			[修改内容]
 */
package com.lenovo.minimap.dto;

/**
 * 版权所有(c)联想集团有限公司 1998-2010 保留所有权利. <br />
 * 项目： <br />
 * 描述： 途经站具体数据<br />
 * 
 * @author zhangguojun<br />
 * @version 1.0
 * @since JDK1.6, HttpClient4.0
 */

public class BusViaPlatform {

	/**
	 * 构造一个新的 BusViaPlatform
	 */
	public BusViaPlatform() {}
	
	/**
	 * 通过指定的途经站名称, coordX， coordY坐标（20级像素坐标），构造一个新的 BusViaPlatform
	 */
	public BusViaPlatform(String name, int coord20X, int coord20Y) {
		this.name = name;
		this.coord20X = coord20X;
		this.coord20Y = coord20Y;
	}

	/** 途经站名称 */
	private String name;
	/** 坐标经度（20级像素坐标） */
	private int coord20X;
	/** 坐标纬度（20级像素坐标） */
	private int coord20Y;

	/**
	 * 返回途经站名称
	 * 
	 * @return 途经站名称
	 */
	public String getName() {
		return name;
	}

	/**
	 * 设置途经站名称
	 * 
	 * @param name
	 *            途经站名称
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * 返回坐标经度（20级像素坐标）
	 * 
	 * @return 坐标经度（20级像素坐标）
	 */
	public int getCoord20X() {
		return coord20X;
	}

	/**
	 * 设置坐标经度（20级像素坐标）
	 * 
	 * @param coord20X
	 *            坐标经度（20级像素坐标）
	 */
	public void setCoord20X(int coord20X) {
		this.coord20X = coord20X;
	}

	/**
	 * 返回坐标纬度（20级像素坐标）
	 * 
	 * @return 坐标纬度（20级像素坐标）
	 */
	public int getCoord20Y() {
		return coord20Y;
	}

	/**
	 * 设置坐标纬度（20级像素坐标）
	 * 
	 * @param coord20Y 坐标纬度（20级像素坐标）
	 */
	public void setCoord20Y(int coord20Y) {
		this.coord20Y = coord20Y;
	}
}
