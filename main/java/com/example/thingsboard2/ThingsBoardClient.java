package com.example.thingsboard2;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ThingsBoardClient {
    private static final String TAG = "ThingsBoardClient";
    private final String baseUrl;
    private final String username;
    private final String password;
    private String authToken;
    private final OkHttpClient client;

    // 定义回调接口，用于传递结果给调用方
    public interface Callback {
        void onSuccess(String response);
        void onFailure(Exception e);
    }

    public ThingsBoardClient(String baseUrl, String username, String password) {
        this.baseUrl = baseUrl;
        this.username = username;
        this.password = password;
        this.client = new OkHttpClient();
    }

    // 登录方法：异步执行登录请求，获取 JWT Token
    public void login(Callback callback) {
        new LoginTask(callback).execute();
    }

    // 获取温度方法：异步执行温度查询请求
    public void getTemperature(String deviceId, Callback callback) {
        if (authToken == null) {
            callback.onFailure(new Exception("请先登录获取 Token"));
            return;
        }
        new TemperatureTask(deviceId, callback).execute();
    }

    // 控制设备方法
    public void controlDevice(String deviceId, boolean isOn, Callback callback) {
        if (authToken == null) {
            callback.onFailure(new Exception("请先登录获取 Token"));
            return;
        }
        new ControlDeviceTask(deviceId, isOn, callback).execute();
    }

    // 登录任务：在后台线程执行登录请求
    private class LoginTask extends AsyncTask<Void, Void, String> {
        private final Callback callback;
        private Exception exception;

        public LoginTask(Callback callback) {
            this.callback = callback;
        }

        @Override
        protected String doInBackground(Void... voids) {
            try {
                MediaType JSON = MediaType.get("application/json; charset=utf-8");
                String loginUrl = baseUrl + "/api/auth/login";

                // 构建登录请求体
                JSONObject json = new JSONObject();
                json.put("username", username);
                json.put("password", password);
                RequestBody body = RequestBody.create(json.toString(), JSON);

                // 构建并执行登录请求
                Request request = new Request.Builder()
                        .url(loginUrl)
                        .post(body)
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        throw new IOException("登录失败，状态码: " + response.code());
                    }
                    String responseData = response.body().string();
                    JSONObject jsonResponse = new JSONObject(responseData);
                    authToken = jsonResponse.getString("token");
                    return responseData;
                }
            } catch (IOException | JSONException e) {
                exception = e;
                return null;
            }
        }

        @Override
        protected void onPostExecute(String responseData) {
            if (callback == null) return;

            if (responseData != null) {
                callback.onSuccess(responseData);
            } else {
                callback.onFailure(exception != null ? exception : new Exception("登录失败"));
            }
        }
    }

    // 温度查询任务：在后台线程执行温度查询请求
    private class TemperatureTask extends AsyncTask<Void, Void, String> {
        private final String deviceId;
        private final Callback callback;
        private Exception exception;

        public TemperatureTask(String deviceId, Callback callback) {
            this.deviceId = deviceId;
            this.callback = callback;
        }

        @Override
        protected String doInBackground(Void... voids) {
            try {
                String tempUrl = baseUrl + "/api/plugins/telemetry/DEVICE/" + deviceId + "/values/timeseries";

                // 构建带 Token 的请求
                Request request = new Request.Builder()
                        .url(tempUrl)
                        .header("X-Authorization", "Bearer " + authToken)
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        throw new IOException("获取温度失败，状态码: " + response.code());
                    }
                    return response.body().string();
                }
            } catch (IOException e) {
                exception = e;
                return null;
            }
        }

        @Override
        protected void onPostExecute(String responseData) {
            if (callback == null) return;

            if (responseData != null) {
                callback.onSuccess(responseData);
            } else {
                callback.onFailure(exception != null ? exception : new Exception("获取温度失败"));
            }
        }
    }

    // 控制设备任务
    private class ControlDeviceTask extends AsyncTask<Void, Void, String> {
        private final String deviceId;
        private final boolean isOn;
        private final Callback callback;
        private Exception exception;

        public ControlDeviceTask(String deviceId, boolean isOn, Callback callback) {
            this.deviceId = deviceId;
            this.isOn = isOn;
            this.callback = callback;
        }

        @Override
        protected String doInBackground(Void... voids) {
            try {
                MediaType JSON = MediaType.get("application/json; charset=utf-8");
                String controlUrl = baseUrl + "/api/plugins/rpc/oneway/" + deviceId;

                // 构建控制请求体
                JSONObject json = new JSONObject();
                json.put("method", "setOn");
                json.put("params", isOn);
                RequestBody body = RequestBody.create(json.toString(), JSON);

                // 构建并执行控制请求
                Request request = new Request.Builder()
                        .url(controlUrl)
                        .header("X-Authorization", "Bearer " + authToken)
                        .post(body)
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        throw new IOException("控制设备失败，状态码: " + response.code());
                    }
                    return response.body().string();
                }
            } catch (IOException | JSONException e) {
                exception = e;
                return null;
            }
        }

        @Override
        protected void onPostExecute(String responseData) {
            if (callback == null) return;

            if (responseData != null) {
                callback.onSuccess(responseData);
            } else {
                callback.onFailure(exception != null ? exception : new Exception("控制设备失败"));
            }
        }
    }
}