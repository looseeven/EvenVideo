package com.even.video;

import java.io.File;
import java.io.IOException;

import android.app.Activity;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.media.MediaPlayer.OnSeekCompleteListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;


public class VideoMainActivity extends Activity implements 
SurfaceHolder.Callback
,OnPreparedListener
,OnErrorListener
,OnCompletionListener
,OnSeekCompleteListener{
	private static final int STATE_ERROR              = -1; //错误
	private static final int STATE_IDLE               = 0; //空闲
	private static final int STATE_PREPARING          = 1; //准备
	private static final int STATE_PREPARED           = 2; //准备完毕
	private static final int STATE_PLAYING            = 3; //播放中
	private static final int STATE_PAUSED             = 4; //暂停
	private static final int STATE_PLAYBACK_COMPLETED = 5; //播放完成

	private int mMediaPlayerState = STATE_IDLE;
	private String TAG = "VideoMainActivity";
	private MediaPlayer mMediaPlayer = null;
    private static final int SHOW_PROGRESS = 0x01;
	/*
	 * the path of the file, or the http/rtsp URL of the stream you want to play
	 */
	String path = "/sdcard/钢铁侠.mp4";

	private Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			try {
				switch (msg.what) {
				case SHOW_PROGRESS:
					((ImageView)findViewById(R.id.pp)).getDrawable().setLevel(isPlaying() ? 1 : 0);
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

	SurfaceView mSurfaceView;
	private void initView() {
		mSurfaceView = (SurfaceView) findViewById(R.id.video);
		mSurfaceView.getHolder().addCallback(this);
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
		Log.i("md", "isInPlaybackState(): "+isInPlaybackState() +"    "+mMediaPlayerState);
		if (isInPlaybackState()) {
			mMediaPlayer.start();
			mMediaPlayerState = STATE_PLAYING;
		}
	}

	private void mPause(){
		if (isPlaying()) {
			mMediaPlayer.pause();
		}
		mMediaPlayerState = STATE_PAUSED;
	}
	
	private void mSeekTo(int sk){
		if (isInPlaybackState()) {
			mMediaPlayer.seekTo(sk);
		}
	}
	
	private void Stop(){
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
	 * @see android.media.MediaPlayer.OnPreparedListener#onPrepared(android.media.MediaPlayer)
	 * 当装载流媒体完毕的时候回调
	 */
	@Override
	public void onPrepared(MediaPlayer mp) {
		mMediaPlayerState = STATE_PREPARED;
	}

	/*
	 * (non-Javadoc)
	 * @see android.media.MediaPlayer.OnErrorListener#onError(android.media.MediaPlayer, int, int)
	 * 当播放中发生错误的时候回调
	 */
	@Override
	public boolean onError(MediaPlayer mp, int what, int extra) {
		mMediaPlayerState = STATE_ERROR;
		return false;
	}

	/*
	 * (non-Javadoc)
	 * @see android.media.MediaPlayer.OnCompletionListener#onCompletion(android.media.MediaPlayer)
	 * 当流媒体播放完毕的时候回调
	 */
	@Override
	public void onCompletion(MediaPlayer mp) {
		mMediaPlayerState = STATE_PLAYBACK_COMPLETED;
	}

	/*
	 * (non-Javadoc)
	 * @see android.media.MediaPlayer.OnSeekCompleteListener#onSeekComplete(android.media.MediaPlayer)
	 * 使用seekTo()设置播放位置的时候回调
	 */
	@Override
	public void onSeekComplete(MediaPlayer mp) {

	}

	@Override
	protected void onResume() {
		super.onResume();
//		mMediaPlayer.reset();
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
		Stop();
	}

	private boolean isInPlaybackState() {
		return (mMediaPlayer != null &&
				mMediaPlayerState != STATE_ERROR &&
				mMediaPlayerState != STATE_IDLE &&
				mMediaPlayerState != STATE_PREPARING);
	}
}
