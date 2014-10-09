package com.peter.appmanager;

import java.lang.reflect.Field;

import android.content.Context;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.view.WindowManager;
import android.widget.TextView;

public class FloatWindowSmallView extends TextView {

	private boolean performClick;

	private int statusBarHeight;

	private WindowManager windowManager;

	private WindowManager.LayoutParams mParams;

	private int mPercent;
	
	public boolean mAnim = false;
	
	private int mTouchSlop;
	
	/**
	 * 记录手指按下时在小悬浮窗的View上的横坐标的值
	 */
	private int xInView;

	/**
	 * 记录手指按下时在小悬浮窗的View上的纵坐标的值
	 */
	private int yInView;
	
	public FloatWindowSmallView(final Context context) {
		super(context);
		windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
		setBackgroundResource(R.drawable.float_bg);
		initStatusBarHeight();
		mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
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

		final int x = (int) event.getX();
		final int y = (int) event.getY();
		
		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			performClick = true;
			xInView = x;
			yInView = y;

			break;
		case MotionEvent.ACTION_MOVE:
			
			Log.i("peter", "move-------x= "+ x +", y= " + y);
			
			if(performClick) {
				int deltaX = Math.abs(x - xInView);
				int deltaY = Math.abs(y - yInView);
				if(deltaX > mTouchSlop || deltaY > mTouchSlop) {
					performClick = false;
				}
			}
			
			Log.i("peter", " MotionEvent.ACTION_MOVE" + event.getX());
			
			if(!performClick) {
				float screenX = event.getRawX();
				float screenY = event.getRawY() - statusBarHeight;
				Log.i("peter", "move-------" + y);
				updateViewPosition(screenX, screenY);
				return true;
			}
			
			break;
		case MotionEvent.ACTION_UP:
			savePosition();
			break;
			
		case MotionEvent.ACTION_CANCEL:
			Log.i("peter", " MotionEvent.ACTION_CANCEL");
			break;
		case MotionEvent.ACTION_OUTSIDE:
			Log.i("peter", " MotionEvent.ACTION_OUTSIDE");
			break;
			
		}

		return super.onTouchEvent(event);
	}
	
    public boolean performClick() {
    	if(performClick) {
    		return super.performClick();
    	}
    	return false;
    }
	
	private void savePosition() {
		getContext().getSharedPreferences(AppManager.CONFIG, Context.MODE_PRIVATE).edit()
		.putInt("pos_x", mParams.x).commit();
		getContext().getSharedPreferences(AppManager.CONFIG, Context.MODE_PRIVATE).edit()
		.putInt("pos_y", mParams.y).commit();
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
