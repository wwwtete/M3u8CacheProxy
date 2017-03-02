package com.wangw.samples;

import android.app.Application;
import android.os.Environment;

import com.wangw.m3u8cahceproxy.CacheProxyException;
import com.wangw.m3u8cahceproxy.CacheProxyManager;

import java.io.File;

/**
 * Created by wangw on 2017/2/28.
 */

public class App extends Application {

    public static App instance;

    private CacheProxyManager mCacheProxyManager;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
    }


    public CacheProxyManager getCacheProxy(){
        if (mCacheProxyManager == null) {
            try {
                File file = new File( Environment.getExternalStorageDirectory().getAbsolutePath(),"AAA");
                if (!file.exists()){
                    file.mkdir();
                }
                mCacheProxyManager = new CacheProxyManager.Build(this)
                        .setCacheRoot(file)
                        .build();
            } catch (CacheProxyException e) {
                e.printStackTrace();
            }
        }
        return mCacheProxyManager;
    }

}
