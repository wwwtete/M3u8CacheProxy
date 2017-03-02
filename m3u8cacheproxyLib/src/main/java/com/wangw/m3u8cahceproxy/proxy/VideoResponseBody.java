package com.wangw.m3u8cahceproxy.proxy;

import com.wangw.m3u8cahceproxy.CacheProxyException;
import com.wangw.m3u8cahceproxy.CacheUtils;
import com.wangw.m3u8cahceproxy.L;
import com.wangw.m3u8cahceproxy.source.Cache;
import com.wangw.m3u8cahceproxy.source.FileCache;
import com.wangw.m3u8cahceproxy.source.HttpUrlSource;
import com.wangw.m3u8cahceproxy.source.Source;
import com.wangw.m3u8cahceproxy.source.SourceInfo;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicInteger;

import static com.wangw.m3u8cahceproxy.CacheProxyManager.KEY_SERVER;
import static com.wangw.m3u8cahceproxy.CacheUtils.DEFAULT_BUFFER_SIZE;

/**
 * Created by wangw on 2017/3/2.
 */

public class VideoResponseBody {

    private static final int MAX_READ_SOURCE_ATTEMPTS = 1;

    private HttpRequest mRequest;
    private final Source source;
    private final Cache cache;
    private final Object wc = new Object();
    private final Object stopLock = new Object();
    private final AtomicInteger readSourceErrorsCount;
    private volatile Thread sourceReaderThread;
    private volatile boolean stopped;
//    private volatile int percentsAvailable = -1;

    public VideoResponseBody(HttpRequest request, File cacheRoot) throws CacheProxyException {
        mRequest = request;
//        String fileName = mRequest.getUri();
//        fileName = fileName.substring(fileName.lastIndexOf("/")+1,fileName.length());
        String server = mRequest.getParm(KEY_SERVER);
//        String url = "http://devimages.apple.com/iphone/samples/bipbop/gear1/"+fileName;
        SourceInfo info = new SourceInfo(server,Integer.MIN_VALUE,mRequest.getMimeType());

        this.source = new HttpUrlSource(info);
        File file = new File(cacheRoot.getAbsolutePath(),mRequest.getUri());
        this.cache = new FileCache(file);
        this.readSourceErrorsCount = new AtomicInteger();
    }

    public int read(byte[] buffer, long offset, int length) throws CacheProxyException {
        CacheUtils.assertBuffer(buffer, offset, length);

        while (!cache.isCompleted() && cache.available() < (offset + length) && !stopped) {
            readSourceAsync();
            waitForSourceData();
            checkReadSourceErrorsCount();
        }
        int read = cache.read(buffer, offset, length);
//        if (cache.isCompleted() && percentsAvailable != 100) {
//            percentsAvailable = 100;
//            onCachePercentsAvailableChanged(100);
//        }
        return read;
    }

    private void checkReadSourceErrorsCount() throws CacheProxyException {
        int errorsCount = readSourceErrorsCount.get();
        if (errorsCount >= MAX_READ_SOURCE_ATTEMPTS) {
            readSourceErrorsCount.set(0);
            throw new CacheProxyException("Error reading source " + errorsCount + " times");
        }
    }

    public void shutdown() {
        synchronized (stopLock) {
            L.log("Shutdown proxy for " + source);
            try {
                stopped = true;
                if (sourceReaderThread != null) {
                    sourceReaderThread.interrupt();
                }
                cache.close();
            } catch (CacheProxyException e) {
                onError(e);
            }
        }
    }

    private synchronized void readSourceAsync() throws CacheProxyException {
        boolean readingInProgress = sourceReaderThread != null && sourceReaderThread.getState() != Thread.State.TERMINATED;
        if (!stopped && !cache.isCompleted() && !readingInProgress) {
            sourceReaderThread = new Thread(new SourceReaderRunnable(), "Source reader for " + source);
            sourceReaderThread.start();
        }
    }

    private void waitForSourceData() throws CacheProxyException {
        synchronized (wc) {
            try {
                wc.wait(1000);
            } catch (InterruptedException e) {
                throw new CacheProxyException("Waiting source data is interrupted!", e);
            }
        }
    }

    private void notifyNewCacheDataAvailable(long cacheAvailable, long sourceAvailable) {
//        onCacheAvailable(cacheAvailable, sourceAvailable);
        synchronized (wc) {
            wc.notifyAll();
        }
    }

//    protected void onCacheAvailable(long cacheAvailable, long sourceLength) {
//        boolean zeroLengthSource = sourceLength == 0;
//        int percents = zeroLengthSource ? 100 : (int) (cacheAvailable * 100 / sourceLength);
//        boolean percentsChanged = percents != percentsAvailable;
//        boolean sourceLengthKnown = sourceLength >= 0;
//        if (sourceLengthKnown && percentsChanged) {
//            onCachePercentsAvailableChanged(percents);
//        }
//        percentsAvailable = percents;
//    }

//    protected void onCachePercentsAvailableChanged(int percentsAvailable) {
//    }

    private void readSource() {
        int sourceAvailable = -1;
        int offset = 0;
        try {
            offset = cache.available();
            source.open(offset);
            sourceAvailable = source.length();
            byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
            int readBytes;
            while ((readBytes = source.read(buffer)) != -1) {
                synchronized (stopLock) {
                    if (isStopped()) {
                        return;
                    }
                    cache.append(buffer, readBytes);
                }
                offset += readBytes;
                notifyNewCacheDataAvailable(offset, sourceAvailable);
            }
            tryComplete();
//            onSourceRead();
        } catch (Throwable e) {
            readSourceErrorsCount.incrementAndGet();
            onError(e);
        } finally {
            closeSource();
            notifyNewCacheDataAvailable(offset, sourceAvailable);
        }
    }

//    private void onSourceRead() {
//        // guaranteed notify listeners after source read and cache completed
//        percentsAvailable = 100;
//        onCachePercentsAvailableChanged(percentsAvailable);
//    }

    private void tryComplete() throws CacheProxyException {
        synchronized (stopLock) {
            if (!isStopped() && cache.available() == source.length()) {
                cache.complete();
            }
        }
    }

    private boolean isStopped() {
        return Thread.currentThread().isInterrupted() || stopped;
    }

    private void closeSource() {
        try {
            source.close();
        } catch (CacheProxyException e) {
            onError(new CacheProxyException("Error closing source " + source, e));
        }
    }

    protected final void onError(final Throwable e) {
        L.e(e.getMessage());
        e.printStackTrace();
    }

    public void send(OutputStream out,int offset) throws IOException, CacheProxyException {
        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
        int readBytes;
        while ((readBytes = read(buffer, offset, buffer.length)) != -1) {
            out.write(buffer, 0, readBytes);
            offset += readBytes;
        }
    }

    private class SourceReaderRunnable implements Runnable {

        @Override
        public void run() {
            readSource();
        }
    }


}
