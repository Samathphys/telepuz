package com.example.sch;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

public class LoginActivity extends AppCompatActivity implements View.OnClickListener {

    FloatingActionButton fab;
    Button btn_hash, btn_test;
    EditText et_login, et_password, et_hash;
    String fb_id;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setNavigationBarColor(getResources().getColor(R.color.gr1));
        }

        new Thread() {
            @Override
            public void run() {
                log(FirebaseInstanceId.getInstance().getId());
            }
        }.start();

        final SharedPreferences settings = getSharedPreferences("pref", 0);

        if(settings.getString("error", "") != null) {
            if(!settings.getString("error", "").equals("")) {
                final Snackbar s = Snackbar.make(findViewById(R.id.root), settings.getString("error", ""), Snackbar.LENGTH_LONG);
                s.setAction("OK", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        settings.edit().remove("error").apply();
                        s.dismiss();
                    }
                });
                s.show();
                loge(settings.getString("error", ""));
            }
        }

        if (!settings.getBoolean("first_time", true)) {
            //the app is being launched not for the first time

            new Thread() {
                @Override
                public void run() {
                    try {
                        login(settings.getString("login", ""), settings.getString("hash", ""), 1);
                    } catch (Exception e) {
                        loge(e.toString());
                    }
                }
            }.start();
        } else {
            log("first time");
//            findViewById(R.id.l_skip).setVisibility(View.INVISIBLE);
//            findViewById(R.id.l_login).setVisibility(View.VISIBLE);
        }

        fab = findViewById(R.id.fab_go);
        btn_hash = findViewById(R.id.btn_hash);
        btn_test = findViewById(R.id.btn_test);
        et_login = findViewById(R.id.et_login);
        et_password = findViewById(R.id.et_password);
        et_hash = findViewById(R.id.et_hash);

        fab.setOnClickListener(this);
        btn_hash.setOnClickListener(this);
        btn_test.setOnClickListener(this);

        FirebaseInstanceId.getInstance().getInstanceId()
                .addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
                    @Override
                    public void onComplete(@NonNull Task<InstanceIdResult> task) {
                        if (!task.isSuccessful()) {
                            loge("getInstanceId failed: " + task.getException().toString());
                            return;
                        }

                        // Get new Instance ID token
                        String token = task.getResult().getToken();

                        log(token);
                        fb_id = token;
                        TheSingleton.getInstance().setFb_id(fb_id);
                        // Log and toast
//                        String msg = getString(R.string.msg_token_fmt, token);
//                        log(msg);
//                        Toast.makeText(LoginActivity.this, msg, Toast.LENGTH_SHORT).show();
                    }
                });


    }

    @Override
    protected void onResume() {
        log("onResume");
        findViewById(R.id.l_skip).setVisibility(View.INVISIBLE);
        findViewById(R.id.l_login).setVisibility(View.VISIBLE);
        super.onResume();
    }

    public void onClick(final View v) {
        String logi = et_login.getText().toString();
        String password;
        if(v.getId() == R.id.btn_hash)
            password = et_hash.getText().toString();
        else if(v.getId() == R.id.btn_test) {
            logi = "ilya_z";
            password = "Booble19";
        } else
            password = et_password.getText().toString();
        try {
            if(!(v.getId() == R.id.btn_hash)) {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                byte[] hashb = digest.digest(password.getBytes("UTF-8"));
                StringBuilder hexString = new StringBuilder();
                for (byte aHashb : hashb) {
                    String hex = Integer.toHexString(0xff & aHashb);
                    if (hex.length() == 1) hexString.append('0');
                    hexString.append(hex);
                }
                password = hexString.toString();
            }
            final String login = logi, pw = password;
            SharedPreferences settings = getSharedPreferences("pref", 0);
            settings.edit().putBoolean("first_time", false)
                    .putString("login", login).putString("hash", pw).apply();

            new Thread() {
                @Override
                public void run() {
                    try {
                        URL url;
                        HttpURLConnection con;

                        url = new URL("https://still-cove-90434.herokuapp.com/login");
                        con = (HttpURLConnection) url.openConnection();
                        con.setRequestMethod("POST");
                        con.setDoOutput(true);
                        con.connect();
                        OutputStream os = con.getOutputStream();
                        os.write(("login=" + login + "&password=" + pw + "&firebase_id=" + fb_id).getBytes());
                        log("login=" + login + "&password=" + pw + "&firebase_id=" + fb_id);
                        loge(con.getResponseMessage());
                        int mode;
                        if(v.getId() == R.id.btn_test)
                            mode = 0;
                        else if(v.getId() == R.id.btn_hash)
                            mode = 3;
                        else
                            mode = 2;
                        login(login, pw, mode);
                    } catch (Exception e) {
                        loge(e.toString());
                    }
                }
            }.start();

        } catch (Exception e) {
            Log.e("mylog", e.toString());
        }
    }

    @SuppressLint("HandlerLeak")
    Handler h = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            findViewById(R.id.l_skip).setVisibility(View.VISIBLE);
            findViewById(R.id.l_login).setVisibility(View.INVISIBLE);
            TextView tv = findViewById(R.id.tv_dead);
            Calendar calendar = Calendar.getInstance();
            loge("DAYS TO DEADLINE: " + (31-calendar.get(Calendar.DAY_OF_MONTH)) + "!");
            tv.setText("до дедлайна осталось всего " + (31-calendar.get(Calendar.DAY_OF_MONTH)) + " дней");
        }
    };

    void login(final String login, final String hash, int mode) throws Exception {
        FirebaseAnalytics analytics = FirebaseAnalytics.getInstance(this);
        Bundle bundle = new Bundle();
        switch (mode) {
            case 0:
                bundle.putString(FirebaseAnalytics.Param.METHOD, "test");
                break;
            case 1:
                bundle.putString(FirebaseAnalytics.Param.METHOD, "auto");
                log("auto");
                break;
            case 2:
                bundle.putString(FirebaseAnalytics.Param.METHOD, "password");
                break;
            case 3:
                bundle.putString(FirebaseAnalytics.Param.METHOD, "hash");
                break;
                default:
                    bundle.putString(FirebaseAnalytics.Param.METHOD, "some_method");
        }
        analytics.logEvent(FirebaseAnalytics.Event.LOGIN, bundle);
        h.sendEmptyMessage(0);
        URL url;
        HttpURLConnection con;
        StringBuilder result;
        BufferedReader rd;
        String line;

        url = new URL("https://app.eschool.center/ec-server/login");
        con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("POST");
        con.setRequestProperty("Cookie", "_pk_id.1.81ed=de563a6425e21a4f.1553009060.16.1554146944.1554139340.");
        con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        con.setDoOutput(true);
        con.connect();
        OutputStream os = con.getOutputStream();
        os.write(("username=" + login + "&password=" + hash).getBytes());
        con.connect();
        Map<String, List<String>> a = con.getHeaderFields();
        Object[] b = a.entrySet().toArray();
        String route = String.valueOf(b[8]).split("route=")[1].split(";")[0];
        String COOKIE2 = "JSESSIONID=" + String.valueOf(b[8]).split("ID=")[1].split(";")[0];
        log("login: " + COOKIE2);
        //new Scanner(System.in).nextLine();

        url = new URL("https://app.eschool.center/ec-server/state?menu=false");
        con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");
        con.setRequestProperty("Cookie", COOKIE2 + "; site_ver=app; route=" + route + "; _pk_id.1.81ed=de563a6425e21a4f.1553009060.16.1554146944.1554139340.");// "; _pk_id.1.81ed=de563a6425e21a4f.1553009060.13.1554062260.1554051192.");
        con.connect();
        result = new StringBuilder();
        log(con.getResponseMessage());

        rd = new BufferedReader(new InputStreamReader(con.getInputStream()));

        while ((line = rd.readLine()) != null) {
            result.append(line);
        }
        rd.close();

        log("state: " + result.toString());
        JSONObject obj = new JSONObject(result.toString());
        int userId = -1;
        if (obj.has("userId"))
            userId = obj.getInt("userId");
        TheSingleton.getInstance().setCOOKIE(COOKIE2);
        TheSingleton.getInstance().setROUTE(route);
        TheSingleton.getInstance().setUSER_ID(userId);
        TheSingleton.getInstance().setPERSON_ID(obj.getJSONObject("user").getInt("prsId"));
        startActivity(new Intent(getApplicationContext(), MainActivity.class));
    }

    static void log(String msg) {Log.v("mylog", msg);}
    static void loge(String msg) {Log.e("mylog", msg);}

    static String connect(String url, @Nullable String query) throws IOException {
        HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
        con.setRequestProperty("Cookie", TheSingleton.getInstance().getCOOKIE() + "; site_ver=app; route=" + TheSingleton.getInstance().getROUTE() + "; _pk_id.1.81ed=de563a6425e21a4f.1553009060.16.1554146944.1554139340.");
        if(query == null) {
            con.setRequestMethod("GET");
            con.connect();
        } else {
            con.setRequestMethod("POST");
            con.setDoOutput(true);
            con.connect();
            con.getOutputStream().write(query.getBytes());
        }
        if(con.getResponseCode() != 200) {
            loge("connect failed, code " + con.getResponseCode() + ", message: " + con.getResponseMessage());
            return "";
        }
        if(con.getInputStream() != null) {
            BufferedReader rd = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String line;
            StringBuilder result = new StringBuilder();
            while ((line = rd.readLine()) != null) {
                result.append(line);
            }
            rd.close();
            return result.toString();
        } else
            return "";
    }
}
