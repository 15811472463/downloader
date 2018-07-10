
package com.downloader.sdk.dlplugin.util.http;

import com.downloader.sdk.dlplugin.util.log.Logger;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.http.protocol.HttpContext;

import java.io.File;
import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

public class AsyncHttpRequest implements Runnable {
    private final AbstractHttpClient client;
    private final HttpContext context;
    private final HttpUriRequest request;
    private final AsyncHttpResponseHandler responseHandler;
    private boolean isBinaryRequest;
    private int executionCount;
    private final String TAG = "AsyncHttpRequest";

    public AsyncHttpRequest(AbstractHttpClient client, HttpContext context,
            HttpUriRequest request, AsyncHttpResponseHandler responseHandler) {
        this.client = client;
        this.context = context;
        this.request = request;
        this.responseHandler = responseHandler;
        if (responseHandler instanceof FileHttpResponseHandler) {

            FileHttpResponseHandler fileHttpResponseHandler = (FileHttpResponseHandler) responseHandler;
            File tempFile = fileHttpResponseHandler.getTempFile();
            if (tempFile.exists()) {
                long previousFileSize = tempFile.length();
                fileHttpResponseHandler.setPreviousFileSize(previousFileSize);
                this.request.setHeader("RANGE", "bytes=" + previousFileSize
                        + "-");
            }

        }
    }

    @Override
    public void run() {
        try {
            // by robin
            if (responseHandler != null) {
                responseHandler.onStart();
            }
            makeRequestWithRetries();
            if (responseHandler != null) {
                Logger.d("DownloadManager", "robin trace 102=05====");
                responseHandler.onFinish();
            }

        } catch (IOException e) {
            if (responseHandler != null) {

                // by robin responseHandler.sendFinishMessage();
                if (this.isBinaryRequest) {
                    responseHandler.onFailure(e);
                } else {
                    responseHandler.onFailure(e);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Logger.d(TAG, e.getMessage());

        }
    }

    private void makeRequest() throws IOException {
        if (!Thread.currentThread().isInterrupted()) {
            try {
                final HttpResponse response = client.execute(request, context);
                if (!Thread.currentThread().isInterrupted()) {
                    if (responseHandler != null) {
                                responseHandler.sendResponseMessage(response);
                            }

                } else {
                    // TODO: should raise InterruptedException? this block is
                    // reached whenever the request is cancelled before its
                    // response is received
                }
            } catch (IOException e) {
                if (!Thread.currentThread().isInterrupted()) {
                    throw e;
                }
            }
        }
    }

    private void makeRequestWithRetries() throws ConnectException {
        // This is an additional layer of retry logic lifted from droid-fu
        // See:
        // https://github.com/kaeppler/droid-fu/blob/master/src/main/java/com/github/droidfu/http/BetterHttpRequestBase.java

        boolean retry = true;
        IOException cause = null;
        HttpRequestRetryHandler retryHandler = client
                .getHttpRequestRetryHandler();
        while (retry) {
            try {
                makeRequest();
                return;
            } catch (UnknownHostException e) {
                if (responseHandler != null) {
                    responseHandler.onFailure(e);
                }
                return;
            } catch (SocketException e) {
                // Added to detect host unreachable
                if (responseHandler != null) {
                    responseHandler.onFailure(e);
                }
                return;
            } catch (SocketTimeoutException e) {
                if (responseHandler != null) {
                    responseHandler.onFailure(e);
                }
                return;
            } catch (IOException e) {
                cause = e;
                retry = retryHandler.retryRequest(cause, ++executionCount,
                        context);
            } catch (NullPointerException e) {
                // there's a bug in HttpClient 4.0.x that on some occasions
                // causes
                // DefaultRequestExecutor to throw an NPE, see
                // http://code.google.com/p/android/issues/detail?id=5255
                cause = new IOException("NPE in HttpClient" + e.getMessage());
                retry = retryHandler.retryRequest(cause, ++executionCount,
                        context);
            }
        }
        // no retries left, crap out with exception
        ConnectException ex = new ConnectException();
        ex.initCause(cause);
        throw ex;
    }
}
