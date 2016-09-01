package cn.qqwh.weather.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;

import cn.qqwh.weather.activity.MainActivity;
import cn.qqwh.weather.receiver.AutoUpdateReceiver;
import cn.qqwh.weather.util.HttpCallback;
import cn.qqwh.weather.util.HttpUtil;
import cn.qqwh.weather.util.Utility;

/**
 * Created by QQWH on 2016/8/15.
 */
public class AutoUpdateService extends Service {

    SharedPreferences sharedPreferences;
    SharedPreferences.Editor editor;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                updateWeather();
            }
        });

        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        int hour = 60 * 60 * 1000;

        long triggerTime = SystemClock.currentThreadTimeMillis() + hour;

        Intent intent_for_receiver = new Intent(this, AutoUpdateReceiver.class);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, 0);

        alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerTime, pendingIntent);


        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        editor = sharedPreferences.edit();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void updateWeather() {

        String city_code = sharedPreferences.getString("city_code", null);
        String address = "https://api.heweather.com/x3/weather?cityid=" + city_code + "&key=" + MainActivity.WEATHER_KEY;

        HttpUtil.sendHttpRequest(address, new HttpCallback() {
            @Override
            public void onFinish(String response) {
                Utility.handleWeatherResponse(editor, response);
            }

            @Override
            public void onError(Exception e) {
                e.printStackTrace();
            }
        });
    }
}
