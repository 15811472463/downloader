
package com.downloader.sdk.dlplugin.util.common;

import android.text.TextUtils;

import com.downloader.sdk.dlplugin.DownLoadConfig;

import java.io.File;
import java.io.FileInputStream;
import java.text.DecimalFormat;

/**
 * @Title FileInfoUtil
 * @Description FileInfoUtil是一个字符串的操作类
 * @author
 * @date 2013-1-22 下午 14:35
 * @version V1.0
 */
public class FileInfoUtils {
    /**
     * 返回自定文件或文件夹的大小
     * 
     * @param f
     * @return
     * @throws Exception
     */
    public static long getFileSizes(File f) throws Exception {// 取得文件大小
        long s = 0;
        if (f.exists()) {
            FileInputStream fis = null;
            fis = new FileInputStream(f);
            s = fis.available();
        } else {
            f.createNewFile();
            System.out.println("文件不存在");
        }
        return s;
    }

    /**
     * 递归取得文件夹大小
     * 
     * @param f
     * @return
     * @throws Exception
     */
    public static long getFileSize(File f) throws Exception {
        long size = 0;
        File flist[] = f.listFiles();
        for (int i = 0; i < flist.length; i++) {
            if (flist[i].isDirectory()) {
                size = size + getFileSize(flist[i]);
            } else {
                size = size + flist[i].length();
            }
        }
        return size;
    }

    public static String FormetFileSize(long fileS) {// 转换文件大小
        DecimalFormat df = new DecimalFormat("#0.00");
        String fileSizeString = "";
        if (fileS < DownLoadConfig.BITE_DIVING_LINE) {
            fileSizeString = df.format((double) fileS) + "B";
        } else if (fileS < DownLoadConfig.KB_DIVING_LINE) {
            fileSizeString = df.format((double) fileS
                    / DownLoadConfig.BITE_DIVING_LINE)
                    + "K";
        } else if (fileS < DownLoadConfig.MB_DIVING_LINE) {
            fileSizeString = df.format((double) fileS
                    / DownLoadConfig.KB_DIVING_LINE)
                    + "M";
        } else {
            fileSizeString = df.format((double) fileS
                    / DownLoadConfig.MB_DIVING_LINE)
                    + "G";
        }
        return fileSizeString;
    }

    public static long getlist(File f) {// 递归求取目录文件个数
        long size = 0;
        File flist[] = f.listFiles();
        size = flist.length;
        for (int i = 0; i < flist.length; i++) {
            if (flist[i].isDirectory()) {
                size = size + getlist(flist[i]);
                size--;
            }
        }
        return size;

    }

    /**
     * ycw 得到文件后缀名
     * 
     * @param url
     * @return
     */
    public static String getPostFix(String url) {
        String postFix = null;
        if (!TextUtils.isEmpty(url)) {
            if (url.contains(".")) {
                postFix = url.substring(url.lastIndexOf(".") + 1);
            }
        }

        return postFix;
    }

}
