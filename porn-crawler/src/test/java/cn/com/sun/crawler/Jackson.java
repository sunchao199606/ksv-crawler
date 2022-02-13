package cn.com.sun.crawler;

import cn.com.sun.crawler.entity.Video;
import cn.com.sun.crawler.util.FileAccessManager;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Jackson {
    private ObjectMapper mapper;

    @BeforeEach
    public void init() {
        mapper = new ObjectMapper();
        //在序列化时忽略值为 null 的属性
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        //忽略值为默认值的属性
        mapper.setDefaultPropertyInclusion(JsonInclude.Include.NON_DEFAULT);
        //在反序列化时忽略在 json 中存在但 Java 对象不存在的属性
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Test
    public void testDeserialization() {
        File infoFile = new File("F:\\Download\\crawler\\info.json");
        Map<String, Video> map = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader(infoFile))) {
            br.lines().forEach(info -> {
                try {
                    Video video = mapper.readValue(info, Video.class);
                    map.put(video.getId(), video);
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }
            });
        } catch (IOException e) {
            //logger.error(e.getMessage(), e);
        }

    }

    @Test
    public void testJackson() throws JsonProcessingException {
        Video video = new Video();
        video.setId("123");
        video.setTitle("test");
        video.setAuthor("sunChao");
        video.setDate("2020-12-31");
        video.setStoreNum(300);
        video.setWatchNum(30000);
        video.setShareUrl("shareUrl");
        video.setHref("pageUrl");
        video.setDownloadUrl("downloadUrl");
        System.out.println(mapper.writeValueAsString(video));
    }

    @Test
    public void base(String[] args) {
        ReadWriteLock READ_WRITE_LOCK = new ReentrantReadWriteLock();
        Lock READ_LOCK = READ_WRITE_LOCK.readLock();
        // 转化成json
        File infoFile = new File("F:\\Download\\crawler\\downloaded");
        File infoFile2 = new File("F:\\Download\\crawler\\authorInfo");
        FileAccessManager.getInstance().read();
        Map<String, Video> map = new HashMap<>();
        READ_LOCK.lock();
        try {
            //logger.info("{} get readLock", Thread.currentThread().getName());
            try (BufferedReader br = new BufferedReader(new FileReader(infoFile));
                 BufferedReader br2 = new BufferedReader(new FileReader(infoFile2))) {
                br.lines().forEach(info -> {
                    Video video = new Video();
                    String id = info.split("\\|")[0];
                    video.setId(id);
                    video.setTitle(info.split("\\|")[1]);
                    map.put(id, video);
                });
                br2.lines().forEach(info -> {
                    Video video = new Video();
                    String id = info.split("\\|")[0];
                    video.setId(id);
                    video.setTitle(info.split("\\|")[1]);
                    video.setAuthor(info.split("\\|")[2]);
                    map.put(id, video);
                });

            } catch (IOException e) {
                //logger.error(e.getMessage(), e);
            }
        } finally {
            READ_LOCK.unlock();
            //logger.info("{} release readLock", Thread.currentThread().getName());
        }
        File jsonFile = new File("F:\\Download\\crawler\\info.json");

        ObjectMapper mapper = new ObjectMapper();
        //在序列化时忽略值为 null 的属性
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        //忽略值为默认值的属性
        mapper.setDefaultPropertyInclusion(JsonInclude.Include.NON_DEFAULT);
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(jsonFile, true))) {
            map.values().stream().forEach(video -> {
                try {
                    String json = mapper.writeValueAsString(video);
                    bw.write(json + "\n");
                    bw.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        } catch (Exception e) {

        }

//        String jsonString = mapper.writerWithDefaultPrettyPrinter()
//                .writeValueAsString(person);
        //Person deserializedPerson = mapper.readValue(jsonString, Person.class);
    }
}
