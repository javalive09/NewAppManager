package com.peter.appmanager;

import java.lang.reflect.Field;

import android.content.Context;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.view.WindowManager;
import android.widget.TextView;

public class FloatWindowSmallView extends TextView {

	private int mTouchSlop;

	private boolean pointInView;

	private int statusBarHeight;

	private WindowManager windowManager;

	private WindowManager.LayoutParams mParams;

	int mPercent;
	
	public boolean mAnim = false;
	
	/**
	 * 记录手指按下时在小悬浮窗的View上的横坐标的值
	 */
	private float xInView;

	/**
	 * 记录手指按下时在小悬浮窗的View上的纵坐标的值
	 */
	private float yInView;

	public FloatWindowSmallView(final Context context) {
		super(context);
		windowManager = (WindowManager) context
				.getSystemService(Context.WINDOW_SERVICE);
		setBackgroundResource(R.drawable.bg_small);
		setGravity(Gravity.CENTER);
		mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
		initStatusBarHeight();
	}

	private boolean pointInView(int x, int y, int slop) {
		return x >= -slop && y >= -slop
				&& x < ((getRight() - getLeft()) + slop)
				&& y < ((getBottom() - getTop()) + slop);
	}
	
	public void setTextPercent(int percent) {
		mPercent = percent;
		setText(percent + "%");
	}
	
	public void startAnim(final int upLimit, final int msec) {
		mAnim = true;
		setEnabled(false);
		subTractPercent(upLimit, msec);
	}
	
	public void subTractPercent(final int upLimit, final int msec) {
		if(mPercent > 0 ) {
			postDelayed(new Runnable(){

				@Override
				public void run() {
					mPercent--;
					setTextPercent(mPercent);
					int time = 0;
					if(msec > 0) {
						time = msec -1;
					}
					subTractPercent(upLimit, time);
				}
				
			}, msec);
		}else if(mPercent == 0) {
			plusPercent(upLimit, 1);
		}
	}
	
	private void plusPercent(final int uplimit, final int msec) {
		if(mPercent < uplimit) {
			postDelayed(new Runnable(){

				@Override
				public void run() {
					mPercent++;
					int time = 100;
					if(msec < 100) {
						time = msec + 1;
					}
					setTextPercent(mPercent);
					plusPercent(uplimit, time);
				}
				
			}, msec);
		}else{
			mAnim = false;
			setEnabled(true);
		}
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {

		super.onTouchEvent(event);

		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			pointInView = true;

			break;
		case MotionEvent.ACTION_MOVE:

			if (pointInView) {
				int x = (int) event.getX();
				int y = (int) event.getY();
				pointInView = pointInView(x, y, mTouchSlop);
				if (!pointInView) {
					xInView = x;
					yInView = y;
				}
			} else {
				float screenX = event.getRawX();
				float screenY = event.getRawY() - statusBarHeight;
				Log.i("peter", "move-------");
				updateViewPosition(screenX, screenY);
			}

			break;
		}

		return true;
	}

	/**
	 * 将小悬浮窗的参数传入，用于更新小悬浮窗的位置。
	 * 
	 * @param params
	 *            小悬浮窗的参数
	 */
	public void setParams(WindowManager.LayoutParams params) {
		mParams = params;
	}

	/**
	 * 更新小悬浮窗在屏幕中的位置。
	 */
	private void updateViewPosition(float screenX, float screenY) {
		mParams.x = (int) (screenX - xInView);
		mParams.y = (int) (screenY - yInView);
		windowManager.updateViewLayout(this, mParams);
	}

	/**
	 * 用于获取状态栏的高度。
	 * 
	 * @return 返回状态栏高度的像素值。
	 */
	private void initStatusBarHeight() {
		if (statusBarHeight == 0) {
			try {
				Class<?> c = Class.forName("com.android.internal.R$dimen");
				Object o = c.newInstance();
				Field field = c.getField("status_bar_height");
				int x = (Integer) field.get(o);
				statusBarHeight = getResources().getDimensionPixelSize(x);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

}
