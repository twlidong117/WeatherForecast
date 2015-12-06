package com.weather.app;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import com.weather.comp.GPSListAdapter;
import com.weather.comp.MyListAdapter;
import com.weather.dao.DBHelper;
import com.weather.utils.LocationXMLParser;
import com.weather.utils.WeaterInfoParser;
import com.weather.utils.WebAccessTools;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.appwidget.AppWidgetManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ListView;
import android.widget.RemoteViews;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ExpandableListView.OnChildClickListener;

/**
 * 设置城市的Activity
 */
public class SetCityActivity extends Activity {
	//定义的一个自动定位的列表
	private ListView gpsView;
	//定义的一个省份可伸缩性的列表
	private ExpandableListView provinceList;
	//定义的用于过滤的文本输入框
	private TextView filterText;
	
	//定义的一个记录城市码的SharedPreferences文件名
	public static final String CITY_CODE_FILE="city_code";
	
	//城市的编码
	private String[][] cityCodes;
	//省份
	private String[] groups;
	//对应的城市
    private String[][] childs;
    
    //自定义的伸缩列表适配器
    private MyListAdapter adapter;
    
    //记录应用程序widget的ID
    private int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

	//记录定位结果
	Location location = null;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.set_city);
        
        gpsView = (ListView)findViewById(R.id.gps_view);
        provinceList= (ExpandableListView)findViewById(R.id.provinceList);
        
        //设置自动定位的适配器
        gpsView.setAdapter(new GPSListAdapter(SetCityActivity.this));
        
        //==============================GPS=================================
        //当单击自动定位时
        gpsView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				TextView localeCity = (TextView)view.findViewById(R.id.locateCityText);
				localeCity.setText("正在定位...");
				
				final LocateHandler handler = new LocateHandler(localeCity);
				//添加一个线程来处理定位
				new Thread(){
					public void run() {
						Map<Integer, String> cityMap = getLocationCityInfo();
						//记录匹配的城市的索引
						int provinceIndex = -1;
						int cityIndex = -1;
						//传给处理类的数据封装对象
						Bundle bundle = new Bundle();
						if(cityMap!=null) {
							//得到图家名
							String country = cityMap.get(1);
							//只匹配中国地区的天气
							if(country!=null&&country.equals("中国")){
								//得到省、直辖市、自治区
								String province = cityMap.get(2);
								//得到地级市
								String city = cityMap.get(3);
								//得到区县
								String towns = cityMap.get(4);
								
								Log.i("GPS", "============"+province+"."+city+"."+towns+"==============");
								//将GPS定位的城市与提供能查天气的城市进行匹配
								StringBuilder matchCity = new StringBuilder(city);
								matchCity.append(".");
								matchCity.append(towns);
								//找到省份
								for(int i=0; i<groups.length; i++) {
									if(groups[i].equals(province)) {
										provinceIndex = i;
										break;
									}
								}
								//先从区县开始查找匹配的地区
								for(int j=0; j<childs[provinceIndex].length; j++) {
									if(childs[provinceIndex][j].equals(matchCity.toString())) {
										cityIndex = j;
										break;
									}
								}
								//如果未匹配成功,则换为从城市中查找
								if(cityIndex == -1) {
									for(int j=0; j<childs[provinceIndex].length; j++) {
										if(childs[provinceIndex][j].equals(city)) {
											cityIndex = j;
											//匹配成功，则退出循环
											break;
										}
									}
								}
							}
						}
						//将其用bundle封装，用于传给Handler
						bundle.putInt("provinceIndex", provinceIndex);
						bundle.putInt("cityIndex", cityIndex);
						
						Message msg = new Message();
						msg.setData(bundle);
						//正式交由handler处理
						handler.sendMessage(msg);
					}
				}.start();
			}
        	
        });
        
        //为过滤输入文本框添加事件
        filterText = (TextView) findViewById(R.id.filterField);
        filterText.addTextChangedListener(new TextWatcher() {

			@Override
			public void afterTextChanged(Editable s) {
				CharSequence filterContent = filterText.getText();
				//设置列表数据过滤结果显示
				adapter.getFilter().filter(filterContent);
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
			}
        	
        });
        
        //得到MainActivity或Widget传过来的intent
        Intent intent =getIntent();
        //通过判断MainActivity传过来的isFirstRun来确定是否为第一次运行
        boolean isFirstRun = intent.getBooleanExtra("isFirstRun", false);
        
        //通过接收Bundle来判断Widget中传递过来的WidgetId
        Bundle extras = intent.getExtras();
        if (extras != null) {
            mAppWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, 
            		AppWidgetManager.INVALID_APPWIDGET_ID);
            //如果WidgetId有效
            if(mAppWidgetId!=AppWidgetManager.INVALID_APPWIDGET_ID) {
            	//判断它是否是第一次运行
            	SharedPreferences sp=getSharedPreferences(CITY_CODE_FILE, MODE_PRIVATE);
            	if(sp.getString("code", null)==null) {
            		//如果不存在城市码，则说明为第一次运行
            		isFirstRun = true;
            	} else {   		
            		//如存在则直接跳回
            		AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(SetCityActivity.this);
    				RemoteViews views = new RemoteViews(SetCityActivity.this.getPackageName(),
    						R.layout.widget_layout);
    				//得到城市码
    				String cityCode= sp.getString("code", "");
    				if(cityCode!=null&&cityCode.trim().length() > 0) {
    					Log.i("widget", "===================update  weather===========================");
    					//更新widget
    					WeatherWidget.updateAppWidget(views, SetCityActivity.this, appWidgetManager, cityCode);
    				}
    				
    				appWidgetManager.updateAppWidget(mAppWidgetId, views);
    				//设置成功，返回
    				Intent resultValue = new Intent();
    				resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
    				setResult(RESULT_OK, resultValue);
    				//结束当前的Activity
    				finish();
    				return;
            	}
            }
        }
        
        //如果为true说明是第一次运行
        if(isFirstRun) {
        	//导入城市编码数据库
        	importInitDatabase();
        	
        	//显示一个对话框说明为第一次运行
        	AlertDialog.Builder builder = new AlertDialog.Builder(this);
        	builder.setMessage("由于本程序是第一次运行，请选择您需要了解天气的城市").setPositiveButton("确定", null);
        	AlertDialog dialog = builder.create();
        	dialog.show();
        }
        
        //增强用户体验，在加载城市列表时显示进度对话框
        final ProgressDialog dialog = getProgressDialog("", "正在加载城市列表...");
        dialog.show();
        //伸缩性列表的加载处理类
        final MyHandler mHandler = new MyHandler();
        new Thread(new Runnable() {
        	public void run() {
        		//查询处理数据库,装载伸展列表
                DBHelper dbHelper = new DBHelper(SetCityActivity.this, "db_weather.db");
                groups = dbHelper.getAllProvinces();
                List<String[][]> result = dbHelper.getAllCityAndCode(groups);
                childs = result.get(0);
                cityCodes = result.get(1);
                //交给Handler对象加载列表
                Message msg = new Message();
                mHandler.sendMessage(msg);
                dialog.cancel();
                dialog.dismiss();
        	}
        }).start();
    }

	//将res/raw中的城市数据库导入到安装的程序中的database目录下
    public void importInitDatabase() {
    	//数据库的目录
    	String dirPath="/data/data/com.weather.app/databases";
    	File dir = new File(dirPath);
    	if(!dir.exists()) {
    		dir.mkdir();
    	}
    	//数据库文件
    	File dbfile = new File(dir, "db_weather.db");
    	try {
    		if(!dbfile.exists()) {
    			dbfile.createNewFile();
    		}
    		//加载欲导入的数据库
    		InputStream is = this.getApplicationContext().getResources().openRawResource(R.raw.db_weather);
    		FileOutputStream fos = new FileOutputStream(dbfile);
    		byte[] buffere=new byte[is.available()];
    		is.read(buffere);
    		fos.write(buffere);
    		is.close();
    		fos.close();

    	}catch(FileNotFoundException  e){
    		e.printStackTrace();
    	}catch(IOException e) {
    		e.printStackTrace();
    	}
    }
    
    //得到一个进度对话框
    public ProgressDialog getProgressDialog(String title, String content) {
    	//实例化进度条对话框ProgressDialog
    	ProgressDialog dialog=new ProgressDialog(this);
    	
    	//可以不显示标题
    	dialog.setTitle(title);
    	dialog.setIndeterminate(true);
    	dialog.setMessage(content);
    	dialog.setCancelable(true);
    	return dialog;
    }
    
    //利用GPS功能得到当前位置的城市名
    public synchronized Map<Integer, String> getLocationCityInfo() {

		Map<Integer, String> locationCityInfoMap = new HashMap<Integer, String>();
    	LocationManager locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);//success

    	//设置一个Criteria标准用于过滤LocationProvider
    	Criteria criteria = new Criteria();
    	//设置不需要高度信息
    	criteria.setAltitudeRequired(false);
    	//设置不需要方位信息
    	criteria.setBearingRequired(false);

    	//根据criteria和当前设备状态选择的最佳provider（但不能保证一次定位成功，特别是gps）
		String provider = locationManager.getBestProvider(criteria, true);
		//备选provider列表
		String[] providersArr = {LocationManager.GPS_PROVIDER,LocationManager.NETWORK_PROVIDER,LocationManager.PASSIVE_PROVIDER};

		for (int i = 0;;i++) {
			location = locationManager.getLastKnownLocation(provider);
			provider = providersArr[i];
			if (location!=null) break;
		}

		Log.i("LocationInfo",location+"");
    	if(location!=null) {
    		double latitude = location.getLatitude();  //得到经度
        	double longitude = location.getLongitude(); //得到纬度
        	//根据经纬度得到详细的地址信息
        	//定义的一个网络访问工具类
            WebAccessTools webTools = new WebAccessTools(this);
        	String addressContext = webTools.getWebContent("http://maps.google.cn/maps/api/geocode/json?latlng="+latitude+","+longitude);

        	//解析地址信息;得到Map<Integer,String>
			try {
				JSONObject jsonObject = new JSONObject(addressContext);
				JSONArray resultsList = jsonObject.getJSONArray("results");
				JSONObject firstResult = resultsList.getJSONObject(0);
				JSONArray addressComponentList = firstResult.getJSONArray("address_components");
				for (int i = 0; i < addressComponentList.length(); i++) {
					JSONObject addComItem = addressComponentList.getJSONObject(i);
					JSONArray types = addComItem.getJSONArray("types");
					String typeStr = types.getString(0);

					if (typeStr.equals("sublocality_level_1")) {
						//县区 == 4
						String tmpStr = addComItem.getString("long_name");
						tmpStr = tmpStr.substring(0,tmpStr.length()-1);
						locationCityInfoMap.put(4,tmpStr);
					} else if (typeStr.equals("locality")) {
						//地级市 == 3
						locationCityInfoMap.put(3,addComItem.getString("long_name"));
					} else if (typeStr.equals("administrative_area_level_1")) {
						//省、直辖市、自治区 == 2
						locationCityInfoMap.put(2,addComItem.getString("long_name"));
					} else if (typeStr.equals("country")) {
						//国家 == 1
						locationCityInfoMap.put(1,addComItem.getString("long_name"));
					}
				}
				return locationCityInfoMap;

			} catch (JSONException e) {
				e.printStackTrace();
			}

    	}
    	
    	return null;
    }
    
    public void tempMethod() {
    	//==================================for test==========================================
    	WebAccessTools webTools = new WebAccessTools(this);
        //得到访问网络的内容
        String webContent=webTools.getWebContent("http://m.weather.com.cn/data5/city.xml");
        
        //第一次解析得到的为省份或一级直辖市
        String[][] provinces = WeaterInfoParser.parseCity(webContent);
        String[] groups = new String[provinces.length];
        String[][] childs = new String[provinces.length][];
        String[][] cityCode = new String[provinces.length][];
        for(int i=0; i< provinces.length; i++) {
        	groups[i] = provinces[i][1];
        	//由省份码来得到城市码
        	StringBuffer urlBuilder= new StringBuffer("http://m.weather.com.cn/data5/city");
        	urlBuilder.append(provinces[i][0]);
        	urlBuilder.append(".xml");
        	webContent = webTools.getWebContent(urlBuilder.toString());
        	String[][] citys = WeaterInfoParser.parseCity(webContent);
        	//用于保存所的有towns
        	String[][][] towns = new String[citys.length][][];
        	//计算总的城镇数
        	int sum=0;
        	for(int j=0; j<citys.length; j++) {
        		//由城市码来得到地方码
        		urlBuilder= new StringBuffer("http://m.weather.com.cn/data5/city");
        		urlBuilder.append(citys[j][0]);
        		urlBuilder.append(".xml");
        		webContent = webTools.getWebContent(urlBuilder.toString());
        		towns[j] = WeaterInfoParser.parseCity(webContent);
        		sum = sum + towns[j].length;
        	}
        	
        	childs[i] = new String[sum];
        	cityCode[i] = new String[sum];
        	
        	sum=0;
        	for(int j=0; j<citys.length; j++) {
        		for(int n=0; n<towns[j].length; n++) {
        			if(n==0)
        				childs[i][sum] = towns[j][n][1];
        			else
        				childs[i][sum] = towns[j][0][1] + "." + towns[j][n][1];
        			
        			urlBuilder= new StringBuffer("http://m.weather.com.cn/data5/city");
        			urlBuilder.append(towns[j][n][0]);
        			urlBuilder.append(".xml");
        			
        			webContent = webTools.getWebContent(urlBuilder.toString());
        			String[][] code=WeaterInfoParser.parseCity(webContent);
        			cityCode[i][sum] = code[0][1];
        			sum = sum + 1;
        		}
        	}
        	urlBuilder=null;
        }
        
        BaseExpandableListAdapter adapter=new MyListAdapter(this, provinceList, groups, childs);
        provinceList.setAdapter(adapter);
        
        //============================Create Database================================
        //打开或创建一个数据库
        String path="/data"+ Environment.getDataDirectory().getAbsolutePath() + "/com.weather.app/db_weather.db";
        
        SQLiteDatabase database = SQLiteDatabase.openOrCreateDatabase
                                  (path, null);
        //创建一个省份表
        String sql="create table provinces (_id integer primary key autoincrement, name text)";
        database.execSQL(sql);
        
        //创建城市表
        sql = "create table citys (_id integer primary key autoincrement, province_id integer, name text, city_num text)";
        database.execSQL(sql);
        
        //插入省份数据
        ContentValues cv = null;
        for(int i=0; i<provinces.length; i++) {
        	cv = new ContentValues();
        	cv.put("name", provinces[i][1]);
        	database.insert("provinces", null, cv);
        }
        //插入城市数据
        for(int i=0; i<childs.length; i++) {
        	for(int j=0; j<childs[i].length; j++) {
        		cv = new ContentValues();
        		cv.put("province_id", i);
        		cv.put("name", childs[i][j]);
        		cv.put("city_num", cityCode[i][j]);
        		database.insert("citys", null, cv);
        	}
        }
        cv = null;
        database.close();
    }
    
    //用于处理装载伸缩性列表的处理类
    private class MyHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			
			//在伸缩性的列表中显示数据库中的省份与城市
			adapter=new MyListAdapter(SetCityActivity.this, provinceList, groups, childs);
	        provinceList.setAdapter(adapter);
	        
	        //为其子列表选项添加单击事件
	        provinceList.setOnChildClickListener(new OnChildClickListener() {

				@Override
				public boolean onChildClick(ExpandableListView parent, View v,
						int groupPosition, int childPosition, long id) {
					//自动跳至天气的显示界面MainActivity
					
					//========得到单击的城市码=======
					//得到城市名
					String cityName = (String)adapter.getChild(groupPosition, childPosition);
					//从数据库中得到城市码
					DBHelper dbHelper = new DBHelper(SetCityActivity.this, "db_weather.db");
					String cityCode = dbHelper.getCityCodeByName(cityName);
					
					Dialog dialog = getProgressDialog("", "正在加载天气...");
					dialog.show();
					GoToMainActivity thread = new GoToMainActivity(cityCode, dialog);
					thread.start();
					
					return false;
				}
	        	
	        });
		}
    }
    
    //用于处理用户的定位信息
    private class LocateHandler extends Handler {
    	//记录定位的文本视图组件
    	private TextView textView;
    	
    	public LocateHandler(TextView textView) {
    		this.textView = textView;
    	}
    	
		@Override
		public void handleMessage(Message msg) {
			Bundle data = msg.getData();
			int provinceIndex = data.getInt("provinceIndex");
			int cityIndex = data.getInt("cityIndex");

			//判断定位匹配是否成功
			if(provinceIndex >=0 && provinceIndex < groups.length && 
					cityIndex >=0 && cityIndex < childs[provinceIndex].length) {
				//显示定位的城市
				textView.setText(childs[provinceIndex][cityIndex]);
				
				//自动跳至天气的显示界面MainActivity
				Dialog dialog = getProgressDialog("", "正在加载天气...");
				dialog.show();
				GoToMainActivity thread = new GoToMainActivity(cityCodes[provinceIndex][cityIndex], dialog);
				thread.start();
			} else {
				textView.setText("定位失败！");
			}
		}
    }
    
    //处理用户选择好城市后的跳转到MainActivity
    private class GoToMainActivity extends Thread {
    	
    	//保证跳转的城市码
    	private String cityCode;
    	//跳转后显示的进度对话框
    	private Dialog dialog;
    	
    	public GoToMainActivity(String cityCode, Dialog dialog) {
    		this.cityCode = cityCode;
    		this.dialog = dialog;
    	}
    	
    	public void run() {
    		//得到一个私有的SharedPreferences文件编辑对象
			SharedPreferences.Editor edit = getSharedPreferences(CITY_CODE_FILE, MODE_PRIVATE).edit();
			//将城市码保存
			edit.putString("code", cityCode);
			edit.commit();

			//通过判断得到的widgetId是否有效来判断是跳转到MainActivity或Widget
			if(mAppWidgetId==AppWidgetManager.INVALID_APPWIDGET_ID) {
				//设置成功回退到天气情况显示Activity
				Intent intent = getIntent();
				//当用户单击了城市返回，传入一个变量用于区分，是读存储文件天气，还是更新
				intent.putExtra("updateWeather", true);
				SetCityActivity.this.setResult(0, intent);
			} else {
				//当有效则跳至widget
				AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(SetCityActivity.this);
				RemoteViews views = new RemoteViews(SetCityActivity.this.getPackageName(),
						R.layout.widget_layout);
				//更新widget
				Log.i("widget", "===================update  weather===========================");
				//更新widget
				WeatherWidget.updateAppWidget(views, SetCityActivity.this, appWidgetManager, cityCode);
				
				appWidgetManager.updateAppWidget(mAppWidgetId, views);
				//设置成功，返回
				Intent resultValue = new Intent();
				resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
				setResult(RESULT_OK, resultValue);
			}
			SetCityActivity.this.finish();
			dialog.cancel();
			dialog.dismiss();
    	}
    }
}
