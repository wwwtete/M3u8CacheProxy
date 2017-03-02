package com.wangw.m3u8cahceproxy.proxy;

import com.wangw.m3u8cahceproxy.CacheProxyException;
import com.wangw.m3u8cahceproxy.Config;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by wangw on 2017/3/2.
 */

public class PlayProxyServer {


    private final ServerSocket mServerSocket;
    private final ExecutorService mSocketPool = Executors.newFixedThreadPool(8);
    private final Config mConfig;

    public PlayProxyServer(Config config) throws CacheProxyException {
        mConfig = config;
        try {
            mServerSocket = new ServerSocket();
            mServerSocket.setReuseAddress(true);
            WaitRequestsRun run = new WaitRequestsRun();
            Thread thread = new Thread(run);
            thread.setName("WaitRequest Thread");
            thread.start();
            while (!run.mIsBinded && run.mBindException == null){
                try {
                    Thread.sleep(10L);
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
            if (run.mBindException != null)
                throw run.mBindException;
        } catch (Exception e) {
            e.printStackTrace();
            mSocketPool.shutdown();
            throw new CacheProxyException("初始化PlayProxyServer异常",e);
        }

    }

    private class WaitRequestsRun implements Runnable{

        private boolean mIsBinded = false;
        private Exception mBindException;


        public WaitRequestsRun() {
        }

        @Override
        public void run() {
            try {
                mServerSocket.bind(mConfig.getHost() != null ? new InetSocketAddress(mConfig.getHost(),mConfig.getPort()) : new InetSocketAddress(mConfig.getPort()));
                mIsBinded = true;
            } catch (IOException e) {
                e.printStackTrace();
                mBindException = e;
                return;
            }

            do{
                try {
                    Socket socket = mServerSocket.accept();
                    if (mConfig.getTimeOut() > 0)
                        socket.setSoTimeout(mConfig.getTimeOut());
                    mSocketPool.submit(new SocketProcessorRun(socket,mConfig));
                }catch (Exception e){
                    e.printStackTrace();
                }
            }while (!mServerSocket.isClosed());

        }
    }

    public void stop(){
        if (mServerSocket != null){
            try {
                mServerSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }finally {
                mSocketPool.shutdown();
            }
        }
    }


}
