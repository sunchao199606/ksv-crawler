package cn.com.sun.crawler;

import cn.com.sun.crawler.entity.Video;
import cn.com.sun.crawler.util.FileAccessManager;
import cn.com.sun.crawler.util.HttpClient;
import com.google.common.io.Files;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public abstract class AbstractVideoCrawler implements VideoCrawler {
    private static final Logger logger = LoggerFactory.getLogger(AbstractVideoCrawler.class);

    protected List<Video> videoList;
    protected List<Video> downloadList = new ArrayList<>();

    @Override
    public VideoCrawler parseVideoBaseInfo() {
        List<Video> tempVideoList = new ArrayList<>();
        for (String pageUrl : CrawlerConfig.pages) {
            logger.info("crawler parse page :{} start", pageUrl);
            String htmlString = HttpClient.getHtmlByHttpClient(pageUrl);
            Map<String, Video> downloadedMap = FileAccessManager.getInstance().read();
            Document document = Jsoup.parse(htmlString);
            // id title pageUrl
            List<Video> list = getVideoBaseInfo(document).stream().filter(video -> {
                if (downloadedMap.keySet().contains(video.getId())) {
                    logger.info("filter downloaded video:{}", video.getTitle());
                    return false;
                } else {
                    return true;
                }
            }).collect(Collectors.toList());
            tempVideoList.addAll(list);
            logger.info("crawler parse page :{} end", pageUrl);
        }
        videoList = tempVideoList.stream().distinct().collect(Collectors.toList());
        if (videoList.size() > 0) {
            logger.info("video list：↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓");
            videoList.forEach(video -> logger.info(video.toString()));
            logger.info("video list：↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑");
        }
        return this;
    }


    @Override
    public void download() {
        AtomicInteger num = new AtomicInteger(0);
        ThreadFactory threadFactory = r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            t.setName("download-processor-" + num.incrementAndGet());
            return t;
        };
        // 无限任务队列
        BlockingQueue<Runnable> linkedBlockingQueue = new LinkedBlockingQueue<>(Integer.MAX_VALUE);
        ThreadPoolExecutor executor = new ThreadPoolExecutor(16, Integer.MAX_VALUE, 5, TimeUnit.MINUTES,
                linkedBlockingQueue, threadFactory);
        AtomicInteger succeeded = new AtomicInteger(0);
        AtomicInteger failed = new AtomicInteger(0);
        for (Video video : downloadList) {
            Callable<Boolean> downloadTask = createDownloadTask(video);
            Future<Boolean> result = executor.submit(downloadTask);
            Runnable monitorTask = () -> {
                try {
                    if (result.get().booleanValue()) {
                        FileAccessManager.getInstance().write(video);
                        succeeded.incrementAndGet();
                    } else {
                        failed.incrementAndGet();
                    }
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            };
            executor.submit(monitorTask);
        }

        Thread downloadMonitor = new Thread(() -> {
            while (executor.getActiveCount() != 0) {
                try {
                    Thread.sleep(15000);
                    logger.info("total task:{}; succeeded task:{}; failed task:{}", downloadList.size(), succeeded, failed);
                } catch (InterruptedException e) {
                    logger.error(e.getMessage(), e);
                }
            }
        });
        downloadMonitor.setName("download-monitor");
        downloadMonitor.start();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            copyRecordFile();
        }));
    }

    private void copyRecordFile() {
        String downloadedPath = System.getProperty("user.home") + "\\crawler.json";
        File toDownloaded = new File(downloadedPath);
        try {
            Files.copy(CrawlerConfig.JSON, toDownloaded);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
    }

    /**
     * 创建下载任务
     *
     * @param video
     * @return
     */
    protected abstract Callable<Boolean> createDownloadTask(Video video);

    /**
     * 爬取页面中全部视频基础信息
     *
     * @param document
     * @return
     */
    public abstract List<Video> getVideoBaseInfo(Document document);
}
