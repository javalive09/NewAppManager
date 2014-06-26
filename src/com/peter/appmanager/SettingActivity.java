package com.peter.appmanager;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

import com.peter.appmanager.R;
import com.peter.appmanager.AppAdapter.ViewCache;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;

public class SettingActivity extends Activity implements OnItemClickListener {

	private AppAdapter appAdapter;

	@SuppressWarnings("unchecked")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.setting);
		AppManager application = (AppManager) getApplication();
		appAdapter = new AppAdapter(SettingActivity.this,
				application.getRunningAppInfos(SettingActivity.this,
						AppManager.ALL), R.layout.listviewitem);
		ListView appListView = (ListView) findViewById(R.id.setting_list);
		appAdapter.isSelected = (HashMap<String, Boolean>) getSharedPreferences(
				AppManager.SETTING, MODE_PRIVATE).getAll();
		appListView.setAdapter(appAdapter);
		appListView.setOnItemClickListener(this);
		getActionBar().setIcon(R.drawable.setting);
		getActionBar().setHomeButtonEnabled(true);
		getActionBar().setDisplayHomeAsUpEnabled(true);
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
		ViewCache viewCache = (ViewCache) view.getTag();
		viewCache.app_CheckBox.toggle();// 反选
		Boolean isChecked = viewCache.app_CheckBox.isChecked();
		getSharedPreferences(AppManager.SETTING, MODE_PRIVATE).edit()
				.putBoolean(viewCache.info.packageName, isChecked).commit();
	}

}
