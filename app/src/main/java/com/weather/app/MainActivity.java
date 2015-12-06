package com.weather.app;

import java.io.File;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.weather.utils.WebAccessTools;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.view.GravityCompat;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.support.v4.widget.DrawerLayout;

public class MainActivity extends Activity {
	//记录壁纸的文件
	public static final String WALLPAPER_FILE="wallpaper_file";
	//缓存天气的文件
	public static final String STORE_WEATHER="store_weather";

	//当前Activity的根布局
	private DrawerLayout rootLayout;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //设置窗口特征,为不显示标题
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.main);

        rootLayout = (DrawerLayout)findViewById(R.id.drawer_layout);

		//侧滑边栏
		ListView list_drawer = (ListView)findViewById(R.id.left_drawer);
		String[] mArr = {"设置城市","刷新","更换壁纸"};
		ArrayAdapter<String> drawerAdapter = new ArrayAdapter<String>(this,R.layout.drawer_item,mArr);
		list_drawer.setAdapter(drawerAdapter);
		list_drawer.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				switch (position) {
					case 0:
						//跳转到设置城市的Activity
						Intent intent = new Intent(MainActivity.this, SetCityActivity.class);
						startActivityForResult(intent, 0);
						rootLayout.closeDrawers();
						break;
					case 1:
						rootLayout.closeDrawers();
						//得到设置的城市码
						SharedPreferences sp = getSharedPreferences(SetCityActivity.CITY_CODE_FILE, MODE_PRIVATE);
						String cityCode = sp.getString("code", "");
						if (cityCode != null && cityCode.trim().length() != 0) {
							setWeatherSituation(cityCode);
						}
						break;
					case 2:
						changeWallpaper();
						break;
				}
			}
		});
		Button btn = (Button)findViewById(R.id.sidebar);
		btn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				rootLayout.openDrawer(GravityCompat.START);
			}
		});


        //通过检查程序中的缓存文件判断程序是否是第一次运行
        String dirPath= "/data/data/com.weather.app/shared_prefs/";
        File file= new File(dirPath);
        boolean isFirstRun = false;
        //如果文件不存在说明是第一次运行
        if(!file.exists()) {
        	//设置默认的壁纸
        	SharedPreferences.Editor editor = getSharedPreferences(WALLPAPER_FILE, MODE_PRIVATE).edit();
        	editor.putInt("wallpaper", R.color.bg6);
        	editor.commit();
        	isFirstRun = true;

        } else {
        	//设置壁纸为文件中保存的
        	SharedPreferences sp= getSharedPreferences(WALLPAPER_FILE, MODE_PRIVATE);
        	rootLayout.setBackgroundResource(sp.getInt("wallpaper",R.drawable.app_bg02));
        }

        //得到保存的城市天气
        SharedPreferences sp = getSharedPreferences(SetCityActivity.CITY_CODE_FILE, MODE_PRIVATE);
    	String cityCode= sp.getString("code", "");
    	if( cityCode!= null && cityCode.trim().length()!=0) {
    		SharedPreferences shared = getSharedPreferences(STORE_WEATHER, MODE_PRIVATE);
    		long currentTime = System.currentTimeMillis();
    		//得到天气缓冲文件中的有效期
    		long vaildTime = shared.getLong("validTime", currentTime);
    		//比较天气缓存文件中的有效期，如果超时了，则访问网络更新天气
    		if(vaildTime > currentTime)
        	   setWeatherSituation(shared);
    		else
    		   setWeatherSituation(cityCode);
        } else {
        	//跳转到设置城市的Activity
    		Intent intent = new Intent(MainActivity.this, SetCityActivity.class);
    		intent.putExtra("isFirstRun", isFirstRun);
    		startActivityForResult(intent, 0);
        }

    }

    @Override //得到设置页面的回退
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	//得到城市的编码
    	SharedPreferences sp = getSharedPreferences(SetCityActivity.CITY_CODE_FILE, MODE_PRIVATE);
		String cityCode = sp.getString("code", "");
    	if(cityCode!=null&&cityCode.trim().length()!=0) {
    		if(data!=null&&data.getBooleanExtra("updateWeather", false)) {
    			//从网上更新新的天气
    			setWeatherSituation(cityCode);
    		} else {
    			//读取缓存文件中的天气
    			SharedPreferences shared = getSharedPreferences(STORE_WEATHER, MODE_PRIVATE);
    			setWeatherSituation(shared);
    		}
    	} else {
    		//如果是没有城市码的回退，则退出程序
    		MainActivity.this.finish();
    	}
    }


    //由城市码设置天气情况,并将得到的信息保存在文件中
    public void setWeatherSituation(String cityCode) {
		ProgressDialog pd = ProgressDialog.show(this, "", "天气更新中……", false, true);

      try {
		  	String info=new WebAccessTools(this).getWeatherInfo(cityCode);
    	    //==========================解析JSON得到天气===========================
			JSONObject json=new JSONObject(info).getJSONArray("HeWeather data service 3.0").getJSONObject(0);
		  	JSONObject jsonBasic = json.getJSONObject("basic");
		  	JSONArray jsonDailyForecastArr = json.getJSONArray("daily_forecast");
		  	JSONObject jsonWeatherNow = json.getJSONObject("now");
		  	JSONObject jsonSuggestion = json.getJSONObject("suggestion");


			//建立一个缓存天气的文件
			SharedPreferences.Editor editor = getSharedPreferences(STORE_WEATHER, MODE_PRIVATE).edit();

			//basic：城市、日期
			//得到城市
			info=jsonBasic.getString("city");
		  	showStringWeatherInfo(info,R.id.cityField,"city",editor);
			//得到更新日期时间
			info= jsonBasic.getJSONObject("update").getString("loc");
		  	showStringWeatherInfo(info, R.id.date_y, "date_now", editor);


		  //now：今天天气信息
			//得到此刻温度
			info=toCelsiusFormat(jsonWeatherNow.getString("tmp"));
		  	Log.i("---now tmp---",info);
		  	showStringWeatherInfo(info,R.id.currentTemp,"temp_now",editor);

		  	//得到温度范围
		  	JSONObject tmpRange=jsonDailyForecastArr.getJSONObject(0).getJSONObject("tmp");
		  	info=toCelsiusFormat(tmpRange.getString("max"))+"~"+toCelsiusFormat(tmpRange.getString("min"));
		  	Log.i("---tmp range---",info);
		  	showStringWeatherInfo(info,R.id.tempRange,"tempRange",editor);

			//得到天气
			JSONObject jsonCond= jsonWeatherNow.getJSONObject("cond");
		  	info=jsonCond.getString("txt");
		  	showStringWeatherInfo(info,R.id.currentWeather,"weather1",editor);

			//天气图标
			info= jsonCond.getString("code");
		  	showImageWeatherIcon(info,R.id.weather_icon00,"img_title1",editor);

			//得到风向
			info= jsonWeatherNow.getJSONObject("wind").getString("dir") + " "
					+ jsonWeatherNow.getJSONObject("wind").getString("sc") + "级";
		  	showStringWeatherInfo(info, R.id.currentWind, "wind1", editor);

		  //suggestion:建议
		  	//舒适度
		  	JSONObject jsonSugObj=jsonSuggestion.getJSONObject("comf");
		  	info=jsonSugObj.getString("brf");
		  	showStringWeatherInfo(info,R.id.comf_brf,"comf_brf",editor);
		  	info=jsonSugObj.getString("txt");
		  	showStringWeatherInfo(info,R.id.comf_txt,"comf_txt",editor);

		  	//洗车
		  	jsonSugObj=jsonSuggestion.getJSONObject("cw");
		  	info=jsonSugObj.getString("brf");
		  	showStringWeatherInfo(info,R.id.cw_brf,"cw_brf",editor);
		  	info=jsonSugObj.getString("txt");
		  	showStringWeatherInfo(info,R.id.cw_txt,"cw_txt",editor);

		  	//穿衣
		  	jsonSugObj=jsonSuggestion.getJSONObject("drsg");
		  	info=jsonSugObj.getString("brf");
		  	showStringWeatherInfo(info,R.id.drsg_brf,"drsg_brf",editor);
		  	info=jsonSugObj.getString("txt");
		  	showStringWeatherInfo(info,R.id.drsg_txt,"drsg_txt",editor);

		  	//感冒
		  	jsonSugObj=jsonSuggestion.getJSONObject("flu");
		  	info=jsonSugObj.getString("brf");
		  	showStringWeatherInfo(info,R.id.flu_brf,"flu_brf",editor);
		  	info=jsonSugObj.getString("txt");
		  	showStringWeatherInfo(info,R.id.flu_txt,"flu_txt",editor);

		  	//运动
		  	jsonSugObj=jsonSuggestion.getJSONObject("sport");
		  	info=jsonSugObj.getString("brf");
		  	showStringWeatherInfo(info,R.id.sport_brf,"sport_brf",editor);
		  	info=jsonSugObj.getString("txt");
		  	showStringWeatherInfo(info,R.id.sport_txt,"sport_txt",editor);

		  	//旅游
		  	jsonSugObj=jsonSuggestion.getJSONObject("trav");
		  	info=jsonSugObj.getString("brf");
		  	showStringWeatherInfo(info,R.id.trav_brf,"trav_brf",editor);
		  	info=jsonSugObj.getString("txt");
		  	showStringWeatherInfo(info,R.id.trav_txt,"trav_txt",editor);

		  	//紫外线
		  	jsonSugObj=jsonSuggestion.getJSONObject("uv");
		  	info=jsonSugObj.getString("brf");
		  	showStringWeatherInfo(info,R.id.uv_brf,"uv_brf",editor);
		  	info=jsonSugObj.getString("txt");
		  	showStringWeatherInfo(info,R.id.uv_txt,"uv_txt",editor);


			//7天天气预报:0今天。1明天。2后天。3……6
		  	JSONObject jsonDay;
		  	for (int i = 1;i < 7;i++) {
				jsonDay=jsonDailyForecastArr.getJSONObject(i);
				showWeatherInfomationForDay(jsonDay,i,editor);
			}


			//设置一个有效日期为5小时
			long validTime = System.currentTimeMillis();
			validTime = validTime + 5*60*60*1000;
			editor.putLong("validTime", validTime);

			//保存
			editor.commit();

		} catch (JSONException e) {
			e.printStackTrace();
		} finally {
		  pd.dismiss();
	  }
	}

    //根据已定的缓存文件来得到天气情况
    public void setWeatherSituation(SharedPreferences shared) {
    	String info = null;
    	//城市
		info= shared.getString("city", "");
		showStringWeatherInfo(info,R.id.cityField);
		//日期
		info= shared.getString("date_now", "");
		showStringWeatherInfo(info, R.id.date_y);
		//温度
		info= shared.getString("temp_now", "");
		showStringWeatherInfo(info, R.id.currentTemp);
		//温度范围
		info= shared.getString("tempRange","");
		showStringWeatherInfo(info, R.id.tempRange);
		//得到天气
		info= shared.getString("weather1", "");
		showStringWeatherInfo(info,R.id.currentWeather);
		//天气图标
		showImageWeatherIcon(shared.getInt("img_title1", 0), R.id.weather_icon01);
		//得到风向
		info= shared.getString("wind1", "");
		showStringWeatherInfo(info, R.id.currentWind);

		//建议
		info= shared.getString("comf_brf","");
		showStringWeatherInfo(info, R.id.comf_brf);
		info= shared.getString("comf_txt","");
		showStringWeatherInfo(info,R.id.comf_txt);
		info= shared.getString("cw_brf","");
		showStringWeatherInfo(info, R.id.cw_brf);
		info= shared.getString("cw_txt","");
		showStringWeatherInfo(info,R.id.cw_txt);
		info= shared.getString("drsg_brf","");
		showStringWeatherInfo(info, R.id.drsg_brf);
		info= shared.getString("drsg_txt","");
		showStringWeatherInfo(info,R.id.drsg_txt);
		info= shared.getString("flu_brf","");
		showStringWeatherInfo(info, R.id.flu_brf);
		info= shared.getString("flu_txt","");
		showStringWeatherInfo(info,R.id.flu_txt);
		info= shared.getString("sport_brf","");
		showStringWeatherInfo(info, R.id.sport_brf);
		info= shared.getString("sport_txt","");
		showStringWeatherInfo(info,R.id.sport_txt);
		info= shared.getString("trav_brf","");
		showStringWeatherInfo(info, R.id.trav_brf);
		info= shared.getString("trav_txt","");
		showStringWeatherInfo(info,R.id.trav_txt);
		info= shared.getString("uv_brf","");
		showStringWeatherInfo(info, R.id.uv_brf);
		info= shared.getString("uv_txt","");
		showStringWeatherInfo(info, R.id.uv_txt);

		//7天天气预报
		showStringWeatherInfo(shared.getString("weather01", ""), R.id.weather01);//天气
		showImageWeatherIcon(shared.getInt("weather_icon01", 0), R.id.weather_icon01);//天气图标
		showStringWeatherInfo(shared.getString("temp01", ""), R.id.temp01);//气温
		showStringWeatherInfo(shared.getString("wind01", ""), R.id.wind01);//风

		showStringWeatherInfo(shared.getString("weather02", ""), R.id.weather02);//天气
		showImageWeatherIcon(shared.getInt("weather_icon02", 0), R.id.weather_icon02);//天气图标
		showStringWeatherInfo(shared.getString("temp02", ""), R.id.temp02);//气温
		showStringWeatherInfo(shared.getString("wind02", ""), R.id.wind02);//风

		showStringWeatherInfo(shared.getString("day03",""),R.id.day03);//日期
		showStringWeatherInfo(shared.getString("weather03", ""), R.id.weather03);//天气
		showImageWeatherIcon(shared.getInt("weather_icon03", 0), R.id.weather_icon03);//天气图标
		showStringWeatherInfo(shared.getString("temp03", ""), R.id.temp03);//气温
		showStringWeatherInfo(shared.getString("wind03",""),R.id.wind03);//风

		showStringWeatherInfo(shared.getString("day04",""),R.id.day04);//日期
		showStringWeatherInfo(shared.getString("weather04", ""), R.id.weather04);//天气
		showImageWeatherIcon(shared.getInt("weather_icon04", 0), R.id.weather_icon04);//天气图标
		showStringWeatherInfo(shared.getString("temp04", ""), R.id.temp04);//气温
		showStringWeatherInfo(shared.getString("wind04",""),R.id.wind04);//风

		showStringWeatherInfo(shared.getString("day05",""),R.id.day05);//日期
		showStringWeatherInfo(shared.getString("weather05", ""), R.id.weather05);//天气
		showImageWeatherIcon(shared.getInt("weather_icon05", 0), R.id.weather_icon05);//天气图标
		showStringWeatherInfo(shared.getString("temp05", ""), R.id.temp05);//气温
		showStringWeatherInfo(shared.getString("wind05",""),R.id.wind05);//风

		showStringWeatherInfo(shared.getString("day06",""),R.id.day06);//日期
		showStringWeatherInfo(shared.getString("weather06", ""), R.id.weather06);//天气
		showImageWeatherIcon(shared.getInt("weather_icon06",0),R.id.weather_icon06);//天气图标
		showStringWeatherInfo(shared.getString("temp06",""),R.id.temp06);//气温
		showStringWeatherInfo(shared.getString("wind06",""),R.id.wind06);//风

    }

    //由天气情况得到图标
    public static int getWeatherIconByCode(String weatherCode) {
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

	//给温度加上摄氏度符号
	private String toCelsiusFormat(String tmp) {
		return tmp + "℃";
	}

	//显示某天的天气
	private void showWeatherInfomationForDay(JSONObject json,int index,SharedPreferences.Editor editor) throws JSONException {
		String info = "";
		String date=json.getString("date");//日期
		JSONObject jsonCond = json.getJSONObject("cond");//天气
		JSONObject jsonTmp = json.getJSONObject("tmp");//温度
		JSONObject jsonWind = json.getJSONObject("wind");//风
		int viewId=0;
		
		
		switch (index) {
			case 1:
				viewId=R.id.weather01;
				info=jsonCond.getString("txt_d");
				showStringWeatherInfo(info,viewId,"weather01",editor);//天气
				viewId=R.id.weather_icon01;
				info=jsonCond.getString("code_d");
				showImageWeatherIcon(info,viewId,"weather_icon01",editor);//天气图标
				viewId=R.id.temp01;
				info= toCelsiusFormat(jsonTmp.getString("max")) + "~"
						+ toCelsiusFormat(jsonTmp.getString("min"));
				showStringWeatherInfo(info,viewId,"temp01",editor);//气温
				viewId=R.id.wind01;
				info=jsonWind.getString("dir")+" "+jsonWind.getString("sc")+"级";
				showStringWeatherInfo(info,viewId,"wind01",editor);//风
				break;
			case 2:
				viewId=R.id.weather02;
				info=jsonCond.getString("txt_d");
				showStringWeatherInfo(info,viewId,"weather02",editor);//天气
				viewId=R.id.weather_icon02;
				info=jsonCond.getString("code_d");
				showImageWeatherIcon(info,viewId,"weather_icon02",editor);//天气图标
				viewId=R.id.temp02;
				info= toCelsiusFormat(jsonTmp.getString("max")) + "~"
						+ toCelsiusFormat(jsonTmp.getString("min"));
				showStringWeatherInfo(info,viewId,"temp02",editor);//气温
				viewId=R.id.wind02;
				info=jsonWind.getString("dir")+" "+jsonWind.getString("sc")+"级";
				showStringWeatherInfo(info,viewId,"wind02",editor);//风
				break;
			case 3:
				viewId=R.id.day03;
				showStringWeatherInfo(date,viewId,"day03",editor);//日期	
				viewId=R.id.weather03;
				info=jsonCond.getString("txt_d");
				showStringWeatherInfo(info,viewId,"weather03",editor);//天气
				viewId=R.id.weather_icon03;
				info=jsonCond.getString("code_d");
				showImageWeatherIcon(info,viewId,"weather_icon03",editor);//天气图标
				viewId=R.id.temp03;
				info= toCelsiusFormat(jsonTmp.getString("max")) + "~"
						+ toCelsiusFormat(jsonTmp.getString("min"));
				showStringWeatherInfo(info,viewId,"temp03",editor);//气温
				viewId=R.id.wind03;
				info=jsonWind.getString("dir")+" "+jsonWind.getString("sc")+"级";
				showStringWeatherInfo(info,viewId,"wind03",editor);//风
				break;
			case 4:
				viewId=R.id.day04;
				showStringWeatherInfo(date,viewId,"day04",editor);//日期	
				viewId=R.id.weather04;
				info=jsonCond.getString("txt_d");
				showStringWeatherInfo(info,viewId,"weather04",editor);//天气
				viewId=R.id.weather_icon04;
				info=jsonCond.getString("code_d");
				showImageWeatherIcon(info,viewId,"weather_icon04",editor);//天气图标
				viewId=R.id.temp04;
				info= toCelsiusFormat(jsonTmp.getString("max")) + "~"
						+ toCelsiusFormat(jsonTmp.getString("min"));
				showStringWeatherInfo(info,viewId,"temp04",editor);//气温
				viewId=R.id.wind04;
				info=jsonWind.getString("dir")+" "+jsonWind.getString("sc")+"级";
				showStringWeatherInfo(info,viewId,"wind04",editor);//风
				break;
			case 5:
				viewId=R.id.day05;
				showStringWeatherInfo(date,viewId,"day05",editor);//日期	
				viewId=R.id.weather05;
				info=jsonCond.getString("txt_d");
				showStringWeatherInfo(info,viewId,"weather05",editor);//天气
				viewId=R.id.weather_icon05;
				info=jsonCond.getString("code_d");
				showImageWeatherIcon(info,viewId,"weather_icon05",editor);//天气图标
				viewId=R.id.temp05;
				info= toCelsiusFormat(jsonTmp.getString("max")) + "~"
						+ toCelsiusFormat(jsonTmp.getString("min"));
				showStringWeatherInfo(info,viewId,"temp05",editor);//气温
				viewId=R.id.wind05;
				info=jsonWind.getString("dir")+" "+jsonWind.getString("sc")+"级";
				showStringWeatherInfo(info,viewId,"wind05",editor);//风
				break;
			case 6:
				viewId=R.id.day06;
				showStringWeatherInfo(date,viewId,"day06",editor);//日期	
				viewId=R.id.weather06;
				info=jsonCond.getString("txt_d");
				showStringWeatherInfo(info,viewId,"weather06",editor);//天气
				viewId=R.id.weather_icon06;
				info=jsonCond.getString("code_d");
				showImageWeatherIcon(info,viewId,"weather_icon06",editor);//天气图标
				viewId=R.id.temp06;
				info= toCelsiusFormat(jsonTmp.getString("max")) + "~"
						+ toCelsiusFormat(jsonTmp.getString("min"));
				showStringWeatherInfo(info,viewId,"temp06",editor);//气温
				viewId=R.id.wind06;
				info=jsonWind.getString("dir")+" "+jsonWind.getString("sc")+"级";
				showStringWeatherInfo(info,viewId,"wind06",editor);//风
				break;
			default:
				break;
		}

	}
	//显示此时文本信息到屏幕上
	private void showStringWeatherInfo(String info,int viewId,String spKey,SharedPreferences.Editor editor) {
		TextView tempText = (TextView)findViewById(viewId);
		tempText.setText(info);
		editor.putString(spKey, info);
	}
	private void showStringWeatherInfo(String info, int viewId) {
		TextView tempText = (TextView)findViewById(viewId);
		tempText.setText(info);
	}
	//显示天气图标
	private void showImageWeatherIcon(String code,int viewId,String spKey,SharedPreferences.Editor editor) {
		ImageView imageView = (ImageView)findViewById(viewId);
		int weatherIcon=getWeatherIconByCode(code);
		imageView.setImageResource(weatherIcon);
		editor.putInt(spKey, weatherIcon);
	}
	private void showImageWeatherIcon(int code,int viewId) {
		ImageView imageView = (ImageView)findViewById(viewId);
		imageView.setImageResource(code);
	}
	//更换壁纸
	private void changeWallpaper() {
		//壁纸
		String[] items = {"绿色小路","夕阳晚霞","深色海洋","魔幻古堡","青","蓝","绿","红","橙","灰","黑"};
		final int[] bgResIds = {R.drawable.app_bg01,R.drawable.app_bg02,R.drawable.app_bg03,R.drawable.app_bg04,R.color.bg1,R.color.bg2
				,R.color.bg3,R.color.bg4,R.color.bg5,R.color.bg6,R.color.bg7};
		//已选壁纸
		SharedPreferences sp = getSharedPreferences(WALLPAPER_FILE, MODE_PRIVATE);
		int oldBgId = sp.getInt("wallpaper", 8);
		for (int i = 0; i < 11 ;i++) {
			if (oldBgId == bgResIds[i]) oldBgId=i;
		}
		//得到SharedPreferences操作对象更改壁纸
		final SharedPreferences.Editor editor = getSharedPreferences(WALLPAPER_FILE, MODE_PRIVATE).edit();

		AlertDialog.Builder builder = new AlertDialog.Builder(this)
				.setTitle("选择壁纸")
				.setIcon(R.drawable.menu_diy)
				.setSingleChoiceItems(items, oldBgId , new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						int bgId = 0;//壁纸资源id
						switch (which) {
							case 0:
								bgId=bgResIds[0];
								break;
							case 1:
								bgId=bgResIds[1];
								break;
							case 2:
								bgId=bgResIds[2];
								break;
							case 3:
								bgId=bgResIds[3];
								break;
							case 4:
								bgId=bgResIds[4];
								break;
							case 5:
								bgId=bgResIds[5];
								break;
							case 6:
								bgId=bgResIds[6];
								break;
							case 7:
								bgId=bgResIds[7];
								break;
							case 8:
								bgId=bgResIds[8];
								break;
							case 9:
								bgId=bgResIds[9];
								break;
							case 10:
								bgId=bgResIds[10];
								break;
							default:
								break;
						}
						rootLayout.setBackgroundResource(bgId);
						editor.putInt("wallpaper", bgId);
						editor.commit();
					}
				});
		builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {}
		});
		builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {}
		});
		builder.create().show();
	}
}