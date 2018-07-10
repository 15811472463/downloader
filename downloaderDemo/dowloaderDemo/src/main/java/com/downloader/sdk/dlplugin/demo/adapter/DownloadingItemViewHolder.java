package com.downloader.sdk.dlplugin.demo.adapter;

import java.util.HashMap;

import com.downloader.sdk.dlplugin.demo.R;
import com.downloader.sdk.dlplugin.util.common.StringUtils;

import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.downloader.sdk.dlplugin.demo.DownloadCheckBox;
public class DownloadingItemViewHolder extends DownloadItemViewHolderBase
{


	public TextView titleText;
	public ProgressBar progressBar;
	public TextView progressText;//下载文件大小+已下载
	public TextView speedText;//显示下载速度
	public Button pauseButton;
	public Button deleteButton;
	public Button continueButton;
	public TextView status_text;//暂停/继续按钮文本
	public ImageView downloadIcon;//下载文件的icon
	public DownloadCheckBox checkBox;//选中框
	public RelativeLayout checkBoxContainer;//选中框的容器
	
	public int curStatus;//current download status:0-downloading 1-pause 2-others
	
	private boolean hasInited = false;

	public DownloadingItemViewHolder(View parentView)
	{
		if (parentView != null)
		{
			titleText = (TextView) parentView
					.findViewById(R.id.title);
			speedText = (TextView) parentView.findViewById(R.id.speed);
			progressBar = (ProgressBar) parentView
					.findViewById(R.id.progress_bar);
			pauseButton = (Button) parentView.findViewById(R.id.btn_pause);
			//deleteButton = (Button) parentView.findViewById(R.id.btn_delete);
			continueButton = (Button) parentView
					.findViewById(R.id.btn_continue);
			status_text = (TextView) parentView.findViewById(R.id.status_text);
			progressText = (TextView) parentView.findViewById(R.id.progressText);
			downloadIcon=(ImageView) parentView.findViewById(R.id.download_icon);
			checkBox=(DownloadCheckBox) parentView.findViewById(R.id.downloading_checkbox_select);
			checkBoxContainer=(RelativeLayout) parentView.findViewById(R.id.downloading_checkbox);
			curStatus=0;
			hasInited = true;
		}
	}

	public static HashMap<Integer, String> getItemDataMap(String url,
			String speed,String filesize,String progress, String isPaused)
	{
		HashMap<Integer, String> item = new HashMap<Integer, String>();
		item.put(KEY_URL, url);
		item.put(KEY_SPEED, speed);
		item.put(KEY_FILE_SIZE, filesize);
		item.put(KEY_PROGRESS, progress);
		item.put(KEY_IS_PAUSED, isPaused);
		return item;
	}
	public void onPause()
	{
		if (hasInited)
		{

			pauseButton.setVisibility(View.GONE);
			continueButton.setVisibility(View.VISIBLE);
		}
	}

	

	public void setData(HashMap<Integer, String> item)
	{
		if (hasInited)
		{
			titleText.setText(StringUtils.getOriginalFileNameFromUrl(item
					.get(KEY_URL)));
			progressText.setText(item.get(KEY_SPEED));

			String progress = item.get(KEY_PROGRESS);
			if (TextUtils.isEmpty(progress))
			{
				progressBar.setProgress(0);
			} else
			{
				progressBar.setProgress(Integer.parseInt(progress));
			}
			if (Boolean.parseBoolean(item.get(KEY_IS_PAUSED)))
			{
				onPause();
			}
		}
	}

	public void setData(String url, String speed, String filesize, String progress)
	{
		setData(url, speed, filesize, progress,false + "");
	}
	
	public void setData(String url, String speed,String filesize, String progress,String isPaused)
	{
		if (hasInited)
		{
			HashMap<Integer, String> item = getItemDataMap(url, speed,filesize,
					progress, isPaused);

			titleText.setText(StringUtils.getOriginalFileNameFromUrl(item
					.get(KEY_URL)));
			
			
			progressText.setText(speed);
			if (TextUtils.isEmpty(progress))
			{
				progressBar.setProgress(0);
			} else
			{
				progressBar
						.setProgress(Integer.parseInt(item.get(KEY_PROGRESS)));
			}

		}
	}

}
