package com.downloader.sdk.dlplugin.demo.adapter;

import java.util.HashMap;

import com.downloader.sdk.dlplugin.demo.R;
import com.downloader.sdk.dlplugin.util.common.StringUtils;

import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class DownloadedItemViewHolder extends DownloadItemViewHolderBase
{




public String downloadId;
public String titleCN;
public TextView title;
public TextView filesize;
public Button installButton;

private boolean hasInited = false;

	public DownloadedItemViewHolder(View parentView)
	{
		if (parentView != null)
		{
			title = (TextView) parentView
					.findViewById(R.id.title);
			filesize = (TextView) parentView
					.findViewById(R.id.filesize);			
			installButton = (Button) parentView
					.findViewById(R.id.btn_install);
			hasInited = true;
		}
	}
	public void setData(HashMap<Integer, String> item)
	{
		if (hasInited)
		{
			title.setText(StringUtils.getOriginalFileNameFromUrl(item
					.get(KEY_URL)));
			filesize.setText(item.get(KEY_FILE_SIZE));

		}
	}
}
