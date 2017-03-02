package com.wangw.samples;

import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.VideoView;

import com.shuyu.gsyvideoplayer.video.StandardGSYVideoPlayer;
import com.wangw.m3u8cahceproxy.CacheProxyCallback;
import com.wangw.m3u8cahceproxy.CacheProxyException;
import com.wangw.m3u8cahceproxy.CacheProxyManager;
import com.wangw.m3u8cahceproxy.cache.Extinfo;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements CacheProxyCallback {

    private StandardGSYVideoPlayer mPlayer;
    private VideoView mVideoView;
    private CacheProxyManager mProxyManager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mPlayer = (StandardGSYVideoPlayer) findViewById(R.id.video_player);
        mVideoView = (VideoView) findViewById(R.id.video_view);

//        M3u8Server.execute();
//        mPlayer.setUp("http://devimages.apple.com/iphone/samples/bipbop/gear1/prog_index.m3u8",false);
        mProxyManager = App.instance.getCacheProxy();
        mProxyManager.addCallback(this);

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
        mPlayer.setUp("http://devimages.apple.com/iphone/samples/bipbop/gear1/prog_index.m3u8",false);
        mPlayer.startPlayLogic();
//        mPlayer.setUp("http://127.0.0.1:2341/test/test.m3u8",false);
//        File file = new File( Environment.getExternalStorageDirectory().getAbsolutePath(),"AAA");
//        mVideoView.setVideoPath("http://live.hkstv.hk.lxdns.com/live/hks/playlist.m3u8");
//        mVideoView.start();

    }

    private void demo() {
        List<Extinfo> list = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Extinfo extinfo = new Extinfo();
            extinfo.duration = 10;
            extinfo.url="http://devimages.apple.com/iphone/samples/bipbop/gear1/fileSequence"+i+".ts";
            list.add(extinfo);
        }
        mProxyManager.start(list,"test2");
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

    @Override
    public void onStartPlay(String name,String url) {
        mPlayer.setUp(url,false);
        mPlayer.startPlayLogic();

        mVideoView.setVideoPath(url);
        mVideoView.start();
    }

    @Override
    public void onError(String name,CacheProxyException e) {
        e.printStackTrace();
    }
}
