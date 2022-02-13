package cn.com.sun.crawler.impl;

import cn.com.sun.crawler.AbstractVideoCrawler;
import cn.com.sun.crawler.CrawlerConfig;
import cn.com.sun.crawler.VideoCrawler;
import cn.com.sun.crawler.entity.Video;
import cn.com.sun.crawler.util.HttpClient;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

public class PornyCrawler extends AbstractVideoCrawler {
    private static final Logger logger = LoggerFactory.getLogger(PornyCrawler.class);
    private static final String URL_PREFIX = "https://91porny.com";

    @Override
    public List<Video> getVideoBaseInfo(Document document) {
        Elements elements = document.select(".colVideoList").select(".video-elem");
        List<Video> videoList = new ArrayList<>();
        for (Element content : elements) {
            Video video = new Video();
            Element first = content.select("a").select(".display").first();
            Element second = content.select("a").select(".title").first();
            Element third = content.select("a").select(".text-dark").first();
            // id
            String style = first.select(".img").first().attr("style");
            int start = style.lastIndexOf("_");
            int end = style.lastIndexOf(".jpg");
            String id = "";
            if (start == -1) {
                start = style.lastIndexOf("/") + 1;
                id = "playvthumb_" + style.substring(start, end);
            } else {
                id = "playvthumb" + style.substring(start, end);
            }
            video.setId(id);
            // href
            String pageUrl = URL_PREFIX + first.attr("href");
            video.setHref(pageUrl);
            // duration
            Element duration = first.select(".layer").first();
            String durationStr = duration.text().trim();
            int minutes = Integer.parseInt(durationStr.split(":")[0]);
            int seconds = Integer.parseInt(durationStr.split(":")[1]);
            //Duration duration = Duration.ofSeconds(minutes * 60 + seconds);
            video.setDuration(minutes * 60 + seconds);
            // title
            video.setTitle(second.text());
            // author
            video.setAuthor(third.text());
//            // watchNum
//            video.setWatchNum(Integer.parseInt(text.split(" ")[1].trim()));
//            // storeNum
//            video.setStoreNum(Integer.parseInt(text.split(" ")[2].trim()));
            videoList.add(video);
        }
        return videoList;
    }

    @Override
    public VideoCrawler parseVideoExtInfo() {
        return this;
    }

    @Override
    public VideoCrawler parseDownloadUrl() {
        for (Video video : videoList) {
            if (!parseVideoDownloadInfo(video)) {
                logger.warn("get {} download url failed", video.getTitle());
                continue;
            }
            logger.info("get {} download url: {}", video.getTitle(), video.getDownloadUrl());
            downloadList.add(video);
        }
        return this;
    }

    private boolean parseVideoDownloadInfo(Video video) {
        String htmlString = HttpClient.getHtmlByHttpClient(video.getHref());
        Document document = Jsoup.parse(htmlString);
        String downloadUrl = getDownloadUrl(document);
        if ("".equals(downloadUrl)) {
            return false;
        }
        video.setDownloadUrl(downloadUrl);
        return true;
    }

    private String getDownloadUrl(Document document) {
        String downloadUrl = "";
        // 1 video标签获取
        Element videoSource = document.select("#video-play").select("source").first();
        if (videoSource != null) {
            if (videoSource.attr("src").contains(".mp4")) {
                downloadUrl = videoSource.attr("src");
                return downloadUrl;
            }
            //logger.info("get video {} download url by page：{}", video.getTitle(), downloadUrl);
        }
        // 2 下载链接获取
        Element downloadDiv = document.select("#videoShowTabDownload").first();
        if (downloadDiv != null && !downloadDiv.children().isEmpty()) {
            Element link = downloadDiv.children().first();
            if (link != null)
                downloadUrl = link.attr("href");
        }
        return downloadUrl;
    }

    @Override
    protected Callable<Boolean> createDownloadTask(Video video) {
        return () -> HttpClient.downloadVideoToFs(video, CrawlerConfig.workspace);
    }
}
