package com.wangw.m3u8cahceproxy.source;

import com.wangw.m3u8cahceproxy.CacheProxyException;

/**
 * Source for proxy.
 *
 * @author Alexey Danilov (danikula@gmail.com).
 */
public interface Source {

    /**
     * Opens source. Source should be open before using {@link #read(byte[])}
     *
     * @param offset offset in bytes for source.
     * @throws CacheProxyException if error occur while opening source.
     */
    void open(int offset) throws CacheProxyException;

    /**
     * Returns length bytes or <b>negative value</b> if length is unknown.
     *
     * @return bytes length
     * @throws CacheProxyException if error occur while fetching source data.
     */
    int length() throws CacheProxyException;

    /**
     * Read data to byte buffer from source with current offset.
     *
     * @param buffer a buffer to be used for reading data.
     * @throws CacheProxyException if error occur while reading source.
     */
    int read(byte[] buffer) throws CacheProxyException;

    /**
     * Closes source and release resources. Every opened source should be closed.
     *
     * @throws CacheProxyException if error occur while closing source.
     */
    void close() throws CacheProxyException;
}
