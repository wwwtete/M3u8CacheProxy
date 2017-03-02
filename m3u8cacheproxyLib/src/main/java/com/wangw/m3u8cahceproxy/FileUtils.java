package com.wangw.m3u8cahceproxy;

import android.text.TextUtils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by wangw on 2017/2/28.
 */

public class FileUtils {

    /**
     * 文件是否存在
     * @param file
     * @return
     */
    public static boolean exists(File file){
        if (file != null){
            return file.exists();
        }
        return false;
    }

    /**
     * 从URL中获取文件名称
     * @param url
     * @return
     */
    public static String getFileNameForUrl(String url){
        if (TextUtils.isEmpty(url))
            return "";
        int index = url.lastIndexOf("/");
        if (index != -1){
            String name = url.substring(index+1,url.length());
            index = name.lastIndexOf("?");
            if (index != -1){
                name = name.substring(0,index);
            }
            return name;
        }
        return url;
    }

    public static void saveFile(InputStream inputStream,File outputFile) throws IOException {
        InputStream ips = new BufferedInputStream(inputStream);
//        int length = connection.getContentLength();
        FileOutputStream ops = new FileOutputStream(outputFile);
        byte[] buffer = new byte[CacheUtils.DEFAULT_BUFFER_SIZE];
        int length;
        while ((length = ips.read(buffer)) != -1){
            ops.write(buffer,0,length);
        }
        ops.flush();
        CacheUtils.close(ips);
        CacheUtils.close(ops);
    }

   public static void makeDir(File directory) throws IOException {
        if (directory.exists()) {
            if (!directory.isDirectory()) {
                throw new IOException("File " + directory + " is not directory!");
            }
        } else {
            boolean isCreated = directory.mkdirs();
            if (!isCreated) {
                throw new IOException(String.format("Directory %s can't be created", directory.getAbsolutePath()));
            }
        }
    }

}
