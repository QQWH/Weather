package cn.qqwh.weather.util;

/**
 * Created by QQWH on 2016/8/15.
 */
public interface HttpCallback {
    void onFinish(String response);
    void onError(Exception e);
}
