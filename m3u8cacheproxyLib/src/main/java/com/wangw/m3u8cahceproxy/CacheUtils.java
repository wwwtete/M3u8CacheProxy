package com.wangw.m3u8cahceproxy;

import android.text.TextUtils;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by wangw on 2017/2/28.
 */

public class CacheUtils {

    public static final int DEFAULT_BUFFER_SIZE = 8*1024;
    private static final Pattern URL_PATTERN = Pattern.compile("GET /(.*) HTTP");

    public static void close(Closeable closeable){
        if (closeable != null){
            try {
                closeable.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static <T> T checkNotNull(T reference,String errorMsg){
        if (reference == null)
            throw new NullPointerException(errorMsg);
        return reference;
    }

    public static String findUrlForStream(InputStream inputStream) throws IOException, CacheProxyException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream,"UTF-8"));
        StringBuilder builder = new StringBuilder();
        String line;
        while (!TextUtils.isEmpty(line = reader.readLine())){
            builder.append(line)
                    .append("\n");
        }
        Matcher matcher = URL_PATTERN.matcher(builder.toString());
        if (matcher.find()){
            return matcher.group(1);
        }
        throw new CacheProxyException("没有找到url");
    }


    public static String decodePercent(String str) {
        String decoded = null;
        try {
            decoded = URLDecoder.decode(str, "UTF8");
        } catch (UnsupportedEncodingException ignored) {
            L.log("Encoding not supported, ignored:"+ignored.getMessage());
        }
        return decoded;
    }

    public static void assertBuffer(byte[] buffer, long offset, int length) {
        checkNotNull(buffer, "Buffer must be not null!");
        checkArgument(offset >= 0, "Data offset must be positive!");
        checkArgument(length >= 0 && length <= buffer.length, "Length must be in range [0..buffer.length]");
    }

    static void checkArgument(boolean expression, String errorMessage) {
        if (!expression) {
            throw new IllegalArgumentException(errorMessage);
        }
    }
}
