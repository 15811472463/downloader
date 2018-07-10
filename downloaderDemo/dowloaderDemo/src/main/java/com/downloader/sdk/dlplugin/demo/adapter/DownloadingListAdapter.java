package com.downloader.sdk.dlplugin.demo.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;

import com.downloader.sdk.dlplugin.DownLoadCallback;
import com.downloader.sdk.dlplugin.DownloadManager;
import com.downloader.sdk.dlplugin.demo.R;
import com.downloader.sdk.dlplugin.util.log.Logger;

import java.util.ArrayList;
import java.util.HashMap;

public class DownloadingListAdapter extends BaseAdapter {

    private Context mContext;
    private ArrayList<HashMap<Integer, String>> dataList;
    private HashMap<String, HashMap<Integer, String>> extraDataList;
    private DownloadManager downloadManager;
    private ListView downloadList;
    private boolean isEditableStatus;
    private volatile boolean isInit;


    private static class SingletonHolder {
        private static final DownloadingListAdapter INSTANCE = new DownloadingListAdapter();
    }

    public static final DownloadingListAdapter getInstance() {
        return SingletonHolder.INSTANCE;
    }

    private DownloadingListAdapter() {
    }

    public DownloadingListAdapter init(Context context, ListView listView) {
        if (!isInit) {
            isInit = true;

            this.mContext = context;
            this.downloadList = listView;
            dataList = new ArrayList<HashMap<Integer, String>>();
            extraDataList = new HashMap<String, HashMap<Integer, String>>();
            downloadManager = DownloadManager.getInstance(this.mContext);
            downloadManager.setContext(mContext);
        }
        return this;
    }

    public void setDownLoaderProgress(String url, long totalSize, long currentSize, long speed) {
        long downloadPercent = currentSize * 100 / totalSize;


        View taskListItem = downloadList.findViewWithTag(url);
        DownloadingItemViewHolder viewHolder = new DownloadingItemViewHolder(taskListItem);
        viewHolder.setData(url,
                FileInfoUtils.FormetFileSize(currentSize) + "|" + FileInfoUtils.FormetFileSize(totalSize),
                FileInfoUtils.FormetFileSize(totalSize), downloadPercent + "");
        viewHolder.speedText.setText(speed + "kbs");
        extraDataList.put(url, DownloadingItemViewHolder.getItemDataMap(url, speed + "", FileInfoUtils.FormetFileSize(totalSize), null, null));
        Logger.d(DownloadingListAdapter.this, "speed" + speed + "kbps"
                + "downloadPercent" + downloadPercent);
    }

    public void reInit() {
        isInit = false;
        dataList = new ArrayList<HashMap<Integer, String>>();
        extraDataList = new HashMap<String, HashMap<Integer, String>>();
        this.notifyDataSetChanged();
    }

    public void reset() {
        dataList = new ArrayList<HashMap<Integer, String>>();
        extraDataList = new HashMap<String, HashMap<Integer, String>>();
        isInit = false;
        init(mContext, downloadList);
        this.notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return dataList.size();
    }

    @Override
    public Object getItem(int position) {
        return dataList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }


    public void addItem(long taskId, String url, boolean isPaused) {
        HashMap<Integer, String> item = new HashMap<Integer, String>();
        item.put(DownloadingItemViewHolder.KEY_TASKID, taskId + "");
        item.put(DownloadingItemViewHolder.KEY_URL, url);
        dataList.add(item);
        extraDataList.put(url, item);
        this.notifyDataSetChanged();
    }

    public void removeItem(long taskId, String url) {
        String tmp;
        HashMap map = null;
        for (int i = 0; i < dataList.size(); i++) {
            tmp = dataList.get(i).get(DownloadingItemViewHolder.KEY_URL);
            if (tmp.equals(url)) {
                dataList.remove(i);
                map = extraDataList.get(url);
                extraDataList.remove(url);
                this.notifyDataSetChanged();

                DownloadedListAdapter.getInstance().addItem(map);
            }
        }

    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(mContext).inflate(
                    R.layout.downloading_list_item, null);
        }

        HashMap<Integer, String> itemData = dataList.get(position);
        String url = itemData.get(DownloadingItemViewHolder.KEY_URL);
        String taskId = itemData.get(DownloadingItemViewHolder.KEY_TASKID);
        convertView.setTag(url);
        DownloadingItemViewHolder viewHolder = new DownloadingItemViewHolder(convertView);
        viewHolder.setData(itemData);
        viewHolder.checkBoxContainer.setVisibility(isEditableStatus ? View.VISIBLE : View.GONE);
        viewHolder.continueButton.setOnClickListener(new DownloadBtnListener(Long.parseLong(taskId),
                url, viewHolder));
        viewHolder.pauseButton.setOnClickListener(new DownloadBtnListener(Long.parseLong(taskId), url,
                viewHolder));
//		viewHolder.deleteButton.setOnClickListener(new DownloadBtnListener(url,
//				viewHolder));

        return convertView;
    }

    private class DownloadBtnListener implements View.OnClickListener {
        private String url;
        private long taskId;
        private DownloadingItemViewHolder mViewHolder;

        public DownloadBtnListener(long taskId, String url, DownloadingItemViewHolder viewHolder) {
            this.url = url;
            this.taskId = taskId;
            this.mViewHolder = viewHolder;
        }

        @Override
        public void onClick(View v) {
            int vid = v.getId();

            if (vid == R.id.btn_continue) {
                downloadManager.continueHandler(url,0);
                mViewHolder.continueButton.setVisibility(View.GONE);
                mViewHolder.pauseButton.setVisibility(View.VISIBLE);
                mViewHolder.status_text.setText(R.string.download_pause);
                mViewHolder.speedText.setText(R.string.download_waitingfor);

            } else if (vid == R.id.btn_pause) {

                downloadManager.pauseTask(taskId);
                mViewHolder.continueButton.setVisibility(View.VISIBLE);
                mViewHolder.pauseButton.setVisibility(View.GONE);
                mViewHolder.status_text.setText(R.string.download_resume);
                mViewHolder.speedText.setText(R.string.download_has_pause);

            }
        }
    }

}