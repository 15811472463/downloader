
package com.downloader.sdk.dlplugin.util.http;

import android.app.NotificationManager;
import android.content.Context;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

import com.downloader.sdk.dlplugin.DownLoadCallback;
import com.downloader.sdk.dlplugin.DownloadTask;
import com.downloader.sdk.dlplugin.Erros;
import com.downloader.sdk.dlplugin.exception.FileAlreadyExistException;
import com.downloader.sdk.dlplugin.util.common.SdCardUtil;
import com.downloader.sdk.dlplugin.util.db.DBUtil;
import com.downloader.sdk.dlplugin.util.log.Logger;

import org.apache.http.HttpResponse;
import org.apache.http.conn.ConnectTimeoutException;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.Timer;
import java.util.TimerTask;

public class FileHttpResponseHandler extends AsyncHttpResponseHandler {
    public final static int TIME_OUT = 30000;
    private final static int BUFFER_SIZE = 1024 * 8;

    private static final String TAG = "FileHttpResponseHandler";
    public static final String TEMP_SUFFIX = ".download";
    private File file;
    private File tempFile;
    private File baseDirFile;
    private RandomAccessFile outputStream;
    private long downloadSize;
    private long previousFileSize;
    private long totalSize;
    private long networkSpeed;
    private long previousTime;
    private long totalTime;
    private boolean interrupt = false;
    private boolean timerInterrupt = false;
    private long taskId;
    private String cnname;
    private String url;
    private boolean start = false;
    private Timer timer = new Timer();
    private static final int TIMERSLEEPTIME = 100;
    private DownLoadCallback mDownLoadCallback;
    private String contextTypePostFix;
    private Context mContext;
    private DBUtil mDbUtil;
    private String mDataBaseName;
    private boolean mIsSaveOldData;

    public FileHttpResponseHandler(long taskId, String url, String rootPath,
            String fileName, String cnname, Context context, String dataBaseName,
            boolean isSaveOldData) {
        super();
        this.url = url;
        this.taskId = taskId;
        this.mDataBaseName = dataBaseName;
        this.mIsSaveOldData = isSaveOldData;
        mDbUtil = DBUtil.getInstance(mContext, mDataBaseName, isSaveOldData);
        DownloadTask task = mDbUtil.fetchOneDownload(taskId);
        if (task != null) {
            String savePath = task.getSavepath();
            if (!TextUtils.isEmpty(savePath)) {
                if (savePath.contains("adsagedlplugin") && isSaveOldData) {
                    if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                        rootPath = Environment
                                .getExternalStorageDirectory().getAbsolutePath() + "/"
                                + "adsagedlplugin/";
                    }
                }
            }
        }
        Logger.d(TAG, "rootFile--->" + rootPath);
        this.baseDirFile = new File(rootPath);
        this.file = new File(rootPath, fileName);
        this.tempFile = new File(rootPath, fileName + TEMP_SUFFIX);
        this.cnname = cnname;
        this.mContext = context;
        init();
    }

    public FileHttpResponseHandler(long taskId, String url, String rootPath,
            String fileName, String cnname, boolean isFirstStart,
            Context context, String dataBaseName,
            boolean isSaveOldData) {
        super();
        this.url = url;
        this.taskId = taskId;
        this.mDataBaseName = dataBaseName;
        this.mIsSaveOldData = isSaveOldData;
        mDbUtil = DBUtil.getInstance(mContext, mDataBaseName, isSaveOldData);
        mDbUtil = DBUtil.getInstance(mContext, mDataBaseName, isSaveOldData);
        DownloadTask task = mDbUtil.fetchOneDownload(taskId);
        if (task != null) {
            String savePath = task.getSavepath();
            if (!TextUtils.isEmpty(savePath)) {
                if (savePath.contains("adsagedlplugin") && isSaveOldData) {
                    if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
                        rootPath = Environment
                                .getExternalStorageDirectory().getAbsolutePath() + "/"
                                + "adsagedlplugin/";
                    }
                }
            }
        }
        Logger.d(TAG, "rootFile--->" + rootPath);
        this.baseDirFile = new File(rootPath);
        this.file = new File(rootPath, fileName);
        this.tempFile = new File(rootPath, fileName + TEMP_SUFFIX);
        this.cnname = cnname;
        this.isFirstStart = isFirstStart;
        this.mContext = context;
        init();
    }

    private void init() {
        // TODO Auto-generated method stub
        if (!this.baseDirFile.exists()) {
            this.baseDirFile.mkdirs();
        }
    }

    private void startTimer() {
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                // TODO Auto-generated method stub
                while (!timerInterrupt) {
                    if (downloadSize == 0 && previousFileSize > 0) {

                    } else {
                        if (start) {
                            onProgress(totalSize, getDownloadSize(), networkSpeed);
                        }

                    }

                    try {
                        Thread.sleep(TIMERSLEEPTIME);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        Logger.e(TAG, e.toString());
                    }
                }

            }
        }, 0, 1000);
    }

    private void stopTimer() {
        timerInterrupt = true;
    }

    public File getFile() {
        return file;
    }

    public String getUrl() {
        return url;
    }

    private class ProgressReportingRandomAccessFile extends RandomAccessFile {
        private int progress = 0;

        public ProgressReportingRandomAccessFile(File file, String mode)
                throws FileNotFoundException {
            super(file, mode);
        }

        @Override
        public void write(byte[] buffer, int offset, int count)
                throws IOException {

            super.write(buffer, offset, count);
            progress += count;
            totalTime = System.currentTimeMillis() - previousTime;
            downloadSize = progress + previousFileSize;
            if (totalTime > 0) {
                networkSpeed = (long) ((progress / totalTime) / 1.024);
            }

        }
    }

    public boolean isInterrupt() {

        return interrupt;
    }

    public void setInterrupt(boolean interrupt) {
        this.interrupt = interrupt;
    }

    public long getDownloadSize() {

        return downloadSize;
    }

    public long getTotalSize() {

        return totalSize;
    }

    public double getDownloadSpeed() {

        return this.networkSpeed;
    }

    public void setPreviousFileSize(long previousFileSize) {
        this.previousFileSize = previousFileSize;
    }

    public long getPreviousFileSize() {
        return this.previousFileSize;
    }

    public long getTotalTime() {

        return this.totalTime;
    }

    public void onSuccess(byte[] binaryData) {
        onSuccess(new String(binaryData));
    }

    public void onSuccess(int statusCode, byte[] binaryData) {
        onSuccess(binaryData);
    }

    public void onFailure(Throwable error, byte[] binaryData) {
        onFailure(error);
    }

    @Override
    protected void sendResponseMessage(HttpResponse response) {

        Throwable error = null;
        byte[] responseBody = null;
        long result = -1;
        int statusCode = 0;
        // previousTime = System.currentTimeMillis();
        try {
            statusCode = response.getStatusLine().getStatusCode();
            long contentLenght = response.getEntity().getContentLength();
            // -1的解决方式ContentLength 在手机访问的时候出现了问题，返回为-1
            if (contentLenght == -1) {
                contentLenght = response.getEntity().getContent().available();
            }
            totalSize = contentLenght + previousFileSize;

            Logger.v(TAG, "totalSize: " + totalSize);
            // sd 卡容量不足
            if (SdCardUtil.getSdRemained() != -1) {
                if (totalSize > SdCardUtil.getSdRemained()) {
                    if (mDownLoadCallback != null) {
                        mDownLoadCallback.onFailure(this.taskId, url,
                                Erros.SD_NOSPACE);
                        cacleNotifaction();
                    }
                    return;
                }
            } else {
                if (mDownLoadCallback != null) {
                    mDownLoadCallback.onFailure(this.taskId, url, Erros.NO_SD);
                    cacleNotifaction();
                }
                return;
            }

            if (file.exists() && totalSize == file.length()) {
                Logger.v(TAG, "Output file already exists. Skipping download.");
                // throw new FileAlreadyExistException(
                // "Output file already exists. Skipping download.");
                file.delete();
            } else if (tempFile.exists()) {
                // response.addHeader("Range", "bytes=" +
                // tempFile.length()+"-");
                Logger.v(TAG, "yahooo: "
                        + response.getEntity().getContentLength());
                previousFileSize = tempFile.length();

                Logger.v(TAG, "File is not complete, download now.");
                Logger.v(TAG, "File length:" + tempFile.length()
                        + " totalSize:" + totalSize);

            }
            outputStream = new ProgressReportingRandomAccessFile(tempFile, "rw");
            InputStream input = response.getEntity().getContent();
            startTimer();
            int bytesCopied = copy(input, outputStream);

            if ((previousFileSize + bytesCopied) != totalSize
                    && totalSize != -1 && !interrupt) {
                throw new IOException("Download incomplete: " + bytesCopied
                        + " != " + totalSize);
            }
            Logger.v(TAG, "Download completed successfully.");
            result = bytesCopied;
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            System.out.println("[hot track]===onFailed==point 0==");
            error = e;
            Logger.e(TAG, e.toString());
        } catch (IllegalStateException e) {
            // TODO Auto-generated catch block
            error = e;
            Logger.e(TAG, e.toString());
            // sendFailureMessage(e, responseBody);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            // sendFailureMessage(e, responseBody);
            error = e;
            Logger.e(TAG, e.toString());
        }
        // 停止打印
        stopTimer();
        // 保证timer被关闭
        try {
            Thread.sleep(TIMERSLEEPTIME);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            Logger.e(TAG, e.toString());
        }
        if (result == -1 || error != null) {
            if (error != null) {
                Log.v(TAG, "Download failed." + error.getMessage());
                if (error instanceof FileAlreadyExistException) {
                    onSuccess(statusCode, "下载成功！".getBytes());
                    Log.e(TAG, "[hot track]===onFailed  =="
                            + error.getMessage());
                } else {
                    Log.e(TAG, "[hot track]===onFailed  =="
                            + error.getMessage());
                    error.printStackTrace();
                    Log.e(TAG, "[hot track]===onFailed==point 1==");
                    onFailure(error, responseBody);
                }
            }
            return;
        }
        tempFile.renameTo(file);
        onSuccess(statusCode, "下载成功！".getBytes());
    }

    private void cacleNotifaction() {
        Logger.e(TAG, "移除 通知条 ！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！");
        NotificationManager notificationManager = (NotificationManager) mContext
                .getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel((int) (this.taskId + mDataBaseName.hashCode()));
        Logger.e(TAG, "移除 通知条 ！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！"
                + (int) (this.taskId + mDataBaseName.hashCode()));
    }

    public int copy(InputStream input, RandomAccessFile out) throws IOException {

        if (input == null || out == null) {
            return -1;
        }

        byte[] buffer = new byte[BUFFER_SIZE];

        BufferedInputStream in = new BufferedInputStream(input, BUFFER_SIZE);
        Logger.v(TAG, "length" + out.length());
        int count = 0, n = 0;
        long errorBlockTimePreviousTime = -1, expireTime = 0;
        try {
            out.seek(out.length());
            previousTime = System.currentTimeMillis();
            start = true;
            while (!interrupt) {
                n = in.read(buffer, 0, BUFFER_SIZE);
                if (n == -1) {
                    break;
                }
                out.write(buffer, 0, n);
                count += n;
                if (networkSpeed == 0) {
                    if (errorBlockTimePreviousTime > 0) {
                        expireTime = System.currentTimeMillis()
                                - errorBlockTimePreviousTime;
                        if (expireTime > TIME_OUT) {
                            throw new ConnectTimeoutException(
                                    "connection time out.");
                        }
                    } else {
                        errorBlockTimePreviousTime = System.currentTimeMillis();
                    }
                } else {
                    expireTime = 0;
                    errorBlockTimePreviousTime = -1;
                }
            }
        } finally {

            try {
                out.close();
                // 无法关闭 inputstram
                // input.close();
                // in.close();
            } catch (IOException e) {
                // TODO: handle exception
            }
        }
        return count;

    }

    public File getTempFile() {
        // TODO Auto-generated method stub
        return tempFile;
    }

    public long getTaskId() {
        return taskId;
    }

    public String getCnname() {
        return cnname;
    }

    public void setCnname(String cnname) {
        this.cnname = cnname;
    }

    public DownLoadCallback getmDownLoadCallback() {
        return mDownLoadCallback;
    }

    public void setmDownLoadCallback(DownLoadCallback mDownLoadCallback) {
        this.mDownLoadCallback = mDownLoadCallback;
    }

    public File getBaseDirFile() {
        return baseDirFile;
    }

    public void setBaseDirFile(File baseDirFile) {
        this.baseDirFile = baseDirFile;
    }

    public static String getTempSuffix() {
        return TEMP_SUFFIX;
    }

    public void setTempFile(File tempFile) {
        this.tempFile = tempFile;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public String getContextTypePostFix() {
        return contextTypePostFix;
    }

    public void setContextTypePostFix(String contextTypePostFix) {
        this.contextTypePostFix = contextTypePostFix;
    }

}
