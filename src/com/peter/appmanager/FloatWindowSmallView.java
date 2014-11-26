package com.peter.appmanager;

import android.content.Context;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.view.WindowManager;
import android.widget.TextView;

public class FloatWindowSmallView extends TextView {

    private boolean performClick;

    private WindowManager.LayoutParams mParams;
    
    private WindowManager mWindowManager;

    private int mPercent;

    public boolean mAnim = false;

    private int mTouchSlop;

    private int mStartX;

    private int mStartY;

    public FloatWindowSmallView(final Context context) {
        super(context);
        setBackgroundResource(R.drawable.float_bg);
        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        mWindowManager = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
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
        if (mPercent > 0) {
            postDelayed(new Runnable() {

                @Override
                public void run() {
                    mPercent--;
                    setTextPercent(mPercent);
                    int time = 0;
                    if (msec > 0) {
                        time = msec - 1;
                    }
                    subTractPercent(upLimit, time);
                }

            }, msec);
        } else if (mPercent == 0) {
            plusPercent(upLimit, 1);
        }
    }

    private void plusPercent(final int uplimit, final int msec) {
        if (mPercent < uplimit) {
            postDelayed(new Runnable() {

                @Override
                public void run() {
                    mPercent++;
                    int time = 100;
                    if (msec < 100) {
                        time = msec + 1;
                    }
                    setTextPercent(mPercent);
                    plusPercent(uplimit, time);
                }

            }, msec);
        } else {
            mAnim = false;
            setEnabled(true);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        final int x = (int) event.getRawX();
        final int y = (int) event.getRawY();
        Log.i("peter", "width = " + mParams.width);
        Log.i("peter", "height = " + mParams.height);
        switch (event.getAction()) {
        case MotionEvent.ACTION_DOWN:
            performClick = true;
            mStartX = x;
            mStartY = y;
            break;
        case MotionEvent.ACTION_MOVE:
            if (performClick) {
                if(Math.abs(x - mStartX) > mTouchSlop 
                        || Math.abs(y - mStartY) > mTouchSlop) {
                    performClick = false;
                }
            } else {
                mParams.x = x - mParams.width / 2;
                mParams.y = y - mParams.width;
                mWindowManager.updateViewLayout(this, mParams);
                return true;
            }

            break;
        case MotionEvent.ACTION_UP:
            Log.i("peter", "width = " + mParams.width);
            Log.i("peter", "height = " + mParams.height);
            savePosition();
            return performClick();
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
        if (performClick) {
            return super.performClick();
        }
        return false;
    }

    private void savePosition() {
        getContext().getSharedPreferences(AppManager.CONFIG, Context.MODE_PRIVATE).edit().putInt("pos_x", mParams.x)
                .commit();
        getContext().getSharedPreferences(AppManager.CONFIG, Context.MODE_PRIVATE).edit().putInt("pos_y", mParams.y)
                .commit();
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

}
