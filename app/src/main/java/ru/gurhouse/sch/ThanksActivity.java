package ru.gurhouse.sch;

import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import static ru.gurhouse.sch.LoginActivity.log;

public class ThanksActivity extends AppCompatActivity {

    private int count = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_thanks);

        ImageView smile = findViewById(R.id.img_smile);
        smile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences pref = getSharedPreferences("pref", MODE_PRIVATE);
                if(!pref.getBoolean("easteregg1", false)) {
                    count++;
                    log("clicked " + count + " times");
                    if (count == 5) {
                        Toast.makeText(getApplicationContext(), "Поздравляю! Вы открыли пасхалку!", Toast.LENGTH_LONG).show();
                        pref.edit().putBoolean("easteregg1", true).apply();
                    }
                } else
                    log("пасхалка уже открыта");
            }
        });
        Button esc = findViewById(R.id.btn_ok);
        esc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }
}
