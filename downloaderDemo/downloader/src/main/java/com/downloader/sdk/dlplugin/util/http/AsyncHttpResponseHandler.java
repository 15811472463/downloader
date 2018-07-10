
package com.downloader.sdk.dlplugin.util.http;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpResponseException;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.util.EntityUtils;

import java.io.IOException;

public class AsyncHttpResponseHandler {
    protected static final int SUCCESS_MESSAGE = 4;
    protected static final int FAILURE_MESSAGE = 1;
    protected static final int START_MESSAGE = 2;
    protected static final int FINISH_MESSAGE = 3;
    protected static final int PROGRESS_MESSAGE = 0;
    protected boolean isFirstStart = true;

    public void onStart() {
    }

    public void onFinish() {
    }

    public void onSuccess(String content) {

    }

    public void onProgress(long totalSize, long currentSize, long speed) {

    }

    public void onSuccess(int statusCode, Header[] headers, String content) {
        onSuccess(statusCode, content);
    }

    public void onSuccess(int statusCode, String content) {
        onSuccess(content);
    }

    public void onFailure(Throwable error) {
    }

    public void onFailure(Throwable error, String content) {

        onFailure(error);
    }

    protected void handleSuccessMessage(int statusCode, Header[] headers,
            String responseBody) {
        onSuccess(statusCode, headers, responseBody);
    }

    protected void handleFailureMessage(Throwable e, String responseBody) {
        onFailure(e, responseBody);
    }

    protected void handleProgressMessage(long totalSize, long currentSize,
            long speed) {
        onProgress(totalSize, currentSize, speed);
    }

    protected void sendResponseMessage(HttpResponse response) {
        StatusLine status = response.getStatusLine();
        String responseBody = null;
        try {
            HttpEntity entity = null;
            HttpEntity temp = response.getEntity();
            if (temp != null) {
                entity = new BufferedHttpEntity(temp);
                responseBody = EntityUtils.toString(entity, "UTF-8");
            }
        } catch (IOException e) {
            // sendFailureMessage(e, (String) null);
            onFailure(e);
        }

        if (status.getStatusCode() >= 300) {
            System.out.println("[hot track]===onFailed==point 5==");
            onFailure(new HttpResponseException(
                    status.getStatusCode(), status.getReasonPhrase()));
        } else {
            onSuccess(status.getStatusCode(), response.getAllHeaders(), responseBody);
        }
    }

    public boolean isFirstStart() {
        return isFirstStart;
    }

    public void setFirstStart(boolean isFirstStart) {
        this.isFirstStart = isFirstStart;
    }

}
