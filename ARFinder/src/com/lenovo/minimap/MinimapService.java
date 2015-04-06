/*
 * 文件名：	Minimap.java
 * 日期：	2010-1-5
 * 修改历史：
 * [时间]		[修改者]			[修改内容]
 */
package com.lenovo.minimap;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.protocol.HTTP;

import android.content.Context;

import com.lenovo.arutil.ARUtil;
import com.lenovo.minimap.dto.Coord20;
import com.lenovo.minimap.dto.CoordDeflect;
import com.lenovo.minimap.dto.CoordGps;
import com.lenovo.minimap.dto.Header;
import com.lenovo.minimap.search.AroundSearch;
import com.lenovo.minimap.search.PoiSearch;
import com.lenovo.minimap.search.RouteSearch;

/**
 * 版权所有(c)联想集团有限公司 1998-2010 保留所有权利。<br />
 * 项目：	<br />
 * 描述：	MinimapSercice 接口方法<br />
 * @author	zhangguojun<br />
 * @version	1.0
 * @since	JDK1.6, HttpClient4.0
 */
public class MinimapService {
	/** Minimap接口网络地址  */
	private static final String URI_API = "http://60.247.103.27:8083/MinimapService/MPS";
	/** 代理上网主机地址  */
	private String proxy = "10.99.60.201:8080";
	/** 是否代理联网(便于Java SE环境下测试，Android平台下该方法不可使用) */
	private boolean isProxy = false;
	/** 接口数据传输编码 */
	public static final String CHARSET = HTTP.UTF_8;
	/** 连接超时时间，单位：毫秒  */
	private static final int TIMEOUT = 5000;
	/** 地理位置更新时间，单位：毫秒  */
	private int locationUpdateTime = 2000;
	/** 公用接口传输参数  */
	private Map<String, String> nameValuePairBase = new HashMap<String, String>();
	/** 客户端版本号  */
	private static final String CLIENT_VERSION = "31kli21";
	/** 开发商标志  */
	private static final String DEVELOPER_CODE = "vnd01245";
	/** 压缩方式： 0不压缩， 1 gzip压缩， 2 zlib压缩 ， 可空:是，默认不压缩  */
	private static final int COMPRESS = 1;
	/** 查询范围，单位：米 */
	private static final int SEARCH_RANGE = 3000;
	/** 查询返回的结果条数 */
	public static final int SEARCH_SIZE = 20;
	/** 上一次周边查询的list列表 */
	private List<List<AroundSearch.Around>> lastAroundList = new ArrayList<List<AroundSearch.Around>>(2);
	/** 上一次步行路线查询的list列表 */
	private List<List<RouteSearch.Route>> lastWalkRouteList = new ArrayList<List<RouteSearch.Route>>(2);
	/** 当前导航段索引 */
	private int navigationCurIndex;
	/** 当前导航路径索引 */
	private int routeCurIndex;
	/** 实景导航核心算法工具 */
	private ARUtil arUtil = null;
	
	/**
	 * (便于Java SE环境下测试，Android平台下该方法不可使用)，构造一个新的 MinimapService
	 */
	public MinimapService() {
		this.init();
	}
	
	/**
	 * 通过指定代理(便于Java SE环境下测试，Android平台下该方法不可使用)，构造一个新的 MinimapService
	 */
	public MinimapService(String proxy) {
		this.init();
		this.proxy = proxy;
		this.isProxy = true;
	}
	
	/**
	 * 通过指定的Context，构造一个新的 MinimapService
	 */
	public MinimapService(Context context) {
		this.init();
		arUtil = new ARUtil(this, context, this.locationUpdateTime);
	}
	
	/**
	 * 通过指定的Context，地理位置更新时间，构造一个新的 MinimapService
	 */
	public MinimapService(Context context, int locationUpdateTime) {
		this.init();
		arUtil = new ARUtil(this, context, locationUpdateTime);
	}
	
	/**
	 * 初始化网络客户端数据
	 */
	private void init() {
		nameValuePairBase.put("div", CLIENT_VERSION);
		nameValuePairBase.put("dic", DEVELOPER_CODE);
		nameValuePairBase.put("cp", String.valueOf(COMPRESS));
	}
	/**
	 * POI搜索，根据关键字查询得到关键字在地球的具体信息
	 * @param keyword 查询关键字
	 * @param cityCode 查询城市代码，全国为000
	 * @param size 最多返回的结果条数
	 * @param page 页码，默认为1，起始为1
	 * @return Poi搜索结果的List数组
	 */
	public List<PoiSearch.Poi> searchPOI(String keyword, String cityCode, int size, int page) {
		Map<String, String> nameValuePair = new HashMap<String, String>();
		try {
			nameValuePair.putAll(nameValuePairBase);
			nameValuePair.put("t", "sname");
			nameValuePair.put("s", keyword);
			nameValuePair.put("city", cityCode);
			nameValuePair.put("size", String.valueOf(size));
			nameValuePair.put("page", String.valueOf(page));
			Result result = new Result(nameValuePair);
			Header header = result.getHeader();
			if(header != null) {
				if(header.getErrorCode() == 0) {//计算结果正常
					PoiSearch poiSearch = new PoiSearch();
					poiSearch.process(result.getDataResults(), CHARSET);
					System.out.println(poiSearch.toString());
					return poiSearch.getPois();
				} else {
					System.out.println("Header ErrorCode : " + header.getErrorCode());
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if(nameValuePair != null) {
				nameValuePair.clear();
				nameValuePair = null;
			}
		}
		return null;
	}
	
	/**
	 * 返回上一次周边查询的list列表
	 * @return 上一次周边查询的list列表
	 */
	public List<AroundSearch.Around> getLastAroundList() {
		if(lastAroundList.size() >= 1) {
			return lastAroundList.get(0);
		}
		return null;
	}
	
	/**
	 * 周边查询，在指定的用户手机坐标点范围内查询关键字地点（当GPS服务和网络服务可用时）<br>
	 * 说明：1, 2, 3, 4, 5 = ("全部", "餐厅", "购物", "公交", "加油站")
	 * @param type 查询关键字的类型
	 * @return 周边查询结果的List数组
	 */
	public List<AroundSearch.Around> searchAround(int type) {
		return this.searchAround(type, SEARCH_SIZE, SEARCH_RANGE);
	}
	
	/**
	 * 周边查询，在指定的用户手机坐标点范围内查询关键字地点（当GPS服务和网络服务可用时） <br>
	 * 说明：1, 2, 3, 4, 5 = ("全部", "餐厅", "购物", "公交", "加油站")
	 * @param type 查询关键字的类型
	 * @param size 最多返回的结果条数
	 * @return 周边查询结果的List数组
	 */
	public List<AroundSearch.Around> searchAround(int type, int size) {
		return this.searchAround(type, size, SEARCH_RANGE);
	}
	
	/**
	 * 周边查询，在指定的用户手机坐标点范围内查询关键字地点（当GPS服务和网络服务可用时） <br>
	 * 说明：1, 2, 3, 4, 5 = ("全部", "餐厅", "购物", "公交", "加油站")
	 * @param type 查询关键字的类型
	 * @param range 查询范围，单位：米
	 * @return 周边查询结果的List数组
	 */
	public List<AroundSearch.Around> searchAround(int type, int size, int range) {
		if(arUtil != null && isGPSDataAvailable()) {//当GPS服务和网络服务可用时
			List<AroundSearch.Around> list = searchAround(AroundSearch.type2Keyword(type), getMyLocationCoord20().getX(), getMyLocationCoord20().getY(), range, size, 1, 1);
			if(lastAroundList.size() == 2) {
				lastAroundList.remove(1);
			}
			lastAroundList.add(list);
			return list;
		} else {
			return null;
		}
	}
	
	/**
	 * 周边查询，在指定的坐标点范围内查询关键字地点
	 * @param keyword 查询关键字
	 * @param coordX 查询中心点坐标经度（20级像素坐标）
	 * @param coordY 查询中心点坐标纬度（20级像素坐标）
	 * @param range 查询范围，单位：米
	 * @param size 最多返回的结果条数
	 * @param page 页码，默认为1,起始为1
	 * @param coordDeflect 可选参数，值为1时有效，将中心点坐标偏转后再查询
	 * @return 周边查询结果的List数组
	 */
	public List<AroundSearch.Around> searchAround(String keyword, int coordX, int coordY, int range, int size, int page, int coordDeflect) {
		Map<String, String> nameValuePair = new HashMap<String, String>();
		try {
			nameValuePair.putAll(nameValuePairBase);
			nameValuePair.put("t", "saround");
			nameValuePair.put("s", keyword);
			nameValuePair.put("cx", String.valueOf(coordX));
			nameValuePair.put("cy", String.valueOf(coordY));
			nameValuePair.put("range", String.valueOf(range));
			nameValuePair.put("size", String.valueOf(size));
			nameValuePair.put("page", String.valueOf(page));
			nameValuePair.put("off", String.valueOf(coordDeflect));
			return searchAround(nameValuePair);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if(nameValuePair != null) {
				nameValuePair.clear();
				nameValuePair = null;
			}
		}
		return null;
	}
	
	/**
	 * 周边查询，在指定的城市的地点关键字范围内查询关键字地点
	 * @param keyword 查询关键字
	 * @param cityCode 中心点城市代码
	 * @param coordKeyword 中心点关键字
	 * @param range 查询范围，单位：米
	 * @param size 最多返回的结果条数
	 * @param page 页码，默认为1，起始为1
	 * @return 周边查询结果的List数组
	 */
	public List<AroundSearch.Around> searchAround(String keyword, String cityCode, String coordKeyword, int range, int size, int page) {
		Map<String, String> nameValuePair = new HashMap<String, String>();
		try {
			nameValuePair.putAll(nameValuePairBase);
			nameValuePair.put("t", "saround");
			nameValuePair.put("cs", coordKeyword);
			nameValuePair.put("city", cityCode);;
			nameValuePair.put("s", keyword);
			nameValuePair.put("range", String.valueOf(range));
			nameValuePair.put("size", String.valueOf(size));
			nameValuePair.put("page", String.valueOf(page));
			return searchAround(nameValuePair);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if(nameValuePair != null) {
				nameValuePair.clear();
				nameValuePair = null;
			}
		}
		return null;
	}
	
	/**
	 * 周边查询
	 * @param nameValuePair 传输给服务器端的多个参数对
	 * @return 周边查询结果的List数组
	 */
	private List<AroundSearch.Around> searchAround(Map<String, String> nameValuePair) {
		try {
			Result result = new Result(nameValuePair);
			// 获取文件头
			Header header = result.getHeader();
			if(header != null) {
				if(header.getErrorCode() == 0) {//计算结果正常
					AroundSearch aroundSearch = new AroundSearch(arUtil, this);
					// 获取数据块，并处理
					aroundSearch.process(result.getDataResults(), CHARSET);
					System.out.println(aroundSearch.toString());
					return aroundSearch.getArounds();
				} else {
					System.out.println("Header ErrorCode : " + header.getErrorCode());
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
//	/**
//	 * 行车路线查询，根据起点和终点的坐标查询路线数据，路线数据中包含了线路的详细的多个点坐标
//	 * @param startX 起点坐标经度（20级像素坐标）
//	 * @param startY 起点坐标纬度（20级像素坐标）
//	 * @param endX 终点坐标经度（20级像素坐标）
//	 * @param endY 终点坐标纬度（20级像素坐标）
//	 * @param method 计算模式 ：0速度优先，1 费用优先，2距离优先，3不走快速路
//	 * @param coordDeflect 坐标偏转：1偏转起点，2偏转终点，3偏转起点终点
//	 * @return 路线查询查询结果的List数组
//	 */
//	public List<RouteSearch.Route> searchCarRoute(int startX, int startY, int endX, int endY, int method, int coordDeflect) {
//		Map<String, String> nameValuePair = new HashMap<String, String>();
//		try {
//			nameValuePair.putAll(nameValuePairBase);
//			nameValuePair.put("t", "carroute");
//			nameValuePair.put("sx", String.valueOf(startX));
//			nameValuePair.put("sy", String.valueOf(startY));
//			nameValuePair.put("ex", String.valueOf(endX));
//			nameValuePair.put("ey", String.valueOf(endY));
//			nameValuePair.put("method", String.valueOf(method));
//			nameValuePair.put("off", String.valueOf(coordDeflect));
//		return searchRouteBase(nameValuePair);
//		} catch (Exception e) {
//			e.printStackTrace();
//		} finally {
//			if(nameValuePair != null) {
//				nameValuePair.clear();
//				nameValuePair = null;
//			}
//		}
//		return null;
//	}
	
	/**
	 * 返回上一次步行路线查询的list列表
	 * @return 上一次步行路线查询的list列表
	 */
	public List<RouteSearch.Route> getLastWalkRouteList() {
		if(lastWalkRouteList.size() >= 1) {
			return lastWalkRouteList.get(0);
		}
		return null;
	}
	
	/**
	 * 步行路线查询，根据起点和终点的坐标查询路线数据，路线数据中包含了线路的详细的多个点坐标
	 * @param endX 终点坐标经度（20级像素坐标）
	 * @param endY 终点坐标纬度（20级像素坐标）
	 * @return 步行查询查询结果的List数组
	 */
	public List<RouteSearch.Route> searchWalkRoute(int endX, int endY) {
		return searchWalkRoute(endX, endY, 2);
	}
	
	/**
	 * 步行路线查询，根据起点和终点的坐标查询路线数据，路线数据中包含了线路的详细的多个点坐标
	 * @param endX 终点坐标经度（20级像素坐标）
	 * @param endY 终点坐标纬度（20级像素坐标）
	 * @param method 计算模式 ：0速度优先，1 费用优先，2距离优先，3不走快速路
	 * @return 步行查询查询结果的List数组
	 */
	public List<RouteSearch.Route> searchWalkRoute(int endX, int endY, int method) {
		return searchWalkRoute(getMyLocationCoord20().getX(), getMyLocationCoord20().getY(), endX, endY, method, 1);
	}
	
	/**
	 * 步行路线查询，根据起点和终点的坐标查询路线数据，路线数据中包含了线路的详细的多个点坐标
	 * @param startX 起点坐标经度（20级像素坐标）
	 * @param startY 起点坐标纬度（20级像素坐标）
	 * @param endX 终点坐标经度（20级像素坐标）
	 * @param endY 终点坐标纬度（20级像素坐标）
	 * @param method 计算模式 ：0速度优先，1 费用优先，2距离优先，3不走快速路
	 * @param coordDeflect 坐标偏转：1偏转起点，2偏转终点，3偏转起点终点
	 * @return 步行查询查询结果的List数组
	 */
	public List<RouteSearch.Route> searchWalkRoute(int startX, int startY, int endX, int endY, int method, int coordDeflect) {
		Map<String, String> nameValuePair = new HashMap<String, String>();
		try {
			nameValuePair.putAll(nameValuePairBase);
			nameValuePair.put("t", "walkroute");
			nameValuePair.put("sx", String.valueOf(startX));
			nameValuePair.put("sy", String.valueOf(startY));
			nameValuePair.put("ex", String.valueOf(endX));
			nameValuePair.put("ey", String.valueOf(endY));
			nameValuePair.put("method", String.valueOf(method));
			nameValuePair.put("off", String.valueOf(coordDeflect));
			List<RouteSearch.Route> list = searchRouteBase(nameValuePair);
			if(lastWalkRouteList.size() == 2) {
				lastWalkRouteList.remove(1);
			}
			lastWalkRouteList.add(list);
			return list;
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if(nameValuePair != null) {
				nameValuePair.clear();
				nameValuePair = null;
			}
		}
		return null;
	}
	
	/**
	 * 行车路线查询，步行路线查询所用到的基础查询
	 * @param nameValuePair 传输给服务器端的多个参数对
	 * @return 查询结果的List数组
	 */
	private List<RouteSearch.Route> searchRouteBase(Map<String, String> nameValuePair) {
		try {
			Result result = new Result(nameValuePair);
			Header header = result.getHeader();
			if(header != null) {
				if(header.getErrorCode() == 0) {//计算结果正常
					RouteSearch routeSearch = new RouteSearch(this);
					routeSearch.process(result.getDataResults(), CHARSET);
					System.out.println(routeSearch.toString());
					return routeSearch.getRoutes();
				} else {
					System.out.println("Header ErrorCode : " + header.getErrorCode());
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
//	/**
//	 * 公交换乘查询，在指定的城市，根据起点和终点的坐标查询公交的换乘路线
//	 * @param cityCode 查询城市代码
//	 * @param startX 起点坐标经度（20级像素坐标）
//	 * @param startY 起点坐标纬度（20级像素坐标）
//	 * @param endX 终点坐标经度（20级像素坐标）
//	 * @param endY 终点坐标纬度（20级像素坐标）
//	 * @param method 计算模式 ：0速度优先，1 费用优先，2距离优先，3不走快速路
//	 * @param coordDeflect 坐标偏转：1偏转起点，2偏转终点，3偏转起点终点
//	 * @return 公交换乘查询结果的List数组
//	 */
//	public List<BusRouteSearch.BusRoute> searchBusRoute(String cityCode, int startX, int startY, int endX, int endY, int method, int coordDeflect) {
//		Map<String, String> nameValuePair = new HashMap<String, String>();
//		try {
//			nameValuePair.putAll(nameValuePairBase);
//			nameValuePair.put("t", "busroute");
//			nameValuePair.put("city", cityCode);
//			nameValuePair.put("sx", String.valueOf(startX));
//			nameValuePair.put("sy", String.valueOf(startY));
//			nameValuePair.put("ex", String.valueOf(endX));
//			nameValuePair.put("ey", String.valueOf(endY));
//			nameValuePair.put("method", String.valueOf(method));
//			nameValuePair.put("off", String.valueOf(coordDeflect));
//			Result result = new Result(nameValuePair);
//			Header header = result.getHeader();
//			if(header != null) {
//				if(header.getErrorCode() == 0) {//计算结果正常
//					BusRouteSearch busRouteSearch = new BusRouteSearch();
//					busRouteSearch.process(result.getDataResults(), CHARSET);
//					System.out.println(busRouteSearch.toString());
//					return busRouteSearch.getBusRoutes();
//				} else {
//					System.out.println("Header ErrorCode : " + header.getErrorCode());
//				}
//			}
//		} catch (Exception e) {
//			e.printStackTrace();
//		} finally {
//			if(nameValuePair != null) {
//				nameValuePair.clear();
//				nameValuePair = null;
//			}
//		}
//		return null;
//	}
//	
//	/**
//	 * 公交线路查询
//	 * @param name 公交线路名称
//	 * @param cityCode 查询城市代码
//	 * @param size 最多返回的结果条数
//	 * @param page 页码，默认为1，起始为1
//	 * @return 公交线路查询结果的List数组
//	 */
//	public List<BusLineSearch.BusLine> searchBusLine(String name, String cityCode, int size, int page) {
//		Map<String, String> nameValuePair = new HashMap<String, String>();
//		try {
//			nameValuePair.putAll(nameValuePairBase);
//			nameValuePair.put("t", "busline");
//			nameValuePair.put("s", name);
//			nameValuePair.put("type", String.valueOf(0));
//			nameValuePair.put("city", cityCode);
//			nameValuePair.put("size", String.valueOf(size));
//			nameValuePair.put("page", String.valueOf(page));
//			return searchBusLineBase(nameValuePair);
//		} catch (Exception e) {
//			e.printStackTrace();
//		} finally {
//			if(nameValuePair != null) {
//				nameValuePair.clear();
//				nameValuePair = null;
//			}
//		}
//		return null;
//	}
//	
//	/**
//	 * 公交站点查询
//	 * @param name 公交站点名称
//	 * @param cityCode 查询城市代码
//	 * @param size 最多返回的结果条数
//	 * @param page 页码，默认为1，起始为1
//	 * @return 公交站点查询查询结果的List数组
//	 */
//	public List<BusLineSearch.BusLine> searchBusStation(String name, String cityCode, int size, int page) {
//		Map<String, String> nameValuePair = new HashMap<String, String>();
//		try {
//			nameValuePair.putAll(nameValuePairBase);
//			nameValuePair.put("t", "busline");
//			nameValuePair.put("s", name);
//			nameValuePair.put("type", String.valueOf(1));
//			nameValuePair.put("city", cityCode);
//			nameValuePair.put("size", String.valueOf(size));
//			nameValuePair.put("page", String.valueOf(page));
//			return searchBusLineBase(nameValuePair);
//		} catch (Exception e) {
//			e.printStackTrace();
//		} finally {
//			if(nameValuePair != null) {
//				nameValuePair.clear();
//				nameValuePair = null;
//			}
//		}
//		return null;
//	}
//	
//	/**
//	 * 公交线路查询，公交站点查询所用到的基础查询
//	 * @param nameValuePair 传输给服务器端的多个参数对
//	 * @return 查询结果的List数组
//	 */
//	private List<BusLineSearch.BusLine> searchBusLineBase(Map<String, String> nameValuePair) {
//		try {
//			Result result = new Result(nameValuePair);
//			Header header = result.getHeader();
//			if(header != null) {
//				if(header.getErrorCode() == 0) {//计算结果正常
//					BusLineSearch busLineSearch = new BusLineSearch();
//					busLineSearch.process(result.getDataResults(), CHARSET);
//					System.out.println(busLineSearch.toString());
//					return busLineSearch.getBusLines();
//				} else {
//					System.out.println("Header ErrorCode : " + header.getErrorCode());
//				}
//			}
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		return null;
//	}
	
	/**
	 * 返回GPS真实经纬度转换后的20级像素坐标
	 * @param coordGps GPS真实经纬度对象
	 * @return GPS真实经纬度转换后的20级像素坐标
	 */
	public Coord20 coordGps2Coord20(CoordGps coordGps) {
		Coord20 coord20 = null;
		try {
			//这里控制小数点后几位，并保持四舍五入
			DecimalFormat df = new DecimalFormat("#.000000");
			double x = Double.parseDouble(df.format(coordGps.getX()));
			double y = Double.parseDouble(df.format(coordGps.getY()));
			String uriGet = "http://wap.mapabc.com/wap/exy.jsp?x=" + x +"&y=" + y +"&key=LeNoVo&uid=";
			String resultGet = null;
			if(isProxy) {
				resultGet = HttpClientUtil.getStringResultForHttpGet(uriGet, proxy, CHARSET, TIMEOUT);
			} else {
				resultGet = HttpClientUtil.getStringResultForHttpGet(uriGet, TIMEOUT);
			}
			if(resultGet == null || resultGet.indexOf("error") > -1) {
				System.out.println("Input date error Or HttpClient Exception!");
				coord20 = new Coord20(-1, -1);
				return coord20;
			}
			int coord20X = Integer.parseInt(resultGet.substring(resultGet.indexOf("<poix_20>") + "<poix_20>".length(), resultGet.indexOf("</poix_20>")).trim());
			int coord20Y = Integer.parseInt(resultGet.substring(resultGet.indexOf("<poiy_20>") + "<poiy_20>".length(), resultGet.indexOf("</poiy_20>")).trim());
			coord20 = new Coord20(coord20X, coord20Y);
		} catch (Exception e) {
			coord20 = new Coord20(-1, -1);
			e.printStackTrace();
		}
		return coord20;
	}
	
	/**
	 * 返回GPS真实经纬度转换后的偏转经纬度坐标
	 * @param coordGps GPS真实经纬度对象
	 * @return GPS真实经纬度转换后的偏转经纬度坐标
	 */
	public CoordDeflect coordGps2CoordDeflect(CoordGps coordGps) {
		CoordDeflect coordDeflect = null;
		try {
			Coord20 coord20 = coordGps2Coord20(coordGps);
			if(coord20.getX() == -1 && coord20.getY() == -1) {
				coordDeflect = new CoordDeflect(-1, -1);
			} else {
				coordDeflect = coord202CoordDeflect(coord20, false);
			}
		} catch (Exception e) {
			coordDeflect = new CoordDeflect(-1, -1);
			e.printStackTrace();
		}
		return coordDeflect;
	}
	
	/**
	 * 返回20级像素坐标转换后的偏转经纬度坐标
	 * @param coord20 20级像素坐标对象
	 * @param isCoordDeflect 高德软件查询得到的20级像素坐标是偏转过的坐标，isCoordDeflect=true，如果是用户真实gps坐标转换得到的20级像素坐标，isCoordDeflect=false
	 * @return 20级像素坐标转换后的偏转经纬度坐标
	 */
	public CoordDeflect coord202CoordDeflect(Coord20 coord20, boolean isCoordDeflect) {
		CoordDeflect coordDeflect = null;
		try {
			String uriGet = "http://wap.mapabc.com/wap/e2xy.jsp?turn=" + (isCoordDeflect ? "y" : "n") + "&x=" + coord20.getX() +"&y=" + coord20.getY() +"&key=LeNoVo&uid=";
			String resultGet = null; 
			if(isProxy) {
				resultGet = HttpClientUtil.getStringResultForHttpGet(uriGet, proxy, CHARSET, TIMEOUT);
			} else {
				resultGet = HttpClientUtil.getStringResultForHttpGet(uriGet, TIMEOUT);
			}
			if(resultGet == null || resultGet.indexOf("error") > -1) {
				System.out.println("Input date error Or HttpClient Exception!");
				coordDeflect = new CoordDeflect(-1, -1);
				return coordDeflect;
			}
			double coordDeflectX = Double.parseDouble(resultGet.substring(resultGet.indexOf("<x-coordinate>") + "<x-coordinate>".length(), resultGet.indexOf("</x-coordinate>")).trim());
			double coordDeflectY = Double.parseDouble(resultGet.substring(resultGet.indexOf("<y-coordinate>") + "<y-coordinate>".length(), resultGet.indexOf("</y-coordinate>")).trim());
			coordDeflect = new CoordDeflect(coordDeflectX, coordDeflectY);
		} catch (Exception e) {
			coordDeflect = new CoordDeflect(-1, -1);
			e.printStackTrace();
		}
		return coordDeflect;
	}
	
	/**
	 * 返回20级像素坐标起始点与目标点之间的直线距离，单位：米
	 * @param startCoord20 20级像素坐标起点
	 * @param endCoord20 20级像素坐标终点
	 * @param isCoordDeflect 高德软件查询得到的20级像素坐标是偏转过的坐标，isCoordDeflect=turn，如果是用户真实gps坐标转换得到的20级像素坐标，isCoordDeflect=false
	 * @return 20级像素坐标起始点与目标点之间的直线距离，单位：米
	 */
	public double getDistance(Coord20 startCoord20, Coord20 endCoord20, boolean isCoordDeflect) {
		CoordDeflect startCoordDeflect = coord202CoordDeflect(startCoord20, isCoordDeflect);
		CoordDeflect endCoordDeflect = coord202CoordDeflect(endCoord20, isCoordDeflect);
		return this.getDistance(startCoordDeflect, endCoordDeflect);
	}

	/**
	 * 返回GPS真实经纬度坐标起始点与目标点之间的直线距离，单位：米
	 * @param startCoordGps GPS真实经纬度坐标起点
	 * @param endCoordGps GPS真实经纬度坐标终点
	 * @return GPS真实经纬度坐标起始点与目标点之间的直线距离，单位：米
	 */
	public double getDistance(CoordGps startCoordGps, CoordGps endCoordGps) {
		CoordDeflect startCoordDeflect = coordGps2CoordDeflect(startCoordGps);
		CoordDeflect endCoordDeflect = coordGps2CoordDeflect(endCoordGps);
		return this.getDistance(startCoordDeflect, endCoordDeflect);
	}
	
	/**
	 * 返回偏转后的坐标起始点与目标点之间的直线距离，单位：米
	 * @param startCoordDeflect 偏转后的坐标起点
	 * @param endCoordDeflect 偏转后的坐标终点
	 * @return 偏转后的坐标起始点与目标点之间的直线距离，单位：米
	 */
	public double getDistance(CoordDeflect startCoordDeflect, CoordDeflect endCoordDeflect) {
		if((startCoordDeflect.getX() == -1 && startCoordDeflect.getY() == -1)
			|| (endCoordDeflect.getX() == -1 && endCoordDeflect.getY() == -1)) {
			return -1;
		}
		return arUtil.getDistance(startCoordDeflect.getX(), startCoordDeflect.getY(), endCoordDeflect.getX(), endCoordDeflect.getY());
	}
	
	/**
	 * 返回兴趣点离用户的距离
	 * @param endCoordDeflect 兴趣点偏转后坐标
	 * @return 返回兴趣点离用户的距离，单位：米
	 */
	public double getDistance(CoordDeflect endCoordDeflect) {
		if(arUtil == null) {
			return -1;
		}
		return this.getDistance(new CoordDeflect(getMyLocationDeflect().getX(), getMyLocationDeflect().getY()), new CoordDeflect(endCoordDeflect.getX(), endCoordDeflect.getY()));
	}
	
	/**
	 * 
	 * 版权所有(c)联想集团有限公司 1998-2010 保留所有权利.	<br />
	 * 项目：	<br />
	 * 描述：	解析服务器端的byte类型的查询结果<br />
	 * @author	zhangguojun<br />
	 * @version	1.0
	 * @since	JDK1.6, HttpClient4.0
	 */
	class Result {
		/** 文件头对象 */
		private Header header;
		/** 具体数据部分的byte数组 */
		private byte[] dataResults;
		/** 服务器端返回的全部byte数据数组 */
		private byte[] results;
		
		/**
		 * 初始化该类时，解析服务器端的byte类型的查询结果，1.服务器端的全部byte数组，2.数据头对象，3.具体数据部分的byte数组
		 * @param nameValuePair 传输给服务器端的多个参数对
		 */
		private Result(Map<String, String> nameValuePair) {
			try {
				if(isProxy) {
					results = HttpClientUtil.getBytesResultForHttpPost(URI_API, nameValuePair, proxy, CHARSET, TIMEOUT);
				} else {
					results = HttpClientUtil.getBytesResultForHttpPost(URI_API, nameValuePair, null, CHARSET, TIMEOUT);
				}
				if(results == null) {
					System.out.println("Result.返回数据大小：" + 0);
					return ;
				} else {
					System.out.println("Result.返回数据大小：" + results.length);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			// 解析文件头
			byte[] headerResults = new byte[Header.HEADER_LENGTH];
			System.arraycopy(results, 0, headerResults, 0, headerResults.length);
			
//			System.out.println("\n========================");
//			int len = headerResults.length;
//			for (int i = 0; i < len; i++) {
//				System.out.print(headerResults[i] + " ");
//			}
//			System.out.println("\n========================");
			
			header = new Header();
			header.process(headerResults);
			System.out.println(header.toString());
			
			// 解析数据块
			dataResults = new byte[results.length - Header.HEADER_LENGTH];
			System.arraycopy(results, Header.HEADER_LENGTH, dataResults, 0, dataResults.length);
			
//			System.out.println("\n========================");
//			int len2 = dataResults.length;
//			for (int i = 0; i < len2; i++) {
//				System.out.print(dataResults[i] + " ");
//			}
//			System.out.println("\n========================");
//			System.out.println("dataResults=" + HttpClientUtil.getString(dataResults, CHARSET));
//			System.out.println("\n========================");
		}
		/**
		 * @return 文件头对象
		 */
		private Header getHeader() {
			return header;
		}
		/**
		 * @return 具体数据部分的byte数组
		 */
		private byte[] getDataResults() {
			return dataResults;
		}
	}
	
	
	//******************实时数据处理（电子罗盘，地理坐标，手机屏幕坐标列表等）*******************
	
	/**
	 * 返回经过处理后的罗盘方向数值，单位：度；<br />
	 * 返回值范围[-∞, +∞]，0.0表示朝正北，以360度为周期，读数按顺时针递增，也就是90.0为正东，270.0为正西
	 * @return 经过处理后的罗盘方向数值，单位：度
	 */
	public float getDirection() {
		return arUtil.getDirection();
	}
	
	/**
	 * 返回经过处理后的垂直倾斜数值，单位：度<br />
	 * 返回值范围[-90.0, +90.0]，0.0表示手机屏幕朝内垂直，-90.0表示手机屏幕朝下水平，+90.0表示手机屏幕朝上水平
	 * @return 返回经过处理后的垂直倾斜数值，单位：度
	 */
	public float getInclination() {
		return arUtil.getInclination();
	}
	
	/**
	 * 返回当前位置GPS真实经纬度
	 * @return 当前位置GPS真实经纬度
	 */
	public CoordGps getMyLocationCoordGps(){
		return arUtil.getMyLocationCoordGps();
	}

	/**
	 * 返回当前位置20级像素坐标
	 * @return 当前位置20级像素坐标
	 */
	public Coord20 getMyLocationCoord20() {
		return arUtil.getMyLocationCoord20();	
	}
	
	/**
	 * 返回当前GPS偏转后位置
	 * @return 返回当前GPS偏转后位置
	 */
	public CoordDeflect getMyLocationDeflect() {
		return arUtil.getMyLocationDeflect();	
	}
	
	/**
	 * 判断GPS是否可用
	 * @return true: GPS已经就绪，可以读取数据；false：GPS未初始化完成
	 */
	public boolean isGPSDataAvailable() {
		return arUtil.isGPSDataAvailable();
	}
	
	/**
	 * 返回当前设备的屏幕宽度
	 * @return 返回当前设备的屏幕宽度
	 */
	public float getScreenWidth(){
		return arUtil.getScreenWidth();
	}
	
	/**
	 * 返回当前设备的屏幕高度
	 * @return 返回当前设备的屏幕高度
	 */
	public float getScreenHeight(){
		return arUtil.getScreenHeight();
	}
	
	/**
	 * 返回目前正在屏幕上显示的周边兴趣点列表List
	 * @return 目前正在屏幕上显示的周边兴趣点列表List
	 */
	public List<AroundSearch.Around> getDisplayAroundList() {
		List<AroundSearch.Around> list = lastAroundList.get((lastAroundList.size() > 1 ? 1 : 0));
		if(list == null){
			return null;
		}
		int len = list.size();
		for (int j = 0; j < len; j++) {
			if( (list.get(j).getScreenX() < 0) || (list.get(j).getScreenX() >= getScreenWidth()) || (list.get(j).getScreenY() < 0) || (list.get(j).getScreenY() >= getScreenHeight()) ){
				list.remove(j);				
			}
		}
		return list;
	}
	
	/**
	 * 设置为当前导航段的序号，在上次调用searchWalkRoute方法获得的导航路径列表中，在开始导航时使用index=0设置第一个导航段
	 * @param index 当前导航段的序号，在开始导航时使用index=0设置第一个导航段
	 * @return 设置正确与否。 true – 正确， false – 错误
	 */
	public boolean setCurrentNavigation(int index) {
		navigationCurIndex = index;
		if(navigationCurIndex >= 0 && navigationCurIndex < lastWalkRouteList.get((lastWalkRouteList.size() > 1 ? 1 : 0)).get(routeCurIndex).getNavigations().size()){
			return true;
		}else{
			return false;
		}
	}
	
	/** 
	 * 设置为当前导航路径的序号
	 * @param index 当前导航路径的序号，根据用户的选择
	 * @return 设置正确与否。 true – 正确， false – 错误
	 */	
	public boolean setCurrentRoute(int index){
		routeCurIndex = index;
		if(routeCurIndex >= 0 && routeCurIndex < lastWalkRouteList.get((lastWalkRouteList.size() > 1 ? 1 : 0)).size()){
			return true;
		}else{
			return false;
		}
	}
	
	/**
	 * 返回本导航段数据。配合setCurrentRoute和setCurrentNavigation使用，当设置了当前导航段后获得数据，就可以进行此段的导航
	 * @return 本导航段数据。配合setCurrentRoute和setCurrentNavigation使用，当设置了当前导航段后获得数据，就可以进行此段的导航
	 */
	public RouteSearch.Route.Navigation getCurrentNavigation() {
		try {
			if(lastWalkRouteList != null) {
				if(lastWalkRouteList.get((lastWalkRouteList.size() > 1 ? 1 : 0)).get(routeCurIndex) != null) {
					return lastWalkRouteList.get((lastWalkRouteList.size() > 1 ? 1 : 0)).get(routeCurIndex).getNavigations().get(navigationCurIndex);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * 判断本导航段是否完成：true-完成；false-未完成
	 * @return true-完成；false-未完成
	 */
	public boolean isNaviSegFinished() {
		int lastCoordIndex = getCurrentNavigation().getCoord20Size() - 1;	
		double distance = getDistance(new CoordDeflect(getMyLocationCoord20().getX(), getMyLocationCoord20().getY()), getCurrentNavigation().getCoordDeflects().get(lastCoordIndex));
		if(distance<10){
			return true;
		}else{
			return false;
		}		
	}
	
	/**
	 * 返回信息点偏转坐标点对应的手机屏幕x，y坐标数组，数组[0]=x坐标，数组[1]=y坐标
	 * @param coordDeflectX 坐标经度（偏转坐标）
	 * @param coordDeflectY 坐标纬度（偏转坐标）
	 * @return 信息点偏转坐标点对应的手机屏幕x，y坐标，数组[0]=x坐标，数组[1]=y坐标
	 */
	public float[] getScreenXY(double coordDeflectX, double coordDeflectY) {
		return arUtil.getScreenXY(coordDeflectX, coordDeflectY);
	}
	
	/**
	 * 结束，在Activity中的onDestroy()中调用
	 */
	public void onDestroy() {
		if(nameValuePairBase != null) {
			nameValuePairBase.clear();
			nameValuePairBase = null;
		}
		if(this.lastAroundList != null) {
			lastAroundList.clear();
			lastAroundList = null;
		}
		if(this.lastWalkRouteList != null) {
			lastWalkRouteList.clear();
			lastWalkRouteList = null;
		}
//		arUtil.onDestroy();
	}
	
	/**
	 * 在activity已经停止，重新开始的时候调用
	 * 会重新注册地理位置和传感器监听
	 */
	public void onRestart(){
		arUtil.onRestart();
    }
    
	/**
	 * 在activity要停止的时候调用
	 * 会销毁地理位置和传感器监听
	 * 如果某了Activity中销毁了地理位置和传感器监听，那么传入的另一个Activity
	 * 中就无法使用 
	 */
    public void onStop(){
    	arUtil.onStop();
    }
	
    /**
	 * 返回传感器原始的方向数值
	 * @return 返回传感器原始的方向数值
	 */
	public float getSensorDirection() {		
		return arUtil.getSensorDirection();			
	}
	
//	/**
//	 * @param args
//	 */
//	public static void main(String[] args) {
//		MinimapService service = new MinimapService("10.99.60.201:8080");
//		//实景导航提示列表详细信息的拼凑
//		System.out.println("\n+++POI搜索，根据关键字查询得到关键字在地球的具体信息+++++++++++++++++++++++");
//		List<PoiSearch.Poi> startPois = service.searchPOI("联想(北京)有限公司(上地西路)", "010", 30, 1);
//		int startCoord20X = startPois.get(0).getCoord20X();
//		int startCoord20Y = startPois.get(0).getCoord20Y();
//		List<PoiSearch.Poi> endPois = service.searchPOI("西二旗中路", "010", 30, 1);
//		int endCoord20X = endPois.get(0).getCoord20X();
//		int endCoord20Y = endPois.get(0).getCoord20Y();
//		System.out.println("\n+++步行路线查询，根据起点和终点的坐标查询路线数据，路线数据中包含了线路的详细的多个点坐标+++++++++++++++++++++++");
//		List<RouteSearch.Route> routes = service.searchWalkRoute(startCoord20X, startCoord20Y, endCoord20X, endCoord20Y, 0, 0);
//		List<Navigation> navigations = routes.get(0).getNavigations();
//    	int len = navigations.size();
//		for (int i = 0; i < len; i++) {
//			String string = (navigations.get(i).getRouteName().length() == 0 ? "" : "沿" + navigations.get(i).getRouteName()) + "向前" + navigations.get(i).getNavigationLength() + "米" + navigations.get(i).getNavigationActionText();
//			System.out.println(string);
//		}
		
//		CoordDeflect coordDeflect = service.coord202CoordDeflect(new Coord20(211825408, 110178324));
//		System.out.println("x=" + coordDeflect.getX() + " y=" + coordDeflect.getY());
		
//		Coord20 coord20 = service.coordGps2Coord20(new CoordGps(103.76537561416626, 29.555566906929016));
//		System.out.println("真实20级像素坐标 x=" + coord20.getX() + ", " + coord20.getY());
		
//		CoordDeflect coordDeflect = service.coordGps2CoordDeflect(new CoordGps(104.08125936985016, 30.662536025047302));
//		System.out.println("偏转坐标 x=" + coordDeflect.getX() + ", " + coordDeflect.getY());
//		
//		CoordDeflect coordDeflect2 = service.coord202CoordDeflect(new Coord20(211827877, 110179349), false);
//		System.out.println("高德偏转坐标 x=" + coordDeflect2.getX() + ", " + coordDeflect2.getY());
		
//		
//		System.out.println("\n+++周边查询，在指定的城市的地点关键字范围内查询关键字地点+++++++++++++++++++++++");
//		service.searchAround("酒店", 211590800, 111131623, 500, 30, 1, 0);
//		service.searchAround("酒店", "0833", "时美酒店", 3000, 30, 1);
		
//		System.out.println("\n+++行车路线查询，根据起点和终点的坐标查询路线数据，路线数据中包含了线路的详细的多个点坐标+++++++++++++++++++++++");
//		service.searchCarRoute(221010326, 101713397, 224796735, 109688998, 0, 0);
//		
//		System.out.println("\n+++步行路线查询，根据起点和终点的坐标查询路线数据，路线数据中包含了线路的详细的多个点坐标+++++++++++++++++++++++");
//		service.searchWalkRoute(220936021, 101572532, 220948983, 101571728, 2, 0);
//		
//		System.out.println("\n+++公交换乘，在指定的城市，根据起点和终点的坐标查询公交的换乘路线+++++++++++++++++++++++");
//		service.searchBusRoute("010", 221064506, 101670521, 221057600, 101677856, 0, 0);
//		
//		System.out.println("\n+++公交线路查询+++++++++++++++++++++++");
//		service.searchBusLine("518", "010", 2, 1);
//		System.out.println("\n+++公交站点查询+++++++++++++++++++++++");
//		service.searchBusStation("软件园广场", "010", 2, 1);
//	}
}