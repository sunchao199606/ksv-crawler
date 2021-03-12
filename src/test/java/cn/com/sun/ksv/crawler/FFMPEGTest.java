package cn.com.sun.ksv.crawler;

import cn.com.sun.ksv.util.FFMPEG;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ws.schild.jave.info.MultimediaInfo;
import ws.schild.jave.info.VideoSize;

import java.io.File;

/**
 * @Description :
 * @Author : mockingbird
 * @Date : 2021/3/11 20:45
 */
public class FFMPEGTest {
    private static final Logger logger = LoggerFactory.getLogger(FFMPEGTest.class);

    @Test
    public void resize() {
        File source = new File("D:\\output\\2021-03-11\\9.mp4");
        FFMPEG.resize(source, new VideoSize(720, 1280).asEncoderArgument());
    }

    @Test
    public void cut() {
        File source = new File("D:\\output\\2021-03-11\\9.mp4");
        FFMPEG.cut(source, "00:00:00", "00:00:08");
    }

    @Test
    public void extend() {
        File source = new File("D:\\output\\2021-03-11\\9.mp4");
        float expect = 12;
        MultimediaInfo info = FFMPEG.getVideoInfo(source);
        float actual = info.getDuration() / 1000;
        if (actual < 12) {
            float factor = expect / actual;
            FFMPEG.extend(source, factor);
        }
    }

    @Test
    public void resizeAndExtend() {

    }
}
