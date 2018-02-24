package com.app.yoo.silenceg;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.app.yoo.silenceg.R.id.tv_result;

public class MainActivity extends SlideBackActivity {

    private Context mContext;
    private EditText et_value;
    private Button  bt_search,bt_gjb;
    private ListView lv_result;
    private List<Map<String,Object>> list;
    private SimpleAdapter adapter;
    private SQLiteDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mContext = MainActivity.this;
        et_value = (EditText)findViewById(R.id.et_value);
        et_value.setText("AG-19380");
        et_value.clearFocus();
        bt_search = (Button)findViewById(R.id.bt_search);
        bt_gjb = (Button)findViewById(R.id.bt_gjb);
        lv_result = (ListView)findViewById(R.id.lv_result);
        list = new ArrayList<Map<String,Object>>();

        db = openOrCreateDatabase("result.db",Context.MODE_PRIVATE,null);
        db.execSQL("create table if not exists records(id integer primary key autoincrement,result varchar(200),sdate TimeStamp NOT NULL DEFAULT (datetime('now','localtime')))");
        db.execSQL("create table if not exists recordData(id integer primary key autoincrement,data1 varchar(50),data2 varchar(50),sdate TimeStamp NOT NULL DEFAULT (datetime('now','localtime')))");
        Cursor c = db.rawQuery("select * from records order by id desc limit 0,20",null);
        while (c.moveToNext()){
            String record = c.getString(1);
            String date = c.getInt(0) + ":" + c.getString(2);
            int id = c.getInt(0);
            Map<String,Object> map = new HashMap<String,Object>();
            map.put("result",record);
            map.put("date",date);
            map.put("id", id);
            list.add(map);
        }

        adapter = new SimpleAdapter(mContext,list,R.layout.resultlist,
                new String[] {"result","date"},
                new int[]{R.id.tv_result,R.id.tv_date});
        lv_result.setAdapter(adapter);

        bt_search.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String value = et_value.getText().toString();
                new GetData().execute(new String[]{value});
            }
        });

        bt_gjb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(mContext,GjbActivity.class);
                startActivity(i);
                //overridePendingTransition(R.anim.base_slide_right_in,R.anim.base_slide_right_out);
            }
        });
        lv_result.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final int p = position;
                new AlertDialog.Builder(mContext).setMessage("是否删除选中记录？")
                        .setNegativeButton("取消", null)
                        .setPositiveButton("删除", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                String sid = list.get(p).get("id").toString();
                                int id = Integer.parseInt(sid);
                                list.remove(p);
                                db.execSQL("delete from records where id=" + id );
                                adapter.notifyDataSetChanged();
                            }
                        }).show();
            }
        });
        lv_result.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                new AlertDialog.Builder(mContext)
                        .setMessage("是否清空所有记录？")
                        .setNegativeButton("取消", null)
                        .setPositiveButton("清空", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                list.clear();
                                db.execSQL("delete from records");
                                adapter.notifyDataSetChanged();
                            }
                        }).show();
                return false;
            }
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(0,R.anim.base_slide_right_out);
    }

    public class GetData extends AsyncTask<String, String, String> {

        private ProgressDialog pd;

        @Override
        protected String doInBackground(String... params) {
            String result = null;
            try {
                // 请求的地址
                String spec = "http://www.njweixin.com/regdo.php";
                // 根据地址创建URL对象
                URL url = new URL(spec);
                // 根据URL对象打开链接
                HttpURLConnection urlConnection = (HttpURLConnection) url
                        .openConnection();
                // 设置请求的方式
                urlConnection.setRequestMethod("POST");
                // 设置请求的超时时间
                urlConnection.setReadTimeout(5000);
                urlConnection.setConnectTimeout(5000);
                // 传递的数据
                String data = "type=search&username=" + URLEncoder.encode(params[0], "UTF-8");
                // 设置请求的头
                urlConnection.setRequestProperty("Connection", "keep-alive");
                // 设置请求的头
                urlConnection.setRequestProperty("Content-Type",
                        "application/x-www-form-urlencoded");
                // 设置请求的头
                urlConnection.setRequestProperty("Content-Length",
                        String.valueOf(data.getBytes().length));
                // 设置请求的头
                urlConnection
                        .setRequestProperty("User-Agent",
                                "Mozilla/5.0 (Windows NT 6.3; WOW64; rv:27.0) Gecko/20100101 Firefox/27.0");

                urlConnection.setDoOutput(true); // 发送POST请求必须设置允许输出
                urlConnection.setDoInput(true); // 发送POST请求必须设置允许输入
                //setDoInput的默认值就是true
                //获取输出流
                OutputStream os = urlConnection.getOutputStream();
                os.write(data.getBytes());
                os.flush();
                if (urlConnection.getResponseCode() == 200) {
                    // 获取响应的输入流对象
                    InputStream is = urlConnection.getInputStream();
                    // 创建字节输出流对象
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    // 定义读取的长度
                    int len = 0;
                    // 定义缓冲区
                    byte buffer[] = new byte[1024];
                    // 按照缓冲区的大小，循环读取
                    while ((len = is.read(buffer)) != -1) {
                        // 根据读取的长度写入到os对象中
                        baos.write(buffer, 0, len);
                    }
                    // 释放资源
                    is.close();
                    baos.close();
                    // 返回字符串
                    result = new String(baos.toByteArray());


                } else {
                    System.out.println("链接失败.........");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }


            return result;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pd = new ProgressDialog(mContext);
            pd.setMessage("正在加载...");
            pd.show();
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            if (s != null) {
                String[] strs = s.split(":");
                if(strs.length>3){
                    Toast.makeText(mContext, "ID："+strs[3]+"，当前分数："+strs[1]+"分", Toast.LENGTH_SHORT).show();
                    db.execSQL("insert into records(result) values('ID："+strs[3]+"，当前分数："+strs[1]+"分')");
                    db.execSQL("insert into recordData(data1,data2) values('"+strs[3]+"','"+strs[1]+"')");
                    list.clear();
                    Cursor c = db.rawQuery("select * from records order by id desc limit 0,20",null);
                    while (c.moveToNext()){
                        String record = c.getString(1);
                        String date = c.getInt(0) + ":" + c.getString(2);
                        int id = c.getInt(0);
                        Map<String,Object> map = new HashMap<String,Object>();
                        map.put("result",record);
                        map.put("date",date);
                        map.put("id", id);
                        list.add(map);
                        SimpleAdapter adapter = new SimpleAdapter(mContext,list,R.layout.resultlist,
                                new String[] {"result","date"},
                                new int[]{R.id.tv_result,R.id.tv_date});
                        lv_result.setAdapter(adapter);

                    }
                }else {
                    Toast.makeText(mContext,"查无数据！",Toast.LENGTH_SHORT).show();
                }
            }else{
                Toast.makeText(mContext,"无法连接服务器！",Toast.LENGTH_SHORT).show();
            }
            pd.dismiss();
        }

    }
}
