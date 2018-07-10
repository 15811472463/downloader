package com.downloader.sdk.dlplugin.demo.adapter;

/**
 * Created by ycw on 2018/7/10.
 */

public class FileInfoUtils {
    public static String FormetFileSize(long size){
        if (size>0&&size<1024){
            return size+"b";
        }else if (size>=1024&&size<1024*1024){
            float format= (float) (size/1024.0);
            return (format)+"kb";
        }else if (size>=1024*1024&&size<1024*1024*1024){
            float format= (float) (size/(1024*1024.0));
            return (format+"M");
        }else {
            float format= (float) (size/(1024*1024.0*1024));
            return (format+"G");
        }
    }
}
