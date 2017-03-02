package com.wangw.m3u8cahceproxy.source;

import com.wangw.m3u8cahceproxy.CacheProxyException;

/**
 * Cache for proxy.
 *
 * @author Alexey Danilov (danikula@gmail.com).
 */
public interface Cache {

    int available() throws CacheProxyException;

    int read(byte[] buffer, long offset, int length) throws CacheProxyException;

    void append(byte[] data, int length) throws CacheProxyException;

    void close() throws CacheProxyException;

    void complete() throws CacheProxyException;

    boolean isCompleted();
}
