package com.app.yoo.silenceg;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;
import java.util.Map;

/**
 * Created by csyoo on 2017/3/20.
 */

public class MyAdapter extends BaseAdapter {
    private List<Map<String,Object>> listData;
    private LayoutInflater layoutInflater;

    public MyAdapter(Context context, List<Map<String, Object>> listData) {
        this.layoutInflater = LayoutInflater.from(context);
        this.listData = listData;
    }

    @Override
    public boolean isEnabled(int position) {
        if (listData.get(position).get("value").equals("tag")) {
            return false;
        }
        return super.isEnabled(position);
    }

    @Override
    public int getCount() {
        return listData.size();
    }

    @Override
    public Object getItem(int position) {
        return listData.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (listData.get(position).get("name").equals("tag")) {
            convertView = layoutInflater.inflate(R.layout.detail_list_tag, null);
            TextView tv_tag = (TextView) convertView.findViewById(R.id.tv_tag);
            tv_tag.setText(listData.get(position).get("date").toString());
        } else {
            convertView = layoutInflater.inflate(R.layout.moneydetaillist, null);
            TextView tv_name = (TextView) convertView.findViewById(R.id.tv_detailName);
            TextView tv_value = (TextView) convertView.findViewById(R.id.tv_detailValue);
            TextView tv_date = (TextView) convertView.findViewById(R.id.tv_detailDate);
            tv_name.setText(listData.get(position).get("name").toString());
            tv_value.setText(listData.get(position).get("value").toString());
            tv_date.setText(listData.get(position).get("date").toString());
        }
        return convertView;
    }
}
