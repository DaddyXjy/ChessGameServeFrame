//package frame.http;
//
//import com.alibaba.fastjson.JSON;
//import com.alibaba.fastjson.JSONObject;
//import org.apache.http.Header;
//import org.apache.http.HttpStatus;
//import org.apache.http.NameValuePair;
//import org.apache.http.client.config.RequestConfig;
//import org.apache.http.client.entity.UrlEncodedFormEntity;
//import org.apache.http.client.methods.CloseableHttpResponse;
//import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
//import org.apache.http.client.methods.HttpPost;
//import org.apache.http.client.methods.HttpRequestBase;
//import org.apache.http.entity.StringEntity;
//import org.apache.http.impl.client.CloseableHttpClient;
//import org.apache.http.impl.client.HttpClients;
//import org.apache.http.message.BasicHeader;
//import org.apache.http.message.BasicNameValuePair;
//import org.apache.http.util.EntityUtils;
//
//import java.io.IOException;
//import java.io.UnsupportedEncodingException;
//import java.net.URI;
//import java.util.*;
//import java.util.Map.Entry;
//
//
//public class HttpClientUtils {
//
//    // 编码格式。发送编码格式统一用UTF-8
//    private static final String ENCODING = "UTF-8";
//
//    // 设置连接超时时间，单位毫秒。
//    private static final int CONNECT_TIMEOUT = 6000;
//
//    // 请求获取数据的超时时间(即响应时间)，单位毫秒。
//    private static final int SOCKET_TIMEOUT = 6000;
//
//
//    /**
//     * 发送post请求；不带请求头和请求参数
//     */
//    public static HashMap doPost(String url) throws Exception {
//        HttpClientResult httpClientResult = doPost(url, null, null);
//        HashMap hashMap = JSONObject.parseObject(httpClientResult.getContent(), HashMap.class);
//        return hashMap;
//    }
//
//    /**
//     * 发送post请求；带请求参数
//     */
//    public static HashMap doPost(String url, Map<String, String> params) throws Exception {
//        HttpClientResult httpClientResult = doPost(url, null, params);
//        HashMap hashMap = JSONObject.parseObject(httpClientResult.getContent(), HashMap.class);
//        return hashMap;
//    }
//
//    /**
//     * 发送post请求；带请求头和请求参数
//     */
//    public static HttpClientResult doPost(String url, Map<String, String> headers, Map<String, String> params) throws Exception {
//        // 创建httpClient对象
//        CloseableHttpClient httpClient = HttpClients.createDefault();
//
//        // 创建http对象
//        HttpPost httpPost = new HttpPost(url);
//        /**
//         * setConnectTimeout：设置连接超时时间，单位毫秒。
//         * setConnectionRequestTimeout：设置从connect Manager(连接池)获取Connection
//         * 超时时间，单位毫秒。这个属性是新加的属性，因为目前版本是可以共享连接池的。
//         * setSocketTimeout：请求获取数据的超时时间(即响应时间)，单位毫秒。 如果访问一个接口，多少时间内无法返回数据，就直接放弃此次调用。
//         */
//        RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(CONNECT_TIMEOUT).setSocketTimeout(SOCKET_TIMEOUT).build();
//        httpPost.setConfig(requestConfig);
//        // 设置请求头
//		/*httpPost.setHeader("Cookie", "");
//		httpPost.setHeader("Connection", "keep-alive");
//		httpPost.setHeader("Accept", "application/json");
//		httpPost.setHeader("Accept-Language", "zh-CN,zh;q=0.9");
//		httpPost.setHeader("content-type", "application/json");
//		httpPost.setHeader("Accept-Encoding", "gzip, deflate, br");
//		httpPost.setHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/65.0.3325.181 Safari/537.36");*/
//        packageHeader(headers, httpPost);
//
//        // 封装请求参数
//        packageParam(params, httpPost);
//
//        // 创建httpResponse对象
//        CloseableHttpResponse httpResponse = null;
//
//        try {
//            // 执行请求并获得响应结果
//            return getHttpClientResult(httpResponse, httpClient, httpPost);
//        } finally {
//            // 释放资源
//            release(httpResponse, httpClient);
//        }
//    }
//
//
//    /**
//     * Description: 封装请求头
//     * @param params
//     * @param httpMethod
//     */
//    public static void packageHeader(Map<String, String> params, HttpRequestBase httpMethod) {
//        // 封装请求头
//        if (params != null) {
//            Set<Entry<String, String>> entrySet = params.entrySet();
//            for (Entry<String, String> entry : entrySet) {
//                // 设置到请求头到HttpRequestBase对象中
//                httpMethod.setHeader(entry.getKey(), entry.getValue());
//            }
//        }
//    }
//
//    /**
//     * Description: 封装请求参数
//     *
//     * @param params
//     * @param httpMethod
//     * @throws UnsupportedEncodingException
//     */
//    public static void packageParam(Map<String, String> params, HttpEntityEnclosingRequestBase httpMethod)
//            throws UnsupportedEncodingException {
//        // 封装请求参数
//        if (params != null) {
//            List<NameValuePair> nvps = new ArrayList<NameValuePair>();
//            Set<Entry<String, String>> entrySet = params.entrySet();
//            for (Entry<String, String> entry : entrySet) {
//                nvps.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
//            }
//
//            // 设置到请求的http对象中
//            httpMethod.setEntity(new UrlEncodedFormEntity(nvps, ENCODING));
//        }
//    }
//
//    /**
//     * Description: 获得响应结果
//     *
//     * @param httpResponse
//     * @param httpClient
//     * @param httpMethod
//     * @return
//     * @throws Exception
//     */
//    public static HttpClientResult getHttpClientResult(CloseableHttpResponse httpResponse,
//                                                       CloseableHttpClient httpClient, HttpRequestBase httpMethod) throws Exception {
//        // 执行请求
//        httpResponse = httpClient.execute(httpMethod);
//
//        // 获取返回结果
//        if (httpResponse != null && httpResponse.getStatusLine() != null) {
//            String content = "";
//            if (httpResponse.getEntity() != null) {
//                content = EntityUtils.toString(httpResponse.getEntity(), ENCODING);
//            }
//            return new HttpClientResult(httpResponse.getStatusLine().getStatusCode(), content);
//        }
//        return new HttpClientResult(HttpStatus.SC_INTERNAL_SERVER_ERROR);
//    }
//
//    /**
//     * Description: 释放资源
//     *
//     * @param httpResponse
//     * @param httpClient
//     * @throws IOException
//     */
//    public static void release(CloseableHttpResponse httpResponse, CloseableHttpClient httpClient) throws IOException {
//        // 释放资源
//        if (httpResponse != null) {
//            httpResponse.close();
//        }
//        if (httpClient != null) {
//            httpClient.close();
//        }
//    }
//
//    //---------------------------------
//    /**
//     * @Title: sendPost
//     * @Description: TODO(发送post请求)
//     * @param url 请求地址
//     * @param headers 请求头
//     * @param data 请求实体
//     * @param encoding 字符集
//     * @author wangxy
//     * @return String
//     * @date 2018年5月10日 下午4:36:17
//     * @throws
//     */
//    public static String sendPost(String url, Map<String, String> headers, JSONObject data, String encoding) {
//        // 请求返回结果
//        String resultJson = null;
//        // 创建Client
//        CloseableHttpClient client = HttpClients.createDefault();
//        // 创建HttpPost对象
//        HttpPost httpPost = new HttpPost();
//
//        try {
//            // 设置请求地址
//            httpPost.setURI(new URI(url));
//            // 设置请求头
//            if (headers != null) {
//                Header[] allHeader = new BasicHeader[headers.size()];
//                int i = 0;
//                for (Entry<String, String> entry: headers.entrySet()){
//                    allHeader[i] = new BasicHeader(entry.getKey(), entry.getValue());
//                    i++;
//                }
//                httpPost.setHeaders(allHeader);
//            }
//            // 设置实体
//            httpPost.setEntity(new StringEntity(JSON.toJSONString(data)));
//            // 发送请求,返回响应对象
//            CloseableHttpResponse response = client.execute(httpPost);
//            // 获取响应状态
//            int status = response.getStatusLine().getStatusCode();
//            if (status == HttpStatus.SC_OK) {
//                // 获取响应结果
//                resultJson = EntityUtils.toString(response.getEntity(), encoding);
//            } else {
//                System.out.println("响应失败，状态码：" + status);
//            }
//
//        } catch (Exception e) {
////            log.error("发送post请求失败", e);
//            e.printStackTrace();
//        } finally {
//            httpPost.releaseConnection();
//        }
//        return resultJson;
//    }
//
//    /**
//     * @Title: sendPost
//     * @Description: TODO(发送post请求，请求数据默认使用json格式，默认使用UTF-8编码)
//     * @param url 请求地址
//     * @param data 请求实体
//     * @author wangxy
//     * @return String
//     * @date 2018年5月10日 下午4:37:28
//     * @throws
//     */
//    public static String doPostJson(String url, JSONObject data) {
//        // 设置默认请求头
//        Map<String, String> headers = new HashMap<>();
//        headers.put("content-type", "application/json");
//
//        return sendPost(url, headers, data, ENCODING);
//    }
//
//    /**
//     * @Title: sendPost
//     * @Description: TODO(发送post请求，请求数据默认使用json格式，默认使用UTF-8编码)
//     * @param url 请求地址
//     * @param params 请求实体
//     * @author wangxy
//     * @return String
//     * @date 2018年5月10日 下午6:11:05
//     * @throws
//     */
//    public static HashMap doPostJson(String url,Map<String,Object> params){
//        // 设置默认请求头
//        Map<String, String> headers = new HashMap<>();
//        headers.put("content-type", "application/json");
//        // 将map转成json
//        JSONObject data = JSONObject.parseObject(JSON.toJSONString(params));
//        String s = sendPost(url, headers, data, ENCODING);
//        HashMap hashMap = JSONObject.parseObject(s, HashMap.class);
//        return hashMap;
//    }
//
//    /**
//     * @Title: sendPost
//     * @Description: TODO(发送post请求，请求数据默认使用UTF-8编码)
//     * @param url 请求地址
//     * @param headers 请求头
//     * @param data 请求实体
//     * @author wangxy
//     * @return String
//     * @date 2018年5月10日 下午4:39:03
//     * @throws
//     */
//    public static HashMap doPostJson(String url, Map<String, String> headers, JSONObject data) {
//        String s = sendPost(url, headers, data, ENCODING);
//        HashMap hashMap = JSONObject.parseObject(s, HashMap.class);
//        return hashMap;
//    }
//
//    /**
//     * @Title: sendPost
//     * @Description:(发送post请求，请求数据默认使用UTF-8编码)
//     * @param url 请求地址
//     * @param headers 请求头
//     * @param params 请求实体
//     * @author wangxy
//     * @return String
//     * @date 2018年5月10日 下午5:58:40
//     * @throws
//     */
//    public static HashMap doPostJson(String url,Map<String,String> headers,Map<String,String> params){
//        // 将map转成json
//        JSONObject data = JSONObject.parseObject(JSON.toJSONString(params));
//        String s = sendPost(url, headers, data, ENCODING);
//        HashMap hashMap = JSONObject.parseObject(s, HashMap.class);
//        return hashMap;
//
//
//    }
//
//
//}
//
