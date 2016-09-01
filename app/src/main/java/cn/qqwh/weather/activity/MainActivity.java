package cn.qqwh.weather.activity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import cn.qqwh.weather.R;
import cn.qqwh.weather.model.City;
import cn.qqwh.weather.service.AutoUpdateService;
import cn.qqwh.weather.util.HttpCallback;
import cn.qqwh.weather.util.HttpUtil;
import cn.qqwh.weather.util.Utility;

public class MainActivity extends AppCompatActivity {
    //我的和风天气KEY（我隐藏了个人的KEY,如需使用，请自行注册免费的和风天气，会有个人的KEY，来这里替换就好了）
    public static final String WEATHER_KEY = "aaa47302990742ea9c0b8f45e765f351";

    private ProgressDialog mProgressDialog;//进度条
    private SharedPreferences mSharedPreferences;//数据存储对象
    private SharedPreferences.Editor mEditor;
    public static final int REQUEST_CODE = 1;

    private Button mChangeCityButton;//小房子按钮
    private TextView mTextView_cityName;//标题栏城市名称
    private Button mRefreshButton;//刷新按钮
    private TextView mTextView_updateTime;//更新时间
    private TextView mTextView_current_date;//当前日期
    private TextView mTextView_weather_desp;//具体的天气情况
    private TextView mTextView_textView_temp1;//最低温度
    private TextView mTextView_textView_temp2;//最高温度

    private City mCity_current = new City();//当前显示的城市对象

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //实例化本地存储
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        mEditor = mSharedPreferences.edit();

        //变更城市（小房子按钮）
        mChangeCityButton = (Button) findViewById(R.id.button_changeCity);
        mChangeCityButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //就是启动CityChooseActivity
                Intent intent = new Intent(MainActivity.this, CityChooseActivity.class);
                //以要求返回结果的方式启动
                startActivityForResult(intent, REQUEST_CODE);
            }
        });
        //实例化各个组件
        mTextView_cityName = (TextView) findViewById(R.id.textView_city_name);
        mTextView_updateTime = (TextView) findViewById(R.id.textView_publishTime);
        mTextView_current_date = (TextView) findViewById(R.id.textView_current_date);
        mTextView_weather_desp = (TextView) findViewById(R.id.textView_weather_desp);
        mTextView_textView_temp1 = (TextView) findViewById(R.id.textView_temp1);
        mTextView_textView_temp2 = (TextView) findViewById(R.id.textView_temp2);
        //刷新按钮
        mRefreshButton = (Button) findViewById(R.id.button_refresh);
        mRefreshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //从服务器更新
                updateWeatherFromServer();
            }
        });
        //这个是为了在在第一次安装的时候，判断本地存储还没有数据，所以默认获取武汉的数据
        //如果需要修改，可以从和风天气官网 http://www.heweather.com/documents/cn-city-list查询城市ID
        if (mSharedPreferences.getString("city_code", null) == null) {
            mCity_current.setCity_code("CN101200101");
            updateWeatherFromServer();
        } else {
            //有数据，则从本地取出来，也就是上次访问的城市，先确定这个
            loadWeatherData(mSharedPreferences.getString("city_code", null), mSharedPreferences.getString("city_name_ch", null), mSharedPreferences.getString("update_time", null), mSharedPreferences.getString("data_now", null), mSharedPreferences.getString("txt_d", null), mSharedPreferences.getString("txt_n", null), mSharedPreferences.getString("tmp_min", null), mSharedPreferences.getString("tmp_max", null));
            //然后从服务器更新一次
            updateWeatherFromServer();//可以注释掉，使用服务进行自动更新
        }
        //启动自动更新服务（不过我这里没怎么使用到自动更新，我这里都是打开后实时更新的，可以打开后不从服务器更新，只从本地获取）
        Intent intent = new Intent(this, AutoUpdateService.class);
        startService(intent);

        /*有米广告栏，不多做解释，第一行代码里面解释很清楚，也很简单
        AdView adView = new AdView(this, AdSize.FIT_SCREEN);
        LinearLayout linearLayout = (LinearLayout) findViewById(R.id.adLayout);
        linearLayout.addView(adView);*/
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                loadWeatherData(mSharedPreferences.getString("city_code", null), mSharedPreferences.getString("city_name_ch", null), mSharedPreferences.getString("update_time", null), mSharedPreferences.getString("data_now", null), mSharedPreferences.getString("txt_d", null), mSharedPreferences.getString("txt_n", null), mSharedPreferences.getString("tmp_min", null), mSharedPreferences.getString("tmp_max", null));
            }
        }
    }

    //刷新各组件数据的封装
    private void loadWeatherData(String city_code, String city_name, String update_time, String current_data, String txt_d, String txt_n, String tmp_min, String tmp_max) {

        mTextView_cityName.setText(city_name);
        mTextView_updateTime.setText(update_time);
        mTextView_current_date.setText(current_data);

        if (txt_d.equals(txt_n)) {
            mTextView_weather_desp.setText(txt_d);
        } else {
            mTextView_weather_desp.setText(txt_d + "转" + txt_n);
        }
        mTextView_textView_temp1.setText(tmp_min + "℃");
        mTextView_textView_temp2.setText(tmp_max + "℃");

        mCity_current.setCity_name_ch(city_name);
        mCity_current.setCity_code(city_code);

    }
    //从服务器更新数据（CityChooseActivity中有相似方法）
    private void updateWeatherFromServer() {
        String address = "https://api.heweather.com/x3/weather?cityid=" + mCity_current.getCity_code() + "&key=" + MainActivity.WEATHER_KEY;
        showProgressDialog();
        HttpUtil.sendHttpRequest(address, new HttpCallback() {
            @Override
            public void onFinish(final String response) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (Utility.handleWeatherResponse(mEditor, response)) {
                            loadWeatherData(mSharedPreferences.getString("city_code", null), mSharedPreferences.getString("city_name_ch", null), mSharedPreferences.getString("update_time", null), mSharedPreferences.getString("data_now", null), mSharedPreferences.getString("txt_d", null), mSharedPreferences.getString("txt_n", null), mSharedPreferences.getString("tmp_min", null), mSharedPreferences.getString("tmp_max", null));
                            closeProgressDialog();
                        }
                    }
                });
            }

            @Override
            public void onError(final Exception e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        closeProgressDialog();
                        Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }
    private void showProgressDialog() {

        if (mProgressDialog == null) {

            mProgressDialog = new ProgressDialog(this);
            mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            mProgressDialog.setMessage("正在同步数据...");
            mProgressDialog.setCanceledOnTouchOutside(false);
        }
        mProgressDialog.show();
    }

    private void closeProgressDialog() {
        if (mProgressDialog != null)
            mProgressDialog.dismiss();
    }
}
