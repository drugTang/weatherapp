package com.weatherapp.app.activity;

import java.util.ArrayList;
import java.util.List;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.weatherapp.app.R;
import com.weatherapp.app.db.WeatherDB;
import com.weatherapp.app.model.City;
import com.weatherapp.app.model.County;
import com.weatherapp.app.model.Province;
import com.weatherapp.app.util.BaseActivity;
import com.weatherapp.app.util.HttpCallbackListener;
import com.weatherapp.app.util.HttpUtils;
import com.weatherapp.app.util.Utility;

public class ChooseAreaActivity extends BaseActivity{
	/**
	 * ��¼��ǰListView��ʾ�����ĸ�����
	 */
	private int currentLevel;
	private int LEVEL_PROVINCE = 0;
	private int LEVEL_CITY = 1;
	private int LEVEL_COUNTY = 2;
	
	private List<Province> provinceList;
	private List<City> cityList;
	private List<County> countyList;
	private Province selectedProvince;
	private City selectedCity;
	
	private WeatherDB weatherDB;
	private TextView titleText;
	private ListView listView;
	private ArrayAdapter<String> adapter;
	private ProgressDialog progressDialog;
	private List<String> dataList = new ArrayList<String>();
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.choose_area);
		titleText = (TextView)findViewById(R.id.title_text);
		listView = (ListView)findViewById(R.id.list_view);
		adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, dataList);
		listView.setAdapter(adapter);
		weatherDB = WeatherDB.getInstance(this);
		//��ѯȫ�����е�ʡ��
		queryProvinces();
		listView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				if(currentLevel == LEVEL_PROVINCE) {
					selectedProvince = provinceList.get(position);
					queryCities();
				} else if (currentLevel == LEVEL_CITY) {
					selectedCity = cityList.get(position);
					queryCounties();
				}
			}
			
		});
	}
	
	/**
	 * ��ѯȫ�����е�ʡ�����ȴ����ݿ��ѯ�����û�в�ѯ�ٵ���������ȥ��ѯ
	 */
	private void queryProvinces() {
		provinceList = weatherDB.loadProvince();
		if(provinceList.size() > 0) {
			dataList.clear();
			for(Province province : provinceList) {
				dataList.add(province.getProvinceName());
			}
			adapter.notifyDataSetChanged();
			listView.setSelection(0);
			titleText.setText("�й�");
			currentLevel = LEVEL_PROVINCE;
		} else {
			queryFromServer(null, "province");
		}
	}
	
	/**
	 * ����ʡ�ݲ�ѯʡ�����е��У����ȴ����ݿ��в�ѯ����û����ӷ������ϲ�ѯ
	 * @param selectedProvince Ҫ��ѯ��ʡ��
	 */
	private void queryCities() {
		cityList = weatherDB.loadCities(selectedProvince.getId());
		if(cityList.size() > 0) {
			dataList.clear();
			for(City city : cityList) {
				dataList.add(city.getCityName());
			}
			adapter.notifyDataSetChanged();
			titleText.setText(selectedProvince.getProvinceName());
			listView.setSelection(0);
			currentLevel = LEVEL_CITY;
		} else {
			queryFromServer(selectedProvince.getProvinceCode(),"city");
		}
	}
	
	/**
	 * ������ѡ�в�ѯ�������е��أ����ȴ����ݿ��в�ѯ����û����ӷ������ϲ�ѯ
	 * @param selectedCounty
	 */
	private void queryCounties() {
		countyList = weatherDB.loadCounties(selectedCity.getId());
		if(countyList.size() > 0) {
			dataList.clear();
			for(County county : countyList) {
				dataList.add(county.getCountyName());
			}
			adapter.notifyDataSetChanged();
			listView.setSelection(0);
			titleText.setText(selectedCity.getCityName());
			currentLevel = LEVEL_COUNTY;
		} else {
			queryFromServer(selectedCity.getCityCode(),"county");
		}
	}
	
	private void queryFromServer(final String code, final String type) {
		String address;
		if (!TextUtils.isEmpty(code)) {
			address = "http://www.weather.com.cn/data/list3/city" + code + ".xml";
		} else {
			address = "http://www.weather.com.cn/data/list3/city.xml";
		}
		showProgressDialog();
		HttpUtils.sendHttpRequest(address, new HttpCallbackListener() {

			@Override
			public void onFinish(String response) {
				boolean result = false;
				if("province".equals(type)) {
					result = Utility.handleProvincesResponse(weatherDB, response);
				} else if("city".equals(type)) {
					result = Utility.handleCityResponse(weatherDB, response, selectedProvince.getId());
				} else if("county".equals(type)) {
					result = Utility.handleCountyResponse(weatherDB, response, selectedCity.getId());
				}
				if(result) {
					//ͨ��runOnUiThread()�����ص����̴߳����߼�
					runOnUiThread(new Runnable() {
						
						@Override
						public void run() {
							closeProgressDialog();
							//����ǵ����ݿ��ж�ȡ����
							if("province".equals(type)) {
								queryProvinces();
							} else if("city".equals(type)) {
								queryCities();
							} else if("county".equals(type)) {
								queryCounties();
							}
						}
						
					});
				}
			}

			@Override
			public void onError(Exception e) {
				//ͨ��runOnUiThread()�����ص����̴߳����߼�
				runOnUiThread(new Runnable() {

					@Override
					public void run() {
						closeProgressDialog();
						Toast.makeText(ChooseAreaActivity.this, "����ʧ��", Toast.LENGTH_SHORT).show();
					}
					
				});
			}
			
		});
	}
	
	private void showProgressDialog() {
		if(progressDialog == null) {
			progressDialog = new ProgressDialog(this);
			progressDialog.setMessage("���ڼ���");
			progressDialog.setCancelable(false);
		}
		progressDialog.show();
	}
	
	private void closeProgressDialog() {
		if(progressDialog != null) {
			progressDialog.dismiss();
		}
	}
	
	@Override
	public void onBackPressed() {
		if(currentLevel == LEVEL_COUNTY) {
			queryCities();
		} else if(currentLevel == LEVEL_CITY) {
			queryProvinces();
		} else {
			finish();
		}
	}
}
