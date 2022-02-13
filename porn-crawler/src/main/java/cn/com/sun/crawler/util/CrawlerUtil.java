package cn.com.sun.crawler.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CrawlerUtil {
    public static String filterBannedChar(String string) {
        String regEx = "[:<>/\\|\"*?]";
        Pattern p = Pattern.compile(regEx);
        Matcher m = p.matcher(string);
        return m.replaceAll("").trim();
    }
}
