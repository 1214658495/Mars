package com.byd.vtdr2;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.pili.pldroid.player.AVOptions;
import com.pili.pldroid.player.PLMediaPlayer;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;

public class ActivityRTVideo extends BaseActivity {
    private static final String TAG = "ActivityRTVideo";
    //类名若要更改，需提交给5部，不然无法实现全屏
    Unbinder unbinder;
    @BindView(R.id.sv_videoPlayView_activity)
    SurfaceView svVideoPlayView;
    @BindView(R.id.ib_playVideo3_activity)
    ImageButton ibPlayVideo;
    @BindView(R.id.btn_back_to_videoGridview)
    ImageButton btnBackToVideoGridview;
    @BindView(R.id.tv_title_video_activity)
    TextView tvTitleVideo;
    @BindView(R.id.sb_mediaCtrlBar_activity)
    SeekBar sbMediaCtrlBar;

    private RelativeLayout rlBarShowVideoTitle;
    private ImageButton btnStop;
    private TextView tvCurrentTime;
    private LinearLayout LoadingView;
    private TextView tvEndTime;
    private LinearLayout llBarEditVideo;
    private ImageButton btnStart;
    private String url;
    private String fileName;
    private AVOptions mAVOptions;
    private PLMediaPlayer mMediaPlayer;
    private static final int SHOW_PROGRESS1 = 0;
    private static final int SHOW_CONTROLLER1 = 1;
    private boolean isShowControl;
    private boolean isVideoStop;
    private boolean mDragging;

    private int durationtime = 0;
    private int CurrentTime = 0;
    private int lastTime = 0;
    private int mSurfaceWidth = 0;
    private int mSurfaceHeight = 0;
    private AudioManager audioManager;
    private TelephonyManager telephonyManager;
    private MyPhoneListener myPhoneListener;
    private boolean isRinging;

    protected Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case SHOW_PROGRESS1:
                    long pos = setProgress();
                    // if (!mDragging) {
                    msg = obtainMessage(SHOW_PROGRESS1);
                    sendMessageDelayed(msg, 1000 - (pos % 1000));
                    // }
                    break;
                case SHOW_CONTROLLER1:
                    showControlBar();
                    break;
                default:
                    break;
            }
        }
    };

    @SuppressLint("WrongViewCast")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_rtvideo);
        unbinder = ButterKnife.bind(this);
        Intent intent = getIntent();
        url = intent.getStringExtra("url");
        if (url.contains("LOCK")) {
            fileName = url.substring(31);
        } else {
            fileName = url.substring(32);
        }
        CurrentTime = intent.getIntExtra("CurrentTime", 0);
        lastTime = CurrentTime;
        tvCurrentTime = findViewById(R.id.tv_currentTime_activity);
        tvEndTime = findViewById(R.id.tv_endTime_activity);
        rlBarShowVideoTitle = findViewById(R.id.rl_bar_showVideoTitle_activity);
        llBarEditVideo = findViewById(R.id.ll_bar_editVideo_activity);
        LoadingView = findViewById(R.id.loadingView_activity);
        btnStart = findViewById(R.id.btn_start3_activity);
        btnStop = findViewById(R.id.btn_stop3_activity);
        initData();

    }

    private void initData() {

        tvTitleVideo.setText(fileName);

        svVideoPlayView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                prepare();
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                mSurfaceWidth = width;
                mSurfaceHeight = height;
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                releaseWithoutStop();
            }
        });
        svVideoPlayView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showControlBar();
                mHandler.removeMessages(SHOW_CONTROLLER1);
                mHandler.sendEmptyMessageDelayed(SHOW_CONTROLLER1, 3000);
            }
        });
        mAVOptions = new AVOptions();
        // the unit of timeout is ms
        mAVOptions.setInteger(AVOptions.KEY_PREPARE_TIMEOUT, 10 * 1000);
        mAVOptions.setInteger(AVOptions.KEY_GET_AV_FRAME_TIMEOUT, 10 * 1000);
        mAVOptions.setInteger(AVOptions.KEY_PROBESIZE, 64 * 1024);
        // Some optimization with buffering mechanism when be set to 1
        mAVOptions.setInteger(AVOptions.KEY_LIVE_STREAMING, 0);//2018/06/14  0 to 1
        // 1 -> hw codec enable, 0 -> disable [recommended]
        mAVOptions.setInteger(AVOptions.KEY_MEDIACODEC, 0);
        // whether start play automatically after prepared, default value is 1
        mAVOptions.setInteger(AVOptions.KEY_START_ON_PREPARED, 0);
        audioManager = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
        telephonyManager = (TelephonyManager) this.getSystemService(TELEPHONY_SERVICE);
        myPhoneListener = new MyPhoneListener();
    }
    //    如下声音焦点的监听并未使用
    private AudioManager.OnAudioFocusChangeListener mAudioFocusListener = new AudioManager.OnAudioFocusChangeListener() {
        @Override
        public void onAudioFocusChange(int focusChange) {
            switch (focusChange) {
                case AudioManager.AUDIOFOCUS_LOSS:
                    //长时间丢失焦点,当其他应用申请的焦点为AUDIOFOCUS_GAIN时，
                    //会触发此回调事件，例如播放QQ音乐，网易云音乐等
                    //通常需要暂停音乐播放，若没有暂停播放就会出现和其他音乐同时输出声音
                    Log.d(TAG, "AUDIOFOCUS_LOSS");
//                    stop();
                    //释放焦点，该方法可根据需要来决定是否调用
                    //若焦点释放掉之后，将不会再自动获得
//                    mAudioManager.abandonAudioFocus(mAudioFocusChange);
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                    //短暂性丢失焦点，当其他应用申请AUDIOFOCUS_GAIN_TRANSIENT或AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE时，
                    //会触发此回调事件，例如播放短视频，拨打电话等。
                    //通常需要暂停音乐播放
//                    stop();
                    Log.d(TAG, "AUDIOFOCUS_LOSS_TRANSIENT");
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                    //短暂性丢失焦点并作降音处理
                    Log.d(TAG, "AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK");
                    break;
                case AudioManager.AUDIOFOCUS_GAIN:
                    //当其他应用申请焦点之后又释放焦点会触发此回调
                    //可重新播放音乐
                    Log.d(TAG, "AUDIOFOCUS_GAIN");
//                    start();
                    break;
                default:
                    break;
            }
        }
    };

    @Override
    public void onResume() {
        super.onResume();
        audioManager.requestAudioFocus(mAudioFocusListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        telephonyManager.listen(myPhoneListener, PhoneStateListener.LISTEN_CALL_STATE);
        if (mMediaPlayer != null && isVideoStop) {
            mMediaPlayer.start();
            isVideoStop = false;
            mHandler.sendEmptyMessage(SHOW_PROGRESS1);
            mHandler.removeMessages(SHOW_CONTROLLER1);
            mHandler.sendEmptyMessageDelayed(SHOW_CONTROLLER1, 3000);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mMediaPlayer != null) {
            mMediaPlayer.pause();
            mHandler.removeMessages(SHOW_PROGRESS1);
        }
        isVideoStop = true;
    }

    @Override
    public void onStop() {
        super.onStop();
        audioManager.abandonAudioFocus(mAudioFocusListener);
        telephonyManager.listen(null, PhoneStateListener.LISTEN_CALL_STATE);
//        stop后（即app退到后台）置标志位，监听到到电话 播放器也不会播放
        isRinging = false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        release();
//        new MyTheard().start();
        unbinder.unbind();
        mHandler.removeMessages(SHOW_PROGRESS1);
        mHandler.removeCallbacksAndMessages(null);
        myPhoneListener = null;
    }


    private class MyTheard extends Thread {
        @Override
        public void run() {
            release();
        }
    }

    public void release() {
        if (mMediaPlayer != null) {
            mMediaPlayer.stop();
            // TODO: 2018/3/12 导致内存越来越大
            mMediaPlayer.release();
            mMediaPlayer = null;
            System.gc();
        }
    }
    private long setProgress() {
        // TODO: 2017/12/15 setprogress 的逻辑、handler的运行原理。
        if (mMediaPlayer == null) {
            return 0;
        }
        long currentPosition = mMediaPlayer.getCurrentPosition();
        long duration = mMediaPlayer.getDuration();
        if (tvCurrentTime != null && tvEndTime != null && sbMediaCtrlBar != null) {
            tvCurrentTime.setText(generateTime(currentPosition));
            tvEndTime.setText(generateTime(duration));
            sbMediaCtrlBar.setProgress((int) (currentPosition / 1000));
        }
        return currentPosition;
    }

    private static String generateTime(long position) {
        int totalSeconds = (int) (position / 1000);
        int seconds = totalSeconds % 60;
        int minutes = (totalSeconds / 60) % 60;
        int hours = totalSeconds / 3600;
        if (hours > 0) {
            return String.format(Locale.US, "%02d:%02d:%02d", hours, minutes,
                    seconds);
        } else {
            return String.format(Locale.US, "%02d:%02d", minutes, seconds);
        }
    }

    private void showControlBar() {
        if (mMediaPlayer == null) {
            return;
        }
        if (isShowControl) {
            rlBarShowVideoTitle.setVisibility(View.VISIBLE);
            llBarEditVideo.setVisibility(View.VISIBLE);
            if (btnStart.getVisibility() == View.INVISIBLE && isVideoStop) {
                btnStart.setVisibility(View.VISIBLE);
            }
            if (btnStop.getVisibility() == View.INVISIBLE && !isVideoStop) {
                btnStop.setVisibility(View.VISIBLE);
            }
        } else {
            rlBarShowVideoTitle.setVisibility(View.INVISIBLE);
            llBarEditVideo.setVisibility(View.INVISIBLE);
            if (btnStart.getVisibility() == View.VISIBLE) {
                btnStart.setVisibility(View.INVISIBLE);
            }
            if (btnStop.getVisibility() == View.VISIBLE) {
                btnStop.setVisibility(View.INVISIBLE);
            }
        }
        isShowControl = !isShowControl;
    }

    private void prepare() {

        if (mMediaPlayer != null) {
            mMediaPlayer.setDisplay(svVideoPlayView.getHolder());
            mMediaPlayer.seekTo(mMediaPlayer.getCurrentPosition());
            return;
        }
        try {
            mMediaPlayer = new PLMediaPlayer(getApplicationContext(), mAVOptions);
            mMediaPlayer.setOnPreparedListener(mOnPreparedListener);
            mMediaPlayer.setOnCompletionListener(mOnCompletionListener);
            mMediaPlayer.setOnInfoListener(mOnInfoListener);
            mMediaPlayer.setOnBufferingUpdateListener(mOnBufferingUpdateListener);
//            mMediaPlayer.setOnVideoSizeChangedListener(mOnVideoSizeChangedListener);
            mMediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
            mMediaPlayer.setDataSource(url);
            mMediaPlayer.setDisplay(svVideoPlayView.getHolder());
            mMediaPlayer.prepareAsync();
        } catch (UnsatisfiedLinkError e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private PLMediaPlayer.OnPreparedListener mOnPreparedListener = new PLMediaPlayer.OnPreparedListener() {
        @Override
        public void onPrepared(PLMediaPlayer mp, int preparedTime) {
            Log.i(TAG, "On Prepared !");
            mMediaPlayer.start();
            long duration = mMediaPlayer.getDuration();
            durationtime = (int) (duration / 1000);
            /*
             * 视屏播放后开始进度条初始化
             * */
            sbMediaCtrlBar.setMax(durationtime);
            sbMediaCtrlBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    CurrentTime = progress;
                    if (!fromUser) {
                        return;
                    }
                    long newposition = (progress) * 1000;
                    String time = generateTime(newposition);
                    tvCurrentTime.setText(time);
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                    mHandler.removeMessages(SHOW_PROGRESS1);
                    mDragging = false;
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    mMediaPlayer.seekTo(seekBar.getProgress() * 1000);
                    CurrentTime = seekBar.getProgress();
                    mHandler.removeMessages(SHOW_PROGRESS1);
                    mDragging = false;
                    mHandler.sendEmptyMessageDelayed(SHOW_PROGRESS1, 500);
                }
            });

            //                    旋转刷新
            if (lastTime != 0) {
                mMediaPlayer.seekTo((lastTime) * 1000);
                sbMediaCtrlBar.setProgress((lastTime));
                lastTime = 0;
            }
            mHandler.sendEmptyMessage(SHOW_PROGRESS1);
            mHandler.sendEmptyMessageDelayed(SHOW_CONTROLLER1, 3000);
        }
    };

    private PLMediaPlayer.OnInfoListener mOnInfoListener = new PLMediaPlayer.OnInfoListener() {
        @Override
        public boolean onInfo(PLMediaPlayer mp, int what, int extra) {
            Log.i(TAG, "OnInfo, what = " + what + ", extra = " + extra);
            switch (what) {
                case PLMediaPlayer.MEDIA_INFO_BUFFERING_START:
                    LoadingView.setVisibility(View.VISIBLE);
                    break;
                case PLMediaPlayer.MEDIA_INFO_BUFFERING_END:
                case PLMediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START:
                    LoadingView.setVisibility(View.GONE);
                    HashMap<String, String> meta = mMediaPlayer.getMetadata();
                    Log.i(TAG, "meta: " + meta.toString());
                    break;
                case PLMediaPlayer.MEDIA_INFO_SWITCHING_SW_DECODE:
                    Log.i(TAG, "Hardware decoding failure, switching software decoding!");
                    break;
                default:
                    break;
            }
            return true;
        }
    };
    private PLMediaPlayer.OnBufferingUpdateListener mOnBufferingUpdateListener = new PLMediaPlayer.OnBufferingUpdateListener() {
        @Override
        public void onBufferingUpdate(PLMediaPlayer mp, int percent) {
//            Log.d(TAG, "onBufferingUpdate: " + percent + "%");
        }
    };

    /**
     * Listen the event of playing complete
     * For playing local file, it's called when reading the file EOF
     * For playing network stream, it's called when the buffered bytes played over
     * <p>
     * If setLooping(true) is called, the player will restart automatically
     * And ｀onCompletion｀ will not be called
     */
    private PLMediaPlayer.OnCompletionListener mOnCompletionListener = new PLMediaPlayer.OnCompletionListener() {
        @Override
        public void onCompletion(PLMediaPlayer mp) {
            Log.e(TAG, "Play Completed !");
//            showToastTips("Play Completed !");
//            finish();
            // TODO: 2017/12/18 播放结束的逻辑交互处理
            isVideoStop = true;
            showControlBar();
            mHandler.removeMessages(SHOW_PROGRESS1);
        }
    };

    @OnClick({R.id.btn_back_to_videoGridview, R.id.btn_stop3_activity, R.id
            .btn_start3_activity, R.id.btn_VideoZoom})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.btn_back_to_videoGridview:
                if (mMediaPlayer != null) {
                    if (mMediaPlayer.isPlaying()) {
                        mMediaPlayer.stop();
                    }
                }
                Intent intent = new Intent();
                intent.putExtra("CurrentTime", CurrentTime);
                setResult(RESULT_OK, intent);
                this.finish();

                break;
            case R.id.btn_stop3_activity:
                mMediaPlayer.pause();
                btnStop.setVisibility(View.INVISIBLE);
                btnStart.setVisibility(View.VISIBLE);
                isVideoStop = true;
                mHandler.removeMessages(SHOW_CONTROLLER1);
                mHandler.sendEmptyMessageDelayed(SHOW_CONTROLLER1, 3000);
                break;

            case R.id.btn_start3_activity:
                mMediaPlayer.start();

                btnStop.setVisibility(View.VISIBLE);
                btnStart.setVisibility(View.INVISIBLE);
                isVideoStop = false;
                mHandler.removeMessages(SHOW_CONTROLLER1);
                mHandler.sendEmptyMessageDelayed(SHOW_CONTROLLER1, 3000);
                mHandler.sendEmptyMessageDelayed(SHOW_PROGRESS1, 500);
                break;
            case R.id.btn_VideoZoom:
                if (mMediaPlayer != null) {
                    if (mMediaPlayer.isPlaying()) {
                        mMediaPlayer.stop();
                    }
                }
                Intent intent1 = new Intent();
                intent1.putExtra("CurrentTime", CurrentTime);
                setResult(RESULT_OK, intent1);
                this.finish();
                break;
            default:
                break;
        }
    }

    public void releaseWithoutStop() {
        if (mMediaPlayer != null) {
            mMediaPlayer.setDisplay(null);
            System.gc();
            Runtime.getRuntime().runFinalization();
            System.gc();
        }
    }
    @Override
    public void onBackPressed() {
        Intent intent = new Intent();
        intent.putExtra("CurrentTime", CurrentTime);
        setResult(RESULT_OK, intent);
        super.onBackPressed();
        this.finish();
    }

    private class MyPhoneListener extends PhoneStateListener {

        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            super.onCallStateChanged(state, incomingNumber);
            switch (state) {
                //空闲状态。
                case TelephonyManager.CALL_STATE_IDLE:
//                    电话挂断继续播放
                    if (isRinging && mMediaPlayer != null) {
//                        失去焦点为啥还可以监听？
                        mMediaPlayer.start();
                        btnStop.setVisibility(View.VISIBLE);
                        btnStart.setVisibility(View.INVISIBLE);
                        isVideoStop = false;
                        mHandler.removeMessages(SHOW_CONTROLLER1);
                        mHandler.sendEmptyMessageDelayed(SHOW_CONTROLLER1, 3000);
//                开始播放再更新进度条
                        mHandler.sendEmptyMessageDelayed(SHOW_PROGRESS1, 500);
                        isRinging = false;
                    }
                    //继续播放音乐
                    Log.v("myService", "空闲状态");
                    break;
                //铃响状态。
                case TelephonyManager.CALL_STATE_RINGING:
                    //暂停播放音乐
                    if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
                        isRinging = true;
                        mMediaPlayer.pause();
                        btnStop.setVisibility(View.INVISIBLE);
                        btnStart.setVisibility(View.VISIBLE);
                        isVideoStop = true;
                        mHandler.removeMessages(SHOW_CONTROLLER1);
                        mHandler.sendEmptyMessageDelayed(SHOW_CONTROLLER1, 3000);
                    }
                    Log.v("myService", "铃响状态");
                    break;
                //通话状态
                case TelephonyManager.CALL_STATE_OFFHOOK:

                    Log.v("myService", "通话状态");
                    break;
                default:
                    break;
            }
        }
    }

}
