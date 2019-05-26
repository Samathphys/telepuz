package com.example.sch;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.support.v7.widget.Toolbar;

import static com.example.sch.LoginActivity.connect;
import static com.example.sch.LoginActivity.loge;

public class SettingsActivity extends AppCompatActivity {

    EditText et;
    Button send;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

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
                    send.setEnabled(false);
                } else
                    send.setEnabled(true);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String text = et.getText().toString();
                new Thread() {
                    @Override
                    public void run() {
                        try {
                            connect("https://still-cove-90434.herokuapp.com/new_event",
                                    "firebase_id=" + TheSingleton.getInstance().getFb_id() + "&event=feedback" +
                                            "&msg=" + text + "&time=" + System.nanoTime());
                        } catch (Exception e) {loge(e.toString());}
                    }
                }.start();
            }
        });

        Toolbar toolbar = findViewById(R.id.toolbar2);
        toolbar.setTitle("Settings");
        setSupportActionBar(toolbar);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, 1, 0, "Quit");
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
}
