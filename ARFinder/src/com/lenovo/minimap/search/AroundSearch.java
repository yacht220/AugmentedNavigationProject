/*
 * 文件名：	AroundSearch.java
 * 日期：	2010-1-11
 * 修改历史：
 * [时间]		[修改者]			[修改内容]
 */
package com.lenovo.minimap.search;

import java.util.ArrayList;
import java.util.List;

import com.lenovo.arutil.ARUtil;
import com.lenovo.minimap.MinimapService;
import com.lenovo.minimap.HttpClientUtil;
import com.lenovo.minimap.dto.Coord20;
import com.lenovo.minimap.dto.CoordDeflect;

/**
 * 版权所有(c)联想集团有限公司 1998-2010 保留所有权利。<br />
 * 项目：	<br />
 * 描述：	周边查询结果的解析操作<br />
 * @author	zhangguojun<br />
 * @version	1.0
 * @since	JDK1.6, HttpClient4.0
 */
public class AroundSearch {
	/** 数据包总长度 */
	private int dataLength;
	/** 总结果数 */
	private int resultSum;
	/** 此次返回数据条数 */
	private int resultSize;
	/** 处理结果的List数组 */
	private List<Around> arounds = null;
	
	ARUtil arUtil = null;
	MinimapService ms = null;
	
	public AroundSearch(ARUtil arUtil, MinimapService ms){
		this.arUtil = arUtil;
		this.ms = ms;
	}
	
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
	 * @return Returns the arounds.
	 */
	public List<Around> getArounds() {
		return arounds;
	}
	
	/**
	 * 
	 * 版权所有(c)联想集团有限公司 1998-2010 保留所有权利.	<br />
	 * 项目：	<br />
	 * 描述：	具体的周边查询得到的详细数据<br />
	 * @author	zhangguojun<br />
	 * @version	1.0
	 * @since	JDK1.6, HttpClient4.0
	 */
	public class Around {
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
		/** 偏转后的x坐标 */
		private double coordDeflectX;
		/** 偏转后的y坐标 */
		private double coordDeflectY;
		/** 离中心点的距离 */
		private int staticDistance;
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
		 * 返回偏转后的x坐标
		 * @return 偏转后的x坐标
		 */
		public double getCoordDeflectX() {
			return coordDeflectX;
		}
		/**
		 * 返回偏转后的y坐标
		 * @return 偏转后的y坐标
		 */
		public double getCoordDeflectY() {
			return coordDeflectY;
		}
		/**
		 * 返回屏幕坐标位置x坐标
		 * @return 屏幕坐标位置x坐标
		 */
		public float getScreenX() {
			return arUtil == null ? -1 : arUtil.getScreenXY(this.coordDeflectX,  this.coordDeflectY)[0];
		}
		/**
		 * 返回屏幕坐标位置y坐标，远中近区的信息点随手机俯仰具有不同的摆幅效果
		 * @return 屏幕坐标位置y坐标
		 */
		public float getScreenY() {
			if(arUtil == null) {return -1;}
			float screenY = arUtil.getScreenXY(this.coordDeflectX,  this.coordDeflectY)[1];
			
			/*
			 * 根据AR UI设计中信息点显示的要求，远中近区的信息点在屏幕上的纵向排列
			 * 以及随手机俯仰的摆幅是不同的。
			 */
			if(Math.round(getRealTimeDistance())>=0 
					&& Math.round(getRealTimeDistance()) < 200){
		        /* 采用原始的Y坐标数据 */
				return screenY;
			}else if(Math.round(getRealTimeDistance())>=200 
					&& Math.round(getRealTimeDistance()) < 1000){
				/* 手机俯仰角度越大，Y坐标的摆幅就越大，最后一个因子是摆幅系数 */
				return screenY-arUtil.getInclination()*3;
			}else {
				/* 手机俯仰角度越大，Y坐标的摆幅就越大，最后一个因子是摆幅系数 */
				return screenY-arUtil.getInclination()*5;
			}				
		}
		/**
		 * 返回离中心点的距离（实时距离）
		 * @return 离中心点的距离（实时距离）
		 */
		public double getRealTimeDistance(){
			return ms.getDistance(new CoordDeflect(this.coordDeflectX, this.coordDeflectY));
		}
		/**
		 * 返回离中心点的距离（高德距离，非实时）
		 * @return 离中心点的距离（高德距离，非实时）
		 */
		public int getStaticDistance() {
			return staticDistance;
		}
		/**
		 * 返回城市代码
		 * @return 城市代码
		 */
		public String getCityCode() {
			return cityCode;
		}
		/**
		 * 返回所需时间，单位：毫秒，当结果为-1时，未知
		 * @return 所需时间，单位：毫秒，当结果为-1时，未知
		 */
		public int getNeedTime() {
			return -1;
		}
	}
	
	/**
	 * 处理字节数组
	 * @param bytes 需要处理的字节数组
	 * @param charset 字符串编码
	 */
	public void process(byte[] bytes, String charset) {
		int index = 0; // 初始化索引
		byte[] h1 = new byte[4]; // 建立新的字节数组
		System.arraycopy(bytes, index, h1, 0, h1.length); // 根据Minimap Service V1.doc的定义，将原始字节数组的某一部分拷贝至新的字节数组中，该部分数据为需要的值
		this.dataLength = HttpClientUtil.getInt(h1); // 将新的字节数组内容转换成需要的数据类型
		index += h1.length; // 索引跳至下一个位置
		
		byte[] h2 = new byte[4];
		System.arraycopy(bytes, index, h2, 0, h2.length);
		this.resultSum = HttpClientUtil.getByte(h2);
		index += h2.length;
		
		byte[] h3 = new byte[4];
		System.arraycopy(bytes, index, h3, 0, h3.length);
		this.resultSize = HttpClientUtil.getInt(h3);
		index += h3.length;
		
		arounds = new ArrayList<Around>();
		for (int i = 0; i < getResultSize(); i++) {
			Around around = new Around();
			byte[] d1 = new byte[4];
			System.arraycopy(bytes, index, d1, 0, d1.length);
			around.dataLength = HttpClientUtil.getInt(d1);
			index += d1.length;
			
			byte[] d2 = new byte[2];
			System.arraycopy(bytes, index, d2, 0, d2.length);
			byte[] d3 = new byte[HttpClientUtil.getShort(d2)];
			System.arraycopy(bytes, index + d2.length, d3, 0, d3.length);
			around.poiId = new String(d3);
			index += d2.length + d3.length;
			
			byte[] d4 = new byte[2];
			System.arraycopy(bytes, index, d4, 0, d4.length);
			byte[] d5 = new byte[HttpClientUtil.getShort(d4)];
			System.arraycopy(bytes, index + d4.length, d5, 0, d5.length);
			around.name = HttpClientUtil.getString(d5, charset);
			index += d4.length + d5.length;

			byte[] d6 = new byte[2];
			System.arraycopy(bytes, index, d6, 0, d6.length);
			byte[] d7 = new byte[HttpClientUtil.getShort(d6)];
			System.arraycopy(bytes, index + d6.length, d7, 0, d7.length);
			around.address = HttpClientUtil.getString(d7, charset);
			index += d6.length + d7.length;
			
			byte[] d8 = new byte[2];
			System.arraycopy(bytes, index, d8, 0, d8.length);
			byte[] d9 = new byte[HttpClientUtil.getShort(d8)];
			System.arraycopy(bytes, index + d8.length, d9, 0, d9.length);
			around.phoneNumber = HttpClientUtil.getString(d9, charset);
			index += d8.length + d9.length;
			
			byte[] d10 = new byte[4];
			System.arraycopy(bytes, index, d10, 0, d10.length);
			around.coord20X = HttpClientUtil.getInt(d10);
			index += d10.length;
			
			byte[] d11 = new byte[4];
			System.arraycopy(bytes, index, d11, 0, d11.length);
			around.coord20Y = HttpClientUtil.getInt(d11);
			index += d11.length;
			
			CoordDeflect coordDeflect = ms.coord202CoordDeflect(new Coord20(around.getCoord20X(), around.getCoord20Y()), true);
			around.coordDeflectX = coordDeflect.getX();
			around.coordDeflectY = coordDeflect.getY();
			
			byte[] d12 = new byte[4];
			System.arraycopy(bytes, index, d12, 0, d12.length);
			around.staticDistance = HttpClientUtil.getInt(d12);
			index += d12.length;
			
			byte[] d13 = new byte[2];
			System.arraycopy(bytes, index, d13, 0, d13.length);
			byte[] d14 = new byte[HttpClientUtil.getShort(d13)];
			System.arraycopy(bytes, index + d13.length, d14, 0, d14.length);
			around.cityCode = HttpClientUtil.getString(d14, charset);
			index += d13.length +  d14.length;
			arounds.add(around);
		}
	}
	
	/**
	 * 便于打印该对象的各个详细的数据部分的字符串表示形式
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("AroundSearch [")
		.append("dataLength=").append(dataLength).append(", ")
		.append("resultSum=").append(resultSum).append(", ")
		.append("resultSize=").append(resultSize)
		.append("]");
		int len = arounds.size();
		for (int i = 0; i < len; i++) {
			sb.append("\n\tAround(").append(i).append(") [")
			.append("poiId=").append(arounds.get(i).getPoiId()).append(", ")
			.append("name=").append(arounds.get(i).getName()).append(", ")
			.append("address=").append(arounds.get(i).getAddress()).append(", ")
			.append("cityCode=").append(arounds.get(i).getCityCode()).append(", ")
			.append("coord20X=").append(arounds.get(i).getCoord20X()).append(", ")
			.append("coord20Y=").append(arounds.get(i).getCoord20Y()).append(", ")
			.append("coordDeflectX=").append(arounds.get(i).getCoordDeflectX()).append(", ")
			.append("coordDeflectY=").append(arounds.get(i).getCoordDeflectY()).append(", ")
			.append("screenX=").append(arounds.get(i).getScreenX()).append(", ")
			.append("screenY=").append(arounds.get(i).getScreenY()).append(", ")
			.append("realTimeDistance=").append(arounds.get(i).getRealTimeDistance()).append(", ")
			.append("staticDistance=").append(arounds.get(i).getStaticDistance()).append(", ")
			.append("needTime=").append(arounds.get(i).getNeedTime()).append(", ")
			.append("phoneNumber=").append(arounds.get(i).getPhoneNumber()).append(", ")
			.append("dataLength=").append(arounds.get(i).getDataLength())
			.append("]");
		}
		return sb.toString();
	}
	
	/**
	 * 返回周边查询搜索类别对应的具体名称
	 * @param type 周边查询搜索类别定义
	 * @return 周边查询搜索类别对应的具体名称
	 */
	public static String type2Keyword(int type) {
		Integer TYPES[] = {1, 2, 3, 4, 5};
		String NAMES[] = {"", "餐厅", "购物", "公交", "加油站"};
		type = (type > 5 || type < 1) ? 1 : type;
		for (int i = 0; i < TYPES.length; i++) {
			if(type == TYPES[i]) {
				return NAMES[i];
			}
		}
		return NAMES[0];
	}
}