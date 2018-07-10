
package com.downloader.sdk.dlplugin.exception;

/**
 * @Title DLException
 * @Description 所有异常的基类
 * @author
 * @date 2013-1-6
 * @version V1.0
 */
public class DLException extends Exception {
    private static final long serialVersionUID = 1L;

    public DLException() {
        super();
    }

    public DLException(String detailMessage) {
        super(detailMessage);
    }
}
