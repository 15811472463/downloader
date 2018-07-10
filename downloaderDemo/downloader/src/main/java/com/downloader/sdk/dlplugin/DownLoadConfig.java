
package com.downloader.sdk.dlplugin;

import android.os.Environment;

public class DownLoadConfig {
    private static final String SDCARD_ROOT = Environment
            .getExternalStorageDirectory().getAbsolutePath() + "/";
    public static String FILE_ROOT = SDCARD_ROOT + "mfdownload/";
    public static final int MAX_handler_COUNT = 100;
    public static int MAX_DOWNLOAD_THREAD_COUNT = 2;
    public static final int PLAY_SOUND_WHEN_COMPLETED = 1;
    public static final String PLAY_SOUND = "";
    // 清除历史下载任务时间间隔,default：14天
    public static final int CLEAN_HISTORY_INTERVAL = 14 * 24 * 60 * 60 * 1000;
    // 清除过期 apk 时间
    public static final int CLEAN_APK_OUTDATE = 7 * 24 * 60 * 60 * 1000;
    // 是否开启自动删除临时文件
    public static boolean autoCleanApk = false;

    // 大文件wifi环境下下载
    public static final int FILE_SIZE_THRESHOLD_WIFI = 1024 * 2000000;

    public static boolean DEBUG = true;

    public static boolean isClickable = false;

    public static boolean isShow = false;

    public static boolean notifactionCancleAble = false;

    public static int wifiTimeOut = 30 * 1000;

    public static int unWifiTimeOut = 60 * 1000;

    public static final String DOWNLOAD_VERSION = "1.1";

    public static final int REDIRECT_COUNT = 5;

    public static final long BITE_DIVING_LINE = 1024;
    public static final long KB_DIVING_LINE = 1024 * 1024;
    public static final long MB_DIVING_LINE = 1024 * 1024 * 1024;

}
