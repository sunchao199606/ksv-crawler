package cn.com.sun.crawler;

/**
 * @Description : 入口
 * @Author : Mockingbird
 * @Date : 2020-08-16 11:33
 */
public class CrawlerRunner {
    public static void main(String[] args) {
        CrawlerFactory.newCrawler().parseVideoBaseInfo().parseVideoExtInfo().parseDownloadUrl().download();
    }
}
