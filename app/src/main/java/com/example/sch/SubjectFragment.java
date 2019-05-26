package com.example.sch;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.widget.Toolbar;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.DateFormatSymbols;
import java.util.ArrayList;

public class SubjectFragment extends Fragment {

    private static DateFormatSymbols myDateFormatSymbols = new DateFormatSymbols() {

        @Override
        public String[] getWeekdays() {
            return new String[]{" ", "воскресенье", "понедельник", "вторник", "среду", "четверг", "пятницу", "субботу"};
        }

        @Override
        public String[] getMonths() {
            return new String[]{"января", "февраля", "марта", "апреля", "мая", "июня",
                    "июля", "августа", "сентября", "октября", "ноября", "декабря"};
        }

    };
    String subname;
    String periodname = "4 четверть"; // пока так
    ArrayList<PeriodFragment.Cell> cells;
    ArrayList<PeriodFragment.Subject> subjects;
    Double avg;
    String rating;
    String totalmark;


    public SubjectFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    Button btn;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_subject, container, false);
        LinearLayout linearLayout = v.findViewById(R.id.ll);
        linearLayout.setBaselineAligned(false);
        if (subname != " " && subname != "") {
            TextView tv1 = new TextView(getActivity().getApplicationContext());
            tv1.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            String s1 = subname;
            Spannable spans1 = new SpannableString(s1);
            spans1.setSpan(new RelativeSizeSpan(2f), 0, s1.length(), Spanned.SPAN_EXCLUSIVE_INCLUSIVE);
            spans1.setSpan(new ForegroundColorSpan(Color.WHITE), 0, s1.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            tv1.setText(spans1);
            tv1.setPadding(50, 50, 50, 10);
            tv1.setGravity(Gravity.CENTER);
            linearLayout.addView(tv1);
        }
        LinearLayout ln1 = new LinearLayout(getActivity().getApplicationContext());
        ln1.setOrientation(LinearLayout.HORIZONTAL);
        ln1.setWeightSum(1);
        int sum = 0;
        if (avg != null && avg != 0) {
            sum++;
        }
        if (rating != " " && rating != "" && rating != null) {
            sum++;
        }
        if (totalmark != " " && totalmark != "" && totalmark != null) {
            sum++;
        }
        if (avg != null && avg != 0) {
            TextView tv1 = new TextView(getActivity().getApplicationContext());
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            p.weight = (float) 1 / sum;
            tv1.setLayoutParams(p);
            String s1 = new StringBuilder().append("Средний\nбалл:").append("\n").append(avg).toString();
            Spannable spans1 = new SpannableString(s1);
            spans1.setSpan(new RelativeSizeSpan(1f), 0, s1.lastIndexOf("\n"), Spanned.SPAN_EXCLUSIVE_INCLUSIVE);
            spans1.setSpan(new ForegroundColorSpan(Color.LTGRAY), 0, s1.lastIndexOf("\n"), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            spans1.setSpan(new RelativeSizeSpan(1.5f), s1.lastIndexOf("\n"), s1.length(), Spanned.SPAN_EXCLUSIVE_INCLUSIVE);
            spans1.setSpan(new ForegroundColorSpan(Color.WHITE), s1.lastIndexOf("\n"), s1.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            tv1.setText(spans1);
            tv1.setPadding(0, 50, 0, 50);
            tv1.setGravity(Gravity.CENTER);
            tv1.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {

                    FragmentTransaction transaction = getFragmentManager().beginTransaction();
                    Countcoff fragment = new Countcoff();
                    transaction.replace(R.id.frame, fragment);
                    try {
                        fragment.subjects = subjects;
                        fragment.subname = subname;
                        fragment.avg = avg;
                    } catch (Exception e) {
                    }
                    transaction.addToBackStack(null);
                    transaction.commit();
                }
            });
            ln1.addView(tv1);
        }
        if (totalmark != " " && totalmark != "" && totalmark != null) {
            TextView tv1 = new TextView(getActivity().getApplicationContext());
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            p.weight = (float) 1 / sum;
            tv1.setLayoutParams(p);
            String s1 = new StringBuilder().append("Оценка за\nпериод:").append("\n").append(totalmark).toString();
            Spannable spans1 = new SpannableString(s1);
            spans1.setSpan(new RelativeSizeSpan(1f), 0, s1.lastIndexOf("\n"), Spanned.SPAN_EXCLUSIVE_INCLUSIVE);
            spans1.setSpan(new ForegroundColorSpan(Color.LTGRAY), 0, s1.lastIndexOf("\n"), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            spans1.setSpan(new RelativeSizeSpan(1.5f), s1.lastIndexOf("\n"), s1.length(), Spanned.SPAN_EXCLUSIVE_INCLUSIVE);
            spans1.setSpan(new ForegroundColorSpan(Color.WHITE), s1.lastIndexOf("\n"), s1.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            tv1.setText(spans1);
            tv1.setPadding(0, 50, 0, 50);
            tv1.setGravity(Gravity.CENTER);
            ln1.addView(tv1);
        }
        if (rating != " " && rating != "" && rating != null) {
            TextView tv1 = new TextView(getActivity().getApplicationContext());
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            p.weight = (float) 1 / sum;
            tv1.setLayoutParams(p);
            String s1 = new StringBuilder().append("Рейтинг\nв классе:").append("\n").append(rating).toString();
            Spannable spans1 = new SpannableString(s1);
            spans1.setSpan(new RelativeSizeSpan(1f), 0, s1.lastIndexOf("\n"), Spanned.SPAN_EXCLUSIVE_INCLUSIVE);
            spans1.setSpan(new ForegroundColorSpan(Color.LTGRAY), 0, s1.lastIndexOf("\n"), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            spans1.setSpan(new RelativeSizeSpan(1.5f), s1.lastIndexOf("\n"), s1.length(), Spanned.SPAN_EXCLUSIVE_INCLUSIVE);
            spans1.setSpan(new ForegroundColorSpan(Color.WHITE), s1.lastIndexOf("\n"), s1.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            tv1.setText(spans1);
            tv1.setPadding(0, 50, 0, 50);
            tv1.setGravity(Gravity.CENTER);
            ln1.addView(tv1);
        }
        linearLayout.addView(ln1);
        Toolbar toolbar = getActivity().findViewById(R.id.toolbar);
        toolbar.setTitle(periodname);
        setHasOptionsMenu(true);
        ((MainActivity) getActivity()).setSupActionBar(toolbar);
        ((MainActivity) getActivity()).getSupportActionBar().setHomeButtonEnabled(true);
        return v;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        menu.add(0, 1, 0, "Выход");
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == 1) {
            ((MainActivity) getActivity()).quit();
        }
        return super.onOptionsItemSelected(item);
    }
}
