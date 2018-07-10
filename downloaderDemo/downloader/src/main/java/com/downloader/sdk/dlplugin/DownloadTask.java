
package com.downloader.sdk.dlplugin;

import android.text.TextUtils;

import com.downloader.sdk.dlplugin.util.common.FileInfoUtils;
import com.downloader.sdk.dlplugin.util.common.FileType;

public class DownloadTask {
    public static final String KEY_ROWID = "_id";// auto increatment
    public static final String KEY_NAME = "name";// file name
    public static final String KEY_CNNAME = "cnname";// chinese name
    public static final String KEY_FILESIZE = "filesize";
    public static final String KEY_URL = "url";// source url
    public static final String KEY_PACKAGE_NAME = "packagename";
    public static final String KEY_DOWNLOAD_STRATEGY = "strategy";

    public static final String KEY_STATUS = "status";
    public static final String KEY_INSTALLED = "installed";// 是否安装
    public static final String KEY_CREATETIME = "createtime";// 創建時間
    public static final String KEY_SAVEPATH = "savepath";// 保存路径

    private long taskId = -1;
    private String name;
    private String cnname;
    private long filesize;
    private String url;
    private String packageName;
    private String downloadStrategy;

    private int installed;// 是否安装
    private long createtime = -1;// 創建時間
    private String savepath;// 下载完成后文件存放的物理路径
    private String postFix;// 文件类型
    private FileType fileType;
    private int progress = -1;// 下载进度

    private int status = -1;

    private int visibility = 0;

    public DownloadTask() {

    }

    public DownloadTask(long taskId, String url) {
        this.taskId = taskId;
        this.url = url;
        this.postFix = FileInfoUtils.getPostFix(url);
        this.fileType = getFileTypeByUrl(url);

    }

    public DownloadTask(String url, String cnname) {
        this.url = url;
        this.cnname = cnname;
        this.postFix = FileInfoUtils.getPostFix(url);
        this.fileType = getFileTypeByUrl(url);
    }

    public long getId() {
        return taskId;
    }

    public DownloadTask setId(long id) {
        this.taskId = id;
        return this;
    }

    public String getName() {
        return name;
    }

    public DownloadTask setName(String name) {
        this.name = name;
        return this;
    }

    public String getCnname() {
        return cnname;
    }

    public DownloadTask setCnname(String cnname) {
        this.cnname = cnname;
        return this;
    }

    public long getFilesize() {
        return filesize;
    }

    public DownloadTask setFilesize(long filesize) {
        this.filesize = filesize;
        return this;
    }

    public String getUrl() {
        return url;
    }

    public DownloadTask setUrl(String url) {
        this.url = url;
        return this;
    }

    public int getStatus() {
        return status;
    }

    public DownloadTask setStatus(int status) {
        this.status = status;
        return this;
    }

    public int getInstalled() {
        return installed;
    }

    public DownloadTask setInstalled(int installed) {
        this.installed = installed;
        return this;
    }

    public long getCreatetime() {
        return createtime;
    }

    public DownloadTask setCreatetime(long createtime) {
        this.createtime = createtime;
        return this;
    }

    public String getSavepath() {
        return savepath;
    }

    public DownloadTask setSavepath(String savepath) {
        this.savepath = savepath;
        return this;
    }

    public String getPostFix() {
        return postFix;
    }

    public void setPostFix(String postFix) {
        this.postFix = postFix;
    }

    public FileType getFileType() {
        return fileType;
    }

    public void setFileType(FileType fileType) {
        this.fileType = fileType;
    }

    public String getPackageName() {
        return packageName;
    }

    public DownloadTask setPackageName(String packageName) {
        this.packageName = packageName;
        return this;
    }

    public String getDownloadStrategy() {
        return downloadStrategy;
    }

    public DownloadTask setDownloadStrategy(String downloadStrategy) {
        this.downloadStrategy = downloadStrategy;
        return this;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public FileType getFileTypeByUrl(String url) {
        String postFix = FileInfoUtils.getPostFix(url);
        if ("OGG".equalsIgnoreCase(postFix)) {
            this.fileType = FileType.OGG;
        } else if ("PNG".equalsIgnoreCase(postFix)) {
            this.fileType = FileType.PNG;
        } else if ("GIF".equalsIgnoreCase(postFix)) {
            this.fileType = FileType.GIF;
        } else if ("JPG".equalsIgnoreCase(postFix)) {
            this.fileType = FileType.JPG;
        } else if ("JS".equalsIgnoreCase(postFix)) {
            this.fileType = FileType.JS;
        } else if ("MP3".equalsIgnoreCase(postFix)) {
            this.fileType = FileType.MP3;
        } else if ("WAV".equalsIgnoreCase(postFix)) {
            this.fileType = FileType.WAV;
        } else if ("APK".equalsIgnoreCase(postFix)) {
            this.fileType = FileType.APK;
        } else if ("HTML".equalsIgnoreCase(postFix)
                || "htm".equalsIgnoreCase(postFix)) {
            this.fileType = FileType.HTML;
        } else if ("PHP".equalsIgnoreCase(postFix)) {
            this.fileType = FileType.PHP;
        } else if ("JAR".equalsIgnoreCase(postFix)) {
            this.fileType = FileType.JAR;
        } else if ("SO".equalsIgnoreCase(postFix)) {
            this.fileType = FileType.SO;
        } else if ("EXE".equalsIgnoreCase(postFix)) {
            this.fileType = FileType.EXE;
        } else if ("AVI".equalsIgnoreCase(postFix)) {
            this.fileType = FileType.AVI;
        } else if ("MP4".equalsIgnoreCase(postFix)) {
            this.fileType = FileType.MP4;
        } else if ("3GP".equalsIgnoreCase(postFix)) {
            this.fileType = FileType.GP3;
        } else if ("JSP".equalsIgnoreCase(postFix)) {
            this.fileType = FileType.JSP;
        } else if ("APK".equalsIgnoreCase(postFix)) {
            this.fileType = FileType.APK;
        } else if ("TEXT".equalsIgnoreCase(postFix)) {
            this.fileType = FileType.TEXT;
        } else if ("JSON".equalsIgnoreCase(postFix)) {
            this.fileType = FileType.JSON;
        } else if ("DOC".equalsIgnoreCase(postFix)) {
            this.fileType = FileType.DOC;
        } else if ("EXL".equalsIgnoreCase(postFix)) {
            this.fileType = FileType.EXL;
        } else if ("PPT".equalsIgnoreCase(postFix)) {
            this.fileType = FileType.PPT;
        } else if ("ZIP".equalsIgnoreCase(postFix)) {
            this.fileType = FileType.ZIP;
        } else if ("RAR".equalsIgnoreCase(postFix)) {
            this.fileType = FileType.RAR;
        } else {
            this.fileType = FileType.UNKOWN;
        }
        return fileType;

    }

    public DownloadTask setFileTypeByUrl(String url) {
        String postFix = FileInfoUtils.getPostFix(url);
        if ("OGG".equalsIgnoreCase(postFix)) {
            this.fileType = FileType.OGG;
        } else if ("PNG".equalsIgnoreCase(postFix)) {
            this.fileType = FileType.PNG;
        } else if ("GIF".equalsIgnoreCase(postFix)) {
            this.fileType = FileType.GIF;
        } else if ("JPG".equalsIgnoreCase(postFix)) {
            this.fileType = FileType.JPG;
        } else if ("JS".equalsIgnoreCase(postFix)) {
            this.fileType = FileType.JS;
        } else if ("MP3".equalsIgnoreCase(postFix)) {
            this.fileType = FileType.MP3;
        } else if ("WAV".equalsIgnoreCase(postFix)) {
            this.fileType = FileType.WAV;
        } else if ("APK".equalsIgnoreCase(postFix)) {
            this.fileType = FileType.APK;
        } else if ("HTML".equalsIgnoreCase(postFix)
                || "htm".equalsIgnoreCase(postFix)) {
            this.fileType = FileType.HTML;
        } else if ("PHP".equalsIgnoreCase(postFix)) {
            this.fileType = FileType.PHP;
        } else if ("JAR".equalsIgnoreCase(postFix)) {
            this.fileType = FileType.JAR;
        } else if ("SO".equalsIgnoreCase(postFix)) {
            this.fileType = FileType.SO;
        } else if ("EXE".equalsIgnoreCase(postFix)) {
            this.fileType = FileType.EXE;
        } else if ("AVI".equalsIgnoreCase(postFix)) {
            this.fileType = FileType.AVI;
        } else if ("MP4".equalsIgnoreCase(postFix)) {
            this.fileType = FileType.MP4;
        } else if ("3GP".equalsIgnoreCase(postFix)) {
            this.fileType = FileType.GP3;
        } else if ("JSP".equalsIgnoreCase(postFix)) {
            this.fileType = FileType.JSP;
        } else if ("APK".equalsIgnoreCase(postFix)) {
            this.fileType = FileType.APK;
        } else if ("TEXT".equalsIgnoreCase(postFix)) {
            this.fileType = FileType.TEXT;
        } else if ("JSON".equalsIgnoreCase(postFix)) {
            this.fileType = FileType.JSON;
        } else if ("DOC".equalsIgnoreCase(postFix)) {
            this.fileType = FileType.DOC;
        } else if ("EXL".equalsIgnoreCase(postFix)) {
            this.fileType = FileType.EXL;
        } else if ("PPT".equalsIgnoreCase(postFix)) {
            this.fileType = FileType.PPT;
        } else if ("ZIP".equalsIgnoreCase(postFix)) {
            this.fileType = FileType.ZIP;
        } else if ("RAR".equalsIgnoreCase(postFix)) {
            this.fileType = FileType.RAR;
        } else {
            this.fileType = FileType.UNKOWN;
        }
        return this;

    }

    public int getVisibility() {
        int visibility = 0;
        if (!TextUtils.isEmpty(downloadStrategy)) {
            DownloadStrategy downloadStrategy2 = DownloadStrategy
                    .parseFromJsonString(downloadStrategy);
            if (downloadStrategy2 != null) {
                visibility = downloadStrategy2.getVisibility();

            }
        }
        return visibility;

    }

    public DownloadTask setVisibility(int visibility) {
        this.visibility = visibility;
        return this;
    }

}
