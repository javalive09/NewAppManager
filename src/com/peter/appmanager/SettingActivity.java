package com.peter.appmanager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

import com.peter.appmanager.MyService.MyBinder;
import com.peter.appmanager.R;
import com.peter.appmanager.AppAdapter.AppInfo;
import com.peter.appmanager.AppAdapter.ViewCache;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.RadioButton;
import android.widget.RadioGroup;

public class SettingActivity extends Activity implements OnItemClickListener {

	private AppAdapter<AppInfo> appAdapter;
	private MyService mService;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		getActionBar().setIcon(R.drawable.setting);
		getActionBar().setHomeButtonEnabled(true);
		getActionBar().setDisplayHomeAsUpEnabled(true);
		
	}

	@SuppressWarnings("unchecked")
	private void reloadData() {
		setContentView(R.layout.setting);
		AppManager application = (AppManager) getApplication();
		appAdapter = new AppAdapter<AppInfo>(SettingActivity.this,
				application.getRunningAppInfos(SettingActivity.this,
						AppManager.ALL), R.layout.listviewitem);
		ListView appListView = (ListView) findViewById(R.id.setting_list);
		appAdapter.isSelected = (HashMap<String, Boolean>) getSharedPreferences(
				AppManager.CONFIG, MODE_PRIVATE).getAll();
		appListView.setAdapter(appAdapter);
		appListView.setOnItemClickListener(this);
		
		bindService(new Intent(SettingActivity.this, MyService.class), conn, Context.BIND_AUTO_CREATE);
		RadioGroup mRadiogroup = (RadioGroup) findViewById(R.id.cleansetting);
		final RadioButton screenoffsetting = (RadioButton) findViewById(R.id.screenoffsetting);
		final RadioButton floatsetting = (RadioButton) findViewById(R.id.floatsetting);
		final String screenoff = getResources().getString(R.string.screenoff_setting);
		
		boolean isScreenOff = getSharedPreferences(AppManager.CLEAN_METHOD, MODE_PRIVATE).getBoolean(screenoff, false);
		if(isScreenOff) {
			screenoffsetting.setChecked(true);
		}else{
			floatsetting.setChecked(true);
		}
		
		mRadiogroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(RadioGroup group, int checkedId) {
				
				if(checkedId == screenoffsetting.getId()) {
					mService.registerReceiver();
					getSharedPreferences(AppManager.CLEAN_METHOD, MODE_PRIVATE).edit()
					.putBoolean(screenoff, true).commit();
				}else if(checkedId == floatsetting.getId()) {
					mService.unregisterReceiver();
					getSharedPreferences(AppManager.CLEAN_METHOD, MODE_PRIVATE).edit()
					.putBoolean(screenoff, false).commit();
					mService.showFloatView();
				}
			}
			
		});
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
	protected void onResume() {
		reloadData();
		super.onResume();
	}

	@Override
	protected void onDestroy() {
		unbindService(conn);
		super.onDestroy();
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			finish();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.install:
			saveToSDCard("NewAppManagerPlug.apk");
			installPlug();
			break;
		case R.id.uninstall:
			uninstallPlug();
			break;

		default:
			break;
		}
	}

	File file = null;
	
	public void saveToSDCard(String sdcardFileName) {

		FileOutputStream fos = null;
		InputStream is = null;
		
		file = new File(Environment.getExternalStorageDirectory(), sdcardFileName);
		
//		if(!file.exists()) {
			try {
				is = getResources().getAssets().open("NewAppManagerPlug.apk");
				
				fos = new FileOutputStream(file);
				
				byte[] buffer = new byte[4096];
				
				int byteCount = 0;
				while((byteCount = is.read(buffer)) != -1) {
					fos.write(buffer, 0, byteCount);;
				}
				
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				try {
					if(is != null) {
						is.close();
					}
					if(fos != null) {
						fos.close();
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
//		}
		

	}

	private void installPlug() {
		String fileName = file.getAbsolutePath();
		Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.setDataAndType(Uri.fromFile(new File(fileName)), "application/vnd.android.package-archive");
		startActivity(intent);
	}

	private void uninstallPlug() {
		Uri packageURI = Uri.parse("package:com.peter.managerplug");
		Intent uninstallIntent = new Intent(Intent.ACTION_DELETE, packageURI);
		startActivity(uninstallIntent);
	}

	/**
	 * AdapterView 即 ListView
	 */
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		//反选
		ViewCache viewCache = (ViewCache) view.getTag();
		viewCache.app_CheckBox.toggle();// 反选
		Boolean isChecked = viewCache.app_CheckBox.isChecked();
		
		//更新值
		@SuppressWarnings("unchecked")
		AppAdapter<AppInfo> adapter = ((AppAdapter<AppInfo>) parent.getAdapter());
		AppInfo info = (AppInfo) adapter.getItem(position);
		
		adapter.isSelected.put(info.packageName, isChecked);
		
		getSharedPreferences(AppManager.CONFIG, MODE_PRIVATE).edit()
				.putBoolean(viewCache.info.packageName, isChecked).commit();
	}

}
