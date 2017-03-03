package com.wangw.m3u8cahceproxy;

import android.content.Context;

import com.wangw.m3u8cahceproxy.proxy.SocketProcessorRun;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.wangw.m3u8cahceproxy.CacheUtils.encode;

/**
 * Created by wangw on 2017/3/2.
 */

public class PlayProxyServer {

    public static final String KEY_SERVER = "server";

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

    public String getProxyUrl(String tsFile, String server) {
        return String.format(Locale.US, "%s?%s=%s", tsFile,KEY_SERVER, encode(server));
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


    public static class Builder {
        private File mCacheRoot;
        private String mHost;
        private int mProt;

        public Builder(Context context) {
            mCacheRoot = context.getCacheDir();
            mHost = "127.0.0.1";
            mProt = 2341;
        }

        public Builder setCacheRoot(File cacheRoot) {
            mCacheRoot = cacheRoot;
            return this;
        }

        public Builder setProt(int prot) {
            mProt = prot;
            return this;
        }

        public PlayProxyServer build() throws CacheProxyException {
            return new PlayProxyServer(buildConfig());
        }

        public Config buildConfig() {
            return new Config(mCacheRoot,mHost,mProt);
        }


    }

}
