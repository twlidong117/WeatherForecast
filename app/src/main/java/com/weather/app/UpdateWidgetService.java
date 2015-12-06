package com.weather.app;

import java.util.Calendar;
import java.util.GregorianCalendar;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.util.Log;
import android.widget.RemoteViews;

/**
 *
 * 创建一个用于更新天气widget小组件的后台服务
 */
public class UpdateWidgetService extends Service {

	//记录定时管理器
	AlarmManager alarm;
	PendingIntent pintent;
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override //开始服务，执生更新widget组件的操作
	public int onStartCommand(Intent intent,int flags ,int startId) {
		super.onStartCommand(intent, flags, startId);
		
		Log.i("widget Service", "===================update  widget===========================");
		//得到widget的布局对象
		RemoteViews views = WeatherWidget.getWeatherView(this);
		//得到AppWidgetManager widget管理器
		AppWidgetManager appWidgetManager=AppWidgetManager.getInstance(this);
		
		int[] appids=appWidgetManager.getAppWidgetIds(new ComponentName(this, WeatherWidget.class));
		
		//======================================================
		//得到城市码
		SharedPreferences sp=getSharedPreferences(SetCityActivity.CITY_CODE_FILE, SetCityActivity.MODE_PRIVATE);
		String cityCode= sp.getString("code", "");
		if(cityCode!=null&&cityCode.trim().length() > 0) {
			Log.i("widget", "===================update  weather===========================");
			WeatherWidget.updateAppWidget(views, this, appWidgetManager, cityCode);
		}
		//======================================================
		
		
		appWidgetManager.updateAppWidget(appids, views);
		
		//获取当前时间
		GregorianCalendar calendar = new GregorianCalendar();
		long now = calendar.get(Calendar.MILLISECOND);
		long unit=240000;//间隔一分钟
		
		pintent=PendingIntent.getService(this, 0, intent, 0);
		
		//计时器
		alarm=(AlarmManager)getSystemService(Context.ALARM_SERVICE);
		//AlarmManager.RTC_WAKEUP设置服务在系统休眠时同样会运行
		//第二个参数是下一次启动service时间
		alarm.set(AlarmManager.RTC_WAKEUP, now+unit, pintent);

		return START_REDELIVER_INTENT;
	}

	@Override //当widget中通过调用context.stopService方法来指定销毁service时，被调用
	public void onDestroy() {
		
		//取消定时管理
		if(alarm!=null) {
			alarm.cancel(pintent);
		}
		super.onDestroy();
	}
}
