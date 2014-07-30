package com.peter.appmanager;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.os.AsyncTask;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.WindowManager.LayoutParams;


public class MyWindowManager {
	
	private static MyWindowManager mManager = new MyWindowManager();
	private FloatWindowSmallView mFloatView;
	private WindowManager mWindowManager;

	private MyWindowManager() {}
	
	public static MyWindowManager getInstance() {
		return mManager;
	}
	
	private LayoutParams getParams(Context context) {
		
		LayoutParams params = new LayoutParams();
		params.type = LayoutParams.TYPE_PHONE;
		params.format = PixelFormat.RGBA_8888;
		params.flags = LayoutParams.FLAG_NOT_TOUCH_MODAL
					| LayoutParams.FLAG_NOT_FOCUSABLE;
		params.gravity = Gravity.LEFT | Gravity.TOP;
		params.width = ViewGroup.LayoutParams.WRAP_CONTENT;
		params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
		
		int x = context.getSharedPreferences(AppManager.CONFIG, Context.MODE_PRIVATE).getInt("pos_x", -1);
		int y = context.getSharedPreferences(AppManager.CONFIG, Context.MODE_PRIVATE).getInt("pos_y", -1);
		
		if(x == -1) {
			Point outSize = new Point();
			mWindowManager.getDefaultDisplay().getSize(outSize);
			x = outSize.x;
		}
		
		if(y == -1) {
			Point outSize = new Point();
			mWindowManager.getDefaultDisplay().getSize(outSize);
			y = outSize.y / 2;
		}
		
		params.x = x;
		params.y = y;
		
		return params;
	}
	
	public void createSmallWindow(final Context context) {
		if(mWindowManager == null) {
			mWindowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
		}
		
		if (mFloatView == null) {
			mFloatView = new FloatWindowSmallView(context);
			LayoutParams params = getParams(context);
			mFloatView.setParams(params);
			
			final MyService service = (MyService) context;
			updateUsedPercent(service.getUsedPercentValue(context));
			
			mWindowManager.addView(mFloatView, params);
			mFloatView.setOnClickListener(new OnClickListener() {

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
							mFloatView.startAnim(service.getUsedPercentValue(context) / 3 * 2 , 50);
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
		if (mFloatView != null) {
			mWindowManager.removeView(mFloatView);
			mFloatView = null;
		}
	}
	
	public boolean isWindowShowing() {
		return mFloatView != null;
	}
	
	public void updateUsedPercent(int percent) {
		if (mFloatView != null) {
			if(!mFloatView.mAnim) {
				mFloatView.setTextPercent(percent);
			}
		}
	}

}
