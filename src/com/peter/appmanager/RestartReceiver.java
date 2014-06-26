package com.peter.appmanager;

import android.content.BroadcastReceiver;
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
	}

}
