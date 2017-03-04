package com.wangw.samples;

import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.VideoView;

import com.shuyu.gsyvideoplayer.video.StandardGSYVideoPlayer;
import com.wangw.m3u8cahceproxy.FileUtils;
import com.wangw.m3u8cahceproxy.PlayProxyServer;
import com.wangw.m3u8cahceproxy.source.Extinfo;
import com.wangw.m3u8cahceproxy.source.M3u8Help;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class MainActivity extends AppCompatActivity {

    private StandardGSYVideoPlayer mPlayer;
    private VideoView mVideoView;
//    private CacheProxyManager mProxyManager;
    private PlayProxyServer mProxyServer;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mPlayer = (StandardGSYVideoPlayer) findViewById(R.id.video_player);
        mVideoView = (VideoView) findViewById(R.id.video_view);

//        M3u8Server.execute();
//        mPlayer.setUp("http://devimages.apple.com/iphone/samples/bipbop/gear1/prog_index.m3u8",false);
//        mProxyManager = App.instance.getCacheProxy();
//        mProxyManager.addCallback(this);
        mProxyServer = App.instance.getProxyServer();

    }

    public void onClick(View v){
        demo();
    }

    public void onFileSequence(View v){
        String path3 = Environment.getExternalStorageDirectory().getAbsolutePath() + "/AAA/fileSequence/fileSequence.m3u8";
//        mPlayer.setUp(mVideoSource,false);
//        mVideoView.setVideoPath(mVideoSource);
//        mPlayer.setUp("http://127.0.0.1:2341/fileSequence/fileSequence.m3u8",false);
//        mPlayer.setUp("http://192.168.1.17:8000/mmm.m3u8",false);
        mVideoView.setVideoPath("http://127.0.0.1:2341/fileSequence/fileSequence.m3u8");
//        mPlayer.startPlayLogic();
        mVideoView.start();
    }

    public void onTest(View v){
//        mPlayer.setUp("http://127.0.0.1:2341/test/test.m3u8",false);
//                mPlayer.setUp("http://live.hkstv.hk.lxdns.com/live/hks/playlist.m3u8",false);
//        mPlayer.setUp("http://devimages.apple.com/iphone/samples/bipbop/gear1/prog_index.m3u8",false);
//        mPlayer.startPlayLogic();
//        mPlayer.setUp("http://127.0.0.1:2341/test/test.m3u8",false);
//        File file = new File( Environment.getExternalStorageDirectory().getAbsolutePath(),"AAA");
//        mVideoView.setVideoPath("http://live.hkstv.hk.lxdns.com/live/hks/playlist.m3u8");
//        mVideoView.start();
        try {
            CountDownLatch latch = new CountDownLatch(1);
            initData("test5/test5.m3u8",latch);
            latch.await();
            onplay("http://127.0.0.1:2341/test5/test5.m3u8");
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void initData(final String patch, final CountDownLatch latch) throws IOException {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String filePatch = Environment.getExternalStorageDirectory().getAbsolutePath() + "/AAA/"+patch;
                    File file = new File(filePatch);
                    if (file.exists()){
                        file.delete();
                    }else {
                        File parent = file.getParentFile();
                        FileUtils.makeDir(parent);
                    }
                    String name = file.getName();
                    name = name.substring(0,name.lastIndexOf("."));
                    M3u8Help help = new M3u8Help(file);
                    int i;
                    for (i = 1; i < 6; i++) {
                        Extinfo extinfo = new Extinfo();
                        extinfo.duration = 10;
                        extinfo.url= "http://devimages.apple.com/iphone/samples/bipbop/gear1/fileSequence"+i+".ts";
                        extinfo.fileName = mProxyServer.getProxyUrl(name+"_"+i+".ts",extinfo.url);
                        help.insert(extinfo);
                    }
                    help.endlist();
                    latch.countDown();
//                    Thread.sleep(1000*22);
//                    while (i < 50){
//                        i++;
//                        Extinfo extinfo = new Extinfo();
//                        extinfo.duration = 10;
//                        extinfo.url= "http://devimages.apple.com/iphone/samples/bipbop/gear1/fileSequence"+i+".ts";
//                        extinfo.fileName = String.format(Locale.US, "%s?%s=%s", name+"_"+i+".ts",CacheProxyManager.KEY_SERVER, encode(extinfo.url));
//                        help.insert(extinfo);
//                        Thread.sleep(1000*20);
//                    }
                    help.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void onplay(String url){
        mPlayer.setUp(url,false);
        mPlayer.startPlayLogic();

        mVideoView.setVideoPath(url);
        mVideoView.start();
    }

    private void demo() {
        List<Extinfo> list = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Extinfo extinfo = new Extinfo();
            extinfo.duration = 10;
            extinfo.url="http://devimages.apple.com/iphone/samples/bipbop/gear1/fileSequence"+i+".ts";
            list.add(extinfo);
        }
//        mProxyManager.start(list,"test2");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mPlayer.release();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mPlayer.onVideoPause();
    }

//    @Override
//    public void onStartPlay(String name,String url) {
//        mPlayer.setUp(url,false);
//        mPlayer.startPlayLogic();
//
//        mVideoView.setVideoPath(url);
//        mVideoView.start();
//    }

//    @Override
//    public void onError(String name,CacheProxyException e) {
//        e.printStackTrace();
//    }
}
