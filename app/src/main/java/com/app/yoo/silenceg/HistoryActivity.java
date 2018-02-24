package com.app.yoo.silenceg;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Spinner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HistoryActivity extends SlideBackActivity {

    private ListView lv_historyDetail;
    private Spinner sp_name,sp_date;
    private List<Map<String,Object>> list;
    private SQLiteDatabase db;
    private List<String> nameList,dateList;
    private Context mContext;
    private SimpleAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);
        mContext = HistoryActivity.this;
        android.support.v7.app.ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        lv_historyDetail = (ListView) findViewById(R.id.lv_historyDetail);
        sp_name = (Spinner) findViewById(R.id.sp_type);
        sp_date = (Spinner) findViewById(R.id.sp_date);
        list = new ArrayList<Map<String, Object>>();
        nameList = new ArrayList<String>();
        dateList = new ArrayList<String>();
        db = openOrCreateDatabase("result.db", Context.MODE_PRIVATE,null);
        db.execSQL("create table if not exists moneyDetail(id integer primary key autoincrement," +
                "name varchar(50)," +
                "value varchar(50)," +
                "date varchar(50)," +
                "insertDate TimeStamp NOT NULL DEFAULT (datetime('now','localtime')))");
        Cursor c = db.rawQuery("select name,value,date from moneyDetail order by name,date desc",new String[0]);
        while (c.moveToNext()) {
            Map<String, Object> map = new HashMap<String,Object>();
            map.put("name", c.getString(0));
            map.put("value", c.getString(1));
            map.put("date", c.getString(2));
            list.add(map);
        }
        Map<String, Object> map_temp = new HashMap<String, Object>();
        map_temp.put("name", "统计");
        map_temp.put("value", getCount(list));
        map_temp.put("date", null);
        list.add(0,map_temp);
        Cursor c_name = db.rawQuery("select name from moneyDetail group by name", new String[0]);
        nameList.add("全部类型");
        while (c_name.moveToNext()) {
            nameList.add(c_name.getString(0));
        }
        Cursor c_date = db.rawQuery("select date from moneyDetail group by date order by date desc", new String[0]);
        dateList.add("全部日期");
        while (c_date.moveToNext()) {
            dateList.add(c_date.getString(0));
        }
        adapter = new SimpleAdapter(HistoryActivity.this,
                list,
                R.layout.moneydetaillist,
                new String[] {"name","value","date"},
                new int[]{R.id.tv_detailName,R.id.tv_detailValue,R.id.tv_detailDate});
        lv_historyDetail.setAdapter(adapter);
        if (nameList.size() > 0) {
            ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(HistoryActivity.this,android.R.layout.simple_spinner_item,nameList);
            sp_name.setAdapter(arrayAdapter);
            //sp_name.
        }
        if (dateList.size() > 0) {
            ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(HistoryActivity.this,android.R.layout.simple_spinner_item,dateList);
            sp_date.setAdapter(arrayAdapter);
        }
        db.close();
        sp_name.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position > 0) {
                    String nameSelected = nameList.get(position);
                    List<Map<String, Object>> list_temp = new ArrayList<Map<String, Object>>();
                    for (int i = 0; i < list.size(); i++) {
                        if (list.get(i).get("name").equals(nameSelected)) {
                            list_temp.add(list.get(i));
                        }
                    }
                    Map<String, Object> map_temp = new HashMap<String, Object>();
                    map_temp.put("name", "统计");
                    map_temp.put("value", getCount(list_temp));
                    map_temp.put("date", null);
                    list_temp.add(0,map_temp);
                    SimpleAdapter adapter_temp = new SimpleAdapter(HistoryActivity.this,
                            list_temp,
                            R.layout.moneydetaillist,
                            new String[]{"name", "value", "date"},
                            new int[]{R.id.tv_detailName, R.id.tv_detailValue, R.id.tv_detailDate});
                    lv_historyDetail.setAdapter(adapter_temp);
                } else {
                    lv_historyDetail.setAdapter(adapter);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        sp_date.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position > 0) {
                    String dateSelected = dateList.get(position);
                    List<Map<String, Object>> list_temp = new ArrayList<Map<String, Object>>();
                    for (int i = 0; i < list.size(); i++) {
                        if (list.get(i).get("date") != null) {
                            if (list.get(i).get("date").equals(dateSelected)) {
                                list_temp.add(list.get(i));
                            }
                        }
                    }
                    Map<String, Object> map_temp = new HashMap<String, Object>();
                    map_temp.put("name", "统计");
                    map_temp.put("value", getCount(list_temp));
                    map_temp.put("date", null);
                    list_temp.add(0,map_temp);
                    SimpleAdapter adapter_temp = new SimpleAdapter(HistoryActivity.this,
                            list_temp,
                            R.layout.moneydetaillist,
                            new String[]{"name", "value", "date"},
                            new int[]{R.id.tv_detailName, R.id.tv_detailValue, R.id.tv_detailDate});
                    lv_historyDetail.setAdapter(adapter_temp);
                } else {
                    lv_historyDetail.setAdapter(adapter);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                this.finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public String getCount(List<Map<String, Object>> li) {
        String result = null;
        int size = li.size();
        //String name = null;
        float count = 0;
        if (size > 0) {
            //name = li.get(0).get("name").toString();
            for(int i=0;i<size;i++) {
                String str = li.get(i).get("value").toString();
                if (str.contains("≈")) {
                    str = str.substring(str.indexOf("≈")+1, str.indexOf("元"));
                    count = count + Float.parseFloat(str);
                }
            }
        }
        result = "累计"+count+"元，记录数：" + size;
        return result;
    }
}
