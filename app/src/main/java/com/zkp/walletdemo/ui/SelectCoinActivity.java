package com.zkp.walletdemo.ui;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;

import com.zkp.walletdemo.R;
import com.zkp.walletdemo.commonAdapter.CommonAdapter;
import com.zkp.walletdemo.commonAdapter.ViewHolder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SelectCoinActivity extends AppCompatActivity {

    private ListView lv_coin;
    private List<Map<String, String>> list;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_coin);
        initView();
        initData();
    }

    private void initData() {
        list = new ArrayList<>();
        Map<String, String> map = new HashMap();
        map.put("coin", "ETH");
        map.put("coinId", "34190899187000");
        list.add(map);
        map = new HashMap<>();
        map.put("coin", "HEC");
        map.put("coinId", "5773162675373");
        list.add(map);

        setAdapter();
    }

    private void setAdapter() {

        CommonAdapter adapter = new CommonAdapter(this, list, R.layout.item_list) {
            @Override
            public void convert(ViewHolder helper, Object item, final int position) {
                helper.setText(R.id.coinName, list.get(position).get("coin"));
                helper.setText(R.id.coinId, list.get(position).get("coinId"));
                helper.getView(R.id.ll_item).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent intent = new Intent(SelectCoinActivity.this, MainActivity.class);
                        intent.putExtra("coinId", list.get(position).get("coinId"));
                        intent.putExtra("coin", list.get(position).get("coin"));
                        setResult(3, intent);
                        finish();
                    }
                });
            }
        };
        lv_coin.setAdapter(adapter);
    }

    private void initView() {
        lv_coin = findViewById(R.id.lv_coin);
    }
}
