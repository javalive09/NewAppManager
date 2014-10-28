package com.peter.appmanager;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;
import java.util.Map.Entry;
import com.peter.appmanager.R;
import com.peter.appmanager.AppAdapter.AppInfo;
import com.peter.appmanager.AppAdapter.ViewCache;
import com.peter.appmanager.MyService.MyBinder;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemProperties;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

public class MainActivity extends Activity implements OnItemClickListener, OnItemLongClickListener,
		OnClickListener {

	private AlertDialog mDialog = null;
	private AppAdapter<AppInfo> appAdapter = null;
	private ProgressBar mPb = null;
	private Button mKill = null;
	private MyService mService = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getOverflowMenu();
		Intent intent = new Intent(MainActivity.this, MyService.class);
		startService(intent);
		bindService(intent, conn, Context.BIND_AUTO_CREATE);
	}

	private ServiceConnection conn = new ServiceConnection() {

		@Override
		public void onServiceDisconnected(ComponentName name) {
		}

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			MyBinder binder = (MyBinder)service;
            mService = binder.getService();
		}
	};

	@Override
	protected void onDestroy() {
		unbindService(conn);
		super.onDestroy();
	}

	@Override
	protected void onResume() {
		super.onResume();
		relodData();
	}

	private void relodData() {
		setContentView(R.layout.main);
		AppManager application = (AppManager) getApplication();
		appAdapter = new AppAdapter<AppInfo>(MainActivity.this,
				application.getRunningAppInfos(MainActivity.this,
						AppManager.SHOW), R.layout.listviewitem);
		ListView appListView = (ListView) findViewById(R.id.app_list);
		mKill = (Button) findViewById(R.id.kill);
		mKill.setText(getResources().getString(R.string.kill));
		mPb = (ProgressBar) findViewById(R.id.pb);
		mPb.setVisibility(View.INVISIBLE);
		appListView.setAdapter(appAdapter);
		appListView.setOnItemClickListener(this);
		appListView.setOnItemLongClickListener(this);
		selectAll(true);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (mDialog == null) {
			mDialog = new AlertDialog.Builder(MainActivity.this).create();
		}
		switch (item.getItemId()) {
		case R.id.action_help:
			mDialog.setTitle(getResources().getString(R.string.action_help));
			mDialog.setMessage(getResources().getString(
					R.string.action_help_msg));
			mDialog.show();
			break;
		case R.id.action_about:
			mDialog.setTitle(getResources().getString(R.string.action_about));
			mDialog.setMessage(getResources().getString(
					R.string.action_about_msg));
			mDialog.show();

			break;
		case R.id.action_feedback:
			break;
		case R.id.action_settings:
			startActivity(new Intent(MainActivity.this, SettingActivity.class));
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	private void getOverflowMenu() {
		try {
			ViewConfiguration config = ViewConfiguration.get(this);
			Field menuKeyField = ViewConfiguration.class
					.getDeclaredField("sHasPermanentMenuKey");
			if (menuKeyField != null) {
				menuKeyField.setAccessible(true);
				menuKeyField.setBoolean(config, false);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * AdapterView 即 ListView
	 */
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		ViewCache viewCache = (ViewCache) view.getTag();
		viewCache.app_CheckBox.toggle();// 反选
		Boolean isChecked = viewCache.app_CheckBox.isChecked();
		appAdapter.isSelected.put(viewCache.info.packageName, isChecked);
		setTitle("已选中" + getSelectCount() + "项");
	}

	/**
	 * 获取 选中的个数
	 * 
	 * @return
	 */
	private int getSelectCount() {
		int count = 0;
		Set<Entry<String, Boolean>> entrySet = appAdapter.isSelected.entrySet();
		Iterator<Entry<String, Boolean>> it = entrySet.iterator();
		while (it.hasNext()) {
			Entry<String, Boolean> entry = it.next();
			Boolean isSlected = entry.getValue();
			if (isSlected) {
				count++;
			}
		}
		return count;
	}

	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.kill:
			kill();
			break;

		default:
			break;
		}
	}

	/**
	 * 全选,全不选
	 * 
	 * @param isSlected
	 */
	private void selectAll(boolean isSlected) {
		for (AppInfo appInfo : appAdapter.getInfos()) {
			appAdapter.isSelected.put(appInfo.packageName, isSlected);
		}
	}

	private ArrayList<String> getKillList() {
		ArrayList<String> list = new ArrayList<String>();
		Set<Entry<String, Boolean>> entrySet = appAdapter.isSelected.entrySet();
		Iterator<Entry<String, Boolean>> iterator = entrySet.iterator();
		String mPackageName = getPackageName();
		ArrayList<String> packageNames = new ArrayList<String>();
		while (iterator.hasNext()) {
			Entry<String, Boolean> entry = iterator.next();
			if (entry.getValue()) {
				final String packageName = entry.getKey();
				if (!packageName.equals(mPackageName)) {
					list.add(packageName);
				}
				packageNames.add(packageName);
			}
		}
		return list;
	}

	private void kill() {
		new AsyncTask<Void, Integer, ArrayList<String>>() {

			@Override
			protected ArrayList<String> doInBackground(Void... params) {
				ArrayList<String> packageNames = getKillList();
				mService.kill(packageNames);
				try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				return packageNames;
			}

			@Override
			protected void onPreExecute() {
				mPb.setVisibility(View.VISIBLE);
				mKill.setEnabled(false);
			}

			@Override
			protected void onPostExecute(ArrayList<String> packageNames) {
				String mPackageName = getPackageName();
				boolean killSelf = false;
				for (String packageName : packageNames) {
					Toast.makeText(MainActivity.this, "Kill " + packageName,
							Toast.LENGTH_SHORT).show();
					if (packageName.equals(mPackageName)) {
						killSelf = true;
					}
				}

				if (killSelf) {
					finish();
				} else {
					relodData();
				}
			}

		}.execute();
	}

	@Override
	public boolean onItemLongClick(AdapterView<?> parent, View view,
			int position, long id) {
		AppInfo info = (AppInfo) appAdapter.getItem(position);
		showForceStopView(info.packageName);
		return false;
	}
	
	public void showForceStopView(String packageName) {
		int version = Build.VERSION.SDK_INT;
		Intent intent = new Intent();
		if(version >= 9) {
//			if(isMiUI()) {
//				intent.setAction("miui.intent.action.APP_PERM_EDITOR");
//				intent.setClassName("com.miui.securitycenter", "com.miui.permcenter.permissions.AppPermissionsEditorActivity");
//				intent.putExtra("extra_pkgname", packageName);
//			}else {
				intent.setAction("android.settings.APPLICATION_DETAILS_SETTINGS");
				Uri uri = Uri.fromParts("package", packageName, null);  
				intent.setData(uri);
//			}
		}else {
			final String appPkgName = "pkg";  
	        intent.setAction(Intent.ACTION_VIEW);  
	        intent.setClassName("com.android.settings", "com.android.settings.InstalledAppDetails");  
	        intent.putExtra(appPkgName, packageName);  
		}
//		intent.addFlags(0x10008000);
		startActivity(intent);  
	}
	
	private boolean isMiUI() {
		String str = SystemProperties.get("ro.miui.ui.version.name", "unkonw");
	    if ((str.equalsIgnoreCase("V5")) || (str.equalsIgnoreCase("V6"))) {
	      return true;
	    }
	    return false;
	}
	
}
