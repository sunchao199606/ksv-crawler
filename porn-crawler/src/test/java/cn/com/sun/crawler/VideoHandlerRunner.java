package cn.com.sun.crawler;

import cn.com.sun.crawler.entity.Video;
import cn.com.sun.crawler.impl.PornyCrawler;
import cn.com.sun.crawler.util.FileAccessManager;
import cn.com.sun.crawler.util.HttpClient;
import cn.com.sun.crawler.util.VideoHandler;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import ws.schild.jave.EncoderException;
import ws.schild.jave.MultimediaObject;
import ws.schild.jave.info.MultimediaInfo;
import ws.schild.jave.info.VideoSize;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Stream;

/**
 * @Description :
 * @Author : mockingbird
 * @Date : 2020/12/5 18:35
 */
public class VideoHandlerRunner {
    private static String dir = "F:\\Download\\crawler\\2020-12-30";

    private VideoHandler handler;

    public static Stream<File> fileGenerator() {
        String dir = "F:\\Download\\crawler\\2020-09";
        List<File> fileList = new ArrayList<>();
        VideoHandler.listFiles(new File(dir), fileList);
        return fileList.stream();
    }

    @BeforeEach
    public void init() {
        handler = new VideoHandler();
    }

    @Test
    public void m3u8() {
        File workspace = new File("F:\\Download\\crawler\\2021-01-09");
        Video video = new Video();
        video.setDownloadUrl("");
        video.setTitle("");
        Date now = new Date();
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        video.setDate(format.format(now));
        handler.downloadFromM3U8(video, workspace);
        FileAccessManager.getInstance().write(video);
    }

    @ParameterizedTest()
    @ValueSource(strings = {".mp4"})
    public void encodeTo202(String name) {
        File file = new File(dir + "\\" + name);
        VideoHandler.encode(file, new VideoSize(202, 360));
    }

    @ParameterizedTest()
    @ValueSource(strings = {".mp4"})
    public void encodeTo360(String name) {
        File file = new File(dir + "\\" + name);
        VideoHandler.encode(file, new VideoSize(400, 360));
    }

    @ParameterizedTest()
    @ValueSource(strings = {".mp4", ".mp4"})
    public void encodeTo480(String name) {
        File file = new File(dir + "\\" + name);
        VideoHandler.encode(file, new VideoSize(480, 360));
    }

    @ParameterizedTest()
    @ValueSource(strings = {".mp4"})
    public void encodeTo540(String name) {
        File file = new File(dir + "\\" + name);
        VideoHandler.encode(file, new VideoSize(540, 360));
    }

    @ParameterizedTest()
    @ValueSource(strings = {".mp4"})
    public void encodeTo640(String name) {
        File file = new File(dir + "\\" + name);
        VideoHandler.encode(file, new VideoSize(640, 360));
    }

    @ParameterizedTest()
    @MethodSource("fileGenerator")
    public void encodeToNhd(File f) throws EncoderException {
        MultimediaInfo info = new MultimediaObject(f).getInfo();
        if (info.getVideo().getSize().asEncoderArgument().
                equals(new VideoSize(540, 360).asEncoderArgument())) {
            VideoHandler.encode(f, VideoSize.nhd);
        }
    }

    @ParameterizedTest()
    @ValueSource(strings = {"Abyss22b"})
    public void runStoreByAuthor(String authorName) {
        handler.storeByAuthor(authorName, false);
    }

    private List<Video> getVideos(String url) {
        String htmlString = HttpClient.getHtmlByHttpClient(url);
        AbstractVideoCrawler crawler = new PornyCrawler();
        Document document = Jsoup.parse(htmlString);
        return crawler.getVideoBaseInfo(document);
    }
}
