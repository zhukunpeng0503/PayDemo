package com.zkp.walletdemo.platform;

import android.util.Base64;

/**
 * Created by silentalk on 2018/4/13.
 */

public class Base64Comp {
    public static byte[] decode(String src) {
        return Base64.decode(src, Base64.NO_WRAP);
    }
    public static String encodeToString(byte[] src) {
        return Base64.encodeToString(src, Base64.NO_WRAP);
    }
}
