package com.byd.vtdr2;

import android.app.Application;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Environment;
import android.util.Log;

import com.byd.vtdr2.utils.SysProp;
import com.byd.vtdr2.widget.Theme;
import com.byd.vtdr2.widget.ThemeManager;
import com.squareup.leakcanary.RefWatcher;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

import skin.support.SkinCompatManager;
import skin.support.constraint.app.SkinConstraintViewInflater;

/**
 * Created by ximsfei on 2017/1/10.
 */

public class MyApplication extends Application {
    private static final String TAG = "MyApplication";
    private ThemeManager themeManager;
    public boolean isRescod;
    private RemoteCam mRemoteCam;
    public boolean isRemoteCreate;
    private RefWatcher refWatcher;
    private String padVersion;
    private boolean isNewVersion;
    private static MyApplication instance;

    public static MyApplication getInstance() {
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        /*建议在测试阶段建议设置成true，发布时设置为false。*/
       /* CrashReport.initCrashReport(getApplicationContext(), "c7b49bac36", false);*/
//        Bugly.init(getApplicationContext(), "0c07b3a819", false);
//        Thread.setDefaultUncaughtExceptionHandler(new MYExceptionHandler());
//        第三方换肤库，实现非自定义控件换肤
        SkinCompatManager.withoutActivity(this)
//                .addStrategy(new CustomSDCardLoader())          // 自定义加载策略，指定SDCard路径
//                .addInflater(new SkinMaterialViewInflater())    // material design
                .addInflater(new SkinConstraintViewInflater())  // ConstraintLayout
//                .addInflater(new SkinCardViewInflater())        // CardView v7
//                .addInflater(new SkinCircleImageViewInflater()) // hdodenhof/CircleImageView
//                .addInflater(new SkinFlycoTabLayoutInflater())  // H07000223/FlycoTabLayout
//                .setSkinStatusBarColorEnable(false)             // 关闭状态栏换肤
//                .setSkinWindowBackgroundEnable(false)           // 关闭windowBackground换肤
//                .setSkinAllActivityEnable(false)                // true: 默认所有的Activity都换肤; false: 只有实现SkinCompatSupportable接口的Activity换肤
                .loadSkin();
//        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);

        themeManager = ThemeManager.getInstance();
        initView();

        mRemoteCam = new RemoteCam(this);
/*//        内存泄漏检查
        refWatcher = setupLeakCanary();*/
//        获取系统版本
        padVersion = SysProp.get("apps.setting.product.outswver", "");
//        Log.e(TAG, "onCreate: version: 4.2.1806080 ");
        isNewVersion = SysProp.versionCompare(padVersion);
        Log.e(TAG, "onCreate: version: " + isNewVersion);
    }

    /**
     * 初始化换肤
     */
    private void initView() {
        int bydTheme = getResources().getConfiguration().byd_theme;
        changeSkin(bydTheme);
    }

 /*   *//**
     * 内存泄露有关功能
     *
     * @return
     *//*
    private RefWatcher setupLeakCanary() {
        if (LeakCanary.isInAnalyzerProcess(this)) {
            return RefWatcher.DISABLED;
        }
        return LeakCanary.install(this);
    }

    *//**
     * 内存泄露有关功能
     *
     * @param
     * @return
     *//*
    public static RefWatcher getRefWatcher(Context context) {
        MyApplication myApplication = (MyApplication) context.getApplicationContext();
        return myApplication.refWatcher;
    }*/

    public void setisRescod(boolean is) {
        isRescod = is;
    }

    public boolean getisRescod() {
        return isRescod;
    }

    public void setRemoteCam(RemoteCam remoteCam) {
        mRemoteCam = remoteCam;
    }

    public RemoteCam getRemoteCam() {
        return mRemoteCam;
    }

    /*  private RemoteCam setRemoteCam() {
          mRemoteCam = new RemoteCam(this);
          return mRemoteCam;
      }
  */

    public static RemoteCam getRemoteCam(Context context) {
        MyApplication myApplication = (MyApplication) context.getApplicationContext();
        return myApplication.mRemoteCam;
    }

    /**
     * 配置改变-换肤或屏幕旋转都会触发
     *
     * @param newConfig
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        int bydTheme = newConfig.byd_theme;
        changeSkin(bydTheme);
    }

    /**
     * 换肤 改变自定义的Theme控件和非自定义控件。
     *
     * @param bydTheme
     */
    private void changeSkin(int bydTheme) {
        switch (bydTheme) {
            case 1:
                //经济模式
                themeManager.updateTheme(Theme.NORMAL);
                SkinCompatManager.getInstance().restoreDefaultTheme();
                break;
            case 2:
                //运动模式
                themeManager.updateTheme(Theme.SPORT);
                SkinCompatManager.getInstance().loadSkin("sport", null, SkinCompatManager.SKIN_LOADER_STRATEGY_BUILD_IN);
//                SkinCompatManager.getInstance().loadSkin("stsport.skin", SkinCompatManager.SKIN_LOADER_STRATEGY_ASSETS);
                break;
            case 101:
                //经济模式
                themeManager.updateTheme(Theme.HAD_NORMAL);
                SkinCompatManager.getInstance().loadSkin("hadeco", null, SkinCompatManager.SKIN_LOADER_STRATEGY_BUILD_IN);
//                SkinCompatManager.getInstance().loadSkin("hadeco.skin", SkinCompatManager.SKIN_LOADER_STRATEGY_ASSETS);
                break;
            case 102:
                //运动模式
                themeManager.updateTheme(Theme.HAD_SPORT);
                SkinCompatManager.getInstance().loadSkin("hadsport", null, SkinCompatManager.SKIN_LOADER_STRATEGY_BUILD_IN);
//                SkinCompatManager.getInstance().loadSkin("hadsport.skin", SkinCompatManager.SKIN_LOADER_STRATEGY_ASSETS);
                break;
//            case 1011:
//                //star经济模式
//                themeManager.updateTheme(Theme.STAR_NORMAL);
////                SkinCompatManager.getInstance().loadSkin("stareco", null, SkinCompatManager.SKIN_LOADER_STRATEGY_BUILD_IN);
//                SkinCompatManager.getInstance().loadSkin("stareco.skin", SkinCompatManager.SKIN_LOADER_STRATEGY_ASSETS);
//
//                break;
//            case 1012:
//                //star运动模式
//                themeManager.updateTheme(Theme.STAR_SPORT);
////                SkinCompatManager.getInstance().loadSkin("starsport", null, SkinCompatManager.SKIN_LOADER_STRATEGY_BUILD_IN);
//                SkinCompatManager.getInstance().loadSkin("starsport.skin", SkinCompatManager.SKIN_LOADER_STRATEGY_ASSETS);
//                break;
            case 1021:
                //blackgold经济模式
                themeManager.updateTheme(Theme.BLACKGOLD_NORMAL);
                SkinCompatManager.getInstance().loadSkin("blackgoldeco", null, SkinCompatManager.SKIN_LOADER_STRATEGY_BUILD_IN);
//                SkinCompatManager.getInstance().loadSkin("blackgoldeco.skin", SkinCompatManager.SKIN_LOADER_STRATEGY_ASSETS);
                break;
            case 1022:
                //blackgold运动模式
                themeManager.updateTheme(Theme.BLACKGOLD_SPORT);
                SkinCompatManager.getInstance().loadSkin("blackgoldsport", null, SkinCompatManager.SKIN_LOADER_STRATEGY_BUILD_IN);
//                SkinCompatManager.getInstance().loadSkin("blackgoldsport.skin", SkinCompatManager.SKIN_LOADER_STRATEGY_ASSETS);
                break;
            case 1031:
                //eyeshot经济模式
                themeManager.updateTheme(Theme.EYESHOT_NORMAL);
                SkinCompatManager.getInstance().loadSkin("eyeshoteco", null, SkinCompatManager.SKIN_LOADER_STRATEGY_BUILD_IN);
//                SkinCompatManager.getInstance().loadSkin("eyeshoteco.skin", SkinCompatManager.SKIN_LOADER_STRATEGY_ASSETS);

                break;
            case 1032:
                //eyeshot运动模式
                themeManager.updateTheme(Theme.EYESHOT_SPORT);
                SkinCompatManager.getInstance().loadSkin("eyeshotsport", null, SkinCompatManager.SKIN_LOADER_STRATEGY_BUILD_IN);
//                SkinCompatManager.getInstance().loadSkin("eyeshotsport.skin", SkinCompatManager.SKIN_LOADER_STRATEGY_ASSETS);

                break;
//            case 1041:
//                //business经济模式
//                themeManager.updateTheme(Theme.BUSSINESS_NORMAL);
//                SkinCompatManager.getInstance().loadSkin("businesseco.skin", SkinCompatManager.SKIN_LOADER_STRATEGY_ASSETS);
//                break;
//            case 1042:
//                //business运动模式
//                themeManager.updateTheme(Theme.BUSSINESS_SPORT);
//                SkinCompatManager.getInstance().loadSkin("businesssport.skin", SkinCompatManager.SKIN_LOADER_STRATEGY_ASSETS);
//                break;

            default:
                themeManager.updateTheme(Theme.NORMAL);
                SkinCompatManager.getInstance().restoreDefaultTheme();
                break;
        }
    }

    /**
     * 当app出现异常就会走下面的方法！！！
     */
    class MYExceptionHandler implements Thread.UncaughtExceptionHandler {

        @Override
        public void uncaughtException(Thread thread, Throwable throwable) {
            if (throwable != null) {
                final Writer result = new StringWriter();
                final PrintWriter printWriter = new PrintWriter(result);
                throwable.printStackTrace(printWriter);
                String errorReport = result.toString();
                Log.d("-<<<<>", errorReport);

                //这里吧errorReport post到http去就行了！！！
                //...

                //或者保存文件到sd卡中
                saveInFile(errorReport);

                //最后杀死自己             android.os.Process.killProcess(android.os.Process.myPid());

            }
        }
    }

    private void saveInFile(String errorReport) {
        FileOutputStream out = null;

        File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "CarDV-Log", "error-log.txt");
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            try {
                out = new FileOutputStream(file);
                out.write(errorReport.getBytes());
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (out != null) {
                    try {
                        out.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }


}
