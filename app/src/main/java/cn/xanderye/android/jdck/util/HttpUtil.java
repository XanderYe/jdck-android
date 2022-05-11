package cn.xanderye.android.jdck.util;

import lombok.Data;
import cz.msebera.android.httpclient.*;
import cz.msebera.android.httpclient.client.HttpRequestRetryHandler;
import cz.msebera.android.httpclient.client.config.CookieSpecs;
import cz.msebera.android.httpclient.client.config.RequestConfig;
import cz.msebera.android.httpclient.client.entity.UrlEncodedFormEntity;
import cz.msebera.android.httpclient.client.methods.*;
import cz.msebera.android.httpclient.client.protocol.HttpClientContext;
import cz.msebera.android.httpclient.conn.ConnectTimeoutException;
import cz.msebera.android.httpclient.conn.ssl.SSLConnectionSocketFactory;
import cz.msebera.android.httpclient.entity.ByteArrayEntity;
import cz.msebera.android.httpclient.entity.StringEntity;
import cz.msebera.android.httpclient.impl.client.CloseableHttpClient;
import cz.msebera.android.httpclient.impl.client.HttpClientBuilder;
import cz.msebera.android.httpclient.impl.client.HttpClients;
import cz.msebera.android.httpclient.impl.conn.PoolingHttpClientConnectionManager;
import cz.msebera.android.httpclient.message.BasicNameValuePair;
import cz.msebera.android.httpclient.ssl.SSLContextBuilder;
import cz.msebera.android.httpclient.util.EntityUtils;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * http请求工具 返回对象包括状态码、响应头、cookie和响应体，自行根据状态码判断
 * 默认不使用连接池，需要请调用 enableConnectionPool
 *
 * @author XanderYe
 * @date 2020/2/4
 */
public class HttpUtil {

    /**
     * 默认请求超时
     */
    private static final int DEFAULT_CONNECT_TIMEOUT = 30000;
    /**
     * 默认读取超时
     */
    private static final int DEFAULT_SOCKET_TIMEOUT = 30000;
    /**
     * 默认重试次数
     */
    private static final int DEFAULT_RETRY_COUNT = 3;
    /**
     * 默认最大连接数
     */
    private static final int DEFAULT_MAX_TOTAL = 200;
    /**
     * 默认单个路由最大连接数
     */
    private static final int DEFAULT_MAX_PER_ROUTE = 100;

    /**
     * 默认清空空闲连接 秒
     */
    private static final int DEFAULT_IDLE_TIMEOUT = 5;
    /**
     * 默认定时器间隔 秒
     */
    private static final int DEFAULT_MONITOR_PERIOD = 30;
    /**
     * 默认编码
     */
    private static final String CHARSET = "UTF-8";
    /**
     * 默认请求头
     */
    private static final String DEFAULT_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/74.0.3729.169 Safari/537.36";
    /**
     * 基本请求路径
     */
    private static String baseUrl = "";
    /**
     * 是否使用代理
     */
    private static boolean enableProxy = false;
    private static String proxyIp = "127.0.0.1";
    private static int proxyPort = 8888;
    /**
     * 是否自动重定向
     */
    private static boolean redirect = false;
    /**
     * 是否重试
     */
    private static boolean enableRetry = false;
    /**
     * 连接超时
     */
    private static int connectTimeout;
    /**
     * 读取超时
     */
    private static int socketTimeout;
    /**
     * 重试次数
     */
    private static int retryCount;
    /**
     * 最大连接数
     */
    private static int maxTotal;
    /**
     * 单个路由最大连接数
     */
    private static int maxPerRoute;
    /**
     * 清空空闲连接 秒
     */
    private static int idleTimeout;
    /**
     * 定时器间隔  秒
     */
    private static int monitorPeriod;

    /**
     * 是否启用连接池
     */
    private static AtomicBoolean connectionPool = new AtomicBoolean(false);
    /**
     * 连接池httpClient对象
     */
    private static volatile CloseableHttpClient httpClient;
    /**
     * 连接池配置
     */
    private static volatile PoolingHttpClientConnectionManager connectionManager;
    /**
     * 定时器
     */
    private static volatile ScheduledExecutorService monitorExecutor;

    // 静态代码块初始化配置
    static {
        socketTimeout = DEFAULT_SOCKET_TIMEOUT;
        connectTimeout = DEFAULT_CONNECT_TIMEOUT;
        retryCount = DEFAULT_RETRY_COUNT;
        maxTotal = DEFAULT_MAX_TOTAL;
        maxPerRoute = DEFAULT_MAX_PER_ROUTE;
        idleTimeout = DEFAULT_IDLE_TIMEOUT;
        monitorPeriod = DEFAULT_MONITOR_PERIOD;
    }

    /**
     * 获取客户端
     * @param
     * @return void
     * @author XanderYe
     * @date 2021/5/24
     */
    private static CloseableHttpClient getHttpClient() {
        // 启用了连接池配置，直接返回全局客户端
        if (connectionPool.get()) {
            return httpClient;
        }
        RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(connectTimeout)
                .setSocketTimeout(socketTimeout)
                .setCookieSpec(CookieSpecs.IGNORE_COOKIES)
                .setRedirectsEnabled(redirect)
                .build();
        return custom().setDefaultRequestConfig(config).build();
    }

    /**
     * 初始化连接池配置
     * @param
     * @return void
     * @author XanderYe
     * @date 2022/1/21
     */
    private static void initPoolingHttpClient() {
        if (httpClient == null) {
            synchronized (HttpUtil.class) {
                if (httpClient == null) {
                    RequestConfig config = RequestConfig.custom()
                            .setConnectTimeout(connectTimeout)
                            .setSocketTimeout(socketTimeout)
                            .setCookieSpec(CookieSpecs.IGNORE_COOKIES)
                            .setRedirectsEnabled(redirect)
                            .build();
                    connectionManager = new PoolingHttpClientConnectionManager();
                    connectionManager.setMaxTotal(maxTotal);
                    connectionManager.setDefaultMaxPerRoute(maxPerRoute);
                    httpClient = custom().setDefaultRequestConfig(config).setConnectionManager(connectionManager).build();
                    connectionPool.set(true);
                    //开启监控线程,对异常和空闲线程进行关闭
                    monitorExecutor = Executors.newScheduledThreadPool(1);
                    monitorExecutor.scheduleAtFixedRate(() -> {
                        //关闭异常连接
                        connectionManager.closeExpiredConnections();
                        //关闭空闲的连接
                        connectionManager.closeIdleConnections(idleTimeout, TimeUnit.SECONDS);
                    }, 0, monitorPeriod, TimeUnit.SECONDS);
                }
            }
        }
    }

    /**
     * 创建httpClientBuilder
     *
     * @return cz.msebera.android.httpclient.impl.client.HttpClientBuilder
     * @author XanderYe
     * @date 2020/2/14
     */
    private static HttpClientBuilder custom() {
        HttpClientBuilder httpClientBuilder = HttpClients.custom();
        // 忽略证书
        httpClientBuilder.setSSLSocketFactory(ignoreCertificates());
        if (enableProxy) {
            // 使用代理
            httpClientBuilder.setProxy(new HttpHost(proxyIp, proxyPort));
        }
        if (enableRetry) {
            // 使用重试机制
            httpClientBuilder.setRetryHandler(retryHandler());
        }
        return httpClientBuilder;
    }

    /**
     * GET请求
     *
     * @param url
     * @param params
     * @return cn.xanderye.util.HttpUtil.ResEntity
     * @author XanderYe
     * @date 2020-03-15
     */
    public static ResEntity doGet(String url, Map<String, Object> params) throws IOException {
        return doGet(url, null, null, params);
    }


    /**
     * POST请求
     *
     * @param url
     * @param params
     * @return cn.xanderye.util.HttpUtil.ResEntity
     * @author XanderYe
     * @date 2020-03-15
     */
    public static ResEntity doPost(String url, Map<String, Object> params) throws IOException {
        return doPost(url, null, null, params);
    }

    /**
     * POST提交JSON请求
     * @param url
     * @param jsonString
     * @return cn.xanderye.util.HttpUtil.ResEntity
     * @author XanderYe
     * @date 2020/10/22
     */
    public static ResEntity doPostJSON(String url, String jsonString) throws IOException {
        return doPostJSON(url, null, null, jsonString);
    }

    /**
     * POST提交XML请求
     * @param url
     * @param xml
     * @return cn.xanderye.util.HttpUtil.ResEntity
     * @author XanderYe
     * @date 2020/12/10
     */
    public static ResEntity doPostXML(String url, String xml) throws IOException {
        return doPostXML(url, null, null, xml);
    }

    /**
     * get请求基础方法
     *
     * @param url
     * @param headers
     * @param params
     * @return cn.xanderye.util.HttpUtil.ResEntity
     * @author XanderYe
     * @date 2020/2/4
     */
    public static ResEntity doGet(String url, Map<String, Object> headers, Map<String, Object> cookies, Map<String, Object> params) throws IOException {
        url = baseUrl + url;
        // 拼接参数
        if (params != null && !params.isEmpty()) {
            List<NameValuePair> pairs = new ArrayList<>(params.size());
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                String value = entry.getValue() == null ? null : (entry.getValue()).toString();
                if (value != null) {
                    pairs.add(new BasicNameValuePair(entry.getKey(), value));
                }
            }
            try {
                String parameters = EntityUtils.toString(new UrlEncodedFormEntity(pairs, CHARSET));
                String symbol = url.contains("?") ? "&" : "?";
                // 判断是否已带参数
                url += symbol + parameters;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        HttpGet httpGet = new HttpGet(url);
        // 添加headers
        addHeaders(httpGet, headers);
        // 添加cookies
        addCookies(httpGet, cookies);
        HttpClientContext httpClientContext = new HttpClientContext();
        CloseableHttpClient httpClient = getHttpClient();
        try (CloseableHttpResponse response = httpClient.execute(httpGet, httpClientContext)) {
            return getResEntity(response, false);
        } finally {
            if (!connectionPool.get()) {
                httpClient.close();
            }
        }
    }

    /**
     * post请求基础方法
     *
     * @param url
     * @param headers
     * @param params
     * @return cn.xanderye.util.HttpUtil.ResEntity
     * @author XanderYe
     * @date 2020/2/4
     */
    public static ResEntity doPost(String url, Map<String, Object> headers, Map<String, Object> cookies, Map<String, Object> params) throws IOException {
        HttpPost httpPost = new HttpPost(baseUrl + url);
        // 拼接参数
        if (params != null && !params.isEmpty()) {
            List<NameValuePair> pairs = new ArrayList<>(params.size());
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                String value = entry.getValue() == null ? null : (entry.getValue()).toString();
                if (value != null) {
                    pairs.add(new BasicNameValuePair(entry.getKey(), value));
                }
            }
            try {
                httpPost.setEntity(new UrlEncodedFormEntity(pairs, CHARSET));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        // 添加headers
        addHeaders(httpPost, headers);
        // 添加cookies
        addCookies(httpPost, cookies);
        HttpClientContext httpClientContext = new HttpClientContext();
        CloseableHttpClient httpClient = getHttpClient();
        try (CloseableHttpResponse response = httpClient.execute(httpPost, httpClientContext)) {
            return getResEntity(response, false);
        } finally {
            if (!connectionPool.get()) {
                httpClient.close();
            }
        }
    }

    /**
     * POST提交JSON基础方法
     *
     * @param url
     * @param headers
     * @param json
     * @return cn.xanderye.util.HttpUtil.ResEntity
     * @author XanderYe
     * @date 2020/2/4
     */
    public static ResEntity doPostJSON(String url, Map<String, Object> headers, Map<String, Object> cookies, String json) throws IOException {
        HttpPost httpPost = new HttpPost(baseUrl + url);
        // 拼接参数
        if (json != null && !"".equals(json)) {
            StringEntity requestEntity = new StringEntity(json, CHARSET);
            requestEntity.setContentType("application/json");
            httpPost.setEntity(requestEntity);
        }
        // 添加headers
        addHeaders(httpPost, headers);
        // 添加cookies
        addCookies(httpPost, cookies);
        HttpClientContext httpClientContext = new HttpClientContext();
        CloseableHttpClient httpClient = getHttpClient();
        try (CloseableHttpResponse response = httpClient.execute(httpPost, httpClientContext)) {
            return getResEntity(response, false);
        } finally {
            if (!connectionPool.get()) {
                httpClient.close();
            }
        }
    }

    /**
     * PUT提交JSON基础方法
     *
     * @param url
     * @param headers
     * @param json
     * @return cn.xanderye.util.HttpUtil.ResEntity
     * @author XanderYe
     * @date 2020/2/4
     */
    public static ResEntity doPutJSON(String url, Map<String, Object> headers, Map<String, Object> cookies, String json) throws IOException {
        HttpPut httpPut = new HttpPut(baseUrl + url);
        // 拼接参数
        if (json != null && !"".equals(json)) {
            StringEntity requestEntity = new StringEntity(json, CHARSET);
            requestEntity.setContentType("application/json");
            httpPut.setEntity(requestEntity);
        }
        // 添加headers
        addHeaders(httpPut, headers);
        // 添加cookies
        addCookies(httpPut, cookies);
        HttpClientContext httpClientContext = new HttpClientContext();
        CloseableHttpClient httpClient = getHttpClient();
        try (CloseableHttpResponse response = httpClient.execute(httpPut, httpClientContext)) {
            return getResEntity(response, false);
        } finally {
            if (!connectionPool.get()) {
                httpClient.close();
            }
        }
    }

    /**
     * POST提交XML基础方法
     *
     * @param url
     * @param headers
     * @param xml
     * @return cn.xanderye.util.HttpUtil.ResEntity
     * @author XanderYe
     * @date 2020/2/4
     */
    public static ResEntity doPostXML(String url, Map<String, Object> headers, Map<String, Object> cookies, String xml) throws IOException {
        HttpPost httpPost = new HttpPost(baseUrl + url);
        // 拼接参数
        if (xml != null && !"".equals(xml)) {
            StringEntity requestEntity = new StringEntity(xml, CHARSET);
            requestEntity.setContentEncoding(CHARSET);
            requestEntity.setContentType("application/xml");
            httpPost.setEntity(requestEntity);
        }
        // 添加headers
        addHeaders(httpPost, headers);
        // 添加cookies
        addCookies(httpPost, cookies);
        HttpClientContext httpClientContext = new HttpClientContext();
        CloseableHttpClient httpClient = getHttpClient();
        try (CloseableHttpResponse response = httpClient.execute(httpPost, httpClientContext)) {
            return getResEntity(response, false);
        } finally {
            if (!connectionPool.get()) {
                httpClient.close();
            }
        }
    }


    /**
     * get下载基础方法
     *
     * @param url
     * @param headers
     * @param cookies
     * @param params
     * @return cn.xanderye.util.HttpUtil.ResEntity
     * @author XanderYe
     * @date 2020/2/4
     */
    public static ResEntity doDownload(String url, Map<String, Object> headers, Map<String, Object> cookies, Map<String, Object> params) throws IOException {
        url = baseUrl + url;
        // 拼接参数
        if (params != null && !params.isEmpty()) {
            List<NameValuePair> pairs = new ArrayList<>(params.size());
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                String value = entry.getValue() == null ? null : (entry.getValue()).toString();
                if (value != null) {
                    pairs.add(new BasicNameValuePair(entry.getKey(), value));
                }
            }
            try {
                String parameters = EntityUtils.toString(new UrlEncodedFormEntity(pairs, CHARSET));
                String symbol = url.contains("?") ? "&" : "?";
                // 判断是否已带参数
                url += symbol + parameters;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        HttpGet httpGet = new HttpGet(url);
        // 添加headers
        addHeaders(httpGet, headers);
        // 添加cookies
        addCookies(httpGet, cookies);
        HttpClientContext httpClientContext = new HttpClientContext();
        CloseableHttpClient httpClient = getHttpClient();
        try (CloseableHttpResponse response = httpClient.execute(httpGet, httpClientContext)) {
            return getResEntity(response, true);
        } finally {
            if (!connectionPool.get()) {
                httpClient.close();
            }
        }
    }

    /**
     * post上传基础方法，注意Content-Type
     *
     * @param url
     * @param headers
     * @param cookies
     * @param bytes
     * @return cn.xanderye.util.HttpUtil.ResEntity
     * @author XanderYe
     * @date 2020/2/4
     */
    public static ResEntity doUpload(String url, Map<String, Object> headers, Map<String, Object> cookies, byte[] bytes) throws IOException {
        HttpPost httpPost = new HttpPost(baseUrl + url);
        // 拼接参数
        if (bytes != null && bytes.length > 0) {
            ByteArrayEntity requestEntity = new ByteArrayEntity(bytes);
            httpPost.setEntity(requestEntity);
        }
        // 添加headers
        addHeaders(httpPost, headers);
        // 添加cookies
        addCookies(httpPost, cookies);
        HttpClientContext httpClientContext = new HttpClientContext();
        CloseableHttpClient httpClient = getHttpClient();
        try (CloseableHttpResponse response = httpClient.execute(httpPost, httpClientContext)) {
            return getResEntity(response, false);
        } finally {
            if (!connectionPool.get()) {
                httpClient.close();
            }
        }
    }

    /**
     * 获取请求返回对象
     * @param response
     * @return cn.xanderye.util.HttpUtil.ResEntity
     * @author XanderYe
     * @date 2021/8/30
     */
    private static ResEntity getResEntity(CloseableHttpResponse response, boolean binary) throws IOException {
        int statusCode = response.getStatusLine().getStatusCode();
        ResEntity resEntity = new ResEntity();
        resEntity.setStatusCode(statusCode);
        resEntity.setHeaders(getHeaders(response));
        resEntity.setCookies(formatCookies(getCookieString(response)));
        HttpEntity resultEntity = response.getEntity();
        if (resultEntity != null) {
            if (binary) {
                byte[] bytes = EntityUtils.toByteArray(resultEntity);
                resEntity.setBytes(bytes);
            } else {
                String res = EntityUtils.toString(resultEntity, CHARSET);
                resEntity.setResponse(res);
            }
            EntityUtils.consume(resultEntity);
        }
        return resEntity;
    }

    /**
     * 添加cookie
     *
     * @param httpRequestBase
     * @return void
     * @author XanderYe
     * @date 2020-03-15
     */
    private static void addHeaders(HttpRequestBase httpRequestBase, Map<String, Object> headers) {
        // 设置默认UA
        httpRequestBase.setHeader("User-Agent", DEFAULT_USER_AGENT);
        if (headers != null && !headers.isEmpty()) {
            for (Map.Entry<String, Object> entry : headers.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue() == null ? "" : String.valueOf(entry.getValue());
                httpRequestBase.setHeader(key, value);
            }
        }
    }

    /**
     * 添加cookie
     *
     * @param cookies
     * @return void
     * @author XanderYe
     * @date 2020-03-15
     */
    private static void addCookies(HttpRequestBase httpRequestBase, Map<String, Object> cookies) {
        if (cookies != null && !cookies.isEmpty()) {
            StringBuilder stringBuilder = new StringBuilder();
            for (Map.Entry<String, Object> entry : cookies.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue() == null ? "" : String.valueOf(entry.getValue());
                stringBuilder.append(key).append("=").append(value).append("; ");
            }
            httpRequestBase.addHeader("Cookie", stringBuilder.toString());
        }
    }

    /**
     * 从请求头中获取cookie字符串
     * @param response
     * @return java.lang.String
     * @author XanderYe
     * @date 2021/1/26
     */
    private static String getCookieString(CloseableHttpResponse response) {
        Header[] headers = response.getHeaders("Set-Cookie");
        return Arrays.stream(headers).map(header -> {
            String valueStr = header.getValue();
            if (valueStr != null) {
                String[] valueStrArray = valueStr.split(";");
                if (valueStrArray.length > 0) {
                    return valueStrArray[0];
                }
            }
            return valueStr;
        }).collect(Collectors.joining("; "));
    }

    /**
     * 获取请求头
     * @param response
     * @return java.util.Map<java.lang.String,java.lang.Object>
     * @author XanderYe
     * @date 2021/6/21
     */
    private static Map<String, Object> getHeaders(CloseableHttpResponse response) {
        Header[] headers = response.getAllHeaders();
        Map<String, Object> headersMap = new HashMap<>();
        for (Header header : headers) {
            headersMap.put(header.getName(), header.getValue());
        }
        return headersMap;
    }

    /**
     * 格式化请求头
     *
     * @param headerString
     * @return java.util.Map<java.lang.String, java.lang.Object>
     * @author XanderYe
     * @date 2020/4/1
     */
    public static Map<String, Object> formatHeaders(String headerString) {
        if (headerString != null && !"".equals(headerString)) {
            String[] headers = headerString.split(";");
            if (headers.length > 0) {
                Map<String, Object> headerMap = new HashMap<>(16);
                for (String header : headers) {
                    int index = header.indexOf(":");
                    if (index > 0) {
                        String k = header.substring(0, index).trim();
                        String v = header.substring(index + 1).trim();
                        headerMap.put(k, v);
                    }
                }
                return headerMap;
            }
        }
        return null;
    }

    /**
     * 格式化cookie
     *
     * @param cookieString
     * @return java.util.Map<java.lang.String, java.lang.Object>
     * @author XanderYe
     * @date 2020/4/1
     */
    public static Map<String, Object> formatCookies(String cookieString) {
        Map<String, Object> cookieMap = new HashMap<>(16);
        if (cookieString != null && !"".equals(cookieString)) {
            String[] cookies = cookieString.split(";");
            if (cookies.length > 0) {
                for (String parameter : cookies) {
                    int eqIndex = parameter.indexOf("=");
                    if (eqIndex > -1) {
                        String k = parameter.substring(0, eqIndex).trim();
                        String v = parameter.substring(eqIndex + 1).trim();
                        if (!"".equals(v)) {
                            cookieMap.put(k, v);
                        }
                    }
                }
            }
        }
        return cookieMap;
    }

    /**
     * 格式化请求体
     *
     * @param parameterString
     * @return java.util.Map<java.lang.String, java.lang.Object>
     * @author XanderYe
     * @date 2020/4/1
     */
    public static Map<String, Object> formatParameters(String parameterString) {
        if (parameterString != null) {
            String[] parameters = parameterString.split("&");
            if (parameters.length > 0) {
                Map<String, Object> paramMap = new HashMap<>(16);
                for (String parameter : parameters) {
                    String[] value = parameter.split("=");
                    String k = value[0].trim();
                    String v = null;
                    if (value.length == 2) {
                        v = value[1].trim();
                    }
                    paramMap.put(k, v);
                }
                return paramMap;
            }
        }
        return null;
    }

    /**
     * cookie转字符串
     * @param cookieMap
     * @return java.lang.String
     * @author XanderYe
     * @date 2022/3/29
     */
    public static String cookiesToString(Map<String, Object> cookieMap) {
        if (cookieMap == null || cookieMap.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Object> entry : cookieMap.entrySet()) {
            sb.append(entry.getKey()).append("=").append(entry.getValue()).append("; ");
        }
        return sb.toString();
    }

    /**
     * 忽略证数配置
     *
     * @param
     * @return cz.msebera.android.httpclient.conn.ssl.SSLConnectionSocketFactory
     * @author XanderYe
     * @date 2020/2/14
     */
    private static SSLConnectionSocketFactory ignoreCertificates() {
        try {
            SSLContext sslContext = new SSLContextBuilder().loadTrustMaterial(null, (chain, authType) -> true).build();
            return new SSLConnectionSocketFactory(sslContext);
        } catch (NoSuchAlgorithmException | KeyManagementException | KeyStoreException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 重试配置
     * @param
     * @return cz.msebera.android.httpclient.client.HttpRequestRetryHandler
     * @author XanderYe
     * @date 2021/6/24
     */
    private static HttpRequestRetryHandler retryHandler() {
        return (e, retryTimes, httpContext) -> {
            if (retryTimes > retryCount) {
                // 重试次数大于3次
                return false;
            }
            HttpClientContext clientContext = HttpClientContext.adapt(httpContext);
            HttpRequest request = clientContext.getRequest();
            boolean idempotent = !(request instanceof HttpEntityEnclosingRequest);
            if (idempotent) {
                // 如果请求被认为是幂等的，则重试
                return true;
            }
            if (e instanceof NoHttpResponseException) {
                // NoHttpResponseException异常重试
                return true;
            }
            if (e instanceof ConnectTimeoutException) {
                // 连接超时重试
                return true;
            }
            if (e instanceof SocketTimeoutException) {
                // 响应超时
                return false;
            }
            if (e instanceof InterruptedIOException) {
                // 超时
                return false;
            }
            if (e instanceof UnknownHostException) {
                // 未知主机
                return false;
            }
            if (e instanceof SSLException) {
                // SSL异常
                return false;
            }
            return false;
        };
    }

    /**
     * 设置重定向
     * @param customRedirect
     * @return void
     * @author XanderYe
     * @date 2021/5/24
     */
    public static void setRedirect(boolean customRedirect) {
        redirect = customRedirect;
    }

    /**
     * 配置代理
     * @param customProxyIp
     * @param customProxyPort
     * @return void
     * @author XanderYe
     * @date 2021/5/24
     */
    public static void setProxy(String customProxyIp, Integer customProxyPort) {
        if (null != customProxyIp && !"".equals(customProxyIp) && null != customProxyPort) {
            enableProxy = true;
            proxyIp = customProxyIp;
            proxyPort = customProxyPort;
        }
    }

    /**
     * 设置超时
     * @param customConnectTimeout
     * @param customSocketTimeout
     * @return void
     * @author XanderYe
     * @date 2021/5/24
     */
    public static void setTimeout(int customConnectTimeout, int customSocketTimeout) {
        connectTimeout = customConnectTimeout;
        socketTimeout = customSocketTimeout;
    }

    /**
     * 设置重试机制
     * @param customRetry
     * @return void
     * @author XanderYe
     * @date 2021/6/24
     */
    public static void setRetry(boolean retry, int customRetry) {
        enableRetry = retry;
        if (customRetry > 0) {
            retryCount = customRetry;
        }
    }

    /**
     * 启用连接池
     * @param customMaxTotal
     * @param customMaxPerRoute
     * @param customIdleTimeout
     * @param customMonitorPeriod
     * @return void
     * @author XanderYe
     * @date 2022/1/21
     */
    public static void enableConnectionPool(int customMaxTotal, int customMaxPerRoute, int customIdleTimeout, int customMonitorPeriod) {
        maxTotal = customMaxTotal;
        maxPerRoute = customMaxPerRoute;
        idleTimeout = customIdleTimeout;
        monitorPeriod = customMonitorPeriod;
        enableConnectionPool();
    }

    /**
     * 启用连接池
     * @return void
     * @author XanderYe
     * @date 2022/1/21
     */
    public static void enableConnectionPool() {
        initPoolingHttpClient();
    }

    /**
     * 停用连接池
     * @param
     * @return void
     * @author XanderYe
     * @date 2022/1/21
     */
    public static synchronized void disableConnectionPool() {
        try {
            connectionPool.set(false);
            httpClient.close();
            httpClient = null;
            connectionManager = null;
            monitorExecutor.shutdown();
            monitorExecutor = null;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void setBaseUrl(String base) {
        baseUrl = base;
        if ('/' != base.charAt(base.length() - 1)) {
            baseUrl += "/";
        }
    }

    public static String getBaseUrl() {
        return baseUrl;
    }

    @Data
    public static class ResEntity {
        
        private Integer statusCode;

        private byte[] bytes;

        private String response;

        private Map<String, Object> headers;

        private Map<String, Object> cookies;
    }
}
