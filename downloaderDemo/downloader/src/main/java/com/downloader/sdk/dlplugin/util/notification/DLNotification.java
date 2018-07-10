
package com.downloader.sdk.dlplugin.util.notification;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.RemoteViews;

import com.downloader.sdk.dlplugin.DownLoadConfig;
import com.downloader.sdk.dlplugin.DownloadManager;
import com.downloader.sdk.dlplugin.util.common.StringUtils;

import java.lang.reflect.Field;
import java.util.UUID;

public class DLNotification {

    private Notification notification;

    private Context mContext;

    private static final int MAX_NAME_LENGTH = 30;
    private int[] remoteViewInnerids;
    private boolean existProgressBar;
    private long taskId;
    private String url;

    public DLNotification(Context context, long taskId, int icon, String name,
            String cnname, String url) {
        this.mContext = context.getApplicationContext();
        this.notification = new Notification();
        this.taskId = taskId;
        this.url = url;

        Intent intent = new Intent(mContext, this.getClass());
        Bundle bundle = new Bundle();
        intent.putExtra("ExtraData", bundle);

        PendingIntent pendingIntent = PendingIntent.getService(mContext, UUID
                .randomUUID().hashCode(), intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        this.notification.icon = android.R.drawable.stat_sys_download;

        this.notification.defaults=Notification.DEFAULT_SOUND;

        String displayName = StringUtils.isEmpty(cnname) ? StringUtils
                .toLength(name, MAX_NAME_LENGTH) : cnname;

        this.notification.when = System.currentTimeMillis();
//        this.notification.setLatestEventInfo(mContext, displayName, null,
//                pendingIntent);


        Notification.Builder builder = new Notification.Builder(context);//新建Notification.Builder对象
        builder.setContentTitle(displayName);//设置标题
        builder.setContentText("正在下载");//设置内容
        builder.setSmallIcon(notification.icon);//设置图片
        builder.setContentIntent(pendingIntent);//执行intent
        builder.build();

        notification = builder.getNotification();//将builder对象转换为普通的notification

        if (this.notification.tickerText == null) {
            this.notification.tickerText = "【" + displayName + "】开始下载";
        }
        RemoteViews remoteViews = this.notification.contentView;

        remoteViewInnerids = fetchNecessaryIds();
        if (remoteViewInnerids[0] > 0) {
            remoteViews.setViewVisibility(remoteViewInnerids[0], View.VISIBLE);
            remoteViews.setImageViewResource(remoteViewInnerids[0],
                    android.R.drawable.stat_sys_download_done);
        }
        if (remoteViewInnerids[1] > 0)
            remoteViews.setTextViewText(remoteViewInnerids[1], displayName);
        if (remoteViewInnerids[2] > 0)
            remoteViews.setTextViewText(remoteViewInnerids[2], "0%");

        View v = null;
        try {
            v = LayoutInflater.from(mContext).inflate(
                    remoteViews.getLayoutId(), null);
        } catch (Exception e) {

        }

        if (v != null)
            findProgressBarInRemoteViewLayout(v);

        if (remoteViewInnerids[7] > 0 && existProgressBar) {
            remoteViews.setProgressBar(remoteViewInnerids[7], 100, 0, false);
            remoteViews.setViewVisibility(remoteViewInnerids[7], View.VISIBLE);
        }

    }

    public void setProcess(int downloadedPercent, int progress) {

        if (remoteViewInnerids[2] > 0)
            this.notification.contentView.setTextViewText(
                    remoteViewInnerids[2], progress + "%");
        if (existProgressBar)
            this.notification.contentView.setProgressBar(remoteViewInnerids[7],
                    100, downloadedPercent, false);
        if (progress == 100) {
            this.notification.icon = android.R.drawable.stat_sys_download_done;
            Bundle bundle = new Bundle();
            bundle.putLong("taskid", this.taskId);
            bundle.putString("taskurl", this.url);
            Intent mIntent = new Intent(
                    DownloadManager.NOTIFICATION_CLICK_ACTION);
            mIntent.putExtras(bundle);

            PendingIntent pintent = PendingIntent.getBroadcast(mContext, Long
                    .valueOf(this.taskId).intValue(), mIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT);
            this.notification.contentView.setOnClickPendingIntent(
                    this.notification.contentView.getLayoutId(), pintent);
            this.notification.contentIntent = pintent;
            if (DownLoadConfig.notifactionCancleAble) {
                this.notification.flags = Notification.FLAG_AUTO_CANCEL;
            }
        } else {
            this.notification.icon = android.R.drawable.stat_sys_download_done;
            Bundle bundle = new Bundle();
            bundle.putLong("taskid", this.taskId);
            bundle.putString("taskurl", this.url);
            Intent mIntent = new Intent(
                    DownloadManager.NOFINISH_NOTIFICATION_CLICK_ACTION);
            mIntent.putExtras(bundle);
            PendingIntent pintent = PendingIntent.getBroadcast(mContext, Long
                    .valueOf(this.taskId).intValue(), mIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT);
            this.notification.contentView.setOnClickPendingIntent(
                    this.notification.contentView.getLayoutId(), pintent);
            this.notification.contentIntent = pintent;
        }

    }

    public void setIcon(Drawable icon) {
        if (remoteViewInnerids[0] > 0) {
            BitmapDrawable bd = (BitmapDrawable) icon;
            this.notification.contentView.setImageViewBitmap(
                    remoteViewInnerids[0], bd.getBitmap());
        }
    }

    public static int getNotificationIdByTaskId(long taskId) {
        return Long.valueOf(taskId).intValue();
    }

    public Notification getNotification() {
        return notification;
    }

    public void setNotification(Notification notification) {
        this.notification = notification;
    }

    private int[] fetchNecessaryIds() {
        int[] rtn = {
                0, 0, 0, 0, 0, 0, 0, 0
        };// order:icon,title,text,text1,text2,info,time,progress
        Field[] fields = null;
        try {
            Class cls = Class.forName("com.android.internal.R$id");
            fields = cls.getFields();
        } catch (ClassNotFoundException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

        for (int i = 0; i < fields.length; i++) {
            Field fd = fields[i];
            String fname = fd.getName();
            int fvalue = 0;
            try {
                fvalue = fd.getInt(fname);
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
            if (fname.equals("icon")) {
                rtn[0] = fvalue;
            }
            if (fname.equals("title")) {
                rtn[1] = fvalue;
            }
            if (fname.equals("text")) {
                rtn[2] = fvalue;
            }
            if (fname.equals("text1")) {
                rtn[3] = fvalue;
            }
            if (fname.equals("text2")) {
                rtn[4] = fvalue;
            }
            if (fname.equals("info")) {
                rtn[5] = fvalue;
            }
            if (fname.equals("time")) {
                rtn[6] = fvalue;
            }
            if (fname.equals("progress")) {
                rtn[7] = fvalue;
            }
        }
        return rtn;
    }

    private void findProgressBarInRemoteViewLayout(View paramView) {
        if (paramView == null)
            return;
        out: if (paramView instanceof ViewGroup) {
            for (int i = 0; i < ((ViewGroup) paramView).getChildCount(); ++i) {
                View localView = ((ViewGroup) paramView).getChildAt(i);
                if (localView instanceof ProgressBar) {
                    existProgressBar = true;
                    break out;
                }

                if (localView instanceof ViewGroup)
                    findProgressBarInRemoteViewLayout(localView);
            }
        }
    }

}
