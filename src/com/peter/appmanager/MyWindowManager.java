package com.peter.appmanager;

import java.util.ArrayList;
import android.content.Context;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.os.AsyncTask;
import android.os.Vibrator;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.WindowManager.LayoutParams;


public class MyWindowManager {
	
	private static MyWindowManager mManager = new MyWindowManager();
	private FloatWindowSmallView smallWindow;
	private LayoutParams smallWindowParams;

	public static MyWindowManager getInstance() {
		return mManager;
	}
	
	public void createSmallWindow(final Context context) {
		WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
		Point outSize = new Point();
		windowManager.getDefaultDisplay().getSize(outSize);
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
				smallWindowParams.x = outSize.x;
				smallWindowParams.y = 0;
			}
			smallWindow.setParams(smallWindowParams);
			
			final MyService service = (MyService) context;
			updateUsedPercent(service.getUsedPercentValue(context));
			
			windowManager.addView(smallWindow, smallWindowParams);
			smallWindow.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(final View v) {

					new AsyncTask<Void, Integer, ArrayList<String>>() {

						@Override
						protected ArrayList<String> doInBackground(Void... params) {
							
							service.killAll();

							try {
								Thread.sleep(2000);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
							return null;
						}

						@Override
						protected void onPreExecute() {
							smallWindow.startAnim(50, service.getUsedPercentValue(context) / 3 * 2);
						}

						@Override
						protected void onPostExecute(ArrayList<String> packageNames) {
							
						}

					}.execute();
				}
			});
		}
	}
	
	public void removeSmallWindow(Context context) {
		if (smallWindow != null) {
			WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
			windowManager.removeView(smallWindow);
			smallWindow = null;
		}
	}
	
	public boolean isWindowShowing() {
		return smallWindow != null;
	}
	
	public void updateUsedPercent(int percent) {
		if (smallWindow != null) {
			if(!smallWindow.mAnim) {
				smallWindow.setTextPercent(percent);
			}
		}
	}

}
