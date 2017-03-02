package com.wangw.m3u8cahceproxy.proxy;

import android.os.Build;

import com.wangw.m3u8cahceproxy.Config;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import static com.wangw.m3u8cahceproxy.CacheUtils.close;

/**
 * Created by wangw on 2017/3/2.
 */

public class SocketProcessorRun implements Runnable {

    private final Config mConfig;
    private final Socket mSocket;

    public SocketProcessorRun(Socket socket, Config config) {
        this.mConfig = config;
        this.mSocket = socket;
    }

    @Override
    public void run() {
        OutputStream outputStream = null;
        InputStream inputStream = null;
        try {
            outputStream = mSocket.getOutputStream();
            inputStream = mSocket.getInputStream();
            HttpRequest request = new HttpRequest(inputStream,mSocket.getInetAddress());
            while (!mSocket.isClosed()){
                request.parseRequest();
                HttpResponse response = new HttpResponse(request,mConfig.getCacheRoot());
                response.send(outputStream);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            close(outputStream);
            close(inputStream);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                close(mSocket);
            }
            stop();
        }

    }

    public void stop() {

    }
}
