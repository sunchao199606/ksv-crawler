package cn.com.sun.ksv.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ws.schild.jave.info.AudioInfo;
import ws.schild.jave.info.MultimediaInfo;
import ws.schild.jave.info.VideoInfo;
import ws.schild.jave.info.VideoSize;
import ws.schild.jave.process.ProcessLocator;
import ws.schild.jave.process.ProcessWrapper;
import ws.schild.jave.utils.RBufferedReader;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @Description : FFMPEG工具
 * @Author : mockingbird
 * @Date : 2021/3/11 20:48
 */
public class FFMPEG {
    private static final Logger logger = LoggerFactory.getLogger(FFMPEG.class);
    /**
     * This regexp is used to parse the ffmpeg output about the size of a video stream.
     */
    private static final Pattern SIZE_PATTERN =
            Pattern.compile("(\\d+)x(\\d+)", Pattern.CASE_INSENSITIVE);
    /**
     * This regexp is used to parse the ffmpeg output about the frame rate value of a video stream.
     */
    private static final Pattern FRAME_RATE_PATTERN =
            Pattern.compile("([\\d.]+)\\s+(?:fps|tbr)", Pattern.CASE_INSENSITIVE);
    /**
     * This regexp is used to parse the ffmpeg output about the bit rate value of a stream.
     */
    private static final Pattern BIT_RATE_PATTERN =
            Pattern.compile("(\\d+)\\s+kb/s", Pattern.CASE_INSENSITIVE);
    /**
     * This regexp is used to parse the ffmpeg output about the sampling rate of an audio stream.
     */
    private static final Pattern SAMPLING_RATE_PATTERN =
            Pattern.compile("(\\d+)\\s+Hz", Pattern.CASE_INSENSITIVE);
    /**
     * This regexp is used to parse the ffmpeg output about the channels number of an audio stream.
     */
    private static final Pattern CHANNELS_PATTERN =
            Pattern.compile("(mono|stereo|quad)", Pattern.CASE_INSENSITIVE);

    private static ProcessLocator locator = new CustomLocator();

    public static void resizeAndExtend(File source, VideoSize videoSize, float factor) {
        File temp = getTemp(source);
        List<String> paraList = new ArrayList<>();
        paraList.add("-i");
        paraList.add(source.getAbsolutePath());
        paraList.add("-filter:v");
        paraList.add(String.format("\"setpts=%f*PTS\"", factor));
        paraList.add("-s");
        paraList.add(String.format("%sx%s", videoSize.getWidth(), videoSize.getHeight()));
        paraList.add(temp.getAbsolutePath());
        FFMPEG.execute(paraList);
        // 处理temp视频
        deleteAndRename(source, temp);
    }

    public static void resize(File source, String size) {
        File temp = getTemp(source);
        List<String> paraList = new ArrayList<>();
        paraList.add("-i");
        paraList.add(source.getAbsolutePath());
        paraList.add("-s");
        paraList.add(size);
        paraList.add(temp.getAbsolutePath());
        FFMPEG.execute(paraList);
        // 处理temp视频
        deleteAndRename(source, temp);
    }

    private static void deleteAndRename(File source, File temp) {
        // 重命名文件
        File target = new File(temp.getAbsolutePath().replace("temp.mp4", ".mp4"));
        if (source.delete()) {
            temp.renameTo(target);
            //logger.info("视频{}修改成功", source.getName());
        } else
            logger.error("视频重命名失败");
    }

    private static File getTemp(File source) {
        // 获取temp文件
        String tempPath = source.getParent() + File.separator + source.getName().replace(".mp4", "temp.mp4");
        File temp = new File(tempPath);
        return temp;
    }

    public static void extend(File source, float factor) {
        File temp = getTemp(source);
        List<String> paraList = new ArrayList<>();
        paraList.add("-i");
        paraList.add(source.getAbsolutePath());
        paraList.add("-filter:v");
        paraList.add(String.format("\"setpts=%f*PTS\"", factor));
        paraList.add(temp.getAbsolutePath());
        FFMPEG.execute(paraList);
        deleteAndRename(source, temp);
    }

    public static void cut(File source, String start, String end) {
        File temp = getTemp(source);
        List<String> paraList = new ArrayList<>();
        paraList.add("-ss");
        paraList.add(start);
        paraList.add("-i");
        paraList.add(source.getAbsolutePath());
        paraList.add("-vcodec");
        paraList.add("copy");
        paraList.add("-acodec");
        paraList.add("copy");
        paraList.add("-t");
        paraList.add(end);
        paraList.add(temp.getAbsolutePath());
        FFMPEG.execute(paraList);
        deleteAndRename(source, temp);
    }

    /**
     * 调用FFmpeg
     *
     * @param parameters
     */
    private static void execute(List<String> parameters) {
        //执行命令
        ProcessWrapper executor = locator.createExecutor();
        parameters.forEach(parameter -> executor.addArgument(parameter));
        try {
            executor.execute();
            processOutput(executor.getInputStream(), executor.getErrorStream());
            // 同步等待任务完成
            executor.getProcessExitCode();
        } catch (IOException e) {
            logger.error(e.getMessage());
        } finally {
            executor.destroy();
        }
    }

    private static void processOutput(InputStream inputStream, InputStream errorStream) {
        Thread outputThread = new Thread(() -> {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(inputStream))) {
                String line = null;
                while ((line = in.readLine()) != null) {
                    logger.debug("output:" + line);
                }
            } catch (IOException e) {
                logger.error(e.getMessage());
            }
        });
        outputThread.setName("ffmpeg-output-tracker");
        outputThread.start();

        Thread errThread = new Thread(() -> {
            try (BufferedReader err = new BufferedReader(new InputStreamReader(errorStream))) {
                String line = null;
                while ((line = err.readLine()) != null) {
                    logger.debug("err:" + line);
                }
            } catch (IOException e) {
                logger.error(e.getMessage());
            }
        });
        errThread.setName("ffmpeg-err-tracker");
        errThread.start();
    }

    /**
     * 获取视频时长
     *
     * @param source
     * @return
     */
    public static MultimediaInfo getVideoInfo(File source) {

        List<String> paraList = new ArrayList<>();
        paraList.add("-i");
        paraList.add(source.getAbsolutePath());
        //执行命令
        ProcessWrapper executor = locator.createExecutor();
        paraList.forEach(parameter -> executor.addArgument(parameter));
        MultimediaInfo info = null;
        try {
            executor.execute();
            try {
                RBufferedReader reader = new RBufferedReader(new InputStreamReader(executor.getErrorStream()));
                info = parseMultimediaInfo(source.getAbsolutePath(), reader);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        } finally {
            executor.destroy();
        }
        return info;
    }

    private static MultimediaInfo parseMultimediaInfo(String source, RBufferedReader reader)
            throws Exception {
        Pattern p1 = Pattern.compile("^\\s*Input #0, (\\w+).+$\\s*", Pattern.CASE_INSENSITIVE);
        Pattern p21 = Pattern.compile("^\\s*Duration:.*$", Pattern.CASE_INSENSITIVE);
        Pattern p22 =
                Pattern.compile(
                        "^\\s*Duration: (\\d\\d):(\\d\\d):(\\d\\d)\\.(\\d\\d).*$", Pattern.CASE_INSENSITIVE);
        Pattern p3 =
                Pattern.compile(
                        "^\\s*Stream #\\S+: ((?:Audio)|(?:Video)|(?:Data)): (.*)\\s*$",
                        Pattern.CASE_INSENSITIVE);
        @SuppressWarnings("unused")
        Pattern p4 = Pattern.compile("^\\s*Metadata:", Pattern.CASE_INSENSITIVE);
        MultimediaInfo info = null;
        try {
            int step = 0;
            while (true) {
                String line = reader.readLine();
                if (line == null) {
                    break;
                }
                switch (step) {
                    case 0: {
                        String token = source + ": ";
                        if (line.startsWith(token)) {
                            String message = line.substring(token.length());
                            throw new Exception(message);
                        }
                        Matcher m = p1.matcher(line);
                        if (m.matches()) {
                            String format = m.group(1);
                            info = new MultimediaInfo();
                            info.setFormat(format);
                            step++;
                        }
                        break;
                    }
                    case 1: {
                        Matcher m1 = p21.matcher(line);
                        Matcher m2 = p22.matcher(line);
                        if (m1.matches()) {
                            if (m2.matches()) {
                                long hours = Integer.parseInt(m2.group(1));
                                long minutes = Integer.parseInt(m2.group(2));
                                long seconds = Integer.parseInt(m2.group(3));
                                long dec = Integer.parseInt(m2.group(4));
                                long duration =
                                        (dec * 10L)
                                                + (seconds * 1000L)
                                                + (minutes * 60L * 1000L)
                                                + (hours * 60L * 60L * 1000L);
                                info.setDuration(duration);
                                step++;
                            } else {
                                logger.warn("Invalid duration found {}", line);
                                step++;
                                // step = 3;
                            }
                        } else {
                            // step = 3;
                        }
                        break;
                    }
                    case 2: {
                        Matcher m = p3.matcher(line);
                        if (m.matches()) {
                            String type = m.group(1);
                            String specs = m.group(2);
                            if ("Video".equalsIgnoreCase(type)) {
                                VideoInfo video = new VideoInfo();
                                StringTokenizer st = new StringTokenizer(specs, ",");
                                for (int i = 0; st.hasMoreTokens(); i++) {
                                    String token = st.nextToken().trim();
                                    if (i == 0) {
                                        video.setDecoder(token);
                                    } else {
                                        boolean parsed = false;
                                        // Video size.
                                        Matcher m2 = SIZE_PATTERN.matcher(token);
                                        if (!parsed && m2.find()) {
                                            int width = Integer.parseInt(m2.group(1));
                                            int height = Integer.parseInt(m2.group(2));
                                            video.setSize(new VideoSize(width, height));
                                            parsed = true;
                                        }
                                        // Frame rate.
                                        m2 = FRAME_RATE_PATTERN.matcher(token);
                                        if (!parsed && m2.find()) {
                                            try {
                                                float frameRate = Float.parseFloat(m2.group(1));
                                                video.setFrameRate(frameRate);
                                            } catch (NumberFormatException e) {
                                                logger.info("Invalid frame rate value: " + m2.group(1), e);
                                            }
                                            parsed = true;
                                        }
                                        // Bit rate.
                                        m2 = BIT_RATE_PATTERN.matcher(token);
                                        if (!parsed && m2.find()) {
                                            int bitRate = Integer.parseInt(m2.group(1));
                                            video.setBitRate(bitRate * 1000);
                                            parsed = true;
                                        }
                                    }
                                }
                                info.setVideo(video);
                            } else if ("Audio".equalsIgnoreCase(type)) {
                                AudioInfo audio = new AudioInfo();
                                StringTokenizer st = new StringTokenizer(specs, ",");
                                for (int i = 0; st.hasMoreTokens(); i++) {
                                    String token = st.nextToken().trim();
                                    if (i == 0) {
                                        audio.setDecoder(token);
                                    } else {
                                        boolean parsed = false;
                                        // Sampling rate.
                                        Matcher m2 = SAMPLING_RATE_PATTERN.matcher(token);
                                        if (!parsed && m2.find()) {
                                            int samplingRate = Integer.parseInt(m2.group(1));
                                            audio.setSamplingRate(samplingRate);
                                            parsed = true;
                                        }
                                        // Channels.
                                        m2 = CHANNELS_PATTERN.matcher(token);
                                        if (!parsed && m2.find()) {
                                            String ms = m2.group(1);
                                            if ("mono".equalsIgnoreCase(ms)) {
                                                audio.setChannels(1);
                                            } else if ("stereo".equalsIgnoreCase(ms)) {
                                                audio.setChannels(2);
                                            } else if ("quad".equalsIgnoreCase(ms)) {
                                                audio.setChannels(4);
                                            }
                                            parsed = true;
                                        }
                                        // Bit rate.
                                        m2 = BIT_RATE_PATTERN.matcher(token);
                                        if (!parsed && m2.find()) {
                                            int bitRate = Integer.parseInt(m2.group(1));
                                            audio.setBitRate(bitRate * 1000);
                                            parsed = true;
                                        }
                                    }
                                }
                                info.setAudio(audio);
                            }
                        } else // if (m4.matches())
                        {
                            // Stay on level 2
                        }
              /*
                 else
                 {
                 step = 3;
                 }
              */
                        break;
                    }
                    default:
                        break;
                }
                if (line.startsWith("frame=")) {
                    reader.reinsertLine(line);
                    break;
                }
            }
        } catch (Exception e) {
            throw new Exception(e);
        }
        if (info == null) {
            throw new Exception();
        }
        return info;
    }
}
