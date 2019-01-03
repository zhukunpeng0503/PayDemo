package com.zkp.walletdemo.utils;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.provider.Settings;

/**
 * 作者：Administrator on 2019/1/3 0003 14:31
 * 邮箱：2698571071@qq.com
 */
public class AndroidUtils {


    /**
     * 获取android ID
     *
     * @see
     */
    public static String getAndroidId(Context context) {
        try {
            return Settings.Secure.getString(context.getContentResolver(),
                    Settings.Secure.ANDROID_ID);
        } catch (Exception e) {

            return null;
        }
    }


    /**
     * get VersionName of this App
     */
    public static String getVersionName(Context context) {
        try {
            PackageManager manager = context.getPackageManager();
            PackageInfo info = manager.getPackageInfo(
                    context.getPackageName(), PackageManager.GET_CONFIGURATIONS);
            return info.versionName;
        } catch (Exception e) {
        }
        return null;
    }

    /**
     * 获取包名
     */
    public static String getPackageName(Context context) {
        try {
            return context.getPackageName();
        } catch (Exception e) {

            return null;
        }
    }
}
