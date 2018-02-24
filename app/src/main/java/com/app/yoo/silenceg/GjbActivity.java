package com.app.yoo.silenceg;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.channels.FileChannel;

import static android.os.Environment.getExternalStorageDirectory;

public class GjbActivity extends SlideBackActivity {

    public static final String GJB_HOME_URL = "http://www.flyzhuan.com";
    private EditText et_username, et_password;
    private Button bt_login, bt_getMoney, bt_refresh, bt_showDetail,bt_history,bt_withdraw,bt_withdrawHistory,bt_backupDB,bt_restoreDB;
    private TextView tv_status, tv_total, tv_getMoneyResult;
    private Context mContext;
    private String cookieString;
    private SharedPreferences settings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gjb);

        android.support.v7.app.ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        findView();
        settings = getSharedPreferences("settings", Context.MODE_PRIVATE);
        String homeData = settings.getString("homeData", null);
        tv_total.setText(homeData);
        mContext = GjbActivity.this;
        bt_login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String[] paras = new String[]{et_username.getText().toString(), et_password.getText().toString()};
                new LoginGjb().execute(paras);
            }
        });
        bt_getMoney.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new GetMoney().execute(new String[0]);
            }
        });
        bt_refresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new GetHomeData().execute(new String[0]);
                new GetData().execute(new String[0]);
            }
        });
        bt_showDetail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(mContext,MoneyDetailActivity.class);
                startActivity(i);
            }
        });
        bt_history.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(mContext, HistoryActivity.class);
                startActivity(i);
            }
        });
        bt_withdraw.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(mContext, WithdrawActivity.class);
                startActivity(i);
            }
        });
        bt_withdrawHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(mContext, WithdrawHistoryActivity.class);
                startActivity(i);
            }
        });
        bt_backupDB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(mContext).setMessage("是否备份数据库？")
                        .setNegativeButton("取消", null)
                        .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                new BackupTask(mContext).execute("backupDatabase");
                            }
                        }).show();
            }
        });
        bt_restoreDB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(mContext).setMessage("是否还原数据库？")
                        .setNegativeButton("取消", null)
                        .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                new BackupTask(mContext).execute("restroeDatabase");
                            }
                        }).show();
            }
        });
/*
        SildingFinishLayout mSildingFinishLayout = (SildingFinishLayout) findViewById(R.id.activity_gjb);
        mSildingFinishLayout.setOnSildingFinishListener(new SildingFinishLayout.OnSildingFinishListener() {
            @Override
            public void onSildingFinish() {
                GjbActivity.this.finish();
            }
        });
        LinearLayout ll_gjb = (LinearLayout) findViewById(R.id.ll_gjb);
        mSildingFinishLayout.setTouchView(ll_gjb);
*/
    }
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(0, R.anim.base_slide_right_out);
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

    public void findView() {
        et_username = (EditText) findViewById(R.id.et_username);
        et_password = (EditText) findViewById(R.id.et_password);
        bt_login = (Button) findViewById(R.id.bt_login);
        bt_getMoney = (Button) findViewById(R.id.bt_getmoney);
        bt_refresh = (Button) findViewById(R.id.bt_refresh);
        bt_showDetail = (Button) findViewById(R.id.bt_showDetail);
        bt_history = (Button) findViewById(R.id.bt_history);
        bt_withdraw = (Button) findViewById(R.id.bt_withdraw);
        tv_status = (TextView) findViewById(R.id.tv_status);
        tv_total = (TextView) findViewById(R.id.tv_total);
        tv_getMoneyResult = (TextView) findViewById(R.id.tv_getMoneyResult);
        et_username.setText("shaoyou666");
        et_password.setText("5165344");
        bt_withdrawHistory = (Button) findViewById(R.id.bt_gjb_withdraw_history);
        bt_backupDB = (Button) findViewById(R.id.bt_backupDB);
        bt_restoreDB = (Button) findViewById(R.id.bt_restoreDB);
    }

    //登陆账号
    public class LoginGjb extends AsyncTask<String, String, String> {
        private ProgressDialog pd;

        @Override
        protected String doInBackground(String... params) {
            String result = null;
            try {
                // 请求的地址
                String spec = GJB_HOME_URL + "/login";
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
                String data = "username=" + URLEncoder.encode(params[0], "UTF-8") +
                        "&password=" + URLEncoder.encode(params[1], "UTF-8") +
                        "&from=http://www.flyzhuan.com/";
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
                        .setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.3; WOW64; rv:27.0) Gecko/20100101 Firefox/27.0");

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
                    //保存Cookies
                    cookieString = urlConnection.getHeaderField("Set-Cookie");
                    SharedPreferences.Editor editor = settings.edit();
                    editor.putString("cookieString", cookieString);
                    editor.commit();
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
            pd.setMessage("正在登录挂机邦...");
            pd.show();
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            if (s != null) {
                //System.out.println(s);
                String username = et_username.getText().toString();
                if (s.contains(username)) {
                    tv_status.setText(username + "登录成功！");
                    bt_login.setText("重新登录");
                    bt_refresh.setVisibility(View.VISIBLE);
                    new GetHomeData().execute(new String[0]);
                }
            } else {
                Toast.makeText(mContext,"无法连接服务器！",Toast.LENGTH_SHORT).show();
            }
            pd.dismiss();
            new GetData().execute(new String[0]);
        }

    }

    //获取账号主页信息
    public class GetHomeData extends AsyncTask<String, String, String> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            tv_total.setText("正在查询信息...");
        }

        @Override
        protected String doInBackground(String... params) {
            String result = getWebData(GJB_HOME_URL + "/ucenter/my");
            return result;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            if (s != null) {
                Document doc = Jsoup.parse(s);
                Elements elements = doc.getElementsByClass("seller-mod-5").select("p");
                //System.out.println("System.out.println:"+elements.size()+elements.text().toString());
                if (elements.size() > 0) {
                    String strTotal = "可用积分：" + elements.get(0).text() + "\n可用邦币：" + elements.get(1).text();
                    tv_total.setText(strTotal);
                    SharedPreferences.Editor editor = settings.edit();
                    editor.putString("homeData", strTotal);
                    editor.commit();
                    bt_withdraw.setVisibility(View.VISIBLE);
                }
            } else {
                Toast.makeText(mContext,"无法连接服务器！",Toast.LENGTH_SHORT).show();
            }
        }
    }

    //获取待收工资信息
    public class GetData extends AsyncTask<String, String, String> {
        private ProgressDialog pd;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            tv_getMoneyResult.setText("正在查询待领工资...");
            bt_getMoney.setVisibility(View.GONE);
            bt_showDetail.setVisibility(View.GONE);
            pd = new ProgressDialog(mContext);
            pd.setMessage("正在查询待领工资...");
            pd.show();
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            if (s != null) {
                Document doc = Jsoup.parse(s);
                Element element = doc.getElementById("showList");
                Elements elements = element.select("p");
                if (elements.size() > 1) {
                    String str = elements.get(0).text() + elements.get(1).text();
                    tv_getMoneyResult.setText(str);
                    //bt_getMoney.setVisibility(View.VISIBLE);
                    bt_showDetail.setVisibility(View.VISIBLE);
                } else {
                    tv_getMoneyResult.setText("查无可领工资记录！");
                    bt_getMoney.setVisibility(View.GONE);
                    bt_showDetail.setVisibility(View.GONE);
                }
            } else {
                Toast.makeText(mContext,"无法连接服务器！",Toast.LENGTH_SHORT).show();
            }
            pd.dismiss();
        }

        @Override
        protected String doInBackground(String... params) {
            String result = getWebData(GJB_HOME_URL + "/dama/detail_93");
            return result;
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
                        tv_getMoneyResult.setText(result);
                        new GetHomeData().execute(new String[0]);
                    }
                }
                bt_getMoney.setVisibility(View.GONE);
                bt_showDetail.setVisibility(View.GONE);
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

    public class BackupTask extends AsyncTask<String, Void, String> {
        private static final String COMMAND_BACKUP = "backupDatabase";
        public static final String COMMAND_RESTORE = "restroeDatabase";
        private Context mContext;

        public BackupTask(Context context) {
            this.mContext = context;
        }

        @Override
        protected String doInBackground(String... params) {
            // TODO Auto-generated method stub
            String result = null;
            // 获得正在使用的数据库路径，我的是 sdcard 目录下的 /dlion/db_dlion.db
            // 默认路径是 /data/data/(包名)/databases/*.db
            //File dbFile = mContext.getDatabasePath(Environment
            //        .getExternalStorageDirectory().getAbsolutePath()
            //        + "/result.db");
            File dbFile = mContext.getDatabasePath("result.db");
            File exportDir = new File(getExternalStorageDirectory(),
                    "GJBBackup");
            if (!exportDir.exists()) {
                exportDir.mkdirs();
            }
            File backup = new File(exportDir, dbFile.getName());
            String command = params[0];
            if (command.equals(COMMAND_BACKUP)) {
                try {
                    backup.createNewFile();
                    fileCopy(dbFile, backup);
                    result = "备份成功！备份路径："+backup.getAbsolutePath();
                    return result;
                } catch (Exception e) {
                    // TODO: handle exception
                    e.printStackTrace();
                    result = "备份失败！！！";
                    return result;
                }
            } else if (command.equals(COMMAND_RESTORE)) {
                try {
                    fileCopy(backup, dbFile);
                    result = "恢复成功！";
                    return result;
                } catch (Exception e) {
                    // TODO: handle exception
                    e.printStackTrace();
                    result = "恢复失败！！！";
                    return result;
                }
            } else {
                return null;
            }
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            if (result != null) {
                Toast.makeText(mContext,result,Toast.LENGTH_LONG).show();
            }
        }

        private void fileCopy(File dbFile, File backup) throws IOException {
            // TODO Auto-generated method stub
            FileChannel inChannel = new FileInputStream(dbFile).getChannel();
            FileChannel outChannel = new FileOutputStream(backup).getChannel();
            try {
                inChannel.transferTo(0, inChannel.size(), outChannel);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } finally {
                if (inChannel != null) {
                    inChannel.close();
                }
                if (outChannel != null) {
                    outChannel.close();
                }
            }
        }
    }

}
