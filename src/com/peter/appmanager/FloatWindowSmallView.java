package com.peter.appmanager;

import java.lang.reflect.Field;

import android.content.Context;
import android.os.Vibrator;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

public class FloatWindowSmallView extends LinearLayout {

	/**
	 * 记录小悬浮窗的宽度
	 */
	public static int viewWidth;

	/**
	 * 记录小悬浮窗的高度
	 */
	public static int viewHeight;

	/**
	 * 记录系统状态栏的高度
	 */
	 private static int statusBarHeight;

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
		LayoutInflater.from(context).inflate(R.layout.float_window_small, this);
		View view = findViewById(R.id.small_window_layout);
		viewWidth = view.getLayoutParams().width;
		viewHeight = view.getLayoutParams().height;
		TextView percentView = (TextView) findViewById(R.id.percent);
		percentView.setText(MyWindowManager.getInstance().getUsedPercentValue(context));
		vibrator= (Vibrator)context.getSystemService(Context.VIBRATOR_SERVICE); 
		mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
		
		setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if(canClick) {
					isClicked = true;
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
			}
		});
	}

	boolean isClicked = false;
	boolean canClick = true;
	Vibrator vibrator;
	int mTouchSlop;
	float mStartX;
	float mStartY;
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		
		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			// 手指按下时记录必要数据,纵坐标的值都需要减去状态栏高度
			xInView = event.getX();
			yInView = event.getY();
			mStartX = xInView;
			mStartY = yInView;
			xInScreen = event.getRawX();
			yInScreen = event.getRawY() - getStatusBarHeight();
			isClicked = false;
			canClick = true;
			break;
		case MotionEvent.ACTION_MOVE:
			
			xInScreen = event.getRawX();
			yInScreen = event.getRawY() - getStatusBarHeight();
			
			float deltaX = Math.abs(xInScreen - mStartX);
			float deltaY = Math.abs(yInScreen - mStartY);
			
			if(!isClicked) {
				if(deltaX > mTouchSlop || deltaY > mTouchSlop) {
					canClick = false;
					
					Log.i("peter", "canClick = false");
	//				if(!isLongClick) {
						// 手指移动的时候更新小悬浮窗的位置
						updateViewPosition();
	//				}
				}
			}
			
			break;
		case MotionEvent.ACTION_UP:
			
			break;
			
		default:
			break;
		}
		
		boolean result = super.onTouchEvent(event);
		Log.i("peter", "" + event.getAction() + result);
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
