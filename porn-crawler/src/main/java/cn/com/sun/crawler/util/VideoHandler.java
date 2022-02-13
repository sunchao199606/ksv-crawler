package cn.com.sun.crawler.util;

import cn.com.sun.crawler.AbstractVideoCrawler;
import cn.com.sun.crawler.CrawlerConfig;
import cn.com.sun.crawler.entity.Video;
import cn.com.sun.crawler.impl.PornyCrawler;
import cn.com.sun.crawler.m3u8.M3U8;
import com.google.common.collect.Lists;
import com.google.common.io.Files;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ws.schild.jave.Encoder;
import ws.schild.jave.EncoderException;
import ws.schild.jave.MultimediaObject;
import ws.schild.jave.encode.AudioAttributes;
import ws.schild.jave.encode.EncodingAttributes;
import ws.schild.jave.encode.VideoAttributes;
import ws.schild.jave.info.MultimediaInfo;
import ws.schild.jave.info.VideoInfo;
import ws.schild.jave.info.VideoSize;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static cn.com.sun.crawler.util.CrawlerUtil.filterBannedChar;

/**
 * @Description : 视频工具类
 * @Author : mockingbird
 * @Date : 2020/11/29 16:16
 */
public class VideoHandler {

    private static final Logger logger = LoggerFactory.getLogger(VideoHandler.class);

    private final Map<String, Video> recordMap;

    private final Map<String, List<File>> downloadedMap;

    public VideoHandler() {
        this.recordMap = FileAccessManager.getInstance().read();
        this.downloadedMap = getDownloadedMap();
    }

    /**
     * 获取视频时长
     *
     * @param source
     * @return
     */
    public static int getVideoDuration(File source) {
        MultimediaObject object = new MultimediaObject(source);
        MultimediaInfo info;
        try {
            info = object.getInfo();
            int second = (int) info.getDuration() / 1000;
            return second;
        } catch (EncoderException e) {
            logger.debug("parse error");
            return 0;
        }
    }

    public static void encode(File source) {
        encode(source, null, 600 * 1024);
    }

    public static void encode(File source, VideoSize size) {
        encode(source, size, 600 * 1024);
    }

    /**
     * 视频编码
     */
    public static void encode(File source, VideoSize size, Integer bitRate) {
        logger.info("encode file : {}", source.getPath());
        String tempPath = source.getParent() + File.separator + source.getName().replace(".mp4", "");
        File temp = new File(tempPath);
        MultimediaObject object = new MultimediaObject(source);

        AudioAttributes audio = new AudioAttributes();
        VideoAttributes video = new VideoAttributes();
        if (size != null) video.setSize(size);
        if (bitRate != null) video.setBitRate(bitRate);
        EncodingAttributes attrs = new EncodingAttributes();
        attrs.setEncodingThreads(4);
        attrs.setOutputFormat("mp4");
        attrs.setVideoAttributes(video);
        attrs.setAudioAttributes(audio);
        Encoder encoder = new Encoder();
        VideoInfo originVideoInfo = null;
        MultimediaInfo originMultimediaInfo = null;
        MultimediaInfo newMultimediaInfo = null;
        VideoInfo newVideoInfo = null;
        try {
            originMultimediaInfo = object.getInfo();
            originVideoInfo = originMultimediaInfo.getVideo();
            encoder.encode(object, temp, attrs);
            MultimediaObject newObj = new MultimediaObject(temp);
            newMultimediaInfo = newObj.getInfo();
            newVideoInfo = newMultimediaInfo.getVideo();
        } catch (EncoderException e) {
            logger.error(e.getMessage(), e);
        } finally {
            logger.info("before encode width:{} height:{} bitRate:{} frameRate:{} duration:{}s",
                    originVideoInfo.getSize().getWidth(), originVideoInfo.getSize().getHeight(),
                    originVideoInfo.getBitRate(), originVideoInfo.getFrameRate(), originMultimediaInfo.getDuration() / 1000);
            logger.info("after encode width:{} height:{} bitRate:{} frameRate:{} duration:{}s",
                    newVideoInfo.getSize().getWidth(), newVideoInfo.getSize().getHeight(), newVideoInfo.getBitRate(),
                    newVideoInfo.getFrameRate(), newMultimediaInfo.getDuration() / 1000);
        }
        File target = new File(tempPath + ".mp4");

        if (source.delete())
            temp.renameTo(target);
        else
            logger.error("视频重命名失败");
    }

    public static void main(String[] args) throws EncoderException {
        //VideoHandler.encode();
    }

    public static void listFiles(File dir, Consumer<File> fileConsumer) {
        for (File f : dir.listFiles()) {
            if (f.isFile()) {
                fileConsumer.accept(f);
            } else {
                listFiles(f, fileConsumer);
            }
        }
    }

    public static void listFiles(File dir, List<File> fileList) {
        for (File f : dir.listFiles()) {
            if (f.isFile()) {
                fileList.add(f);
            } else {
                listFiles(f, fileList);
            }
        }
    }

    public void storeByAuthor(String authorName, boolean move) {
        // 1 获取作者全部视频
        List<Video> authorAllVideoList = getAllVideoByAuthor(authorName);
        // 2 获取已下载视频
        List<File> selectedFileList = new ArrayList<>();
        List<String> repeatList = new ArrayList<>();
        List<String> notDownloadList = new ArrayList<>();
        authorAllVideoList.forEach(video -> {
            String videoName = video.getTitle() + CrawlerConfig.EXT;
            if (downloadedMap.keySet().contains(videoName)) {
                List<File> list = downloadedMap.get(videoName);
                if (list.size() == 1) {
                    selectedFileList.addAll(downloadedMap.get(videoName));
                } else if (list.size() > 1) {
                    repeatList.add(videoName);
                    logger.error("repeat file : {}", videoName);
                }
                return;
            } else {
                if (recordMap.keySet().contains(video.getId())) {
                    Video info = recordMap.get(video.getId());
                    String originName = video.getTitle() + CrawlerConfig.EXT;
                    List<File> list = downloadedMap.get(originName);
                    if (list == null) {
                        logger.error("not exist file: {}", originName);
                        return;
                    }
                    if (list.size() == 1) {
                        selectedFileList.addAll(downloadedMap.get(originName));
                        //logger.error("name changed: origin name:{},new name:{}", originName, videoName);
                    } else if (list.size() > 1) {
                        repeatList.add(videoName);
                        logger.error("repeat file : {}", videoName);
                    }
                    return;
                } else {
                    notDownloadList.add(videoName);
                    logger.error("not download file : {}", videoName);
                }
            }
        });
        // 3 移动视频
        if (move) {
            File destDir = new File("F:\\Download\\crawler\\author\\" + authorName);
            selectedFileList.forEach(file -> {
                logger.info("moving file : {}", file.getName());
                File dest = new File(destDir.getAbsolutePath() + File.separator + file.getName());
                if (dest.exists()) {
                    logger.info("has store file : {}", dest.getPath());
                    return;
                }
                IOUtil.move(file, dest);
                System.out.println(file.getName());
            });
        } else {
            notDownloadList.forEach(name -> logger.warn("not download file : {}", name));
            repeatList.forEach(name -> logger.warn("repeat file : {}", name));
            selectedFileList.forEach(file -> logger.info("selected file : {}", file.getAbsolutePath()));
        }

    }

    public void deleteSmallVideo(int limitSize) {
        File dir = new File("F:\\Download\\crawler");
        List<File> smallFileList = new ArrayList<>();
        listFiles(dir, f -> {
            //&& VideoUtil.getVideoDuration(f) > 90
            if (f.length() < limitSize) smallFileList.add(f);
        });
        Set<String> downloadedFileSet = FileAccessManager.getInstance().read().values().stream().map(video -> video.getTitle()).collect(Collectors.toSet());
//                map(key -> key.split("|")[1]).collect(Collectors.toSet());
        //downloadedFileSet.forEach(s -> System.out.println(s));
        List<File> deleteList = new ArrayList<>();

        smallFileList.forEach(file -> System.out.println(file.getName()));
        System.out.println(smallFileList.size());
        smallFileList.forEach(file -> {
            if (!downloadedFileSet.contains(file.getName())) deleteList.add(file);
        });

        deleteList.sort((f1, f2) -> ((Long) f1.lastModified()).compareTo(f2.lastModified()));
        deleteList.forEach(file -> System.out.println(file.getAbsolutePath()));
        System.out.println(deleteList.size());
//        smallFileList.forEach(f -> {
//            f.delete();
//            if (downloadedFileSet.contains(f.getName())) ;
//        });

    }

    public void deleteRepeatVideo() {
        Map<String, List<File>> videoMap = getDownloadedMap();
        //Map<String, List<File>> repeatVideoMap = new HashMap<>();
        videoMap.forEach((fileName, files) -> {
            if (files.size() > 1) {
//                if (files.size() == 2 && durationPredicate.test(files.get(0), files.get(1))) {
//                    repeatVideoMap.put(fileName, files);
//                }
                StringBuilder stringBuilder = new StringBuilder(fileName + ":");
                files.stream().map(file -> file.getAbsolutePath() + "|").forEach(stringBuilder::append);
                System.out.println(stringBuilder);
            }
        });
//        repeatVideoMap.values().stream().forEach(list -> {
//            if (list.get(0).length() < list.get(1).length()) {
//                list.get(0).delete();
//            } else {
//                list.get(1).delete();
//            }
//        });
    }

    private List<Video> getVideos(String url) {
        String htmlString = HttpClient.getHtmlByHttpClient(url);
        AbstractVideoCrawler crawler = new PornyCrawler();
        Document document = Jsoup.parse(htmlString);
        return crawler.getVideoBaseInfo(document);
    }

    private List<Video> getAllVideoByAuthor(String authorName) {
        // 转码
        String name = null;
        try {
            name = URLEncoder.encode(authorName, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            logger.error(e.getMessage(), e);
        }
        int pageNum = 1;
        List<Video> allVideoList = new ArrayList<>();
        String url = "";
        List<Video> tempVideoList;
        do {
            url = String.format("https://91porny.com/author?keywords=%s&page=%d", name, pageNum);
            tempVideoList = getVideos(url);
            allVideoList.addAll(tempVideoList);
            pageNum++;
        } while (!tempVideoList.isEmpty());
        return allVideoList;
    }

    private Map<String, List<File>> getDownloadedMap() {
        Map<String, List<File>> videoMap = new HashMap<>();
        //Map<String, List<File>> repeatVideoMap = new HashMap<>();
        Consumer<File> fileConsumer = file -> {
            if (videoMap.get(file.getName()) == null) {
                videoMap.put(file.getName(), Lists.newArrayList(file));
            } else {
                videoMap.get(file.getName()).add(file);
            }
        };
        listFiles(new File(CrawlerConfig.FILE_SAVE_PATH), fileConsumer);
        return videoMap;
    }

    public boolean downloadFromM3U8(Video video, File workspace) {
        AtomicBoolean success = new AtomicBoolean(true);
        M3U8 m3u8;
        try {
            m3u8 = getM3U8ByUrl(video.getDownloadUrl());
        } catch (Exception e) {
            logger.error("get m3u8 failed : " + e.getMessage(), e);
            return false;
        }
        // 为了兼容单个文件下载
        if (video.getId().isEmpty()) {
            video.setId("playvthumb_" + m3u8.getId());
        }
        File tempDir = new File(workspace.getPath() + "//" + m3u8.getId());
        if (!tempDir.exists()) {
            tempDir.mkdirs();
        }
        // 删除已有的
        for (File old : tempDir.listFiles()) {
            old.delete();
        }
        String basePath = m3u8.getBasePath();
        CountDownLatch countDownLatch = new CountDownLatch(m3u8.getTsList().size());
        logger.info("download video start name:{}", video.getTitle());
        m3u8.getTsList().stream().parallel().forEach(m3U8Ts -> {
            File file = new File(tempDir + File.separator + m3U8Ts.getFile());
            if (!file.exists()) {
                try {
                    URL url = new URL(basePath + m3U8Ts.getFile());
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setConnectTimeout(60 * 1000);
                    conn.setReadTimeout(60 * 1000);
                    if (conn.getResponseCode() == 200) {
                        try (InputStream inputStream = conn.getInputStream();
                             FileOutputStream fos = new FileOutputStream(file)) {
                            int len = 0;
                            byte[] buf = new byte[4096];
                            while ((len = inputStream.read(buf)) != -1) {
                                fos.write(buf, 0, len);// 写入流中
                            }
                            fos.flush();
                        }
                    }
                } catch (Exception e) {
                    success.set(false);
                    logger.error("download " + video.getTitle() + " " + m3U8Ts.getFile() + " failed");
                }
                //logger.info("{} download success", file.getName());
                countDownLatch.countDown();
            }
        });
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            logger.error(e.getMessage(), e);
            return false;
        }
        if (!success.get()) {
            logger.info("download video failed name:{}", video.getTitle());
            return success.get();
        }
        logger.info("download video success name:{}", video.getTitle());
        // ffmpeg工具合并视频片段
        File outputFile = new File(workspace + File.separator + m3u8.getId() + CrawlerConfig.EXT);
        if (outputFile.exists()) outputFile.delete();
        mergeVideo(tempDir, outputFile);
        // 删除
        for (File file : tempDir.listFiles()) file.delete();
        tempDir.delete();
        // 重命名文件
        File newNameFile = new File(workspace + File.separator + filterBannedChar(video.getTitle()) + CrawlerConfig.EXT);
        if (newNameFile.exists()) {
            logger.info("{} exists and delete it", newNameFile);
            if (newNameFile.delete()) {
                logger.info("delete {} success", newNameFile);
            } else {
                logger.warn("delete {} failed", newNameFile);
            }
        }
        if (!outputFile.renameTo(newNameFile)) {
            logger.error("{} rename to {} failed", outputFile, newNameFile);
            return false;
        }
        return true;
    }

    private M3U8 getM3U8ByUrl(String m3u8Url) throws Exception {
        m3u8Url = m3u8Url.replace("cdn.workgreat14.live", "la.killcovid2021.com");
        HttpURLConnection conn = (HttpURLConnection) new URL(m3u8Url).openConnection();
        if (conn.getResponseCode() == 200) {
            String realUrl = conn.getURL().toString();
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String basePath = realUrl.substring(0, realUrl.lastIndexOf(".m3u8") - 6);
            M3U8 m3U8 = new M3U8();
            m3U8.setBasePath(basePath);
            m3U8.setId(basePath.substring(basePath.length() - 7, basePath.length() - 1));
            String line;
            float seconds = 0;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("#")) {
                    if (line.startsWith("#EXTINF:")) {
                        int start = line.indexOf(":");
                        line = line.substring(start + 1, line.length() - 1);
                        try {
                            seconds = Float.parseFloat(line);
                        } catch (Exception e) {
                            seconds = 0;
                        }
                    }
                    continue;
                }
                if (line.endsWith("m3u8")) {
                    return getM3U8ByUrl(basePath + line);
                }
                m3U8.addTs(new M3U8.Ts(line, seconds));
                seconds = 0;
            }
            reader.close();
            return m3U8;
        }
        return null;
    }

    private int file2Num(File file) {
        String name = file.getName();
        int num = Integer.parseInt(name.substring(6, name.lastIndexOf(".")));
        return num;
    }

    private void mergeVideo(File tempDir, File outputFile) {
        File[] files = tempDir.listFiles();
        Arrays.sort(files, (f1, f2) -> {
            int num1 = file2Num(f1);
            int num2 = file2Num(f2);
            return Integer.compare(num1, num2);
        });
        if (files.length > 1) {
            String ffmpegPath = "D:\\dev\\software\\ffmpeg\\bin\\ffmpeg.exe";// 此处是配置地址，可自行写死如“”
            String outputPath = outputFile.getPath();
            String txtPath = tempDir.getPath() + File.separator + "fileList.txt";
            try (FileOutputStream fos = new FileOutputStream(new File(txtPath))) {
                for (File file : files) {
                    fos.write(("file '" + file.getPath() + "'\r\n").getBytes());
                }
                fos.flush();
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
            StringBuffer command = new StringBuffer("");
            command.append(ffmpegPath);
            command.append(" -f");
            command.append(" concat");
            command.append(" -safe");
            command.append(" 0");
            command.append(" -i ");
            command.append(txtPath);
            command.append(" -c");
            command.append(" copy ");// -c copy 避免解码，提高处理速度
            command.append(outputPath);
            exeCommand(command.toString());
        } else {
            // copy
            try {
                Files.copy(files[0], outputFile);
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    private void exeCommand(String command) {
        Process process = null;
        logger.info("start run cmd {}", command);
        try {
            process = Runtime.getRuntime().exec(command);
            //此处代码是因为如果合并大视频文件会产生大量的日志缓存导致线程阻塞，最终合并失败，所以加两个线程处理日志的缓存，之后再调用waitFor方法，等待执行结果。
            Process finalProcess = process;
            Thread outputThread = new Thread(() -> {
                try (BufferedReader in = new BufferedReader(new InputStreamReader(finalProcess.getInputStream()))) {
                    String line = null;
                    while ((line = in.readLine()) != null) {
                        logger.debug("output:" + line);
                    }
                } catch (IOException e) {
                    logger.error(e.getMessage(), e);
                }
            });
            outputThread.setName("ffmpeg-output-tracker");
            outputThread.start();

            Thread errThread = new Thread(() -> {
                try (BufferedReader err = new BufferedReader(new InputStreamReader(finalProcess.getErrorStream()))) {
                    String line = null;
                    while ((line = err.readLine()) != null) {
                        logger.debug("err:" + line);
                    }
                } catch (IOException e) {
                    logger.error(e.getMessage(), e);
                }
            });
            errThread.setName("ffmpeg-err-tracker");
            errThread.start();
            // 等待命令子线程执行完成
            finalProcess.waitFor();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        } finally {
            process.destroy();
        }
    }
}
