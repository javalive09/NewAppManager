package com.peter.appmanager;

import java.lang.reflect.Field;

import android.content.Context;
import android.os.Vibrator;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowManager;
import android.widget.TextView;

public class FloatWindowSmallView extends TextView {

	private int mTouchSlop;
	
	private boolean pointInView;
	
	/**
	 * 记录系统状态栏的高度
	 */
	 private int statusBarHeight;

	/**
	 * 用于更新小悬浮窗的位置
	 */
	private WindowManager windowManager;

	/**
	 * 小悬浮窗的参数
	 */
	private WindowManager.LayoutParams mParams;

	/**
	 * 记录当前手指位置在屏幕上的横坐标值
	 */
	private float xInScreen;

	/**
	 * 记录当前手指位置在屏幕上的纵坐标值
	 */
	private float yInScreen;


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
		windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
		setBackgroundResource(R.drawable.bg_small);

		setGravity(Gravity.CENTER);
		setText(MyWindowManager.getInstance().getUsedPercentValue(context));
		final Vibrator vibrator = (Vibrator)context.getSystemService(Context.VIBRATOR_SERVICE); 
		mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
		
		setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				long[] pattern = {800, 50, 400, 30}; // OFF/ON/OFF/ON...   
				vibrator.vibrate(pattern, 2);
				
				MyService service = (MyService) context;
				service.kill();
				
				new Thread(new Runnable() {

					@Override
					public void run() {
						try {
							Thread.sleep(2000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						vibrator.cancel();
					}
					
				}).start();
				
			}
		});
	}
	
	private boolean pointInView(int x, int y, int slop) {
		Log.i("peter", "right="+getRight() + ";left="+getLeft());
		Log.i("peter", "top="+getTop() + ";bottom="+getBottom());
		
		 return x >= -slop && y >= -slop && x < ((getRight() - getLeft()) + slop) &&
	                y < ((getBottom() - getTop()) + slop);
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		
		super.onTouchEvent(event);
		
		int x = (int) event.getX();
		int y = (int) event.getY();
		xInScreen = event.getRawX();
		yInScreen = event.getRawY() - getStatusBarHeight();
		Log.i("peter", "x="+x + ";y="+y);
		
		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			xInView = x;
			yInView = y;
			pointInView = true;
			
			break;
		case MotionEvent.ACTION_MOVE:
		
			if(pointInView) {
				pointInView = pointInView(x, y, mTouchSlop);
			}else {
				Log.i("peter", "move-------");
				updateViewPosition();
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
	private void updateViewPosition() {
		mParams.x = (int) (xInScreen - xInView);
		mParams.y = (int) (yInScreen - yInView);
		windowManager.updateViewLayout(this, mParams);
		
	}

	/**
	 * 用于获取状态栏的高度。
	 * 
	 * @return 返回状态栏高度的像素值。
	 */
	private int getStatusBarHeight() {
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
		return statusBarHeight;
	}

}
