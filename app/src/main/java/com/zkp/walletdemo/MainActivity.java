package com.zkp.walletdemo;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.NetworkRequest;
import android.os.AsyncTask;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zkp.walletdemo.platform.Base64Comp;
import com.zkp.walletdemo.rsa.DCPEncryptor;
import com.zkp.walletdemo.utils.OnLazyClickListener;
import com.zkp.walletdemo.utils.network.Get;
import com.zkp.walletdemo.utils.network.NetResult;
import com.zkp.walletdemo.utils.network.Post;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends Activity {

    private static final String TAG = "MainActivity";
    public static final boolean isLogOn = true;

    protected ObjectMapper mapper;
    protected JavaType innerDataType;

    protected Context appContext;
    protected Map<String, List<String>> mHeadMap;

    // 模拟应用后台返回签名好的订单信息（实际功能由你们的后台服务器实现）
//    private static final String URL_PREPAY = "http://192.168.1.223:8101/dcpayCore/payBills/prepay";
    private static final String URL_PREPAY = "http://47.52.175.22/dcpayCore/payBills/prepay";
    private View button1;
    private String headerString;
    private HashMap<String, Object> bodyEncryptMap;
    private Map<String, String> myMap;
    private Device device;
    private int i = 1;

    private static final String KEY_BUNDLE = "PAY_REQUEST_PARAMS";
    private static final String KEY_BUNDLE_CALLBACK_NAME = "n";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initViews();
        initListeners();
    }

    private void initListeners() {
        button1.setOnClickListener(new OnLazyClickListener() {
            @Override
            public void onLazyClick(View view) {
                pay(); // 开始支付
            }
        });
    }

    private PrePayTask preTask;

    private void pay() {
        if (preTask != null) {
            Toast.makeText(MainActivity.this, "前一次支付正在执行，请稍后。。。", Toast.LENGTH_SHORT).show();
            return;
        }
        preTask = new PrePayTask();
        preTask.execute();
    }

    public String postString() {
        return Object2Json.map2Json(post());
    }

    public HashMap<String, Object> post() {

        myMap = new HashMap<>();
        myMap.put("amount", "1");
        myMap.put("attach", "2");
        myMap.put("coinId", "34190899187000");
        myMap.put("goodsTag", "coin");
        myMap.put("goodsType", "ETH");
        myMap.put("industry", "fubt");
        myMap.put("refBizNo", "00000000008" + i);

        mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);


        try {
            HashMap<String, Object> bodyEncryptMap = DCPEncryptor.encrypt(mapper.writeValueAsString(myMap), ProtocolConfigSeparate.mbr_public_key, ProtocolConfigSeparate.private_key);
            device = new Device();
            device.deviceId = getAndroidId(this);
            device.appVersion = getVersionName(this);
            device.pushId = "213231";
            device.system = "Android";
            device.language = "zh_cn";
            device.packageName = getPackageName(this);
            device.channel = ProtocolConfigSeparate.channel;

            Header header = new Header();
            header.setDevice(device);
            header.setSignType("RSA2");
            header.setSignature((String) bodyEncryptMap.get("signature"));
            header.setMerchantId(Long.parseLong(ProtocolConfigSeparate.merchant_id));
            header.setApiVersion("0.1");
            header.setTimestamp(new Date().getTime());
            header.setCharset("UTF-8");

            String headerString = mapper.writeValueAsString(header);

            String token = Base64Comp.encodeToString(headerString.getBytes("UTF-8"));
            bodyEncryptMap.remove("signature");

            mHeadMap.put("token", new ArrayList<String>(Arrays.asList(token)));
            return bodyEncryptMap;
        } catch (Exception e) {
            return null;
        }
    }

    private class PrePayTask extends AsyncTask<Void, Void, String> {
        PrePayTask() {
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            showProgress();
        }

        /**
         * 不重载此方法时发送和接收的数据都保持原样，重载了可以进行类似加密、压缩等处理
         */
        public Encryptor getEncryptor() {
            return null;
        }

        @Override
        protected String doInBackground(Void... voids) {
            try {

                Encryptor encryptor = getEncryptor();

                //请求网络数据
                NetResult netResult = Post.post(mHeadMap, postString(), URL_PREPAY, null, encryptor);

                Map<String, String> map = mapper.readValue(netResult.mData, Map.class);
                String dataStr = map.get("data");

                Map<String, String> mapData = mapper.readValue(dataStr, Map.class);
                //校验签名是否正确
                boolean b = DCPEncryptor.verifySignature(mapData.get("signature"), "RSA2", mapData.get("key"), mapData.get("iv"), mapData.get("cipher"), ProtocolConfigSeparate.mbr_public_key);
                if (!b) {
                    Log.i("post", "signature verification failed");
                    return null;
                }
                //数据的解密 拿到明文数据
                byte[] responseBodyByte = DCPEncryptor.decrypt(mapData.get("key"), mapData.get("iv"), mapData.get("cipher"), ProtocolConfigSeparate.private_key);
                String innerDataJson = new String(responseBodyByte, "UTF-8");

                JSONObject jsonObject = new JSONObject(innerDataJson);
                Log.i("post", "innerDataJson:" + innerDataJson);
                String orderInfo = jsonObject.getString("data");
                Log.i("post", "orderInfo" + orderInfo);

                return orderInfo;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(String orderInfo) {
            preTask = null;
            hideProgress();
            if (orderInfo == null) return;
            callPaySdk(orderInfo); // 使用得到的签名好的订单信息字符串唤起支付APP
        }
    }

    /**
     * 跳转到钱包APP
     *
     * @param orderInfo
     */
    private void callPaySdk(String orderInfo) {

        Log.i("callPaySdk", orderInfo);

        Intent intent = new Intent();
        Bundle bundle = new Bundle();
        bundle.putString(KEY_BUNDLE, orderInfo);
        bundle.putString(KEY_BUNDLE_CALLBACK_NAME, KEY_BUNDLE_CALLBACK_NAME);
        intent.putExtras(bundle);
        intent.setAction("com.mbr.coin.pay");
        startActivity(intent);
    }


    private void initViews() {
        button1 = findViewById(R.id.include1);

        mHeadMap = new HashMap<>();
        mHeadMap.put("Accept", new ArrayList<String>(Arrays.asList("application/json")));
        mHeadMap.put("Content-Type", new ArrayList<String>(Arrays.asList("application/json")));
    }

    private Dialog progressDialog;

    private void showProgress() {
        if (progressDialog != null) return;
        progressDialog = new ProgressDialog(this);
        progressDialog.show();
    }

    private void hideProgress() {
        if (progressDialog != null) {
            progressDialog.dismiss();
            progressDialog = null;
        }
    }

    private class ProgressDialog extends Dialog {

        public ProgressDialog(@NonNull Context context) {
            super(context);
            requestWindowFeature(Window.FEATURE_NO_TITLE);
            Window window = getWindow();
            window.setBackgroundDrawableResource(android.R.color.transparent);
            RelativeLayout root = new RelativeLayout(context);
            ProgressBar progressBar = new ProgressBar(context);
            progressBar.setIndeterminate(true);
            RelativeLayout.LayoutParams params;
            params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
            params.addRule(RelativeLayout.CENTER_IN_PARENT);
            root.addView(progressBar, params);
            setContentView(root);
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            setCanceledOnTouchOutside(false);
        }
    }


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
/*public class Solution {
    public bool IsPalindrome(int x) {
        // 特殊情况：
        // 如上所述，当 x < 0 时，x 不是回文数。
        // 同样地，如果数字的最后一位是 0，为了使该数字为回文，
        // 则其第一位数字也应该是 0
        // 只有 0 满足这一属性
        if(x < 0 || (x % 10 == 0 && x != 0)) {
            return false;
        }

        int revertedNumber = 0;
        while(x > revertedNumber) {
            revertedNumber = revertedNumber * 10 + x % 10;
            x /= 10;
        }

        // 当数字长度为奇数时，我们可以通过 revertedNumber/10 去除处于中位的数字。
        // 例如，当输入为 12321 时，在 while 循环的末尾我们可以得到 x = 12，revertedNumber = 123，
        // 由于处于中位的数字不影响回文（它总是与自己相等），所以我们可以简单地将其去除。
        return x == revertedNumber || x == revertedNumber/10;
    }
}*/

