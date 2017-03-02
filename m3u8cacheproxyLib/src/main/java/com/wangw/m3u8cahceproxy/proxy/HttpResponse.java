package com.wangw.m3u8cahceproxy.proxy;

import com.wangw.m3u8cahceproxy.CacheProxyException;
import com.wangw.m3u8cahceproxy.L;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import static com.wangw.m3u8cahceproxy.CacheUtils.close;

/**
 * Created by wangw on 2017/3/2.
 */

public class HttpResponse {

    private final HttpRequest mRequest;
    private final File mCacheRoot;
    private IStatus status;
    private final String mimeType;
    private InputStream data;

    public HttpResponse(HttpRequest request, File cacheRoot) throws CacheProxyException {
        this.mRequest = request;
        this.mCacheRoot = cacheRoot;
        this.mimeType = mRequest.getMimeType();

        File file = new File(cacheRoot,mRequest.getUri());
        if (file.exists()){
            try {
                data = new FileInputStream(file);
                this.status = Status.OK;
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                throw new CacheProxyException("没有找到要请求的文件:"+file.getAbsolutePath(),e);
            }
        }else {
            status = Status.INTERNAL_ERROR;
            throw new CacheProxyException("没有找到要请求的文件:"+file.getAbsolutePath());
        }


    }

    public void send(OutputStream outputStream) throws CacheProxyException {
        SimpleDateFormat gmtFrmt = new SimpleDateFormat("E, d MMM yyyy HH:mm:ss 'GMT'", Locale.US);
        gmtFrmt.setTimeZone(TimeZone.getTimeZone("GMT"));

        try {
            if (this.status == null) {
                throw new Error("sendResponse(): Status can't be null.");
            }
            PrintWriter pw = new PrintWriter(new BufferedWriter(new OutputStreamWriter(outputStream, new ContentType(this.mimeType).getEncoding())), false);
            pw.append("HTTP/1.1 ").append(this.status.getDescription()).append(" \r\n");
            if (this.mimeType != null) {
                printHeader(pw, "Content-Type", this.mimeType);
            }
//            if (getHeader("date") == null) {
            printHeader(pw, "Date", gmtFrmt.format(new Date()));
//            }
//            for (Map.Entry<String, String> entry : this.header.entrySet()) {
//                printHeader(pw, entry.getKey(), entry.getValue());
//            }
//            if (getHeader("connection") == null) {
            printHeader(pw, "Connection", (mRequest.keepAlive() ? "keep-alive" : "close"));
//            }
//            if (getHeader("content-length") != null) {
//                encodeAsGzip = false;
//            }
//            if (encodeAsGzip) {
//                printHeader(pw, "Content-Encoding", "gzip");
//                setChunkedTransfer(true);
//            }
            long pending = -1;//this.data != null ? this.contentLength : 0;
            if (mRequest.requestMethod() != Method.HEAD ) {
                printHeader(pw, "Transfer-Encoding", "chunked");
            }
//            else if (!encodeAsGzip) {
//                pending = sendContentLengthHeaderIfNotAlreadyPresent(pw, pending);
//            }
            pw.append("\r\n");
            pw.flush();
            sendBodyWithCorrectTransferAndEncoding(outputStream, pending);
            outputStream.flush();
            close(this.data);
        } catch (IOException ioe) {
            ioe.printStackTrace();
            throw new CacheProxyException("Response异常",ioe);
        }
    }

    protected void printHeader(PrintWriter pw, String key, String value) {
        L.log("[printHeader] key="+key+" value="+value);
        pw.append(key).append(": ").append(value).append("\r\n");
    }

    private void sendBodyWithCorrectTransferAndEncoding(OutputStream outputStream, long pending) throws IOException {
//        if (this.requestMethod != Method.HEAD && this.chunkedTransfer) {
        ChunkedOutputStream chunkedOutputStream = new ChunkedOutputStream(outputStream);
        sendBody(chunkedOutputStream, -1);
        chunkedOutputStream.finish();
//        } else {
//            sendBodyWithCorrectEncoding(outputStream, pending);
//        }
    }

    private void sendBody(OutputStream outputStream, long pending) throws IOException {
        long BUFFER_SIZE = 16 * 1024;
        byte[] buff = new byte[(int) BUFFER_SIZE];
        boolean sendEverything = pending == -1;
        while (pending > 0 || sendEverything) {
            long bytesToRead = sendEverything ? BUFFER_SIZE : Math.min(pending, BUFFER_SIZE);
            int read = this.data.read(buff, 0, (int) bytesToRead);
            if (read <= 0) {
                break;
            }
            outputStream.write(buff, 0, read);
            if (!sendEverything) {
                pending -= read;
            }
        }
    }
}
