
package com.downloader.sdk.dlplugin.util.notification;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;

import com.downloader.sdk.dlplugin.util.log.Logger;

import java.util.HashMap;
import java.util.Map;

public class NotificationHelper {

    private NotificationManager mNotificationManager;

    private Map<Long, DLNotification> mNotifications = new HashMap<Long, DLNotification>();

    private final String TAG = "NotificationHelper";

    private Context mContext;

    public NotificationHelper(Context context) {
        this.mContext = context;
        this.mNotifications = new HashMap<Long, DLNotification>();
        this.mNotificationManager = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);

    }

    public synchronized void addNotification(long notifactionId, int icon,
            String name, String cnname, String url) {
        try {
            DLNotification dlNotification = mNotifications.get(notifactionId);
            Logger.e(TAG, "taskId--->" + notifactionId + ";notifaction--->" + "添加了");
            if (dlNotification != null) {
                if (dlNotification.getNotification() != null) {
                    return;
                }
            }
            DLNotification dlnotification = new DLNotification(this.mContext,
                    notifactionId, icon, name, cnname, url);
            dlnotification.getNotification().flags = Notification.FLAG_NO_CLEAR;
            mNotifications.put(notifactionId, dlnotification);
            mNotificationManager.notify(
                    DLNotification.getNotificationIdByTaskId(notifactionId),
                    dlnotification.getNotification());
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void updateNotification(long notifationId, int downloadedPercent,
            int progress) {
        DLNotification dlnotification = mNotifications.get(notifationId);
        if (dlnotification == null)
            return;
        dlnotification.setProcess(downloadedPercent, progress);
        mNotificationManager.notify(
                DLNotification.getNotificationIdByTaskId(notifationId),
                dlnotification.getNotification());
    }

    public void updateNotificationIcon(long notifactionId, Drawable icon) {
        if (icon == null)
            return;
        DLNotification dlnotification = mNotifications.get(notifactionId);
        if (dlnotification == null)
            return;
        dlnotification.setIcon(icon);
        mNotificationManager.notify(
                DLNotification.getNotificationIdByTaskId(notifactionId),
                dlnotification.getNotification());
    }

    public void cancelNotification(long notifactionId) {
        mNotifications.remove(notifactionId);
        mNotificationManager.cancel(DLNotification
                .getNotificationIdByTaskId(notifactionId));
    }

    public void updateNotificationTicker(long notifactionId, String tickerText) {
        if (TextUtils.isEmpty(tickerText)) {
            return;
        }
        DLNotification dlnotification = mNotifications.get(notifactionId);
        if (dlnotification == null) {
            return;
        }
        String tickerStr = dlnotification.getNotification().tickerText
                .toString();
        if (tickerStr != null && tickerStr.length() > 4) {
            dlnotification.getNotification().tickerText = tickerStr.substring(
                    0, tickerStr.length() - 4) + tickerText;
            mNotificationManager.notify(
                    DLNotification.getNotificationIdByTaskId(notifactionId),
                    dlnotification.getNotification());

        } else {
            dlnotification.getNotification().tickerText = "初始化错误";
            mNotificationManager.notify(
                    DLNotification.getNotificationIdByTaskId(notifactionId),
                    dlnotification.getNotification());
        }

    }

    public Map<Long, DLNotification> getNotifications() {
        return mNotifications;
    }

    public void setNotifications(Map<Long, DLNotification> notifications) {
        this.mNotifications = notifications;
    }

    public void cancelAllNotifactions() {
        mNotificationManager.cancelAll();
        if (mNotifications != null) {
            mNotifications.clear();
        }
    }

}
