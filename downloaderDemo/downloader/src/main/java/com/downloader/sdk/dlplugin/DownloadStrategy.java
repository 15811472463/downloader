
package com.downloader.sdk.dlplugin;

import android.text.TextUtils;

import org.json.JSONObject;

import java.io.Serializable;

public class DownloadStrategy implements Serializable {

    private static final long serialVersionUID = -3180250690188963342L;

    /*
     * 步进,小于100且能被100整除的正整数 用途:控制有进度显示的广告的通知频次
     */
    public int step = 100;
    // 是否show在wifi下载的提示
    public int onlyDownloadUnderWifiEnv;
    public static final String SEGMENT_KEY = "segment";
    public static final String VISIBILITY_KEY = "visibility";
    public static final String ROOTPATH_KEY = "rootpath";

    public static final int VISIBILITY = 0;
    public static final int GONE = 1;
    public static final int ALL_UNCOMPELETED=2;
    public int visibility = 0;
    public String rootpath = null;

    public DownloadStrategy() {

    }

    public static DownloadStrategy parseFromJsonString(String jsonStr) {
        if (jsonStr == null)
            return null;
        try {
            return parseFromJsonObject(new JSONObject(jsonStr));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static DownloadStrategy parseFromJsonObject(JSONObject jsonObj) {
        if (jsonObj == null)
            return null;
        try {
            if (jsonObj != null) {
                DownloadStrategy rt = new DownloadStrategy();
                int segment = jsonObj.optInt(SEGMENT_KEY);
                if (segment > 1) {
                    rt.step = segment;
                }
                int visibility = jsonObj.optInt(VISIBILITY_KEY);
                rt.visibility = visibility;
                String rootPath = jsonObj.optString(ROOTPATH_KEY);
                if (!TextUtils.isEmpty(rootPath)) {
                    rt.rootpath = rootPath;
                }

                return rt;
            }
        } catch (Exception e) {

        }
        return null;
    }

    public int getVisibility() {
        return visibility;
    }

    public void setVisibility(int visibility) {
        this.visibility = visibility;
    }

    public String getRootpath() {
        return rootpath;
    }

    public void setRootpath(String rootpath) {
        this.rootpath = rootpath;
    }

}
