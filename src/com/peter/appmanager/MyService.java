package com.peter.appmanager;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

import com.peter.appmanager.AppAdapter.AppInfo;

import android.app.ActivityManager;
import android.app.ActivityManager.MemoryInfo;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

public class MyService extends Service {

	public static final String TARGET_PACKAGE_NAME = "com.peter.managerplug";
	public static final String TARGET_ACTION = "com.peter.managerplug";

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	private void kill() {
		AppManager application = (AppManager) getApplication();
		List<AppInfo> list = application.getRunningAppInfos(MyService.this,
				AppManager.SHOW);
		String mPackageName = getPackageName();
		for (AppInfo info : list) {
			String packageName = info.packageName;
			if (!packageName.equals(mPackageName)) {
				application.commandLineForceStop(packageName);
			}
		}
		application.commandLineKillAll();
		Toast.makeText(this, "Manager Service invoke kill()!!", Toast.LENGTH_SHORT).show();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Toast.makeText(this, "Manager Service onStartCommand()", Toast.LENGTH_SHORT)
				.show();
		return START_STICKY;
	}

	@Override
	public void onCreate() {
		Toast.makeText(this, "Manager Service onCreate()", Toast.LENGTH_SHORT).show();
		mHandler.sendEmptyMessageDelayed(0, 5000);

		IntentFilter filter = new IntentFilter();
		filter.addAction(Intent.ACTION_SCREEN_OFF);
		registerReceiver(screenOffReceiver, filter);
		
	}

	final BroadcastReceiver screenOffReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(final Context context, final Intent intent) {

			String action = intent.getAction();

			if (Intent.ACTION_SCREEN_OFF.equals(action)) {
				float rate = (float)getAvailMemory() / getTotalMemory();
				Toast.makeText(context, "available memery rate =" + rate,Toast.LENGTH_LONG).show();
				if(rate < 0.3) {
					kill();
				}
			}
		}
	};

	@Override
	public void onDestroy() {
		Toast.makeText(this, "Manager Service onDestroy()", Toast.LENGTH_SHORT).show();
		unregisterReceiver(screenOffReceiver);
		Intent intent = new Intent(getApplicationContext(), MyService.class);
		startService(intent);
	}

	Handler mHandler = new Handler(Looper.getMainLooper()) {
		public void handleMessage(Message msg) {
			AppManager application = (AppManager) getApplication();
			boolean havePlug = application.havePackage(TARGET_ACTION);
			Log.i("peter", "havePlug=" + havePlug);
			if (!havePlug) {
				Toast.makeText(MyService.this, "plug not Running", Toast.LENGTH_SHORT).show();
				startService(new Intent(TARGET_ACTION));
				sendEmptyMessageDelayed(0, 5000);
			}
		};
	};

	private long getTotalMemory() {
		String str1 = "/proc/meminfo";// 系统内存信息文件
		String str2;
		String[] arrayOfString;
		long initial_memory = 0;

		try {
			FileReader localFileReader = new FileReader(str1);
			BufferedReader localBufferedReader = new BufferedReader(
					localFileReader, 8192);
			str2 = localBufferedReader.readLine();// 读取meminfo第一行，系统总内存大小

			arrayOfString = str2.split("\\s+");

			initial_memory = Integer.valueOf(arrayOfString[1]).intValue() * 1024;// 获得系统总内存，单位是KB，乘以1024转换为Byte
			localBufferedReader.close();

		} catch (IOException e) {
		}
//		return Formatter.formatFileSize(getBaseContext(), initial_memory);// Byte转换为KB或者MB，内存大小规格化
		return initial_memory;
	}

	private long getAvailMemory() {// 获取android当前可用内存大小

		ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
		MemoryInfo mi = new MemoryInfo();
		am.getMemoryInfo(mi);
		// mi.availMem; 当前系统的可用内存

//		return Formatter.formatFileSize(getBaseContext(), mi.availMem);// 将获取的内存大小规格化
		return mi.availMem;
	}

}
