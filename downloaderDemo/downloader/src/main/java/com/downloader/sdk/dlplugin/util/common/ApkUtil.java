
package com.downloader.sdk.dlplugin.util.common;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;
import android.util.Log;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ApkUtil {

    public static Object[] fetchApkFileInfo(Context context, String apkPath)
            throws ClassNotFoundException, NoSuchMethodException,
            IllegalArgumentException, InstantiationException,
            IllegalAccessException, InvocationTargetException,
            NoSuchFieldException {
        String PATH_PackageParser = "android.content.pm.PackageParser";
        String PATH_AssetManager = "android.content.res.AssetManager";
        // apk包的文件路径
        // 这是一个Package 解释器, 是隐藏的
        // 构造函数的参数只有一个, apk文件的路径
        // PackageParser packageParser = new PackageParser(apkPath);
        Class pkgParserCls = Class.forName(PATH_PackageParser);
        Class[] typeArgs = new Class[1];
        typeArgs[0] = String.class;
        Constructor pkgParserCt = pkgParserCls.getConstructor(typeArgs);
        Object[] valueArgs = new Object[1];
        valueArgs[0] = apkPath;
        Object pkgParser = pkgParserCt.newInstance(valueArgs);
        Log.d("ANDROID_LAB", "pkgParser:" + pkgParser.toString());
        // 这个是与显示有关的, 里面涉及到一些像素显示等等, 我们使用默认的情况
        DisplayMetrics metrics = new DisplayMetrics();
        metrics.setToDefaults();
        // PackageParser.Package mPkgInfo = packageParser.parsePackage(new
        // File(apkPath), apkPath,
        // metrics, 0);
        typeArgs = new Class[4];
        typeArgs[0] = File.class;
        typeArgs[1] = String.class;
        typeArgs[2] = DisplayMetrics.class;
        typeArgs[3] = Integer.TYPE;
        Method pkgParser_parsePackageMtd = pkgParserCls.getDeclaredMethod(
                "parsePackage", typeArgs);
        valueArgs = new Object[4];
        valueArgs[0] = new File(apkPath);
        valueArgs[1] = apkPath;
        valueArgs[2] = metrics;
        valueArgs[3] = 0;
        Object pkgParserPkg = pkgParser_parsePackageMtd.invoke(pkgParser,
                valueArgs);
        // 应用程序信息包, 这个公开的, 不过有些函数, 变量没公开
        // ApplicationInfo info = mPkgInfo.applicationInfo;
        Field appInfoFld = pkgParserPkg.getClass().getDeclaredField(
                "applicationInfo");
        ApplicationInfo appInfo = (ApplicationInfo) appInfoFld
                .get(pkgParserPkg);
        // uid 输出为"-1"，原因是未安装，系统未分配其Uid。
        Log.d("ANDROID_LAB", "pkg:" + appInfo.packageName + " uid="
                + appInfo.uid);
        // Resources pRes = getResources();
        // AssetManager assmgr = new AssetManager();
        // assmgr.addAssetPath(apkPath);
        // Resources res = new Resources(assmgr, pRes.getDisplayMetrics(),
        // pRes.getConfiguration());
        Class assetMagCls = Class.forName(PATH_AssetManager);
        Constructor assetMagCt = assetMagCls.getConstructor((Class[]) null);
        Object assetMag = assetMagCt.newInstance((Object[]) null);
        typeArgs = new Class[1];
        typeArgs[0] = String.class;
        Method assetMag_addAssetPathMtd = assetMagCls.getDeclaredMethod(
                "addAssetPath", typeArgs);
        valueArgs = new Object[1];
        valueArgs[0] = apkPath;
        assetMag_addAssetPathMtd.invoke(assetMag, valueArgs);
        Resources res = context.getResources();
        typeArgs = new Class[3];
        typeArgs[0] = assetMag.getClass();
        typeArgs[1] = res.getDisplayMetrics().getClass();
        typeArgs[2] = res.getConfiguration().getClass();
        Constructor resCt = Resources.class.getConstructor(typeArgs);
        valueArgs = new Object[3];
        valueArgs[0] = assetMag;
        valueArgs[1] = res.getDisplayMetrics();
        valueArgs[2] = res.getConfiguration();
        res = (Resources) resCt.newInstance(valueArgs);

        CharSequence label = null;
        if (appInfo.labelRes != 0) {
            label = res.getText(appInfo.labelRes);
        }
        if (label == null) {
            label = (appInfo.nonLocalizedLabel != null) ? appInfo.nonLocalizedLabel
                    : appInfo.packageName;
        }
        // Log.d("ANDROID_LAB", "label=" + label);
        // 这里就是读取一个apk程序的图标

        Drawable icon = null;// 得到图标信息

        if (appInfo.icon != 0) {
            icon = res.getDrawable(appInfo.icon);
        }
        return new Object[] {
                appInfo.packageName, icon, label
        };

    }
}
