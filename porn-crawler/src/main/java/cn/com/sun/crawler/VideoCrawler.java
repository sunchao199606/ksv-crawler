package cn.com.sun.crawler;

public interface VideoCrawler {
    /**
     * 基础信息
     *
     * @return
     */
    VideoCrawler parseVideoBaseInfo();

    /**
     * 扩展信息
     *
     * @return
     */
    VideoCrawler parseVideoExtInfo();

    /**
     * 下载链接
     *
     * @return
     */
    VideoCrawler parseDownloadUrl();

    void download();
}
