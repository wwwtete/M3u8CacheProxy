package com.wangw.m3u8cahceproxy;

import java.io.File;

/**
 * Created by wangw on 2017/2/28.
 */

public class Config {

    private File mCacheRoot;
    private String mHost;
    private int mPort;
    private int mTimeOut = 5000;

    public Config(File cacheRoot, String host, int port,int timeOut) {
        mCacheRoot = cacheRoot;
        mHost = host;
        mPort = port;
        this.mTimeOut = timeOut;
    }

    public int getTimeOut() {
        return mTimeOut;
    }

    public int getPort() {
        return mPort;
    }

    public String getHost() {
        return mHost;
    }

    public File getCacheRoot() {
        return mCacheRoot;
    }
}
