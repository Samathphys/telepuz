package ru.gurhouse.sch;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.design.internal.BottomNavigationItemView;
import android.support.design.internal.BottomNavigationMenuView;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.google.firebase.analytics.FirebaseAnalytics;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.List;

import static ru.gurhouse.sch.LoginActivity.connect;
import static ru.gurhouse.sch.LoginActivity.log;
import static ru.gurhouse.sch.LoginActivity.loge;

public class MainActivity extends AppCompatActivity {

    private PeriodFragment periodFragment;
    private PeriodFragment1 periodFragment1;
    private MessagesFragment messagesFragment;
    private ConstraintLayout main, chat;
    private int state = 2;
    private BroadcastReceiver receiver,  auth_receiver;
    private BottomNavigationView bottomnav;
    private BottomNavigationItemView itemView;
    private boolean mode0 = false;

    ScheduleFragment scheduleFragment;

    private BottomNavigationView.OnNavigationItemSelectedListener mNavigationListener = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            Toolbar toolbar = findViewById(R.id.toolbar);
            if(getSupportActionBar() == null)
                setSupportActionBar(toolbar);
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            getSupportActionBar().setHomeButtonEnabled(false);
            switch (item.getItemId()) {
                case R.id.navigation_period:
                    toolbar.setClickable(true);
                    state = 1;
                    if(mode0)
                        loadFragment(periodFragment);
                    else
                        loadFragment(periodFragment1);
                    break;
                case R.id.navigation_diary:
                    state = 2;
                    toolbar.setTitle("Дневник");
                    toolbar.setClickable(false);
                    loadFragment(scheduleFragment);
                    break;
                case R.id.navigation_messages:
                    toolbar.setTitle("Сообщения");
                    toolbar.setClickable(false);
                    if(getStackTop() instanceof MessagesFragment)
                        ((MessagesFragment) getStackTop()).refresh();
                    else
                        loadFragment(messagesFragment);
                    state = 3;
                    break;
                default:
                    state = 0;
                    return false;
            }
            setSupportActionBar(toolbar);
            return true;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        mode0 = getSharedPreferences("pref", 0).getBoolean("period_normal", false);

        periodFragment1 = new PeriodFragment1();
        periodFragment = new PeriodFragment();
        scheduleFragment = new ScheduleFragment();
        messagesFragment = new MessagesFragment();

        final String login = getIntent().getStringExtra("login"), hash = getIntent().getStringExtra("hash");
        new Thread() {
            @Override
            public void run() {
                try {
                    login(login, hash);
                } catch (LoginActivity.NoInternetException e) {
                    findViewById(R.id.refresh).setVisibility(View.VISIBLE);
                    findViewById(R.id.tv_error).setVisibility(View.VISIBLE);
                    findViewById(R.id.frame).setVisibility(View.INVISIBLE);
                } catch (Exception e) {
                    loge("login: " + e.toString());}
            }
        }.start();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setNavigationBarColor(getResources().getColor(R.color.gr1));
        }

        new Thread() {
            @Override
            public void run() {
                final FirebaseAnalytics analytics = FirebaseAnalytics.getInstance(getApplicationContext());
                Bundle bundle = new Bundle();
                bundle.putString("text", "test_text");
                bundle.putString("info", "test_info");
                bundle.putLong(FirebaseAnalytics.Param.VALUE, 1);
                bundle.putString("currency", "rub");
                analytics.logEvent("test_conversion", bundle);
                /*bundle = new Bundle();
                bundle.putString(FirebaseAnalytics.Param.ITEM_ID, "item_id!");
                bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, "item_name!");
                bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "content_type!");
                analytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);*/
            }
        };//.start();

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (getStackTop() instanceof MessagesFragment) {
                    ((MessagesFragment) getStackTop()).newMessage(intent.getStringExtra("text"),
                            intent.getLongExtra("time", 0), intent.getIntExtra("sender_id", 0),
                            intent.getIntExtra("thread_id", 0));
                } else if (getStackTop() instanceof chat1Fragment) {
                    chat1Fragment.Msg msg = new chat1Fragment.Msg();
                    msg.text = intent.getStringExtra("text");
                    msg.time = new Date(intent.getLongExtra("time", 0));
                    msg.sender = intent.getStringExtra("sender_fio");
                    msg.user_id = intent.getIntExtra("thread_id",0);
                    ((chat1Fragment) getStackTop()).DrawMsg(msg);

                } else {
                    BottomNavigationMenuView bottomNavigationMenuView =
                            (BottomNavigationMenuView) bottomnav.getChildAt(0);
                    View v = bottomNavigationMenuView.getChildAt(2);
                    BottomNavigationItemView itemView = (BottomNavigationItemView) v;
                    TextView tv = itemView.findViewById(R.id.tv_badge);
                    if(tv.getVisibility() == View.INVISIBLE) {
                        tv.setVisibility(View.VISIBLE);
                        tv.setText("1");
                    } else
                        tv.setText(Integer.parseInt(tv.getText().toString()) + 1 + "");
                }
            }
        };
        auth_receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                log("auth received");
                MainActivity.this.setResult(239, new Intent().putExtra("auth", "true"));
                finish();
            }
        };

        main = findViewById(R.id.main_container);
        chat = findViewById(R.id.chat_container);

        //snackbar = Snackbar.make(findViewById(R.id.frame), "Нет подключения к интернету", Snackbar.LENGTH_INDEFINITE);

        bottomnav = findViewById(R.id.bottomnav);
        bottomnav.setOnNavigationItemSelectedListener(mNavigationListener);
        BottomNavigationMenuView bottomNavigationMenuView =
                (BottomNavigationMenuView) bottomnav.getChildAt(0);
        View v = bottomNavigationMenuView.getChildAt(2);
        itemView = (BottomNavigationItemView) v;
        /*View badge = */getLayoutInflater().inflate(R.layout.badge, itemView, true);

        if(getIntent().getBooleanExtra("notif", false)) {
            log("notif");
            if(getIntent().getStringExtra("type").equals("msg")) {
                log("type - msg");
                bottomnav.setSelectedItemId(R.id.navigation_messages);
                Toolbar toolbar = findViewById(R.id.toolbar);
                toolbar.setTitle("Сообщения");
                setSupportActionBar(toolbar);
                loadFragment(messagesFragment);
            }
        } else {
            Toolbar toolbar = findViewById(R.id.toolbar);
            toolbar.setTitle("Дневник");
            setSupportActionBar(toolbar);
            loadFragment(scheduleFragment);
            bottomnav.setSelectedItemId(R.id.navigation_diary);
        }
        new Thread(() -> {
            try {
                Thread.sleep(100);
            } catch (Exception ignore) {}
                runOnUiThread(() -> {
                    if(findViewById(R.id.refresh) != null) {
                        findViewById(R.id.refresh).setOnClickListener((view) -> {
                            view.setVisibility(View.INVISIBLE);
                            findViewById(R.id.tv_error).setVisibility(View.INVISIBLE);
                            findViewById(R.id.frame).setVisibility(View.VISIBLE);
                            try {
                                if (getIntent().getStringExtra("login") != null) {
                                    login(getIntent().getStringExtra("login"), getIntent().getStringExtra("hash"));
                                }
                            } catch (LoginActivity.NoInternetException e) {
                                view.setVisibility(View.VISIBLE);
                                findViewById(R.id.tv_error).setVisibility(View.VISIBLE);
                                findViewById(R.id.frame).setVisibility(View.INVISIBLE);
                            } catch (Exception e) {
                                loge(e.toString());
                            }
                        });
                    }
                });
        }).start();
    }

    @SuppressLint("HandlerLeak")
    private final Handler h = new Handler() {
        @Override
        public void handleMessage(final Message msg) {
            if(getStackTop() instanceof MessagesFragment)
                messagesFragment.show();
            else
                runOnUiThread(() -> {
                        TextView tv = itemView.findViewById(R.id.tv_badge);
                        if(msg.arg1 == 0)
                            tv.setVisibility(View.INVISIBLE);
                        else
                            tv.setText(msg.arg1 + "");
                });
        }
    };

    private void login(String login, String hash) throws IOException, JSONException, LoginActivity.NoInternetException {
        URL url;
        HttpURLConnection con;

        int userId = -1, prsId;
        String name;
        SharedPreferences pref = getSharedPreferences("pref", 0);
        if(pref.getInt("userId", -1) == -1) {
         //if(true) {
            log("userId not found, calling state");
            String result = connect("https://app.eschool.center/ec-server/state?menu=false", null);

            log("state: " + result);
            JSONObject obj = new JSONObject(result);
            if (obj.has("userId"))
                userId = obj.getInt("userId");
            prsId = obj.getJSONObject("user").getInt("prsId");
            name = obj.getJSONObject("profile").getString("firstName");
            pref.edit().putInt("userId", userId).putInt("prsId", prsId)
                    .putString("name", name).apply();
        } else {
            userId = pref.getInt("userId", -1);
            prsId = pref.getInt("prsId", -1);
            name = pref.getString("name", "");
        }
        log("userId: " + userId + ", prsId: " + prsId + ", name: " + name);
        TheSingleton.getInstance().setUSER_ID(userId);
        TheSingleton.getInstance().setPERSON_ID(prsId);
        new Thread() {
            @Override
            public void run() {
                try {
                    scheduleFragment.start();
                } catch (Exception e) {
                    loge(e.toString());
                }
            }
        }.start();
        if(getIntent().getBooleanExtra("notif", false)) {
            if(getIntent().getStringExtra("type").equals("msg")) {
                messagesFragment.fromNotification = true;
                messagesFragment.notifThreadId = getIntent().getIntExtra("threadId", -1);
                messagesFragment.notifCount = getIntent().getIntExtra("count", -1);
            }
        }
        messagesFragment.start(h);
        FirebaseAnalytics analytics = FirebaseAnalytics.getInstance(this);
        Bundle bundle = new Bundle();
        switch (getIntent().getIntExtra("mode", -1)) {
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

        KnockFragment.connect("https://still-cove-90434.herokuapp.com/login",
                "login=" + login + "&password=" + hash + "&firebase_id=" + TheSingleton.getInstance().getFb_id());
        log("login=" + login + "&password=" + hash + "&firebase_id=" + TheSingleton.getInstance().getFb_id());
    }

    void set(ScheduleFragment.Period[] periods, int pernum, int t) {
        String[] period = scheduleFragment.period;
        if (t == 1) {
            periodFragment1 = new PeriodFragment1();
            periodFragment1.period = period;
            periodFragment1.pernum = pernum;
            periodFragment1.periods = periods;
            if (state == 1 && !mode0)
                loadFragment(periodFragment1);
        } else {
            periodFragment = new PeriodFragment();
            periodFragment.period = period;
            periodFragment.pernum = pernum;
            periodFragment.periods = periods;
            if (state == 1 && mode0)
                loadFragment(periodFragment);
        }
        TheSingleton.getInstance().setSubjects(periods[pernum].subjects);
       // TheSingleton.getInstance().setDays(days);
    }

    private void loadFragment(Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.frame, fragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    public void setSupActionBar(android.support.v7.widget.Toolbar toolbar) {
        //if(getSupportActionBar() == null)
            setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
    }

    public void set_visible(boolean b) {
        if(b) {
            main.setVisibility(View.VISIBLE);
            chat.setVisibility(View.INVISIBLE);
        } else {
            main.setVisibility(View.INVISIBLE);
            chat.setVisibility(View.VISIBLE);
        }
    }

    public void saveFile(String url, String name, boolean useCookies) {
        log("saving " + name);
        int permissionCheck = ContextCompat.checkSelfPermission(this, "android.permission.WRITE_EXTERNAL_STORAGE");
        if (permissionCheck < 0) {
            log("requesting permission to save file");
            this.url = url;
            this.name = name;
            this.useCookies = useCookies;
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, 12345);
        } else {
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
            try {
                request.setDescription("Downloading file from " + new URL(url).getHost());
            } catch (MalformedURLException e) {
                loge(e.toString());
                request.setDescription("Some Description");
            }
            request.setTitle(name);
            if (useCookies)
                request.addRequestHeader("Cookie", TheSingleton.getInstance().getCOOKIE() + "; site_ver=app;" +
                        " route=" + TheSingleton.getInstance().getROUTE() + "; _pk_id.1.81ed=de563a6425e21a4f.1553009060.16.1554146944.1554139340.");
            request.allowScanningByMediaScanner();
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, name);

            // get download service and enqueue file
            DownloadManager manager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
            manager.enqueue(request);
        }
    }

    private String url = null, name;
    private boolean useCookies;

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(url != null && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            log("permission granted: " + permissions[0]);
            saveFile(url, name, useCookies);
        } else
            log("permission denied: " + permissions[0]);
        url = null;
    }

    @Override
    public void onBackPressed() {
        log("fragments on MainActivity: " + getSupportFragmentManager().getFragments().size());
        List<Fragment> a = getSupportFragmentManager().getFragments();
        for (int i = 0; i < a.size(); i++) {
            log(a.get(i).toString());
        }
        //log("top: " + getStackTop());
<<<<<<< HEAD
<<<<<<< HEAD
        if(getStackTop() instanceof chat1Fragment || getStackTop() instanceof DayFragment || getStackTop() instanceof MarkFragment ||
=======
        boolean chat = getStackTop() instanceof ChatFragment;
=======
>>>>>>> upstream/master
        if(getStackTop() instanceof ChatFragment || getStackTop() instanceof DayFragment || getStackTop() instanceof MarkFragment ||
>>>>>>> upstream/master
                getStackTop() instanceof SubjectFragment || getStackTop() instanceof KnockFragment || getStackTop() instanceof Countcoff) {
            set_visible(true);
            if (getStackTop() instanceof chat1Fragment || getStackTop() instanceof KnockFragment) {
                getSupportActionBar().setTitle("Сообщения");
                getSupportActionBar().setSubtitle("");
            }
            getSupportActionBar().setHomeButtonEnabled(false);
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            getSupportActionBar().setDisplayShowHomeEnabled(false);
        }
        if(!(getSupportFragmentManager().getBackStackEntryCount() == 0))
            if(!(getStackTop() instanceof PeriodFragment || getStackTop() instanceof PeriodFragment1
                    || getStackTop() instanceof ScheduleFragment// || getStackTop() instanceof ScheduleFragment1
                    || getStackTop() instanceof MessagesFragment))
                getSupportFragmentManager().popBackStack();
        if(getStackTop() instanceof ChatFragment && a.get(a.size()-2) instanceof MessagesFragment)
            ((MessagesFragment) a.get(a.size()-2)).refresh();
    }

    @Override
    protected void onResume() {
        super.onResume();
        log("onResume MainActivity");
        try {
            registerReceiver(receiver, new IntentFilter("ru.gurhouse.sch.action"));
            registerReceiver(auth_receiver, new IntentFilter("ru.gurhouse.sch.auth"));
        } catch (Exception e) {
            loge(e.toString());
        }
        mode0 = getSharedPreferences("pref", 0).getBoolean("period_normal", false);
        if (state == 1) {
            if (mode0)
                loadFragment(periodFragment);
            else
                loadFragment(periodFragment1);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        log("onPause");
        try {
            unregisterReceiver(receiver);
            unregisterReceiver(auth_receiver);
        } catch (Exception e) {
            loge(e.toString());
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        try {
            LoginActivity.login();
        } catch (LoginActivity.NoInternetException e) {
            TextView tv = findViewById(R.id.tv_error);
            tv.setText("Нет доступа к интернету");
            tv.setVisibility(View.VISIBLE);
            findViewById(R.id.refresh).setVisibility(View.VISIBLE);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
        }
        return super.onOptionsItemSelected(item);
    }

    void quit() {
        new Thread() {
            @Override
            public void run() {
                try {
                    connect("https://still-cove-90434.herokuapp.com/logout",
                            "firebase_id=" + TheSingleton.getInstance().getFb_id());
                } catch (Exception e) {
                    loge("logout: " + e.toString());
                }
            }
        }.start();
        TheSingleton.getInstance().setPERSON_ID(-1);
        TheSingleton.getInstance().setUSER_ID(-1);
        SharedPreferences pref = getSharedPreferences("pref", 0);
        pref.edit().putBoolean("first_time", true).putInt("userId", -1).putInt("prsId", -1).putString("name", "")
                .putString("knock_token", "").putString("knock_name", "").putString("knock_id", "").apply();
        finish();
    }

    Fragment getStackTop() {
        List<Fragment> a = getSupportFragmentManager().getFragments();
        if(a.size()==0)
            return null;
        return a.get(a.size() - 1);
    }

    /*public static boolean hasConnection(final Context context) {
        ConnectivityManager cm = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo wifiInfo = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if (wifiInfo != null && wifiInfo.isConnected())
        {
            return true;
        }
        wifiInfo = cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        if (wifiInfo != null && wifiInfo.isConnected())
        {
            return true;
        }
        wifiInfo = cm.getActiveNetworkInfo();
        return wifiInfo != null && wifiInfo.isConnected();
    }*/
}
