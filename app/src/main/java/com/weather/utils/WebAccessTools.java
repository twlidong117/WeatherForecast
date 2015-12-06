package com.weather.utils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;

import android.content.Context;

import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;

/**
 * 网站访问工具类，用于Android的网络访问
 */
public class WebAccessTools {

	/**
	 * 当前的Context上下文对象
	 */
	private Context context;
	/**
	 * 构造一个网站访问工具类
	 * @param context 记录当前Activity中的Context上下文对象
	 */
	public WebAccessTools(Context context) {
		this.context = context;
	}

	/**
	 * 根据给定的url地址访问网络，得到响应内容(这里为GET方式访问)
	 * @param url 指定的url地址
	 * @return web服务器响应的内容，为<code>String</code>类型，当访问失败时，返回为null
	 */
	public  String getWebContent(String url) {
        //创建一个http请求对象
        HttpGet request = new HttpGet(url);
        //创建HttpParams以用来设置HTTP参数
        HttpParams params=new BasicHttpParams();
        //设置连接超时或响应超时
        //HttpConnectionParams.setConnectionTimeout(params, 3000);
        //HttpConnectionParams.setSoTimeout(params, 5000);
        //创建一个网络访问处理对象
        HttpClient httpClient = new DefaultHttpClient(params);
        try{
            //执行请求参数项
            HttpResponse response = httpClient.execute(request);
            //判断是否请求成功
            if(response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                //获得响应信息
               return EntityUtils.toString(response.getEntity());
            } else {
                //网连接失败，使用Toast显示提示信息
                Toast.makeText(context, "网络访问失败，请检查您机器的联网设备!", Toast.LENGTH_LONG).show();
            }

        }catch(Exception e) {
            e.printStackTrace();
        } finally {
            //释放网络连接资源
            httpClient.getConnectionManager().shutdown();
        }
		return null;
	}
	public String getWeatherInfo(String cityId) {

		BufferedReader reader = null;
		String result = "";
		String httpUrl = "http://apis.baidu.com/heweather/weather/free?cityid=CN"+cityId;


		try {
			URL url = new URL(httpUrl);
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("GET");
			// 填入apikey到HTTP header
			connection.setRequestProperty("apikey","082c11ecc64ccffd9c3cc2a46c3bf6fd");
			connection.connect();

			InputStream is = connection.getInputStream();
			reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));

			String strRead;
			while ((strRead = reader.readLine()) != null) {
				result += strRead;
			}

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (reader!=null) reader.close();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
		return result;
	}
}
