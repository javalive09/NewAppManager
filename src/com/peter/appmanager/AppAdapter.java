package com.peter.appmanager;

import java.util.HashMap;
import java.util.List;

import com.peter.appmanager.R;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

public class AppAdapter extends BaseAdapter {

	private List<AppInfo> appInfos;
	private LayoutInflater inflater;
	private int itemResource;
	public HashMap<String, Boolean> isSelected;

	public AppAdapter(Context context, List<AppInfo> appInfos, int resource) {
		this.appInfos = appInfos;
		this.itemResource = resource;
		inflater = LayoutInflater.from(context);
		isSelected = new HashMap<String, Boolean>();
	}

	public List<AppInfo> getInfos() {
		return appInfos;
	}
	
	/**
	 * 更新列表数据
	 * 
	 * @param appInfos
	 */
	public void updateData(List<AppInfo> appInfos) {
		this.appInfos = appInfos;
		notifyDataSetInvalidated();
	}

	@Override
	public int getCount() {
		return appInfos.size();
	}

	@Override
	public Object getItem(int position) {
		return appInfos != null ? appInfos.get(position) : null;
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		// 获取数据
		AppInfo info = (AppInfo) getItem(position);

		// 获取View
		ViewCache cache = null;
		if (convertView == null) {
			convertView = inflater.inflate(itemResource, null);
			ImageView app_icon = (ImageView) convertView
					.findViewById(R.id.app_icon);
			TextView app_name = (TextView) convertView
					.findViewById(R.id.app_name);
			CheckBox app_CheckBox = (CheckBox) convertView
					.findViewById(R.id.app_checkbox);

			cache = new ViewCache();

			cache.app_icon = app_icon;
			cache.app_name = app_name;
			cache.app_CheckBox = app_CheckBox;

			convertView.setTag(cache);
		} else {
			cache = (ViewCache) convertView.getTag();
		}

		// 绑定数据
		cache.app_icon.setImageDrawable(info.appIcon);
		cache.app_name.setText(info.appName);
		Boolean slected = isSelected.get(info.packageName);
		if(slected == null) {
			slected = Boolean.FALSE;
		}
		cache.app_CheckBox.setChecked(slected);
		cache.info = info;
		
		return convertView;
	}
	
	public static class ViewCache {
		ImageView app_icon;
		TextView app_name;
		CheckBox app_CheckBox;
		AppInfo info;
	}

	public static class AppInfo {
		public String appName;
		public String packageName;
		public Drawable appIcon;
	}
}