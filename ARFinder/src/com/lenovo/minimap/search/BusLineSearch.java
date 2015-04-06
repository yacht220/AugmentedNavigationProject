/*
 * 文件名：	BusLineSearch.java
 * 日期：	2010-1-13
 * 修改历史：
 * [时间]		[修改者]			[修改内容]
 */
package com.lenovo.minimap.search;

import java.util.ArrayList;
import java.util.List;

import com.lenovo.minimap.MinimapService;
import com.lenovo.minimap.HttpClientUtil;
import com.lenovo.minimap.dto.BusViaPlatform;
import com.lenovo.minimap.dto.Coord20;

/**
 * 版权所有(c)联想集团有限公司 1998-2010 保留所有权利。<br />
 * 项目：	<br />
 * 描述：	公交线路查询结果的解析操作<br />
 * @author	zhangguojun<br />
 * @version	1.0
 * @since	JDK1.6, HttpClient4.0
 */
public class BusLineSearch {
	/** 数据包总长度 */
	private int dataLength;
	/** 总结果数 */
	private int resultSum;
	/** 此次返回的结果条数 */
	private int resultLength;
	/** 处理结果的List数组 */
	private List<BusLine> busLines = null;
	
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
	 * 返回此次返回的结果条数
	 * @return 此次返回的结果条数
	 */
	public int getResultLength() {
		return resultLength;
	}
	/**
	 * 返回处理结果的List数组
	 * @return 处理结果的List数组
	 */
	public List<BusLine> getBusLines() {
		return busLines;
	}
	
	/**
	 * 
	 * 版权所有(c)联想集团有限公司 1998-2010 保留所有权利.	<br />
	 * 项目：	<br />
	 * 描述：	具体的公交线路查询得到的详细数据<br />
	 * @author	zhangguojun<br />
	 * @version	1.0
	 * @since	JDK1.6, HttpClient4.0
	 */
	public class BusLine {
		/** 该条数据总长度 */
		private int dataLength;
		/** 线路名称 */
		private String lineName;
		/** 起点名称 */
		private String startName;
		/** 终点名称 */
		private String endName;
		/** 首车时间，格式：HH:mm */
		private String startTime;
		/** 末车时间，格式：HH:mm */
		private String endTime;
		/** 行驶长度，单位：米 */
		private int steerLength;
		/** 坐标点个数 */
		private int coord20Size;
		/** 途经站数 */
		private int viaPlatformSize;
		/** 坐标点List数组 */
		private List<Coord20> coord20s = null;
		/** 途经站数List数组 */
		private List<BusViaPlatform> busViaPlatforms = null;
		
		/**
		 * 返回该条数据总长度
		 * @return 该条数据总长度
		 */
		public int getDataLength() {
			return dataLength;
		}
		/**
		 * 返回线路名称
		 * @return 线路名称
		 */
		public String getLineName() {
			return lineName;
		}
		/**
		 * 返回起点名称
		 * @return 起点名称
		 */
		public String getStartName() {
			return startName;
		}
		/**
		 * 返回终点名称
		 * @return 终点名称
		 */
		public String getEndName() {
			return endName;
		}
		/**
		 * 返回首车时间，格式：HH:mm
		 * @return 首车时间，格式：HH:mm
		 */
		public String getStartTime() {
			return startTime;
		}
		/**
		 * 返回末车时间，格式：HH:mm
		 * @return 末车时间，格式：HH:mm
		 */
		public String getEndTime() {
			return endTime;
		}
		/**
		 * 返回行驶长度，单位：米
		 * @return 行驶长度，单位：米
		 */
		public int getSteerLength() {
			return steerLength;
		}
		/**
		 * 返回途经站数
		 * @return 途经站数
		 */
		public int getViaPlatformSize() {
			return viaPlatformSize;
		}
		/**
		 * 返回坐标点个数
		 * @return 坐标点个数
		 */
		public int getCoord20Size() {
			return coord20Size;
		}
		/**
		 * 返回途经站数List数组
		 * @return 途经站数List数组
		 */
		public List<BusViaPlatform> getBusViaPlatforms() {
			return busViaPlatforms;
		}
		/**
		 * 返回坐标点List数组
		 * @return 坐标点List数组
		 */
		public List<Coord20> getCoord20s() {
			return coord20s;
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
		resultSum = HttpClientUtil.getInt(h2);
		index += h2.length;
		
		byte[] h3 = new byte[4];
		System.arraycopy(bytes, index, h3, 0, h3.length);
		resultLength = HttpClientUtil.getInt(h3);
		index += h3.length;
		
		busLines = new ArrayList<BusLine>();
		for (int i = 0; i < getResultLength(); i++) {
			BusLine busLine =  new BusLine();
			byte[] n1 = new byte[4];
			System.arraycopy(bytes, index, n1, 0, n1.length);
			busLine.dataLength = HttpClientUtil.getInt(n1);
			index += n1.length;
			
			byte[] n2 = new byte[2];
			System.arraycopy(bytes, index, n2, 0, n2.length);
			byte[] n3 = new byte[HttpClientUtil.getShort(n2)];
			System.arraycopy(bytes, index + n2.length, n3, 0, n3.length);
			busLine.lineName = HttpClientUtil.getString(n3, MinimapService.CHARSET);
			index += n2.length + n3.length;
			
			byte[] n4 = new byte[2];
			System.arraycopy(bytes, index, n4, 0, n4.length);
			byte[] n5 = new byte[HttpClientUtil.getShort(n4)];
			System.arraycopy(bytes, index + n4.length, n5, 0, n5.length);
			busLine.startName = HttpClientUtil.getString(n5, MinimapService.CHARSET);
			index += n4.length + n5.length;
			
			byte[] n6 = new byte[2];
			System.arraycopy(bytes, index, n6, 0, n6.length);
			byte[] n7 = new byte[HttpClientUtil.getShort(n6)];
			System.arraycopy(bytes, index + n6.length, n7, 0, n7.length);
			busLine.endName = HttpClientUtil.getString(n7, MinimapService.CHARSET);
			index += n6.length + n7.length;
			
			byte[] n8 = new byte[2];
			System.arraycopy(bytes, index, n8, 0, n8.length);
			busLine.startTime = String.valueOf(HttpClientUtil.getShort(n8)).length() == 3 ? ((HttpClientUtil.getShort(n8) < 10 ?  "": "0") + String.valueOf(HttpClientUtil.getShort(n8)).substring(0, 1) + ":" + String.valueOf(HttpClientUtil.getShort(n8)).substring(1)) : (String.valueOf(HttpClientUtil.getShort(n8)).length() == 4 ? (String.valueOf(HttpClientUtil.getShort(n8)).substring(0, 2) + ":" + String.valueOf(HttpClientUtil.getShort(n8)).substring(2)) : (String.valueOf(HttpClientUtil.getShort(n8))));
			index += n8.length;
			
			byte[] n9 = new byte[2];
			System.arraycopy(bytes, index, n9, 0, n9.length);
			busLine.endTime = String.valueOf(HttpClientUtil.getShort(n9)).length() == 3 ? (String.valueOf(HttpClientUtil.getShort(n9)).substring(0, 1) + ":" + String.valueOf(HttpClientUtil.getShort(n9)).substring(1)) : (String.valueOf(HttpClientUtil.getShort(n9)).length() == 4 ? (String.valueOf(HttpClientUtil.getShort(n9)).substring(0, 2) + ":" + String.valueOf(HttpClientUtil.getShort(n9)).substring(2)) : (String.valueOf(HttpClientUtil.getShort(n9))));
			index += n9.length;
			
			byte[] n10 = new byte[4];
			System.arraycopy(bytes, index, n10, 0, n10.length);
			busLine.steerLength = HttpClientUtil.getInt(n10);
			index += n10.length;
			
			byte[] n11 = new byte[4];
			System.arraycopy(bytes, index, n11, 0, n11.length);
			busLine.coord20Size = HttpClientUtil.getInt(n11);
			index += n11.length;
			
			busLine.coord20s = new ArrayList<Coord20>();
			for (int k = 0; k < busLine.getCoord20Size(); k++) {
				byte[] c1 = new byte[4];
				System.arraycopy(bytes, index, c1, 0, c1.length);
				index += c1.length;
				
				byte[] c2 = new byte[4];
				System.arraycopy(bytes, index, c2, 0, c2.length);
				index += c2.length;
				busLine.coord20s.add(new Coord20(HttpClientUtil.getInt(c1), HttpClientUtil.getInt(c2)));
			}
			
			byte[] n12 = new byte[4];
			System.arraycopy(bytes, index, n12, 0, n12.length);
			busLine.viaPlatformSize = HttpClientUtil.getInt(n12);
			index += n12.length;
			
			busLine.busViaPlatforms = new ArrayList<BusViaPlatform>();
			for (int k = 0; k < busLine.getViaPlatformSize(); k++) {
				byte[] v1 = new byte[2];
				System.arraycopy(bytes, index, v1, 0, v1.length);
				byte[] v2 = new byte[HttpClientUtil.getShort(v1)];
				System.arraycopy(bytes, index + v1.length, v2, 0, v2.length);
				index += v1.length + v2.length;
				
				byte[] v3 = new byte[4];
				System.arraycopy(bytes, index, v3, 0, v3.length);
				index += v3.length;
				
				byte[] v4 = new byte[4];
				System.arraycopy(bytes, index, v4, 0, v4.length);
				index += v4.length;
				busLine.busViaPlatforms.add(new BusViaPlatform(HttpClientUtil.getString(v2, MinimapService.CHARSET), HttpClientUtil.getInt(v3), HttpClientUtil.getInt(v4)));
			}
			busLines.add(busLine);
		}
	}
	
	/**
	 * 便于打印该对象的各个详细的数据部分的字符串表示形式
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("BusLineSearch [")
		.append("dataLength=").append(dataLength).append(", ")
		.append("resultSum=").append(resultSum).append(", ")
		.append("resultLength=").append(resultLength)
		.append("]");
		int len = busLines.size();
		for (int i = 0; i < len; i++) {
			sb.append("\n\tBusLine(").append(i).append(") [")
			.append("dataLength=").append(busLines.get(i).getDataLength()).append(", ")
			.append("lineName=").append(busLines.get(i).getLineName()).append(", ")
			.append("startName=").append(busLines.get(i).getStartName()).append(", ")
			.append("endName=").append(busLines.get(i).getEndName()).append(", ")
			.append("startTime=").append(busLines.get(i).getStartTime()).append(", ")
			.append("endTime=").append(busLines.get(i).getEndTime()).append(", ")
			.append("steerLength=").append(busLines.get(i).getSteerLength()).append(", ")
			.append("coord20Size=").append(busLines.get(i).getCoord20Size()).append(", ")
			.append("viaPlatformSize=").append(busLines.get(i).getViaPlatformSize())
			.append("]");
			int len2 = 2;//busLines.get(i).getCoord20s().size()
			for (int j = 0; j < len2; j++) {
				sb.append("\n\t\tCoord20(").append(j).append(") [")
				.append("x=").append(busLines.get(i).getCoord20s().get(j).getX()).append(", ")
				.append("y=").append(busLines.get(i).getCoord20s().get(j).getY())
				.append("]");
			}
			sb.append("\n\t\t").append((busLines.get(i).getCoord20s().size() > len2 ? "......" : ""));
			int len3 = busLines.get(i).getBusViaPlatforms().size();//busLines.get(i).getBusViaPlatforms().size()
			for (int k = 0; k < len3; k++) {
				sb.append("\n\t\tBusViaPlatform(").append(k).append(") [")
				.append("name=").append(busLines.get(i).getBusViaPlatforms().get(k).getName()).append(", ")
				.append("coord20X=").append(busLines.get(i).getBusViaPlatforms().get(k).getCoord20X()).append(", ")
				.append("coord20Y=").append(busLines.get(i).getBusViaPlatforms().get(k).getCoord20Y())
				.append("]");
			}
		}
		return sb.toString();
	}
	
}