/*
 * 文件名：	RouteSearch.java
 * 日期：	2010-1-11
 * 修改历史：
 * [时间]		[修改者]			[修改内容]
 */
package com.lenovo.minimap.search;

import java.util.ArrayList;
import java.util.List;

import com.lenovo.minimap.MinimapService;
import com.lenovo.minimap.HttpClientUtil;
import com.lenovo.minimap.dto.Coord20;
import com.lenovo.minimap.dto.CoordDeflect;
import com.lenovo.minimap.search.RouteSearch.Route.Navigation;

/**
 * 版权所有(c)联想集团有限公司 1998-2010 保留所有权利。<br />
 * 项目：	<br />
 * 描述：	路线查询结果的解析操作<br />
 * @author	zhangguojun<br />
 * @version	1.0
 * @since	JDK1.6, HttpClient4.0
 */
public class RouteSearch {
	/** 数据包总长度 */
	private int dataLength;
	/** 总结果数 */
	private int resultSum;
	/** 处理结果的List数组 */
	private List<Route> routes = null;
	/** MinimapService对象 */
	private MinimapService ms;
	
	public RouteSearch(MinimapService ms) {
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
	 * 返回处理结果的List数组
	 * @return 处理结果的List数组
	 */
	public List<Route> getRoutes() {
		return routes;
	}
	
	/**
	 * 
	 * 版权所有(c)联想集团有限公司 1998-2010 保留所有权利.	<br />
	 * 项目：	<br />
	 * 描述：	具体的路线查询得到的详细数据<br />
	 * @author	zhangguojun<br />
	 * @version	1.0
	 * @since	JDK1.6, HttpClient4.0
	 */
	public class Route {
		/** 该条数据总长度 */
		private int dataLength;
		/** 该条路径总长度，单位：米 */
		private int routeLength;
		/** 导航段个数 */
		private int navigationSize;
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
		 * 返回该条路径总长度，单位：米
		 * @return 该条路径总长度，单位：米
		 */
		public int getRouteLength() {
			return routeLength;
		}
		/**
		 * 返回导航段个数
		 * @return 导航段个数
		 */
		public int getNavigationSize() {
			return navigationSize;
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
			/** 道路名称 */
			private String routeName;
			/** 导航段长度， 单位：米 */
			private int navigationLength;
			/** 导航动作 */
			private byte navigationAction;
			/** 导航动作文字描述 */
			private String navigationActionText;
			/** 20级坐标点个数 */
			private int coord20Size;
			/** 偏转坐标点个数 */
			private int coordDeflectSize;
			/** 20级坐标点List数组 */
			private List<Coord20> coord20s = null;
			/** 偏转坐标点List数组 */
			private List<CoordDeflect> coordDeflects = null;
			
			/**
			 * 返回该条数据总长度
			 * @return 该条数据总长度
			 */
			public int getDataLength() {
				return dataLength;
			}
			/**
			 * 返回道路名称
			 * @return 道路名称
			 */
			public String getRouteName() {
				return routeName;
			}
			/**
			 * 返回导航段长度， 单位：米
			 * @return 导航段长度， 单位：米
			 */
			public int getNavigationLength() {
				return navigationLength;
			}
			/**
			 * 返回导航动作
			 * @return 导航动作
			 */
			public byte getNavigationAction() {
				return navigationAction;
			}
			/**
			 * 返回导航动作文字描述
			 * @return 导航动作文字描述
			 */
			public String getNavigationActionText() {
				return navigationActionText;
			}
			/**
			 * 返回20级像素坐标点个数
			 * @return 20级像素坐标点个数
			 */
			public int getCoord20Size() {
				return coord20Size;
			}
			/**
			 * 返回偏转坐标点个数
			 * @return 偏转坐标点个数
			 */
			public int getCoordDeflectSize() {
				return coordDeflectSize;
			}
			
			/**
			 * 返回20级像素 坐标点List数组
			 * @return 20级像素坐标点List数组
			 */
			public List<Coord20> getCoord20s() {
				return coord20s;
			}
			/**
			 * 返回 偏转后的坐标点List数组
			 * @return  偏转后的坐标点List数组
			 */
			public List<CoordDeflect> getCoordDeflects() {
				return coordDeflects;
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
		resultSum = HttpClientUtil.getByte(h2);
		index += h2.length;
		
		routes = new ArrayList<Route>();
		for (int i = 0; i < getResultSum(); i++) {
			Route route = new Route();
			byte[] d1 = new byte[4];
			System.arraycopy(bytes, index, d1, 0, d1.length);
			route.dataLength = HttpClientUtil.getInt(d1);
			index += d1.length;
			
			byte[] d2 = new byte[4];
			System.arraycopy(bytes, index, d2, 0, d2.length);
			route.routeLength = HttpClientUtil.getInt(d2);
			index += d2.length;
			
			byte[] d3 = new byte[4];
			System.arraycopy(bytes, index, d3, 0, d3.length);
			route.navigationSize = HttpClientUtil.getInt(d3);
			index += d3.length;
			
			route.navigations = new ArrayList<Navigation>();
			for (int j = 0; j < route.getNavigationSize(); j++) {
				Navigation navigation =  new Route().new Navigation();
				byte[] n1 = new byte[4];
				System.arraycopy(bytes, index, n1, 0, n1.length);
				navigation.dataLength = HttpClientUtil.getInt(n1);
				index += n1.length;
				
				byte[] n2 = new byte[2];
				System.arraycopy(bytes, index, n2, 0, n2.length);
				byte[] n3 = new byte[HttpClientUtil.getShort(n2)];
				System.arraycopy(bytes, index + n2.length, n3, 0, n3.length);
				navigation.routeName = HttpClientUtil.getString(n3, MinimapService.CHARSET);
				index += n2.length + n3.length;
				
				byte[] n4 = new byte[4];
				System.arraycopy(bytes, index, n4, 0, n4.length);
				navigation.navigationLength = HttpClientUtil.getInt(n4);
				index += n4.length;
				
				byte[] n5 = new byte[1];
				System.arraycopy(bytes, index, n5, 0, n5.length);
				navigation.navigationAction = n5[0];
				navigation.navigationActionText = navigationAction2NavigationActionText(navigation.getNavigationAction());
				index += n5.length;
				
				byte[] n6 = new byte[4];
				System.arraycopy(bytes, index, n6, 0, n6.length);
				navigation.coordDeflectSize = navigation.coord20Size = HttpClientUtil.getInt(n6);
				index += n6.length;
				
				navigation.coord20s = new ArrayList<Coord20>();
				navigation.coordDeflects = new ArrayList<CoordDeflect>();
				for (int k = 0; k < navigation.getCoord20Size(); k++) {
					byte[] c1 = new byte[4];
					System.arraycopy(bytes, index, c1, 0, c1.length);
					index += c1.length;
					
					byte[] c2 = new byte[4];
					System.arraycopy(bytes, index, c2, 0, c2.length);
					index += c2.length;
					
					Coord20 tempCoord20 = new Coord20(HttpClientUtil.getInt(c1), HttpClientUtil.getInt(c2));
					navigation.coord20s.add(tempCoord20);
					navigation.coordDeflects.add(ms.coord202CoordDeflect(tempCoord20, true));
					tempCoord20 = null;
				}
				route.navigations.add(navigation);
			}
			routes.add(route);
		}
	}
	
	/**
	 * 便于打印该对象的各个详细的数据部分的字符串表示形式
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("RouteSearch [")
		.append("dataLength=").append(dataLength).append(", ")
		.append("resultSum=").append(resultSum)
		.append("]");
		int len = routes.size();
		for (int i = 0; i < len; i++) {
			sb.append("\n\tRoute(").append(i).append(") [")
			.append("dataLength=").append(routes.get(i).getDataLength()).append(", ")
			.append("routeLength=").append(routes.get(i).getRouteLength()).append(", ")
			.append("navigationSize=").append(routes.get(i).getNavigationSize())
			.append("]");
			int len2 = routes.get(i).getNavigations().size();
			for (int j = 0; j < len2; j++) {
				sb.append("\n\t\tNavigation(").append(j).append(") [")
				.append("dataLength=").append(routes.get(i).getNavigations().get(j).getDataLength()).append(", ")
				.append("routeName=").append(routes.get(i).getNavigations().get(j).getRouteName()).append(", ")
				.append("navigationLength=").append(routes.get(i).getNavigations().get(j).getNavigationLength()).append(", ")
				.append("navigationAction=").append(routes.get(i).getNavigations().get(j).getNavigationAction()).append(", ")
				.append("navigationActionText=").append(routes.get(i).getNavigations().get(j).getNavigationActionText()).append(", ")
				.append("coord20Size=" + routes.get(i).getNavigations().get(j).getCoord20Size())
				.append("]");
				int len3 = routes.get(i).getNavigations().get(j).getCoord20s().size();
				for (int k = 0; k < len3; k++) {
					sb.append("\n\t\t\tCoord20(").append(k ).append(") [")
					.append("x=").append(routes.get(i).getNavigations().get(j).getCoord20s().get(k).getX()).append(", ")
					.append("y=").append(routes.get(i).getNavigations().get(j).getCoord20s().get(k).getY())
					.append("]");
				}
				int len4 = routes.get(i).getNavigations().get(j).getCoordDeflects().size();
				for (int k = 0; k < len4; k++) {
					sb.append("\n\t\t\tCoordDeflect(").append(k ).append(") [")
					.append("x=").append(routes.get(i).getNavigations().get(j).getCoordDeflects().get(k).getX()).append(", ")
					.append("y=").append(routes.get(i).getNavigations().get(j).getCoordDeflects().get(k).getY())
					.append("]");
				}
			}
		}
		return sb.toString();
	}
	
	/**
	 * 返回路线查询搜索导航动作具体文字说明
	 * @param navigationAction 路线查询搜索导航动作标示
	 * @return 路线查询搜索导航动作具体文字说明
	 */
	public static String navigationAction2NavigationActionText(byte navigationAction) {
//		"",			// 0x0			
//		"左转",		// 0x1,	
//		"右转",		// 0x2,	
//		"偏左转",	// 0x3,	
//		"偏右转",	// 0x4,	
//		"左后转",	// 0x5,		
//		"右后转",	// 0x6,		
//		"左转调头",	// 0x7,		
//		"直行",		// 0x8,		
//		"靠左",		// 0x9,	
//		"靠右",		// 0x0A,		
//		"进入环岛",	// 0x0B,	
//		"减速行驶"	// 0x0C
		byte keys[] = {0x0, 0x1, 0x2, 0x3, 0x4, 0x5, 0x6, 0x7, 0x8, 0x9, 0x0A, 0x0B, 0x0C};
		String texts[] = {"", "左转", "右转", "偏左转", "偏右转", "左后转", "右后转", "左转调头", "直行", "靠左", "靠右", "进入环岛", "减速行驶"};
		navigationAction = (navigationAction > 0x0C || navigationAction < 0x0) ? 0x0 : navigationAction;
		for (int i = 0; i < keys.length; i++) {
			if(navigationAction == keys[i]) {
				return texts[i];
			}
		}
		return texts[0];
	}
}