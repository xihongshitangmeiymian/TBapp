package com.example.thingsboard2;

import androidx.appcompat.app.AppCompatActivity;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    private TextView tvTemperature;
    private TextView tvHumidity;
    private TextView tvFlame;
    private TextView tvLight;
    private TextView tvCo2;
    private BroadcastReceiver temperatureReceiver;
    private Handler handler;
    private Runnable runnable;
    private ThingsBoardClient thingsBoardClient;
    private static final String FAN_DEVICE_ID = "ddb9a500-676e-11f0-931c-e980284fc7f6";
    private static final String ALARM_LIGHT_DEVICE_ID = "fb1adab0-676e-11f0-931c-e980284fc7f6";
    private static final String WARNING_LIGHT_DEVICE_ID = "ddb97df0-676e-11f0-931c-e980284fc7f6";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvTemperature = findViewById(R.id.tv_temperature);
        tvHumidity = findViewById(R.id.tv_humidity);
        tvFlame = findViewById(R.id.tv_flame);
        tvLight = findViewById(R.id.tv_light);
        tvCo2 = findViewById(R.id.tv_co2);

        // 初始化 ThingsBoardClient
        thingsBoardClient = new ThingsBoardClient(
                "http://172.21.19.15:8080",
                "user@qq.com",
                "admin123"
        );

        // 注册广播接收器
        registerTemperatureReceiver();

        // 启动每3秒刷新一次的任务
        startPeriodicTemperatureRefresh();

        // 监听 switch_fan 的状态变化
        Switch switchFan = findViewById(R.id.switch_fan);
        switchFan.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // 先登录获取 Token
                thingsBoardClient.login(new ThingsBoardClient.Callback() {
                    @Override
                    public void onSuccess(String response) {
                        // 登录成功后控制设备
                        thingsBoardClient.controlDevice(FAN_DEVICE_ID, isChecked, new ThingsBoardClient.Callback() {
                            @Override
                            public void onSuccess(String response) {
                                Toast.makeText(MainActivity.this, "风扇控制成功", Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onFailure(Exception e) {
                                Toast.makeText(MainActivity.this, "风扇控制失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
                    }

                    @Override
                    public void onFailure(Exception e) {
                        Toast.makeText(MainActivity.this, "登录失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        // 监听 switch_alarm_light 的状态变化
        Switch switchAlarmLight = findViewById(R.id.switch_alarm_light);
        switchAlarmLight.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // 先登录获取 Token
                thingsBoardClient.login(new ThingsBoardClient.Callback() {
                    @Override
                    public void onSuccess(String response) {
                        // 登录成功后控制设备
                        thingsBoardClient.controlDevice(ALARM_LIGHT_DEVICE_ID, isChecked, new ThingsBoardClient.Callback() {
                            @Override
                            public void onSuccess(String response) {
                                Toast.makeText(MainActivity.this, "报警灯控制成功", Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onFailure(Exception e) {
                                Toast.makeText(MainActivity.this, "报警灯控制失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
                    }

                    @Override
                    public void onFailure(Exception e) {
                        Toast.makeText(MainActivity.this, "登录失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        // 监听 switch_warning_light 的状态变化
        Switch switchWarningLight = findViewById(R.id.switch_warning_light);
        switchWarningLight.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // 先登录获取 Token
                thingsBoardClient.login(new ThingsBoardClient.Callback() {
                    @Override
                    public void onSuccess(String response) {
                        // 登录成功后控制设备
                        thingsBoardClient.controlDevice(WARNING_LIGHT_DEVICE_ID, isChecked, new ThingsBoardClient.Callback() {
                            @Override
                            public void onSuccess(String response) {
                                Toast.makeText(MainActivity.this, "警示灯控制成功", Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onFailure(Exception e) {
                                Toast.makeText(MainActivity.this, "警示灯控制失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
                    }

                    @Override
                    public void onFailure(Exception e) {
                        Toast.makeText(MainActivity.this, "登录失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void startPeriodicTemperatureRefresh() {
        handler = new Handler();
        runnable = new Runnable() {
            @Override
            public void run() {
                // 启动温度刷新任务
                Intent intent = new Intent(MainActivity.this, TemperatureRefreshWorker.class);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    MainActivity.this.startForegroundService(intent);
                } else {
                    MainActivity.this.startService(intent);
                }
                // 每3秒执行一次
                handler.postDelayed(this, 3000);
            }
        };
        // 立即启动第一次任务
        handler.post(runnable);
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private void registerTemperatureReceiver() {
        temperatureReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (TemperatureRefreshWorker.ACTION_TEMPERATURE_UPDATED.equals(intent.getAction())) {
                    String temperature = intent.getStringExtra(TemperatureRefreshWorker.EXTRA_TEMPERATURE);
                    if (temperature != null) {
                        // 更新UI显示
                        tvTemperature.setText(temperature + " °C");
                        //Toast.makeText(context, "温度已更新: " + temperature + " °C", Toast.LENGTH_SHORT).show();
                    }

                    String humidity = intent.getStringExtra(TemperatureRefreshWorker.EXTRA_HUMIDITY);
                    if (humidity != null) {
                        tvHumidity.setText(humidity + " %RH");
                    }

                    String flame = intent.getStringExtra(TemperatureRefreshWorker.EXTRA_FLAME);
                    if (flame != null) {
                        tvFlame.setText(flame);
                    }

                    String light = intent.getStringExtra(TemperatureRefreshWorker.EXTRA_LIGHT);
                    if (light != null) {
                        tvLight.setText(light + " lux");
                    }

                    String co2 = intent.getStringExtra(TemperatureRefreshWorker.EXTRA_CO2);
                    if (co2 != null) {
                        tvCo2.setText(co2 + " ppm");
                    }
                }
            }
        };

        // 创建意图过滤器
        IntentFilter filter = new IntentFilter(TemperatureRefreshWorker.ACTION_TEMPERATURE_UPDATED);

        // 根据Android版本使用不同的注册方法
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13及以上版本需要指定导出标志
            registerReceiver(temperatureReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            // Android 12及以下版本使用旧方法
            registerReceiver(temperatureReceiver, filter);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 注销广播接收器以避免内存泄漏
        unregisterReceiver(temperatureReceiver);
        // 移除所有待执行的任务
        if (handler != null) {
            handler.removeCallbacks(runnable);
        }
    }
}