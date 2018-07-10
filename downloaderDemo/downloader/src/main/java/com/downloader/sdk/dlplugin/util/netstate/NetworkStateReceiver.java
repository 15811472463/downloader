
package com.downloader.sdk.dlplugin.util.netstate;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import com.downloader.sdk.dlplugin.util.log.Logger;
import com.downloader.sdk.dlplugin.util.netstate.NetWorkUtil.netType;

import java.util.ArrayList;

/**
 * @Title NetworkStateReceiver com.adsage.sdk.dlplugin.util.netstate
 * @Description 是一个检测网络状态改变的，需要配置 <receiver
 *              android:name="com.adsage.sdk.dlplugin.util.netstate.TANetworkStateReceiver"
 *              > <intent-filter> <action
 *              android:name="android.net.conn.CONNECTIVITY_CHANGE" /> <action
 *              android:name="android.gzcpc.conn.CONNECTIVITY_CHANGE" />
 *              </intent-filter> </receiver> 需要开启权限 <uses-permission
 *              android:name="android.permission.CHANGE_NETWORK_STATE" />
 *              <uses-permission
 *              android:name="android.permission.CHANGE_WIFI_STATE" />
 *              <uses-permission
 *              android:name="android.permission.ACCESS_NETWORK_STATE" />
 *              <uses-permission
 *              android:name="android.permission.ACCESS_WIFI_STATE" />
 * @author
 * @date 2013-5-5 下午 22:47
 * @version V1.2
 */
public class NetworkStateReceiver extends BroadcastReceiver {
    private static Boolean networkAvailable = false;
    private static netType netType;
    private static ArrayList<NetChangeObserver> netChangeObserverArrayList = new ArrayList<NetChangeObserver>();
    private final static String ANDROID_NET_CHANGE_ACTION = "android.net.conn.CONNECTIVITY_CHANGE";
    public final static String DL_ANDROID_NET_CHANGE_ACTION = "dl.android.net.conn.CONNECTIVITY_CHANGE";
    private static BroadcastReceiver receiver;

    private static BroadcastReceiver getReceiver() {
        if (receiver == null) {
            receiver = new NetworkStateReceiver();
        }
        return receiver;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        receiver = NetworkStateReceiver.this;
        if (intent.getAction().equalsIgnoreCase(ANDROID_NET_CHANGE_ACTION)
                || intent.getAction().equalsIgnoreCase(
                        DL_ANDROID_NET_CHANGE_ACTION)) {
            Logger.i(NetworkStateReceiver.this, "网络状态改变.");
            if (!NetWorkUtil.isNetworkAvailable(context)) {
                Logger.i(NetworkStateReceiver.this, "没有网络连接.");
                networkAvailable = false;
            } else {
                Logger.i(NetworkStateReceiver.this, "网络连接成功.");
                netType = NetWorkUtil.getAPNType(context);
                networkAvailable = true;
            }
            notifyObserver();
        }
    }

    /**
     * 注册网络状态广播
     * 
     * @param mContext
     */
    public static void registerNetworkStateReceiver(Context mContext) {
        IntentFilter filter = new IntentFilter();
        filter.addAction(DL_ANDROID_NET_CHANGE_ACTION);
        filter.addAction(ANDROID_NET_CHANGE_ACTION);
        mContext.getApplicationContext()
                .registerReceiver(getReceiver(), filter);
    }

    /**
     * 检查网络状态
     * 
     * @param mContext
     */
    public static void checkNetworkState(Context mContext) {
        Intent intent = new Intent();
        intent.setAction(DL_ANDROID_NET_CHANGE_ACTION);
        mContext.sendBroadcast(intent);
    }

    /**
     * 注销网络状态广播
     * 
     * @param mContext
     */
    public static void unRegisterNetworkStateReceiver(Context mContext) {
        if (receiver != null) {
            try {
                mContext.getApplicationContext().unregisterReceiver(receiver);
            } catch (Exception e) {
                // TODO: handle exception
                Logger.d("NetworkStateReceiver", e.getMessage());
            }
        }

    }

    /**
     * 获取当前网络状态，true为网络连接成功，否则网络连接失败
     * 
     * @return
     */
    public static Boolean isNetworkAvailable() {
        return networkAvailable;
    }

    public static netType getAPNType() {
        return netType;
    }

    private void notifyObserver() {

        for (int i = 0; i < netChangeObserverArrayList.size(); i++) {
            NetChangeObserver observer = netChangeObserverArrayList.get(i);
            if (observer != null) {
                if (isNetworkAvailable()) {
                    observer.onConnect(netType);
                } else {
                    observer.onDisConnect();
                }
            }
        }

    }

    /**
     * 注册网络连接观察者
     * 
     * @param observerKey observerKey
     */
    public static void registerObserver(NetChangeObserver observer) {
        if (netChangeObserverArrayList == null) {
            netChangeObserverArrayList = new ArrayList<NetChangeObserver>();
        }
        netChangeObserverArrayList.add(observer);
    }

    /**
     * 注销网络连接观察者
     * 
     * @param resID observerKey
     */
    public static void removeRegisterObserver(NetChangeObserver observer) {
        if (netChangeObserverArrayList != null) {
            netChangeObserverArrayList.remove(observer);
        }
    }

}
