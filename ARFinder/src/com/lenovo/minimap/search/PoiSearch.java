/*
 * 文件名：	PioSearch.java
 * 日期：	2010-1-8
 * 修改历史：
 * [时间]		[修改者]			[修改内容]
 */
package com.lenovo.minimap.search;

import java.util.ArrayList;
import java.util.List;

import com.lenovo.minimap.HttpClientUtil;

/**
 * 版权所有(c)联想集团有限公司 1998-2010 保留所有权利。<br />
 * 项目：	<br />
 * 描述：	Poi搜索结果的解析操作<br />
 * @author	zhangguojun<br />
 * @version	1.0
 * @since	JDK1.6, HttpClient4.0
 */
public class PoiSearch {
	/** 数据包总长度 */
	private int dataLength;
	/** 总结果数 */
	private int resultSum;
	/** 此次返回数据条数 */
	private int resultSize;
	/** 处理结果的List数组 */
	private List<Poi> pois = null;
	
	/**
	 * 返回数据包总长度
	 * @return 数据包总长度
	 */
	public int getDataLength() {
		return dataLength;
	}
	/**
	 * 返回总结果数
	 * @return 总结果数
	 */
	public int getResultSum() {
		return resultSum;
	}
	/**
	 * 返回此次返回数据条数
	 * @return 此次返回数据条数
	 */
	public int getResultSize() {
		return resultSize;
	}
	/**
	 * 返回处理结果的List数组
	 * @return 处理结果的List数组
	 */
	public List<Poi> getPois() {
		return pois;
	}
	
	/**
	 * 
	 * 版权所有(c)联想集团有限公司 1998-2010 保留所有权利.	<br />
	 * 项目：	<br />
	 * 描述：	具体的Poi搜索得到的详细数据<br />
	 * @author	zhangguojun<br />
	 * @version	1.0
	 * @since	JDK1.6, HttpClient4.0
	 */
	public class Poi {
		/** 该条数据总长度 */
		private int dataLength;
		/** poiId */
		private String poiId;
		/** 关键字对应的地点名称 */
		private String name;
		/** 具体地址 */
		private String address;
		/** 电话 */
		private String phoneNumber;
		/** 20级像素x坐标 */
		private int coord20X;
		/** 20级像素y坐标 */
		private int coord20Y;
		/** 城市代码 */
		private String cityCode;
		
		/**
		 * 返回该条数据总长度
		 * @return 该条数据总长度
		 */
		public int getDataLength() {
			return dataLength;
		}
		/**
		 * 返回poiId
		 * @return poiId
		 */
		public String getPoiId() {
			return poiId;
		}
		/**
		 * 返回关键字对应的地点名称
		 * @return 关键字对应的地点名称
		 */
		public String getName() {
			return name;
		}
		/**
		 * 返回具体地址
		 * @return 具体地址
		 */
		public String getAddress() {
			return address;
		}
		/**
		 * 返回电话
		 * @return 电话
		 */
		public String getPhoneNumber() {
			return phoneNumber;
		}
		/**
		 * 返回20级像素x坐标
		 * @return 20级像素x坐标
		 */
		public int getCoord20X() {
			return coord20X;
		}
		/**
		 * 返回20级像素y坐标
		 * @return 20级像素y坐标
		 */
		public int getCoord20Y() {
			return coord20Y;
		}
		/**
		 * 返回城市代码
		 * @return 城市代码
		 */
		public String getCityCode() {
			return cityCode;
		}
	}
	
	/**
	 * 处理字节数组
	 * @param bytes 需要处理的字节数组
	 * @param charset 字符串编码
	 */
	public void process(byte[] bytes, String charset) {
		int index = 0;
		byte[] h1 = new byte[4];
		System.arraycopy(bytes, index, h1, 0, h1.length);
		dataLength = HttpClientUtil.getInt(h1);
		index += h1.length;
		
		byte[] h2 = new byte[4];
		System.arraycopy(bytes, index, h2, 0, h2.length);
		resultSum = HttpClientUtil.getByte(h2);
		index += h2.length;
		
		byte[] h3 = new byte[4];
		System.arraycopy(bytes, index, h3, 0, h3.length);
		resultSize = HttpClientUtil.getInt(h3);
		index += h3.length;
		
		pois = new ArrayList<Poi>();
		for (int i = 0; i < getResultSize(); i++) {
			Poi poi = new Poi();
			byte[] d1 = new byte[4];
			System.arraycopy(bytes, index, d1, 0, d1.length);
			poi.dataLength = HttpClientUtil.getInt(d1);
			index += d1.length;
			
			byte[] d2 = new byte[2];
			System.arraycopy(bytes, index, d2, 0, d2.length);
			byte[] d3 = new byte[HttpClientUtil.getShort(d2)];
			System.arraycopy(bytes, index + d2.length, d3, 0, d3.length);
			poi.poiId = new String(d3);
			index += d2.length + d3.length;
			
			byte[] d4 = new byte[2];
			System.arraycopy(bytes, index, d4, 0, d4.length);
			byte[] d5 = new byte[HttpClientUtil.getShort(d4)];
			System.arraycopy(bytes, index + d4.length, d5, 0, d5.length);
			poi.name = HttpClientUtil.getString(d5, charset);
			index += d4.length + d5.length;

			byte[] d6 = new byte[2];
			System.arraycopy(bytes, index, d6, 0, d6.length);
			byte[] d7 = new byte[HttpClientUtil.getShort(d6)];
			System.arraycopy(bytes, index + d6.length, d7, 0, d7.length);
			poi.address = HttpClientUtil.getString(d7, charset);
			index += d6.length + d7.length;
			
			byte[] d8 = new byte[2];
			System.arraycopy(bytes, index, d8, 0, d8.length);
			byte[] d9 = new byte[HttpClientUtil.getShort(d8)];
			System.arraycopy(bytes, index + d8.length, d9, 0, d9.length);
			poi.phoneNumber = HttpClientUtil.getString(d9, charset);
			index += d8.length + d9.length;
			
			byte[] d10 = new byte[4];
			System.arraycopy(bytes, index, d10, 0, d10.length);
			poi.coord20X = HttpClientUtil.getInt(d10);
			index += d10.length;
			
			byte[] d11 = new byte[4];
			System.arraycopy(bytes, index, d11, 0, d11.length);
			poi.coord20Y = HttpClientUtil.getInt(d11);
			index += d11.length;
			
			byte[] d12 = new byte[2];
			System.arraycopy(bytes, index, d12, 0, d12.length);
			byte[] d13 = new byte[HttpClientUtil.getShort(d12)];
			System.arraycopy(bytes, index + d12.length, d13, 0, d13.length);
			poi.cityCode = HttpClientUtil.getString(d13, charset);
			index += d12.length +  d13.length;
			pois.add(poi);
		}
	}
	
	/**
	 * 便于打印该对象的各个详细的数据部分的字符串表示形式
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("PoiSearch [")
		.append("dataLength=").append(dataLength).append(", ")
		.append("resultSum=").append(resultSum).append(", ")
		.append("resultSize=").append(resultSize) 
		.append("]");
		int len = pois.size();
		for (int i = 0; i < len; i++) {
			sb.append("\n\tPoi(").append(i).append(") [")
			.append("poiId=").append(pois.get(i).getPoiId()).append(", ")
			.append("name=").append(pois.get(i).getName()).append(", ")
			.append("address=").append(pois.get(i).getAddress()).append(", ")
			.append("cityCode=").append(pois.get(i).getCityCode()).append(", ")
			.append("coord20X=").append(pois.get(i).getCoord20X()).append(", ")
			.append("coord20Y=").append(pois.get(i).getCoord20Y()).append(", ")
			.append("phoneNumber=").append(pois.get(i).getPhoneNumber()).append(", ")
			.append("dataLength=").append(pois.get(i).getDataLength())
			.append("]");
		}
		return sb.toString();
	}
	
}