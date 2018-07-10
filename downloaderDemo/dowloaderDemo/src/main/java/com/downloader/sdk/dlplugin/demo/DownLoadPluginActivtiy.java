package com.downloader.sdk.dlplugin.demo;


import android.app.Activity;
import android.net.Proxy;
import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.downloader.sdk.dlplugin.DownLoadCallback;
import com.downloader.sdk.dlplugin.DownloadManager;
import com.downloader.sdk.dlplugin.DownloadStrategy;
import com.downloader.sdk.dlplugin.DownloadTask;
import com.downloader.sdk.dlplugin.DownloadTaskStatus;
import com.downloader.sdk.dlplugin.demo.adapter.DownloadedListAdapter;
import com.downloader.sdk.dlplugin.demo.adapter.DownloadingListAdapter;
import com.downloader.sdk.dlplugin.demo.data.DownloadUrls;

import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;


public class DownLoadPluginActivtiy extends Activity {

    private Button addButton = null;
    private Button resetButton = null;
    private Button stopButton = null;
    private DownloadingListAdapter downloadListAdapter;
    private DownloadedListAdapter downloadedListAdapter;

    private int urlIndex = 0;


    //pageInditator

    private View v_downloading, v_downloaded;// 需要显示的View
    private ListView lv_downloading, lv_downloaded;
    private LinearLayout ll_downloading, ll_downloaded;
    private ViewPager viewPager;
    private TranslateAnimation transAnima;
    private ImageView indicator = null;
    private ArrayList<View> views;
    private int currentIndex = 0;
    int width;
    private DownloadManager downloadManager;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dlface);
        downloadManager = DownloadManager.getInstance(this);
        initView();

    }


    private void initView() {
        DisplayMetrics dms = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dms);
        width = dms.widthPixels / 2 - 50;
        indicator = (ImageView) findViewById(R.id.iv_line);
        LayoutInflater lf = getLayoutInflater().from(this);
        v_downloading = lf.inflate(R.layout.downloading_tab, null);
        v_downloaded = lf.inflate(R.layout.downloaded_tab, null);

        views = new ArrayList<View>();
        views.add(v_downloading);
        views.add(v_downloaded);

        ll_downloading = (LinearLayout) findViewById(R.id.ll_downloading);
        ll_downloaded = (LinearLayout) findViewById(R.id.ll_downloaded);

        ll_downloading.setOnClickListener(tabClickHandler);
        ll_downloaded.setOnClickListener(tabClickHandler);

        lv_downloading = (ListView) v_downloading.findViewById(R.id.lv_downloading);
        lv_downloaded = (ListView) v_downloaded.findViewById(R.id.lv_downloaded);


        downloadListAdapter = DownloadingListAdapter.getInstance().init(this, lv_downloading);
        downloadedListAdapter = DownloadedListAdapter.getInstance().init(this, lv_downloaded);

        lv_downloading.setAdapter(downloadListAdapter);
        lv_downloaded.setAdapter(downloadedListAdapter);

        addButton = (Button) v_downloading.findViewById(R.id.btn_add);
        resetButton = (Button) v_downloading.findViewById(R.id.btn_reset);

        addButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                /*
                 * 入口
				 * 1、生成taskId
				 * 2、插入数据库记录
				 * 3、添加下载任务
				 *
				 */


                try {
                    JSONObject downloadSteategyJsonObj = new JSONObject();
                    downloadSteategyJsonObj.put(DownloadStrategy.SEGMENT_KEY, 5);
                    downloadSteategyJsonObj.put(DownloadStrategy.VISIBILITY_KEY, 0);
                    DownloadTask task = downloadManager.postTask(DownloadUrls.url[urlIndex], DownloadUrls.apkName[urlIndex], null, downloadSteategyJsonObj);
                    if (task != null && task.getId() >= 0 && task.getStatus() == DownloadTaskStatus.WAITING) {
                        downloadListAdapter.addItem(task.getId(), DownloadUrls.url[urlIndex], false);
                    }
                    urlIndex++;
                    downloadManager.setDownLoadCallback(new DownLoadCallback() {
                        @Override
                        public void onStart(long taskId, String url) {
                            super.onStart(taskId, url);
                            Log.e("YCW", "onStart-taskId--->" + taskId + ";url--->" + url);
                        }


                        @Override
                        public void onFailure(long taskId, String url, int erro) {
                            super.onFailure(taskId, url, erro);
                            Log.e("YCW", "onFailure-taskId--->" + taskId + ";url--->" + url);
                        }


                        @Override
                        public void onLoading(long taskId, String url, long totalSize, long currentSize, long speed, int percent) {
                            super.onLoading(taskId, url, totalSize, currentSize, speed, percent);
                            Log.e("YCW", "onLoading-taskId--->" + taskId + ";url--->" + url + ";percent--->" + percent);
                            downloadListAdapter.setDownLoaderProgress(url, totalSize, currentSize, speed);
                        }


                        @Override
                        public void onFinish(long taskId, String url) {
                            super.onFinish(taskId, url);
                            Log.e("YCW", "onFinish-taskId--->" + taskId + ";url--->" + url);
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }


            }
        });
        resetButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
        /*
          * 0、清空数据库
           * 1、清空队列
          * 2、重置url索引
           * 3、清空下载文件
           *
        */


                downloadListAdapter.reInit();
                downloadListAdapter = DownloadingListAdapter.getInstance().init(v.getContext(), lv_downloading);
                lv_downloading.setAdapter(downloadListAdapter);

                downloadManager.deleteAllHandler();

                downloadListAdapter.reInit();
                downloadedListAdapter.reset();

                deleteAllFiles(new File("/sdcard/adsagedlplugin/"));
                urlIndex = 0;
                long a = 10L;
                int bb = new Long(a).intValue();

                String proxyHost = Proxy.getHost(v.getContext());
                int proxyPort = Proxy.getPort(v.getContext());


            }
        });

        viewPager = (ViewPager) findViewById(R.id.viewpager);
        viewPager.setAdapter(new MyPageAdapter());

        viewPager.setOnPageChangeListener(new OnPageChangeListener() {

            @Override
            public void onPageSelected(int arg0) {

                initAnimation(arg0);
            }

            @Override
            public void onPageScrolled(int arg0, float arg1, int arg2) {


            }

            @Override
            public void onPageScrollStateChanged(int arg0) {


            }
        });
    }

    OnClickListener tabClickHandler = new OnClickListener() {
        @Override
        public void onClick(View v) {
            int tempIdx = 0;
            if (v == ll_downloading) {
                tempIdx = 0;
            } else if (v == ll_downloaded) {
                tempIdx = 1;
            }
            if (currentIndex != tempIdx) {
                initAnimation(tempIdx);
                viewPager.setCurrentItem(currentIndex);
            }
        }
    };

    private void initAnimation(int moveto) {
        transAnima = new TranslateAnimation(currentIndex * width, moveto
                * width, 0, 0);
        transAnima.setFillAfter(true);
        transAnima.setDuration(200);
        indicator.startAnimation(transAnima);
        currentIndex = moveto;
    }

    private void deleteAllFiles(File root) {
        File files[] = root.listFiles();
        if (files != null)
            for (File f : files) {
                if (f.isDirectory()) { // 判断是否为文件夹  
                    deleteAllFiles(f);
                    try {
                        f.delete();
                    } catch (Exception e) {
                    }
                } else {
                    if (f.exists()) { // 判断是否存在  
                        deleteAllFiles(f);
                        try {
                            f.delete();
                        } catch (Exception e) {
                        }
                    }
                }
            }
    }


    private class MyPageAdapter extends PagerAdapter {
        @Override
        public Object instantiateItem(ViewGroup container, int position) {

            container.addView(views.get(position));
            return views.get(position);
        }

        @Override
        public int getCount() {

            return views.size();
        }

        @Override
        public boolean isViewFromObject(View arg0, Object arg1) {

            return arg0 == arg1;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {

            container.removeView(views.get(position));
        }

        @Override
        public int getItemPosition(Object object) {

            return super.getItemPosition(object);
        }


    }
}