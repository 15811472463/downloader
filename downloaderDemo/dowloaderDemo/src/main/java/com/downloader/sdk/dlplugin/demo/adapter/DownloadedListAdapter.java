package com.downloader.sdk.dlplugin.demo.adapter;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import com.downloader.sdk.dlplugin.demo.R;
import com.downloader.sdk.dlplugin.util.common.StringUtils;
import com.downloader.sdk.dlplugin.DownloadManager;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;

public class DownloadedListAdapter extends BaseAdapter
{

	private Context mContext;
	private ArrayList<HashMap<Integer, String>> dataList;
	private ListView downloadList;

	private volatile boolean isInit; 
	
	private static class SingletonHolder {
		private static final DownloadedListAdapter INSTANCE = new DownloadedListAdapter();
	}
	public static final DownloadedListAdapter getInstance() {
		return SingletonHolder.INSTANCE;
	}
	
	private DownloadedListAdapter(){}

	public DownloadedListAdapter init(Context context, ListView listView)
	{
		if(!isInit){
			isInit=true;
			this.mContext = context;
			this.downloadList = listView;
			this.dataList = new ArrayList<HashMap<Integer, String>>();
		}
		return this;
	}
	public void  reset(){
		dataList = new ArrayList<HashMap<Integer, String>>();
		this.notifyDataSetChanged();
	}
	@Override
	public int getCount()
	{
		return this.dataList.size();
	}

	@Override
	public Object getItem(int position)
	{
		return this.dataList.get(position);
	}

	@Override
	public long getItemId(int position)
	{
		return position;
	}

//	public void addItem(String url)
//	{
//		HashMap<Integer, String> item = new HashMap<Integer, String>();
//		item.put(DownloadedItemViewHolder.KEY_URL, url);
//		dataList.add(item);
//		this.notifyDataSetChanged();
//	}
	public void addItem(HashMap item)
	{
		dataList.add(item);
		this.notifyDataSetChanged();
	}


	public void removeItem(String url)
	{
		String tmp;
		for (int i = 0; i < dataList.size(); i++)
		{
			tmp = dataList.get(i).get(DownloadedItemViewHolder.KEY_URL);
			if (tmp.equals(url))
			{
				dataList.remove(i);
				this.notifyDataSetChanged();
			}
		}
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent)
	{
		if (convertView == null)
		{
			convertView = LayoutInflater.from(mContext).inflate(
					R.layout.downloaded_list_item, null);
		}

		HashMap<Integer, String> itemData = dataList.get(position);
		String url = itemData.get(DownloadedItemViewHolder.KEY_URL);
		convertView.setTag(url);
		DownloadedItemViewHolder viewHolder = new DownloadedItemViewHolder(convertView);
		viewHolder.setData(itemData);

		viewHolder.installButton.setOnClickListener(new DownloadBtnListener(
				url, viewHolder));


		return convertView;
	}
	private void openFile(String url) {
		String filepath=DownloadManager.getInstance(this.mContext).getRootPath()+StringUtils.getOriginalFileNameFromUrl(url);
		File file =new File(filepath);
        Intent intent = new Intent();
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setAction(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.fromFile(file),"application/vnd.android.package-archive");
        mContext.startActivity(intent);
}
	private class DownloadBtnListener implements View.OnClickListener
	{
		private String url;
		private DownloadedItemViewHolder mViewHolder;

		public DownloadBtnListener(String url, DownloadedItemViewHolder viewHolder)
		{
			this.url = url;
			this.mViewHolder = viewHolder;
		}

		@Override
		public void onClick(View v)
		{
			int t0=v.getId();
			int t1=R.id.btn_install;
			if(v.getId()==R.id.btn_install)
			{
			

				openFile(url);
				//downloadManager.continueHandler(url);
//				mViewHolder.continueButton.setVisibility(View.GONE);
//				mViewHolder.pauseButton.setVisibility(View.VISIBLE);


//			case R.id.btn_delete:
//				downloadManager.deleteHandler(url);
//				removeItem(url);
//
//				break;
			}
		}
	}

}