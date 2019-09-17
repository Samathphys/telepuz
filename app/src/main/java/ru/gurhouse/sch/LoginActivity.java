package ru.gurhouse.sch;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.PorterDuff;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.iid.FirebaseInstanceId;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.util.List;
import java.util.Map;

import javax.net.ssl.SSLException;

public class LoginActivity extends AppCompatActivity implements View.OnClickListener {

    private EditText et_login;
    private EditText et_password;
    private int threadId = -1;
    private BroadcastReceiver internet = null;

    private int mode = -1;

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
        // to see that window with setting nickname in chat, add this
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

        FloatingActionButton fab = findViewById(R.id.fab_go);
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
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        loge("getInstanceId failed: " + task.getException().toString());
                        return;
                    }

                    // Get new Instance ID token
                    String token = task.getResult().getToken();

                    log(token);
                    TheSingleton.getInstance().setFb_id(token);
                });
            findViewById(R.id.btn_refresh).setOnClickListener((v)->{
                    new Thread(()->{
                        try {
                            login(settings.getString("login", ""), settings.getString("hash", ""), mode);
                        } catch (Exception e) {
                            loge(e.toString());
                        }
                    }).start();

                    v.setVisibility(View.INVISIBLE);
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

    boolean flag_shown = false;
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
        if(getSharedPreferences("pref", 0).getBoolean("first_time", true) && !flag_shown) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setPositiveButton("OK", (dialog, id) -> onClick(v));
            builder.setMessage("Продолжая использовать данное приложение" +
                    " вы соглашаетесь, что ваши данные, имеющиеся у платформы eschool.center теоретически будут доступны " +
                    "третьим лицам (т. е. разработчикам данного приложения)")
                    .setTitle("Политика конфиденциальности");
            AlertDialog dialog = builder.create();
            dialog.show();
            flag_shown = true;
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
    private Handler h = new Handler() {
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
                findViewById(R.id.btn_refresh).setVisibility(View.VISIBLE);
                /*internet = new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        log("network settings changed");
                        if(hasConnection(context) && mode != -1) {
                            new Thread() {
                                @Override
                                public void run() {
                                    try {
                                        unregisterReceiver(internet);
                                        internet_count++;
                                        login(login, hash, mode);
                                    } catch (IOException e) {
                                        loge(e.toString());
                                    }
                                }
                            }.start();
                        }
                    }
                };
                registerReceiver(internet, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));*/
            }
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if(data != null)
            log("activity result received, auth = " + data.getStringExtra("auth"));
    }

    private void login(final String login, final String hash, int mode) throws IOException {
        log("login mode " + mode);
        h.sendEmptyMessage(0);

        log("login " + login);
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
                TheSingleton.getInstance().login = login;
                TheSingleton.getInstance().hash = hash;

                if (threadId != -1)
                    startActivity(new Intent(getApplicationContext(), MainActivity.class)
                            .putExtra("type", "msg").putExtra("notif", true)
                            .putExtra("threadId", threadId).putExtra("login", login).putExtra("hash", hash)
                            .putExtra("mode", mode).putExtra("count", getIntent().getIntExtra("count", -1)));
                else
                    startActivity(new Intent(getApplicationContext(), MainActivity.class)
                            .putExtra("login", login).putExtra("hash", hash).putExtra("mode", mode));
            } else {
                log("response code " + con.getResponseCode());
                h.sendEmptyMessage(1);
            }
        } catch (UnknownHostException e) {
            this.mode = mode;
            h.sendEmptyMessage(2);
        } catch (SSLException e) {
            this.mode = mode;
            h.sendEmptyMessage(2);
        } catch (ConnectException e) {
            this.mode = mode;
            h.sendEmptyMessage(2);
        }
    }

    static <T> void log(T msg) { if(msg != null) Log.v("mylog", msg.toString()); else loge("null log");}
    static <T> void loge(T msg) {
        if(msg instanceof Exception)
            ((Exception) msg).printStackTrace();
        if(msg != null) Log.e("mylog", msg.toString()); else loge("null log");
    }

    static void login(String login, String password) throws NoInternetException {
        try {
            URL Url = new URL("https://app.eschool.center/ec-server/login");
            HttpURLConnection con = (HttpURLConnection) Url.openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("Cookie", "_pk_id.1.81ed=de563a6425e21a4f.1553009060.16.1554146944.1554139340.");
            con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            con.setDoOutput(true);
            OutputStream os = con.getOutputStream();
            log("username=" + login + "&password=" + password);
            os.write(("username=" + login + "&password=" + password).getBytes());
            con.connect();
            log(con.getResponseMessage());
            Map<String, List<String>> a = con.getHeaderFields();
            Object[] b = a.entrySet().toArray();
            String route = String.valueOf(b[8]).split("route=")[1].split(";")[0];
            String COOKIE2 = "JSESSIONID=" + String.valueOf(b[8]).split("ID=")[1].split(";")[0];
            TheSingleton.getInstance().setROUTE(route);
            TheSingleton.getInstance().setCOOKIE(COOKIE2);
            log("route: " + route + ", cookie: " + COOKIE2);
        } catch (UnknownHostException e) {
            throw new NoInternetException();
        } catch (IOException e) {loge(e.toString());}
    }
    static void login() throws NoInternetException {
        login(TheSingleton.getInstance().login, TheSingleton.getInstance().hash);
    }

    // todo отложенные запросы
    static String connect(String url, @Nullable String query, boolean put) throws IOException, NoInternetException {
        log("connect " + url.replaceAll("https://app.eschool.center", "") + ", query: " + query);
        try {
            HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
            con.setRequestProperty("Cookie", TheSingleton.getInstance().getCOOKIE() + "; site_ver=app; route=" + TheSingleton.getInstance().getROUTE() + "; _pk_id.1.81ed=de563a6425e21a4f.1553009060.16.1554146944.1554139340.");
            if (query == null) {
                con.setRequestMethod("GET");
                con.connect();
            } else {
                if (put)
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
                if(con.getResponseCode() == 401) {
                    login();
                    return connect(url, query, put);

                /*con = (HttpURLConnection) new URL(url).openConnection();
                con.setRequestProperty("Cookie", COOKIE2 + "; site_ver=app; route=" + route + "; _pk_id.1.81ed=de563a6425e21a4f.1553009060.16.1554146944.1554139340.");
                if(query == null) {
                    con.setRequestMethod("GET");
                    con.connect();
                } else {
                    con.setRequestMethod("POST");
                    con.setDoOutput(true);
                    con.connect();
                    con.getOutputStream().write(query.getBytes());
                }*/
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
        } catch (UnknownHostException e) {
            throw new NoInternetException();
        } catch (SSLException e) {
            throw new NoInternetException();
        } catch (ConnectException e) {
            throw new NoInternetException();
        }
    }
    static String connect(String url, @Nullable String query) throws IOException, NoInternetException {
        return connect(url, query, false);
    }

    static class NoInternetException extends Exception {
        NoInternetException() {
            loge("NoInternetException created");
        }
    }
}

