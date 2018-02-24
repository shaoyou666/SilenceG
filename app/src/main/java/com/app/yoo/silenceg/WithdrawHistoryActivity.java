package com.app.yoo.silenceg;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.app.yoo.silenceg.GjbActivity.GJB_HOME_URL;
//提现历史
public class WithdrawHistoryActivity extends SlideBackActivity {

    private SharedPreferences settings;
    private Context mContext;
    private String cookieString;
    private ListView lv_withdrawHistory;
    private List<Map<String, Object>> list;
    private SQLiteDatabase db;
    private Button bt_refreshWithdrawHistory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_withdraw_history);
        mContext = WithdrawHistoryActivity.this;
        android.support.v7.app.ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        bt_refreshWithdrawHistory = (Button) findViewById(R.id.bt_refreshWithdrawHistory);
        settings = getSharedPreferences("settings", Context.MODE_PRIVATE);
        db = openOrCreateDatabase("result.db", Context.MODE_PRIVATE, null);
        db.execSQL("create table if not exists withdrawHistory(id integer primary key autoincrement," +
                "name varchar(50)," +
                "applyTime varchar(100)," +
                "actionTime varchar(100)," +
                "status varchar(50)," +
                /*"applyTime varchar(50)," +
                "walletType varchar(50)," +
                "name varchar(50)," +
                "account varchar(50)," +
                "money varchar(50)," +
                "actionTime varchar(50)," +
                "status varchar(50)," +
                "content varchar(50)," +*/
                "insertDate TimeStamp NOT NULL DEFAULT (datetime('now','localtime')))");
        //table withdrawHistory(id,applyTime,walletType,name,account,money,actionTime,status,remark,insertDate

        cookieString = settings.getString("cookieString", null);
        lv_withdrawHistory = (ListView) findViewById(R.id.lv_withdrawHistory);
        list = new ArrayList<Map<String, Object>>();

        loadListViewData();
        bt_refreshWithdrawHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new GetWithdrawHistory().execute(new String[0]);
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

    public class GetWithdrawHistory extends AsyncTask<String, String, String> {
        private ProgressDialog pd;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pd = new ProgressDialog(mContext);
            pd.setMessage("查询可提现金额...");
            pd.show();
        }

        @Override
        protected String doInBackground(String... params) {
            String result = null;
            result = getWebData(GJB_HOME_URL + "/ucenter/cash/detail");
            return result;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            if (s != null) {
                Document doc = Jsoup.parse(s);
                Elements tables = doc.getElementsByClass("datatable");
                if (tables.size() > 0) {
                    Element table = tables.get(0);
                    Elements trs = table.select("tr");
                    if (trs.size() > 1) {
                        for (int i = 1; i < trs.size(); i++) {
                            Elements tds = trs.get(i).select("td");
                            if (tds.size() > 7) {
                                //table withdrawHistory(id,applyTime,walletType,name,account,money,actionTime,status,remark,insertDate
                                //db.execSQL("insert into withdrawHistory(applyTime,walletType,name,account,money,actionTime,status,content) values(?,?,?,?,?,?,?,?)" ,
                                //        new String[]{tds.get(0).text(),tds.get(1).text(),tds.get(2).text(),tds.get(3).text(),
                                //                tds.get(4).text(),tds.get(5).text(),tds.get(6).text(),tds.get(7).text()});
                                String applyTime = tds.get(0).text();
                                String status = tds.get(6).text();
                                String actionTime = tds.get(5).text();
                                Cursor c = db.rawQuery("select count(*) from withdrawHistory where applyTime=?", new String[]{applyTime});
                                if (c.moveToNext()) {
                                    if (c.getInt(0) == 0) {
                                        db.execSQL("insert into withdrawHistory(name,applyTime,actionTime,status) values(?,?,?,?)",
                                                new String[]{tds.get(7).text(), tds.get(0).text(), tds.get(5).text(), tds.get(6).text()});
                                    } else {
                                        Cursor c2 = db.rawQuery("select count(*) from withdrawHistory where applyTime=? and actionTime=? and status=?", new String[]{applyTime,actionTime,status});
                                        if (c2.moveToNext()) {
                                            if (c2.getInt(0) == 0) {
                                                db.execSQL("update withdrawHistory set status=?,actionTime=? where applyTime=?",new String[]{status,actionTime,applyTime});
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            loadListViewData();
            pd.dismiss();
        }
    }

    public void loadListViewData() {
        list.clear();

        Cursor c = db.rawQuery("select name,applyTime,actionTime,status from withdrawHistory group by applyTime order by applyTime desc limit 0,50", null);
        List<Map<String, Object>> tagList = new ArrayList<Map<String, Object>>();
        int position = 0;
        while (c.moveToNext()) {
            Map<String, Object> map = new HashMap<String, Object>();
            map.put("name", c.getString(0));
            map.put("value", "申请时间：" + c.getString(1) + "\n处理时间：" + c.getString(2));
            map.put("date", c.getString(3));
            list.add(map);

            Map<String, Object> tagMap = new HashMap<String, Object>();
            String month = c.getString(1).substring(0, 7);
            String value = c.getString(0);
            value = value.replace("邦币提现", "");
            value = value.replace("元", "");
            if (tagList.size() > 0) {
                String m = (String) tagList.get(position).get("month");
                int v = Integer.parseInt(tagList.get(position).get("value").toString());
                tagMap.put("month", month);
                if (m.equals(month)) {
                    int vt = Integer.parseInt(value);
                    v = vt + v;
                    tagMap.put("value", v);
                    tagList.set(position, tagMap);
                } else {
                    tagMap.put("value", value);
                    tagList.add(tagMap);
                    position = tagList.size()-1;
                }
            } else {
                tagMap.put("month", month);
                tagMap.put("value", value);
                tagList.add(tagMap);
                position = tagList.size()-1;
            }
        }

        for(int i=0,j=0;i<list.size();i++) {
            if (tagList.size() > 0 && j<tagList.size()) {
                String tagMonth = tagList.get(j).get("month").toString();
                String tagValue = tagList.get(j).get("value").toString();
                String listMonth = list.get(i).get("value").toString().substring(5,12);
                if (tagMonth.equals(listMonth)) {
                    Map<String, Object> tagMap = new HashMap<String, Object>();
                    tagMap.put("name", "tag");
                    tagMap.put("value", "tag");
                    tagMap.put("date", tagMonth+"累计提现"+tagValue+"元");
                    list.add(i,tagMap);
                    j++;
                }
            }
        }
        /*
        SimpleAdapter adapter = new SimpleAdapter(mContext, list, R.layout.moneydetaillist,
                new String[]{"name", "value", "date"},
                new int[]{R.id.tv_detailName, R.id.tv_detailValue, R.id.tv_detailDate});
        lv_withdrawHistory.setAdapter(adapter);
        */
        MyAdapter adapter = new MyAdapter(mContext, list);
        lv_withdrawHistory.setAdapter(adapter);
    }

    public String getWebData(String url) {
        String result = null;
        try {
            // 请求的地址
            String spec2 = url;
            // 根据地址创建URL对象
            URL url2 = new URL(spec2);
            // 根据URL对象打开链接
            HttpURLConnection urlConnection2 = (HttpURLConnection) url2.openConnection();
            // 设置请求的方式
            urlConnection2.setRequestMethod("GET");
            //注意，把存在本地的cookie值加在请求头上
            urlConnection2.addRequestProperty("Cookie", cookieString);
            if (urlConnection2.getResponseCode() == 200) {
                // 获取响应的输入流对象
                InputStream is2 = urlConnection2.getInputStream();
                // 创建字节输出流对象
                ByteArrayOutputStream baos2 = new ByteArrayOutputStream();
                // 定义读取的长度
                int len2 = 0;
                // 定义缓冲区
                byte buffer2[] = new byte[1024];
                // 按照缓冲区的大小，循环读取
                while ((len2 = is2.read(buffer2)) != -1) {
                    // 根据读取的长度写入到os对象中
                    baos2.write(buffer2, 0, len2);
                }
                // 释放资源
                is2.close();
                baos2.close();
                // 返回字符串
                result = new String(baos2.toByteArray());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }
}
