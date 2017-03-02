package fi.iki.elonen;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends Activity {
	
    private final String TAG = "MainActivity";
	
    Button play_url_m3u8;
	
    Button play_location_m3u8;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.activity_main);
        
        initUI();
    }
    
    void initUI(){

		play_url_m3u8 = (Button) findViewById(R.id.play_url_m3u8);
		play_location_m3u8 = (Button) findViewById(R.id.play_location_m3u8);

		play_url_m3u8.setOnClickListener(clickListener);
		play_location_m3u8.setOnClickListener(clickListener);
        findViewById(R.id.customplayer).setOnClickListener(clickListener);
   }
    
    OnClickListener clickListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			int id = v.getId();
			if (id == R.id.play_url_m3u8) {
				// TODO 确保此网络文件可用
				String path2 = "http://devimages.apple.com/iphone/samples/bipbop/gear1/prog_index.m3u8";
				Log.i(TAG, "path2 : "+path2);
				playVideo(path2, "播放网络M3U8");
			} else if (id == R.id.play_location_m3u8) {
				// TODO 确保本地存在此文件
				// TODO 代理服务器地址，加本地存储地址
                //fileSequence
//				String path3 = Environment.getExternalStorageDirectory().getAbsolutePath() + "/AAA/test/test.m3u8";
                String path3 = Environment.getExternalStorageDirectory().getAbsolutePath() + "/AAA/fileSequence/fileSequence.m3u8";
				Log.i(TAG, "path3 : "+path3);
				playVideo(path3, "播放本地M3U8");
			}else if (id == R.id.customplayer){
                Intent intent = new Intent(MainActivity.this, CustomPlayerActivity.class);
                startActivity(intent);
            }
		}
	};

    private void playVideo(String source, String title) {
        if (source == null || source.equals("")) {
            Toast.makeText(this, "视频内容不存在！", Toast.LENGTH_LONG).show();
         	source = "http://devimages.apple.com/iphone/samples/bipbop/gear1/prog_index.m3u8";
			Intent intent = new Intent(this, VideoActivity.class);
            intent.setData(Uri.parse(source));
            intent.putExtra("mVideoTitle", title);
            startActivity(intent);
        } else {
            Intent intent = new Intent(this, VideoActivity.class);
            intent.setData(Uri.parse(source));
            intent.putExtra("mVideoTitle", title);
            startActivity(intent);
        }
    }
}
