package com.wangw.m3u8cahceproxy.proxy;

import android.text.TextUtils;

import com.wangw.m3u8cahceproxy.CacheProxyException;
import com.wangw.m3u8cahceproxy.L;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.SSLException;

import static com.wangw.m3u8cahceproxy.CacheUtils.DEFAULT_BUFFER_SIZE;
import static com.wangw.m3u8cahceproxy.CacheUtils.close;
import static com.wangw.m3u8cahceproxy.CacheUtils.decodePercent;

/**
 * Created by wangw on 2017/3/2.
 */

public class HttpRequest {
    private static final Pattern RANGE_HEADER_PATTERN = Pattern.compile("[R,r]ange:[ ]?bytes=(\\d*)-");
    private final BufferedInputStream inputStream;
    private final String remoteIp;
    private final String remoteHostname;
    private final HashMap<String, String> headers;
    private HashMap<String, String> parms;
    private Method method;
    private String uri;
    private String protocolVersion;
    private boolean keepAlive;
    private String queryParameterString;
    private long rangeOffset;
    private boolean partial;

    public HttpRequest(InputStream inputStream, InetAddress inetAddress) {
        this.inputStream = new BufferedInputStream(inputStream);
        this.remoteIp = inetAddress.isLoopbackAddress() || inetAddress.isAnyLocalAddress() ? "127.0.0.1" : inetAddress.getHostAddress().toString();
        this.remoteHostname = inetAddress.isLoopbackAddress() || inetAddress.isAnyLocalAddress() ? "localhost" : inetAddress.getHostName().toString();
        this.headers = new HashMap<String, String>();
    }

    public void parseRequest() throws Exception {
        byte[] buf = new byte[DEFAULT_BUFFER_SIZE];
        int splitbyte = 0;
        int rlen = 0;

        int read = -1;
        this.inputStream.mark(DEFAULT_BUFFER_SIZE);
        try {
            read = this.inputStream.read(buf, 0, DEFAULT_BUFFER_SIZE);
        } catch (SSLException e) {
            throw e;
        } catch (IOException e) {
            close(this.inputStream);
            throw new SocketException("Socket Shutdown");
        }
        if (read == -1) {
            close(this.inputStream);
            throw new SocketException("Socket Shutdown");
        }
        while (read > 0) {
            rlen += read;
            splitbyte = findHeaderEnd(buf, rlen);
            if (splitbyte > 0) {
                break;
            }
            read = this.inputStream.read(buf, rlen, DEFAULT_BUFFER_SIZE - rlen);
        }

        if (splitbyte < rlen) {
            this.inputStream.reset();
            this.inputStream.skip(splitbyte);
        }

        this.parms = new HashMap<String, String>();
        this.headers.clear();

        // Create a BufferedReader for parsing the header.
        BufferedReader hin = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(buf, 0, rlen)));

        // Decode the header into parms and header java properties
        Map<String, String> pre = new HashMap<String, String>();
        decodeHeader(hin, pre, this.parms, this.headers);

        if (null != this.remoteIp) {
            this.headers.put("remote-addr", this.remoteIp);
            this.headers.put("http-client-ip", this.remoteIp);
        }

        this.method = Method.lookup(pre.get("method"));
        if (this.method == null) {
            throw new CacheProxyException("BAD REQUEST: Syntax error. HTTP verb " + pre.get("method") + " unhandled.");
        }

        this.uri = pre.get("uri");
        L.log("请求URI="+uri);
//        this.cookies = new CookieHandler(this.headers);

        String connection = this.headers.get("connection");
        keepAlive = "HTTP/1.1".equals(protocolVersion) && (connection == null || !connection.matches("(?i).*close.*"));
    }

    private int findHeaderEnd(final byte[] buf, int rlen) {
        int splitbyte = 0;
        while (splitbyte + 1 < rlen) {

            // RFC2616
            if (buf[splitbyte] == '\r' && buf[splitbyte + 1] == '\n' && splitbyte + 3 < rlen && buf[splitbyte + 2] == '\r' && buf[splitbyte + 3] == '\n') {
                return splitbyte + 4;
            }

            // tolerance
            if (buf[splitbyte] == '\n' && buf[splitbyte + 1] == '\n') {
                return splitbyte + 2;
            }
            splitbyte++;
        }
        return 0;
    }

    private int[] getBoundaryPositions(ByteBuffer b, byte[] boundary) {
        int[] res = new int[0];
        if (b.remaining() < boundary.length) {
            return res;
        }

        int search_window_pos = 0;
        byte[] search_window = new byte[4 * 1024 + boundary.length];

        int first_fill = (b.remaining() < search_window.length) ? b.remaining() : search_window.length;
        b.get(search_window, 0, first_fill);
        int new_bytes = first_fill - boundary.length;

        do {
            // Search the search_window
            for (int j = 0; j < new_bytes; j++) {
                for (int i = 0; i < boundary.length; i++) {
                    if (search_window[j + i] != boundary[i])
                        break;
                    if (i == boundary.length - 1) {
                        // Match found, add it to results
                        int[] new_res = new int[res.length + 1];
                        System.arraycopy(res, 0, new_res, 0, res.length);
                        new_res[res.length] = search_window_pos + j;
                        res = new_res;
                    }
                }
            }
            search_window_pos += new_bytes;

            // Copy the end of the buffer to the start
            System.arraycopy(search_window, search_window.length - boundary.length, search_window, 0, boundary.length);

            // Refill search_window
            new_bytes = search_window.length - boundary.length;
            new_bytes = (b.remaining() < new_bytes) ? b.remaining() : new_bytes;
            b.get(search_window, boundary.length, new_bytes);
        } while (new_bytes > 0);
        return res;
    }

    private void decodeHeader(BufferedReader in, Map<String, String> pre, Map<String, String> parms, Map<String, String> headers) throws CacheProxyException {
        try {
            String inLine = in.readLine();
            if (inLine == null) {
                return;
            }
            findRangeOffset(inLine);
            StringTokenizer st = new StringTokenizer(inLine);
            if (!st.hasMoreTokens()) {
                throw new CacheProxyException("错误的请求，语法错误，正确格式: GET /example/file.html");
            }

            pre.put("method", st.nextToken());

            if (!st.hasMoreTokens()) {
                throw new CacheProxyException("错误的请求，语法错误，正确格式: GET /example/file.html");
            }

            String uri = st.nextToken();

            int qmi = uri.indexOf('?');
            if (qmi >= 0) {
                decodeParms(uri.substring(qmi + 1), parms);
                uri = decodePercent(uri.substring(0, qmi));
            } else {
                uri = decodePercent(uri);
            }

            if (st.hasMoreTokens()) {
                protocolVersion = st.nextToken();
            } else {
                protocolVersion = "HTTP/1.1";
            }
            String line = in.readLine();
            while (line != null && !line.trim().isEmpty()) {
                int p = line.indexOf(':');
                if (p >= 0) {
                    headers.put(line.substring(0, p).trim().toLowerCase(Locale.US), line.substring(p + 1).trim());
                }
                line = in.readLine();
            }

            pre.put("uri", uri);
        } catch (IOException ioe) {
            throw new CacheProxyException( "解析Header异常: IOException: " + ioe.getMessage(), ioe);
        }
    }

    private void findRangeOffset(String request) {
        Matcher matcher = RANGE_HEADER_PATTERN.matcher(request);
        if (matcher.find()) {
            String rangeValue = matcher.group(1);
            rangeOffset = Long.parseLong(rangeValue);
        }
    }

    private void decodeParms(String parms, Map<String, String> p) {
        if (parms == null) {
            this.queryParameterString = "";
            return;
        }

        this.queryParameterString = parms;
        StringTokenizer st = new StringTokenizer(parms, "&");
        while (st.hasMoreTokens()) {
            String e = st.nextToken();
            int sep = e.indexOf('=');
            if (sep >= 0) {
                p.put(decodePercent(e.substring(0, sep)).trim(), decodePercent(e.substring(sep + 1)));
            } else {
                p.put(decodePercent(e).trim(), "");
            }
        }
    }

    public String getMimeType() {
        String patch = getUri();
        if (!TextUtils.isEmpty(patch) && patch.contains(".m3u8")){
            return "video/x-mpegURL";
        }else {
            return "video/mpeg";
        }
    }

    public String getUri(){
        return String.valueOf(uri);
    }

    public boolean keepAlive() {
        return keepAlive;
    }

    public Method requestMethod() {
        return method;
    }

    public String getQueryParameterString() {
        return queryParameterString;
    }

    public HashMap<String, String> getParms() {
        return parms;
    }

    public String getParm(String key){
        if (parms != null)
            return parms.get(key);
        return null;
    }
}
