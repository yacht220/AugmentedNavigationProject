/*
 * 文件名：	Header.java
 * 日期：	2010-1-8
 * 修改历史：
 * [时间]		[修改者]			[修改内容]
 */
package com.lenovo.minimap.dto;

import com.lenovo.minimap.HttpClientUtil;

/**
 * 版权所有(c)联想集团有限公司 1998-2010 保留所有权利。<br />
 * 项目：	<br />
 * 描述：	数据头和数据块部分数据处理<br />
 * @author	zhangguojun<br />
 * @version	1.0
 * @since	JDK1.6, HttpClient4.0
 */
public class Header {
	/** 数据头部分字节长度 */
	public static final int HEADER_LENGTH = 10;
	/** 标志位 */
	private int mark;
	/** 压缩标志(暂不支持)，0不压缩，1 gzip压缩,， zlib压缩,默认不压缩  */
	private int compress;
	/** 服务标志 */
	private int serviceMark;
	/** 错误代码 */
	private int errorCode;
	/**
	 * 返回标志位
	 * @return 标志位
	 */
	public int getMark() {
		return mark;
	}
	/**
	 * 设置标志位
	 * @param mark 标志位
	 */
	private void setMark(int mark) {
		this.mark = mark;
	}
	/**
	 * 返回压缩标志，0不压缩，1 gzip压缩,， zlib压缩,默认不压缩
	 * @return 压缩标志(暂不支持)，0不压缩，1 gzip压缩,， zlib压缩,默认不压缩
	 */
	public int getCompress() {
		return compress;
	}
	/**
	 * 设置压缩标志(暂不支持)，0不压缩，1 gzip压缩,， zlib压缩,默认不压缩
	 * @param compress 压缩标志(暂不支持)，0不压缩，1 gzip压缩,， zlib压缩,默认不压缩
	 */
	private void setCompress(int compress) {
		this.compress = compress;
	}
	/**
	 * 返回服务标志
	 * @return 服务标志
	 */
	public int getServiceMark() {
		return serviceMark;
	}
	/**
	 * 设置服务标志
	 * @param serviceMark 服务标志
	 */
	private void setServiceMark(int serviceMark) {
		this.serviceMark = serviceMark;
	}
	/**
	 * 返回错误代码
	 * @return 错误代码
	 */
	public int getErrorCode() {
		return errorCode;
	}
	/**
	 * 设置错误代码
	 * @param errorCode 错误代码
	 */
	private void setErrorCode(int errorCode) {
		this.errorCode = errorCode;
	}
	
	/**
	 * 处理字节数组，解析成Header的各个部分<br />
	 * <pre>
	 * 文件头部分
	 * 		标志位4字节	0 0 0 0 
	 * 		压缩标志1字节	0
	 * 
	 * 数据块部分
	 * 		服务标志4字节	0 0 0 0
	 * 		错误代码1字节	0  
	 * </pre>
	 * @param bytes 需要处理的字节数组
	 */
	public void process(byte[] bytes) {
		if(bytes.length < 10) return ;
		int indexh = 0;
		byte[] h1 = new byte[4];
		System.arraycopy(bytes, indexh, h1, 0, h1.length);
		setMark(HttpClientUtil.getInt(h1));
		indexh += h1.length;
		
		byte[] h2 = new byte[1];
		System.arraycopy(bytes, indexh, h2, 0, h2.length);
		setCompress(HttpClientUtil.getByte(h2));
		indexh += h2.length;
		
		byte[] h3 = new byte[4];
		System.arraycopy(bytes, indexh, h3, 0, h3.length);
		setServiceMark(HttpClientUtil.getInt(h3));
		indexh += h3.length;
		
		byte[] h4 = new byte[1];
		System.arraycopy(bytes, indexh, h4, 0, h4.length);
		setErrorCode(HttpClientUtil.getByte(h4));
		indexh += h4.length;
	}
	
	/**
	 * 便于打印该对象的各个详细的数据部分的字符串表示形式
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "Header [mark=" + mark + ", compress=" + compress 
				+ ", serviceMark=" + serviceMark + ", errorCode=" + errorCode + "]";
	}

}
