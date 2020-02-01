package frame.http;

import com.alibaba.fastjson.JSON;
import frame.config.Config;
import okhttp3.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import frame.log;

public class OkHttpDemo {

    public static final String URL_PRE = "http://";

    public static final String GAME_URL = URL_PRE + Config.GAME_HOST + ":" + Config.GAME_PORT;
    public static final String ACCOUNT_URL = URL_PRE + Config.ACCOUNT_HOST + ":" + Config.ACCOUNT_PORT;

    private static OkHttpClient client = new OkHttpClient();

    public static void postJson(String url, String json, frame.Callback callback) {
        try {
            Call call = client.newCall(
                    getRequest(RequestBody.create(MediaType.parse("application/json; charset=utf-8"), json), url));
            String string = call.execute().body().string();
            // log.info("json接受到的消息==>{}", string);
            setCallbackData(url, string, callback);
        } catch (IOException e) {
            log.error("okHttpDemo.postJson:", e);
        }
    }

    public static void postForm(RequestBody body, String url, frame.Callback callback) {
        try {
            String string = client.newCall(getRequest(body, url)).execute().body().string();
            // log.info("form接受到的消息==>{}", string);
            setCallbackData(url, string, callback);
        } catch (IOException e) {
            log.error("okHttpDemo.postForm:", e);
        }
    }

    private static Request getRequest(RequestBody body, String url) {
        return new Request.Builder().url(url).post(body).build();
    }

    private static void setCallbackData(String url, String data, frame.Callback callback) {
        try {
            callback.setData(JSON.parseObject(data, GlobeResponse.class));
        } catch (Exception e) {
            log.error("error http request:{}", url);
            log.error("数据解析异常:", e);
            GlobeResponse<String> response = new GlobeResponse<String>();
            response.setGlobeResponse("404", "返回数据异常");
            response.setData("数据异常");
            callback.setData(response);
        }
    }
}
