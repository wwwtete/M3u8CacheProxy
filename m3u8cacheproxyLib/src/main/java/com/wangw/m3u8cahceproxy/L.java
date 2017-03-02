package com.wangw.m3u8cahceproxy;

import android.util.Log;

/**
 * Created by wangw on 2017/3/1.
 */

public class L {

    private static final String TAG = "CacheProxy";

    public static void log(String log){
        Log.d(TAG,log);
    }

    public static void e(String log){
        Log.e(TAG,log);
    }

    public static void e(Exception e){
        e.printStackTrace();
        Log.e(TAG,e.getMessage());
    }

}
