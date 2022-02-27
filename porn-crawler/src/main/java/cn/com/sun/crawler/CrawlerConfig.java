package cn.com.sun.crawler;

import cn.com.sun.crawler.impl.PornCrawler;
import cn.com.sun.crawler.impl.PornyCrawler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

public class CrawlerConfig {
    public static final String EXT = ".mp4";
    public static final String BROWSER_PATH = "C://Program Files//Google//Chrome//Application//chrome.exe";
    public static final String HTTP_PROXY_HOSTNAME = "localhost";
    public static final String FILE_SAVE_PATH = "E://Download//crawler//";
    public static final String AUTHOR_PATH = "E://Download//crawler//author//";
    public static final String KEYWORD_PATH = "E://Download//crawler//keyword//";
    public static final File JSON = new File(FILE_SAVE_PATH + "crawler.json");
    public static final String KEYWORD_URL = "/search_result.php";
    //__cfduid=da2d24a1298789d9a151168cb3efaba2a1611119632; CLIPSHARE=5f6vnld23jeg0tnduk98s64id3;
    // __utmz=50351329.1608039490.1.1.utmcsr=(direct)|utmccn=(direct)|utmcmd=(none); CLIPSHARE=rpaabtk6gcqgif0f6ov7ia4i9b; __utma=50351329.1142673181.1608039490.1629944937.1629953587.18; __utmb=50351329.0.10.1629953587; __utmc=50351329; 91username=9c4a7VQrwhfwvdvN4m6Y3TAKeiRW30dIpygVx9WGAVU2QAnug19p; DUID=a735khT6Vo%2FgX0Tca%2F48ermRadyAh04GIdWpS%2FOJz0FCj88h; USERNAME=f42bw0JzWqwobR3pccSZm21lw%2BG9OWmi35cCBdWOERm5n8fx4jjE; EMAILVERIFIED=yes; school=6ffadWWinIjUUwUQOG2bxJvQzJEULQtKY2qK%2Bxw; level=15a2Xvtxj1kgdBxQZ68%2FyCbFIKBSHR05Nc0oRkUT
    public static final String COOKIE = "91username=9c4a7VQrwhfwvdvN4m6Y3TAKeiRW30dIpygVx9WGAVU2QAnug19p;";
    public static final int CONNECT_TIMEOUT = 10000;
    public static final int READ_TIMEOUT = 5000;
    public static final int READ_FILE_TIMEOUT = 60000;
    /**
     * 请求失败重新尝试请求的次数
     */
    public static final int REQUEST_RETRY_COUNT = 8;
    /**
     * 本机代理端口，若为-1，则不走代理
     */
    public static final int HTTP_PROXY_PORT = 58888;
    private static final Logger logger = LoggerFactory.getLogger(CrawlerConfig.class);
    public static String stage = "parseVideoBaseInfo";
    public static String domain;
    public static String useCookieUrl;
    public static String homePage;
    public static String hot;
    public static String recentHighlight;
    public static String monthHot;
    public static String monthStore;
    public static String monthDiscuss;
    public static String lastMonthHot;

    /**
     * crawler类型
     */
    public static Class crawler = PornCrawler.class;
    public static String[] pages;
    public static String[] daily;
    public static String[] allLastMonthHot = new String[5];
    public static String[] allMonthHot = new String[5];
    public static String[] topTenMonthStore = new String[10];
    public static String[] author = new String[3];
    public static String[] keywords = new String[3];
    public static String authorName = "";
    public static File workspace;
    public static String keyword = "";

    static {
        initPages();
        pages = daily;
        //pages = new String[]{"http://0728.91p50.com/uvideos.php?UID=b41cJANU7wyUhcQnmUc3AveGBv3rT35NOuM8Qj6x449R6vj1&type=public&page=4"};
        initWorkspace();
    }

    static void initPages() {
        if (crawler == PornCrawler.class) {
            //http://91.91p07.com
            //http://0728.91p50.com
            domain = "http://91porn.com";
            homePage = domain + "/index.php";
            hot = domain + "/v.php?category=hot&viewtype=basic";
            recentHighlight = domain + "/v.php?category=rf&viewtype=basic";
            monthHot = domain + "/v.php?category=top&viewtype=basic";
            monthStore = domain + "/v.php?category=tf&viewtype=basic";
            monthDiscuss = domain + "/v.php?category=md&viewtype=basic";
            lastMonthHot = domain + "/v.php?category=top&m=-1&viewtype=basic";
            daily = new String[]{homePage, hot, recentHighlight, monthHot, monthStore, monthDiscuss, lastMonthHot};
            /*homePage,*/
        } else if (crawler == PornyCrawler.class) {
            domain = "https://91porny.com";
            hot = domain + "/video/category/hot-list";
            recentHighlight = domain + "/video/category/recent-favorite";
            monthHot = domain + "/video/category/top-list";
            monthStore = domain + "/video/category/top-favorite";
            monthDiscuss = domain + "/video/category/month-discuss";
            lastMonthHot = domain + "/video/category/top-last";
            daily = new String[]{hot, recentHighlight, monthHot, monthStore, monthDiscuss, lastMonthHot};
        }
        String query = "&page=";
        if (crawler == PornyCrawler.class) {
            query = "/";
        }
        for (int i = 0; i < allMonthHot.length; i++) {
            allMonthHot[i] = String.format("%s%s%s", monthHot, query, i + 1);
        }
        for (int i = 0; i < allLastMonthHot.length; i++) {
            allLastMonthHot[i] = String.format("%s%s%s", lastMonthHot, query, i + 1);
        }
        for (int i = 0; i < topTenMonthStore.length; i++) {
            topTenMonthStore[i] = String.format("%s%s%s", monthStore, query, i + 1);
        }
        if (!authorName.isEmpty()) {
            try {
                for (int i = 0; i < author.length; i++)
                    //AUTHOR[i] = String.format("%s/search?keywords=%s&page=%s", DOMAIN, URLEncoder.encode(authorName, "UTF-8"), i + 1);
                    author[i] = String.format("%s/author?keywords=%s&page=%s", domain, URLEncoder.encode(authorName, "UTF-8"), i + 1);
            } catch (UnsupportedEncodingException e) {
                logger.error(e.getMessage(), e);
            }
        }
        if (!keyword.isEmpty()) {
            useCookieUrl = domain + "/search_result.php";
            try {
                for (int i = 0; i < keywords.length; i++)
                    //AUTHOR[i] = String.format("%s/search?keywords=%s&page=%s", DOMAIN, URLEncoder.encode(authorName, "UTF-8"), i + 1);
                    keywords[i] = String.format("%s%s?search_id=%s&search_type=search_videos&sort=favorite&page=%s", domain, CrawlerConfig.KEYWORD_URL, URLEncoder.encode(keyword, "UTF-8"), i + 1);
            } catch (UnsupportedEncodingException e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    static void initWorkspace() {
        // 存储目录信息
        if (Arrays.stream(pages).allMatch(p -> p.contains("/author"))) {
            workspace = new File(CrawlerConfig.AUTHOR_PATH + CrawlerConfig.authorName);
        } else if (Arrays.stream(pages).allMatch(p -> p.contains(CrawlerConfig.KEYWORD_URL))) {
            workspace = new File(CrawlerConfig.KEYWORD_PATH + CrawlerConfig.keyword);
        } else {
            // 创建对应日期的文件夹
            String date = LocalDate.now().format(DateTimeFormatter.ISO_DATE);
            workspace = new File(CrawlerConfig.FILE_SAVE_PATH + date);
        }
    }
}
