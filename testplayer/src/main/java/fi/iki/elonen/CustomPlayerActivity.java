package fi.iki.elonen;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.widget.VideoView;

import com.shuyu.gsyvideoplayer.video.StandardGSYVideoPlayer;

/**
 * Created by wangw on 2017/3/1.
 */

public class CustomPlayerActivity extends Activity {

    private StandardGSYVideoPlayer mPlayer;
    private VideoView mVideoView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_custom);
        mPlayer = (StandardGSYVideoPlayer) findViewById(R.id.video_player);
        mVideoView = (VideoView) findViewById(R.id.videoview);

        String path3 = Environment.getExternalStorageDirectory().getAbsolutePath() + "/AAA/fileSequence/fileSequence.m3u8";
        String mVideoSource = String.format("http://127.0.0.1:%d%s", M3u8Server.PORT, path3);
        M3u8Server.execute();
        mPlayer.setUp(mVideoSource,false);
        mPlayer.startPlayLogic();

        mVideoView.setVideoPath(mVideoSource);
        mVideoView.start();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        M3u8Server.finish();
    }
}
