package com.byd.vtdr2;


import android.Manifest;
import android.app.BydResourceUtil;
import android.app.DownloadManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.StatFs;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.byd.vtdr2.connectivity.IChannelListener;
import com.byd.vtdr2.connectivity.IFragmentListener;
import com.byd.vtdr2.fragment.FragmentPlaybackList;
import com.byd.vtdr2.fragment.FragmentRTVideo;
import com.byd.vtdr2.fragment.FragmentSetting;
import com.byd.vtdr2.view.CustomDialog;
import com.byd.vtdr2.view.MyDialog;
import com.byd.vtdr2.view.ProgressDialogFragment;
import com.byd.vtdr2.widget.ThemeLightButton;
import com.byd.vtdr2.widget.ThemeLightRadioButton;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import skin.support.annotation.Skinable;
import skin.support.widget.SkinCompatSupportable;

/**
 * @author byd_tw 若要实现所以activity的换肤，把@Skinable放到BaseActivity中
 * 实现（即使用）IChannelListener，即MainActivity有了这个接口功能，若在其他地方获取MainActivity对象，就可以回调该功能。
 */

@Skinable
public class MainActivity extends AppCompatActivity implements IChannelListener, IFragmentListener, SkinCompatSupportable {
    private static final String TAG = "MainActivity";
    private final static String KEY_CONNECTIVITY_TYPE = "connectivity_type";
    @BindView(R.id.fl_main)
    FrameLayout flMain;
    private int mConnectivityType;
    public SharedPreferences mPref;
    public RemoteCam mRemoteCam;

    @BindView(R.id.rb_realTimeVideo)
    ThemeLightRadioButton rbRealTimeVideo;
    @BindView(R.id.rb_playbackList)
    ThemeLightRadioButton rbPlaybackList;
    @BindView(R.id.rb_setting)
    ThemeLightRadioButton rbSetting;
    @BindView(R.id.rg_group)
    RadioGroup rgGroup;
    @BindView(R.id.btn_back)
    ThemeLightButton btnBack;

    private FragmentRTVideo fragmentRTVideo;
    private FragmentPlaybackList fragmentPlaybackList;
    private FragmentSetting fragmentSetting;
    private Fragment currentFragment = new Fragment();
    private FragmentManager fragmentManager;
    private int currentIndex = 0;
    private static final String CURRENT_FRAGMENT = "STATE_FRAGMENT_SHOW";

    private String appStateStr;
    private MyDialog myDialog;
    private ArrayList<Model> selectedLists;
    private static ArrayList<Model> selectedListsA = new ArrayList<Model>();

    private static int selectedCounts;
    private int hadDelete;
    private String mGetFileName;
    private ProgressDialogFragment progressDialogFragment;
    public static final int EXTERNAL_STORAGE_REQ_CODE = 10;
    private CustomDialog customDialog = null;

    //控制弹出框的显示，页面切换网络错误时，弹出一次控制
    public static boolean isDialogShow = false;
    private Toast toast;
    //    private final ScheduledExecutorService worker =
//            Executors.newSingleThreadScheduledExecutor();
//ThreadFactory namedThreadFactory = new ThreadFactoryBuilder()
//        .setNameFormat("demo-pool-%d").build();
    private ScheduledExecutorService worker = Executors.newSingleThreadScheduledExecutor();

    private ScheduledFuture mScheduledTask;
    private boolean isReconnecting;
    private boolean isCardNoExist;
    public static int isSensormessage = 0;
    private boolean isMicOn;
    MyApplication myApplication;
    private static boolean hasCard;
    private int valueEventRecord;
    private boolean isLocking;
    private int valueSdcardInit;
    private int valueRecordInit;
    private boolean isSingleDeleteInPreview;
    /* private UpdateHandler updateHandler = new UpdateHandler(this);*/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        /* LogcatHelper.getInstance(getApplicationContext()).start();*/
        setContentView(R.layout.activity_main);
//        调用系统接口设置主背景图，不要自己去切背景图
        BydResourceUtil.setWindowBackground(this);
        ButterKnife.bind(this);
        initSkinView();
        initView(savedInstanceState);
        myApplication = (MyApplication) this.getApplicationContext();
        requestPermission();
        initConnect();
    }

    /**
     * 初始化皮肤
     */
    private void initSkinView() {
        int bydTheme = getResources().getConfiguration().byd_theme;
        changeSkin(bydTheme);
    }

    /**
     * 初始化视图
     *
     * @param savedInstanceState 页面销毁时被保存的数据
     */
    private void initView(Bundle savedInstanceState) {
        fragmentManager = getSupportFragmentManager();
        if (savedInstanceState == null) {
            fragmentRTVideo = FragmentRTVideo.newInstance();
            changeFragment(fragmentRTVideo);
        } else {
            currentIndex = savedInstanceState.getInt(CURRENT_FRAGMENT, 0);
            switch (currentIndex) {
                case 0:
                    fragmentRTVideo = (FragmentRTVideo) fragmentManager.findFragmentByTag(FragmentRTVideo.class.getName());
                    currentFragment = fragmentRTVideo;
                    break;
                case 1:
                    fragmentPlaybackList = (FragmentPlaybackList) fragmentManager.findFragmentByTag(FragmentPlaybackList.class.getName());
                    currentFragment = fragmentPlaybackList;
                    break;
                case 2:
                    fragmentSetting = (FragmentSetting) fragmentManager.findFragmentByTag(FragmentSetting.class.getName());
                    currentFragment = fragmentSetting;
                    break;
                default:
                    break;
            }
        }
        rgGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                //遍历RadioGroup 里面所有的子控件。
                for (int index = 0; index < radioGroup.getChildCount(); index++) {
                    //获取到指定位置的RadioButton
                    RadioButton rb = (RadioButton) radioGroup.getChildAt(index);
                    //如果被选中
                    if (rb.isChecked()) {
                        setIndexSelected(index);
                        break;
                    }
                }
            }
        });
    }

    /**
     * 管理Fragment事务
     *
     * @param fm
     */
    private void changeFragment(Fragment fm) {
        FragmentManager supportFragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = supportFragmentManager.beginTransaction();
        transaction.replace(flMain.getId(), fm, fm.getClass().getName());
        transaction.commit();
        currentFragment = fm;
    }

    /**
     * radiobutton的点击处理
     *
     * @param index 点击的位置
     */
    private void setIndexSelected(int index) {
        switch (index) {
            case 0:
                if (fragmentRTVideo == null) {
                    fragmentRTVideo = FragmentRTVideo.newInstance();
                }
                changeFragment(fragmentRTVideo);
                currentIndex = 0;
                break;
            case 1:
                if (fragmentPlaybackList == null) {
                    fragmentPlaybackList = FragmentPlaybackList.newInstance();
                }
                changeFragment(fragmentPlaybackList);
                currentIndex = 1;
                break;
            case 2:
                if (fragmentSetting == null) {
                    fragmentSetting = FragmentSetting.newInstance();
                }
                changeFragment(fragmentSetting);
                currentIndex = 2;
                break;
            default:
                break;
        }
    }

    /**
     * 初始化网络连接
     */
    private void initConnect() {
        mPref = getPreferences(MODE_PRIVATE);
        getPrefs(mPref);
        mRemoteCam = myApplication.getRemoteCam();
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        mRemoteCam.setChannelListener(this).setConnectivity(mConnectivityType)
                .setWifiInfo(wifiManager.getConnectionInfo().getSSID().replace("\"", ""), getWifiIpAddress());
        if (!myApplication.isRemoteCreate) {
            mRemoteCam.startSession();
            myApplication.isRemoteCreate = true;
        }
        isDialogShow = false;
        /* worker.scheduleAtFixedRate(new ConnectRunnable(this), 0, 500, TimeUnit.MILLISECONDS);*/
    }

    @Override
    protected void onStart() {
        super.onStart();
//        判断以太网通断
        mScheduledTask = worker.scheduleAtFixedRate(new ConnectRunnable(this), 0, 500, TimeUnit.MILLISECONDS);
    }

    /**
     * 页面旋转重建后，会回调该方法用于恢复数据
     *
     * @param savedInstanceState
     */
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();
//       receiverNetworkBroadcast();
//        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION); //网络连接消息
//        filter.addAction(EthernetManager.ETHERNET_STATE_CHANGED_ACTION); //以太网消息
//        this.registerReceiver(receiver, filter);
    }

    /**
     * ！！！！如下方法调用的前提是：app的包名被加入到系统的白名单里了，否则无法触发换肤
     * 实时改变主题时换肤
     *
     * @param newConfig
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
//        app运行中切换主题时调用此接口更改背景图
        BydResourceUtil.setWindowBackground(this);
        int bydTheme = newConfig.byd_theme;
        changeSkin(bydTheme);
    }

    /**
     * 获取mConnectivityType的type
     *
     * @param preferences
     */
    private void getPrefs(SharedPreferences preferences) {
        mConnectivityType = preferences.getInt(KEY_CONNECTIVITY_TYPE, RemoteCam
                .CAM_CONNECTIVITY_WIFI_WIFI);
//        neverShow = mPref.getBoolean(KEY_NEVER_SHOW, false);
    }

    /**
     * 保存mConnectivityType的初始选值
     *
     * @param preferences
     */
    public void putPrefs(SharedPreferences preferences) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt(KEY_CONNECTIVITY_TYPE, mConnectivityType);
        editor.commit();
    }

    /**
     * 获取IP地址
     *
     * @return
     */
    private String getWifiIpAddress() {
//         如下连接类型为Wi-Fi时使用
       /* int type = NetworkUtils.getANType(getApplicationContext());
        if (type == ConnectivityManager.TYPE_WIFI) {
            WifiManager mgr = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            int ip = mgr.getConnectionInfo().getIpAddress();
            return String.format("%d.%d.%d.%d", (ip & 0xFF), (ip >> 8 & 0xFF), (ip >> 16 & 0xFF), ip
                    >> 24);
        } else if (type == ConnectivityManager.TYPE_ETHERNET) {
//            得到自己的ip
            return ServerConfig.PADIP;
//            return Settings.System.getString(getContentResolver(),Settings.System.);
        }
        return null;*/
        return ServerConfig.PADIP;
    }

    @Override
    protected void onPause() {
        super.onPause();
        // TODO: 2018/2/3 如下的作用不知
//        putPrefs(mPref);
//        此处解注册因为app从后台快速切换回来
//        unregisterReceiver(mBroadcastReceiver);
    }

    /**
     * 页面旋转重建时，会回调该方法用于保存数据
     *
     * @param outState
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(CURRENT_FRAGMENT, currentIndex);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onStop() {
        super.onStop();
//        退到后台取消网络通断判断
        if (mScheduledTask != null) {
            mScheduledTask.cancel(false);
        }
    }

    @Override
    protected void onDestroy() {
        if (customDialog != null && customDialog.isShowing()) {
            customDialog.dismiss();
            customDialog = null;
        }
        super.onDestroy();
        /*LogcatHelper.getInstance(getApplicationContext()).stop();*/

       /* if (worker != null) {
            worker.shutdown();
        }*/
       /* if (mScheduledTask != null) {
            mScheduledTask.cancel(false);
        }*/
//        mRemoteCam.stopSession();
     /*   if (updateHandler != null) {
            updateHandler.removeCallbacksAndMessages(null);
        }
        updateHandler = null;*/
    }

    /**
     * 弹toast窗
     *
     * @param tips
     */
    private void showToastTips(final String tips) {
       /* if (toast == null) {
            toast = Toast.makeText(this, tips, Toast.LENGTH_SHORT);
        } else {
            toast.setText(tips);
        }
        toast.show();*/
//        runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
        if (toast != null) {
            toast.cancel();
        }
        toast = Toast.makeText(this, tips, Toast.LENGTH_SHORT);
        toast.show();
//            }
//        });


    }

    /**
     * 弹一个确认button的窗
     *
     * @param tips
     */
    private void showConfirmDialog(String tips) {
        final String temp = tips;
        if (customDialog != null && !isFinishing()) {
            customDialog.dismiss();
        }
        CustomDialog.Builder builder = new CustomDialog.Builder(this);
        customDialog = builder.cancelTouchOut(false)
                .view(R.layout.fragment_custom_dialog)
                .style(R.style.CustomDialog)
                .setTitle(tips)
                .addViewOnclick(R.id.btn_dialogSure, new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        customDialog.dismiss();
                        isDialogShow = false;
                        if (temp == (getString(R.string.format_finished))) {
                            if (fragmentPlaybackList.mAdapter != null) {
                                fragmentPlaybackList.mAdapter.clear();
                                fragmentPlaybackList.mAdapter.cancelAllTasks();
                            }
                        }
                    }
                })
                .build();
        customDialog.show();
    }


    /**
     * 弹2个确认button的窗
     *
     * @param tips
     */
    private void showDoubleButtonDialog(String tips) {
        if (customDialog != null && !isFinishing()) {
            customDialog.dismiss();
        }
        CustomDialog.Builder builder = new CustomDialog.Builder(this);
        customDialog = builder.cancelTouchOut(false)
                .view(R.layout.fragment_doublebutton_dialog)
                .style(R.style.CustomDialog)
                .setTitle(tips)
                .addViewOnclick(R.id.btn_dialogSure, new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        mRemoteCam.formatSD("C:");
                        customDialog.dismiss();
                        showWaitingDialog(getString(R.string.storage_card_formatting));
                    }
                })
                .addViewOnclick(R.id.btn_dialogCancel, new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        customDialog.dismiss();
                    }
                })
                .build();
        customDialog.show();
    }

    /**
     * 弹等待效果窗
     *
     * @param tips
     */
    private void showWaitingDialog(String tips) {
        if (customDialog != null && !isFinishing()) {
            customDialog.dismiss();
        }
        CustomDialog.Builder builder = new CustomDialog.Builder(this);
        customDialog = builder.cancelTouchOut(false)
                .view(R.layout.fragment_waiting_dialog)
                .style(R.style.CustomDialog)
                .setTitle(tips)
                .build();
        customDialog.show();
    }

    private void showDoubleFormatDialog(String tips) {
        if (customDialog != null && !isFinishing()) {
            customDialog.dismiss();
        }
        CustomDialog.Builder builder = new CustomDialog.Builder(this);
        customDialog = builder.cancelTouchOut(false)
                .view(R.layout.fragment_doublebutton_reformat__dialog)
                .style(R.style.CustomDialog)
                .setTitle(tips)
                .addViewOnclick(R.id.btn_dialogSure, new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        mRemoteCam.formatSD("C:");
                        customDialog.dismiss();
                        showWaitingDialog(getString(R.string.storage_card_formatting));
                    }
                })
                .addViewOnclick(R.id.btn_dialogCancel, new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        customDialog.dismiss();
                    }
                })
                .build();
        customDialog.show();
    }

    /**
     * 弹立即格式化的窗
     *
     * @param tips
     */
    private void showDoubleImmeFormatDialog(String tips) {
        if (customDialog != null && !isFinishing()) {
            customDialog.dismiss();
        }
        CustomDialog.Builder builder = new CustomDialog.Builder(this);
        customDialog = builder.cancelTouchOut(false)
                .view(R.layout.fragment_doublebutton_immeformatdetail_dialog)
                .style(R.style.CustomDialog)
                .setTitle(tips)
                .addViewOnclick(R.id.btn_dialogSure, new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        mRemoteCam.formatSD("C:");
                        customDialog.dismiss();
                        showWaitingDialog(getString(R.string.storage_card_formatting));
                    }
                })
                .addViewOnclick(R.id.btn_dialogCancel, new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        customDialog.dismiss();
                    }
                })
                .build();
        customDialog.show();
    }

    /**
     * 返回view的逻辑
     *
     * @param view
     */
    @OnClick(R.id.btn_back)
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.btn_back:
                onBackPressed();
                break;
            default:
                break;
        }
    }

    /**
     * 返回实体键的逻辑
     */
    @Override
    public void onBackPressed() {
//        如下只有为getSupportFragmentManager时才能弹出
        int count = getSupportFragmentManager().getBackStackEntryCount();
        if (count == 0) {
            if (currentFragment == fragmentPlaybackList && fragmentPlaybackList.isMultiChoose) {
                fragmentPlaybackList.cancelMultiChoose();
            } else if (currentFragment == fragmentPlaybackList && fragmentPlaybackList.fragmentPhotoPreview != null && fragmentPlaybackList.fragmentPhotoPreview.isVisible()) {
                // TODO: 2018/8/13 按后退键逐层退出，在预览旋转的情况下fragmentPhotoPreview为空，不能后退
                fragmentPlaybackList.getChildFragmentManager().popBackStack();
//                在视频预览界面按下后退键，让列表显示出来
                if (fragmentPlaybackList.getConstraintLayout().getVisibility() == View.INVISIBLE) {
                    fragmentPlaybackList.getConstraintLayout().setVisibility(View.VISIBLE);
                }
//                按后退键刷新图片列表
                this.updateCardData();
            } else if (currentFragment == fragmentPlaybackList && fragmentPlaybackList.fragmentVideoPreview != null && fragmentPlaybackList.fragmentVideoPreview.isVisible()) {
//                this.updateCardData();
                fragmentPlaybackList.getChildFragmentManager().popBackStack();
//                在图片预览界面按下后退键，让列表显示出来
                if (fragmentPlaybackList.getConstraintLayout().getVisibility() == View.INVISIBLE) {
                    fragmentPlaybackList.getConstraintLayout().setVisibility(View.VISIBLE);
                }
            } else {
                super.onBackPressed();
                mRemoteCam.stopSession();
                finish();
                Log.e(TAG, "kill the process to force fresh launch next time");
//                Process.killProcess(Process.myPid());
                System.exit(0);
            }
        } else {
            getSupportFragmentManager().popBackStack();
        }
    }

    /**
     * 数据/指令通道的函数回调
     *
     * @param type
     * @param param
     * @param array
     */
    @Override
    public void onChannelEvent(final int type, final Object param, final String... array) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                switch (type & IChannelListener.MSG_MASK) {
                    case IChannelListener.CMD_CHANNEL_MSG:
                        handleCmdChannelEvent(type, param, array);
                        return;
                    case IChannelListener.DATA_CHANNEL_MSG:
                        handleDataChannelEvent(type, param);
                        return;
//                    case IChannelListener.STREAM_CHANNEL_MSG:
//                        handleStreamChannelEvent(type, param);
//                        return;
                    default:
                        break;
                }
            }
        });
    }

    /**
     * 指令通道的函数回调
     *
     * @param type
     * @param param
     * @param array
     */
    private void handleCmdChannelEvent(int type, Object param, String... array) {
        if (type >= 80) {
            handleCmdChannelError(type, param);
            return;
        }

        switch (type) {
//            socket连接异常
            case IChannelListener.CMD_CHANNEL_EVENT_SHOW_ALERT:
                String str = (String) param;
                if ("CONNECT_FAIL".equals(str)) {
                    str = getString(R.string.connect_fail);
                    if (!isDialogShow) {
                        showConfirmDialog(str);
                        isDialogShow = true;
                    }
                } else {
                    // TODO: 2019/2/13 当是其他message时显示出来
//                    showAddSingleButtonDialog(str);
                }
                break;
//                SD卡状态改变
            case IChannelListener.CMD_CHANNEL_EVENT_BYDSDCARD_ALERT:
                int value = (int) param;
                if (valueSdcardInit != value) {
                    valueSdcardInit = value;
                    switch (valueSdcardInit) {
                        case ServerConfig.BYD_CARD_STATE_OK:
                            hasCard = true;
//                                清除无卡的弹窗
                            if (customDialog != null && !isFinishing()) {
                                customDialog.dismiss();
                            }
                            if (currentFragment == fragmentRTVideo) {
                                fragmentRTVideo.showCheckSdCordTag(true);
                                if (!isReconnecting) {
                                    fragmentRTVideo.setImagerAple_SD(false);
                                }
                            } else if (currentFragment == fragmentPlaybackList) {
//                                刷新列表
                                if (fragmentPlaybackList.currentRadioButton == ServerConfig.RB_RECORD_VIDEO) {
                                    fragmentPlaybackList.showRecordList();
                                } else if (fragmentPlaybackList.currentRadioButton == ServerConfig.RB_LOCK_VIDEO) {
                                    fragmentPlaybackList.showLockVideoList();
                                } else {
                                    fragmentPlaybackList.showCapturePhotoList();
                                }
                            } else if (currentFragment == fragmentSetting) {

                            }
                            break;
                        case ServerConfig.BYD_CARD_STATE_NOCARD:
                            hasCard = false;
//                            无卡时统一弹窗
                            showConfirmDialog(getString(R.string.card_removed));
                            if (currentFragment == fragmentRTVideo) {
                                fragmentRTVideo.showCheckSdCordTag(false);
                                fragmentRTVideo.setImagerAple_SD(true);
                            } else if (currentFragment == fragmentPlaybackList) {
//                                刷新列表
                                if (fragmentPlaybackList.currentRadioButton == ServerConfig.RB_RECORD_VIDEO) {
                                    fragmentPlaybackList.showRecordList();
                                } else if (fragmentPlaybackList.currentRadioButton == ServerConfig.RB_LOCK_VIDEO) {
                                    fragmentPlaybackList.showLockVideoList();
                                } else {
                                    fragmentPlaybackList.showCapturePhotoList();
                                }
                            }
                            // TODO: 2018/8/7 如果在设置页如何处理？
                            break;
                        case ServerConfig.BYD_CARD_STATE_SMALL_NAND:
                        case ServerConfig.BYD_CARD_STATE_NOT_MEM:
                        case ServerConfig.BYD_CARD_STATE_SETROOT_FAIL:
                        case ServerConfig.BYD_CARD_STATE_UNINIT:
                            hasCard = true;
                            showConfirmDialog(getString(R.string.card_issue));
                            break;
                        case ServerConfig.BYD_CARD_STATE_NEED_FORMAT:
                            hasCard = true;
                            showDoubleImmeFormatDialog(getString(R.string.card_need_format));
                            fragmentRTVideo.showCheckSdCordState();
                            if (currentFragment == fragmentRTVideo) {
                                fragmentRTVideo.setImagerAple_SD(true);
                            }
                            break;
                        case ServerConfig.BYD_CARD_STATE_NOT_ENOUGH:
                            hasCard = true;
                            showConfirmDialog(getString(R.string.card_not_enough));
                            break;
                        case ServerConfig.BYD_CARD_STATE_WP:
                            hasCard = true;
                            showConfirmDialog(getString(R.string.card_write_protect));
                            break;
                        default:
                            break;
                    }
                }
                break;
//                录像状态实时发生改变时
            case IChannelListener.CMD_CHANNEL_EVENT_BYDRECORD_ALERT:
                int valueRecord = (int) param;
//                if (valueRecordInit != valueRecord) {
//                    valueRecordInit = valueRecord;
//                    switch (valueRecordInit) {
                switch (valueRecord) {
                    case ServerConfig.REC_CAP_STATE_PREVIEW:
                        break;
                    case ServerConfig.REC_CAP_STATE_RECORD:
                        Log.e(TAG, "handleCmdChannelEvent: record:1");
                        if (currentFragment == fragmentRTVideo) {
                                /*if (customDialog != null && !isFinishing()) {
                                    customDialog.dismiss();
                                }*/
                                /*rgGroup.check(R.id.rb_realTimeVideo);
                                fragmentRTVideo = FragmentRTVideo.newInstance();
                                currentFragment = fragmentRTVideo;
                                getSupportFragmentManager().beginTransaction().replace(flMain.getId(), currentFragment).commitAllowingStateLoss();*/
//                                rgGroup.check(R.id.rb_realTimeVideo);
//                                fragmentRTVideo = FragmentRTVideo.newInstance();
//                                currentFragment = fragmentRTVideo;
//                                getSupportFragmentManager().beginTransaction().replace(flMain.getId(), currentFragment,fragmentRTVideo.getClass().getName()).commitAllowingStateLoss();

                            fragmentRTVideo.release();
                            fragmentRTVideo.prepare();
                            /* mRemoteCam.getSystemState();*/
                            fragmentRTVideo.setRecordState(true);
                        } else if (currentFragment == fragmentPlaybackList) {
                               /* if (customDialog != null && !isFinishing()) {
                                    customDialog.dismiss();
                                }
                                *//*fragmentPlaybackList.setRemoteCam(mRemoteCam);*//*
                                if (fragmentPlaybackList.currentRadioButton == ServerConfig.RB_RECORD_VIDEO) {
                                    fragmentPlaybackList.showRecordList();
                                } else if (fragmentPlaybackList.currentRadioButton == ServerConfig.RB_LOCK_VIDEO) {
                                    fragmentPlaybackList.showLockVideoList();
                                } else {
                                    fragmentPlaybackList.showCapturePhotoList();
                                }
                                mRemoteCam.getSystemState();*/
                        } else {
                              /*  if (customDialog != null && !isFinishing()) {
                                    customDialog.dismiss();
                                }
                                mRemoteCam.getSystemState();
//                                    防止格式化时，收到录像状态弹窗被隐藏*/
                        }
//                        }
                        myApplication.setisRescod(true);
                        // TODO: 2018/4/13 先屏蔽
//                        mRemoteCam.appStatus();
                        break;
                    case ServerConfig.REC_CAP_STATE_PRE_RECORD:
                        break;
                    case ServerConfig.REC_CAP_STATE_FOCUS:
                        break;
                    case ServerConfig.REC_CAP_STATE_CAPTURE:
                        break;
                    case ServerConfig.REC_CAP_STATE_VF:
                        Log.e(TAG, "handleCmdChannelEvent: VF:5");
                        if (currentFragment == fragmentRTVideo) {
                                /*if (customDialog != null && !isFinishing()) {
                                    customDialog.dismiss();
                                }*/
                               /* rgGroup.check(R.id.rb_realTimeVideo);
                                fragmentRTVideo = FragmentRTVideo.newInstance();
                                currentFragment = fragmentRTVideo;
                                getSupportFragmentManager().beginTransaction().replace(flMain.getId(), currentFragment).commitAllowingStateLoss();*/
//                                rgGroup.check(R.id.rb_realTimeVideo);
//                                fragmentRTVideo = FragmentRTVideo.newInstance();
//                                currentFragment = fragmentRTVideo;
//                                getSupportFragmentManager().beginTransaction().replace(flMain.getId(), currentFragment,fragmentRTVideo.getClass().getName()).commitAllowingStateLoss();

                            fragmentRTVideo.release();
                            fragmentRTVideo.prepare();
                            fragmentRTVideo.setRecordState(false);
                        } else if (currentFragment == fragmentPlaybackList) {
                                /*if (customDialog != null && !isFinishing()) {
                                    customDialog.dismiss();
                                }
                                *//*   fragmentPlaybackList.setRemoteCam(mRemoteCam);*//*
                                if (fragmentPlaybackList.currentRadioButton == ServerConfig.RB_RECORD_VIDEO) {
                                    fragmentPlaybackList.showRecordList();
                                } else if (fragmentPlaybackList.currentRadioButton == ServerConfig.RB_LOCK_VIDEO) {
                                    fragmentPlaybackList.showLockVideoList();
                                } else {
                                    fragmentPlaybackList.showCapturePhotoList();
                                }
                                mRemoteCam.getSystemState();*/
                        } else {
                   /*             if (customDialog != null && !isFinishing()) {
                                    customDialog.dismiss();
                                }
                                mRemoteCam.getSystemState();
//                                    防止格式化时，收到录像状态弹窗被隐藏*/
                        }
                        myApplication.setisRescod(false);
//
                        break;
                    case ServerConfig.REC_CAP_STATE_TRANSIT_TO_VF:

                        break;
                    case ServerConfig.REC_CAP_STATE_RESET:
                        break;
                    default:
                        break;
                }
//                }

                break;
            case IChannelListener.CMD_CHANNEL_EVENT_BYDSENSOR_ALERT:
                int valueSensor = (int) param;
                switch (valueSensor) {
                    case 0:
                        isSensormessage = 0;//控制对话框弹出一次
//                        if (customDialog != null && !isFinishing()) {
//                            customDialog.dismiss();
//                        }
                        break;
                    case 1:
                        if (currentFragment == fragmentRTVideo && !isDialogShow && isSensormessage == 0) {
                            showConfirmDialog(getString(R.string.sensor_issue));
                            isDialogShow = true;
                            isSensormessage++;
                        }
                        break;
                    default:
                        break;
                }
                break;
//                实时拍照的通知
            case IChannelListener.CMD_CHANNEL_EVENT_BYDPHOTO_ALERT:
                int valuePhoto = (int) param;
                switch (valuePhoto) {
                    case 0:
                        showToastTips(getString(R.string.Pictures_success));
                        break;
                    case -2:
                        showToastTips(getString(R.string.Pictures_fail));
                        break;
                    case -1:
                        showToastTips(getString(R.string.image_max));
                        break;
                    default:
                        break;
                }
                break;
//                实时锁定的通知
            case IChannelListener.CMD_CHANNEL_EVENT_BYDEVENTRECORD_ALERT:
                valueEventRecord = (int) param;
                switch (valueEventRecord) {
                    case 0:
//                        showToastTips(getString(R.string.LockVideo_start));
                        break;
                    case 1:
//                        开始锁定，当锁定指令发送或实时触发锁定时，都会收到该值
//                        如下判断是当收到按下锁定时，直接跳出该判断，不弹toast。
                        if (isLocking) {
                            break;
                        }
                        showToastTips(getString(R.string.LockVideo_start));
                        break;
                    case 2:
                        showToastTips(getString(R.string.LockVideo_end));
                        break;
                    default:
                        break;
                }
                break;
            case IChannelListener.CMD_CHANNEL_EVENT_GET_SPACE:
                fragmentRTVideo.showCheckSdCordTag(false);
                // 如下操作不会闪退
//                showAddSingleButtonDialog(getString(R.string.card_removed));
                showConfirmDialog(getString(R.string.card_removed));
                isCardNoExist = true;
                break;

            case IChannelListener.CMD_CHANNEL_EVENT_START_SESSION:
//                mRemoteCam.getAllSettings();
//                mRemoteCam.appStatus();
//                mRemoteCam.micStatus();
//                mRemoteCam.getSystemState();
//                mRemoteCam.actionQuerySessionHolder();
//                mRemoteCam.getTotalFreeSpace();
//                mRemoteCam.getTotalFileCount();
                break;
//               拍照的指令应答
            case IChannelListener.CMD_CHANNEL_EVENT_TAKE_PHOTO:
                int capturePhotoFlag = (int) param;
                switch (capturePhotoFlag) {
                    case 1:
                        showToastTips(getString(R.string.Pictures_success));
                        break;
                    case -1:
                        showToastTips(getString(R.string.Pictures_fail));
                        break;
                    case -30:
                        showToastTips(getString(R.string.image_max));
                        break;
                    default:
                        break;
                }
                break;
//                锁定的指令应答
            case IChannelListener.CMD_CHANNEL_EVENT_LOCK_VIDEO:
//                收到锁定指令应答
                isLocking = false;
                int isLockVideoFlag = (int) param;
                switch (isLockVideoFlag) {
                    case 0:
//                        锁定成功，延时3s隐藏弹窗
//                        showToastTips(getString(R.string.LockVideo_start));
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                if (customDialog != null && !isFinishing()) {
                                    customDialog.dismiss();
                                }
                            }
                        }, 3000);
                        break;
                    case 1:
//                        正在锁定
                        if (customDialog != null && !isFinishing()) {
                            customDialog.dismiss();
                        }
                        showToastTips(getString(R.string.video_locking));
                        break;
                    case -1:
//                        锁定失败
                        if (customDialog != null && !isFinishing()) {
                            customDialog.dismiss();
                        }
                        showToastTips(getString(R.string.LockVideo_fail));
                        if (fragmentRTVideo != null) {
                            fragmentRTVideo.lastClickTime2 = 0;
                        }
                        break;
                    default:
                        break;
                }
                break;
            case IChannelListener.CMD_CHANNEL_EVENT_FRIMWORK_VERSION:
                String str1 = (String) param;
                if ("null".equals(str1)) {
                    showToastTips(getString(R.string.get_firmware_version_fail));
                } else {
                    showConfirmDialog(getString(R.string.firmware_version) + " " + str1);
                }
                break;
            case IChannelListener.CMD_CHANNEL_EVENT_APP_STATE:
                appStateStr = (String) param;
                if (Objects.equals(appStateStr, "record")) {
                    myApplication.setisRescod(true);
                    fragmentRTVideo.setRecordState(true);
                } else if (Objects.equals(appStateStr, "vf")) {
                    myApplication.setisRescod(false);
                    fragmentRTVideo.setRecordState(false);
                } else if (Objects.equals(appStateStr, "idle")) {
//                    showToastTips(getString(R.string.reboot_drivingReorder));
//                    Toast.makeText(getApplicationContext(), "请重启记录仪！", Toast.LENGTH_LONG).show();
//                    showAddSingleButtonDialog(getString(R.string.reboot_drivingReorder));
                    // TODO: 2018/1/4 如下显示弹窗，旋转就闪退。
                }
                break;
            case IChannelListener.CMD_CHANNEL_EVENT_MIC_STATE:
                // TODO: 2018/6/6 录音如何逻辑
                isMicOn = (boolean) param;
                fragmentRTVideo.setMicState(isMicOn);
//                如下若放开则初始化时会有提示
            /*    if (isMicOn) {
                    showToastTips(getString(R.string.open_voice));
                } else {
                    showToastTips(getString(R.string.close_voice));
                }*/
                break;
            case IChannelListener.CMD_CHANNEL_EVENT_RECORD_TIME:
                // TODO: 2017/12/25
                fragmentRTVideo.updateRecordTime((String) param);
//                seconds = Integer.parseInt((String) param);
//                mHandler.postDelayed(runnable,1000);
//                Timer timer = new Timer();
//                timer.schedule(new RecordTimeTask(), 1000);
                break;
            case IChannelListener.CMD_CHANNEL_EVENT_START_LS:
                // TODO: 2017/12/27 开始发送获取视频的列表，需做刷新或提示
                break;
            case IChannelListener.CMD_CHANNEL_EVENT_LS:
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        fragmentPlaybackList.showLoadView(false);
                    }
                });
                fragmentPlaybackList.updateDirContents((JSONObject) param);
                break;

            case IChannelListener.CMD_CHANNEL_EVENT_GET_THUMB_TEST:
                if ((boolean) param) {
                    fragmentPlaybackList.isYuvDownload = true;
                } else {
                    fragmentPlaybackList.isYuvDownload = false;
                }
                break;

            case IChannelListener.CMD_CHANNEL_EVENT_GET_THUMB_FAIL:
                fragmentPlaybackList.isThumbGetFail = (boolean) param;
                break;

            case IChannelListener.CMD_CHANNEL_EVENT_FORMAT_SD:
                int isFormatSD = (int) param;
                switch (isFormatSD) {
                    case 0:
                        if (customDialog != null && !isFinishing()) {
                            customDialog.dismiss();
                        }
                        showToastTips(getString(R.string.format_finished));
                        if (currentFragment == fragmentRTVideo) {
                            fragmentRTVideo.setImagerAple_SD(false);
                        }
                        break;
                    case -31:
                        if (customDialog != null && !isFinishing()) {
                            customDialog.dismiss();
                        }
                        showToastTips(getString(R.string.video_locking_format_later));
                        break;
                    case -1:
                        if (customDialog != null && !isFinishing()) {
                            customDialog.dismiss();
                        }
                        showToastTips(getString(R.string.format_fail));
                        if (currentFragment == fragmentRTVideo) {
                            fragmentRTVideo.setImagerAple_SD(true);
                        }
                        break;
                    default:
                        break;
                }
                break;
            case IChannelListener.CMD_CHANNEL_EVENT_DEL:
                if (isSingleDeleteInPreview) {
                    isSingleDeleteInPreview = false;
                    showToastTips(getString(R.string.delete_success));
                    break;
                }
                hadDelete++;
                if (hadDelete == selectedCounts) {
                    if (customDialog != null && !isFinishing()) {
                        customDialog.dismiss();
                    }
                    if (fragmentPlaybackList.currentRadioButton == ServerConfig.RB_RECORD_VIDEO) {
                        fragmentPlaybackList.showRecordList();
                    } else if (fragmentPlaybackList.currentRadioButton == ServerConfig.RB_LOCK_VIDEO) {
                        fragmentPlaybackList.showLockVideoList();
                    } else {
                        fragmentPlaybackList.showCapturePhotoList();
                    }
                    hadDelete = 0;
                    showToastTips(getString(R.string.delete_success));
                }
                break;
            case IChannelListener.CMD_CHANNEL_EVENT_DEL_FAIL:
                if (isSingleDeleteInPreview) {
                    isSingleDeleteInPreview = false;
                    showToastTips(getString(R.string.delete_fail));
                    break;
                }
                hadDelete++;
//                当删除失败为最后一个时
                if (hadDelete == selectedCounts) {
                    if (customDialog != null && !isFinishing()) {
                        customDialog.dismiss();
                    }
                    if (fragmentPlaybackList.currentRadioButton == ServerConfig.RB_RECORD_VIDEO) {
                        fragmentPlaybackList.showRecordList();
                    } else if (fragmentPlaybackList.currentRadioButton == ServerConfig.RB_LOCK_VIDEO) {
                        fragmentPlaybackList.showLockVideoList();
                    } else {
                        fragmentPlaybackList.showCapturePhotoList();
                    }
                    hadDelete = 0;
                    showToastTips(getString(R.string.delete_fail));
                }
                break;
            case IChannelListener.CMD_CHANNEL_EVENT_RECORD_START_FAIL:
                boolean isRecordStartFail = (boolean) param;
                if (isRecordStartFail) {
                    showToastTips(getString(R.string.openVideo_fail));
                    fragmentRTVideo.setRecordState(false);
                } else {
                    fragmentRTVideo.setRecordState(true);
                }
                break;
            case IChannelListener.CMD_CHANNEL_EVENT_RECORD_STOP_FAIL:
                boolean isRecordStopFail = (boolean) param;
                if (isRecordStopFail) {
                    showToastTips(getString(R.string.closeVideo_fail));
                    fragmentRTVideo.setRecordState(true);
                } else {
                    fragmentRTVideo.setRecordState(false);
                }
                break;

            case IChannelListener.CMD_CHANNEL_EVENT_WAKEUP_START:
                Log.e(TAG, "handleCmdChannelEvent: Waking up the Remote Camera START");
                break;
            case IChannelListener.CMD_CHANNEL_EVENT_CONNECTED:
                break;
//                以太网连接OK
            case IChannelListener.CMD_CHANNEL_EVENT_WAKEUP_OK:
                if (isReconnecting) {
                    if (customDialog != null && !isFinishing()) {
                        customDialog.dismiss();
                    }
                    WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                    mRemoteCam = new RemoteCam(getApplicationContext());
                    myApplication.setRemoteCam(mRemoteCam);
                    mRemoteCam.setChannelListener(this).setConnectivity(RemoteCam
                            .CAM_CONNECTIVITY_WIFI_WIFI)
                            .setWifiInfo(wifiManager.getConnectionInfo().getSSID().replace("\"", ""), getWifiIpAddress());
                    mRemoteCam.startSession();
                    // TODO: 2018/8/13 只是开启回话能否建立通道？
/*todo
                    fragmentPlaybackList.setRemoteCam(mRemoteCam);
*/
//                    fragmentRTVideo.setRemoteCam(mRemoteCam);
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            rgGroup.check(R.id.rb_realTimeVideo);
                            fragmentRTVideo = FragmentRTVideo.newInstance();
                            currentFragment = fragmentRTVideo;
                            getSupportFragmentManager().beginTransaction().replace(flMain.getId(), currentFragment, fragmentRTVideo.getClass().getName()).commitAllowingStateLoss();
                            // TODO: 2018/8/4 如何配合如上优化下面的代码
//                            fragmentRTVideo.release();
//                            fragmentRTVideo.prepare();
//                            mRemoteCam.getSystemState();
                        }
                    }, 200);
                    if (currentFragment == fragmentRTVideo) {
                        if (hasCard) {
                            fragmentRTVideo.setImagerAple(false);
                        }
                    }
                }
                isReconnecting = false;
                break;
            case IChannelListener.CMD_CHANNEL_EVENT_QUERY_SESSION_HOLDER:
                mRemoteCam.actionQuerySessionHolder();
                break;
            case IChannelListener.CMD_CHANNEL_EVENT_SET_SETTING:
//                fragmentRTVideo.setMicState(isMicOn);
                showToastTips(getString(R.string.voice_settingfail));
                break;
//                录像状态的初始值
            case IChannelListener.CMD_CHANNEL_EVENT_APP_STATE_INIT:
                valueRecordInit = (int) param;
                switch (valueRecordInit) {
                    case ServerConfig.REC_CAP_STATE_PREVIEW:
                        break;
                    case ServerConfig.REC_CAP_STATE_RECORD:
                        myApplication.setisRescod(true);
                        fragmentRTVideo.setRecordState(true);
                        break;
                    case ServerConfig.REC_CAP_STATE_PRE_RECORD:
                        break;
                    case ServerConfig.REC_CAP_STATE_FOCUS:
                        break;
                    case ServerConfig.REC_CAP_STATE_CAPTURE:
                        break;
                    case ServerConfig.REC_CAP_STATE_VF:
                        myApplication.setisRescod(false);
                        fragmentRTVideo.setRecordState(false);
                        break;
                    case ServerConfig.REC_CAP_STATE_TRANSIT_TO_VF:
                        break;
                    case ServerConfig.REC_CAP_STATE_RESET:
                        break;
                    default:
                        break;
                }
                break;
//                SD卡状态的初始值
            case IChannelListener.CMD_CHANNEL_EVENT_SDCARD_STATE_INIT:
                valueSdcardInit = (int) param;
                switch (valueSdcardInit) {
                    case ServerConfig.BYD_CARD_STATE_OK:
                        hasCard = true;
                        if (currentFragment == fragmentRTVideo) {
                            if (!isReconnecting) {
                                fragmentRTVideo.setImagerAple_SD(false);
                            }
                        }
                        break;
                    case ServerConfig.BYD_CARD_STATE_NOCARD:
                        hasCard = false;
                        fragmentRTVideo.showCheckSdCordTag(false);
                        showConfirmDialog(getString(R.string.card_removed));
                        if (currentFragment == fragmentRTVideo) {
                            fragmentRTVideo.setImagerAple_SD(true);
                        }
                        break;
                    case ServerConfig.BYD_CARD_STATE_SMALL_NAND:
                    case ServerConfig.BYD_CARD_STATE_NOT_MEM:
                    case ServerConfig.BYD_CARD_STATE_SETROOT_FAIL:
                    case ServerConfig.BYD_CARD_STATE_UNINIT:
                        hasCard = true;
                        showConfirmDialog(getString(R.string.card_issue));
                        break;
                    case ServerConfig.BYD_CARD_STATE_NEED_FORMAT:
                        hasCard = true;
                        showDoubleImmeFormatDialog(getString(R.string.card_need_format));
                        fragmentRTVideo.showCheckSdCordState();
                        if (currentFragment == fragmentRTVideo) {
                            fragmentRTVideo.setImagerAple_SD(true);
                        }
                        break;
                    case ServerConfig.BYD_CARD_STATE_NOT_ENOUGH:
                        hasCard = true;
                        showConfirmDialog(getString(R.string.card_not_enough));
                        break;
                    case ServerConfig.BYD_CARD_STATE_WP:
                        hasCard = true;
                        showConfirmDialog(getString(R.string.card_write_protect));
                        break;
                    default:
                        break;
                }
                break;
            case IChannelListener.CMD_CHANNEL_EVENT_EVENTRECORD_STATE_INIT:
                valueEventRecord = (int) param;
                switch (valueEventRecord) {
                    case 0:
//                        showToastTips(getString(R.string.LockVideo_success));
                        break;
                    case 1:
//                        showToastTips(getString(R.string.video_locking));
                        break;
                    case 2:
//                        showToastTips(getString(R.string.LockVideo_fail));
                        break;
                    default:
                        break;
                }
                break;
            case IChannelListener.CMD_CHANNEL_EVENT_GET_VIDEO_RESOLUTION_STATE:
                String strResolutoin = (String) param;
                if ("null".equals(strResolutoin)) {
                    showToastTips("获取视频分辨率失败");
                } else {
                    showConfirmDialog("视频分辨率：" + " " + strResolutoin);
                }
                break;
            case IChannelListener.CMD_CHANNEL_EVENT_SET_VIDEO_RESOLUTION_STATE:
                showToastTips("恢复出厂设置成功！");
                break;
            default:
                break;
        }
    }

    /**
     * 数据通道的函数回调
     *
     * @param type
     * @param param
     */
    private void handleDataChannelEvent(int type, Object param) {
        switch (type) {
            case IChannelListener.DATA_CHANNEL_EVENT_GET_START:
                myDialog = MyDialog.newInstance(2, getString(R.string.downloading));
                myDialog.show(getFragmentManager(), "doingDownload");
                myDialog.setOnDialogButtonClickListener(new MyDialog.OnDialogButtonClickListener() {
                    @Override
                    public void okButtonClick() {
                    }

                    @Override
                    public void cancelButtonClick() {
                    }
                });
                break;
            default:
                break;
        }
    }

    /**
     * 指令通道的函数回调错误反馈部分
     *
     * @param type
     * @param param
     */
    private void handleCmdChannelError(int type, Object param) {
        switch (type) {
            case IChannelListener.CMD_CHANNEL_ERROR_INVALID_TOKEN:
//                showToastTips(getString(R.string.invalid_token));
                break;
            case IChannelListener.CMD_CHANNEL_ERROR_TIMEOUT:
                //showToastTips(getString(R.string.time_out));
//                mRemoteCam.stopSession();
//                对应管道破裂
                WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                mRemoteCam = new RemoteCam(getApplicationContext());
                myApplication.setRemoteCam(mRemoteCam);
                mRemoteCam.setChannelListener(this).setConnectivity(RemoteCam
                        .CAM_CONNECTIVITY_WIFI_WIFI)
                        .setWifiInfo(wifiManager.getConnectionInfo().getSSID().replace("\"", ""), getWifiIpAddress());
                mRemoteCam.startSession();
/*
                fragmentPlaybackList.setRemoteCam(mRemoteCam);
*/
//                fragmentRTVideo.setRemoteCam(mRemoteCam);
                break;

            case IChannelListener.CMD_CHANNEL_ERROR_BLE_INVALID_ADDR:
//                showAlertDialog("Error", "Invalid bluetooth device");
                break;
            case IChannelListener.CMD_CHANNEL_ERROR_BLE_DISABLED:
                break;
            case IChannelListener.CMD_CHANNEL_ERROR_BROKEN_CHANNEL:
                break;
            case IChannelListener.CMD_CHANNEL_ERROR_CONNECT:
                break;
//                以太网断开
            case IChannelListener.CMD_CHANNEL_ERROR_WAKEUP:

                // TODO: 2018/4/14 此处socket会断，闪退
                if (!isDialogShow) {
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (isReconnecting) {
                                showConfirmDialog(getString(R.string.connect_fail));
                            }
                        }
                    }, 7000);
                    isDialogShow = true;
                    if (currentFragment == fragmentRTVideo) {
                        fragmentRTVideo.setImagerAple(true);
                    }
                }
                isReconnecting = true;

                Log.e(TAG, "handleCmdChannelEvent: Waking up the Remote Camera ERROR");
                break;
            default:
                break;
        }
    }


    /**
     * activity里Fragment的函数的回调
     *
     * @param type
     * @param param
     * @param array
     */
    @Override
    public void onFragmentAction(int type, Object param, Integer... array) {
        switch (type) {
            case IFragmentListener.ACTION_PHOTO_START:
                mRemoteCam.takePhoto();
                break;
            case IFragmentListener.ACTION_LOCK_VIDEO_START:
                isLocking = true;
                showWaitingDialog(getString(R.string.video_locking));
                mRemoteCam.lockVideo();
                break;
            case IFragmentListener.ACTION_FRIMWORK_VERSION:
                mRemoteCam.frimworkVersion();
                break;
            case IFragmentListener.ACTION_APP_VERSION:
                String ver = getAppVersion(getApplicationContext());
                showConfirmDialog("App" + getString(R.string.version) + " " + ver);
//                checkUpdateThread();
                break;
            case IFragmentListener.ACTION_RECORD_START:
                boolean isRecord = (boolean) param;
                if (isRecord) {
                    mRemoteCam.startRecord();
                } else {
                    mRemoteCam.stopRecord();
                }
                break;
            case IFragmentListener.ACTION_MIC_ON:
//                若使用旧协议使用如下
                isMicOn = (boolean) param;
                if (isMicOn) {
                    mRemoteCam.startMic();
//                    showToastTips(getString(R.string.open_voice));
                } else {
                    mRemoteCam.stopMic();
//                    showToastTips(getString(R.string.close_voice));
                }
//                fragmentRTVideo.setMicState(isMicOn);
                // TODO: 2018/4/25 后续收vil
                break;
            case IFragmentListener.ACTION_RECORD_TIME:
                // TODO: 2018/4/3
//                mRemoteCam.getRecordTime();
                break;
            case IFragmentListener.ACTION_FS_LS:
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        fragmentPlaybackList.showLoadView(true);
                    }
                });
                mRemoteCam.listDir((String) param);
                break;
            case IFragmentListener.ACTION_DEFAULT_SETTING:
                mRemoteCam.defaultSetting();
                break;

            case IFragmentListener.ACTION_FS_FORMAT_SD:
//                if (isCardNoExist) {
                if (!hasCard) {
                    showConfirmDialog(getString(R.string.card_removed));
                } else {
                    showDoubleButtonDialog(getString(R.string.confirm_format_memory_card));
                }
                break;
            case IFragmentListener.ACTION_FS_DELETE_MULTI:
                selectedLists = (ArrayList<Model>) param;
                selectedCounts = selectedLists.size();
                break;
            case IFragmentListener.ACTION_FS_DELETE:
                if (array != null) {
                    if (array[0] == 1) {
                        isSingleDeleteInPreview = true;
                        array = null;
                    }
                }
                mRemoteCam.deleteFile((String) param);
                break;
//                开始下载
            case IFragmentListener.ACTION_FS_DOWNLOAD:
                if (param != null) {

                } else {

                    countsDownload();
                    downloadManager = (DownloadManager) getApplicationContext().getSystemService
                            (Context.DOWNLOAD_SERVICE);
                    ContentObserver mObserver;
                    mObserver = new DownloadChangeObserver(null);
                    getContentResolver().registerContentObserver(CONTENT_URI, true, mObserver);
                    query = new DownloadManager.Query();

                    new MyTheardDownLoad().start();
                }
                break;
            case IFragmentListener.ACTION_FS_DELETE_WAITING_TIP:
                showWaitingDialog(getString(R.string.deleting));
                break;
            case IFragmentListener.ACTION_SET_RESOLUTION:
//                设置视频分辨率，单用此方法无效，需改变录像状态先
                mRemoteCam.setVideoResolution();
                break;
            case IFragmentListener.ACTION_GET_RESOLUTION:
//                获取视频分辨率
                mRemoteCam.getVideoResolution();
                break;
            default:
                break;
        }
    }

    /**
     * 获取app的版本
     *
     * @param context
     * @return
     */
    public String getAppVersion(Context context) {
        try {
            PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
//            return info.versionCode;
            return info.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
     * 在预览页删除完图片或退出预览页时，刷新list。
     */
    public void updateCardData() {
        if (customDialog != null && !isFinishing()) {
            customDialog.dismiss();
        }
//        if (getSupportFragmentManager().isStateSaved()) {
//            showConfirmDialog(getString(R.string.reboot_drivingReorder));
//        } else {
        if (currentFragment == fragmentRTVideo) {
        } else if (currentFragment == fragmentPlaybackList) {
            if (fragmentPlaybackList.currentRadioButton == ServerConfig.RB_RECORD_VIDEO) {
                fragmentPlaybackList.showRecordList();
            } else if (fragmentPlaybackList.currentRadioButton == ServerConfig.RB_LOCK_VIDEO) {
                fragmentPlaybackList.showLockVideoList();
            } else {
                fragmentPlaybackList.showCapturePhotoList();
            }
        }
//        }
    }

    /**
     * 查询下载进度，文件总大小多少，已经下载多少？
     */
    public static final Uri CONTENT_URI = Uri.parse("content://downloads/my_downloads");
    private int newsize = 0, totalsize = 0;
    private static int IDcount = 0;
    //获取下载管理器
    private DownloadManager downloadManager;
    DownloadManager.Query query;


    class DownloadChangeObserver extends ContentObserver {

        public DownloadChangeObserver(Handler handler) {
            super(handler);
            // TODO Auto-generated constructor stub
        }

        @Override
        public void onChange(boolean selfChange) {
            queryDownloadStatus();
        }
    }

    public static double div(double v1, double v2, int scale) {
        if (scale < 0) {
            throw new IllegalArgumentException("The scale must be a positive integer or zero");
        }
        BigDecimal b1 = new BigDecimal(Double.toString(v1));
        BigDecimal b2 = new BigDecimal(Double.toString(v2));
        return b1.divide(b2, scale, BigDecimal.ROUND_HALF_UP).doubleValue();
    }

    private void queryDownloadStatus() {
        try {
            query.setFilterById(IdA);
            Cursor c = downloadManager.query(query);
            if (c != null && c.moveToFirst()) {
                int status = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS));

                int reasonIdx = c.getColumnIndex(DownloadManager.COLUMN_REASON);
                int titleIdx = c.getColumnIndex(DownloadManager.COLUMN_TITLE);
                int fileSizeIdx = c.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES);
                int bytesDLIdx = c.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR);
                String title = c.getString(titleIdx);
                int fileSize = c.getInt(fileSizeIdx);
                int bytesDL = c.getInt(bytesDLIdx);
                newsize = bytesDL;
                totalsize = fileSize;
                // Translate the pause reason to friendly text.
                int reason = c.getInt(reasonIdx);
                StringBuilder sb = new StringBuilder();
                sb.append(title).append("\n");
                sb.append("Downloaded ").append(bytesDL).append(" / ").append(fileSize);

                // Display the status
                Log.d("tag", sb.toString());
                switch (status) {
                    case DownloadManager.STATUS_PAUSED:
                        Log.v("tag", "STATUS_PAUSED");
                        // progressDialogFragment.dismissAllowingStateLoss();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (IDcount != 0) {
                                    //更新UI
                                    Toast.makeText(MainActivity.this, getString(R.string
                                            .Download_fail), Toast.LENGTH_SHORT).show();
                                }
                                showdialogA = false;
                                countsOKdownload = 0;
                                IDcount = 0;
                                downloading = false;
                                downloadManager.remove(IdA);
                            }
                        });
                        break;

                    case DownloadManager.STATUS_PENDING:
                        Log.v("tag", "STATUS_PENDING");
                        break;

                    case DownloadManager.STATUS_RUNNING:
                        // 正在下载，不做任何事情
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                //更新UI
                                double temp = 0;
                                if (totalsize != 0) {
                                    temp = div((double) newsize, (double) totalsize, 3);
                                }
                                if (progressDialogFragment != null) {
                                    progressDialogFragment.setProgressText((int) (temp * 100.00));
                                    progressDialogFragment.setMessageText(getString(R.string
                                            .downloading) + (IDcount) + "/" + countsOKdownload);
                                }
                            }
                        });
                        downloading = true;

                        Log.v("tag", "STATUS_RUNNING");
                        break;
                    case DownloadManager.STATUS_SUCCESSFUL:
                        // 完成
                        Log.v("tag", "下载完成");
                        if (countsOKdownload == IDcount) {
                            progressDialogFragment.dismissAllowingStateLoss();
                            showdialogA = false;
                            countsOKdownload = 0;
                            IDcount = 0;

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    // TODO: 2018/8/17 旋转后会再导出时弹窗
                                    showToastTips(getString(R.string
                                            .Download_completed));
                                }
                            });
                        }
                        downloading = false;


                        break;
                    case DownloadManager.STATUS_FAILED:
                        // 清除已下载的内容，重新下载
                        Log.v("tag", "STATUS_FAILED");
                        countsOKdownload = 0;
                        IDcount = 0;
                        downloading = false;
                        downloadManager.remove(IdA);
                        break;
                    default:
                        break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void readSDCard() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            File sdcardDir = Environment.getExternalStorageDirectory();
            StatFs sf = new StatFs(sdcardDir.getPath());
            //String count2 = Formatter.formatFileSize(getApplicationContext(), sf.getFreeBytes());
            int free = (int) (sf.getFreeBytes() / 1024 / 1024 / 1024);
            if (free < 2) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, R.string.download_free_space, Toast.LENGTH_SHORT).show();

                    }
                });
            }
        }
    }

    private long IdA;
    boolean showdialogA = false;
    boolean downloading = false;
    private int countsOKdownload = 0;

    private void countsDownload() {
        boolean downloadImage = false;
        countsOKdownload = 0;
        IDcount = 0;
        downloading = false;
        selectedListsA.clear();
        readSDCard();

        for (int i = 0; i < selectedCounts; i++) {
            if (fragmentPlaybackList.currentRadioButton == ServerConfig.RB_RECORD_VIDEO) {
                mGetFileName = "http://" + ServerConfig.VTDRIP + "/SD0/NORMAL/" + selectedLists
                        .get(i).getName();

            } else if (fragmentPlaybackList.currentRadioButton == ServerConfig.RB_LOCK_VIDEO) {
                mGetFileName = "http://" + ServerConfig.VTDRIP + "/SD0/EVENT/" + selectedLists
                        .get(i).getName();

            } else {
                mGetFileName = "http://" + ServerConfig.VTDRIP + "/SD0/PHOTO/" + selectedLists
                        .get(i).getName();
                downloadImage = true;
            }
            String fileName = Environment.getExternalStorageDirectory() + "/行车记录仪" + mGetFileName
                    .substring(mGetFileName.lastIndexOf('/'));
            File file = new File(fileName);
            if (!file.exists()) {
                selectedListsA.add(selectedLists.get(i));
                countsOKdownload++;
            }
        }

        if (downloadImage) {
            if (selectedCounts > 9) {
                countsOKdownload = 0;
                IDcount = 0;
                downloading = false;
                selectedListsA.clear();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, R.string.download_num, Toast.LENGTH_SHORT).show();

                    }
                });
                return;
            }
        } else {
            if (selectedCounts > 5) {
                countsOKdownload = 0;
                IDcount = 0;
                downloading = false;
                selectedListsA.clear();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, R.string.download_num_tv, Toast.LENGTH_SHORT).show();

                    }
                });
                return;
            }
        }

        if (countsOKdownload == 0) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(MainActivity.this, R.string.File_downloaded, Toast
                            .LENGTH_SHORT).show();
                }
            });
        }
    }

    /**
     * 下载文件
     */
    private void downloadfilesA() {

        if (countsOKdownload > IDcount) {

            if (fragmentPlaybackList.currentRadioButton == ServerConfig.RB_RECORD_VIDEO) {
                mGetFileName = "http://" + ServerConfig.VTDRIP + "/SD0/NORMAL/" + selectedListsA
                        .get(IDcount).getName();
            } else if (fragmentPlaybackList.currentRadioButton == ServerConfig.RB_LOCK_VIDEO) {
                mGetFileName = "http://" + ServerConfig.VTDRIP + "/SD0/EVENT/" + selectedListsA
                        .get(IDcount).getName();
            } else {
                mGetFileName = "http://" + ServerConfig.VTDRIP + "/SD0/PHOTO/" + selectedListsA
                        .get(IDcount).getName();
            }
            String fileName = Environment.getExternalStorageDirectory() + "/行车记录仪" + mGetFileName
                    .substring(mGetFileName.lastIndexOf('/'));
            //创建下载任务,downloadUrl就是下载链接
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(mGetFileName));
            //指定下载路径和下载文件名
            request.setDestinationInExternalPublicDir("/行车记录仪/", selectedListsA.get(IDcount)
                    .getName());
            //不显示下载界面
            request.setVisibleInDownloadsUi(true);
            //将下载任务加入下载队列，否则不会进行下载
            IdA = downloadManager.enqueue(request);
            IDcount++;//注意关键

            if (!showdialogA) {

                progressDialogFragment = ProgressDialogFragment.newInstance(getString(R.string
                        .downloading) + (IDcount) + "/" + countsOKdownload);

                progressDialogFragment.show(getFragmentManager(), "text");
                progressDialogFragment.setOnDialogButtonClickListener(new ProgressDialogFragment
                        .OnDialogButtonClickListener() {
                    @Override

                    public void okButtonClick() {

                    }

                    @Override
                    public void cancelButtonClick() {
                        downloadManager.remove(IdA);
                        countsOKdownload = 0;
                        downloading = false;
                        showdialogA = false;
                        mRemoteCam.restartHttp();//重置记录仪Http
                    }
                });
                showdialogA = true;
            }
        }
    }

    /**
     * 文件下载的线程
     */
    public class MyTheardDownLoad extends Thread {
        @Override
        public void run() {
            while (countsOKdownload > IDcount) {
                try {
                    if (!downloading) {
                        downloadfilesA();
                    }
                    MyTheardDownLoad.sleep(1000);

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 动态请求app的存储权限
     */
    public void requestPermission() {
        //判断当前Activity是否已经获得了该权限
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            //如果App的权限申请曾经被用户拒绝过，就需要在这里跟用户做出解释
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                showToastTips(getString(R.string.open_permissions));
            } else {
                //进行权限请求
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        EXTERNAL_STORAGE_REQ_CODE);
            }
        }
    }
/*
    private static class UpdateHandler extends Handler {
        private WeakReference<Context> reference;

        UpdateHandler(Context context) {
            reference = new WeakReference<>(context);
        }

        @Override
        public void handleMessage(Message msg) {
            MainActivity mainActivity = (MainActivity) reference.get();
            if (mainActivity != null) {
                switch (msg.what) {
                }
            }
        }
    }*/


    @Override
    public void applySkin() {

    }

    /**
     * 换肤
     * @param bydTheme
     */
    private void changeSkin(int bydTheme) {
        switch (bydTheme) {
            case 1:
                //经济模式
                if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                    btnBack.setCompoundDrawablesRelativeWithIntrinsicBounds(null,
                            getResources().getDrawable(R.drawable.btn_tab_back_selector), null, null);
                    rbRealTimeVideo.setCompoundDrawablesRelativeWithIntrinsicBounds(null,
                            getResources().getDrawable(R.drawable.btn_tab_realtimevideo_selector), null, null);
                    rbPlaybackList.setCompoundDrawablesRelativeWithIntrinsicBounds(null,
                            getResources().getDrawable(R.drawable.btn_tab_playbacklist_selector), null, null);
                    rbSetting.setCompoundDrawablesRelativeWithIntrinsicBounds(null,
                            getResources().getDrawable(R.drawable.btn_tab_setting_selector), null, null);
                } else {
                    btnBack.setCompoundDrawablesRelativeWithIntrinsicBounds(getResources().getDrawable(R.drawable.btn_tab_back_selector),
                            null, null, null);
                    rbRealTimeVideo.setCompoundDrawablesRelativeWithIntrinsicBounds(getResources().getDrawable(R.drawable.btn_tab_realtimevideo_selector),
                            null, null, null);
                    rbPlaybackList.setCompoundDrawablesRelativeWithIntrinsicBounds(getResources().getDrawable(R.drawable.btn_tab_playbacklist_selector),
                            null, null, null);
                    rbSetting.setCompoundDrawablesRelativeWithIntrinsicBounds(getResources().getDrawable(R.drawable.btn_tab_setting_selector),
                            null, null, null);
                }
                break;
            case 2:
                //运动模式
                if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                    btnBack.setCompoundDrawablesRelativeWithIntrinsicBounds(null,
                            getResources().getDrawable(R.drawable.btn_tab_back_selector_sport), null, null);
                    rbRealTimeVideo.setCompoundDrawablesRelativeWithIntrinsicBounds(null,
                            getResources().getDrawable(R.drawable.btn_tab_realtimevideo_selector_sport), null, null);
                    rbPlaybackList.setCompoundDrawablesRelativeWithIntrinsicBounds(null,
                            getResources().getDrawable(R.drawable.btn_tab_playbacklist_selector_sport), null, null);
                    rbSetting.setCompoundDrawablesRelativeWithIntrinsicBounds(null,
                            getResources().getDrawable(R.drawable.btn_tab_setting_selector_sport), null, null);
                } else {
                    btnBack.setCompoundDrawablesRelativeWithIntrinsicBounds(getResources().getDrawable(R.drawable.btn_tab_back_selector_sport),
                            null, null, null);
                    rbRealTimeVideo.setCompoundDrawablesRelativeWithIntrinsicBounds(getResources().getDrawable(R.drawable.btn_tab_realtimevideo_selector_sport),
                            null, null, null);
                    rbPlaybackList.setCompoundDrawablesRelativeWithIntrinsicBounds(getResources().getDrawable(R.drawable.btn_tab_playbacklist_selector_sport),
                            null, null, null);
                    rbSetting.setCompoundDrawablesRelativeWithIntrinsicBounds(getResources().getDrawable(R.drawable.btn_tab_setting_selector_sport),
                            null, null, null);
                }
                break;
            case 101:
                //hadeco模式
                if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                    btnBack.setCompoundDrawablesRelativeWithIntrinsicBounds(null,
                            getResources().getDrawable(R.drawable.btn_tab_back_selector_hadeco), null, null);
                    rbRealTimeVideo.setCompoundDrawablesRelativeWithIntrinsicBounds(null,
                            getResources().getDrawable(R.drawable.btn_tab_realtimevideo_selector_hadeco), null, null);
                    rbPlaybackList.setCompoundDrawablesRelativeWithIntrinsicBounds(null,
                            getResources().getDrawable(R.drawable.btn_tab_playbacklist_selector_hadeco), null, null);
                    rbSetting.setCompoundDrawablesRelativeWithIntrinsicBounds(null,
                            getResources().getDrawable(R.drawable.btn_tab_setting_selector_hadeco), null, null);
                } else {
                    btnBack.setCompoundDrawablesRelativeWithIntrinsicBounds(getResources().getDrawable(R.drawable.btn_tab_back_selector_hadeco),
                            null, null, null);
                    rbRealTimeVideo.setCompoundDrawablesRelativeWithIntrinsicBounds(getResources().getDrawable(R.drawable.btn_tab_realtimevideo_selector_hadeco),
                            null, null, null);
                    rbPlaybackList.setCompoundDrawablesRelativeWithIntrinsicBounds(getResources().getDrawable(R.drawable.btn_tab_playbacklist_selector_hadeco),
                            null, null, null);
                    rbSetting.setCompoundDrawablesRelativeWithIntrinsicBounds(getResources().getDrawable(R.drawable.btn_tab_setting_selector_hadeco),
                            null, null, null);
                }
                break;
            case 102:
                //had运动模式
                if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                    btnBack.setCompoundDrawablesRelativeWithIntrinsicBounds(null,
                            getResources().getDrawable(R.drawable.btn_tab_back_selector_hadsport), null, null);
                    rbRealTimeVideo.setCompoundDrawablesRelativeWithIntrinsicBounds(null,
                            getResources().getDrawable(R.drawable.btn_tab_realtimevideo_selector_hadsport), null, null);
                    rbPlaybackList.setCompoundDrawablesRelativeWithIntrinsicBounds(null,
                            getResources().getDrawable(R.drawable.btn_tab_playbacklist_selector_hadsport), null, null);
                    rbSetting.setCompoundDrawablesRelativeWithIntrinsicBounds(null,
                            getResources().getDrawable(R.drawable.btn_tab_setting_selector_hadsport), null, null);
                } else {
                    btnBack.setCompoundDrawablesRelativeWithIntrinsicBounds(getResources().getDrawable(R.drawable.btn_tab_back_selector_hadsport),
                            null, null, null);
                    rbRealTimeVideo.setCompoundDrawablesRelativeWithIntrinsicBounds(getResources().getDrawable(R.drawable.btn_tab_realtimevideo_selector_hadsport),
                            null, null, null);
                    rbPlaybackList.setCompoundDrawablesRelativeWithIntrinsicBounds(getResources().getDrawable(R.drawable.btn_tab_playbacklist_selector_hadsport),
                            null, null, null);
                    rbSetting.setCompoundDrawablesRelativeWithIntrinsicBounds(getResources().getDrawable(R.drawable.btn_tab_setting_selector_hadsport),
                            null, null, null);
                }
                break;
//            case 1011:
//                //stareco模式
//                if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
//                    btnBack.setCompoundDrawablesRelativeWithIntrinsicBounds(null,
//                            getResources().getDrawable(R.drawable.btn_tab_back_selector), null, null);
//                    rbRealTimeVideo.setCompoundDrawablesRelativeWithIntrinsicBounds(null,
//                            getResources().getDrawable(R.drawable.btn_tab_realtimevideo_selector), null, null);
//                    rbPlaybackList.setCompoundDrawablesRelativeWithIntrinsicBounds(null,
//                            getResources().getDrawable(R.drawable.btn_tab_playbacklist_selector), null, null);
//                    rbSetting.setCompoundDrawablesRelativeWithIntrinsicBounds(null,
//                            getResources().getDrawable(R.drawable.btn_tab_setting_selector), null, null);
//                } else {
//                    btnBack.setCompoundDrawablesRelativeWithIntrinsicBounds(getResources().getDrawable(R.drawable.btn_tab_back_selector),
//                            null, null, null);
//                    rbRealTimeVideo.setCompoundDrawablesRelativeWithIntrinsicBounds(getResources().getDrawable(R.drawable.btn_tab_realtimevideo_selector),
//                            null, null, null);
//                    rbPlaybackList.setCompoundDrawablesRelativeWithIntrinsicBounds(getResources().getDrawable(R.drawable.btn_tab_playbacklist_selector),
//                            null, null, null);
//                    rbSetting.setCompoundDrawablesRelativeWithIntrinsicBounds(getResources().getDrawable(R.drawable.btn_tab_setting_selector),
//                            null, null, null);
//                }
//                break;
//            case 1012:
//                //star运动模式
//                if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
//                    btnBack.setCompoundDrawablesRelativeWithIntrinsicBounds(null,
//                            getResources().getDrawable(R.drawable.btn_tab_back_selector), null, null);
//                    rbRealTimeVideo.setCompoundDrawablesRelativeWithIntrinsicBounds(null,
//                            getResources().getDrawable(R.drawable.btn_tab_realtimevideo_selector), null, null);
//                    rbPlaybackList.setCompoundDrawablesRelativeWithIntrinsicBounds(null,
//                            getResources().getDrawable(R.drawable.btn_tab_playbacklist_selector), null, null);
//                    rbSetting.setCompoundDrawablesRelativeWithIntrinsicBounds(null,
//                            getResources().getDrawable(R.drawable.btn_tab_setting_selector), null, null);
//                } else {
//                    btnBack.setCompoundDrawablesRelativeWithIntrinsicBounds(getResources().getDrawable(R.drawable.btn_tab_back_selector),
//                            null, null, null);
//                    rbRealTimeVideo.setCompoundDrawablesRelativeWithIntrinsicBounds(getResources().getDrawable(R.drawable.btn_tab_realtimevideo_selector),
//                            null, null, null);
//                    rbPlaybackList.setCompoundDrawablesRelativeWithIntrinsicBounds(getResources().getDrawable(R.drawable.btn_tab_playbacklist_selector),
//                            null, null, null);
//                    rbSetting.setCompoundDrawablesRelativeWithIntrinsicBounds(getResources().getDrawable(R.drawable.btn_tab_setting_selector),
//                            null, null, null);
//                }
//                break;
            case 1021:
                //blackgold经济模式
                if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                    btnBack.setCompoundDrawablesRelativeWithIntrinsicBounds(null,
                            getResources().getDrawable(R.drawable.btn_tab_back_selector_blackgoldeco), null, null);
                    rbRealTimeVideo.setCompoundDrawablesRelativeWithIntrinsicBounds(null,
                            getResources().getDrawable(R.drawable.btn_tab_realtimevideo_selector_blackgoldeco), null, null);
                    rbPlaybackList.setCompoundDrawablesRelativeWithIntrinsicBounds(null,
                            getResources().getDrawable(R.drawable.btn_tab_playbacklist_selector_blackgoldeco), null, null);
                    rbSetting.setCompoundDrawablesRelativeWithIntrinsicBounds(null,
                            getResources().getDrawable(R.drawable.btn_tab_setting_selector_blackgoldeco), null, null);
                } else {
                    btnBack.setCompoundDrawablesRelativeWithIntrinsicBounds(getResources().getDrawable(R.drawable.btn_tab_back_selector_blackgoldeco),
                            null, null, null);
                    rbRealTimeVideo.setCompoundDrawablesRelativeWithIntrinsicBounds(getResources().getDrawable(R.drawable.btn_tab_realtimevideo_selector_blackgoldeco),
                            null, null, null);
                    rbPlaybackList.setCompoundDrawablesRelativeWithIntrinsicBounds(getResources().getDrawable(R.drawable.btn_tab_playbacklist_selector_blackgoldeco),
                            null, null, null);
                    rbSetting.setCompoundDrawablesRelativeWithIntrinsicBounds(getResources().getDrawable(R.drawable.btn_tab_setting_selector_blackgoldeco),
                            null, null, null);
                }
                break;
            case 1022:
                //blackgold运动模式
                if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                    btnBack.setCompoundDrawablesRelativeWithIntrinsicBounds(null,
                            getResources().getDrawable(R.drawable.btn_tab_back_selector_blackgoldsport), null, null);
                    rbRealTimeVideo.setCompoundDrawablesRelativeWithIntrinsicBounds(null,
                            getResources().getDrawable(R.drawable.btn_tab_realtimevideo_selector_blackgoldsport), null, null);
                    rbPlaybackList.setCompoundDrawablesRelativeWithIntrinsicBounds(null,
                            getResources().getDrawable(R.drawable.btn_tab_playbacklist_selector_blackgoldsport), null, null);
                    rbSetting.setCompoundDrawablesRelativeWithIntrinsicBounds(null,
                            getResources().getDrawable(R.drawable.btn_tab_setting_selector_blackgoldsport), null, null);
                } else {
                    btnBack.setCompoundDrawablesRelativeWithIntrinsicBounds(getResources().getDrawable(R.drawable.btn_tab_back_selector_blackgoldsport),
                            null, null, null);
                    rbRealTimeVideo.setCompoundDrawablesRelativeWithIntrinsicBounds(getResources().getDrawable(R.drawable.btn_tab_realtimevideo_selector_blackgoldsport),
                            null, null, null);
                    rbPlaybackList.setCompoundDrawablesRelativeWithIntrinsicBounds(getResources().getDrawable(R.drawable.btn_tab_playbacklist_selector_blackgoldsport),
                            null, null, null);
                    rbSetting.setCompoundDrawablesRelativeWithIntrinsicBounds(getResources().getDrawable(R.drawable.btn_tab_setting_selector_blackgoldsport),
                            null, null, null);
                }
                break;
            case 1031:
                //eyeshoteco模式
                if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                    btnBack.setCompoundDrawablesRelativeWithIntrinsicBounds(null,
                            getResources().getDrawable(R.drawable.btn_tab_back_selector_eyeshoteco), null, null);
                    rbRealTimeVideo.setCompoundDrawablesRelativeWithIntrinsicBounds(null,
                            getResources().getDrawable(R.drawable.btn_tab_realtimevideo_selector_eyeshoteco), null, null);
                    rbPlaybackList.setCompoundDrawablesRelativeWithIntrinsicBounds(null,
                            getResources().getDrawable(R.drawable.btn_tab_playbacklist_selector_eyeshoteco), null, null);
                    rbSetting.setCompoundDrawablesRelativeWithIntrinsicBounds(null,
                            getResources().getDrawable(R.drawable.btn_tab_setting_selector_eyeshoteco), null, null);
                } else {
                    btnBack.setCompoundDrawablesRelativeWithIntrinsicBounds(getResources().getDrawable(R.drawable.btn_tab_back_selector_eyeshoteco),
                            null, null, null);
                    rbRealTimeVideo.setCompoundDrawablesRelativeWithIntrinsicBounds(getResources().getDrawable(R.drawable.btn_tab_realtimevideo_selector_eyeshoteco),
                            null, null, null);
                    rbPlaybackList.setCompoundDrawablesRelativeWithIntrinsicBounds(getResources().getDrawable(R.drawable.btn_tab_playbacklist_selector_eyeshoteco),
                            null, null, null);
                    rbSetting.setCompoundDrawablesRelativeWithIntrinsicBounds(getResources().getDrawable(R.drawable.btn_tab_setting_selector_eyeshoteco),
                            null, null, null);
                }
                break;
            case 1032:
                //eyeshotsport运动模式
                if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                    btnBack.setCompoundDrawablesRelativeWithIntrinsicBounds(null,
                            getResources().getDrawable(R.drawable.btn_tab_back_selector_eyeshotsport), null, null);
                    rbRealTimeVideo.setCompoundDrawablesRelativeWithIntrinsicBounds(null,
                            getResources().getDrawable(R.drawable.btn_tab_realtimevideo_selector_eyeshotsport), null, null);
                    rbPlaybackList.setCompoundDrawablesRelativeWithIntrinsicBounds(null,
                            getResources().getDrawable(R.drawable.btn_tab_playbacklist_selector_eyeshotsport), null, null);
                    rbSetting.setCompoundDrawablesRelativeWithIntrinsicBounds(null,
                            getResources().getDrawable(R.drawable.btn_tab_setting_selector_eyeshotsport), null, null);
                } else {
                    btnBack.setCompoundDrawablesRelativeWithIntrinsicBounds(getResources().getDrawable(R.drawable.btn_tab_back_selector_eyeshotsport),
                            null, null, null);
                    rbRealTimeVideo.setCompoundDrawablesRelativeWithIntrinsicBounds(getResources().getDrawable(R.drawable.btn_tab_realtimevideo_selector_eyeshotsport),
                            null, null, null);
                    rbPlaybackList.setCompoundDrawablesRelativeWithIntrinsicBounds(getResources().getDrawable(R.drawable.btn_tab_playbacklist_selector_eyeshotsport),
                            null, null, null);
                    rbSetting.setCompoundDrawablesRelativeWithIntrinsicBounds(getResources().getDrawable(R.drawable.btn_tab_setting_selector_eyeshotsport),
                            null, null, null);
                }
                break;
//            case 1041:
//                //businesseco模式
//                if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
//                    btnBack.setCompoundDrawablesRelativeWithIntrinsicBounds(null,
//                            getResources().getDrawable(R.drawable.btn_tab_back_selector), null, null);
//                    rbRealTimeVideo.setCompoundDrawablesRelativeWithIntrinsicBounds(null,
//                            getResources().getDrawable(R.drawable.btn_tab_realtimevideo_selector), null, null);
//                    rbPlaybackList.setCompoundDrawablesRelativeWithIntrinsicBounds(null,
//                            getResources().getDrawable(R.drawable.btn_tab_playbacklist_selector), null, null);
//                    rbSetting.setCompoundDrawablesRelativeWithIntrinsicBounds(null,
//                            getResources().getDrawable(R.drawable.btn_tab_setting_selector), null, null);
//                } else {
//                    btnBack.setCompoundDrawablesRelativeWithIntrinsicBounds(getResources().getDrawable(R.drawable.btn_tab_back_selector),
//                            null, null, null);
//                    rbRealTimeVideo.setCompoundDrawablesRelativeWithIntrinsicBounds(getResources().getDrawable(R.drawable.btn_tab_realtimevideo_selector),
//                            null, null, null);
//                    rbPlaybackList.setCompoundDrawablesRelativeWithIntrinsicBounds(getResources().getDrawable(R.drawable.btn_tab_playbacklist_selector),
//                            null, null, null);
//                    rbSetting.setCompoundDrawablesRelativeWithIntrinsicBounds(getResources().getDrawable(R.drawable.btn_tab_setting_selector),
//                            null, null, null);
//                }
//                break;
//            case 1042:
//                //businesssport运动模式
//                if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
//                    btnBack.setCompoundDrawablesRelativeWithIntrinsicBounds(null,
//                            getResources().getDrawable(R.drawable.btn_tab_back_selector), null, null);
//                    rbRealTimeVideo.setCompoundDrawablesRelativeWithIntrinsicBounds(null,
//                            getResources().getDrawable(R.drawable.btn_tab_realtimevideo_selector), null, null);
//                    rbPlaybackList.setCompoundDrawablesRelativeWithIntrinsicBounds(null,
//                            getResources().getDrawable(R.drawable.btn_tab_playbacklist_selector), null, null);
//                    rbSetting.setCompoundDrawablesRelativeWithIntrinsicBounds(null,
//                            getResources().getDrawable(R.drawable.btn_tab_setting_selector), null, null);
//                } else {
//                    btnBack.setCompoundDrawablesRelativeWithIntrinsicBounds(getResources().getDrawable(R.drawable.btn_tab_back_selector),
//                            null, null, null);
//                    rbRealTimeVideo.setCompoundDrawablesRelativeWithIntrinsicBounds(getResources().getDrawable(R.drawable.btn_tab_realtimevideo_selector),
//                            null, null, null);
//                    rbPlaybackList.setCompoundDrawablesRelativeWithIntrinsicBounds(getResources().getDrawable(R.drawable.btn_tab_playbacklist_selector),
//                            null, null, null);
//                    rbSetting.setCompoundDrawablesRelativeWithIntrinsicBounds(getResources().getDrawable(R.drawable.btn_tab_setting_selector),
//                            null, null, null);
//                }
//                break;
            default:
                //经济模式
                if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                    btnBack.setCompoundDrawablesRelativeWithIntrinsicBounds(null,
                            getResources().getDrawable(R.drawable.btn_tab_back_selector), null, null);
                    rbRealTimeVideo.setCompoundDrawablesRelativeWithIntrinsicBounds(null,
                            getResources().getDrawable(R.drawable.btn_tab_realtimevideo_selector), null, null);
                    rbPlaybackList.setCompoundDrawablesRelativeWithIntrinsicBounds(null,
                            getResources().getDrawable(R.drawable.btn_tab_playbacklist_selector), null, null);
                    rbSetting.setCompoundDrawablesRelativeWithIntrinsicBounds(null,
                            getResources().getDrawable(R.drawable.btn_tab_setting_selector), null, null);
                } else {
                    btnBack.setCompoundDrawablesRelativeWithIntrinsicBounds(getResources().getDrawable(R.drawable.btn_tab_back_selector),
                            null, null, null);
                    rbRealTimeVideo.setCompoundDrawablesRelativeWithIntrinsicBounds(getResources().getDrawable(R.drawable.btn_tab_realtimevideo_selector),
                            null, null, null);
                    rbPlaybackList.setCompoundDrawablesRelativeWithIntrinsicBounds(getResources().getDrawable(R.drawable.btn_tab_playbacklist_selector),
                            null, null, null);
                    rbSetting.setCompoundDrawablesRelativeWithIntrinsicBounds(getResources().getDrawable(R.drawable.btn_tab_setting_selector),
                            null, null, null);
                }
                break;
        }
    }


    /**
     * 3s检测网络是否正常连接的Runnable
     */
    private static class ConnectRunnable implements Runnable {
        private WeakReference<Context> reference;

        ConnectRunnable(Context context) {
            reference = new WeakReference<>(context);
        }

        @Override
        public void run() {
            MainActivity mainActivity = (MainActivity) reference.get();
//            mainActivity.mRemoteCam.socketTest();
            if (mainActivity != null) {
                mainActivity.isSocketAvailable();
            }
        }
    }

    /**
     * 检测网络的方法
     */
    private void isSocketAvailable() {
        try {
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress(ServerConfig.VTDRIP, ServerConfig.cmdPort), 2000);
            socket.close();
            socket = null;
            this.onChannelEvent(IChannelListener.CMD_CHANNEL_EVENT_WAKEUP_OK, null);
//            Log.e(TAG, "isSocketAvailable: connect to socket ok");
        } catch (IOException e) {
            Log.e(CommonUtility.LOG_TAG, e.getMessage());
            this.onChannelEvent(IChannelListener.CMD_CHANNEL_ERROR_WAKEUP, null);
            Log.e(TAG, "isSocketAvailable: Can't connect to socket");
            String message = "CONNECT_FAIL";
        }
    }

}
