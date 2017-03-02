package com.wangw.m3u8cahceproxy;

/**
 * Created by wangw on 2017/2/28.
 */

public interface CacheProxyCallback {

    void onStartPlay(String name,String url);

    void onError(String name,CacheProxyException e);
}
