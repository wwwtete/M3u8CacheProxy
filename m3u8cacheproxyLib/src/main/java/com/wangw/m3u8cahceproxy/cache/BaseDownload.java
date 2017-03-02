package com.wangw.m3u8cahceproxy.cache;

import com.wangw.m3u8cahceproxy.CacheProxyException;
import com.wangw.m3u8cahceproxy.CacheUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import static java.net.HttpURLConnection.HTTP_MOVED_PERM;
import static java.net.HttpURLConnection.HTTP_SEE_OTHER;

/**
 * Created by wangw on 2017/2/28.
 */

public class BaseDownload {


    private static final int TIMEOUT = 1000*5;
    private static final int REDIRECTED_COUNT = 3;

    public void downloadFile(String url, File file) throws CacheProxyException {
        HttpURLConnection connection = null;
        InputStream inputStream = null;
        try {
            connection = openConnection(url);
            inputStream = connection.getInputStream();
            FileUtils.saveFile(inputStream, file);
        }catch (Exception e){
            e.printStackTrace();
            throw new CacheProxyException("保存文件失败:"+file.getAbsolutePath(),e);
        }finally {
            if (connection != null)
                connection.disconnect();
            CacheUtils.close(inputStream);
        }

    }

    private HttpURLConnection openConnection(String url) throws IOException, CacheProxyException {
        HttpURLConnection connection;
        boolean redirected;
        int redirectedCount =0;
        do {
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setConnectTimeout(TIMEOUT);
            connection.setReadTimeout(TIMEOUT);
            int code = connection.getResponseCode();
            redirected = code == HTTP_MOVED_PERM || code == HTTP_MOVED_PERM || code == HTTP_SEE_OTHER;
            if (redirected){
                redirectedCount++;
                connection.disconnect();
            }
            if (redirectedCount > REDIRECTED_COUNT){
                throw new CacheProxyException("连接网络失败，已超过最大重试次数");
            }
        }while (redirected);
        return connection;
    }




}
