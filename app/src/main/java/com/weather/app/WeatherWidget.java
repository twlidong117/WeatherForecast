package com.weather.app;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.weather.utils.WebAccessTools;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.RemoteViews;

public class WeatherWidget extends AppWidgetProvider {
	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager,
			int[] appWidgetIds) {
		super.onUpdate(context, appWidgetManager, appWidgetIds);
		
		//这样在第一次运行时也能响应用户的单击事件
		getWeatherView(context);
		
		//启动一个自定义更新widget的后台服务
		context.startService(new Intent(context,UpdateWidgetService.class));
	}
	
	@Override //当删除最后一个Widget组件后调用
	public void onDisabled(Context context) {
		super.onDisabled(context);
		//关闭后台服务
		context.stopService(new Intent(context, UpdateWidgetService.class));
	}

	//返回widget中的布局视图对象
	public static RemoteViews getWeatherView(Context context){
		RemoteViews views=new RemoteViews(context.getPackageName(), R.layout.widget_layout);
		
		//当击widget的主体来启动MainActivity返回到天气精灵的天气显示界面
		Intent intent = new Intent(context, MainActivity.class);
		PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
		views.setOnClickPendingIntent(R.id.weather_rootLayout, pendingIntent);
		
		return views;
	}
	
	public static void updateAppWidget(RemoteViews views, Context context, 
			AppWidgetManager appWidgetManager, String cityCode) {

        updateWeather(views, context, cityCode);
		
		//更新时间
		Date date = new Date();
		SimpleDateFormat format = new SimpleDateFormat("hh:mm");
		String timeText = format.format(date);
		Log.i("widget", "===================update  time======"+timeText+"=====================");
		views.setTextViewText(R.id.widget_time, timeText);
	}

	
	//从网络中更新天气文件和views中的显示数据
	public static void updateWeather(RemoteViews views, Context context, String cityCode) {

		try {
			String info=new WebAccessTools(context).getWeatherInfo(cityCode);

            JSONObject json=new JSONObject(info).getJSONArray("HeWeather data service 3.0").getJSONObject(0);
            JSONObject jsonBasic = json.getJSONObject("basic");
            JSONObject jsonWeatherNow = json.getJSONObject("now");

            //城市
            info=jsonBasic.getString("city");
            views.setTextViewText(R.id.widget_city, info);
            //更新时间
            info= "更新于"+jsonBasic.getJSONObject("update").getString("loc");
            views.setTextViewText(R.id.widget_date,info);
            //天气
            JSONObject jsonCond= jsonWeatherNow.getJSONObject("cond");
            info=jsonCond.getString("txt");
            views.setTextViewText(R.id.widget_weather,info);
            //天气图标
            info=jsonCond.getString("code");
            views.setImageViewResource(R.id.widget_icon,getWeatherIconByCode(info));
            //温度
            info=toCelsiusFormat(jsonWeatherNow.getString("tmp"));
            views.setTextViewText(R.id.widget_temp,info);
            //体感温度
            info="体感:"+toCelsiusFormat(jsonWeatherNow.getString("fl"));
            views.setTextViewText(R.id.widget_fl,info);

		}catch(JSONException e) {
			e.printStackTrace();
		}
	}
    static private String toCelsiusFormat(String tmp) {
        return tmp + "℃";
    }
    //由天气情况得到图标
    static private int getWeatherIconByCode(String weatherCode) {
        int code = Integer.parseInt(weatherCode);
        switch (code) {
            case 100:
                return R.drawable.weathericon_100;

            case 101:
                return R.drawable.weathericon_101;

            case 102:
                return R.drawable.weathericon_102;

            case 103:
                return R.drawable.weathericon_103;

            case 104:
                return R.drawable.weathericon_104;

            case 200:
                return R.drawable.weathericon_200;

            case 201:
                return R.drawable.weathericon_201;

            case 202:
            case 203:
            case 204:
                return R.drawable.weathericon_200;

            case 205:
            case 206:
            case 207:
                return R.drawable.weathericon_205;

            case 208:
            case 209:
            case 210:
            case 211:
            case 212:
            case 213:
                return R.drawable.weathericon_208;

            case 300:
                return R.drawable.weathericon_300;

            case 301:
                return R.drawable.weathericon_301;

            case 302:
                return R.drawable.weathericon_302;

            case 303:
                return R.drawable.weathericon_303;

            case 304:
                return R.drawable.weathericon_304;

            case 305:
                return R.drawable.weathericon_305;

            case 306:
                return R.drawable.weathericon_306;

            case 307:
                return R.drawable.weathericon_307;

            case 308:
                return R.drawable.weathericon_308;

            case 309:
                return R.drawable.weathericon_309;

            case 310:
                return R.drawable.weathericon_310;

            case 311:
                return R.drawable.weathericon_311;

            case 312:
                return R.drawable.weathericon_312;

            case 313:
                return R.drawable.weathericon_313;

            case 400:
                return R.drawable.weathericon_400;

            case 401:
                return R.drawable.weathericon_401;

            case 402:
                return R.drawable.weathericon_402;

            case 403:
                return R.drawable.weathericon_403;

            case 404:
                return R.drawable.weathericon_404;

            case 405:
                return R.drawable.weathericon_405;

            case 406:
                return R.drawable.weathericon_406;

            case 407:
                return R.drawable.weathericon_407;

            case 500:
                return R.drawable.weathericon_500;

            case 501:
                return R.drawable.weathericon_501;

            case 502:
                return R.drawable.weathericon_502;

            case 503:
                return R.drawable.weathericon_503;

            case 504:
                return R.drawable.weathericon_504;

            case 506:
                return R.drawable.weathericon_999;

            case 507:
                return R.drawable.weathericon_507;

            case 508:
                return R.drawable.weathericon_508;

            case 900:
                return R.drawable.weathericon_900;

            case 901:
                return R.drawable.weathericon_901;

            case 999:
                return R.drawable.weathericon_999;

            default:
                return R.drawable.weathericon_999;
        }

    }
}
