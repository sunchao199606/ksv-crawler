package cn.com.sun.crawler.m3u8;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class M3U8 {
    private String basePath;
    private String id;
    private List<Ts> tsList = new ArrayList<>();
    private long startTime;// 开始时间
    private long endTime;// 结束时间
    private long startDownloadTime;// 开始下载时间
    private long endDownloadTime;// 结束下载时间

    public String getBasePath() {
        return basePath;
    }

    public void setBasePath(String basePath) {
        this.basePath = basePath;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<Ts> getTsList() {
        return tsList;
    }

    public void setTsList(List<Ts> tsList) {
        this.tsList = tsList;
    }

    public void addTs(Ts ts) {
        this.tsList.add(ts);
    }

    public long getStartDownloadTime() {
        return startDownloadTime;
    }

    public void setStartDownloadTime(long startDownloadTime) {
        this.startDownloadTime = startDownloadTime;
    }

    public long getEndDownloadTime() {
        return endDownloadTime;
    }

    public void setEndDownloadTime(long endDownloadTime) {
        this.endDownloadTime = endDownloadTime;
    }

    /**
     * 获取开始时间
     *
     * @return
     */
    public long getStartTime() {
        if (tsList.size() > 0) {
            Collections.sort(tsList);
            startTime = tsList.get(0).getLongDate();
            return startTime;
        }
        return 0;
    }

    /**
     * 获取结束时间(加上了最后一段时间的持续时间)
     *
     * @return
     */
    public long getEndTime() {
        if (tsList.size() > 0) {
            Ts m3U8Ts = tsList.get(tsList.size() - 1);
            endTime = m3U8Ts.getLongDate() + (long) (m3U8Ts.getSeconds() * 1000);
            return endTime;
        }
        return 0;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("basePath: " + basePath);
        for (Ts ts : tsList) {
            sb.append("\nts_file_name = " + ts);
        }
        sb.append("\n\nstartTime = " + startTime);
        sb.append("\n\nendTime = " + endTime);
        sb.append("\n\nstartDownloadTime = " + startDownloadTime);
        sb.append("\n\nendDownloadTime = " + endDownloadTime);
        return sb.toString();
    }

    public static class Ts implements Comparable<Ts> {
        private String file;
        private float seconds;

        public Ts(String file, float seconds) {
            this.file = file;
            this.seconds = seconds;
        }

        public String getFile() {
            return file;
        }

        public void setFile(String file) {
            this.file = file;
        }

        public float getSeconds() {
            return seconds;
        }

        public void setSeconds(float seconds) {
            this.seconds = seconds;
        }

        @Override
        public String toString() {
            return file + " (" + seconds + "sec)";
        }

        /**
         * 获取时间
         */
        public long getLongDate() {
            try {
                return Long.parseLong(file.substring(0, file.lastIndexOf(".")));
            } catch (Exception e) {
                return 0;
            }
        }

        @Override
        public int compareTo(Ts o) {
            return file.compareTo(o.file);
        }
    }
}


