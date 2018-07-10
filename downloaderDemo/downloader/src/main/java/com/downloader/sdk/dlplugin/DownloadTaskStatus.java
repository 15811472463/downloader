
package com.downloader.sdk.dlplugin;

public class DownloadTaskStatus {

    public static final int TEMP_NODOWNLOAD = -1;
    public static final int WAITING = 0;
    public static final int DOWNLOADING = 1;
    public static final int PAUSEING = 2;
    public static final int UNCOMPLETED = 4;
    public static final int UNKNOW = 5;
    public static final int COMPLETED = 6;

    private long taskId;
    private String url;

    private long speed;
    private long totalSize;
    private long currentSize;
    private long downloadPercent;
    private int currentStatus;

    public DownloadTaskStatus(long taskId, String url) {
        this.taskId = taskId;
        this.url = url;
    }

    public long getTaskId() {
        return taskId;
    }

    public DownloadTaskStatus setTaskId(long taskId) {
        this.taskId = taskId;
        return this;
    }

    public String getUrl() {
        return url;
    }

    public DownloadTaskStatus setUrl(String url) {
        this.url = url;
        return this;
    }

    public long getSpeed() {
        return speed;
    }

    public DownloadTaskStatus setSpeed(long speed) {
        this.speed = speed;
        return this;
    }

    public long getTotalSize() {
        return totalSize;
    }

    public DownloadTaskStatus setTotalSize(long totalSize) {
        this.totalSize = totalSize;
        return this;
    }

    public long getCurrentSize() {
        return currentSize;
    }

    public DownloadTaskStatus setCurrentSize(long currentSize) {
        this.currentSize = currentSize;
        return this;
    }

    public long getDownloadPercent() {
        return downloadPercent;
    }

    public DownloadTaskStatus setDownloadPercent(long downloadPercent) {
        this.downloadPercent = downloadPercent;
        return this;
    }

    public int getCurrentStatus() {
        return currentStatus;
    }

    public DownloadTaskStatus setCurrentStatus(int currentStatus) {
        this.currentStatus = currentStatus;
        return this;
    }
}
