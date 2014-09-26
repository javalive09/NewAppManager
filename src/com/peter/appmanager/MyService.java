package com.peter.appmanager;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.peter.appmanager.AppAdapter.AppInfo;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

public class MyService extends Service {

	public static final String TARGET_PACKAGE_NAME = "com.peter.managerplug";
	public static final String TARGET_ACTION = "com.peter.managerplug";
	public static final int FLOATVIEW_HEART_BEAT = 500;
	public static final int CHECKPLUG_HEART_BEAT = 5000;

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
		unregisterReceiver(screenOffReceiver);
	}

	public void registerReceiver() {
		IntentFilter filter = new IntentFilter();
		filter.addAction(Intent.ACTION_SCREEN_OFF);
		registerReceiver(screenOffReceiver, filter);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Toast.makeText(this, "Manager Service onStartCommand()",
				Toast.LENGTH_SHORT).show();
		return START_STICKY;
	}

	private ArrayList<String> getKillList() {
		ArrayList<String> killList = new ArrayList<String>();
		AppManager application = (AppManager) getApplicationContext();
		List<AppInfo> list = application.getRunningAppInfos(MyService.this,
				AppManager.SHOW);
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
			manager.killBackgroundProcesses(packageName);
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
		Toast.makeText(this, "Manager Service onCreate()", Toast.LENGTH_SHORT).show();
		mHandler.sendEmptyMessageDelayed(0, CHECKPLUG_HEART_BEAT);
		showFloatView();

		final String screenoff = getResources().getString(
				R.string.screenoff_setting);
		boolean isScreenOff = getSharedPreferences(AppManager.CLEAN_METHOD,
				MODE_PRIVATE).getBoolean(screenoff, false);
		if (isScreenOff) {
			registerReceiver();
		}

	}

	public void showFloatView() {
		mFloatViewHandler.sendEmptyMessageDelayed(0, FLOATVIEW_HEART_BEAT);
	}

	final BroadcastReceiver screenOffReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(final Context context, final Intent intent) {

			String action = intent.getAction();

			if (Intent.ACTION_SCREEN_OFF.equals(action)) {
				int percent = getUsedPercentValue(context);
				Toast.makeText(context, "available memery rate =" + percent + "%", Toast.LENGTH_LONG).show();
				if (percent > 80) {
					killAll();
				}
			}
		}
	};

	@Override
	public void onDestroy() {
		Toast.makeText(this, "Manager Service onDestroy()", Toast.LENGTH_SHORT).show();
		unregisterReceiver();
		Intent intent = new Intent(getApplicationContext(), MyService.class);
		startService(intent);
	}

	Handler mHandler = new Handler(Looper.getMainLooper()) {
		public void handleMessage(Message msg) {
			AppManager application = (AppManager) getApplication();
			boolean havePlug = application.havePackage(TARGET_ACTION);
			Log.i("peter", "havePlug=" + havePlug);
			if (!havePlug) {
				Toast.makeText(MyService.this, "plug not Running",
						Toast.LENGTH_SHORT).show();
				sendEmptyMessageDelayed(0, CHECKPLUG_HEART_BEAT);
			} else {
				startService(new Intent(TARGET_ACTION));
			}
		};
	};

	Handler mFloatViewHandler = new Handler(Looper.getMainLooper()) {
		public void handleMessage(Message msg) {
			final String screenoff = getResources().getString(R.string.screenoff_setting);
			boolean isScreenOff = getSharedPreferences(AppManager.CLEAN_METHOD,
					MODE_PRIVATE).getBoolean(screenoff, false);

			if (!isScreenOff) {
				floatViewAction();
				sendEmptyMessageDelayed(0, FLOATVIEW_HEART_BEAT);
			}

		};
	};

	private void floatViewAction() {
		// 当前界面是桌面，且没有悬浮窗显示，则创建悬浮窗。
		if (isHome() && !MyWindowManager.getInstance().isWindowShowing()) {
			MyWindowManager.getInstance().createSmallWindow(MyService.this);
		}
		// 当前界面不是桌面，且有悬浮窗显示，则移除悬浮窗。
		else if (!isHome() && MyWindowManager.getInstance().isWindowShowing()) {
			MyWindowManager.getInstance().removeSmallWindow(MyService.this);
		}
		// 当前界面是桌面，且有悬浮窗显示，则更新内存数据。
		else if (isHome() && MyWindowManager.getInstance().isWindowShowing()) {
			MyWindowManager.getInstance().updateUsedPercent(getUsedPercentValue(MyService.this));
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
			return percent ;
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
		List<String> names = new ArrayList<String>();
		PackageManager packageManager = this.getPackageManager();
		Intent intent = new Intent(Intent.ACTION_MAIN);
		intent.addCategory(Intent.CATEGORY_HOME);
		List<ResolveInfo> resolveInfo = packageManager.queryIntentActivities(
				intent, PackageManager.MATCH_DEFAULT_ONLY);
		for (ResolveInfo ri : resolveInfo) {
			names.add(ri.activityInfo.packageName);
		}
		return names;
	}


}
