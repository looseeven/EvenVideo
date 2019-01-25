package com.even.video;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import android.app.Activity;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.media.MediaPlayer.OnSeekCompleteListener;
import android.media.MediaPlayer.OnVideoSizeChangedListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;


public class VideoMainActivity extends Activity implements 
SurfaceHolder.Callback
,OnPreparedListener
,OnErrorListener
,OnCompletionListener
,OnSeekCompleteListener
,OnVideoSizeChangedListener
,OnSeekBarChangeListener{
	private static final int STATE_ERROR              = -1; //错误
	private static final int STATE_IDLE               = 0; //空闲
	private static final int STATE_PREPARING          = 1; //准备
	private static final int STATE_PREPARED           = 2; //准备完毕
	private static final int STATE_PLAYING            = 3; //播放中
	private static final int STATE_PAUSED             = 4; //暂停
	private static final int STATE_PLAYBACK_COMPLETED = 5; //播放完成
	private int         mVideoWidth;
	private int         mVideoHeight;
	private int         mSurfaceWidth;
	private int         mSurfaceHeight;

	private int mMediaPlayerState = STATE_IDLE;
	private String TAG = "VideoMainActivity";
	private MediaPlayer mMediaPlayer = null;
	private static final int SHOW_PROGRESS = 0x01;
	/*
	 * the path of the file, or the http/rtsp URL of the stream you want to play
	 */
//	String path = "/sdcard/FOOD_2560_1440.mp4";
	String path = "/sdcard/变形金刚_1280_720.mp4";

	private Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			try {
				switch (msg.what) {
				case SHOW_PROGRESS:
					((ImageView)findViewById(R.id.pp)).getDrawable().setLevel(isPlaying() ? 1 : 0);
					if (isPlaying()) {
						int duration = mMediaPlayer.getDuration();
						int position = mMediaPlayer.getCurrentPosition();
						if(duration < 0) {
							duration = 0;
						}
						if(position < 0) {
							position = 0;
						}
						int totaltime = duration / 1000;
						int stotaltime = totaltime;
						int mtotaltime = stotaltime / 60;
						int htotaltime = mtotaltime / 60;
						stotaltime %= 60;
						mtotaltime %= 60;
						htotaltime %= 24;
						if(htotaltime == 0) {
							((TextView)findViewById(R.id.totaltime)).setText(String.format(Locale.US, "%d:%02d", mtotaltime, stotaltime));
						} else {
							((TextView)findViewById(R.id.totaltime)).setText(String.format(Locale.US, "%d:%02d:%02d", htotaltime, mtotaltime, stotaltime));
						}
						int currenttime = position / 1000;
						int scurrenttime = currenttime;
						int mcurrenttime = scurrenttime / 60;
						int hcurrenttime = mcurrenttime / 60;
						scurrenttime %= 60;
						mcurrenttime %= 60;
						hcurrenttime %= 24;
						if(hcurrenttime == 0) {
							((TextView)findViewById(R.id.currenttime)).setText(String.format(Locale.US, "%d:%02d", mcurrenttime, scurrenttime));
						} else {
							((TextView)findViewById(R.id.currenttime)).setText(String.format(Locale.US, "%d:%02d:%02d", hcurrenttime, mcurrenttime, scurrenttime));
						}
						ProgressBar progress = (ProgressBar)findViewById(R.id.progress);
						progress.setMax(duration);
						progress.setProgress(position);
					}
					mHandler.removeMessages(SHOW_PROGRESS);
					mHandler.sendEmptyMessageDelayed(SHOW_PROGRESS, 1000);
					break;
				}
			} catch (Exception e) {

			}
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		initView();
		initData();
	}

	private SurfaceView mSurfaceView;
	private SeekBar mSeekBar;
	private TextView mCurrenttime;
	private TextView mTotaltime;
	private void initView() {
		mVideoWidth = 0;
		mVideoHeight = 0;
		mSurfaceView = (SurfaceView) findViewById(R.id.video);
		mSurfaceView.getHolder().addCallback(this);
		mSeekBar = (SeekBar) findViewById(R.id.progress);
		mSeekBar.setOnSeekBarChangeListener(this);
		mCurrenttime = (TextView) findViewById(R.id.currenttime);
		mTotaltime = (TextView) findViewById(R.id.totaltime);
	}

	private void initData() {
		File file = new File(path);
		if (file.exists() && file.length() > 0) {
			try {
				mMediaPlayer = new MediaPlayer();
				mMediaPlayer.setOnErrorListener(this);
				mMediaPlayer.setOnCompletionListener(this);
				mMediaPlayer.setOnSeekCompleteListener(this);
				mMediaPlayer.setOnPreparedListener(this);
				mMediaPlayer.setOnPreparedListener(this);
				mMediaPlayer.setOnVideoSizeChangedListener(this);
				mMediaPlayer.setDataSource(path);
				mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
				mMediaPlayer.setScreenOnWhilePlaying(true); //保持屏幕开启
				mMediaPlayer.prepareAsync();
				mMediaPlayerState = STATE_PREPARING;
			} catch (Exception e) {
				Log.i(TAG, e.toString());
			}
		}
	}

	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.pp:
			if (isPlaying()) {
				mPause();
			}else{
				mStart();
			}
			break;
		}
		mHandler.sendEmptyMessageDelayed(SHOW_PROGRESS, 250);
	}

	private void mStart(){
		mHandler.removeMessages(SHOW_PROGRESS);
		mHandler.sendEmptyMessageDelayed(SHOW_PROGRESS, 1000);
		if (isInPlaybackState()) {
			mMediaPlayer.start();
			mMediaPlayerState = STATE_PLAYING;
		}
	}

	private void mPause(){
		if (isPlaying()) {
			mMediaPlayer.pause();
		}
		mHandler.removeMessages(SHOW_PROGRESS);
		mMediaPlayerState = STATE_PAUSED;
	}

	private void mSeekTo(int sk){
		if (isInPlaybackState()) {
			mMediaPlayer.seekTo(sk);
		}
	}

	private void Stop(){
		mHandler.removeMessages(SHOW_PROGRESS);
		if (mMediaPlayer != null) {
			mMediaPlayer.stop();
			mMediaPlayer.release();
			mMediaPlayer = null;
		}
	}

	private boolean isPlaying(){
		return mMediaPlayer.isPlaying()&&isInPlaybackState();
	}

	@Override
	public void surfaceCreated(SurfaceHolder surfaceHolder) {
		mMediaPlayer.setDisplay(surfaceHolder);
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
	}

	/*
	 * (non-Javadoc)
	 * 当装载流媒体完毕的时候回调
	 */
	@Override
	public void onPrepared(MediaPlayer mp) {
		mMediaPlayerState = STATE_PREPARED;
	}

	/*
	 * (non-Javadoc)
	 * 当播放中发生错误的时候回调
	 */
	@Override
	public boolean onError(MediaPlayer mp, int what, int extra) {
		mMediaPlayerState = STATE_ERROR;
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 当流媒体播放完毕的时候回调
	 */
	@Override
	public void onCompletion(MediaPlayer mp) {
		mMediaPlayerState = STATE_PLAYBACK_COMPLETED;
	}

	/*
	 * (non-Javadoc)
	 * 使用seekTo()设置播放位置的时候回调
	 */
	@Override
	public void onSeekComplete(MediaPlayer mp) {

	}

	@Override
	protected void onResume() {
		super.onResume();
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (mMediaPlayer.isPlaying()) {
			mMediaPlayer.pause();
		}
		mMediaPlayerState = STATE_PAUSED;
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		mHandler.removeMessages(SHOW_PROGRESS);
		Stop();
	}

	private boolean isInPlaybackState() {
		return (mMediaPlayer != null &&
				mMediaPlayerState != STATE_ERROR &&
				mMediaPlayerState != STATE_IDLE &&
				mMediaPlayerState != STATE_PREPARING);
	}

	/*
	 * (non-Javadoc)
	 * 这里可拿到当前加载视频文件的分辨率 1280x720 1920x1080 ..
	 * 通过视频分辨率给播放界面定大小
	 */
	@Override
	public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
		mVideoWidth = mp.getVideoWidth();
		mVideoHeight = mp.getVideoHeight();
		Log.i(TAG, "width: "+width+"  height: "+height);
		if (mVideoHeight != 0 && mVideoWidth != 0) {
			mSurfaceView.getHolder().setFixedSize(mVideoWidth, mVideoHeight);
			mSurfaceView.requestLayout();
		}
	}

	/*
	 * 视频进度条
	 */
	@Override
	public void onProgressChanged(SeekBar seekBar, int progress,
			boolean fromUser) {
		if(fromUser) {
			mSeekTo(progress);
		}
	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {

	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {

	}
}
