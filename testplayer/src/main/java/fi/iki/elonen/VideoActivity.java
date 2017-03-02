package fi.iki.elonen;

import android.app.Activity;
import android.content.Context;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.VideoView;

import java.text.SimpleDateFormat;
import java.util.Date;

public class VideoActivity extends Activity {
	
	/**-----------------------系统参数-----------------------**/
	private final String TAG = "VideoActivity";
    
    /**-----------------------播放参数-----------------------**/
	
	private String mVideoSource = null;
	
	private String mVideoTitle = null;
	
	/**-----------------------播放组件-----------------------**/

	private ImageButton mPlaybtn = null;
	
	private LinearLayout mController = null;
	private LinearLayout titleLayout = null;
	private LinearLayout playLayout = null;
	
	private SeekBar mProgress = null;
	private TextView mDuration = null;
	private TextView mCurrPostion = null;
	private TextView video_title = null;
	private TextView video_time = null;
	
	private VideoView playerView;
	
	/**-----------------------播放数据------------------------**/
	
	/**
	 * 控制面板显示时间，毫秒
	 */
	private int showTime = 5000;
	
	/**
	 * 记录播放位置
	 */
	private int mLastPos = 0;
    
    private boolean turnLeft = false;
    private boolean turnRight = false;
    private boolean isSeeking = false;
    
    /**
	 * 状态：0，初始状态，3，播放器初始化完成(prepared)，4，播放中，
	 * 5， 暂停中，8，播放完毕，9，播放错误
	 */
	int state = 0;
	
	private WakeLock mWakeLock = null;
	private static final String POWER_LOCK = "VideoActivity";
	
	private final int EVENT_PLAY = 0;
	private final int UI_EVENT_UPDATE_CURRPOSITION = 1;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
				
		setContentView(R.layout.play_video);
		
		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		mWakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE, POWER_LOCK);
		
		Uri uriPath = getIntent().getData();
		mVideoTitle = getIntent().getStringExtra("mVideoTitle");
		if (null != uriPath) {
			String scheme = uriPath.getScheme();
			if (null != scheme) {
				mVideoSource = uriPath.toString();
			} else {
				mVideoSource = uriPath.getPath();
			}
		}
		
		initUI();
		
		mUIHandler.postDelayed(hideControlBar, showTime);
	}
	
	/**
	 * 初始化界面
	 */
	private void initUI() {
		mPlaybtn = (ImageButton)findViewById(R.id.play_btn);
		mController = (LinearLayout)findViewById(R.id.controlbar);
		titleLayout = (LinearLayout)findViewById(R.id.title_layout);
		playLayout = (LinearLayout)findViewById(R.id.play_layout);
		
		mProgress = (SeekBar)findViewById(R.id.media_progress);
		mDuration = (TextView)findViewById(R.id.time_total);
		mCurrPostion = (TextView)findViewById(R.id.time_current);
		video_title = (TextView)findViewById(R.id.video_title);
		video_time = (TextView)findViewById(R.id.video_time);
		
		video_title.setText("" + mVideoTitle);
		video_time.setText("");
		mUIHandler.post(updateTime);
		
		registerCallbackForControl();
		
		if(!mVideoSource.contains("http://")){
			M3u8Server.execute();
			mVideoSource = String.format("http://127.0.0.1:%d%s", M3u8Server.PORT, mVideoSource);
		}
		
		/**
		 *获取SurfaceView对象
		 */
		playerView = (VideoView) findViewById(R.id.playerView);
		// 设置播放时打开屏幕
		playerView.getHolder().setKeepScreenOn(true);
		//处理屏幕的触摸事件
		playerView.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				updateControlBar(true);
				mUIHandler.removeCallbacks(hideControlBar);
				updateControlBar(true);
				mUIHandler.postDelayed(hideControlBar, showTime);
				return false;
			}
		});
		play();
	}
	
	public void play(){
		if(state == 0){
			getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
			initMediaPlayer();
			state = 4;
			if (mLastPos > 0) {
				playerView.seekTo(mLastPos);
				mLastPos = 0;
			}
			playerView.start();
			mPlaybtn.setImageResource(R.drawable.ic_stop_media);
			mUIHandler.sendEmptyMessage(UI_EVENT_UPDATE_CURRPOSITION);
		} else if(state == 4){
			state = 5;
			playerView.pause();
			mPlaybtn.setImageResource(R.drawable.ic_play_media);
			mUIHandler.removeMessages(UI_EVENT_UPDATE_CURRPOSITION);
		} else if(state == 5){
			state = 4;
			if (mLastPos > 0) {
				playerView.seekTo(mLastPos);
				mLastPos = 0;
			}
			playerView.start();
			mPlaybtn.setImageResource(R.drawable.ic_stop_media);
			mUIHandler.sendEmptyMessage(UI_EVENT_UPDATE_CURRPOSITION);
		} else if(state == 9){
			playerView.setVideoPath(mVideoSource);
			if (mLastPos > 0) {
				playerView.seekTo(mLastPos);
				mLastPos = 0;
			}
			playerView.start();
			mPlaybtn.setImageResource(R.drawable.ic_stop_media);
			mUIHandler.sendEmptyMessage(UI_EVENT_UPDATE_CURRPOSITION);
		}
		mUIHandler.removeCallbacks(hideControlBar);
		updateControlBar(true);
		mUIHandler.postDelayed(hideControlBar, showTime);
	}
	
	private void initMediaPlayer() {
		playerView.setVideoPath(mVideoSource);
		
		playerView.setOnCompletionListener(new OnCompletionListener() {

			@Override
			public void onCompletion(MediaPlayer mp) {
				state = 8;
				mUIHandler.removeMessages(UI_EVENT_UPDATE_CURRPOSITION);
				finish();
			}
		});

		playerView.setOnErrorListener(new OnErrorListener() {

			@Override
			public boolean onError(MediaPlayer mp, int what, int extra) {

				state = 9;
				mp.stop();
				mp.reset();
				mUIHandler.removeMessages(UI_EVENT_UPDATE_CURRPOSITION);
				
				play();

				return true;
			}
		});
	}
			
    Handler mUIHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
            /**
            * 更新进度及时间
            */
            case UI_EVENT_UPDATE_CURRPOSITION:
                int currPosition = playerView.getCurrentPosition();
                int duration = playerView.getDuration();
                updateTextViewWithTimeFormat(mDuration, duration);
                mProgress.setMax(duration);
                if (playerView.isPlaying() && !turnLeft && !turnRight){
                	if(!isSeeking || Math.abs(mProgress.getProgress() - currPosition) < 8){
                		mProgress.setProgress(currPosition);
                        updateTextViewWithTimeFormat(mCurrPostion, currPosition);
                		isSeeking = false;
                	}
                }
                mUIHandler.sendEmptyMessageDelayed(UI_EVENT_UPDATE_CURRPOSITION, 200);
                break;
            default:
                break;
            }
        }
    };
	
	/**
	 * 为控件注册回调处理函数
	 */
	private void registerCallbackForControl(){
		mPlaybtn.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				play();
			}
		});
		
		OnSeekBarChangeListener osbc = new OnSeekBarChangeListener() {
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				//Log.v(TAG, "progress: " + progress);
				updateTextViewWithTimeFormat(mCurrPostion, progress);
			}

			public void onStartTrackingTouch(SeekBar seekBar) {
				/**
				 * SeekBar开始seek时停止更新
				 */
				mUIHandler.removeMessages(UI_EVENT_UPDATE_CURRPOSITION);
			}

			public void onStopTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub
				if(!turnLeft && !turnRight){
					// SeekBark完成seek时执行seekTo操作并更新界面
					int iseekPos = seekBar.getProgress();
					isSeeking = true;
					playerView.seekTo(iseekPos);
					Log.v(TAG, "seek to " + iseekPos);
					mUIHandler.sendEmptyMessage(UI_EVENT_UPDATE_CURRPOSITION);
				}
			}
		};
		mProgress.setOnSeekBarChangeListener(osbc);
	}

	private void updateTextViewWithTimeFormat(TextView view, int second){
		second /= 1000;
		int hh = second / 3600;
		int mm = second % 3600 / 60;
		int ss = second % 60;
		String strTemp = null;
		if (0 != hh) {
			strTemp = String.format("%02d:%02d:%02d", hh, mm, ss);
		} else {
			strTemp = String.format("%02d:%02d", mm, ss);
		}
		view.setText(strTemp);
	}
	
	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		playerView.pause();
		mPlaybtn.setImageResource(R.drawable.ic_play_media);
		mUIHandler.removeCallbacks(updateTime);
	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		Log.v(TAG, "onResume");
		if (null != mWakeLock && (!mWakeLock.isHeld())) {
			mWakeLock.acquire();
		}
	}
	
	private long mTouchTime;
	private boolean barShow = true;

	@Override
	public boolean onTouchEvent(MotionEvent event) {

		// TODO Auto-generated method stub
		if (event.getAction() == MotionEvent.ACTION_DOWN)
			mTouchTime = System.currentTimeMillis();
		else if (event.getAction() == MotionEvent.ACTION_UP) {
			long time = System.currentTimeMillis() - mTouchTime;
			if (time < 400) {
				updateControlBar(!barShow);
				if(barShow){
					mUIHandler.postDelayed(hideControlBar, showTime);
				}
			}
		}

		return true;
	}

	public void updateControlBar(boolean show) {
		if (show) {
			mController.setVisibility(View.VISIBLE);
			titleLayout.setVisibility(View.VISIBLE);
			playLayout.setVisibility(View.VISIBLE);
		} else {
			mController.setVisibility(View.INVISIBLE);
			titleLayout.setVisibility(View.INVISIBLE);
			playLayout.setVisibility(View.INVISIBLE);
		}
		barShow = show;
	}
	
	Runnable hideControlBar = new Runnable() {
		@Override
		public void run() {
			updateControlBar(false);
		}
	};
	
	@Override
	public boolean dispatchKeyEvent(KeyEvent event) {
		
		int keyCode = event.getKeyCode();
		if(keyCode != KeyEvent.KEYCODE_MENU
				&& keyCode != KeyEvent.KEYCODE_DPAD_CENTER
				&& keyCode != KeyEvent.KEYCODE_ENTER
				&& keyCode != KeyEvent.KEYCODE_DPAD_LEFT
				&& keyCode != KeyEvent.KEYCODE_DPAD_RIGHT){
			if(event.getAction() == KeyEvent.ACTION_DOWN &&
					(keyCode != KeyEvent.KEYCODE_DPAD_UP || keyCode != KeyEvent.KEYCODE_DPAD_DOWN)){
				mUIHandler.removeCallbacks(hideControlBar);
				updateControlBar(!barShow);
				if(barShow){
					mUIHandler.postDelayed(hideControlBar, showTime);
				}
			}
			return super.dispatchKeyEvent(event);
		}

		mUIHandler.removeCallbacks(hideControlBar);
		updateControlBar(true);
		mUIHandler.postDelayed(hideControlBar, showTime);
		
		if(event.getAction() == KeyEvent.ACTION_DOWN){
			if(keyCode == KeyEvent.KEYCODE_DPAD_CENTER
					|| keyCode == KeyEvent.KEYCODE_ENTER){
				play();
			} else if(keyCode == KeyEvent.KEYCODE_DPAD_LEFT){
				turnLeft = true;
				mUIHandler.postDelayed(addProgress, 200);
			} else if(keyCode == KeyEvent.KEYCODE_DPAD_RIGHT){
				turnRight = true;
				mUIHandler.postDelayed(addProgress, 200);
			}
		} else if(event.getAction() == KeyEvent.ACTION_UP){
			 if(keyCode == KeyEvent.KEYCODE_DPAD_LEFT ||
					 keyCode == KeyEvent.KEYCODE_DPAD_RIGHT){
				mUIHandler.removeCallbacks(addProgress);
				isSeeking = true;
				playerView.seekTo(mProgress.getProgress());
				turnLeft = false;
				turnRight = false;
			}
		}
		
		return true;
	}
	
	@Override
	protected void onStop(){
		super.onStop();
	}
	
	@Override
	protected void onDestroy(){
		super.onDestroy();
		M3u8Server.finish();
	}

	Runnable updateTime = new Runnable() {
		@Override
		public void run() {
			if(video_time != null){
				video_time.setText(formatDate(null, "MM-dd HH:mm"));
			}
			mUIHandler.postDelayed(updateTime, 20000);
		}
	};
	
	Runnable addProgress = new Runnable() {
		@Override
		public void run() {
			int currPosition = mProgress.getProgress();
            int duration = playerView.getDuration();
            int addTime = 0;
            if(turnLeft){
            	addTime = -5000;
            } else if(turnRight){
            	addTime = 5000;
            }
            if(currPosition + addTime < 0){
            	updateTextViewWithTimeFormat(mCurrPostion, 0);
            	mProgress.setProgress(0);
            } else if(currPosition + addTime < duration){
            	updateTextViewWithTimeFormat(mCurrPostion, currPosition + addTime);
            	mProgress.setProgress(currPosition + addTime);
            	mUIHandler.postDelayed(addProgress, 200);
            }
		}
	};
	
	/**
	 * 格式化时间字符串(默认为yyyy-MM-dd HH:mm:ss)
	 * @param date
	 * @param format
	 * @return
	 */
	public static String formatDate(Date date, String format) {
		if(date == null)
			date = new Date();
		if(format == null)
			format = "yyyy-MM-dd HH:mm:ss";
		String code = new String();
		SimpleDateFormat matter = new SimpleDateFormat(format);
		code = matter.format(date);
		return code;
	}
}
