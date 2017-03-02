package com.wangw.m3u8cahceproxy.cache;

import com.wangw.m3u8cahceproxy.CacheProxyException;

/**
 * Created by wangw on 2017/2/28.
 */

public interface TsListDownLoadCallback {

    void allowPlay(String name,String uri);

    void dwonloadFinish(String name);

    void downloadFailed(String name,CacheProxyException e);
}
