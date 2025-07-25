package com.example.thingsboard2;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class TemperatureRefreshWorker extends Service {
    private static final String TAG = "TemperatureWorker";
    private final ThingsBoardClient thingsBoardClient;
    private final Context appContext;
    private final Handler mainHandler;

    // 温度更新的广播动作（用于通知 UI）
    public static final String ACTION_TEMPERATURE_UPDATED = "com.example.thingsboard2.TEMPERATURE_UPDATED";
    public static final String EXTRA_TEMPERATURE = "temperature";
    public static final String EXTRA_HUMIDITY = "humidity";
    public static final String EXTRA_FLAME = "flame";
    public static final String EXTRA_LIGHT = "light";
    public static final String EXTRA_CO2 = "co2";

    private static final Map<String, String> SENSOR_ID_MAP = new HashMap<>();

    static {
        SENSOR_ID_MAP.put(EXTRA_TEMPERATURE, "c97e5a90-676e-11f0-931c-e980284fc7f6");
        SENSOR_ID_MAP.put(EXTRA_HUMIDITY, "c97e5a91-676e-11f0-931c-e980284fc7f6");
        SENSOR_ID_MAP.put(EXTRA_FLAME, "c97e5a92-676e-11f0-931c-e980284fc7f6");
        SENSOR_ID_MAP.put(EXTRA_LIGHT, "c97e5a93-676e-11f0-931c-e980284fc7f6");
        SENSOR_ID_MAP.put(EXTRA_CO2, "c9766b50-676e-11f0-931c-e980284fc7f6");
    }

    public TemperatureRefreshWorker() {
        this.appContext = this;
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.thingsBoardClient = new ThingsBoardClient(
                "http://172.21.19.15:8080",
                "user@qq.com",
                "admin123"
        );
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "开始执行温度刷新任务");

        // 先登录获取 Token
        thingsBoardClient.login(new ThingsBoardClient.Callback() {
            @Override
            public void onSuccess(String response) {
                Log.d(TAG, "登录成功，准备获取传感器数据");
                // 登录成功后获取所有传感器数据
                for (Map.Entry<String, String> entry : SENSOR_ID_MAP.entrySet()) {
                    final String sensorType = entry.getKey();
                    final String deviceId = entry.getValue();
                    thingsBoardClient.getTemperature(deviceId,
                            new ThingsBoardClient.Callback() {
                                @Override
                                public void onSuccess(String tempResponse) {
                                    try {
                                        // 解析温度数据
                                        JSONObject json = new JSONObject(tempResponse);
                                        JSONArray values = json.getJSONArray("value");
                                        if (values.length() > 0) {
                                            String sensorValue = values.getJSONObject(0).getString("value");
                                            Log.d(TAG, "获取 " + sensorType + " 成功: " + sensorValue);

                                            // 发送广播通知 UI 更新
                                            sendSensorBroadcast(sensorType, sensorValue);

                                            // 显示 Toast（可选，用于调试）
                                            //showToast(sensorType + " 已更新: " + sensorValue);
                                        }
                                    } catch (JSONException e) {
                                        Log.e(TAG, "解析 " + sensorType + " 数据失败", e);
                                        showToast("解析 " + sensorType + " 失败: " + e.getMessage());
                                    }
                                }

                                @Override
                                public void onFailure(Exception e) {
                                    Log.e(TAG, "获取 " + sensorType + " 失败", e);
                                    showToast("获取 " + sensorType + " 失败: " + e.getMessage());
                                }
                            });
                }
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "登录失败", e);
                showToast("登录失败: " + e.getMessage());
            }
        });

        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    // 发送传感器数据更新的广播
    private void sendSensorBroadcast(String sensorType, String sensorValue) {
        Intent intent = new Intent(ACTION_TEMPERATURE_UPDATED);
        intent.putExtra(sensorType, sensorValue);
        appContext.sendBroadcast(intent);
    }

    // 在主线程显示 Toast
    private void showToast(final String message) {
        mainHandler.post(() -> {
            Toast.makeText(appContext, message, Toast.LENGTH_SHORT).show();
        });
    }
}