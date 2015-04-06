/*
 * 文件名：	HttpClientUtil.java
 * 日期：	2010-1-8
 * 修改历史：
 * [时间]		[修改者]			[修改内容]
 */
package com.lenovo.minimap;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.conn.params.ConnRouteParams;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.apache.http.util.VersionInfo;

/**
 * 版权所有(c)联想集团有限公司 1998-2010 保留所有权利。<br />
 * 项目：	<br />
 * 描述：	Apache的HttpClient应用，模拟客户端提交，包括Post和Get两种提交方式<br />
 * @author	zhangguojun<br />
 * @version	1.0
 * @since	JDK1.6, HttpClient4.0
 */
public class HttpClientUtil {
	/** 提交数据的默认编码 */
	private static final String DEFAULT_CHARSET = HTTP.UTF_8;
	/** 连接超时时间，单位：毫秒  */
	private static final int DEFAULT_TIMEOUT = 5000;

	/**
	 * 返回以Post方式提交的服务器端返回数据
	 * @param uri 服务器通 用资源标志符URI
	 * @param nameValuePair 传输给服务器端的多个参数对
	 * @return 服务器端返回结果的字节数组表示形式
	 */
	public static byte[] getBytesResultForHttpPost(String uri, Map<String, String> nameValuePair) {
		return getBytesResultForHttpPost(uri, nameValuePair, null, DEFAULT_CHARSET, DEFAULT_TIMEOUT);
	}
	
	/**
	 * 返回以Post方式提交的服务器端返回数据
	 * @param uri 服务器通 用资源标志符URI
	 * @param nameValuePair 传输给服务器端的多个参数对
	 * @param proxy 代理主机地址(地址:端口号)
	 * @return 服务器端返回结果的字节数组表示形式
	 */
	public static byte[] getBytesResultForHttpPost(String uri, Map<String, String> nameValuePair, String proxy) {
		return getBytesResultForHttpPost(uri, nameValuePair, proxy, DEFAULT_CHARSET, DEFAULT_TIMEOUT);
	}
	
	/**
	 * 返回以Post方式提交的服务器端返回数据
	 * @param uri 服务器通 用资源标志符URI
	 * @param nameValuePair 传输给服务器端的多个参数对
	 * @param proxy 代理主机地址(地址:端口号)
	 * @param charset 提交内容的编码方式
	 * @param timeout 连接超时时间，单位：毫秒
	 * @return 服务器端返回结果的字节数组表示形式
	 */
	public static byte[] getBytesResultForHttpPost(String uri, Map<String, String> nameValuePair, String proxy, String charset, int timeout) {
		byte[] result = null;
		HttpEntity entity = null;
		HttpPost httpRequest = new HttpPost(uri); // POST提交方式
		List<NameValuePair> params = new ArrayList<NameValuePair>();
		for (Map.Entry<String, String> entry : nameValuePair.entrySet()) {
			params.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
		}
		HttpClient httpClient = getHttpClient(proxy, charset, timeout);
		try {
			httpRequest.setEntity(new UrlEncodedFormEntity(params, DEFAULT_CHARSET));
			HttpResponse httpResponse = httpClient.execute(httpRequest); // 连网提交请求，并获取返回信息
			if (httpResponse.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
				entity = httpResponse.getEntity();
				result = EntityUtils.toByteArray(entity);
//				System.out.println("HttpStatusCode : " + String.valueOf(httpResponse.getStatusLine().getStatusCode()));
			} else {
//				System.err.println("Error Response: " + httpResponse.getStatusLine().toString());
			}
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			close(entity, httpRequest, httpClient);
		}
		return result;
	}
	
	/**
	 * 返回以Post方式提交的服务器端返回数据
	 * @param uri 服务器通 用资源标志符URI
	 * @param nameValuePair 传输给服务器端的多个参数对
	 * @return 服务器端返回结果的字符串表示形式
	 */
	public static String getStringResultForHttpPost(String uri, Map<String, String> nameValuePair) {
		return getStringResultForHttpPost(uri, nameValuePair, null, DEFAULT_CHARSET, DEFAULT_TIMEOUT);
	}
	
	/**
	 * 返回以Post方式提交的服务器端返回数据
	 * @param uri 服务器通 用资源标志符URI
	 * @param nameValuePair 传输给服务器端的多个参数对
	 * @param proxy 代理主机地址(地址:端口号)
	 * @return 服务器端返回结果的字符串表示形式
	 */
	public static String getStringResultForHttpPost(String uri, Map<String, String> nameValuePair, String proxy) {
		return getStringResultForHttpPost(uri, nameValuePair, proxy, DEFAULT_CHARSET, DEFAULT_TIMEOUT);
	}
	
	/**
	 * 返回以Post方式提交的服务器端返回数据
	 * @param uri 服务器通 用资源标志符URI
	 * @param nameValuePair 传输给服务器端的多个参数对
	 * @param proxy 代理主机地址(地址:端口号)
	 * @param charset 提交内容的编码方式
	 * @return 服务器端返回结果的字符串表示形式
	 */
	public static String getStringResultForHttpPost(String uri, Map<String, String> nameValuePair, String proxy, String charset) {
		return getStringResultForHttpPost(uri, nameValuePair, proxy, charset, DEFAULT_TIMEOUT);
	}
	
	/**
	 * 返回以Post方式提交的服务器端返回数据
	 * @param uri 服务器通 用资源标志符URI
	 * @param nameValuePair 传输给服务器端的多个参数对
	 * @param proxy 代理主机地址(地址:端口号)
	 * @param charset 提交内容的编码方式
	 * @param timeout 连接超时时间，单位：毫秒
	 * @return 服务器端返回结果的字符串表示形式
	 */
	public static String getStringResultForHttpPost(String uri, Map<String, String> nameValuePair, String proxy, String charset, int timeout) {
		if(uri == null) return null;
		String result = null;
		HttpEntity entity = null;
		HttpPost httpRequest = new HttpPost(uri);
		List<NameValuePair> params = new ArrayList<NameValuePair>();
		for (Map.Entry<String, String> entry : nameValuePair.entrySet()) {
			params.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
		}
		HttpClient httpClient = getHttpClient(proxy, charset, timeout);
		try {
			httpRequest.setEntity(new UrlEncodedFormEntity(params, charset));
			HttpResponse httpResponse = httpClient.execute(httpRequest);
			if (httpResponse.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
				entity = httpResponse.getEntity();
				result = EntityUtils.toString(entity);
//				System.out.println("HttpStatusCode : " + String.valueOf(httpResponse.getStatusLine().getStatusCode()));
			} else {
//				System.err.println("Error Response: " + httpResponse.getStatusLine().toString());
			}
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			close(entity, httpRequest, httpClient);
		}
		return result;
	}
	
	/**
	 * 返回以Get方式提交的服务器端返回数据
	 * @param uri 服务器通 用资源标志符URI
	 * @return 服务器端返回结果的字节数组表示形式
	 */
	public static byte[] getBytesResultForHttpGet(String uri) {
		return getBytesResultForHttpGet(uri, null,DEFAULT_CHARSET, DEFAULT_TIMEOUT);
	}
	
	/**
	 * 返回以Get方式提交的服务器端返回数据
	 * @param uri 服务器通 用资源标志符URI
	 * @param proxy 代理主机地址(地址:端口号)
	 * @return 服务器端返回结果的字节数组表示形式
	 */
	public static byte[] getBytesResultForHttpGet(String uri, String proxy) {
		return getBytesResultForHttpGet(uri, proxy, DEFAULT_CHARSET, DEFAULT_TIMEOUT);
	}

	/**
	 * 返回以Get方式提交的服务器端返回数据
	 * @param uri 服务器通 用资源标志符URI
	 * @param proxy 代理主机地址(地址:端口号)
	 * @param charset 提交内容的编码方式
	 * @param timeout 连接超时时间，单位：毫秒
	 * @return 服务器端返回结果的字节数组表示形式
	 */
	public static byte[] getBytesResultForHttpGet(String uri, String proxy, String charset, int timeout) {
		byte[] result = null;
		HttpEntity entity = null;
		HttpClient httpClient = getHttpClient(proxy, charset, timeout);
		HttpGet httpRequest = new HttpGet(uri);
		try {
			HttpResponse httpResponse = httpClient.execute(httpRequest);
			if (httpResponse.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
				entity = httpResponse.getEntity();
				result = EntityUtils.toByteArray(entity);
//				System.out.println("HttpStatusCode : " + String.valueOf(httpResponse.getStatusLine().getStatusCode()));
			} else {
//				System.err.println("Error Response: " + httpResponse.getStatusLine().toString());
			}
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			close(entity, httpRequest, httpClient);
		}
		return result;
	}
	
	/**
	 * 返回以Get方式提交的服务器端返回数据
	 * @param uri 服务器通 用资源标志符URI
	 * @return 服务器端返回结果的字符串表示形式
	 */
	public static String getStringResultForHttpGet(String uri) {
		return getStringResultForHttpGet(uri, null, DEFAULT_CHARSET, DEFAULT_TIMEOUT);
	}
	
	/**
	 * 返回以Get方式提交的服务器端返回数据
	 * @param uri 服务器通 用资源标志符URI
	 * @param timeout 连接超时时间，单位：毫秒
	 * @return 服务器端返回结果的字符串表示形式
	 */
	public static String getStringResultForHttpGet(String uri, int timeout) {
		return getStringResultForHttpGet(uri, null, DEFAULT_CHARSET, timeout);
	}
	
	/**
	 * 返回以Get方式提交的服务器端返回数据
	 * @param uri 服务器通 用资源标志符URI
	 * @param proxy 代理主机地址(地址:端口号)
	 * @return 服务器端返回结果的字符串表示形式
	 */
	public static String getStringResultForHttpGet(String uri, String proxy) {
		return getStringResultForHttpGet(uri, proxy, DEFAULT_CHARSET, DEFAULT_TIMEOUT);
	}
	
	/**
	 * 返回以Get方式提交的服务器端返回数据
	 * @param uri 服务器通 用资源标志符URI
	 * @param proxy 代理主机地址(地址:端口号)
	 * @param charset 提交内容的编码方式
	 * @return 服务器端返回结果的字符串表示形式
	 */
	public static String getStringResultForHttpGet(String uri, String proxy, String charset) {
		return getStringResultForHttpGet(uri, proxy, charset, DEFAULT_TIMEOUT);
	}

	/**
	 * 返回以Get方式提交的服务器端返回数据
	 * @param uri 服务器通 用资源标志符URI
	 * @param proxy 代理主机地址(地址:端口号)
	 * @param charset 提交内容的编码方式
	 * @param timeout 连接超时时间，单位：毫秒
	 * @return 服务器端返回结果的字符串表示形式
	 */
	public static String getStringResultForHttpGet(String uri, String proxy, String charset, int timeout) {
		String result = null;
		HttpEntity entity = null;
		HttpClient httpClient = getHttpClient(proxy, charset, timeout);
		HttpGet httpRequest = new HttpGet(uri);
		try {
			HttpResponse httpResponse = httpClient.execute(httpRequest);
			if (httpResponse.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
				entity = httpResponse.getEntity();
				result = EntityUtils.toString(entity);
//				System.out.println("HttpStatusCode : " + String.valueOf(httpResponse.getStatusLine().getStatusCode()));
			} else {
//				System.err.println("Error Response: " + httpResponse.getStatusLine().toString());
			}
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			close(entity, httpRequest, httpClient);
		}
		return result;
	}

	/**
	 * 关闭连接过程中的Http实体对象，Http请求对象，Http客户端对象
	 * @param entity Http实体对象
	 * @param httpRequest Http请求对象
	 * @param httpClient Http客户端对象
	 */
	private static void close(HttpEntity entity, HttpRequestBase httpRequest, HttpClient httpClient) {
		if (entity != null) {
			 try {
				entity.consumeContent();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		if(httpRequest != null) {
			// Do not feel like reading the response body
	        // Call abort on the request object
			httpRequest.abort();
		}
		// When HttpClient instance is no longer needed, 
       // shut down the connection manager to ensure
       // immediate deallocation of all system resources
		httpClient.getConnectionManager().shutdown();
	}
	
	/**
	 * 返回字符串中符合某正则表达式条件的字符串替换成指定字符串的新字符串<br />
	 * 如：result = matcherReplace("(\r\n|\r|\n|\n\r)", "", strResult);
	 * @param strFrom 需要符合的正则表达式条件
	 * @param strTo 替换成指定字符串
	 * @param strTarget 需要处理的字符串
	 * @return 字符串中符合某正则表达式条件的字符串替换成指定字符串的新字符串
	 */
	public static String matcherReplace(String strFrom, String strTo, String strTarget) {
		Matcher m = Pattern.compile("(?i)" + strFrom).matcher(strTarget);
		if (m.find()) {
			return strTarget.replaceAll(strFrom, strTo);
		} else {
			return strTarget;
		}
	}

	/**
	 * 返回输入流转换后的字节数组
	 * @param is 输入流
	 * @return 输入流转换后的字节数组
	 * @throws IOException 失败或中断的 I/O 操作生成的异常
	 */
	public static byte[] inputStream2Bytes(InputStream is) {
		int BUFFER_SIZE = 1024;
		byte[] bytes = null;
		ByteArrayOutputStream baos = null;
		try {
			baos = new ByteArrayOutputStream();
			byte[] b = new byte[BUFFER_SIZE];
			int len = 0;
			while ((len = is.read(b, 0, BUFFER_SIZE)) != -1) {
				baos.write(b, 0, len);
			}
			bytes = baos.toByteArray();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if(baos != null) {
					baos.flush();
					baos.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return bytes;
	}

	/**
	 * 返回字节数组转换后的输入流
	 * @param bytes 字节数组
	 * @return 字节数组转换后的输入流
	 */
	public InputStream bytes2InputStream(byte[] bytes) {
		if(bytes != null && bytes.length > 0) {
			return new ByteArrayInputStream(bytes);
		} else {
			return null;
		}
	}
	
	/**
	 * 返回字符串的指定地址栏编码后的新字符串
	 * @param str 字符串
	 * @param charset 编码方式
	 * @return 字符串的指定地址栏编码后的新字符串
	 * @throws UnsupportedEncodingException 不支持字符编码异常
	 */
	public static String getUrlEncode(String str, String charset) {
		try {
			return URLEncoder.encode(str, charset);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return str;
	}
	
	/**
	 * 返回字符串的地址栏UTF-8编码后的新字符串
	 * @param str 字符串
	 * @return 字符串的地址栏UTF-8编码后的新字符串
	 */
	public static String getUrlEncodeForUTF8(String str) {
		return getUrlEncode(str, DEFAULT_CHARSET);
	}
	
	/**
	 * 通过使用指定的 charset 解码指定的 byte 数组，返回一个新的 String
	 * @param bytes  byte 数组
	 * @param toEncode 指定的 charset 解码
	 * @return 通过使用指定的 charset 解码指定的 byte 数组，返回一个新的 String
	 * @throws UnsupportedEncodingException 不支持字符编码异常
	 */
	public static String getString(byte[] bytes, String toEncode) {
		try {
			return new String(bytes, toEncode);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return bytes.toString();
	}
	
	/**
	 * 通过使用指定的 charset 解码指定的String，返回一个新的 String
	 * @param str 指定的String
	 * @param toEncode 指定的 charset 解码
	 * @return 通过使用指定的 charset 解码指定的String，返回一个新的 String
	 */
	public static String getString(String str, String toEncode) {
		return getString(str, null, toEncode);
	}
	
	/**
	 * 通过使用指定的 charset 编码指定的String再使用指定的 charset 解码该字符串，返回一个新的 String
	 * @param str 指定的String
	 * @param fromEncode
	 * @param toEncode
	 * @return 通过使用指定的 charset 编码指定的String再使用指定的 charset 解码该字符串，返回一个新的 String
	 */
	public static String getString(String str, String fromEncode, String toEncode) {
		try {
			return new String(fromEncode == null ? str.getBytes() : str.getBytes(fromEncode), toEncode);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return str;
	}
	
	/**
	 * 返回1位长度的byte数组对应的int值
	 * @param b byte数组
	 * @return 1位长度的byte数组对应的int值
	 */
	public static int getByte(byte[] b) {
		if(b.length < 1) return -1;
		return (b[0]&0xff);
	}
	
	/**
	 * 返回4位长度的byte数组对应的int值
	 * @param b byte数组
	 * @return 4位长度的byte数组对应的int值
	 */
	public static int getInt(byte[] b) {
		if(b.length < 4) return -1;
		return ((b[3]&0xff)<<24) + ((b[2]&0xff)<<16) + ((b[1]&0xff)<<8) + (b[0]&0xff);
	}
	
	/**
	 * 返回2位长度的byte数组对应的int值
	 * @param b byte数组
	 * @return 2位长度的byte数组对应的int值
	 */
	public static int getShort(byte[] b) {
		if(b.length < 2) return -1;
		return ((b[1]&0xff)<<8) + (b[0]&0xff);
	}
	
	/**
	 * 返回一个自定义的 HttpClient 对象
	 * @param proxy 代理主机地址(地址:端口号)
	 * @param charset 提交内容的编码方式
	 * @param timeout 连接超时时间，单位：毫秒
	 * @return 一个自定义的 HttpClient 对象
	 */
	private static HttpClient getHttpClient(String proxy, String charset, int timeout) {
        HttpParams params = new BasicHttpParams();
        if(proxy != null) {
        	ConnRouteParams.setDefaultProxy(params, new HttpHost((proxy.lastIndexOf(':') == -1 ? proxy : proxy.substring(0, proxy.lastIndexOf(':'))), Integer.parseInt(proxy.lastIndexOf(':') == -1 ? "8080" : proxy.substring(proxy.lastIndexOf(':') + 1))));//设置代理
        }
        HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
        HttpProtocolParams.setContentCharset(params, (charset == null ? DEFAULT_CHARSET : charset));
        HttpProtocolParams.setUseExpectContinue(params, true);
        HttpConnectionParams.setTcpNoDelay(params, true);
        HttpConnectionParams.setSocketBufferSize(params, 8192);
        HttpConnectionParams.setSoTimeout(params, (timeout == 0 ? DEFAULT_TIMEOUT : timeout));
        VersionInfo vi = VersionInfo.loadVersionInfo("org.apache.http.client", HttpClientUtil.class.getClassLoader());
        String release = vi == null ? "UNAVAILABLE" : vi.getRelease();
        HttpProtocolParams.setUserAgent(params, (new StringBuilder()).append("Apache-HttpClient/").append(release).append(" (java 1.6)").toString());
        return new DefaultHttpClient(params);
    }
	
//	/**
//	 * @param args
//	 */
//	public static void main(String[] args) {
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
//	}
}
