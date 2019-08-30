package com.example.sch;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import static com.example.sch.MainActivity.hasConnection;

public class LoginActivity extends AppCompatActivity implements View.OnClickListener {

    FloatingActionButton fab;
    EditText et_login;
    EditText et_password;
    int threadId = -1;
    BroadcastReceiver internet = null;

    String login, hash;
    int mode = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        TheSingleton.getInstance().t1 = System.currentTimeMillis();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setNavigationBarColor(getResources().getColor(R.color.gr1));
        }

        new Thread() {
            @Override
            public void run() {
                log(FirebaseInstanceId.getInstance().getId());
            }
        }.start();

        if(getIntent().getStringExtra("type") != null) {
            threadId = getIntent().getIntExtra("threadId", -1);
        }
        NotificationManagerCompat.from(this).cancelAll();

        final SharedPreferences settings = getSharedPreferences("pref", 0);
        // to see that awesome window with setting nickname in chat, add this
        //settings.edit().putString("knock_token", "").apply();

        if (!settings.getBoolean("first_time", true)) {
            //the app is being launched not for the first time

            new Thread() {
                @Override
                public void run() {
                    try {
                        if(settings.getBoolean("auto", true))
                            login(settings.getString("login", ""), settings.getString("hash", ""), 1);
                    } catch (Exception e) {
                        loge(e.toString());
                    }
                }
            }.start();
        } else {
            log("first time");
        }

        fab = findViewById(R.id.fab_go);
        et_login = findViewById(R.id.et_login);
        et_password = findViewById(R.id.et_password);

        fab.setOnClickListener(this);

        et_login.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if(!s.toString().replaceAll(" ", "").equals(""))
                    et_login.getBackground().mutate().setColorFilter(getResources().getColor(R.color.text_gray), PorterDuff.Mode.SRC_ATOP);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        et_password.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if(!s.toString().replaceAll(" ", "").equals(""))
                    et_password.getBackground().mutate().setColorFilter(getResources().getColor(R.color.text_gray), PorterDuff.Mode.SRC_ATOP);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

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
                        TheSingleton.getInstance().setFb_id(token);
                    }
                });
    }

    @Override
    protected void onResume() {
        log("onResume");
        findViewById(R.id.l_skip).setVisibility(View.INVISIBLE);
        findViewById(R.id.l_login).setVisibility(View.VISIBLE);
        if(internet != null)
            registerReceiver(internet, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        super.onResume();
    }

    @Override
    protected void onPause() {
        if(internet != null)
            unregisterReceiver(internet);
        super.onPause();
    }

    public void onClick(final View v) {
        String logi = et_login.getText().toString();
        String password = et_password.getText().toString();
        if(logi.replaceAll(" ", "").equals("")) {
            et_login.getBackground().mutate().setColorFilter(getResources().getColor(android.R.color.holo_red_light), PorterDuff.Mode.SRC_ATOP);
        } else {
            et_login.getBackground().mutate().setColorFilter(getResources().getColor(R.color.text_gray), PorterDuff.Mode.SRC_ATOP);
        }
        if(password.replaceAll(" ", "").equals("")) {
            et_password.getBackground().mutate().setColorFilter(getResources().getColor(android.R.color.holo_red_light), PorterDuff.Mode.SRC_ATOP);
        } else {
            et_password.getBackground().mutate().setColorFilter(getResources().getColor(R.color.text_gray), PorterDuff.Mode.SRC_ATOP);
        }
        if(logi.replaceAll(" ", "").equals("") || password.replaceAll(" ", "").equals("")) {
            return;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashb = digest.digest(password.getBytes("UTF-8"));
            StringBuilder hexString = new StringBuilder();
            for (byte aHashb : hashb) {
                String hex = Integer.toHexString(0xff & aHashb);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            password = hexString.toString();
            final String login = logi, pw = password;
            SharedPreferences settings = getSharedPreferences("pref", 0);
            settings.edit().putBoolean("first_time", false)
                    .putString("login", login).putString("hash", pw).apply();

            new Thread() {
                @Override
                public void run() {
                    try {
                        login(login, pw, 2);
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
            if(msg.what == 0) {
                findViewById(R.id.pb).setVisibility(View.VISIBLE);
                ((TextView) findViewById(R.id.tv_dead)).setText("Rаботать, Александр, Rаботать");
                findViewById(R.id.l_skip).setVisibility(View.VISIBLE);
                findViewById(R.id.l_login).setVisibility(View.INVISIBLE);
                et_login.setText("");
                et_password.setText("");
            } else if(msg.what == 1){
                loge("wrong login/password");
                Toast.makeText(getApplicationContext(), "Неправильный логин/пароль", Toast.LENGTH_LONG).show();
                findViewById(R.id.l_skip).setVisibility(View.INVISIBLE);
                findViewById(R.id.l_login).setVisibility(View.VISIBLE);
            } else if(msg.what == 2) {
                loge("no internet");
                findViewById(R.id.pb).setVisibility(View.INVISIBLE);
                ((TextView) findViewById(R.id.tv_dead)).setText("Нет подключения к интернету");
                internet = new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        log("network settings changed");
                        if(hasConnection(context) && mode != -1) {
                            new Thread() {
                                @Override
                                public void run() {
                                    try {
                                        login(login, hash, mode);
                                    } catch (IOException e) {
                                        loge(e.toString());
                                    }
                                }
                            }.start();
                        }
                    }
                };
                registerReceiver(internet, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
            }
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if(data != null)
            log("activity result received, auth = " + data.getStringExtra("auth"));
    }

    void login(final String login, final String hash, int mode) throws IOException {
        log("login mode " + mode);
        h.sendEmptyMessage(0);

        URL url;
        HttpURLConnection con;

        log("connect /ec-server/login");
        url = new URL("https://app.eschool.center/ec-server/login");
        con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("POST");
        con.setRequestProperty("Cookie", "_pk_id.1.81ed=de563a6425e21a4f.1553009060.16.1554146944.1554139340.");
        con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        con.setDoOutput(true);
        try {
            OutputStream os = con.getOutputStream();
            os.write(("username=" + login + "&password=" + hash).getBytes());
            con.connect();
            log("code " + con.getResponseCode());
            if(con.getResponseCode() == 200) {
                Map<String, List<String>> a = con.getHeaderFields();
                Object[] b = a.entrySet().toArray();
                String route = String.valueOf(b[8]).split("route=")[1].split(";")[0];
                String COOKIE2 = "JSESSIONID=" + String.valueOf(b[8]).split("ID=")[1].split(";")[0];

                log("login: " + COOKIE2);
                TheSingleton.getInstance().setCOOKIE(COOKIE2);
                TheSingleton.getInstance().setROUTE(route);

                if (threadId != -1)
                    startActivity(new Intent(getApplicationContext(), MainActivity.class)
                            .putExtra("type", "msg").putExtra("notif", true)
                            .putExtra("threadId", threadId).putExtra("login", login).putExtra("hash", hash)
                            .putExtra("mode", mode).putExtra("count", getIntent().getIntExtra("count", -1)));
                else
                    startActivity(new Intent(getApplicationContext(), MainActivity.class)
                            .putExtra("login", login).putExtra("hash", hash).putExtra("mode", mode));
            } else {
                h.sendEmptyMessage(1);
            }
        } catch (UnknownHostException e) {
            loge(e.toString());
            this.login = login;
            this.hash = hash;
            this.mode = mode;
            h.sendEmptyMessage(2);
        }
    }

    static <T> void log(T msg) { if(msg != null) Log.v("mylog", msg.toString()); else loge("null log");}
    static <T> void loge(T msg) {if(msg != null) Log.e("mylog", msg.toString()); else loge("null log");}

    static String connect(String url, @Nullable String query, Context context, boolean put) throws IOException {
        log("connect " + url.replaceAll("https://app.eschool.center", ""));
        HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
        con.setRequestProperty("Cookie", TheSingleton.getInstance().getCOOKIE() + "; site_ver=app; route=" + TheSingleton.getInstance().getROUTE() + "; _pk_id.1.81ed=de563a6425e21a4f.1553009060.16.1554146944.1554139340.");
        if(query == null) {
            con.setRequestMethod("GET");
            con.connect();
        } else {
            if(put)
                con.setRequestMethod("PUT");
            else
                con.setRequestMethod("POST");
            con.setDoOutput(true);
            con.connect();
            con.getOutputStream().write(query.getBytes());
        }
        if(con.getResponseCode() != 200) {
            loge("connect failed, code " + con.getResponseCode() + ", message: " + con.getResponseMessage());
            loge(url);
            loge("query: '" + query + "'");
            if(context == null) {
                loge("null context");
                return "";
            }
            if(con.getResponseCode() == 401) {
                Toast.makeText(context, "Error 401", Toast.LENGTH_SHORT).show();

                URL Url = new URL("https://app.eschool.center/ec-server/login");
                con = (HttpURLConnection) Url.openConnection();
                con.setRequestMethod("POST");
                con.setRequestProperty("Cookie", "_pk_id.1.81ed=de563a6425e21a4f.1553009060.16.1554146944.1554139340.");
                con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                con.setDoOutput(true);
                con.connect();
                OutputStream os = con.getOutputStream();
                SharedPreferences data = context.getSharedPreferences("pref", 0);
                String login = data.getString("login", ""),
                        password = data.getString("hash", "");
                log("username=" + login + "&password=" + password);
                os.write(("username=" + login + "&password=" + password).getBytes());
                con.connect();
                log(con.getResponseMessage());
                Map <String, List<String>> a = con.getHeaderFields();
                Object[] b = a.entrySet().toArray();
                String route = String.valueOf(b[8]).split("route=")[1].split(";")[0];
                String COOKIE2 = "JSESSIONID=" + String.valueOf(b[8]).split("ID=")[1].split(";")[0];
                TheSingleton.getInstance().setROUTE(route);
                TheSingleton.getInstance().setCOOKIE(COOKIE2);
                log("route: " + route);
                log(COOKIE2);


                con = (HttpURLConnection) new URL(url).openConnection();
                con.setRequestProperty("Cookie", COOKIE2 + "; site_ver=app; route=" + route + "; _pk_id.1.81ed=de563a6425e21a4f.1553009060.16.1554146944.1554139340.");
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
                    return "";
                }
            } else {
                return "";
            }
        }
        if(con.getInputStream() != null) {
            BufferedReader rd = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String line;
            StringBuilder result = new StringBuilder();
            while ((line = rd.readLine()) != null) {
                result.append(line);
            }
            rd.close();
            //log("flag \n" + result.toString());
            return result.toString();
        } else
            return "";
    }
    static String connect(String url, @Nullable String query, Context context) throws IOException {
        return connect(url, query, context, false);
    }
}
