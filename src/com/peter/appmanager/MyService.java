package com.peter.appmanager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import com.peter.appmanager.AppAdapter.AppInfo;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Binder;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

public class MyService extends Service {

    public static final String TARGET_ACTION = "com.peter.managerplug";
    public static final String ACTION = "com.peter.appmanager";
    private static boolean isRollingStart = false;
    private List<String> launcherList = new ArrayList<String>();
    private static final int HEART_BEAT = 1;
    private static final String APK_PATH = "NewAppManagerPlug.apk";

    @Override
    public IBinder onBind(Intent intent) {
        return new MyBinder();
    }

    public class MyBinder extends Binder {
        public MyService getService() {
            return MyService.this;
        }
    }

    public void unregisterReceiver() {
        unregisterReceiver(screenReceiver);
    }

    public void registerReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        registerReceiver(screenReceiver, filter);
    }

    /**
     * 每次都会调用,用于刷新悬浮窗
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        showFloatView();
        Log.i("peter", "=============");
        return START_STICKY;
    }

    private ArrayList<String> getKillList() {
        ArrayList<String> killList = new ArrayList<String>();
        AppManager application = (AppManager) getApplicationContext();
        List<AppInfo> list = application.getRunningAppInfos(MyService.this, AppManager.SHOW);
        String mPackageName = getPackageName();
        for (AppInfo info : list) {
            String packageName = info.packageName;
            if (!packageName.equals(mPackageName)) {
                killList.add(packageName);
            }
        }
        return killList;
    }

    public void killAll() {
        kill(getKillList());
    }

    public void kill(ArrayList<String> list) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (String packageName : list) {
            manager.restartPackage(packageName);
            commandLineForceStop(packageName);
        }
        commandLineKillAll();
    }

    private void commandLineForceStop(String packageName) {
        List<String> commands = new ArrayList<String>();
        commands.add("su");
        commands.add("|");
        commands.add("am");
        commands.add("force-stop");
        commands.add(packageName);
        ProcessBuilder pb = new ProcessBuilder(commands);
        try {
            pb.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void commandLineInstallApk(String path) {
        List<String> commands = new ArrayList<String>();
        commands.add("su");
        commands.add("|");
        commands.add("pm");
        commands.add("install");
        commands.add("-r");
        Log.i("peter", path);
        commands.add(path);
        ProcessBuilder pb = new ProcessBuilder(commands);
        try {
            pb.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public File saveToSDCard(File file) {

        FileOutputStream fos = null;
        InputStream is = null;

        try {
            is = getResources().getAssets().open("NewAppManagerPlug.apk");

            fos = new FileOutputStream(file);

            byte[] buffer = new byte[4096];

            int byteCount = 0;
            while ((byteCount = is.read(buffer)) != -1) {
                fos.write(buffer, 0, byteCount);
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
                if (fos != null) {
                    fos.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return file;

    }

    private void commandLineKillAll() {
        List<String> commands = new ArrayList<String>();
        commands.add("su");
        commands.add("|");
        commands.add("am");
        commands.add("kill-all");
        ProcessBuilder pb = new ProcessBuilder(commands);
        try {
            pb.start();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onCreate() {
        if (!isRollingStart) {
            isRollingStart = true;
            startPollingService(MyService.this, HEART_BEAT, MyService.class, MyService.ACTION);
        }
        Toast.makeText(this, "Manager Service onCreate()", Toast.LENGTH_SHORT).show();
        mHandler.sendEmptyMessageDelayed(0, 5000);
        registerReceiver();
    }

    //开始轮询服务
    public void startPollingService(Context context, int seconds, Class<?> cls, String action) {
        //获取AlarmManager系统服务
        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        //包装需要执行Service的Intent
        Intent intent = new Intent(context, cls);
        intent.setAction(action);
        PendingIntent pendingIntent = PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        //触发服务的起始时间
        long triggerAtTime = SystemClock.elapsedRealtime();

        //使用AlarmManger的setRepeating方法设置定期执行的时间间隔（seconds秒）和需要执行的Service
        manager.setRepeating(AlarmManager.ELAPSED_REALTIME, triggerAtTime, seconds * 1000, pendingIntent);
    }

    //停止轮询服务
    public void stopPollingService(Context context, Class<?> cls, String action) {
        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, cls);
        intent.setAction(action);
        PendingIntent pendingIntent = PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        //取消正在执行的服务
        manager.cancel(pendingIntent);
    }

    public void showFloatView() {
        mFloatViewHandler.sendEmptyMessage(0);
    }

    final BroadcastReceiver screenReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {

            if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
                final String screenoff = getResources().getString(R.string.screenoff_setting);
                boolean isScreenOff = getSharedPreferences(AppManager.CLEAN_METHOD, MODE_PRIVATE).getBoolean(screenoff,
                        false);
                if (isScreenOff) {
                    int percent = getUsedPercentValue(context);
                    Toast.makeText(context, "available memery rate =" + percent + "%", Toast.LENGTH_LONG).show();
                    if (percent > 80) {
                        killAll();
                    }
                }

                if (isRollingStart) {
                    isRollingStart = false;
                    stopPollingService(MyService.this, MyService.class, MyService.ACTION);
                }
            } else if (Intent.ACTION_SCREEN_ON.equals(intent.getAction())) {
                if (!isRollingStart) {
                    isRollingStart = true;
                    startPollingService(MyService.this, HEART_BEAT, MyService.class, MyService.ACTION);
                }
            }
        }
    };

    @Override
    public void onDestroy() {
        Toast.makeText(this, "Manager Service onDestroy()", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(getApplicationContext(), MyService.class);
        startService(intent);
        unregisterReceiver();
    }

    Handler mHandler = new Handler(Looper.getMainLooper()) {
        public void handleMessage(Message msg) {
            AppManager application = (AppManager) getApplication();
            boolean havePlug = application.havePackage(TARGET_ACTION);
            Log.i("peter", "havePlug=" + havePlug);
            if (!havePlug) {
                Toast.makeText(MyService.this, "plug not Running", Toast.LENGTH_SHORT).show();
                File file = new File(Environment.getExternalStorageDirectory(), "NewAppManagerPlug.apk");
                if(!file.exists()) {
                    saveToSDCard(file);
                    commandLineInstallApk(file.getAbsolutePath());
                }
                sendEmptyMessageDelayed(0, 5000);
            } else {
                startService(new Intent(TARGET_ACTION));
            }
        };
    };

    Handler mFloatViewHandler = new Handler(Looper.getMainLooper()) {
        public void handleMessage(Message msg) {
            final String screenoff = getResources().getString(R.string.screenoff_setting);
            boolean isScreenOff = getSharedPreferences(AppManager.CLEAN_METHOD, MODE_PRIVATE).getBoolean(screenoff,
                    false);

            if (!isScreenOff) {
                floatViewAction();
            }

        };
    };

    private void floatViewAction() {
        // 当前界面是桌面，且没有悬浮窗显示，则创建悬浮窗。
        if (isHome() && !MyWindowManager.instance().isWindowShowing()) {
            MyWindowManager.instance().createSmallWindow(MyService.this);
        }
        // 当前界面不是桌面，且有悬浮窗显示，则移除悬浮窗。
        else if (!isHome() && MyWindowManager.instance().isWindowShowing()) {
            MyWindowManager.instance().removeSmallWindow(MyService.this);
        }
        // 当前界面是桌面，且有悬浮窗显示，则更新内存数据。
        else if (isHome() && MyWindowManager.instance().isWindowShowing()) {
            MyWindowManager.instance().updateUsedPercent(getUsedPercentValue(MyService.this));
        }
    }

    public int getUsedPercentValue(Context context) {
        String dir = "/proc/meminfo";
        try {
            FileReader fr = new FileReader(dir);
            BufferedReader br = new BufferedReader(fr, 2048);
            String memoryLine = br.readLine();
            String subMemoryLine = memoryLine.substring(memoryLine.indexOf("MemTotal:"));
            br.close();
            long totalMemorySize = Integer.parseInt(subMemoryLine.replaceAll("\\D+", ""));
            long availableSize = getAvailableMemory(context) / 1024;
            int percent = (int) ((totalMemorySize - availableSize) / (float) totalMemorySize * 100);
            return percent;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return -1;
    }

    private long getAvailableMemory(Context context) {
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        ActivityManager mActivityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        mActivityManager.getMemoryInfo(mi);
        return mi.availMem;
    }

    /**
     * 判断当前界面是否是桌面
     */
    private boolean isHome() {
        ActivityManager mActivityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        List<RunningTaskInfo> rti = mActivityManager.getRunningTasks(1);
        return getHomes().contains(rti.get(0).topActivity.getPackageName());
    }

    /**
     * 获得属于桌面的应用的应用包名称
     * 
     * @return 返回包含所有包名的字符串列表
     */
    private List<String> getHomes() {
        if (launcherList.size() == 0) {
            PackageManager packageManager = this.getPackageManager();
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_HOME);
            List<ResolveInfo> resolveInfo = packageManager.queryIntentActivities(intent,
                    PackageManager.MATCH_DEFAULT_ONLY);
            for (ResolveInfo ri : resolveInfo) {
                launcherList.add(ri.activityInfo.packageName);
            }
        }
        return launcherList;
    }

}
