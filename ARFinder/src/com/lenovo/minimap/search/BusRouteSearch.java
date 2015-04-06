/*
 * 文件名：	BusRouteSearch.java
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
import com.lenovo.minimap.search.BusRouteSearch.BusRoute.Navigation;

/**
 * 版权所有(c)联想集团有限公司 1998-2010 保留所有权利。<br />
 * 项目：	<br />
 * 描述：	公交换乘查询结果的解析操作<br />
 * @author	zhangguojun<br />
 * @version	1.0
 * @since	JDK1.6, HttpClient4.0
 */
public class BusRouteSearch {
	/** 数据包总长度 */
	private int dataLength;
	/** 总结果数 */
	private int resultSum;
	/** 处理结果的List数组 */
	private List<BusRoute> busRoutes = null;
	
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
	 * 返回处理结果的List数组
	 * @return 处理结果的List数组
	 */
	public List<BusRoute> getBusRoutes() {
		return busRoutes;
	}
	
	/**
	 * 
	 * 版权所有(c)联想集团有限公司 1998-2010 保留所有权利.	<br />
	 * 项目：	<br />
	 * 描述：	具体的公交换乘查询得到的详细数据<br />
	 * @author	zhangguojun<br />
	 * @version	1.0
	 * @since	JDK1.6, HttpClient4.0
	 */
	public class BusRoute {
		/** 该条数据总长度 */
		private int dataLength;
		/** 导航段个数（换乘次数） */
		private int navigationSize;
		/** 起点步行长度，单位：米 */
		private int startWalkLength;
		/** 终点步行长度，单位：米 */
		private int endWalkLength;
		/** 行驶长度，单位：米 */
		private int steerLength;
		/** 导航段List数组 */
		private List<Navigation> navigations = null;
		
		/**
		 * 返回该条数据总长度
		 * @return 该条数据总长度
		 */
		public int getDataLength() {
			return dataLength;
		}
		/**
		 * 返回导航段个数（换乘次数）
		 * @return 导航段个数（换乘次数）
		 */
		public int getNavigationSize() {
			return navigationSize;
		}
		/**
		 * 返回起点步行长度，单位：米
		 * @return 起点步行长度，单位：米
		 */
		public int getStartWalkLength() {
			return startWalkLength;
		}
		/**
		 * 返回终点步行长度，单位：米
		 * @return 终点步行长度，单位：米
		 */
		public int getEndWalkLength() {
			return endWalkLength;
		}
		/**
		 * 返回行驶长度，单位：米
		 * @return 行驶长度，单位：米
		 */
		public int getSteerLength() {
			return steerLength;
		}
		/**
		 * 返回导航段List数组
		 * @return 导航段List数组
		 */
		public List<Navigation> getNavigations() {
			return navigations;
		}

		/**
		 * 
		 * 版权所有(c)联想集团有限公司 1998-2010 保留所有权利.	<br />
		 * 项目：	<br />
		 * 描述：	导航段具体数据<br />
		 * @author	zhangguojun<br />
		 * @version	1.0
		 * @since	JDK1.6, HttpClient4.0
		 */
		public class Navigation {
			/** 该条数据总长度 */
			private int dataLength;
			/** 线路名称 */
			private String lineName;
			/** 起点名称 */
			private String startName;
			/** 终点名称 */
			private String endName;
			/** 行驶长度，单位：米 */
			private int steerLength;
			/** 途经站数 */
			private int busPlatformSize;
			/** 坐标点个数 */
			private int coord20Size;
			/** 途经站数List数组 */
			private List<BusViaPlatform> busViaPlatforms = null;
			/** 坐标点List数组 */
			private List<Coord20> coord20s = null;
			
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
			public int getBusPlatformSize() {
				return busPlatformSize;
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
		
		busRoutes = new ArrayList<BusRoute>();
		for (int i = 0; i < getResultSum(); i++) {
			BusRoute busRoute = new BusRoute();
			byte[] d1 = new byte[4];
			System.arraycopy(bytes, index, d1, 0, d1.length);
			busRoute.dataLength = HttpClientUtil.getInt(d1);
			index += d1.length;
			
			byte[] d2 = new byte[4];
			System.arraycopy(bytes, index, d2, 0, d2.length);
			busRoute.navigationSize = HttpClientUtil.getInt(d2);
			index += d2.length;
			
			byte[] d3 = new byte[4];
			System.arraycopy(bytes, index, d3, 0, d3.length);
			busRoute.steerLength = HttpClientUtil.getInt(d3);
			index += d3.length;
			
			byte[] d4 = new byte[4];
			System.arraycopy(bytes, index, d4, 0, d4.length);
			busRoute.startWalkLength = HttpClientUtil.getInt(d4);
			index += d4.length;
			
			byte[] d5 = new byte[4];
			System.arraycopy(bytes, index, d5, 0, d5.length);
			busRoute.endWalkLength = HttpClientUtil.getInt(d5);
			index += d5.length;
			
			busRoute.navigations = new ArrayList<Navigation>();
			for (int j = 0; j < busRoute.getNavigationSize(); j++) {
				Navigation navigation =  new BusRoute().new Navigation();
				byte[] n1 = new byte[4];
				System.arraycopy(bytes, index, n1, 0, n1.length);
				navigation.dataLength = HttpClientUtil.getInt(n1);
				index += n1.length;
				
				byte[] n2 = new byte[2];
				System.arraycopy(bytes, index, n2, 0, n2.length);
				byte[] n3 = new byte[HttpClientUtil.getShort(n2)];
				System.arraycopy(bytes, index + n2.length, n3, 0, n3.length);
				navigation.lineName = HttpClientUtil.getString(n3, MinimapService.CHARSET);
				index += n2.length + n3.length;
				
				byte[] n4 = new byte[2];
				System.arraycopy(bytes, index, n4, 0, n4.length);
				byte[] n5 = new byte[HttpClientUtil.getShort(n4)];
				System.arraycopy(bytes, index + n4.length, n5, 0, n5.length);
				navigation.startName = HttpClientUtil.getString(n5, MinimapService.CHARSET);
				index += n4.length + n5.length;
				
				byte[] n6 = new byte[2];
				System.arraycopy(bytes, index, n6, 0, n6.length);
				byte[] n7 = new byte[HttpClientUtil.getShort(n6)];
				System.arraycopy(bytes, index + n6.length, n7, 0, n7.length);
				navigation.endName = HttpClientUtil.getString(n7, MinimapService.CHARSET);
				index += n6.length + n7.length;
				
				byte[] n8 = new byte[4];
				System.arraycopy(bytes, index, n8, 0, n8.length);
				navigation.steerLength = HttpClientUtil.getInt(n8);
				index += n8.length;
				
				byte[] n9 = new byte[4];
				System.arraycopy(bytes, index, n9, 0, n9.length);
				navigation.busPlatformSize = HttpClientUtil.getInt(n9);
				index += n9.length;
				
				navigation.busViaPlatforms = new ArrayList<BusViaPlatform>();
				for (int k = 0; k < navigation.getBusPlatformSize(); k++) {
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
					navigation.busViaPlatforms.add(new BusViaPlatform(HttpClientUtil.getString(v2, MinimapService.CHARSET), HttpClientUtil.getInt(v3), HttpClientUtil.getInt(v4)));
				}
				
				byte[] n10 = new byte[4];
				System.arraycopy(bytes, index, n10, 0, n10.length);
				navigation.coord20Size = HttpClientUtil.getInt(n10);
				index += n10.length;
				
				navigation.coord20s = new ArrayList<Coord20>();
				for (int k = 0; k < navigation.getCoord20Size(); k++) {
					byte[] c1 = new byte[4];
					System.arraycopy(bytes, index, c1, 0, c1.length);
					index += c1.length;
					
					byte[] c2 = new byte[4];
					System.arraycopy(bytes, index, c2, 0, c2.length);
					index += c2.length;
					navigation.coord20s.add(new Coord20(HttpClientUtil.getInt(c1), HttpClientUtil.getInt(c2)));
				}
				busRoute.navigations.add(navigation);
			}
			busRoutes.add(busRoute);
		}
	}
	
	/**
	 * 便于打印该对象的各个详细的数据部分的字符串表示形式
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("BusRouteSearch [")
		.append("dataLength=").append(dataLength).append(", ")
		.append("resultSum=").append(resultSum)
		.append("]");
		int len = busRoutes.size();
		for (int i = 0; i < len; i++) {
			sb.append("\n\tBusRoute(").append(i).append(") [")
			.append("dataLength=").append(busRoutes.get(i).getDataLength()).append(", ")
			.append("navigationSize=").append(busRoutes.get(i).getNavigationSize()).append(", ")
			.append("startWalkLength=").append(busRoutes.get(i).getStartWalkLength()).append(", ")
			.append("endWalkLength=").append(busRoutes.get(i).getEndWalkLength()).append(", ")
			.append("steerLength=").append(busRoutes.get(i).getSteerLength())
			.append("]");
			int len2 = busRoutes.get(i).getNavigations().size();
			for (int j = 0; j < len2; j++) {
				sb.append("\n\t\tNavigation(").append(j).append(") [")
				.append("dataLength=").append(busRoutes.get(i).getNavigations().get(j).getDataLength()).append(", ")
				.append("lineName=").append(busRoutes.get(i).getNavigations().get(j).getLineName()).append(", ")
				.append("startName=").append(busRoutes.get(i).getNavigations().get(j).getStartName()).append(", ")
				.append("endName=").append(busRoutes.get(i).getNavigations().get(j).getEndName()).append(", ")
				.append("steerLength=").append(busRoutes.get(i).getNavigations().get(j).getSteerLength()).append(", ")
				.append("busPlatformSize=").append(busRoutes.get(i).getNavigations().get(j).getBusPlatformSize()).append(", ")
				.append("coord20Size=").append(busRoutes.get(i).getNavigations().get(j).getCoord20Size())
				.append("]");
				int len3 = busRoutes.get(i).getNavigations().get(j).getBusViaPlatforms().size();
				for (int k = 0; k < len3; k++) {
					sb.append("\n\t\t\tBusViaPlatform(").append(k).append(") [")
					.append("name=").append(busRoutes.get(i).getNavigations().get(j).getBusViaPlatforms().get(k).getName()).append(", ")
					.append("coord20X=").append(busRoutes.get(i).getNavigations().get(j).getBusViaPlatforms().get(k).getCoord20X()).append(", ")
					.append("coord20Y=").append(busRoutes.get(i).getNavigations().get(j).getBusViaPlatforms().get(k).getCoord20Y())
					.append("]");
				}
				int len4 = 2;//busRoutes.get(i).getNavigations().get(j).getCoord20s().size();
				for (int l = 0; l < len4; l++) {//
					sb.append("\n\t\t\tCoord20(").append(l).append(") [")
					.append("x=").append(busRoutes.get(i).getNavigations().get(j).getCoord20s().get(l).getX()).append(", ")
					.append("y=").append(busRoutes.get(i).getNavigations().get(j).getCoord20s().get(l).getY())
					.append("]");
				}
				sb.append("\n\t\t").append((busRoutes.get(i).getNavigations().get(j).getCoord20s().size() > len4 ? "......" : ""));
			}
		}
		return sb.toString();
	}
	
}
