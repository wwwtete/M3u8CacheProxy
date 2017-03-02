package com.wangw.m3u8cahceproxy.source;

import android.text.TextUtils;

import com.wangw.m3u8cahceproxy.CacheProxyException;
import com.wangw.m3u8cahceproxy.CacheUtils;
import com.wangw.m3u8cahceproxy.L;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.HttpURLConnection;
import java.net.URL;

import static com.wangw.m3u8cahceproxy.CacheUtils.DEFAULT_BUFFER_SIZE;
import static java.net.HttpURLConnection.HTTP_MOVED_PERM;
import static java.net.HttpURLConnection.HTTP_MOVED_TEMP;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.net.HttpURLConnection.HTTP_PARTIAL;
import static java.net.HttpURLConnection.HTTP_SEE_OTHER;

/**
 *
 * @author Alexey Danilov (danikula@gmail.com).
 */
public class HttpUrlSource implements Source {


    private static final int MAX_REDIRECTS = 5;
//    private final SourceInfoStorage sourceInfoStorage;
    private SourceInfo sourceInfo;
    private HttpURLConnection connection;
    private InputStream inputStream;

    public HttpUrlSource(SourceInfo sourceInfo) {
        this.sourceInfo = sourceInfo;
    }

    @Override
    public synchronized int length() throws CacheProxyException {
        if (sourceInfo.length == Integer.MIN_VALUE) {
            fetchContentInfo();
        }
        return sourceInfo.length;
    }

    @Override
    public void open(int offset) throws CacheProxyException {
        try {
            connection = openConnection(offset, -1);
            String mime = connection.getContentType();
            inputStream = new BufferedInputStream(connection.getInputStream(), DEFAULT_BUFFER_SIZE);
            int length = readSourceAvailableBytes(connection, offset, connection.getResponseCode());
            this.sourceInfo = new SourceInfo(sourceInfo.url, length, mime);
//            this.sourceInfoStorage.put(sourceInfo.url, sourceInfo);
        } catch (IOException e) {
            throw new CacheProxyException("Error opening connection for " + sourceInfo.url + " with offset " + offset, e);
        }
    }

    private int readSourceAvailableBytes(HttpURLConnection connection, int offset, int responseCode) throws IOException {
        int contentLength = connection.getContentLength();
        return responseCode == HTTP_OK ? contentLength
                : responseCode == HTTP_PARTIAL ? contentLength + offset : sourceInfo.length;
    }

    @Override
    public void close() throws CacheProxyException {
        if (connection != null) {
            try {
                connection.disconnect();
            } catch (NullPointerException | IllegalArgumentException e) {
                String message = "Wait... but why? WTF!? " +
                        "Really shouldn't happen any more after fixing https://github.com/danikula/AndroidVideoCache/issues/43. " +
                        "If you read it on your device log, please, notify me danikula@gmail.com or create issue here https://github.com/danikula/AndroidVideoCache/issues.";
                throw new RuntimeException(message, e);
            }
        }
    }

    @Override
    public int read(byte[] buffer) throws CacheProxyException {
        if (inputStream == null) {
            throw new CacheProxyException("Error reading data from " + sourceInfo.url + ": connection is absent!");
        }
        try {
            return inputStream.read(buffer, 0, buffer.length);
        } catch (InterruptedIOException e) {
            throw new CacheProxyException("Reading source " + sourceInfo.url + " is interrupted", e);
        } catch (IOException e) {
            throw new CacheProxyException("Error reading data from " + sourceInfo.url, e);
        }
    }

    private void fetchContentInfo() throws CacheProxyException {
        L.log("Read content info from " + sourceInfo.url);
        HttpURLConnection urlConnection = null;
        InputStream inputStream = null;
        try {
            urlConnection = openConnection(0, 10000);
            int length = urlConnection.getContentLength();
            String mime = urlConnection.getContentType();
            inputStream = urlConnection.getInputStream();
            this.sourceInfo = new SourceInfo(sourceInfo.url, length, mime);
//            this.sourceInfoStorage.put(sourceInfo.url, sourceInfo);
            L.log("Source info fetched: " + sourceInfo);
        } catch (IOException e) {
            L.e("Error fetching info from " + sourceInfo.url, e);
        } finally {
            CacheUtils.close(inputStream);
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
    }

    private HttpURLConnection openConnection(int offset, int timeout) throws IOException, CacheProxyException {
        HttpURLConnection connection;
        boolean redirected;
        int redirectCount = 0;
        String url = this.sourceInfo.url;
        do {
            L.log("Open connection " + (offset > 0 ? " with offset " + offset : "") + " to " + url);
            connection = (HttpURLConnection) new URL(url).openConnection();
            if (offset > 0) {
                connection.setRequestProperty("Range", "bytes=" + offset + "-");
            }
            if (timeout > 0) {
                connection.setConnectTimeout(timeout);
                connection.setReadTimeout(timeout);
            }
            int code = connection.getResponseCode();
            redirected = code == HTTP_MOVED_PERM || code == HTTP_MOVED_TEMP || code == HTTP_SEE_OTHER;
            if (redirected) {
                url = connection.getHeaderField("Location");
                redirectCount++;
                connection.disconnect();
            }
            if (redirectCount > MAX_REDIRECTS) {
                throw new CacheProxyException("Too many redirects: " + redirectCount);
            }
        } while (redirected);
        return connection;
    }

    public synchronized String getMime() throws CacheProxyException {
        if (TextUtils.isEmpty(sourceInfo.mime)) {
            fetchContentInfo();
        }
        return sourceInfo.mime;
    }

    public String getUrl() {
        return sourceInfo.url;
    }

    @Override
    public String toString() {
        return "HttpUrlSource{sourceInfo='" + sourceInfo + "}";
    }
}
