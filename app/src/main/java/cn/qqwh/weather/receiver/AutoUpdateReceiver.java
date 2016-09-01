package cn.qqwh.weather.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import cn.qqwh.weather.service.AutoUpdateService;

/**
 * Created by QQWH on 2016/8/15.
 */
public class AutoUpdateReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {

        Intent intent_for_service = new Intent(context, AutoUpdateService.class);
        context.startService(intent_for_service);

    }
}