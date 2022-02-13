package cn.com.sun.crawler.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.Objects;

/**
 * @Description : 一条视频的元数据
 * @Author : Mockingbird
 * @Date: 2020-07-18 18:47
 */
@JsonPropertyOrder({"id", "title", "author", "date", "storeNum", "watchNum", "duration"})
public class Video {

    @JsonIgnore
    private String shareUrl = "";
    @JsonIgnore
    private String downloadUrl = "";
    @JsonIgnore
    private String href = "";
    @JsonIgnore
    private String uid = "";

    private String id = "";

    private String title = "null";

    private String author = "";
    private int duration = 0;
    private String date = "";
    private int storeNum = 0;
    private int watchNum = 0;

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getHref() {
        return href;
    }

    public void setHref(String href) {
        this.href = href;
    }

    public int getStoreNum() {
        return storeNum;
    }

    public void setStoreNum(int storeNum) {
        this.storeNum = storeNum;
    }

    public int getWatchNum() {
        return watchNum;
    }

    public void setWatchNum(int watchNum) {
        this.watchNum = watchNum;
    }

    public String getShareUrl() {
        return shareUrl;
    }

    public void setShareUrl(String shareUrl) {
        this.shareUrl = shareUrl;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int durationSecond) {
        this.duration = durationSecond;
    }

    @Override
    public String toString() {
        return id + "|" + title;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Video video = (Video) o;
        return Objects.equals(id, video.id) &&
                Objects.equals(title, video.title);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, title);
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }
}
