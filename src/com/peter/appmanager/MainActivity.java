package com.peter.appmanager;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;
import java.util.Map.Entry;

import com.peter.appmanager.R;
import com.peter.appmanager.AppAdapter.AppInfo;
import com.peter.appmanager.AppAdapter.ViewCache;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

public class MainActivity extends Activity implements
	OnItemClickListener, OnClickListener{

	private AlertDialog mDialog = null;
	private AppAdapter<AppInfo> appAdapter = null;
	private ProgressBar mPb = null;
	private Button mKill = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getOverflowMenu();
		Intent intent = new Intent(MainActivity.this, MyService.class);
		startService(intent);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		relodData();
	}

	private void relodData() {
		setContentView(R.layout.main);
		AppManager application = (AppManager) getApplication();
		appAdapter = new AppAdapter<AppInfo>(MainActivity.this, application.getRunningAppInfos(MainActivity.this, AppManager.SHOW), R.layout.listviewitem);
		ListView appListView = (ListView) findViewById(R.id.app_list);
		mKill = (Button) findViewById(R.id.kill);
		mKill.setText(getResources().getString(R.string.kill));
		mPb = (ProgressBar) findViewById(R.id.pb);
		mPb.setVisibility(View.INVISIBLE);
		appListView.setAdapter(appAdapter);
		appListView.setOnItemClickListener(this);
		selectAll(true);
	}

	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
	
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	if(mDialog == null) {
    		mDialog = new AlertDialog.Builder(MainActivity.this).create();
    	} 
        switch(item.getItemId()) {
        case R.id.action_help:
        	mDialog.setTitle(getResources().getString(R.string.action_help));
        	mDialog.setMessage(getResources().getString(R.string.action_help_msg));
        	mDialog.show();
        	break;
        case R.id.action_about:
        	mDialog.setTitle(getResources().getString(R.string.action_about));
        	mDialog.setMessage(getResources().getString(R.string.action_about_msg));
        	mDialog.show();
        	
        	break;
        case R.id.action_feedback:
        	AppManager application = (AppManager) getApplication();
        	application.getRecentTask();
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
           Field menuKeyField = ViewConfiguration.class.getDeclaredField("sHasPermanentMenuKey");
           if(menuKeyField != null) {
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
			forceStopByShell();
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
	
	private void forceStopByShell() {
		
		new AsyncTask<Void, Integer, ArrayList<String>>() {
			
			@Override
			protected ArrayList<String> doInBackground(Void... params) {
				
				Set<Entry<String, Boolean>> entrySet = appAdapter.isSelected.entrySet();
				Iterator<Entry<String, Boolean>> iterator = entrySet.iterator();
				String mPackageName = getPackageName();
				ArrayList<String> packageNames = new ArrayList<String>();
				AppManager application = (AppManager) getApplication();
				while (iterator.hasNext()) {
					Entry<String, Boolean> entry = iterator.next();
					if (entry.getValue()) {
						final String packageName = entry.getKey();
						if(!packageName.equals(mPackageName)) {
							application.commandLineForceStop(packageName);
						}
						packageNames.add(packageName);
					}
				}
				application.commandLineKillAll();
				try {
					Thread.sleep(3000);
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
				for(String packageName : packageNames){
					Toast.makeText(MainActivity.this, "Kill " + packageName, Toast.LENGTH_SHORT).show();
					if(packageName.equals(mPackageName)) {
						killSelf = true;
					}
				}
				
				if(killSelf) {
					finish();
				}else {
					relodData();
				}
			}
    		
    	}.execute();
	}
	
}
