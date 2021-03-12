package cn.com.sun.ksv.crawler;

import cn.com.sun.ksv.model.Video;
import cn.com.sun.ksv.util.FFMPEG;
import cn.com.sun.ksv.util.FileAccessManager;
import cn.com.sun.ksv.util.HttpClient;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ws.schild.jave.info.MultimediaInfo;
import ws.schild.jave.info.VideoSize;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import static org.openqa.selenium.support.ui.ExpectedConditions.attributeToBeNotEmpty;

/**
 * @Description :
 * @Author : mockingbird
 * @Date : 2021/3/9 14:09
 */
public class ShortVideoCrawler implements VideoCrawler {

    private static Logger logger = LoggerFactory.getLogger(ShortVideoCrawler.class);

    private List<String> urlList;

    private List<Video> filteredVideoList = new ArrayList<>();

    private List<Video> downloadVideoList = new ArrayList<>();

    private List<Video> successVideoList = new ArrayList<>();
    private List<Video> failedList = new ArrayList<>();

    private File outputDir;

    private WebDriver driver;

    private String date;

    private static String regEx = "[0-9]*.mp4";

    public ShortVideoCrawler(List<String> urlList) {
        this.urlList = urlList;
        initWebDriver();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        this.date = dateFormat.format(new Date());
        this.outputDir = new File(CrawlerConfig.getProperties("outputDir") + File.separator + date);
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }
    }

    private void initWebDriver() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.addArguments("--headless");
        options.setBinary(new File(CrawlerConfig.getProperties("browserPath")));
        this.driver = new ChromeDriver(options);
    }

    @Override
    public VideoCrawler parseVideoBaseInfo() {
        return null;
    }

    @Override
    public VideoCrawler parseVideoExtInfo() {
        return null;
    }

    @Override
    public VideoCrawler parseDownloadUrl() {
        Map<String, Video> downloadedMap = FileAccessManager.getInstance().read();
        // id title pageUrl
        urlList.stream().forEach(url -> {
            String id = url.substring(url.length() - 6);
            if (downloadedMap.keySet().contains(id)) {
                logger.info("filter downloaded video:{}", url);
            } else {
                Video video = new Video();
                video.setUrl(url);
                video.setId(id);
                video.setDate(date);
                filteredVideoList.add(video);
            }
        });

        if (!filteredVideoList.isEmpty()) {
            WebDriverWait wait = new WebDriverWait(driver, 5);
            logger.info("开始解析视频下载地址 总视频数量：{}", filteredVideoList.size());
            Random random = new Random();
            for (Video video : filteredVideoList) {
                float seed = random.nextFloat();
                float second = (seed / 0.5f) + 2;
                String videoUrl = "";
                try {
                    Thread.sleep((long) (second * 1000f));
                } catch (InterruptedException e) {
                    logger.error(e.getMessage(), e);
                }
                driver.get(video.getUrl());
                WebElement videoElement = driver.findElement(By.cssSelector("video"));
                try {
                    wait.until(attributeToBeNotEmpty(videoElement, "src"));
                    videoUrl = videoElement.getAttribute("src");
                } catch (Exception e) {
                    logger.info("获取视频下载地址失败：{}", video.getUrl());
                }
                if (!videoUrl.isEmpty()) {
                    video.setDownloadUrl(videoUrl);
                    downloadVideoList.add(video);
                    logger.info("获取视频下载地址成功：{}", video.getUrl());
                }
                //System.out.println(video.getAttribute("src"));
            }
            logger.info("解析视频下载地址完成 解析成功视频数量:{}", downloadVideoList.size());
            // 关闭driver
            driver.quit();
        }
        return this;
    }

    @Override
    public VideoCrawler download() {
        if (!downloadVideoList.isEmpty()) {
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
            logger.info("开始下载视频 视频数量：{}", downloadVideoList.size());
            for (int i = 0; i < downloadVideoList.size(); i++) {
                Video video = downloadVideoList.get(i);
                String filePath = outputDir + File.separator + video.getId() + ".mp4";
                Callable<Boolean> downloadTask = () -> HttpClient.downloadVideoToFs(video.getDownloadUrl(), filePath);
                Future<Boolean> result = executor.submit(downloadTask);
                Runnable monitorTask = () -> {
                    try {
                        if (result.get().booleanValue()) {
                            logger.info("下载视频成功,地址:{}", video.getDownloadUrl());
                            video.setPath(filePath);
                            FileAccessManager.getInstance().write(video);
                            successVideoList.add(video);
                            succeeded.incrementAndGet();
                        } else {
                            // 删除失败的视频
                            File file = new File(filePath);
                            if (file.exists()) {
                                file.delete();
                            }
                            failedList.add(video);
                            failed.incrementAndGet();
                        }
                    } catch (Exception e) {
                        logger.error(e.getMessage(), e);
                    }
                };
                executor.submit(monitorTask);
            }
            CountDownLatch countDownLatch = new CountDownLatch(1);
            Thread downloadMonitor = new Thread(() -> {
                while (executor.getActiveCount() != 0) {
                    try {
                        Thread.sleep(10000);
                        logger.info("全部视频数量:{}; 成功:{}; 失败:{}", downloadVideoList.size(), succeeded, failed);
                    } catch (InterruptedException e) {
                        logger.error(e.getMessage(), e);
                    }
                }
                countDownLatch.countDown();
            });
            downloadMonitor.setName("download-monitor");
            downloadMonitor.start();
            try {
                countDownLatch.await();
            } catch (InterruptedException e) {
                logger.error(e.getMessage(), e);
            }
        }
        failedList.forEach(url -> logger.error("下载失败: {}", url));
        return this;
    }

    public void processVideo() {
        // 文件名修改为数字编号
        int currentMaxNum = findMax();
        for (Video video : successVideoList) {
            File source = new File(video.getPath());
            MultimediaInfo info = FFMPEG.getVideoInfo(source);
            float actualDuration = (float) info.getDuration() / 1000f;
            VideoSize actualSize = info.getVideo().getSize();
            //1.时长处理
            if (actualDuration < 12.0f) {
                float factor = 12.5f / actualDuration;
                FFMPEG.extend(source, factor);
                logger.info("视频{}加长成功", source.getName());
            } else if (actualDuration > 120f) {
                FFMPEG.cut(source, "00:00:00", "00:00:119");
                logger.info("视频{}减短成功", source.getName());
            }
            //2.分辨率处理
            VideoSize expectSize = new VideoSize(720, 1280);
            if (!expectSize.asEncoderArgument().equals(actualSize.asEncoderArgument())) {
                FFMPEG.resize(source, expectSize.asEncoderArgument());
                logger.info("视频{}分辨率修改成功", source.getName());
            }
            //3.编号
            String fileName = source.getName();
            if (fileName.length() == 10) {
                // 英文 修改文件名
                File target = new File(outputDir + File.separator + (++currentMaxNum) + ".mp4");
                if (source.renameTo(target)) logger.info("{}重命名为{}", source.getName(), target.getName());
            }
        }
    }

    private int findMax() {
        int maxNum = 0;
        for (File file : outputDir.listFiles()) {
            String fileName = file.getName();
            if (Pattern.matches(regEx, fileName)) {
                // 数字判断最大值
                int num = Integer.parseInt(fileName.replaceAll(".mp4", ""));
                maxNum = num > maxNum ? num : maxNum;
            }
        }
        return maxNum;
    }
}
