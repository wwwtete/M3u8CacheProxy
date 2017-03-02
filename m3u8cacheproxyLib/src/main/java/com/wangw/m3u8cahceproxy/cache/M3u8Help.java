package com.wangw.m3u8cahceproxy.cache;

import com.wangw.m3u8cahceproxy.CacheUtils;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * Created by wangw on 2017/2/28.
 */

public class M3u8Help {


//    private final String mName;
    private File mFile;
    private RandomAccessFile mDataFile;

    public M3u8Help(File m3u8File) throws IOException {
        mFile = CacheUtils.checkNotNull(m3u8File,"m3u8 File对象 不能为空");
        onInit();
    }

    private void onInit() throws IOException {
        if (mFile.exists()){
            mDataFile = new RandomAccessFile(mFile,"rw");
        }else {
            mDataFile = new RandomAccessFile(mFile,"rw");
            mDataFile.seek(0);
            mDataFile.write(getHeader().getBytes());
        }
    }

    private String getHeader(){
        return "#EXTM3U\n#EXT-X-VERSION:3\n#EXT-X-MEDIA-SEQUENCE:0\n#EXT-X-TARGETDURATION:10\n";
    }

    public void insert(Extinfo extinfo) throws IOException {
//        mDataFile.seek(mDataFile.length());
//        mDataFile.write(("#EXTINF:"+extinfo.duration+",\n").getBytes());
//        mDataFile.seek(mDataFile.length());
////        String url = String.format(Locale.US, "http://%s:%d/%s/%s\n", mHost, mPort, mName,extinfo.fileName);
//        mDataFile.write((extinfo.fileName+"\n").getBytes());

//        StringBuilder builder = new StringBuilder();
//        builder.append("#EXTINF:")
//                .append(extinfo.duration)
//                .append(",\n")
//                .append(extinfo.fileName)
//                .append("\n");
        String extInfoStr = String.format("#EXTINF:%d,\n%s\n",extinfo.duration,extinfo.fileName);
        mDataFile.seek(mDataFile.length());
        mDataFile.write(extInfoStr.toString().getBytes());
    }

    public void endlist() throws IOException {
        mDataFile.seek(mDataFile.length());
        mDataFile.write("#EXT-X-ENDLIST".getBytes());
    }


    public void close(){
        CacheUtils.close(mDataFile);
    }

    public File getFile(){
        return mFile;
    }

}
