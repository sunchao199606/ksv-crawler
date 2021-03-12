package cn.com.sun.ksv.util;

import ws.schild.jave.process.ProcessLocator;

/**
 * @Description :
 * @Author : mockingbird
 * @Date : 2021/3/11 22:26
 */
public class CustomLocator implements ProcessLocator {
    @Override
    public String getExecutablePath() {
        return "D:\\Program\\tools\\ffmpeg\\bin\\ffmpeg.exe";
    }
}
