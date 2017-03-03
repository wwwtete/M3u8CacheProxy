package com.wangw.m3u8cahceproxy.source;

/**
 * Created by wangw on 2017/2/28.
 */

public class Extinfo {

    public String url;
    public int duration;
    public String fileName;

    public Extinfo() {
    }

    public Extinfo(String url, int duration) {
        this.url = url;
        this.duration = duration;
    }
}
