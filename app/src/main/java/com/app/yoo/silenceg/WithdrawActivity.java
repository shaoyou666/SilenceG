package com.app.yoo.silenceg;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
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
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.app.yoo.silenceg.GjbActivity.GJB_HOME_URL;

public class WithdrawActivity extends SlideBackActivity {

    private String cookieString;
    private SharedPreferences settings;
    private Context mContext;
    private TextView tv_canWithdraw,tv_email;
    private EditText et_num,et_verifyCode, et_safeCode;
    private Button bt_sendVer,bt_withdraw,bt_withdrawHistory;
    private String email,uid,purseid,money,verify,safecode;
    private int canWithdraw;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_withdraw);

        android.support.v7.app.ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        mContext = WithdrawActivity.this;
        settings = getSharedPreferences("settings", Context.MODE_PRIVATE);
        cookieString = settings.getString("cookieString", null);
        finView();
        et_safeCode.setText("516534");
        new GetWithdrawData().execute(new String[0]);
        bt_sendVer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bt_sendVer.setText("已发送");
                bt_sendVer.setClickable(false);
                bt_sendVer.setTextColor(Color.GRAY);
                new SendVerifyCode().execute(new String[0]);
            }
        });
        bt_withdraw.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                money = et_num.getText().toString();
                verify = et_verifyCode.getText().toString();
                safecode = et_safeCode.getText().toString();
                if (money == null || money.equals("")) {
                    Toast.makeText(mContext,"提现金额不能为空！",Toast.LENGTH_SHORT).show();
                    et_num.requestFocus();
                } else if (verify == null || verify.equals("")) {
                    Toast.makeText(mContext,"请输入验证码！",Toast.LENGTH_SHORT).show();
                    et_verifyCode.requestFocus();
                } else if (safecode == null || safecode.equals("")) {
                    Toast.makeText(mContext, "请输入安全码！", Toast.LENGTH_SHORT).show();
                    et_safeCode.requestFocus();
                } else {
                    int n = Integer.parseInt(money);
                    if (n > canWithdraw) {
                        Toast.makeText(mContext, "提现金额不能大于可提现金额！", Toast.LENGTH_SHORT).show();
                    }else{
                        new Withdraw().execute(new String[0]);
                    }
                }
            }
        });
        bt_withdrawHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(mContext, WithdrawHistoryActivity.class);
                startActivity(i);
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

    public void finView() {
        tv_canWithdraw = (TextView) findViewById(R.id.tv_canWithdraw);
        et_num = (EditText) findViewById(R.id.et_num);
        et_verifyCode = (EditText) findViewById(R.id.et_verifyCodde);
        et_safeCode = (EditText) findViewById(R.id.et_safeCode);
        bt_sendVer = (Button) findViewById(R.id.bt_sendVer);
        bt_withdraw = (Button) findViewById(R.id.bt_withdraw);
        tv_email = (TextView) findViewById(R.id.tv_email);
        bt_withdrawHistory = (Button) findViewById(R.id.bt_withdrawHistory);
    }
    public class GetWithdrawData extends AsyncTask<String, String, String>{
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
            result = getWebData(GJB_HOME_URL + "/ucenter/cash");
            return result;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            if (s != null) {
                Document doc = Jsoup.parse(s);
                Element span = doc.getElementById("con_two_2");
                if (span != null) {
                    String[] strNums = span.text().split("，");
                    tv_canWithdraw.setText(strNums[1]);
                    String regEx = "[^0-9]";
                    Pattern p = Pattern.compile(regEx);
                    Matcher matcher = p.matcher(strNums[1]);
                    String temp = matcher.replaceAll("").trim();
                    canWithdraw = Integer.parseInt(temp);
                    et_num.setText(temp);
                    if (canWithdraw == 0) {
                        bt_withdraw.setClickable(false);
                        bt_withdraw.setTextColor(Color.GRAY);
                        bt_sendVer.setClickable(false);
                        bt_sendVer.setTextColor(Color.GRAY);
                    }
                    /*
                    if (matcher.find()) {
                        String temp = matcher.group();
                        canWithdraw = Integer.parseInt(temp);
                        et_num.setText(temp);
                        if (canWithdraw == 0) {
                            bt_withdraw.setClickable(false);
                            bt_sendVer.setTextColor(Color.GRAY);
                        }
                    }*/
                }
                Element form = doc.getElementById("form1");
                Elements inputs = form.getElementsByTag("input");
                Elements options = form.getElementsByTag("option");
                if (inputs.size() > 0) {
                   for(int i=0;i<inputs.size();i++) {
                       if (inputs.get(i).attr("name").equals("email")) {
                           email = inputs.get(i).attr("value");
                       }
                       if (inputs.get(i).attr("name").equals("uid")) {
                           uid = inputs.get(i).attr("value");
                       }
                       //System.out.println(inputs.get(i).attr("name") + "=" + inputs.get(i).attr("value"));
                   }
                }
                if (options.size() > 0) {
                    purseid =options.get(0).attr("value");
                }
            } else {
                Toast.makeText(mContext,"无法连接服务器！",Toast.LENGTH_SHORT).show();
            }
            tv_email.setText(email);
            pd.dismiss();
        }
    }
    public class SendVerifyCode extends AsyncTask<String, String, String>{

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(String... params) {
            String result = null;
            try {
                // 请求的地址
                String spec = GJB_HOME_URL + "/ucenter/verify/sendverify";
                // 根据地址创建URL对象
                URL url = new URL(spec);
                // 根据URL对象打开链接
                HttpURLConnection urlConnection = (HttpURLConnection) url
                        .openConnection();
                // 设置请求的方式
                urlConnection.setRequestMethod("POST");
                urlConnection.addRequestProperty("Cookie", cookieString);
                // 设置请求的超时时间
                urlConnection.setReadTimeout(5000);
                urlConnection.setConnectTimeout(5000);
                // 传递的数据
                String data = "account=" + URLEncoder.encode("shaoyou666@126.com", "UTF-8") +
                        "&type=" + URLEncoder.encode("email", "UTF-8") +
                        "&action=config";
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
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            if (s != null) {
                if (s.contains("发送成功")) {
                    bt_sendVer.setText("发送成功");
                    TimeCount timeCount = new TimeCount(60000, 1000);
                    timeCount.start();
                }
            } else {
                Toast.makeText(mContext,"无法连接服务器！",Toast.LENGTH_SHORT).show();
            }
        }
    }
    public class Withdraw extends AsyncTask<String, String, String>{
        private ProgressDialog pd;
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pd = new ProgressDialog(mContext);
            pd.setMessage("申请提现...");
            pd.show();
        }

        @Override
        protected String doInBackground(String... params) {
            String result = null;
            try {
                // 请求的地址
                String spec = GJB_HOME_URL + "/ucenter/cash";
                // 根据地址创建URL对象
                URL url = new URL(spec);
                // 根据URL对象打开链接
                HttpURLConnection urlConnection = (HttpURLConnection) url
                        .openConnection();
                // 设置请求的方式
                urlConnection.setRequestMethod("POST");
                //注意，把存在本地的cookie值加在请求头上
                urlConnection.addRequestProperty("Cookie", cookieString);
                // 设置请求的超时时间
                urlConnection.setReadTimeout(5000);
                urlConnection.setConnectTimeout(5000);
                // 传递的数据
                String data = "tp=" + URLEncoder.encode("1", "UTF-8") +
                        "&vt=" + URLEncoder.encode("2", "UTF-8") +
                        "&uid=" + URLEncoder.encode(uid, "UTF-8") +
                        "&type=" + URLEncoder.encode("email", "UTF-8") +
                        "&money=" + URLEncoder.encode(money, "UTF-8") +
                        "&email=" + URLEncoder.encode(email, "UTF-8") +
                        "&verify=" + URLEncoder.encode(verify, "UTF-8") +
                        "&safecode=" + URLEncoder.encode(safecode, "UTF-8") +
                        "&purseid=" + URLEncoder.encode(purseid, "UTF-8");
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
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            if (s != null) {
                Document doc = Jsoup.parse(s);
                Elements elements = doc.getElementsByClass("alert");
                if (elements.size() > 0) {
                    Toast.makeText(mContext,elements.get(0).text(),Toast.LENGTH_LONG).show();
                    if (elements.get(0).text().contains("成功")) {
                        new GetWithdrawData().execute(new String[0]);
                    }
                }
                //System.out.println(s);
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
    public class TimeCount extends CountDownTimer {
        public TimeCount(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);
        }

        @Override
        public void onFinish() {
            bt_sendVer.setText("重新发送");
            if (canWithdraw != 0) {
                bt_sendVer.setClickable(true);
                bt_sendVer.setTextColor(Color.BLACK);
            }
        }

        @Override
        public void onTick(long millisUntilFinished) {
            bt_sendVer.setText(millisUntilFinished/1000 + "秒后重新发送");
        }
    }
}
