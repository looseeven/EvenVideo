package com.even.video;

import java.io.File;
import android.app.Activity;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;


public class MainActivity extends Activity implements SurfaceHolder.Callback{

	private MediaPlayer mMediaPlayer = null;
	String path = "/sdcard/钢铁侠.mp4";

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
				mMediaPlayer.setDataSource(path);
				mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
				mMediaPlayer.prepareAsync();
				mMediaPlayer.setOnPreparedListener(new OnPreparedListener() {

					@Override
					public void onPrepared(MediaPlayer mp) {
						mMediaPlayer.start();
					}
				});
			} catch (Exception e) {
				Log.i("md", e.toString());
			}
		}
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
}
