package com.wangw.m3u8cahceproxy.proxy;

import com.wangw.m3u8cahceproxy.Config;

import java.io.IOException;
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
            //TODO 暂时只处理一次请求，读完后关闭socket
//            while (!mSocket.isClosed()){
                request.parseRequest();
                HttpResponse response = new HttpResponse(request,mConfig.getCacheRoot());
                response.send(outputStream);
//            }
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            close(outputStream);
            close(inputStream);
            stop();
        }

    }

    public void stop() {
       try {
           if (!mSocket.isClosed())
               mSocket.close();
       } catch (IOException e) {
           e.printStackTrace();
       }

    }
}
