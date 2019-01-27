package com.even.video;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
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
import android.view.Display;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.provider.MediaStore;
import android.provider.Settings.System;


public class VideoMainActivity extends Activity implements 
SurfaceHolder.Callback
,OnPreparedListener
,OnErrorListener
,OnCompletionListener
,OnSeekCompleteListener
,OnVideoSizeChangedListener
,OnSeekBarChangeListener
,OnItemClickListener
{
	private static final int STATE_ERROR              = -1; //错误
	private static final int STATE_IDLE               = 0; //空闲
	private static final int STATE_PREPARING          = 1; //准备
	private static final int STATE_PREPARED           = 2; //准备完毕
	private static final int STATE_PLAYING            = 3; //播放中
	private static final int STATE_PAUSED             = 4; //暂停
	private static final int STATE_PLAYBACK_COMPLETED = 5; //播放完成
	/*
	 * 视频宽高
	 */
	private int         mVideoWidth; 
	private int         mVideoHeight;

	private int mMediaPlayerState = STATE_IDLE;
	private static final String TAG = AnimationUtil.class.getSimpleName();
	private MediaPlayer mMediaPlayer = null;
	private static final int SHOW_PROGRESS = 0x01;
	private static final int GONE_PLAYUI = 0x02;
	private static final int VISIBLE_PLAYUI = 0x03;

	private int mCurrentPos;  //播放的位置

	private SurfaceView mSurfaceView;
	private SeekBar mSeekBar;  //进度条
	private TextView mCurrenttime; //当前进度tx
	private TextView mTotaltime; //总长度tx
	private RelativeLayout mPlayui; //控制模块
	private ImageView mCt; //大窗口
	AudioManager mAudioManager = null;//音频管理器
	private int mPhoneHeigth; //当前手机屏幕的高度
	private int mPhoneWidth;//当前手机屏幕的宽度
	private boolean isLandScape = false;

	private mListAdapter adapter;
	/*
	 * the path of the file, or the http/rtsp URL of the stream you want to play
	 */
	private String mPath = "";

	private Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			try {
				switch (msg.what) {
				case SHOW_PROGRESS:{
					if (isPlaying()) {
						int duration = mMediaPlayer.getDuration();  //视频长度
						int position = mMediaPlayer.getCurrentPosition(); //当前播放位置
						mCurrentPos = mMediaPlayer.getCurrentPosition();
						if(duration < 0) {
							duration = 0;
						}
						if(position < 0) {
							position = 0;
						}

						/*
						 * 换算总长度 00:00格式显示
						 */
						mTotaltime.setText(chengTimeShow(duration));
						/*
						 * 换算当前进度 00:00格式显示
						 */
						mCurrenttime.setText(chengTimeShow(position));
						/*
						 * 设置进度条属性
						 */
						ProgressBar progress = (ProgressBar)findViewById(R.id.progress);
						progress.setMax(duration);
						progress.setProgress(position);
					}
					((ImageView)findViewById(R.id.pp)).getDrawable().setLevel(isPlaying() ? 1 : 0);
					mHandler.removeMessages(SHOW_PROGRESS);
					mHandler.sendEmptyMessageDelayed(SHOW_PROGRESS, 250);
				}
				break;
				case GONE_PLAYUI:
					mHandler.removeMessages(GONE_PLAYUI);
					mPlayui.setVisibility(View.GONE);
					mPlayui.setAnimation(AnimationUtil.moveLocationToBottom());
					mCt.setAnimation(AnimationUtil.moveLocationToTop());
					mCt.setVisibility(View.GONE);
					if (isLandScape) {
						getWindow().setFlags(
								WindowManager.LayoutParams.FLAG_FULLSCREEN, 
								WindowManager.LayoutParams. FLAG_FULLSCREEN
								);
					}
					break;
				case VISIBLE_PLAYUI:
					mHandler.sendEmptyMessageDelayed(GONE_PLAYUI,6000);
					if (mCt.getVisibility() != View.VISIBLE) {
						mCt.setVisibility(View.VISIBLE);
						mCt.setAnimation(AnimationUtil.moveTopToLocation());
					}
					if(mPlayui.getVisibility() != View.VISIBLE){
						mPlayui.setVisibility(View.VISIBLE);
						mPlayui.setAnimation(AnimationUtil.moveBottomToLocation());
					}
					getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
					break;
				}
			} catch (Exception e) {

			}
		}
	};



	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.video);
		if (savedInstanceState != null) 
			mCurrentPos = savedInstanceState.getInt("currentPos");
		initView();
		initData();
	}

	private ListView ls_video;
	private void initView() {
		mVideoWidth = 0;
		mVideoHeight = 0;
		mSurfaceView = (SurfaceView) findViewById(R.id.video);
		mSurfaceView.getHolder().addCallback(this);
		mSeekBar = (SeekBar) findViewById(R.id.progress);
		mSeekBar.setOnSeekBarChangeListener(this);
		mCurrenttime = (TextView) findViewById(R.id.currenttime);
		mTotaltime = (TextView) findViewById(R.id.totaltime);
		mPlayui = (RelativeLayout) findViewById(R.id.playui);
		mCt = (ImageView) findViewById(R.id.ct);
		mAudioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
		ls_video = (ListView)findViewById(R.id.ls_video);
		adapter = new mListAdapter(this);
		ls_video.setAdapter(adapter);
		ls_video.setOnItemClickListener(this);
	}

	private void initData() {
		getList();
		Configuration mConfiguration = this.getResources().getConfiguration(); //获取设置的配置信息
		int ori = mConfiguration.orientation; //获取屏幕方向
		if (ori == mConfiguration.ORIENTATION_LANDSCAPE) {
			isLandScape = true;
		} else if (ori == mConfiguration.ORIENTATION_PORTRAIT) {
			isLandScape = false;
		}
		Display display = getWindowManager().getDefaultDisplay();
		mPhoneHeigth = display.getHeight();
		mPhoneWidth = display.getWidth();
	}

	private void initVideo(String path){
		File file = new File(mPath);
		if (file.exists() && file.length() > 0) {
			try {
				/*
				 * 初始化MediaPlayer
				 */
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
		case R.id.ct:
			if (isLandScape) {
				setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);//强制为竖屏
			}else{
				setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);//强制为横屏
			}
			break;
		}
		mHandler.removeMessages(GONE_PLAYUI); 
		mHandler.sendEmptyMessage(VISIBLE_PLAYUI);
	}

	/*
	 * 快进
	 */
	private void mFastforward(){
		if (isPlaying()) {
			int pos = mMediaPlayer.getCurrentPosition();
			pos += 15000;
			if (pos > 0 && pos < mMediaPlayer.getDuration()) {
				mSeekTo(pos);
			}
		}
	}

	/*
	 * 快退
	 */
	private void mRewind(){
		if (isPlaying()) {
			int pos = mMediaPlayer.getCurrentPosition();
			pos -= 15000;
			if (pos > 0 && pos < mMediaPlayer.getDuration()) {
				mSeekTo(pos);
			}
		}
	}

	/*
	 * 开始播放
	 */
	private void mStart(){
		mHandler.removeMessages(SHOW_PROGRESS);
		mHandler.sendEmptyMessageDelayed(SHOW_PROGRESS, 250);
		if (isInPlaybackState()) {
			mMediaPlayer.start();
			mSeekTo(mCurrentPos);
			mMediaPlayerState = STATE_PLAYING;
		}
	}

	/*
	 * 暂停播放
	 */
	private void mPause(){
		if (isPlaying()) {
			mMediaPlayer.pause();
		}
		mMediaPlayerState = STATE_PAUSED;
	}

	/*
	 * 设置进度
	 */
	private void mSeekTo(int sk){
		if (isInPlaybackState()) {
			mMediaPlayer.seekTo(sk);
		}
	}

	/*
	 * 停止播放
	 */
	private void Stop(){
		mHandler.removeMessages(SHOW_PROGRESS);
		if (mMediaPlayer != null) {
			mMediaPlayer.stop();
			mMediaPlayer.release();
			mMediaPlayer = null;
		}
	}

	/*
	 * 判断是否在播放状态
	 */
	private boolean isPlaying(){
		return mMediaPlayer.isPlaying()&&isInPlaybackState();
	}

	/*
	 * surface创建
	 */
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
	 * 当装载流媒体完毕的时候回调
	 */
	@Override
	public void onPrepared(MediaPlayer mp) {
		mMediaPlayerState = STATE_PREPARED;
		mStart();
	}

	/*
	 * 当播放中发生错误的时候回调
	 */
	@Override
	public boolean onError(MediaPlayer mp, int what, int extra) {
		mMediaPlayerState = STATE_ERROR;
		return false;
	}

	/*
	 * 当流媒体播放完毕的时候回调
	 */
	@Override
	public void onCompletion(MediaPlayer mp) {
		mMediaPlayerState = STATE_PLAYBACK_COMPLETED;
		showList();
	}

	/*
	 * 使用seekTo()设置播放进度的时候回调
	 */
	@Override
	public void onSeekComplete(MediaPlayer mp) {
	}

	@Override
	protected void onResume() {
		super.onResume();
		mHandler.sendEmptyMessageDelayed(GONE_PLAYUI, 6000);
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

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt("currentPos", mCurrentPos);
	}

	private boolean isInPlaybackState() {
		return (mMediaPlayer != null &&
				mMediaPlayerState != STATE_ERROR &&
				mMediaPlayerState != STATE_IDLE &&
				mMediaPlayerState != STATE_PREPARING);
	}

	/*
	 * 这里可拿到当前加载视频文件的分辨率 1280x720 1920x1080 ..
	 * 通过视频分辨率给播放界面适配大小
	 */
	@Override
	public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
		// 首先取得video的宽和高
		mVideoWidth = mp.getVideoWidth();
		mVideoHeight = mp.getVideoHeight();

		if (mVideoWidth > mPhoneWidth || mVideoWidth > mPhoneHeigth) {
			// 如果video的宽或者高超出了当前屏幕的大小，则要进行缩放
			float wRatio = (float) mVideoWidth / (float) mPhoneWidth;
			float hRatio = (float) mVideoHeight / (float) mPhoneHeigth;

			// 选择大的一个进行缩放
			float ratio = Math.max(wRatio, hRatio);
			mVideoWidth = (int) Math.ceil((float) mVideoWidth / ratio);
			mVideoHeight = (int) Math.ceil((float) mVideoHeight / ratio);

			if (mVideoHeight != 0 && mVideoWidth != 0) {
				mSurfaceView.getHolder().setFixedSize(mVideoWidth, mVideoHeight);
				mSurfaceView.requestLayout();
			}
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

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		return gd.onTouchEvent(event);
	}

	/*
	 * 手势动作
	 */
	@SuppressWarnings("deprecation")
	private GestureDetector gd = new GestureDetector(new GestureDetector.SimpleOnGestureListener() {
		@Override
		public boolean onDoubleTap(MotionEvent e) {
			/*
			 * 双击
			 */
			if (isPlaying()) {
				mPause();
			}else {
				mStart();
			}
			return true;
		}

		@Override
		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
				float velocityY) {
			float x = e2.getX() - e1.getX();
			float y = e2.getY() - e1.getY();
			if(x > 300) { 
				mFastforward();
				return true;
			} else if(x < -300) {
				mRewind();
				return true;
			}

			if(y < -100) {
				if (e1.getX()<(mPhoneWidth/2)) { //在做区域
					mAddAudioVolume();
				}else{
					mAddLightness();
				}
				return true;
			} else if(y > 100) {
				if (e1.getX()<(mPhoneWidth/2)) {
					mLessAudioVolume();
				}else{
					mLessLightness();
				}
				return true;
			}
			return false;
		}

		@Override
		public boolean onDown(MotionEvent e) {
			if ((e.getY() < mPhoneHeigth && e.getY() > mPhoneHeigth-250)||(e.getY() < 250 && e.getY() > 0)) { //判断是否在底部/顶部点击
				/*
				 * 底部控制栏的显示与隐藏逻辑
				 */
				if (mPlayui.getVisibility() != View.VISIBLE) { 
					mHandler.sendEmptyMessage(VISIBLE_PLAYUI); 
				}else{
					mHandler.removeMessages(GONE_PLAYUI); 
					mHandler.sendEmptyMessage(VISIBLE_PLAYUI);
				}
			}
			return super.onDown(e);
		}
	});

	/*
	 *  获取当前亮度
	 */
	public static int GetLightness(Activity act){
		return System.getInt(act.getContentResolver(),System.SCREEN_BRIGHTNESS,-1);
	}

	/*
	 *  设置亮度
	 */
	public static void SetLightness(Activity act,int value)
	{        
		try {
			System.putInt(act.getContentResolver(),System.SCREEN_BRIGHTNESS,value); 
			WindowManager.LayoutParams lp = act.getWindow().getAttributes(); 
			lp.screenBrightness = (value<=0?1:value) / 255f;
			act.getWindow().setAttributes(lp);
		} catch (Exception e) {
			Log.i(TAG, ""+e.toString());
		}        
	}

	/*
	 * 增加亮度
	 */
	private void mAddLightness(){
		int light = GetLightness(this);
		light += 25;
		if (light>0 && light <255) {
			SetLightness(this,light);
		}
	}

	/*
	 * 减少亮度
	 */
	private void mLessLightness(){
		int light = GetLightness(this);
		light -= 25;
		if (light>0 && light <255) {
			SetLightness(this,light);
		}
	}

	/*
	 * 增加音量
	 */
	private void mAddAudioVolume(){
		mAudioManager.adjustStreamVolume(
				AudioManager.STREAM_MUSIC, 
				AudioManager.ADJUST_RAISE,
				AudioManager.FX_FOCUS_NAVIGATION_UP
				);
	}

	/*
	 * 减小音量
	 */
	ArrayList<LVideo> mList ;  
	private void mLessAudioVolume(){
		mAudioManager.adjustStreamVolume(
				AudioManager.STREAM_MUSIC,
				AudioManager.ADJUST_LOWER,
				AudioManager.FLAG_SHOW_UI 
				);
	}
	public List<LVideo> getList() {  
		if (this != null) {  
			Cursor cursor = this.getContentResolver().query(  
					MediaStore.Video.Media.EXTERNAL_CONTENT_URI, null, null,  
					null, null);  
			if (cursor != null) {  
				mList = new ArrayList<LVideo>();  
				while (cursor.moveToNext()) {  
					int id = cursor.getInt(cursor  
							.getColumnIndexOrThrow(MediaStore.Video.Media._ID));  
					String title = cursor  
							.getString(cursor  
									.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE));  
					String album = cursor  
							.getString(cursor  
									.getColumnIndexOrThrow(MediaStore.Video.Media.ALBUM));  
					String artist = cursor  
							.getString(cursor  
									.getColumnIndexOrThrow(MediaStore.Video.Media.ARTIST));  
					String displayName = cursor  
							.getString(cursor  
									.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME));  
					String mimeType = cursor  
							.getString(cursor  
									.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE));  
					String path = cursor  
							.getString(cursor  
									.getColumnIndexOrThrow(MediaStore.Video.Media.DATA));  
					int duration = cursor  
							.getInt(cursor  
									.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION));  
					long size = cursor  
							.getLong(cursor  
									.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE));  
					LVideo video = new LVideo();  
					video.setName(title);  
					video.setSize(size);  
					video.setUrl(path);  
					video.setDuration(duration);  
					video.setId(id);  
					mList.add(video);  
				}  
				cursor.close();  
			}  
		}  
		return mList;  
	}  

	/*
	 *  获取视频缩略图
	 */
	public static Bitmap getVideoThumbnail(String videoPath) {
		  MediaMetadataRetriever media =new MediaMetadataRetriever();
		  media.setDataSource(videoPath);
		  Bitmap bitmap = media.getFrameAtTime();
		  return bitmap;
		}


	private class mListAdapter extends BaseAdapter {
		public mListAdapter(Context context) {
			mContext = context;
		}
		@Override
		public int getCount() {
			if(getList() == null) {
				return 0;
			} 
			return getList().size();
		}

		@Override
		public Object getItem(int position) {
			return null;
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View v;
			if(convertView == null) {
				v = newView(parent);
			} else {
				v = convertView;
			}
			bindView(v, position, parent);
			return v;
		}
		private class ViewHolder {
			ImageView icon;
			TextView title;
			TextView size;
			TextView time;
			TextView type;
		}

		private View newView(ViewGroup parent) {
			View v = LayoutInflater.from(mContext).inflate(R.layout.video_item, parent, false);
			ViewHolder vh = new ViewHolder();
			vh.icon = (ImageView) v.findViewById(R.id.video_bitmap);
			vh.title = (TextView) v.findViewById(R.id.video_title);
			vh.size = (TextView) v.findViewById(R.id.video_size);
			vh.time = (TextView) v.findViewById(R.id.video_time);
			v.setTag(vh);
			return v;
		}

		@SuppressLint("NewApi") private void bindView(View v, int position, ViewGroup parent) {
			ViewHolder vh = (ViewHolder) v.getTag();
			vh.title.setText(getList().get(position).getName());
			vh.time.setText(chengTimeShow(getList().get(position).getDuration()));
			vh.size.setText(toMB(getList().get(position).getSize()) +"MB");
			Drawable drawable = new BitmapDrawable(getVideoThumbnail(getList().get(position).getUrl())); 
			vh.icon.setBackground(drawable);
		}
		private Context mContext;
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View view, int position, long arg3) {
		mPath = getList().get(position).getUrl();
		showVideo();
		initVideo(mPath);
	}

	private void showVideo() {
		findViewById(R.id.id_player).setVisibility(View.VISIBLE);
		ls_video.setVisibility(View.GONE);
	};

	private void showList(){
		findViewById(R.id.id_player).setVisibility(View.GONE);
		ls_video.setVisibility(View.VISIBLE);
	}

	@Override
	public void onBackPressed() {
		super.onBackPressed();
		if (ls_video.getVisibility() == View.GONE) {
			showList();
		}else{
			finish();
		}
	}

	private String chengTimeShow(int l){
		/*
		 * 换算总长度 00:00格式显示
		 */
		int totaltime = l / 1000;
		int stotaltime = totaltime;
		int mtotaltime = stotaltime / 60;
		int htotaltime = mtotaltime / 60;
		stotaltime %= 60;
		mtotaltime %= 60;
		htotaltime %= 24;
		if(htotaltime == 0) {
			return String.format(Locale.US, "%d:%02d", mtotaltime, stotaltime);
		} else {
			return String.format(Locale.US, "%d:%02d:%02d", htotaltime, mtotaltime, stotaltime);
		}
	}

	private int toMB(long i){
		int mb = (int) (i/1024/1024);
		return mb;
	}
}
