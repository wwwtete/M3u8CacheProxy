package com.wangw.m3u8cahceproxy;

/**
 * Created by wangw on 2017/2/28.
 */

public class CacheProxyException extends Exception {

    public CacheProxyException(String message) {
        super(message);
    }

    public CacheProxyException(String message, Throwable cause) {
        super(message, cause);
    }

    public CacheProxyException(Throwable cause) {
        super(cause);
    }
}
