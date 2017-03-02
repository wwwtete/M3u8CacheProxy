package com.wangw.m3u8cahceproxy;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Pair;

import com.wangw.m3u8cahceproxy.cache.Extinfo;
import com.wangw.m3u8cahceproxy.cache.TsListDownLoadCallback;
import com.wangw.m3u8cahceproxy.cache.TsListDownloadRun;
import com.wangw.m3u8cahceproxy.proxy.PlayProxyServer;

import java.io.File;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by wangw on 2017/2/28.
 */

public class CacheProxyManager implements TsListDownLoadCallback {

    private static final int STATE_FAILED = -1;
    private static final int STATE_ALLOWPLAY = 1;

   private final  Config mConfig;
    private final ExecutorService mRequestPool = Executors.newFixedThreadPool(5);
    private PlayProxyServer mServer;
    private List<CacheProxyCallback> mCallbacks = new CopyOnWriteArrayList<>();
    private Handler mHandler = new NotifyHolder();

    public CacheProxyManager(Context context) throws CacheProxyException {
        this(new Build(context).buildConfig());
    }

    public CacheProxyManager(Config config) throws CacheProxyException {
        mConfig = config;
        mServer = new PlayProxyServer(mConfig);
    }

    public void start(List<Extinfo> list, String name){
        TsListDownloadRun tsListDownloadRun = new TsListDownloadRun(mConfig, list, name);
        tsListDownloadRun.setCallback(this);
        mRequestPool.submit(tsListDownloadRun);
    }


    public void addCallback(CacheProxyCallback callback){
        mCallbacks.add(callback);
    }

    public void removeCallback(CacheProxyCallback callback){
        mCallbacks.remove(callback);
    }

    @Override
    public void allowPlay(String name,String uri) {
        mHandler.obtainMessage(STATE_ALLOWPLAY,new Pair<String,String>(name,uri))
        .sendToTarget();
    }

    @Override
    public void dwonloadFinish(String name) {

    }

    @Override
    public void downloadFailed(String name,CacheProxyException e) {
        mHandler.obtainMessage(STATE_FAILED,new Pair<String,CacheProxyException>(name,e))
        .sendToTarget();
    }



    class NotifyHolder extends Handler{

        public NotifyHolder() {
            super(Looper.getMainLooper());
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            onCallback(msg.what,msg.obj);
        }

        private void onCallback(int state,Object obj){
            for (CacheProxyCallback callback : mCallbacks) {
                switch (state){
                    case STATE_ALLOWPLAY:
                        Pair<String,String> pair = (Pair<String, String>) obj;
                        callback.onStartPlay(pair.first,pair.second);
                        break;
                    case STATE_FAILED:
                        Pair<String,CacheProxyException> pair2 = (Pair<String, CacheProxyException>) obj;
                        callback.onError(pair2.first,pair2.second);
                        break;
                }
            }
        }
    }

    public static class Build{
        private File mCacheRoot;
        private String mHost;
        private int mProt;

        public Build(Context context) {
            mCacheRoot = context.getCacheDir();
            mHost = "127.0.0.1";
            mProt = 2341;
        }

        public Build setCacheRoot(File cacheRoot) {
            mCacheRoot = cacheRoot;
            return this;
        }

        public Build setProt(int prot) {
            mProt = prot;
            return this;
        }

        public CacheProxyManager build() throws CacheProxyException {
            return new CacheProxyManager(buildConfig());
        }

       private Config buildConfig() {
           return new Config(mCacheRoot,mHost,mProt);
       }


   }


}
