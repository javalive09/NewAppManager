package com.peter.appmanager;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.PixelFormat;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;


public class MyWindowManager {
	
	private static MyWindowManager mManager = new MyWindowManager();
	private WindowManager mWindowManager;
	private FloatWindowSmallView smallWindow;
	private LayoutParams smallWindowParams;
	private ActivityManager mActivityManager;

	public static MyWindowManager getInstance() {
		return mManager;
	}
	
	private WindowManager getWindowManager(Context context) {
		if (mWindowManager == null) {
			mWindowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
		}
		return mWindowManager;
	}
	
	@SuppressWarnings("deprecation")
	public void createSmallWindow(Context context) {
		WindowManager windowManager = getWindowManager(context);
		int screenWidth = windowManager.getDefaultDisplay().getWidth();
		if (smallWindow == null) {
			smallWindow = new FloatWindowSmallView(context);
			if (smallWindowParams == null) {
				smallWindowParams = new LayoutParams();
				smallWindowParams.type = LayoutParams.TYPE_PHONE;
				smallWindowParams.format = PixelFormat.RGBA_8888;
				smallWindowParams.flags = LayoutParams.FLAG_NOT_TOUCH_MODAL
						| LayoutParams.FLAG_NOT_FOCUSABLE;
				smallWindowParams.gravity = Gravity.LEFT | Gravity.TOP;
				smallWindowParams.width = ViewGroup.LayoutParams.WRAP_CONTENT;
				smallWindowParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
				smallWindowParams.x = screenWidth;
				smallWindowParams.y = 0;
			}
			smallWindow.setParams(smallWindowParams);
			windowManager.addView(smallWindow, smallWindowParams);
		}
	}
	
	public void removeSmallWindow(Context context) {
		if (smallWindow != null) {
			WindowManager windowManager = getWindowManager(context);
			windowManager.removeView(smallWindow);
			smallWindow = null;
		}
	}
	
	public boolean isWindowShowing() {
		return smallWindow != null;
	}
	
	public void updateUsedPercent(Context context) {
		if (smallWindow != null) {
			smallWindow.setText(getUsedPercentValue(context));
		}
	}
	
	public String getUsedPercentValue(Context context) {
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
			return percent + "%";
		} catch (IOException e) {
			e.printStackTrace();
		}
		return "悬浮窗";
	}
	
	private long getAvailableMemory(Context context) {
		ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
		getActivityManager(context).getMemoryInfo(mi);
		return mi.availMem;
	}
	
	private ActivityManager getActivityManager(Context context) {
		if (mActivityManager == null) {
			mActivityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
		}
		return mActivityManager;
	}

}
