package com.wangw.m3u8cahceproxy.cache;

import com.wangw.m3u8cahceproxy.CacheProxyException;
import com.wangw.m3u8cahceproxy.CacheUtils;
import com.wangw.m3u8cahceproxy.Config;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * Created by wangw on 2017/2/28.
 */

public class TsListDownloadRun extends BaseDownload implements Runnable {

    private final Config mConfig;
    private final List<Extinfo> mInfos;
    private final String mName;
    private TsListDownLoadCallback mCallback;
    private M3u8Help mM3u8Help;

    private boolean mStoped;

    public TsListDownloadRun(Config config, List<Extinfo> infos,String name) {
        mConfig = CacheUtils.checkNotNull(config,"Config不能为空");
        mInfos = CacheUtils.checkNotNull(infos,"Ts文件列表不能为空");
        mName = CacheUtils.checkNotNull(name,"name不能为空");
    }

    public boolean isStoped(){
        return mStoped || Thread.currentThread().isInterrupted();
    }

    public void stop(){
        mStoped = true;
    }

    public TsListDownLoadCallback getCallback() {
        return mCallback;
    }

    public void setCallback(TsListDownLoadCallback callback) {
        mCallback = callback;
    }

    @Override
    public void run() {
        File cacheDir = getCacheDir();
        boolean needUpdate = false;
        try {
            needUpdate = initM3u8Help(cacheDir);
        } catch (IOException e) {
            onFailed(e);
            return;
        }
        String tsName;
        int size = mInfos.size();
        for (int i = 0; i <size; i++) {
            Extinfo info = mInfos.get(i);
            if (isStoped())
                break;
            if (i == 3){
                allowPlay();
            }
            tsName = mName+"_"+i+".ts"; //FileUtils.getFileNameForUrl(info.url);
            info.fileName = tsName;
            File tsFile = new File(cacheDir,tsName);
            try {
                if (!tsFile.exists()){
                    downloadFile(info.url, tsFile);
                    needUpdate = true;
                }
                if (needUpdate)
                    updateM3u8(info);
            } catch (Exception e) {
                onFailed(e);
                mM3u8Help.close();
                return;
            }
        }
        mM3u8Help.close();
        if (mCallback != null){
            mCallback.dwonloadFinish(mName);
        }
    }

    private void allowPlay() {
        if (mCallback != null) {
            String url = String.format(Locale.US, "http://%s:%d/%s/%s", mConfig.getHost(), mConfig.getPort(), mName,mM3u8Help.getFile().getName());
            mCallback.allowPlay(mName, url);//Uri.fromFile(mM3u8Help.getFile()).toString());
        }
    }

    /**
     * 初始化M3u8
     * @param cacheDir
     * @return
     * @throws IOException
     */
    private boolean initM3u8Help(File cacheDir) throws IOException {
        File m3u8File = new File(cacheDir.getAbsolutePath(),mName+".m3u8");
        boolean exists = m3u8File.exists();
        mM3u8Help = new M3u8Help(m3u8File,mName);
        return !exists;
    }

    private void updateM3u8(Extinfo info) throws IOException {
        mM3u8Help.insert(info);
    }

    private void onFailed(Exception e) {
        e.printStackTrace();
        if (mCallback != null){
            mCallback.downloadFailed(mName,new CacheProxyException(e));
        }
    }

    private File getCacheDir() {
        File dir = new File(mConfig.getCacheRoot(),mName);
        if (!dir.exists()){
            boolean flag = dir.mkdir();
            if (!flag)
                dir.mkdirs();
        }
        return dir;
    }
}
