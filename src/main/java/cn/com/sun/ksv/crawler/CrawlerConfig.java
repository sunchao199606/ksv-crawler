package cn.com.sun.ksv.crawler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * @Description : 爬虫配置
 * @Author : mockingbird
 * @Date : 2021/3/9 14:24
 */
public class CrawlerConfig {
    private static final Logger logger = LoggerFactory.getLogger(CrawlerConfig.class);
    private static Properties properties;

    static {
        properties = new Properties();
        try {
            properties.load(new FileInputStream(new File(CrawlerConfig.class.getResource("config.properties").getPath())));
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
    }

    public static String getProperties(String key) {
        return properties.getProperty(key);
    }
}
