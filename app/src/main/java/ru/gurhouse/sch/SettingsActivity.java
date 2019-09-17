package ru.gurhouse.sch;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Switch;

import static ru.gurhouse.sch.KnockFragment.connect;
import static ru.gurhouse.sch.LoginActivity.log;
import static ru.gurhouse.sch.LoginActivity.loge;

public class SettingsActivity extends AppCompatActivity {

    private EditText et, et_nickname;
    private ImageView send;
    private boolean kk_enabled = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setNavigationBarColor(getResources().getColor(R.color.gr1));
        }

        et = findViewById(R.id.et_feedback);
        send = findViewById(R.id.btn_fbsend);

        et.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if(s.toString().trim().equals("")) {
                    send.setVisibility(View.GONE);
                } else
                    send.setVisibility(View.VISIBLE);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        send.setOnClickListener(v -> {
            final String text = et.getText().toString();
            new Thread() {
                @Override
                public void run() {
                    try {
                        connect("https://still-cove-90434.herokuapp.com/new_event",
                                "firebase_id=" + TheSingleton.getInstance().getFb_id() + "&event=feedback" +
                                        "&msg=" + text + "&time=" + System.currentTimeMillis());
                    } catch (Exception e) {loge(e.toString());}
                }
            }.start();
            et.setText("");
            startActivity(new Intent(getApplicationContext(), ThanksActivity.class));
        });

        Switch auto = findViewById(R.id.switch_auto);
        final SharedPreferences pref = getSharedPreferences("pref", 0);
        auto.setChecked(pref.getBoolean("auto", true));
        auto.setOnCheckedChangeListener((buttonView, isChecked) -> {
            log("switch1: " + isChecked);
            pref.edit().putBoolean("auto", isChecked).apply();

        });

        Switch period = findViewById(R.id.switch_period);
        period.setChecked(pref.getBoolean("period_normal", false));
        period.setOnCheckedChangeListener((buttonView, isChecked) -> pref.edit().putBoolean("period_normal", isChecked).apply());

        findViewById(R.id.btn_quit).setOnClickListener(v -> {
            setResult(RESULT_OK, new Intent().putExtra("goal", "quit"));
            finish();
        });


        Switch chat = findViewById(R.id.switch_chat);
        chat.setChecked(pref.getBoolean("show_chat", true));
        chat.setOnCheckedChangeListener((buttonView, isChecked) -> pref.edit().putBoolean("show_chat", isChecked).apply());

        et_nickname = findViewById(R.id.et_nickname);
        kk_enabled = !pref.getString("knock_name", "").equals("");
        if(!kk_enabled) {
            et_nickname.setEnabled(false);
            et_nickname.setText("");
        } else {
            et_nickname.setEnabled(true);
            et_nickname.setText(pref.getString("knock_name", ""));
        }

        findViewById(R.id.btn_ok).setOnClickListener(v -> {
            if(kk_enabled) {
                final String text = et_nickname.getText().toString();
                if(text.replaceAll(" ", "").equals(""))
                    return;
                et_nickname.clearFocus();
                hideKeyboard(SettingsActivity.this, et_nickname);
                new Thread() {
                    @Override
                    public void run() {
                        try {
                            connect("https://warm-bayou-37022.herokuapp.com/update",
                                    "column=name&id=" + pref.getString("knock_id", "") + "&value=" + text);
                        } catch (Exception e) {loge(e.toString());}
                    }
                }.start();
            }
        });

        Toolbar toolbar = findViewById(R.id.toolbar2);
        toolbar.setTitle("Настройки");
        setSupportActionBar(toolbar);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.clear();
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == android.R.id.home)
            finish();
        else {
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
            Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
    }

    private static void hideKeyboard(Context context, View view) {
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Activity.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }
}
