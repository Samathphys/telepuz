package com.example.sch;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.google.firebase.analytics.FirebaseAnalytics;

import java.util.ArrayList;
import java.util.List;

import static com.example.sch.LoginActivity.connect;
import static com.example.sch.LoginActivity.log;
import static com.example.sch.LoginActivity.loge;

public class MainActivity extends AppCompatActivity {

    PeriodFragment periodFragment;
    MessagesFragment messagesFragment;
    ConstraintLayout main, chat;
    ScheduleFragment scheduleFragment;
    ArrayList<PeriodFragment.Subject> subjects;
    ArrayList<PeriodFragment.Day> days;
    Snackbar snackbar;
    BroadcastReceiver receiver, internet_receiver;

    private BottomNavigationView.OnNavigationItemSelectedListener mNavigationListener = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_period:
                    setTitle("Period");
                    loadFragment(periodFragment);
                    return true;
                case R.id.navigation_diary:
                    setTitle("Diary");
                    loadFragment(scheduleFragment);
                    return true;
                case R.id.navigation_messages:
                    setTitle("Messages");
                    if(getStackTop() instanceof MessagesFragment)
                        ((MessagesFragment) getStackTop()).refresh();
                    else
                        loadFragment(messagesFragment);
                    return true;
            }
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        /*Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                loge("caught");
                SharedPreferences pref = getSharedPreferences("pref", 0);
                pref.edit().putString("error", t.toString() + ": " + e.toString()).apply();
                throw new RuntimeException(e);
            }
        });*/

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
                analytics.logEvent("test_conversion", bundle);
                /*bundle = new Bundle();
                bundle.putString(FirebaseAnalytics.Param.ITEM_ID, "item_id!");
                bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, "item_name!");
                bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "content_type!");
                analytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);*/
            }
        }.start();

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (getStackTop() instanceof MessagesFragment) {
                    ((MessagesFragment) getStackTop()).newMessage(intent.getStringExtra("text"),
                            intent.getLongExtra("time", 0), intent.getIntExtra("sender_id", 0),
                            intent.getIntExtra("thread_id", 0));
                } else if (getStackTop() instanceof ChatFragment) {
                    ((ChatFragment) getStackTop()).newMessage(intent.getStringExtra("text"), intent.getLongExtra("time", 0),
                            intent.getIntExtra("sender_id", 0), intent.getIntExtra("thread_id", 0),
                            intent.getStringExtra("sender_fio"));
                }
            }
        };
        /*try {
            unregisterReceiver(receiver);
        } catch (IllegalArgumentException e) {
            log("IllegalArgumentException handled");
        } finally {
            try {
                registerReceiver(receiver, new IntentFilter("com.example.sch.action"));
            } catch (Exception e) {
                loge(e.toString());
            }
        }*/
        internet_receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if(hasConnection(context)) {
                    snackbar.dismiss();
                } else {
                    snackbar.show();
                }
            }
        };

        int permissionCheck = ContextCompat.checkSelfPermission(this, "android.permission.WRITE_EXTERNAL_STORAGE");
        log("permission check: " + permissionCheck);
        if (permissionCheck < 0) {
            ActivityCompat.requestPermissions(this, new String[]{"android.permission.WRITE_EXTERNAL_STORAGE"}, 12345);
        }
        main = findViewById(R.id.main_container);
        chat = findViewById(R.id.chat_container);

        snackbar = Snackbar.make(main, "No internet connection", Snackbar.LENGTH_INDEFINITE);


        scheduleFragment = new ScheduleFragment();
        scheduleFragment.start();
        messagesFragment = new MessagesFragment();
        messagesFragment.start();

        loadFragment(scheduleFragment);
        BottomNavigationView bottomnav = findViewById(R.id.bottomnav);
        bottomnav.setOnNavigationItemSelectedListener(mNavigationListener);
        bottomnav.setSelectedItemId(R.id.navigation_diary);
    }


    void set(ArrayList<PeriodFragment.Subject> subjects, ArrayList<PeriodFragment.Day> days) {
        this.subjects = subjects;
        this.days = days;
        periodFragment = new PeriodFragment();
        periodFragment.subjects = subjects;
    }

    void loadFragment(Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.frame, fragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    public void setSupActionBar(android.support.v7.widget.Toolbar toolbar) {
        if(getSupportActionBar() == null)
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

    @Override
    public void onBackPressed() {
        log("fragments on MainActivity: " + getSupportFragmentManager().getBackStackEntryCount());
        List<Fragment> a = getSupportFragmentManager().getFragments();
        if(a.get(a.size()-1) instanceof ChatFragment) {
            log("last in stack = ChatFragment");
            set_visible(true);
            getSupportActionBar().setTitle("Messages");
            getSupportActionBar().setHomeButtonEnabled(false);
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            getSupportActionBar().setDisplayShowHomeEnabled(false);
        } else if (a.get(a.size() - 1) instanceof DayFragment) {
            log("last in stack = DayFragment");
            set_visible(true);
            getSupportActionBar().setTitle("Diary");
            getSupportActionBar().setHomeButtonEnabled(false);
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            getSupportActionBar().setDisplayShowHomeEnabled(false);
        } else if(a.get(a.size() - 1) instanceof MarkFragment) {
            log("last in stack = MarkFragment");
            getSupportActionBar().setTitle("Period");
            getSupportActionBar().setHomeButtonEnabled(false);
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            getSupportActionBar().setDisplayShowHomeEnabled(false);
        }
        if(!(getSupportFragmentManager().getBackStackEntryCount() == 0))
            if(!(a.get(a.size()-1) instanceof PeriodFragment || a.get(a.size()-1) instanceof ScheduleFragment
                    || a.get(a.size()-1) instanceof MessagesFragment))
                getSupportFragmentManager().popBackStack();
    }

    @Override
    protected void onResume() {
        super.onResume();
        log("onResume");
        try {
            registerReceiver(receiver, new IntentFilter("com.example.sch.action"));
            registerReceiver(internet_receiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        } catch (Exception e) {
            loge(e.toString());
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        log("onPause");
        try {
            unregisterReceiver(receiver);
            unregisterReceiver(internet_receiver);
        } catch (Exception e) {
            loge(e.toString());
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, 1, 0, "quit");
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                break;
            case 1:
                quit();
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
        SharedPreferences pref = getSharedPreferences("pref", 0);
        pref.edit().putBoolean("first_time", true).apply();
        finish();
    }

    Fragment getStackTop() {
        List<Fragment> a = getSupportFragmentManager().getFragments();
        return a.get(a.size() - 1);
    }

    public static boolean hasConnection(final Context context) {
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
    }
}
