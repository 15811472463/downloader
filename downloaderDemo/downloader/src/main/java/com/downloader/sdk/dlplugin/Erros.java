
package com.downloader.sdk.dlplugin;

public class Erros {
    /**
     * 资源不可用,有可能是url 不被识别
     */
    public static final int URL_EXECEPTION = 1;
    /**
     * apk损坏
     */
    public static final int APK_UNINSTALLABLE = 2;
    /**
     * 网络连接超时
     */
    public static final int TIMEOUT = 3;
    /**
     * 无网络连接
     */
    public static final int NO_NETWORK = 4;

    /**
     * sd卡容量不足
     */
    public static final int SD_NOSPACE = 5;

    /**
     * 没有sd卡，或者sd未挂载
     */
    public static final int NO_SD = 6;

}
