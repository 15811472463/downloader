
package com.downloader.sdk.dlplugin;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import com.downloader.sdk.dlplugin.util.common.ApkUtil;
import com.downloader.sdk.dlplugin.util.common.FileInfoUtils;
import com.downloader.sdk.dlplugin.util.common.SdCardUtil;
import com.downloader.sdk.dlplugin.util.common.StringUtils;
import com.downloader.sdk.dlplugin.util.db.DBUtil;
import com.downloader.sdk.dlplugin.util.http.AsyncHttpClient;
import com.downloader.sdk.dlplugin.util.http.AsyncHttpResponseHandler;
import com.downloader.sdk.dlplugin.util.http.FileHttpResponseHandler;
import com.downloader.sdk.dlplugin.util.log.Logger;
import com.downloader.sdk.dlplugin.util.netstate.NetChangeObserver;
import com.downloader.sdk.dlplugin.util.netstate.NetWorkUtil;
import com.downloader.sdk.dlplugin.util.netstate.NetWorkUtil.netType;
import com.downloader.sdk.dlplugin.util.netstate.NetworkStateReceiver;
import com.downloader.sdk.dlplugin.util.notification.NotificationHelper;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.ProtocolException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.RedirectHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * @author ycw
 */

public class DownloadManager extends Thread {
    private HandlerQueue mhandlerQueue;// queue for waiting task
    private static List<AsyncHttpResponseHandler> mDownloadinghandlers;// store
    private static List<AsyncHttpResponseHandler> mPausinghandlers;// store
    private AsyncHttpClient mAsyncHttpClient;
    private Boolean isRunning = false;
    private DownLoadCallback mDownLoadCallback;
    private String mRootPath = "";
    // cache all realtime task status
    private ArrayList<DownloadTaskStatus> mDownloadTaskStatusData;
    private HashMap<Long, DownloadStrategy> mDownloadStrategyHashMap;

    private NotificationHelper mNotificationHelper;

    private Context mContext;
    private DBUtil mDbutil;
    private static DownloadManager mDownloadManager;
    private final static String TAG = "DownloadManager";
    public static final String NOTIFICATION_CLICK_ACTION = "com.downloader.notification_click_action";

    public static final String NOFINISH_NOTIFICATION_CLICK_ACTION = "com.downloader.notification_nofinishclick_action";

    private static BroadcastReceiver mNotifactionClickReceiver;

    private boolean mIsWorking = true;

    private static BroadcastReceiver sSdCardReceiver;
    private static String mDataBaseName;
    private static boolean mIsSaveOldData;
    private static int mDbHashCode;
    private static Handler myHandler;


    public static DownloadManager getInstance(Context context) {
        if (mDownloadManager == null) {
            mDownloadManager = new DownloadManager(context,
                    DownLoadConfig.FILE_ROOT, "mftour_download", false);
            mDataBaseName = "mftour_download";
        }
        return mDownloadManager;

    }

    /**
     * @param context
     * @param dataBaseName
     * @param downLoadPath
     * @param isSaveOldData
     * @return
     */
    public static DownloadManager getInstance(Context context,
                                              String dataBaseName, String downLoadPath, boolean isSaveOldData) {
        Logger.d(TAG, "getInstance(" + "context," + dataBaseName + "," + downLoadPath + ","
                + isSaveOldData + ")");
        if (!TextUtils.isEmpty(downLoadPath)) {
            DownLoadConfig.FILE_ROOT = downLoadPath;
        }

        if (mDownloadManager == null) {
            mDownloadManager = new DownloadManager(context,
                    DownLoadConfig.FILE_ROOT, dataBaseName, isSaveOldData);
            mDataBaseName = dataBaseName;
        }
        return mDownloadManager;
    }

    private DownloadManager(Context context, String rootPath, String dataBaseName,
                            boolean isSaveOldDataBase) {
        initParams(context, rootPath, dataBaseName, isSaveOldDataBase);
        // 获取cup颗数，动态设置并发数
        setWorkerCount();
        sSdCardReceiver = getSdReceiver();
        // 注册sd 监听器
        registerSdReceiver(sSdCardReceiver);
        // sd 卡监听
        // register network state receiver
        NetworkStateReceiver.registerNetworkStateReceiver(mContext);
        NetworkStateReceiver.registerObserver(new NetChangeObserver() {
            @SuppressWarnings("static-access")
            @Override
            public void onConnect(netType type) {
                mIsWorking = true;
                if (null != NetWorkUtil.getAPNType(mContext)
                        && NetWorkUtil.getAPNType(mContext) != type.wifi) {
                    // 说明是移动网络，延时，改变重试次数
                    if (mAsyncHttpClient != null) {
                        mAsyncHttpClient
                                .setTimeout(DownLoadConfig.unWifiTimeOut);
                    }

                } else {
                    if (mAsyncHttpClient != null) {
                        mAsyncHttpClient.setTimeout(DownLoadConfig.wifiTimeOut);
                    }
                }
                showToast("网络已连接");
                continueAllHandler();
            }

            @Override
            public void onDisConnect() {
                mIsWorking = false;
                showToast("网络已断开");
                pauseAllHandler();
            }

        });

        // register notification click action recevier
        if (mNotifactionClickReceiver == null) {
            mNotifactionClickReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (intent.getAction().equalsIgnoreCase(
                            NOTIFICATION_CLICK_ACTION)) {
                        Bundle extras = intent.getExtras();
                        long mTaskId = extras.getLong("taskid");
                        String mTaskUrl = extras.getString("taskurl");
                        sendOnClickMessageToCallback(mTaskId, mTaskUrl);

                    } else if (intent.getAction().equalsIgnoreCase(
                            NOFINISH_NOTIFICATION_CLICK_ACTION)) {
                        Bundle extras = intent.getExtras();
                        long taskId = extras.getLong("taskid");
                        String taskUrl = extras.getString("taskurl");
                        pauseOrContinue(taskId, taskUrl);
                    }
                }
            };
        }

        registerNotificationClickReceiver(mNotifactionClickReceiver);

        if (!StringUtils.isEmpty(rootPath)) {
            File rootFile = new File(rootPath);
            if (!rootFile.exists()) {
                rootFile.mkdir();
            }
        }
        cleanOutdatedDownloadedRecords();
    }

    private BroadcastReceiver getSdReceiver() {
        sSdCardReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String actionStr = intent.getAction();
                Logger.d(TAG, "sdAction--->" + actionStr);
                // if (Intent.ACTION_MEDIA_UNMOUNTED.equals(actionStr)) {
                // List<DownloadTask> downloadingTaskList = mDbutil
                // .findDownLoadingTask();
                // if (downloadingTaskList != null
                // && downloadingTaskList.size() > 0) {
                // for (DownloadTask mTask : downloadingTaskList) {
                // if (mDownLoadCallback != null) {
                // mDownLoadCallback.sendFailureMessage(
                // mTask.getId(), mTask.getUrl(),
                // Erros.NO_SD);
                // Logger.d(TAG, "sdAction--->" + actionStr
                // + ";;mTaskId-->" + mTask.getId());
                // }
                //
                // }
                // }
                //
                // }

            }

        };
        return sSdCardReceiver;
    }

    private void registerSdReceiver(BroadcastReceiver sdCardReceiver) {
        IntentFilter intentFilter = new IntentFilter(
                Intent.ACTION_MEDIA_MOUNTED);
        intentFilter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
        intentFilter.addAction(Intent.ACTION_MEDIA_EJECT);
        intentFilter.addDataScheme("file");
        if (mContext != null) {
            mContext.registerReceiver(sdCardReceiver, intentFilter);
        }

    }

    private void unRegisterSdReceiver() {
        if (mContext != null) {
            mContext.unregisterReceiver(sSdCardReceiver);

        }

    }

    private void setWorkerCount() {
        // int processNum = 0;
        int processNum = Runtime.getRuntime().availableProcessors();
        DownLoadConfig.MAX_DOWNLOAD_THREAD_COUNT = processNum + 1;
        Logger.d(TAG, "processNum-->" + processNum);
    }

    private void initParams(Context context, String rootPath, String dataBaseName,
                            boolean isSaveOldData) {
        this.mDbHashCode = dataBaseName.hashCode();
        this.mRootPath = rootPath;
        this.mIsSaveOldData = isSaveOldData;
        this.mContext = context.getApplicationContext();
        if (!TextUtils.isEmpty(dataBaseName) && !"dlplugin_database".equals(dataBaseName)) {
            this.mDbutil = DBUtil.getInstance(mContext, dataBaseName, isSaveOldData);
        } else {
            this.mDbutil = DBUtil.getInstance(mContext, "dlplugin_database", isSaveOldData);
        }
        mhandlerQueue = new HandlerQueue();
        mDownloadinghandlers = new ArrayList<AsyncHttpResponseHandler>();
        mPausinghandlers = new ArrayList<AsyncHttpResponseHandler>();
        mDownloadTaskStatusData = new ArrayList<DownloadTaskStatus>();
        mDownloadStrategyHashMap = new HashMap<Long, DownloadStrategy>();
        mNotificationHelper = new NotificationHelper(mContext);
        mNotificationHelper.cancelAllNotifactions();
        mAsyncHttpClient = new AsyncHttpClient();
        myHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                // TODO Auto-generated method stub
                super.handleMessage(msg);
                if (msg.what == -1) {
                    mNotificationHelper.cancelNotification(msg.arg1 + mDbHashCode);
                }
            }
        };
    }

    private void pauseOrContinue(long taskId, String url) {
        DownloadTask mDownloadTask = mDbutil.fetchOneDownload(taskId);
        if (mDownloadTask != null) {
            if (mDownloadTask.getStatus() == DownloadTaskStatus.DOWNLOADING
                    && DownLoadConfig.isClickable) {
                pauseTask(taskId);
                mNotificationHelper.updateNotificationTicker(taskId + mDbHashCode, "暂停下载");
            } else if (mDownloadTask.getStatus() == DownloadTaskStatus.PAUSEING
                    && DownLoadConfig.isClickable) {
                String postFix = FileInfoUtils.getPostFix(url);
                continueHandler(url, mDownloadTask.getVisibility());
                mNotificationHelper.updateNotificationTicker(taskId + mDbHashCode, "继续下载");
            }

        }

    }

    public void sendOnClickMessageToCallback(long taskId, String url) {
        if (mDownLoadCallback != null)
            mDownLoadCallback.sendClickNotifationMesage(taskId, url);
    }

    public void registerNotificationClickReceiver(BroadcastReceiver mreceiver) {
        IntentFilter mFilter = new IntentFilter();
        // filter.addAction(DL_ANDROID_NET_CHANGE_ACTION);
        mFilter.addAction(NOTIFICATION_CLICK_ACTION);
        mFilter.addAction(NOFINISH_NOTIFICATION_CLICK_ACTION);
        mContext.getApplicationContext().registerReceiver(mreceiver, mFilter);
    }

    public String getRootPath() {
        if (StringUtils.isEmpty(mRootPath)) {
            mRootPath = DownLoadConfig.FILE_ROOT;
        }
        return mRootPath;
    }

    public void setDownLoadCallback(DownLoadCallback downLoadCallback) {
        this.mDownLoadCallback = downLoadCallback;
    }

    public void toStartMananger() {

        isRunning = true;
        this.start();
        if (mDownLoadCallback != null) {
            mDownLoadCallback.sendStartMangerMessage();
        }
        // checkUncompletehandlers();
    }

    public void close() {
        isRunning = false;
        pauseAllHandler();
        if (mDownLoadCallback != null) {
            mDownLoadCallback.sendStopMessage();
        }
        this.stop();
    }

    @Override
    public void run() {

        super.run();
        while (isRunning) {
            if (mIsWorking) {
                toRequestDownLoad();
            }
        }
    }

    private void toRequestDownLoad() {
        FileHttpResponseHandler handler = (FileHttpResponseHandler) mhandlerQueue
                .poll();
        if (handler.isFirstStart()) {
            Logger.d(TAG,
                    "taskId--->" + handler.getTaskId() + ";" + handler.getUrl()
                            + ";开始下载");
            if (mDownLoadCallback != null) {
                mDownLoadCallback.sendStartDownLoading(handler.getTaskId(),
                        handler.getUrl());
            }
        }

        if (handler != null) {
            mDownloadinghandlers.add(handler);
            DownloadTaskStatus status = getTaskRealtimeStatus(
                    handler.getTaskId(), handler.getUrl());
            if (status != null) {
                status.setCurrentStatus(DownloadTaskStatus.DOWNLOADING);
            }
            handler.setInterrupt(false);
            String url = handler.getUrl();
            DefaultHttpClient httpClient = new DefaultHttpClient();
            String[] redirectArrays;
            try {
                redirectArrays = handleRedirect(handler, httpClient, url);

                String redirectUrl = redirectArrays[0];
                String mineTypePostFix = MimeTypeMap.getSingleton()
                        .getExtensionFromMimeType(redirectArrays[1]);
                String postFix = FileInfoUtils.getPostFix(redirectUrl);
                try {
                    toDownLoad(handler, redirectUrl, mineTypePostFix, postFix);
                } catch (Exception e) {
                    cancelNotifactionByFileResponseHandler(handler);
                    if (mDownLoadCallback != null) {
                        mDownLoadCallback.sendFailureMessage(
                                handler.getTaskId(), url, Erros.URL_EXECEPTION);
                    }
                    Logger.e(TAG, e.toString());
                }
            } catch (ClientProtocolException e) {
                cancelNotifactionByFileResponseHandler(handler);
                if (mDownLoadCallback != null) {
                    mDownLoadCallback.sendFailureMessage(handler.getTaskId(),
                            url, Erros.URL_EXECEPTION);
                }
                Logger.e(TAG, e.toString());
            } catch (MyRedirectException e) {
                cancelNotifactionByFileResponseHandler(handler);
                if (mDownLoadCallback != null) {
                    mDownLoadCallback.sendFailureMessage(handler.getTaskId(),
                            url, Erros.URL_EXECEPTION);
                }
                Logger.e(TAG, e.toString());
            } catch (IOException e) {
                // TODO Auto-generated catch block
                cancelNotifactionByFileResponseHandler(handler);
                if (mDownLoadCallback != null) {
                    mDownLoadCallback.sendFailureMessage(handler.getTaskId(),
                            url, Erros.URL_EXECEPTION);
                }
                Logger.e(TAG, e.toString());
            }

        }

        // saveFailedTask(handler);
        // cancelNotifactionByFileResponseHandler(handler);
        // if (mDownLoadCallback != null) {
        // mDownLoadCallback.sendFailureMessage(handler.getTaskId(),
        // url, Erros.URL_EXECEPTION);
        // }
    }

    private void cancelNotifactionByFileResponseHandler(
            FileHttpResponseHandler handler) {
        if (handler != null) {
            mNotificationHelper.cancelNotification(handler.getTaskId() + mDbHashCode);

        }
    }

    private void saveFailedTask(FileHttpResponseHandler handler) {
        if (mDownloadinghandlers.contains(handler)) {
            mDownloadinghandlers.remove(handler);
        }
        mhandlerQueue.offer(handler);
    }

    private void toDownLoad(FileHttpResponseHandler handler,
                            String redirectUrl, String mineTypePostFix, String postFix)
            throws Exception {
        if (TextUtils.isEmpty(mineTypePostFix) && !TextUtils.isEmpty(postFix)) {
            handler.setContextTypePostFix(mineTypePostFix);
            RenameTempFileByUrlPostFix(handler, redirectUrl);
        } else if (!TextUtils.isEmpty(mineTypePostFix)
                && TextUtils.isEmpty(postFix)) {
            String filePath = handler.getFile().getAbsolutePath();
            File tempFile = new File(filePath + "." + mineTypePostFix
                    + FileHttpResponseHandler.getTempSuffix());
            File file = new File(filePath + "." + mineTypePostFix);
            handler.setTempFile(tempFile);
            handler.setFile(file);
            mDbutil.updateDownload(handler.getTaskId(), null,
                    handler.getCnname(), null, DownloadTaskStatus.DOWNLOADING,
                    0, filePath, null);
        } else if (!TextUtils.isEmpty(mineTypePostFix)
                && !TextUtils.isEmpty(postFix)) {
            handler.setContextTypePostFix(mineTypePostFix);
            RenameTempFileByUrlPostFix(handler, redirectUrl);
        }
        mAsyncHttpClient.download(redirectUrl, handler);
    }

    /**
     * 根据url 的后缀名命名
     *
     * @param handler
     * @param url
     */
    private void RenameTempFileByUrlPostFix(FileHttpResponseHandler handler,
                                            String url) {
        String fileName = StringUtils.getOriginalFileNameFromUrl(url);
        if (TextUtils.isEmpty(fileName)) {
            fileName = System.currentTimeMillis() + "."
                    + handler.getContextTypePostFix();
        }
        String mFilePath = handler.getBaseDirFile() + File.separator + fileName;
        File tempFile = new File(mFilePath
                + FileHttpResponseHandler.getTempSuffix());
        File file = new File(mFilePath);
        handler.setTempFile(tempFile);
        handler.setFile(file);
        mDbutil.updateDownload(handler.getTaskId(), null, handler.getCnname(),
                null, DownloadTaskStatus.DOWNLOADING, 0, mFilePath, null);
    }

    private static class MyRedirectException extends RuntimeException {
        /**
         *
         */
        private static final long serialVersionUID = 1L;

        public MyRedirectException() {
            super();
        }

    }

    private static class MyRedirectHandler implements RedirectHandler {
        private String mFinalUrl = null;
        private int mRedirectCount = 0;
        private String mContentType = null;

        public MyRedirectHandler(String url) {
            mFinalUrl = url;
        }

        @Override
        public boolean isRedirectRequested(HttpResponse response,
                                           HttpContext context) {
            int statueCode = response.getStatusLine().getStatusCode();
            if (statueCode == HttpStatus.SC_MOVED_TEMPORARILY
                    || statueCode == HttpStatus.SC_MOVED_PERMANENTLY) {
                if (mRedirectCount > DownLoadConfig.REDIRECT_COUNT) {
                    throw new MyRedirectException();
                }
                mRedirectCount++;
                return true;
            }
            mContentType = response.getFirstHeader("Content-Type").getValue();
            Logger.d(TAG, "contentType--->" + mContentType);

            return false;
        }

        @Override
        public URI getLocationURI(HttpResponse response, HttpContext context)
                throws ProtocolException {
            URI uri = null;
            Header locationHeader = response.getFirstHeader("location");
            if (locationHeader != null) {
                String url = locationHeader.getValue();
                try {
                    uri = new URI(url);
                    mFinalUrl = uri.toString();
                } catch (URISyntaxException e) {
                }
            }
            return uri;
        }

        public String getFinalUrl() {
            return mFinalUrl;
        }

        public String getContentType() {
            return mContentType;
        }

    }

    private String[] handleRedirect(FileHttpResponseHandler handler,
                                    DefaultHttpClient httpClient, String url)
            throws ClientProtocolException, IOException, MyRedirectException {
        String[] mResult = new String[2];
        try {
            HttpGet request = new HttpGet(url);
            HttpParams params = httpClient.getParams();
            HttpClientParams.setRedirecting(params, true);
            request.setParams(params);
            httpClient.setParams(params);
            MyRedirectHandler redirectHandler = new MyRedirectHandler(url);
            httpClient.setRedirectHandler(redirectHandler);
            httpClient.execute(request);
            if (!TextUtils.isEmpty(redirectHandler.getFinalUrl())) {
                mResult[0] = redirectHandler.getFinalUrl();
                mResult[1] = redirectHandler.getContentType();
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            Logger.e(TAG, e.toString());
            if (e != null) {
                Message message = new Message();
                message.arg1 = (int) handler.getTaskId();
                message.what = -1;
                myHandler.sendMessage(message);
            }
        }
        return mResult;
    }

    /*
     * remove outdated download records
     */
    public void cleanOutdatedDownloadedRecords() {
        long latestUpdateTime = mDbutil.getLatestCleanTime();
        if ((System.currentTimeMillis() - latestUpdateTime) >= DownLoadConfig.CLEAN_HISTORY_INTERVAL) {
            mDbutil.removeCompletedDownloadTask();
        }

    }

    /**
     * 7天清除apk 和临时文件
     */
    public void cleanOutDateApkFile() {
        if (mDbutil != null) {
            ArrayList<DownloadTask> downloadTasksList = mDbutil
                    .findAllTask();
            for (DownloadTask task : downloadTasksList) {
                Logger.d(TAG, "savePath--->" + task.getSavepath()
                        + ";createTime--->" + task.getCreatetime());
                long createTime = task.getCreatetime();
                if (System.currentTimeMillis() - createTime >= DownLoadConfig.CLEAN_APK_OUTDATE) {
                    String path = task.getSavepath();
                    String tempPath = task.getSavepath()
                            + FileHttpResponseHandler.TEMP_SUFFIX;
                    if (!TextUtils.isEmpty(path)) {
                        File apkFile = new File(path);
                        if (apkFile.exists()) {
                            apkFile.delete();
                        } else {
                            File apkTempFile = new File(tempPath);
                            apkTempFile.delete();
                        }
                    }
                }
            }
        }
    }

    /**
     * @param type=0 恢复 有通知 条的任务 ，type =1 恢复 静默 任务 .type=2 恢复所有 未完成的任务
     */
    public void restartUnCompletedTask(int type) {
        List<DownloadTask> taskList = mDbutil.findUnCompletedTask();
        if (type == DownloadStrategy.ALL_UNCOMPELETED) {
            if (taskList != null) {
                int listSize = taskList.size();
                if (listSize > 0) {
                    for (DownloadTask task : taskList) {
                        if (task.getVisibility() == DownloadStrategy.VISIBILITY) {
                            mNotificationHelper.addNotification(task.getId() + mDbHashCode, -1,
                                    StringUtils.getOriginalFileNameFromUrl(task
                                            .getUrl()), task.getCnname(), task
                                            .getUrl());

                        }

                        DownloadStrategy downloadStrategy = DownloadStrategy
                                .parseFromJsonString(task.getDownloadStrategy());

                        addHandler(task.getId(), task.getUrl(),
                                downloadStrategy != null ? downloadStrategy : null,
                                task.getCnname(), false);
                    }
                }
            }
        } else if (type == DownloadStrategy.VISIBILITY) {
            if (taskList != null) {
                int listSize = taskList.size();
                if (listSize > 0) {
                    for (DownloadTask task : taskList) {
                        if (task.getVisibility() == DownloadStrategy.VISIBILITY) {
                            mNotificationHelper.addNotification(task.getId() + mDbHashCode, -1,
                                    StringUtils.getOriginalFileNameFromUrl(task
                                            .getUrl()), task.getCnname(), task
                                            .getUrl());
                            DownloadStrategy downloadStrategy = DownloadStrategy
                                    .parseFromJsonString(task.getDownloadStrategy());

                            addHandler(task.getId(), task.getUrl(),
                                    downloadStrategy != null ? downloadStrategy : null,
                                    task.getCnname(), false);

                        }

                    }
                }
            }

        } else if (type == DownloadStrategy.GONE) {
            if (taskList != null) {
                int listSize = taskList.size();
                if (listSize > 0) {
                    for (DownloadTask task : taskList) {
                        if (task.getVisibility() == DownloadStrategy.GONE) {
                            DownloadStrategy downloadStrategy = DownloadStrategy
                                    .parseFromJsonString(task.getDownloadStrategy());

                            addHandler(task.getId(), task.getUrl(),
                                    downloadStrategy != null ? downloadStrategy : null,
                                    task.getCnname(), false);

                        }

                    }
                }
            }
        }

    }

    public void addHandler(long taskId, String url,
                           DownloadStrategy mDownLoadStrategy, String cnname,
                           boolean isFirstStart) {

        if (getTotalhandlerCount() >= DownLoadConfig.MAX_handler_COUNT) {
            return;
        }
        if (TextUtils.isEmpty(url) || hasHandler(taskId, url)) {
            Logger.d(TAG, "任务中存在这个任务,或者任务不满足要求");
            return;
        }

        try {
            if (isFirstStart) {
                addHandler(
                        newcHttpResponseHandler(mDownLoadStrategy, taskId, url,
                                cnname, true), mDownLoadStrategy);
            } else {
                addHandler(
                        newcHttpResponseHandler(mDownLoadStrategy, taskId, url,
                                cnname, false), mDownLoadStrategy);
            }

            // Logger.d(TAG, "下载完成！！");

        } catch (MalformedURLException e) {
            Logger.e(TAG, e.toString());
        }

    }

    private void addHandler(AsyncHttpResponseHandler handler,
                            DownloadStrategy downLoadStrategy) {
        FileHttpResponseHandler fhandler = (FileHttpResponseHandler) handler;
        fhandler.setmDownLoadCallback(mDownLoadCallback);
        broadcastAddHandler(fhandler.getTaskId(), fhandler.getUrl());
        mhandlerQueue.offer(handler);
        mDownloadTaskStatusData.add(new DownloadTaskStatus(
                fhandler.getTaskId(), fhandler.getUrl())
                .setCurrentStatus(DownloadTaskStatus.WAITING));
        if (downLoadStrategy != null)
            mDownloadStrategyHashMap
                    .put(fhandler.getTaskId(), downLoadStrategy);
        if (!this.isAlive()) {
            this.toStartMananger();
            Logger.d(TAG, "toStartMananger()");
        }
    }

    private void broadcastAddHandler(long taskId, String url)

    {
        broadcastAddHandler(taskId, url, false);
    }

    private void broadcastAddHandler(long taskId, String url,
                                     boolean isInterrupt) {

        if (mDownLoadCallback != null) {
            mDownLoadCallback.sendAddMessage(taskId, url, false);
        }

    }

    /**
     * by robin public void reBroadcastAddAllhandler() { FileHttpResponseHandler
     * handler; for (int i = 0; i < mDownloadinghandlers.size(); i++) { handler
     * = (FileHttpResponseHandler) mDownloadinghandlers.get(i);
     * broadcastAddHandler(handler.getTaskId(),handler.getUrl(),
     * handler.isInterrupt()); } for (int i = 0; i < mhandlerQueue.size(); i++)
     * { handler = (FileHttpResponseHandler) mhandlerQueue.get(i);
     * broadcastAddHandler(handler.getTaskId(),handler.getUrl()); } for (int i =
     * 0; i < mPausinghandlers.size(); i++) { handler =
     * (FileHttpResponseHandler) mPausinghandlers.get(i);
     * broadcastAddHandler(handler.getTaskId(),handler.getUrl()); } }
     */
    public boolean hasHandler(long taskId, String url) {
        boolean mResult = false;
        try {
            FileHttpResponseHandler handler;
            if (mDownloadinghandlers != null && mDownloadinghandlers.size() > 0) {
                for (int i = 0; i < mDownloadinghandlers.size(); i++) {
                    handler = (FileHttpResponseHandler) mDownloadinghandlers
                            .get(i);
                    if (handler.getUrl().equals(url)) {
                        mResult = true;
                    }
                }
            }
            if (mhandlerQueue != null && mhandlerQueue.size() > 0) {
                for (int i = 0; i < mhandlerQueue.size(); i++) {
                    handler = (FileHttpResponseHandler) mhandlerQueue.get(i);
                    if (handler.getUrl().equals(url)) {
                        mResult = true;
                    }
                }

            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            Logger.e(TAG, e.toString());
        }
        return mResult;
    }

    public AsyncHttpResponseHandler gethandler(int postion) {

        if (postion >= mDownloadinghandlers.size()) {
            return mhandlerQueue.get(postion - mDownloadinghandlers.size());
        } else {
            return mDownloadinghandlers.get(postion);
        }
    }

    /**
     * ycw 判断文件状态
     *
     * @param url
     * @return
     */
    public DownloadTask getDownloadTask(String url) {
        if (mDbutil != null) {
            if (!TextUtils.isEmpty(url)) {
                DownloadTask downloadTask = mDbutil.findDownloadByUrl(url);
                if (downloadTask == null) {
                    return null;
                }
                if (downloadTask.getStatus() == DownloadTaskStatus.COMPLETED) {
                    String filePath = downloadTask.getSavepath();
                    File file = new File(filePath);
                    if (TextUtils.isEmpty(filePath) || !file.exists()) {
                        downloadTask.setStatus(DownloadTaskStatus.UNCOMPLETED);
                    }
                }

                return downloadTask;
            } else {
                return null;
            }
        } else {
            return null;
        }

    }

    public int getTotalhandlerCount() {

        // by robin return getQueuehandlerCount() + getDownloadinghandlerCount()
        // + getPausinghandlerCount();
        return mDownloadTaskStatusData.size();
    }

    public synchronized void pauseAllHandler() {

        AsyncHttpResponseHandler handler;
        if (mhandlerQueue.size() > 0) {
            for (int i = 0; i < mhandlerQueue.size(); i++) {
                handler = mhandlerQueue.get(i);
                handler.setFirstStart(false);
                mhandlerQueue.remove(handler);
                mPausinghandlers.add(handler);
            }
        }
        if (mDownloadinghandlers.size() > 0) {
            for (int i = 0; i < mDownloadinghandlers.size(); i++) {
                handler = mDownloadinghandlers.get(i);
                if (handler != null) {
                    handler.setFirstStart(false);
                    pausehandler(handler);
                }
            }
        }

    }

    public synchronized void deleteHandler(long taskId) {

        FileHttpResponseHandler handler;
        for (int i = 0; i < mDownloadinghandlers.size(); i++) {
            handler = (FileHttpResponseHandler) mDownloadinghandlers.get(i);
            if (handler != null && handler.getTaskId() == taskId) {
                File file = handler.getFile();
                if (file.exists())
                    file.delete();
                File tempFile = handler.getTempFile();
                if (tempFile.exists()) {
                    tempFile.delete();
                }
                handler.setInterrupt(true);
                completehandler(handler);
                return;
            }
        }
        for (int i = 0; i < mhandlerQueue.size(); i++) {
            handler = (FileHttpResponseHandler) mhandlerQueue.get(i);
            if (handler != null && handler.getTaskId() == taskId) {
                mhandlerQueue.remove(handler);
            }
        }
        for (int i = 0; i < mPausinghandlers.size(); i++) {
            handler = (FileHttpResponseHandler) mPausinghandlers.get(i);
            if (handler != null && handler.getTaskId() == taskId) {
                mPausinghandlers.remove(handler);
            }
        }
    }

    public synchronized void deleteAllHandler() {
        // pauseAllHandler();
        FileHttpResponseHandler handler;
        for (int i = 0; i < mDownloadinghandlers.size(); i++) {
            handler = (FileHttpResponseHandler) mDownloadinghandlers.get(i);
            handler.setmDownLoadCallback(mDownLoadCallback);
            if (handler != null) {
                File file = handler.getFile();
                if (file.exists())
                    file.delete();
                File tempFile = handler.getTempFile();
                if (tempFile.exists()) {
                    tempFile.delete();
                }
                handler.setInterrupt(true);
                completehandler(handler);
                return;
            }
        }
        for (int i = 0; i < mhandlerQueue.size(); i++) {
            handler = (FileHttpResponseHandler) mhandlerQueue.get(i);
            if (handler != null) {
                mhandlerQueue.remove(handler);
            }
        }
        for (int i = 0; i < mPausinghandlers.size(); i++) {
            handler = (FileHttpResponseHandler) mPausinghandlers.get(i);
            if (handler != null) {
                mPausinghandlers.remove(handler);
            }
        }
    }

    private synchronized void pausehandler(AsyncHttpResponseHandler response) {

        try {
            FileHttpResponseHandler fileHttpResponseHandler = (FileHttpResponseHandler) response;
            if (response != null) {
                // move to pausing list
                long taskId = fileHttpResponseHandler.getTaskId();
                String url = fileHttpResponseHandler.getUrl();
                fileHttpResponseHandler.setInterrupt(true);
                DownloadTask downloadTask = mDbutil.fetchOneDownload(taskId);
                if (downloadTask != null) {
                    DownloadStrategy downloadStrategy = DownloadStrategy
                            .parseFromJsonString(downloadTask
                                    .getDownloadStrategy());
                    if (mDownLoadCallback != null
                            && !NetWorkUtil.isNetworkAvailable(mContext)) {
                        mDownLoadCallback.sendFailureMessage(taskId, url,
                                Erros.NO_NETWORK);
                    }
                    if (mDownloadinghandlers != null) {
                        mDownloadinghandlers.remove(response);
                        String cname = getCName(url);
                        response = newcHttpResponseHandler(downloadStrategy,
                                taskId, url, cname, false);
                        mPausinghandlers.add(response);
                    }

                    DownloadTaskStatus downloadTaskStatus = getTaskRealtimeStatus(
                            taskId, url);
                    if (downloadTaskStatus != null) {
                        downloadTaskStatus
                                .setCurrentStatus(DownloadTaskStatus.PAUSEING);

                    }
                    syncDownloadStatusWithDB(taskId,
                            DownloadTaskStatus.PAUSEING, null, null, null);

                }
            }

        } catch (MalformedURLException e) {
            // TODO Auto-generated catch block
            Logger.e(TAG, e.toString());
        }

    }

    private String getCName(String url) {
        DownloadTask mDownloadTask = mDbutil.findDownloadByUrl(url);
        String cname = "";
        if (mDownloadTask != null) {
            cname = mDownloadTask.getCnname();
        }
        return cname;
    }

    /**
     * ycw 添加是否显示进度条参数
     *
     * @param url
     *
     * @param visibility
     */
    public synchronized void continueHandler(String url,
                                             int visibility) {
        FileHttpResponseHandler handler;
        Logger.e("ydp", "add:" + url);
        for (int i = 0; i < mDownloadinghandlers.size(); i++) {
            handler = (FileHttpResponseHandler) mDownloadinghandlers.get(i);
            Logger.e("ydp", "url:" + i + handler.getUrl());
        }
        for (int i = 0; i < mDownloadinghandlers.size(); i++) {
            String cnname = getCName(url);
            handler = (FileHttpResponseHandler) mDownloadinghandlers.get(i);
            if (handler != null && handler.getUrl().equals(url)) {
                long taskId = handler.getTaskId();
                Logger.d(TAG, "准备改变正在下载的进度条状态" + ";taskId-->" + taskId
                        + ";url-->" + url);
                if (visibility == DownloadStrategy.VISIBILITY) {
                    mNotificationHelper.addNotification(handler.getTaskId() + mDbHashCode,
                            -1, StringUtils.getOriginalFileNameFromUrl(url),
                            cnname, url);
                    Logger.d(TAG, "添加正在下载的进度条状态为显示" + ";taskId-->" + taskId
                            + ";url-->" + url);
                } else {
                    mNotificationHelper.cancelNotification(taskId + mDbHashCode);
                    Logger.d(TAG, "将正在下载的进度条状态为隐藏" + ";taskId-->" + taskId
                            + ";url-->" + url);
                }
                // 更新数据库
                DownloadTask downloadTask = mDbutil.fetchOneDownload(taskId);
                String downloadStrategyStr = downloadTask.getDownloadStrategy();
                try {
                    if (!TextUtils.isEmpty(downloadStrategyStr)) {
                        JSONObject  downLoadStrateObj = new JSONObject(
                                downloadStrategyStr);
                        downLoadStrateObj.put(DownloadStrategy.VISIBILITY_KEY,
                                visibility);
                        Logger.d(TAG, "dbDownloadStrategy--->"
                                + downLoadStrateObj);
                        mDbutil.updateDownloadStrategyById(taskId,
                                downLoadStrateObj.toString());

                        Logger.d(TAG, "改变正在下载的进度条数据库状态" + ";taskId-->" + taskId
                                + ";url-->" + url + ";visibility--->"
                                + visibility);
                    }
                } catch (JSONException e) {
                    // TODO Auto-generated catch block
                    Logger.e(TAG, e.toString());
                }
            }
        }

        for (int i = 0; i < mPausinghandlers.size(); i++) {
            handler = (FileHttpResponseHandler) mPausinghandlers.get(i);
            Logger.d(TAG,
                    "准备改变缓存队列里的进度条状态" + ";taskId-->" + handler.getTaskId()
                            + ";url-->" + handler.getUrl());
            if (handler != null && handler.getUrl().equals(url)) {
                String cnname = getCName(url);
                if (visibility == DownloadStrategy.VISIBILITY) {
                    mNotificationHelper.addNotification(handler.getTaskId() + mDbHashCode,
                            -1, StringUtils.getOriginalFileNameFromUrl(url),
                            cnname, url);
                    Logger.d(TAG, "已经将缓存队列里的进度条状态改为addNotification"
                            + ";taskId-->" + handler.getTaskId() + ";url-->"
                            + handler.getUrl());
                } else {
                    cancelNotifactionByFileResponseHandler(handler);
                    Logger.d(TAG, "隐藏缓存下载的进度条状态为隐藏cancelNotification"
                            + ";taskId-->" + handler.getTaskId() + ";url-->"
                            + url);
                }
                long taskId = handler.getTaskId();
                DownloadTask downloadTask = mDbutil.fetchOneDownload(taskId);
                String downloadStrategyStr = downloadTask.getDownloadStrategy();
                try {
                    if (!TextUtils.isEmpty(downloadStrategyStr)) {
                        JSONObject downLoadStrateObj = new JSONObject(
                                downloadStrategyStr);
                        downLoadStrateObj.put(DownloadStrategy.VISIBILITY_KEY,
                                visibility);
                        Logger.d(TAG, "dbDownloadStrategy--->"
                                + downLoadStrateObj);
                        mDbutil.updateDownloadStrategyById(taskId,
                                downLoadStrateObj.toString());

                        Logger.d(TAG, "改变缓存的进度条数据库状态" + ";taskId-->" + taskId
                                + ";url-->" + url + ";visibility--->"
                                + visibility);
                    }
                } catch (JSONException e) {
                    // TODO Auto-generated catch block
                    Logger.e(TAG, e.toString());
                }

                continuehandler(handler);
                break;
            }
        }
    }

    public synchronized void continuehandler(
            AsyncHttpResponseHandler responseHandler) {

        if (responseHandler != null) {
            if (mPausinghandlers == null || mhandlerQueue == null) {
                Logger.d(TAG,
                        "mPausinghandlers == null || mhandlerQueue == null");
                return;

            }
            mPausinghandlers.remove(responseHandler);
            mhandlerQueue.offer(responseHandler);
            FileHttpResponseHandler fileHttpResponseHandler = (FileHttpResponseHandler) responseHandler;
            fileHttpResponseHandler.setmDownLoadCallback(mDownLoadCallback);
            DownloadTaskStatus status = getTaskRealtimeStatus(
                    fileHttpResponseHandler.getTaskId(),
                    fileHttpResponseHandler.getUrl());
            if (status != null) {
                status.setCurrentStatus(DownloadTaskStatus.DOWNLOADING);

            }
            syncDownloadStatusWithDB(fileHttpResponseHandler.getTaskId(),
                    DownloadTaskStatus.DOWNLOADING, null, null, null);

        }
    }

    public synchronized void continueAllHandler() {
        if (mPausinghandlers == null || mPausinghandlers.size() == 0) {
            return;
        }
        FileHttpResponseHandler handler;
        int count = mPausinghandlers.size();
        if (count == 0) {
            return;
        }
        for (int i = 0; i < count; i++) {
            handler = (FileHttpResponseHandler) mPausinghandlers.get(0);
            handler.setmDownLoadCallback(mDownLoadCallback);
            String cnname = getCName(handler.getUrl());
            String url = handler.getUrl();
            DownloadTask downloadTask = mDbutil.fetchOneDownload(handler
                    .getTaskId());
            if (downloadTask != null) {
                if (downloadTask.getVisibility() == DownloadStrategy.VISIBILITY) {
                    mNotificationHelper.addNotification(handler.getTaskId() + mDbHashCode,
                            -1, StringUtils.getOriginalFileNameFromUrl(url),
                            cnname, handler.getUrl());
                }
            }
            continuehandler(handler);

        }
    }

    public synchronized void completehandler(
            AsyncHttpResponseHandler mAsyncHttpResponseHandler) {
        long taskId = -1;
        String url = null;
        String apkSavePath = null;
        try {
            if (mDownloadinghandlers.contains(mAsyncHttpResponseHandler)) {
                // by robin
                // DownLoadConfigUtil.clearURL(mDownloadinghandlers.indexOf(handler));
                mDownloadinghandlers.remove(mAsyncHttpResponseHandler);
                if (mDownLoadCallback != null) {
                    FileHttpResponseHandler fhandler = (FileHttpResponseHandler) mAsyncHttpResponseHandler;
                    taskId = fhandler.getTaskId();
                    url = fhandler.getUrl();
                    removeRealtimeStatus(taskId, url);
                    // 说明下载完成
                    if (fhandler != null
                            && fhandler.getDownloadSize() == fhandler
                            .getTotalSize()
                            && fhandler.getDownloadSize() > 0
                            && fhandler.getTotalSize() > 0) {
                        apkSavePath = fhandler.getFile().getAbsolutePath();
                        //Object[] apkinfos = null;
                        // syncDownloadStatusWithDB(taskId,DownloadTaskStatus.COMPLETED,apkSavePath,apkinfos!=null?(String)apkinfos[0]:null,apkinfos!=null?(String)apkinfos[2]:null);
                        String packageName = mDbutil.fetchOneDownload(taskId)
                                .getPackageName();
                        String postFix = FileInfoUtils.getPostFix(apkSavePath);
                        if (SdCardUtil.isMounted()) {
                            if (postFix != null
                                    && postFix.equalsIgnoreCase("apk")) {
//                                apkinfos = ApkUtil.fetchApkFileInfo(mContext,
//                                        apkSavePath);
                                // 若传递了包名，者不更改
                                if (!TextUtils.isEmpty(packageName)) {
                                    syncDownloadStatusWithDB(taskId,
                                            DownloadTaskStatus.COMPLETED,
                                            apkSavePath, packageName, null);
                                } else {
                                    syncDownloadStatusWithDB(
                                            taskId,
                                            DownloadTaskStatus.COMPLETED,
                                            apkSavePath,
                                            null, null);
                                }
                            } else {
                                syncDownloadStatusWithDB(taskId,
                                        DownloadTaskStatus.COMPLETED,
                                        apkSavePath, null, null);
                            }
                            if (mDownLoadCallback != null) {
                                mDownLoadCallback
                                        .sendFinishMessage(taskId, url);
                                mDownLoadCallback.sendLoadMessage(taskId,url, fhandler.getTotalSize(), fhandler.getTotalSize(),0,100);
                            }
                            mNotificationHelper.updateNotification(taskId + mDbHashCode, 100,
                                    100);


                        }

                    }

                }
            }
        } catch (Exception exception) {
            Log.e("YCW", "e.toString--->" + exception.toString());
            if (taskId != -1 && url != null) {
                if (mDownLoadCallback != null) {
                    mNotificationHelper.updateNotification(taskId + mDbHashCode, 100, 100);
                    mDownLoadCallback.sendFailureMessage(taskId, url,
                            Erros.APK_UNINSTALLABLE);
                    mNotificationHelper
                            .updateNotificationTicker(taskId + mDbHashCode, "文件损坏");
                    DownloadTask mDownloadTask = mDbutil.findDownloadByUrl(url);
                    if (mDownloadTask != null) {
                        if (!TextUtils.isEmpty(apkSavePath)) {
                            // mDbutil.deleteDownload(taskId);

                            File apkFile = new File(apkSavePath);
                            if (apkFile.exists()) {
                                apkFile.delete();
                            }
                        }

                    }
                    mNotificationHelper.cancelNotification(taskId + mDbHashCode);

                }
            }

        }
    }

    private void testSavepath(long taskId, String tag) {
        String savaPaths = getDownloadedTaskInfo(taskId).getSavepath();
        Logger.d(TAG, tag + "():" + "savaPaths---->" + savaPaths);
    }

    private AsyncHttpResponseHandler newcHttpResponseHandler(
            DownloadStrategy temDownLoadStrategy, long taskId, String url,
            String cnname, boolean isFirstStart) throws MalformedURLException {
        String rootPathStrategy = null;
        if (temDownLoadStrategy != null) {
            if (!TextUtils.isEmpty(temDownLoadStrategy.rootpath)) {
                rootPathStrategy = temDownLoadStrategy.rootpath;
                Logger.d(TAG, "mRootPathStrategy---->" + rootPathStrategy);
            }

        }

        FileHttpResponseHandler handler = new FileHttpResponseHandler(taskId,
                url, rootPathStrategy == null ? mRootPath : rootPathStrategy,
                StringUtils.getOriginalFileNameFromUrl(url), cnname,
                isFirstStart, mContext, mDataBaseName, mIsSaveOldData) {
            private int times = 0;
            private int step = 1;
            private int stepCount = 0;
            private int stepMax = 100;
            private DownloadStrategy downloadStrategy = null;
            private int lastPercent = 0;

            @Override
            public void onProgress(long totalSize, long currentSize, long speed) {

                // super.onProgress(totalSize, currentSize, speed);
                int downloadPercent = Long.valueOf(
                        currentSize * 100 / totalSize).intValue();

                if ((times == 20) || (currentSize == totalSize)) {
                    mNotificationHelper.updateNotification(this.getTaskId() + mDbHashCode,
                            Long.valueOf(downloadPercent).intValue(), Long
                                    .valueOf(downloadPercent).intValue());

                    Logger.d(TAG, "notifaction--->" + this.getTaskId());
                    times = 0;
                }
                times++;

                if (mDownLoadCallback != null && downloadPercent > 0) {
                    if (downloadStrategy == null) {
                        downloadStrategy = mDownloadStrategyHashMap.get(this
                                .getTaskId());
                    }

                    if (downloadStrategy != null && downloadStrategy.step >= 1
                            && downloadStrategy.step < 100
                            && 100 % downloadStrategy.step == 0) {
                        step = downloadStrategy.step;
                        Logger.d(TAG, "step---->" + step);
                        stepMax = 100 / step;
                        if (stepCount < stepMax
                                && downloadPercent >= step * stepCount
                                && downloadPercent % step == 0
                                && lastPercent != downloadPercent) {
                            lastPercent = downloadPercent;
                            mDownLoadCallback.sendLoadMessage(this.getTaskId(),
                                    this.getUrl(), totalSize, currentSize,
                                    speed, downloadPercent);
                            Logger.d(TAG, "stepCount:" + stepCount
                                    + ";taskId--->" + this.getTaskId()
                                    + ";precent--->" + downloadPercent);
                            stepCount++;
                        }
                    }

                }

            }

            @Override
            public void onSuccess(String content) {
                if (mDownLoadCallback != null) {
                    mDownLoadCallback.sendSuccessMessage(this.getTaskId(),
                            this.getUrl());
                }
            }

            @Override
            public void onFinish() {
                completehandler(this);

            }

            @Override
            public void onStart() {
                // 注意这暂停重新开始也走这里
                // 从缓存查找,如果缓存
                if (mDownLoadCallback != null) {
                    mDownLoadCallback.sendStartMessage(this.getTaskId(),
                            this.getUrl());
                }

                DownloadTask downloadTask = getDownloadedTaskInfo(this
                        .getTaskId());
                if (downloadTask != null) {
                    DownloadStrategy downloadStrategy = DownloadStrategy
                            .parseFromJsonString(downloadTask
                                    .getDownloadStrategy());

                    String postFix = FileInfoUtils.getPostFix(this.getUrl());
                    if (downloadTask != null) {
                        if (downloadTask.getStatus() != DownloadTaskStatus.PAUSEING) {
                            if (downloadStrategy != null) {
                                if (downloadStrategy.getVisibility() == DownloadStrategy.VISIBILITY) {
                                    mNotificationHelper.addNotification(this
                                            .getTaskId() + mDbHashCode, -1, StringUtils
                                            .getOriginalFileNameFromUrl(this
                                                    .getUrl()), downloadTask
                                            .getCnname(), this.getUrl());
                                }
                            } else {
                                mNotificationHelper.addNotification(this
                                        .getTaskId() + mDbHashCode, -1, StringUtils
                                        .getOriginalFileNameFromUrl(this
                                                .getUrl()), downloadTask
                                        .getCnname(), this.getUrl());
                            }

                            syncDownloadStatusWithDB(this.getTaskId(),
                                    DownloadTaskStatus.DOWNLOADING, null, null,
                                    null);
                            testSavepath(downloadTask.getId(), "onStart");
                        } else {
                            continueHandler(this.getUrl(),
                                    downloadTask.getVisibility());
                        }

                    }

                }

                // by robin
                // DownLoadConfigUtil.storeURL(mDownloadinghandlers.indexOf(this),getTaskId(),getUrl());
            }

            @Override
            public void onFailure(Throwable error) {
                if (error != null) {
                    Message message = new Message();
                    message.arg1 = (int) this.getTaskId();
                    message.what = -1;
                    myHandler.sendMessage(message);
                }
                if (error != null) {
                    Logger.e(TAG, "Throwable:" + error.getMessage());
                    Logger.d(TAG, "[hot track]===onFailed==point 9==");
                    error.printStackTrace();
                    String erroMessage = error.getMessage();
                    if (TextUtils.isEmpty(erroMessage)) {
                        if (mDownLoadCallback != null) {
                            mDownLoadCallback.sendFailureMessage(
                                    this.getTaskId(), this.getUrl(),
                                    Erros.TIMEOUT);

                        }
                        return;
                    }
                    if (erroMessage.contains("ETIMEDOUT")) {
                        if (mDownLoadCallback != null) {
                            if (NetWorkUtil.isNetworkAvailable(mContext)) {
                                mDownLoadCallback.sendFailureMessage(
                                        this.getTaskId(), this.getUrl(),
                                        Erros.TIMEOUT);
                            }

                        }

                    } else if (erroMessage.contains("host")) {
                        if (mDownLoadCallback != null) {
                            mDownLoadCallback.sendFailureMessage(
                                    this.getTaskId(), this.getUrl(),
                                    Erros.URL_EXECEPTION);
                        }
                    } else if (erroMessage.contains("I/O")) {
                        if (!Environment.MEDIA_MOUNTED.equals(Environment
                                .getExternalStorageState())) {
                            // if (mDownLoadCallback != null) {
                            // mDownLoadCallback.onFailure(this.getTaskId(),
                            // this.getUrl(), Erros.NO_SD);
                            // }
                        }
                    }
                }
                mNotificationHelper.updateNotificationTicker(this.getTaskId() + mDbHashCode,
                        "下载失败");
                mNotificationHelper.cancelNotification(this.getTaskId() + mDbHashCode);
                pauseTask(this.getTaskId());
            }
        };

        return handler;
    }

    @SuppressWarnings("unused")
    private boolean isWaitingTask(String url) {

        for (int i = 0; i < mhandlerQueue.size(); i++) {

            if (((FileHttpResponseHandler) mhandlerQueue.get(i)).getUrl()
                    .equals(url)) {
                return true;
            }

        }

        return false;
    }

    @SuppressWarnings("unused")
    private boolean isPausingTask(String url) {
        for (int i = 0; i < mPausinghandlers.size(); i++) {
            if (((FileHttpResponseHandler) mPausinghandlers.get(i)).getUrl()
                    .equals(url))
                return true;
        }
        return false;
    }

    @SuppressWarnings("unused")
    private boolean isDownloadingTask(String url) {
        for (int i = 0; i < mDownloadinghandlers.size(); i++) {
            if (((FileHttpResponseHandler) mDownloadinghandlers.get(i))
                    .getUrl().equals(url))
                return true;
        }
        return false;
    }

    private class HandlerQueue {
        private Queue<AsyncHttpResponseHandler> handlerQueue;

        public HandlerQueue() {
            handlerQueue = new LinkedList<AsyncHttpResponseHandler>();
        }

        public void offer(AsyncHttpResponseHandler handler) {

            handlerQueue.offer(handler);
        }

        public AsyncHttpResponseHandler poll() {

            AsyncHttpResponseHandler handler = null;
            while (mDownloadinghandlers.size() >= DownLoadConfig.MAX_DOWNLOAD_THREAD_COUNT
                    || (handler = handlerQueue.poll()) == null) {
                try {
                    Thread.sleep(1000); // sleep
                } catch (InterruptedException e) {
                    Logger.e(TAG, e.toString());
                }
            }
            return handler;
        }

        public AsyncHttpResponseHandler get(int position) {

            if (position >= size()) {
                return null;
            }
            return ((LinkedList<AsyncHttpResponseHandler>) handlerQueue)
                    .get(position);
        }

        public int size() {

            return handlerQueue.size();
        }

        @SuppressWarnings("unused")
        public boolean remove(int position) {

            return handlerQueue.remove(get(position));
        }

        public boolean remove(AsyncHttpResponseHandler handler) {

            return handlerQueue.remove(handler);
        }
    }

    private void removeRealtimeStatus(long taskId, String url) {
        for (int i = 0; i < mDownloadTaskStatusData.size(); i++) {
            DownloadTaskStatus s = mDownloadTaskStatusData.get(i);
            if (s.getTaskId() == taskId || s.getUrl().equals(url)) {
                mDownloadTaskStatusData.remove(i);
                break;
            }
        }
    }

    private DownloadTaskStatus getTaskRealtimeStatus(long taskId, String url) {
        if (mDownloadTaskStatusData != null) {
            for (int i = 0; i < mDownloadTaskStatusData.size(); i++) {
                DownloadTaskStatus s = mDownloadTaskStatusData.get(i);
                if (s != null) {
                    if (s.getTaskId() == taskId || s.getUrl().equals(url))
                        return s;
                }

            }
        }

        return null;
    }

    private void syncDownloadStatusWithDB(long taskId, int status,
                                          String savePath, String packageName, String appName) {
        boolean savedbResult = mDbutil.updateDownload(taskId, null, appName,
                null, status, -1, savePath, packageName);
    }

    /*****
     * blow method for client to invoke
     *
     * @throws Exception
     *****/

    /*
     * post one download task with url and chinese name
     * @param url
     * @param cnname app name, mostly in chinese
     * @param packageName
     * @downloadStrategy download Strategy
     * @return DownloadTask
     */
    public synchronized DownloadTask postTask(String url, String cnname,
                                              String packageName, JSONObject downloadStrategyJsonObj)
            throws Exception {
        Logger.d(TAG, "postTask::" + url + "|" + cnname + "|" + packageName
                + "|" + downloadStrategyJsonObj);
        if (!url.startsWith("http") && !url.startsWith("https")) {
            throw new Exception();
        }
        url = url.trim();
        url = encodeUrl(url);
        Logger.d(TAG, "encodeurl--->" + url);
        int visibility = 0;
        String postFix = FileInfoUtils.getPostFix(url);
        DownloadTask downloadTask = new DownloadTask(url, cnname);
        DownloadTaskStatus istatus = getTaskRealtimeStatus(0, url);
        DownloadStrategy downloadStrategy = null;
        if (downloadStrategyJsonObj != null) {
            downloadStrategy = DownloadStrategy
                    .parseFromJsonObject(downloadStrategyJsonObj);
        }
        if (downloadStrategy != null) {
            visibility = downloadStrategy.getVisibility();
            downloadTask.setVisibility(visibility);
        } else {
            downloadTask.setVisibility(visibility);
        }
        if (istatus != null) {
            // 内存中有这个状态
            Logger.d(TAG,
                    "内存中有----》taskId:" + downloadTask.getId() + ";url---->"
                            + url + ";+statues" + istatus.getCurrentStatus());
            downloadTask.setStatus(istatus.getCurrentStatus());
            // 继续任务并且修改内存和文件状态为Dowloading

            if (NetWorkUtil.isNetworkConnected(mContext)) {
                Logger.d(TAG, "有网络连接");
                if (downloadTask.getStatus() == DownloadTaskStatus.WAITING) {
                    DownloadTask downloadTasktemp = mDbutil
                            .findDownloadByUrl(url);

                    Logger.d(TAG, "正在添加进度条---》url" + url + ";taskId"
                            + downloadTasktemp.getId() + "visibility-->"
                            + downloadTasktemp.getDownloadStrategy());
                    downloadTasktemp.setVisibility(visibility);

                    if (visibility == DownloadStrategy.VISIBILITY) {
                        addNotifactions(url, downloadTasktemp);
                        // 修改数据库

                        mDbutil.updateDownloadStrategyById(
                                downloadTasktemp.getId(),
                                downloadStrategyJsonObj != null ? downloadStrategyJsonObj
                                        .toString() : null);

                    } else {
                        mNotificationHelper.cancelNotification(downloadTasktemp
                                .getId() + mDbHashCode);
                        mDbutil.updateDownloadStrategyById(
                                downloadTasktemp.getId(),
                                downloadStrategyJsonObj != null ? downloadStrategyJsonObj
                                        .toString() : null);
                    }
                    if (downloadTasktemp != null) {
                        Logger.d(TAG, "数据库里面存在---》url" + url + ";taskId"
                                + downloadTasktemp.getId());
                        mDownloadManager
                                .addHandler(
                                        downloadTasktemp.getId(),
                                        url,
                                        downloadStrategy != null ? DownloadStrategy
                                                .parseFromJsonObject(downloadStrategyJsonObj)
                                                : null, downloadTask
                                                .getCnname(), false);
                        Logger.d(TAG, "addHandler()---》url" + url + ";taskId"
                                + downloadTasktemp.getId());
                    } else {
                        Logger.d(TAG, "数据库里不存在---》url" + url + ";taskId"
                                + downloadTasktemp.getId());
                    }
                } else {
                    continueHandler(url, visibility);
                }
            }
        } else {
            Logger.d(TAG, "内存无----》taskId:" + downloadTask.getId()
                    + ";url---->" + url);
            // 从数据库里面查看文件状态，数据库有记录
            DownloadTask downloadTasktemp = mDbutil.findDownloadByUrl(url);
            if (downloadTasktemp != null) {
                downloadTask = downloadTasktemp;
                downloadTask.setPostFix(postFix);
                downloadTask.setVisibility(visibility);
                if (downloadTask != null) {
                    if (downloadTask.getStatus() == DownloadTaskStatus.COMPLETED) {
                        if ("apk".equalsIgnoreCase(postFix)) {
                            boolean mApkIsAvaliable = checkApk(downloadTask);
                            String savePath = downloadTask.getSavepath();
                            if (!TextUtils.isEmpty(savePath) && new File(savePath).exists()
                                    && mApkIsAvaliable) {
                                downloadTask.setSavepath(downloadTask
                                        .getSavepath());
                            } else {// 数据库里有完成记录，但是临时文件不存在(或则已经损坏),重新下载
                                deleteTempFile(downloadTask);
                                // 删除数据库的记录
                                mDbutil.deleteDownload(downloadTask.getId());
                                mNotificationHelper
                                        .cancelNotification(downloadTask
                                                .getId() + mDbHashCode);
                                postTaskFirst(url, cnname, packageName,
                                        downloadStrategyJsonObj, downloadTask);

                            }
                        } else {
                            if (!new File(DownLoadConfig.FILE_ROOT
                                    + downloadTask.getName()).exists()) {
                                deleteTempFile(downloadTask);
                                if (downloadTask.getVisibility() == DownloadStrategy.VISIBILITY) {
                                    addNotifactions(url, downloadTask);
                                }
                                putnewTask(url, downloadStrategyJsonObj,
                                        downloadTask);

                            }
                        }

                    } else {// 数据库里面有记录,文件状态没有完成,临时文件存在，断点续传
                        downloadTask.setStatus(DownloadTaskStatus.UNCOMPLETED)
                                .setSavepath(
                                        DownLoadConfig.FILE_ROOT
                                                + downloadTask.getName());
                        if (downloadTask.getVisibility() == DownloadStrategy.VISIBILITY) {
                            addNotifactions(url, downloadTask);
                        }
                        putnewTask(url, downloadStrategyJsonObj, downloadTask);

                    }
                }
            } else {
                postTaskFirst(url, cnname, packageName,
                        downloadStrategyJsonObj, downloadTask);
            }

        }

        return downloadTask;
    }

    public synchronized boolean saveTempTask(String url, String cnname,
                                             String packageName, JSONObject downloadStrategyJsonObj) {
        boolean result = true;
        Logger.d(TAG, "saveTempTask::" + url + "|" + cnname + "|" + packageName
                + "|" + downloadStrategyJsonObj);
        try {
            url = url.trim();
            url = encodeUrl(url);
            Logger.d(TAG, "encodeurl--->" + url);
            if (mDbutil != null) {
                DownloadTask downloadTask = mDbutil.findDownloadByUrl(url);
                if (downloadTask == null) {
                    mDbutil.insertDownload(
                            StringUtils.getOriginalFileNameFromUrl(url),
                            cnname,
                            url,
                            packageName,
                            downloadStrategyJsonObj != null ? downloadStrategyJsonObj
                                    .toString() : null,
                            DownloadTaskStatus.TEMP_NODOWNLOAD);

                }
            } else {
                result = false;
            }

        } catch (Exception e) {
            result = true;
            Logger.e(TAG, e.toString());
        }
        return result;

    }

    public void startTempTask() {
        if (mDbutil != null) {
            List<DownloadTask> downloadTaskList = mDbutil
                    .findAllTempTaskByStatus();
            if (downloadTaskList != null) {
                int listSize = downloadTaskList.size();
                if (listSize > 0) {
                    for (DownloadTask task : downloadTaskList) {
                        Logger.d(
                                TAG,
                                "startTempTask().getVisibility-->"
                                        + task.getVisibility());
                        if (task.getVisibility() == DownloadStrategy.VISIBILITY) {
                            mNotificationHelper.addNotification(task.getId() + mDbHashCode,
                                    -1, StringUtils
                                            .getOriginalFileNameFromUrl(task
                                                    .getUrl()), task
                                            .getCnname(), task.getUrl());
                        }
                        DownloadStrategy downloadStrategy = DownloadStrategy
                                .parseFromJsonString(task.getDownloadStrategy());

                        addHandler(task.getId(), task.getUrl(),
                                downloadStrategy != null ? downloadStrategy
                                        : null, task.getCnname(), false);
                    }
                }
            }
        }

    }

    private void addNotifactions(String url, DownloadTask downloadTask) {
        mNotificationHelper.addNotification(downloadTask.getId() + mDbHashCode, -1,
                StringUtils.getOriginalFileNameFromUrl(url),
                downloadTask.getCnname(), url);
    }

    private String encodeUrl(String url) {
        url = Uri.decode(url);
        String encodeUrl = Uri.encode(url, "UTF-8").replace("%3A", ":")
                .replace("%2F", "/");
        return encodeUrl;
    }

    /**
     * ycw 删除临时文件
     *
     * @param downloadTask
     */
    private void deleteTempFile(DownloadTask downloadTask) {
        File mTempFile = new File(DownLoadConfig.FILE_ROOT
                + downloadTask.getName());
        if (mTempFile.exists()) {
            mTempFile.delete();
        }
    }

    /**
     * ycw
     *
     * @param downloadTask
     * @return
     */
    private boolean checkApk(DownloadTask downloadTask) {
        boolean isAvaliable = true;
        String savePath = downloadTask.getSavepath();
        if (downloadTask != null) {
            try {
                if (new File(savePath).exists()) {
                    ApkUtil.fetchApkFileInfo(mContext,
                            downloadTask.getSavepath());
                } else {
                    isAvaliable = false;
                }

            } catch (Exception e) {
                isAvaliable = false;
                Logger.e(TAG, e.toString());
            }
        }
        return isAvaliable;
    }

    /**
     * ycw
     *
     * @param url
     * @param cnname
     * @param packageName
     * @param downloadStrategy
     * @param downloadTask
     */
    private void postTaskFirst(String url, String cnname, String packageName,
                               JSONObject downloadStrategy, DownloadTask downloadTask) {
        long taskId = mDbutil.insertDownload(
                StringUtils.getOriginalFileNameFromUrl(url), cnname, url,
                packageName,
                downloadStrategy != null ? downloadStrategy.toString() : null);
        Logger.d(TAG, "new taskid=" + taskId);
        if (downloadStrategy != null) {
            DownloadStrategy downloadStrategy2 = DownloadStrategy
                    .parseFromJsonObject(downloadStrategy);
            if (downloadStrategy2 != null) {
                if (downloadStrategy2.getVisibility() == DownloadStrategy.VISIBILITY) {
                    mNotificationHelper.addNotification(taskId + mDbHashCode, -1,
                            StringUtils.getOriginalFileNameFromUrl(url),
                            downloadTask.getCnname(), url);

                }
            }
        } else {
            mNotificationHelper.addNotification(taskId + mDbHashCode, -1,
                    StringUtils.getOriginalFileNameFromUrl(url),
                    downloadTask.getCnname(), url);

        }

        mDownloadManager.addHandler(
                taskId,
                url,
                downloadStrategy != null ? DownloadStrategy
                        .parseFromJsonObject(downloadStrategy) : null,
                downloadTask.getCnname(), true);
        downloadTask.setId(taskId).setStatus(DownloadTaskStatus.WAITING);
    }

    private void putnewTask(String url, JSONObject downloadStrategy,
                            DownloadTask downloadTask) {
        if (downloadTask != null) {
            long taskId = downloadTask.getId();
            syncDownloadStatusWithDB(taskId, DownloadTaskStatus.WAITING, null,
                    null, null);

            mDownloadManager.addHandler(
                    taskId,
                    url,
                    downloadStrategy != null ? DownloadStrategy
                            .parseFromJsonObject(downloadStrategy) : null,
                    downloadTask.getCnname(), false);
            downloadTask.setId(taskId).setStatus(DownloadTaskStatus.WAITING);
        }

    }

    public int getDownloadTaskStatus(long taskId) {
        DownloadTaskStatus istatus = getTaskRealtimeStatus(taskId, null);

        if (istatus != null)
            return istatus.getCurrentStatus();
        // if can't find in cache
        DownloadTask entity = mDbutil.fetchOneDownload(taskId);
        return entity != null ? entity.getStatus() : DownloadTaskStatus.UNKNOW;
    }

    /**
     * get one downloaded task info including base-infomation save path etc.
     *
     * @param taskId
     * @return DownloadTask
     */
    public DownloadTask getDownloadedTaskInfo(long taskId) {
        DownloadTask downloadTask = mDbutil.fetchOneDownload(taskId);
        // return (entity!=null &&
        // entity.getStatus()==DownloadTaskStatus.COMPLETED)?entity:null;
        if (downloadTask == null) {
            return null;
        }
        if (downloadTask.getStatus() == DownloadTaskStatus.COMPLETED) {
            String filePath = downloadTask.getSavepath();
            File file = new File(filePath);
            if (TextUtils.isEmpty(filePath) || !file.exists()) {
                downloadTask.setStatus(DownloadTaskStatus.UNCOMPLETED);
            }
        }

        return (downloadTask != null) ? downloadTask : null;
    }

    public DownloadTask getDownloadedTaskInfo(String url) {
        DownloadTask entity = mDbutil.findDownloadByUrl(url);
        if (entity != null) {
            for (int i = 0; i < mDownloadinghandlers.size(); i++) {
                FileHttpResponseHandler fh = (FileHttpResponseHandler) mDownloadinghandlers
                        .get(i);
                if (fh.getUrl().equals(url)) {
                    long totalSize = fh.getTotalSize();
                    if (totalSize != 0) {
                        int downloadPercent = Long.valueOf(
                                fh.getDownloadSize() * 100 / totalSize)
                                .intValue();
                        entity.setProgress(downloadPercent);

                    } else {
                        entity.setProgress(100);
                    }
                    break;
                }
            }
        }
        return entity;
    }

    public synchronized void pauseTask(long taskId) {
        FileHttpResponseHandler handler;
        for (int i = 0; i < mDownloadinghandlers.size(); i++) {
            handler = (FileHttpResponseHandler) mDownloadinghandlers.get(i);
            if (handler != null && handler.getTaskId() == taskId) {
                pausehandler(handler);
            }
        }
    }

    // remove notification specified by packageName
    public void removeNotification(String packageName) {
        ArrayList<DownloadTask> mDownloadtaskList = mDbutil
                .findDownloadByPackageName(packageName);
        if (mDownloadtaskList.size() > 0) {
            for (DownloadTask mTask : mDownloadtaskList) {
                mNotificationHelper.cancelNotification(mTask.getId() + mDbHashCode);
            }

        }
    }

    public void removeNotification(long taskId) {
        mNotificationHelper.cancelNotification(taskId + mDbHashCode);
    }

    public Context getContext() {
        return mContext;
    }

    public void setContext(Context context) {
        this.mContext = context;
    }

    public void setClickAble(boolean clickAble) {
        if (clickAble) {
            DownLoadConfig.isClickable = true;
        }

    }

    private void showToast(String text) {
        if (DownLoadConfig.isShow) {
            Toast.makeText(mContext, text, Toast.LENGTH_LONG).show();
        }
    }

    public void setShowToast(boolean isShow) {
        if (isShow) {
            DownLoadConfig.isShow = true;
        }
    }

    public void setNotifacationCancleAble(boolean isCancelAble) {
        if (isCancelAble) {
            DownLoadConfig.notifactionCancleAble = true;
        }

    }

    public void unRegisterAllBroadCastReceiver() {
        try {
            NetworkStateReceiver.unRegisterNetworkStateReceiver(mContext);
            if (mNotifactionClickReceiver != null && mContext != null) {
                mContext.unregisterReceiver(mNotifactionClickReceiver);
            }
            unRegisterSdReceiver();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            Logger.e(TAG, e.toString());
        }
    }

    /**
     * 要在getInstance 之后调用
     *
     * @param mContext type=0 恢复 有通知 条的任务 ，type =1 恢复 静默 任务 .type=2 恢复所有 未完成的任务
     */
    public void restartUnCompleteTask(Context mContext, int type) {
        if (mDownloadManager != null) {
            restartUnCompletedTask(type);
        }
    }

    public static String getmDataBaseName() {
        return mDataBaseName;
    }

    public static void setmDataBaseName(String mDataBaseName) {
        DownloadManager.mDataBaseName = mDataBaseName;
    }

    /**
     * 根据包名查找完成记录
     *
     * @param packageName
     * @return
     */
    public List<DownloadTask> getCompletedTaskByPackage(String packageName) {
        ArrayList<DownloadTask> tempDownloadtaskList = null;
        ArrayList<DownloadTask> downloadcompletetaskList = new ArrayList<DownloadTask>();
        if (mDbutil != null) {
            tempDownloadtaskList = mDbutil
                    .findDownloadByPackageName(packageName);
        }
        for (int i = 0; i < tempDownloadtaskList.size(); i++) {
            DownloadTask downloadTask = tempDownloadtaskList.get(i);
            String filePath = downloadTask.getSavepath();
            File file = new File(filePath);
            if (!TextUtils.isEmpty(filePath) && file.exists()) {
                downloadcompletetaskList.add(downloadTask);
            }
        }

        return downloadcompletetaskList;
    }

}
