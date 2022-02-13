package cn.com.sun.crawler.util;

import cn.com.sun.crawler.CrawlerConfig;
import cn.com.sun.crawler.entity.Video;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.time.Duration;
import java.time.LocalTime;
import java.util.Random;
import java.util.zip.GZIPInputStream;

import static cn.com.sun.crawler.util.CrawlerUtil.filterBannedChar;

/**
 * @Description : HTTP客户端工具
 * @Author : Mockingbird
 * @Date: 2020-07-18 00:34
 */
public class HttpClient {

    public static final Logger logger = LoggerFactory.getLogger(HttpClient.class);

    private static CloseableHttpClient httpClient;

    private static RequestConfig requestConfig;

    static {
        PoolingHttpClientConnectionManager conManager = new PoolingHttpClientConnectionManager();
        // 设置整个池子的最大连接数
        conManager.setMaxTotal(50);
        // 设置连接到单个路由地址的最大连接数
        conManager.setDefaultMaxPerRoute(8);
        //conManager.
        httpClient = HttpClients.custom().setConnectionManager(conManager).build();
        requestConfig = RequestConfig.custom().setConnectTimeout(CrawlerConfig.CONNECT_TIMEOUT)
                .setSocketTimeout(CrawlerConfig.READ_TIMEOUT).build();
        //发送Get请求
        if (CrawlerConfig.HTTP_PROXY_PORT != -1) {
            HttpHost proxy = new HttpHost(CrawlerConfig.HTTP_PROXY_HOSTNAME, CrawlerConfig.HTTP_PROXY_PORT);
            requestConfig = RequestConfig.copy(requestConfig).setProxy(proxy).build();
        }
    }

    /**
     * 使用HttpURLConnection获取指定url的返回内容
     *
     * @param urlStr
     * @return
     */
    public static String getHtmlByConnection(String urlStr) {
        String html = "";
        logger.info("request:{} ", urlStr);
        try (InputStream in = getConnection(urlStr).getInputStream(); ByteArrayOutputStream out =
                new ByteArrayOutputStream()) {
            GZIPInputStream gzipIn = new GZIPInputStream(in);
            IOUtil.copy(gzipIn, out);
            html = out.toString();
            logger.debug("response content:{} ", html);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
        return html;
    }

    /**
     * 使用HttpClient获取指定url的返回
     *
     * @param url
     * @return
     */
    public static String getHtmlByHttpClient(String url) {
        String html = "";
        //发送Get请求
        boolean useCookie = false;
        if (null != CrawlerConfig.useCookieUrl && url.contains(CrawlerConfig.useCookieUrl) && "parseVideoBaseInfo".equals(CrawlerConfig.stage)) {
            useCookie = true;
        }
        HttpGet request = createHttpGetRequest(url, useCookie);
        logger.info("begin request : {}", url);
        for (int tryCount = 1; tryCount <= CrawlerConfig.REQUEST_RETRY_COUNT; tryCount++) {
            logger.info("try count：" + tryCount);
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                if (response.getStatusLine().getStatusCode() != 200) {
                    throw new Exception("response status:" + response.getStatusLine());
                }
                // 从响应模型中获取响应实体
                HttpEntity responseEntity = response.getEntity();
                if (responseEntity != null) {
                    logger.info("response status:{} ", response.getStatusLine());
                    html = EntityUtils.toString(responseEntity);
                    break;
                }
            } catch (Exception e) {
                logger.error(e.getMessage());
                continue;
            }
        }
        if (html.isEmpty()) {
            logger.error("request : {} failed", url);
        }
        return html;
    }

    /**
     * 通过http方式将远程资源下载到本地文件
     *
     * @param video
     * @return
     */
    public static boolean downloadVideoToFs(Video video, File dir) {
        // 请求
        HttpGet request = createHttpGetRequest(video.getDownloadUrl());
        request.setConfig(RequestConfig.copy(requestConfig).setSocketTimeout(CrawlerConfig.READ_FILE_TIMEOUT).build());
        if (!dir.exists()) {
            dir.mkdir();
        }
        // 下载
        String fileName = filterBannedChar(video.getTitle());
        String filePath = dir.getPath() + File.separator + fileName + CrawlerConfig.EXT;
        File file = new File(filePath);
        LocalTime startTime;
        try {
            CloseableHttpResponse response = httpClient.execute(request);
            try {
                if (response.getStatusLine().getStatusCode() == 200) {
                    FileOutputStream out = new FileOutputStream(file);
                    InputStream in = response.getEntity().getContent();
                    int bytes = Integer.parseInt(response.getFirstHeader("Content-Length").getValue());
                    float fileSize = ((float) bytes) / (1024 * 1024);
                    logger.info("download file start: name:{},size:{} {},url:{}", fileName, bytes + "byte"
                            , fileSize + "m", video.getDownloadUrl());
                    startTime = LocalTime.now();
                    IOUtil.copy(in, out);
                    LocalTime endTime = LocalTime.now();
                    String costTime = Duration.between(startTime, endTime).getSeconds() + "s";
                    logger.info("download file success: name:{},cost time:{}", fileName, costTime);
                } else {
                    logger.error("download file failed, name:{}, cause:response status:{}", fileName, response.getStatusLine());
                    return false;
                }
            } catch (IOException e) {
                //下载失败之后忽略异常继续执行
                logger.error("download file failed: name:{} , cause:{}", fileName, e.getMessage());
                return false;
            } finally {
                response.close();
            }
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            return false;
        }
        return true;
    }

    private static HttpGet createHttpGetRequest(String url) {
        return createHttpGetRequest(url, false);
    }

    private static HttpGet createHttpGetRequest(String urlStr, boolean useCookie) {
        HttpGet request = new HttpGet(urlStr);
        request.setConfig(requestConfig);
        request.setHeader("Host", request.getURI().getHost());
        request.setHeader("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8,zh-TW;q=0.7");
        request.setHeader("Accept-Encoding", "gzip");
        request.setHeader("Referer", urlStr);
        request.setHeader("X-Forwarded-For", randomIp());
        request.setHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/84.0.4147.89 Safari/537.36");
        if (useCookie) {
            request.setHeader("Cookie", CrawlerConfig.COOKIE);
        }
        return request;
    }

    private static HttpURLConnection getConnection(String urlStr) {
        HttpURLConnection con = null;
        URL url;
        try {
            Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(CrawlerConfig.HTTP_PROXY_HOSTNAME,
                    CrawlerConfig.HTTP_PROXY_PORT));
            url = new URL(urlStr);
            con = (HttpURLConnection) url.openConnection(proxy);
            System.out.println(url.getHost());
            // 请求的目标Host
            con.setRequestProperty("Host", url.getHost());
            // 客户端可理解的语言
            con.setRequestProperty("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8,zh-TW;q=0.7");
            // 客户端可理解的数据压缩方式
            con.setRequestProperty("Accept-Encoding", "gzip");
            // 当前请求页面的来源页面的地址
            con.setRequestProperty("Referer", urlStr);
            String ip = randomIp();
            con.setRequestProperty("X-Forwarded-For", ip);
            // 模拟浏览器访问
            con.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                    "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/84.0.4147" +
                    ".89 Safari/537.36");
            con.setConnectTimeout(CrawlerConfig.CONNECT_TIMEOUT);
            con.setReadTimeout(CrawlerConfig.READ_TIMEOUT);
            con.connect();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return con;
    }

    private static String randomIp() {
        Random random = new Random();
        StringBuilder ipBuilder = new StringBuilder("");
        for (int i = 0; i < 4; i++) {
            ipBuilder.append(random.nextInt(255)).append(".");
        }
        String ip = ipBuilder.deleteCharAt(ipBuilder.length() - 1).toString();
        //logger.debug("request ip:{}", ip);
        return ip;
    }
}
