package com.zkp.walletdemo.ui;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zkp.walletdemo.Device;
import com.zkp.walletdemo.Encryptor;
import com.zkp.walletdemo.Header;
import com.zkp.walletdemo.Object2Json;
import com.zkp.walletdemo.ProtocolConfigSeparate;
import com.zkp.walletdemo.R;
import com.zkp.walletdemo.platform.Base64Comp;
import com.zkp.walletdemo.rsa.DCPEncryptor;
import com.zkp.walletdemo.utils.AndroidUtils;
import com.zkp.walletdemo.utils.OnLazyClickListener;
import com.zkp.walletdemo.utils.network.NetResult;
import com.zkp.walletdemo.utils.network.Post;

import org.json.JSONObject;

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

    // 模拟应用后台返回签名好的订单信息（实际功能由你们的后台服务器实现）请求接口
    private static final String URL_PREPAY = "http://192.168.1.223:8101/dcpayCore/payBills/prepay";
    //    private static final String URL_PREPAY = "http://47.52.175.22/dcpayCore/payBills/prepay";
    private View button1;
    private String headerString;
    private HashMap<String, Object> bodyEncryptMap;
    private Map<String, String> myMap;
    private Device device;
    //订单号随机号
    private int num = (int) (Math.random() * 8998) + 1000 + 1;

    private static final String KEY_BUNDLE = "PAY_REQUEST_PARAMS";
    private static final String KEY_BUNDLE_CALLBACK_NAME = "n";
    private EditText et_remark;
    private TextView tv_number;
    private EditText et_amount;
    private TextView tv_select;
    private String coinId;
    private String coin;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initViews();
        initListeners();
    }

    private void initViews() {
        et_remark = findViewById(R.id.et_remark);
        tv_number = findViewById(R.id.tv_number);
        tv_number.setText("0000008" + num);
        et_amount = findViewById(R.id.et_amount);
        tv_select = findViewById(R.id.tv_select);

        button1 = findViewById(R.id.include1);
        //请求头部的封装
        mHeadMap = new HashMap<>();
        mHeadMap.put("Accept", new ArrayList<String>(Arrays.asList("application/json")));
        mHeadMap.put("Content-Type", new ArrayList<String>(Arrays.asList("application/json")));
    }


    private void initListeners() {
        tv_select.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivityForResult(new Intent(MainActivity.this, SelectCoinActivity.class), 2);
            }
        });


        button1.setOnClickListener(new OnLazyClickListener() {
            @Override
            public void onLazyClick(View view) {
                num = (int) (Math.random() * 8998) + 1000 + 1;
                if (TextUtils.isEmpty(tv_select.getText().toString().trim())) {
                    Toast.makeText(MainActivity.this, "请选择币种!", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (TextUtils.isEmpty(et_amount.getText().toString())) {
                    Toast.makeText(MainActivity.this, "请输入数量!", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (TextUtils.isEmpty(et_remark.getText().toString())) {
                    Toast.makeText(MainActivity.this, "请添加备注!", Toast.LENGTH_SHORT).show();
                    return;
                }
                pay(); // 开始支付

            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data == null) {
            return;
        } else {
            if (requestCode == 2 && resultCode == 3) {
                coinId = data.getStringExtra("coinId");
                coin = data.getStringExtra("coin");

                tv_select.setText(coin);
                Log.i("tag", coinId);
            }
        }
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

    //请求参数的封装
    public String postString() {
        return Object2Json.map2Json(post());
    }

    public HashMap<String, Object> post() {
        //请求参数 存到hashMap中
        Log.i("coinid", coinId);
        myMap = new HashMap<>();
        myMap.put("amount", et_amount.getText().toString());
        myMap.put("attach", et_remark.getText().toString());
        myMap.put("coinId", coinId);
        myMap.put("goodsTag", "coin");
        myMap.put("goodsType", "ETH");
        myMap.put("industry", "fubt");
        myMap.put("refBizNo", tv_number.getText().toString());

        mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        //请求头部header

        try {
            HashMap<String, Object> bodyEncryptMap = DCPEncryptor.encrypt(mapper.writeValueAsString(myMap),
                    ProtocolConfigSeparate.mbr_public_key,
                    ProtocolConfigSeparate.private_key);
            //设备信息
            device = new Device();
            device.deviceId = AndroidUtils.getAndroidId(this);
            device.appVersion = AndroidUtils.getVersionName(this);
            device.pushId = "213231";
            device.system = "Android";
            device.language = "zh_cn";
            device.packageName = AndroidUtils.getPackageName(this);
            device.channel = ProtocolConfigSeparate.channel;
            // 头部header 参数赋值
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
                //获取订单信息
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
     * 携带订单信息
     * 跳转到钱包APP
     *
     * @param orderInfo
     */
    private void callPaySdk(String orderInfo) {

        Intent intent = new Intent();
        Bundle bundle = new Bundle();
        bundle.putString(KEY_BUNDLE, orderInfo);
        bundle.putString(KEY_BUNDLE_CALLBACK_NAME, KEY_BUNDLE_CALLBACK_NAME);
        intent.putExtras(bundle);
        intent.setAction("com.mbr.coin.pay");
        startActivity(intent);
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

}


