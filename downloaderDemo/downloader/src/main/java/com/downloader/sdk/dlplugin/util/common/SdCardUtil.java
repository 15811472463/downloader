
package com.downloader.sdk.dlplugin.util.common;

import android.os.Environment;
import android.os.StatFs;

import java.io.File;

/**
 * @author yangchangwei
 */
public class SdCardUtil {
    /**
     * 判断sd卡是否被挂载
     * 
     * @param mContext
     * @return
     */
    public static boolean isMounted() {
        boolean result = false;
        if (Environment.getExternalStorageState().equals(
                Environment.MEDIA_MOUNTED)) {
            result = true;
        }
        return result;

    }

    /**
     * 得到sd 卡剩余大小
     * 
     * @param mContext
     * @return
     */
    public static long getSdRemained() {
        long avaliableSize = -1;
        if (isMounted()) {
            File path = Environment.getExternalStorageDirectory();
            StatFs stat = new StatFs(path.getPath());
            long blockSize = stat.getBlockSize();
            avaliableSize = stat.getAvailableBlocks() * blockSize;
        }
        return avaliableSize;

    }

}
