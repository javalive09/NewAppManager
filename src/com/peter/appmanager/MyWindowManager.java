package com.peter.appmanager;

import java.util.ArrayList;
import android.content.Context;
import android.graphics.PixelFormat;
import android.os.AsyncTask;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.WindowManager.LayoutParams;

public class MyWindowManager {
	
	private static MyWindowManager mManager = new MyWindowManager();
	private FloatWindowSmallView mFloatView;

	private MyWindowManager() {}
	
	public static MyWindowManager instance() {
		return mManager;
	}
	
	private LayoutParams getParams(Context context) {
		LayoutParams params = new WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT, 
		        WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT, 
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, 
                PixelFormat.TRANSLUCENT);
		params.gravity = Gravity.LEFT | Gravity.TOP;
		int cell = context.getResources().getDimensionPixelSize(R.dimen.float_cell);
		params.width = cell;
		params.height = cell;
		
		int x = context.getSharedPreferences(AppManager.CONFIG, Context.MODE_PRIVATE).getInt("pos_x", 0);
		int y = context.getSharedPreferences(AppManager.CONFIG, Context.MODE_PRIVATE).getInt("pos_y", 0);
		
		params.x = x;
		params.y = y;
		
		return params;
	}
	
	public void createSmallWindow(final Context context) {
		WindowManager mWindowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
		
		if (mFloatView == null) {
			mFloatView = new FloatWindowSmallView(context);
			LayoutParams params = getParams(context);
			mFloatView.setParams(params);
			mFloatView.setGravity(Gravity.CENTER);
			int txtSize = context.getResources().getDimensionPixelSize(R.dimen.float_txt_size);
			mFloatView.setTextSize(TypedValue.COMPLEX_UNIT_PX, txtSize);
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
							return null;
						}

						@Override
						protected void onPreExecute() {
							mFloatView.startAnim(service.getUsedPercentValue(context) / 3 * 2 , 10);
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
			WindowManager mWindowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
			mWindowManager.removeViewImmediate(mFloatView);
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
