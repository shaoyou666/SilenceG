package com.app.yoo.silenceg;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
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
//待领工资明细
public class MoneyDetailActivity extends SlideBackActivity {

    private SharedPreferences settings;
    private Context mContext;
    private String cookieString;
    private ListView lv_moneyDetail;
    private List<Map<String,Object>> list;
    private SQLiteDatabase db;
    private Button bt_getWaitMoney;
    private TextView tv_noMoneyWaitToGet;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_money_detail);

        android.support.v7.app.ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        mContext = MoneyDetailActivity.this;
        settings = getSharedPreferences("settings", Context.MODE_PRIVATE);
        cookieString = settings.getString("cookieString", null);
        lv_moneyDetail = (ListView) findViewById(R.id.lv_moneydetail);
        bt_getWaitMoney = (Button) findViewById(R.id.bt_getWaitMoney);
        tv_noMoneyWaitToGet = (TextView) findViewById(R.id.tv_noMoneyWaitToGet);
        list = new ArrayList<Map<String,Object>>();
        db = openOrCreateDatabase("result.db",Context.MODE_PRIVATE,null);
        db.execSQL("create table if not exists moneyDetail(id integer primary key autoincrement," +
                "name varchar(50)," +
                "value varchar(50)," +
                "date varchar(50)," +
                "insertDate TimeStamp NOT NULL DEFAULT (datetime('now','localtime')))");
        new GetMoneyDetail().execute(new String[0]);
        bt_getWaitMoney.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bt_getWaitMoney.setText("正在领取工资");
                bt_getWaitMoney.setClickable(false);
                bt_getWaitMoney.setTextColor(Color.GRAY);
                new GetMoney().execute(new String[0]);
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

    public class GetMoneyDetail extends AsyncTask<String, String, String>{
        private ProgressDialog pd;
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pd = new ProgressDialog(mContext);
            pd.setMessage("正在加载数据...");
            pd.show();
        }

        @Override
        protected String doInBackground(String... params) {
            String result = null;
            result = getWebData(GJB_HOME_URL + "/dama/index/datareport/t/1");
            return result;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            if (s != null) {
                list.clear();
                Document doc = Jsoup.parse(s);
                Elements elements = doc.getElementsByClass("table-hover");
                if (elements.size() > 0) {
                    Elements trs = elements.select("tr");
                    if (trs.size() > 1) {
                        bt_getWaitMoney.setVisibility(View.VISIBLE);
                        for (int i = 1; i < trs.size(); i++) {
                            Elements tds = trs.get(i).select("td");
                            //System.out.println("tds.size()"+tds.size());
                            if (tds.size() > 6) {
                                String name = tds.get(1).text().trim() + "(" + tds.get(2).text().trim() + ")";
                                String value = tds.get(7).text().trim();
                                String date = tds.get(3).text().trim();
                                //System.out.println("name:" + name + ",value:" + value + ",date:" + date);
                                Map<String, Object> map = new HashMap<String, Object>();
                                map.put("name", name);
                                map.put("value", value);
                                map.put("date", date);
                                list.add(map);
                                System.out.println("list.add(map);");
                                Cursor c = db.rawQuery("select * from moneyDetail where name=? and value=? and date=?", new String[]{name, value, date});
                                if (c.getCount() == 0) {
                                    db.execSQL("insert into moneyDetail(name,value,date) values(?,?,?)", new String[]{name, value, date});
                                }
                                c.close();
                            }
                        }
                        SimpleAdapter adapter = new SimpleAdapter(mContext, list, R.layout.moneydetaillist,
                                new String[]{"name", "value", "date"},
                                new int[]{R.id.tv_detailName, R.id.tv_detailValue, R.id.tv_detailDate});
                        lv_moneyDetail.setAdapter(adapter);
                    } else {
                        tv_noMoneyWaitToGet.setVisibility(View.VISIBLE);
                        bt_getWaitMoney.setVisibility(View.GONE);
                    }
                }
            }else{
                Toast.makeText(mContext,"无法连接服务器！",Toast.LENGTH_SHORT).show();
            }
            pd.dismiss();
            db.close();
        }
    }

    //领取工资
    public class GetMoney extends AsyncTask<String, String, String> {
        private ProgressDialog pd;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pd = new ProgressDialog(mContext);
            pd.setMessage("正在领取工资...");
            pd.show();
        }

        @Override
        protected String doInBackground(String... params) {
            String result = getWebData(GJB_HOME_URL + "/dama/index/doalldaliy");
            return result;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            if (s != null) {
                if (s.contains("领取成功")) {
                    Document doc = Jsoup.parse(s);
                    Elements elements = doc.getElementsByClass("with-icon");
                    if (elements.size() > 0) {
                        String result = elements.get(0).text();
                        Toast.makeText(mContext, result, Toast.LENGTH_LONG).show();
                        //new GetMoneyDetail().execute(new String[0]);
                        tv_noMoneyWaitToGet.setText(result);
                        tv_noMoneyWaitToGet.setVisibility(View.VISIBLE);
                        bt_getWaitMoney.setVisibility(View.GONE);
                        lv_moneyDetail.setAdapter(null);
                    }
                }
            } else {
                Toast.makeText(mContext,"无法连接服务器！",Toast.LENGTH_SHORT).show();
            }
            pd.dismiss();
        }
    }

    public String getWebData(String url){
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
