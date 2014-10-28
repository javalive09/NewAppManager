package com.peter.appmanager;

import java.util.List;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

public class RestartReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		Uri url = intent.getData();
		String str = url.toString();
		String[] infos = str.split(":");
		for(String info : infos) {
			if(info.equals(MyService.TARGET_PACKAGE_NAME)) {
				Toast.makeText(context, "YinService RestartReceiver", Toast.LENGTH_SHORT).show();
				context.startService(new Intent(MyService.TARGET_ACTION));
			}
		}
	
		ActivityManager activityManager = (ActivityManager)(context.getSystemService(android.content.Context.ACTIVITY_SERVICE )) ;  
		List<RunningTaskInfo> runningTaskInfos = activityManager.getRunningTasks(2) ;
		
		ComponentName f1 = runningTaskInfos.get(1).topActivity;
		String topActivityClassName = f1.getClassName(); 
		
		if(f1.getClassName().equals("com.peter.appmanager.MainActivity")) {
			Intent in = new Intent();
			in.setComponent(f1);
			in.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
			in.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			in.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			context.startActivity(in);
		}
		
	}

}
