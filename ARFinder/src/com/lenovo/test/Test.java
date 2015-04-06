/**
 * 
 */
package com.lenovo.test;

import java.util.List;

import com.lenovo.minimap.MinimapService;
import com.lenovo.minimap.search.PoiSearch;

/**
 * @author zhangguojun
 *
 */
public class Test {

	public void testHttpClientUtil() {
//		//Get method
//		String proxy = "10.99.60.201:8080";
//		String charset = "UTF-8";
//		
//		String uriGet = "http://www.dubblogs.cc:8751/Android/Test/API/Get/index.php?str=" + getUrlEncode("这是 Get 方法", "GBK");
//		String resultGet = HttpClientUtil.getStringResultForHttpGet(uriGet, proxy, charset);
//		resultGet = getString(resultGet, "ISO-8859-1", "GBK");
//		System.out.println("Get method : " + resultGet);
//		
//		//Post method
//		String uriPost = "http://www.dubblogs.cc:8751/Android/Test/API/Post/index.php";
//		Map<String, String> nameValuePair = new java.util.HashMap<String, String>();
//		nameValuePair.put("str", "这是 Post 方法");
//		String resultPost = HttpClientUtil.getStringResultForHttpPost(uriPost, nameValuePair, proxy, charset);
//		resultPost = getString(resultPost, "ISO-8859-1", charset);
//		System.out.println("Post method : " + resultPost);
	}
	
	public void testMinimapServiceApi () {
		MinimapService service = new MinimapService("10.99.60.201:8080");
		//实景导航提示列表详细信息的拼凑
		System.out.println("\n+++POI搜索，根据关键字查询得到关键字在地球的具体信息+++++++++++++++++++++++");
		List<PoiSearch.Poi> startPois = service.searchPOI("联想(北京)有限公司(上地西路)", "010", 30, 1);
//		int startCoord20X = startPois.get(0).getCoord20X();
//		int startCoord20Y = startPois.get(0).getCoord20Y();
//		List<PoiSearch.Poi> endPois = service.searchPOI("西二旗中路", "010", 30, 1);
//		int endCoord20X = endPois.get(0).getCoord20X();
//		int endCoord20Y = endPois.get(0).getCoord20Y();
//		System.out.println("\n+++步行路线查询，根据起点和终点的坐标查询路线数据，路线数据中包含了线路的详细的多个点坐标+++++++++++++++++++++++");
//		List<RouteSearch.Route> routes = service.searchWalkRoute(startCoord20X, startCoord20Y, endCoord20X, endCoord20Y, 0, 0);
//		List<Navigation> navigations = routes.get(0).getNavigations();
//		int len = navigations.size();
//		for (int i = 0; i < len; i++) {
//			String string = (navigations.get(i).getRouteName().length() == 0 ? "" : "沿" + navigations.get(i).getRouteName()) + "向前" + navigations.get(i).getNavigationLength() + "米" + navigations.get(i).getNavigationActionText();
//			System.out.println(string);
//		}
//		service.searchAround(1);
		
//		CoordDeflect coordDeflect = service.coord202CoordDeflect(new Coord20(211825408, 110178324), true);
//		System.out.println("x=" + coordDeflect.getX() + " y=" + coordDeflect.getY());
		
//		Coord20 coord20 = service.coordGps2Coord20(new CoordGps(103.76537561416626, 29.555566906929016));
//		System.out.println("真实20级像素坐标 x=" + coord20.getX() + ", " + coord20.getY());
		
//		CoordDeflect coordDeflect2 = service.coordGps2CoordDeflect(new CoordGps(104.08125936985016, 30.662536025047302));
//		System.out.println("偏转坐标 x=" + coordDeflect2.getX() + ", " + coordDeflect2.getY());
//		
//		CoordDeflect coordDeflect3 = service.coord202CoordDeflect(new Coord20(211827877, 110179349), false);
//		System.out.println("高德偏转坐标 x=" + coordDeflect3.getX() + ", " + coordDeflect3.getY());
		
		
		System.out.println("\n+++周边查询，在指定的城市的地点关键字范围内查询关键字地点+++++++++++++++++++++++");
		service.searchAround("酒店", 211590800, 111131623, 500, 30, 1, 0);
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
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Test test = new Test();
//		test.testHttpClientUtil();
		test.testMinimapServiceApi();
	}    

}
