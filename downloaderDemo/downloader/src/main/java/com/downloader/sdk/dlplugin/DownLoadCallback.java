
package com.downloader.sdk.dlplugin;

import android.os.Handler;
import android.os.Message;

public class DownLoadCallback extends Handler {

    protected static final int START_MESSAGE = 0;
    protected static final int ADD_MESSAGE = 1;
    protected static final int PROGRESS_MESSAGE = 2;
    protected static final int SUCCESS_MESSAGE = 3;
    protected static final int FAILURE_MESSAGE = 4;
    protected static final int FINISH_MESSAGE = 5;
    protected static final int STOP_MESSAGE = 6;
    protected static final int START_MANGER_MESSAGE = 8;
    protected static final int ONCLICK_MESSAGE = 9;
    protected static final int THREADPOOL_NOIDLE_MESSAGE = 10;
    protected static final int START_DOWNLOADING = 11;

    public void onClickNotifation(long taskId, String url) {

    }

    public void onStartManger() {

    }

    public void onStart(long taskId, String url) {

    }

    public void onStartDownLoading(long task, String url) {

    }

    public void onAdd(long taskId, String url, Boolean isInterrupt) {

    }

    public void onLoading(long taskId, String url, long totalSize,
            long currentSize, long speed, int percent) {

    }

    public void onSuccess(long taskId, String url) {
    }

    public void onFailure(long taskId, String url, int erro) {

    }

    public void onFinish(long taskId, String url) {
    }

    public void onStop() {
    }

    public void threadPoolNoIdle() {

    }

    @Override
    public void handleMessage(Message msg) {

        super.handleMessage(msg);
        Object[] response;

        switch (msg.what) {

            case START_MANGER_MESSAGE:
                onStartManger();
                break;

            case START_MESSAGE:
                response = (Object[]) msg.obj;
                onStart((Long) response[0], (String) response[1]);
                break;
            case ADD_MESSAGE:
                response = (Object[]) msg.obj;
                onAdd((Long) response[0], (String) response[1],
                        (Boolean) response[2]);
                break;
            case PROGRESS_MESSAGE:
                response = (Object[]) msg.obj;
                onLoading((Long) response[0], (String) response[1],
                        (Long) response[2], (Long) response[3], (Long) response[4],
                        (Integer) response[5]);
                break;
            case SUCCESS_MESSAGE:
                response = (Object[]) msg.obj;
                onSuccess((Long) response[0], (String) response[1]);
                break;
            case FAILURE_MESSAGE:
                response = (Object[]) msg.obj;
                onFailure((Long) response[0], (String) response[1],
                        (Integer) response[2]);
                break;
            case FINISH_MESSAGE:
                response = (Object[]) msg.obj;
                // String postFix = (String) response[2];
                // callOnFinish(response, postFix);
                onFinish((Long) response[0], (String) response[1]);
                break;
            case STOP_MESSAGE:
                onStop();
                break;
            case ONCLICK_MESSAGE:
                response = (Object[]) msg.obj;
                onClickNotifation((Long) response[0], response[1].toString());
                break;
            case THREADPOOL_NOIDLE_MESSAGE:
                threadPoolNoIdle();
                break;
            case START_DOWNLOADING:
                response = (Object[]) msg.obj;
                onStartDownLoading((Long) response[0], response[1].toString());
                break;
        }
    }

    protected void sendSuccessMessage(long taskId, String url) {
        sendMessage(obtainMessage(SUCCESS_MESSAGE, new Object[] {
                taskId, url
        }));
    }

    protected void sendLoadMessage(long taskId, String url, long totalSize,
            long currentSize, long speed, int percent) {
        sendMessage(obtainMessage(PROGRESS_MESSAGE, new Object[] {
                taskId, url,
                totalSize, currentSize, speed, percent
        }));
    }

    protected void sendAddMessage(long taskId, String url, Boolean isInterrupt) {
        sendMessage(obtainMessage(ADD_MESSAGE, new Object[] {
                taskId, url,
                isInterrupt
        }));
    }

    protected void sendFailureMessage(long taskId, String url, int erros) {
        sendMessage(obtainMessage(FAILURE_MESSAGE, new Object[] {
                taskId, url,
                erros
        }));
    }

    protected void sendStartMessage(long taskId, String url) {
        sendMessage(obtainMessage(START_MESSAGE, new Object[] {
                taskId, url
        }));
    }

    protected void sendStartMangerMessage() {
        sendMessage(obtainMessage(START_MANGER_MESSAGE, null));
    }

    protected void sendStopMessage() {
        sendMessage(obtainMessage(STOP_MESSAGE, null));
    }

    protected void sendFinishMessage(long taskId, String url) {
        sendMessage(obtainMessage(FINISH_MESSAGE, new Object[] {
                taskId, url
        }));
    }

    protected void sendClickNotifationMesage(long taskId, String url) {
        sendMessage(obtainMessage(ONCLICK_MESSAGE, new Object[] {
                taskId, url
        }));
    }

    protected void sendThreadPoolNoIdleMessage() {
        sendMessage(obtainMessage(THREADPOOL_NOIDLE_MESSAGE, null));
    }

    /**
     * ycw
     * 
     * @param taskId
     * @param url
     */
    protected void sendStartDownLoading(long taskId, String url) {
        sendMessage(obtainMessage(START_DOWNLOADING,
                new Object[] {
                        taskId, url
                }));
    }
}
