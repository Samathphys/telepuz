package ru.gurhouse.sch;

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
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.DateFormatSymbols;

import static ru.gurhouse.sch.LoginActivity.loge;

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
    ScheduleFragment.Period[] periods = new ScheduleFragment.Period[7];
    Double avg;
    String[] period;
    int pernum = 6;
    String rating;
    String totalmark;

    public SubjectFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_subject, container, false);
        LinearLayout linearLayout = v.findViewById(R.id.ll);
        periodname = period[pernum];
        linearLayout.setBaselineAligned(false);
        if(subname == null) {
            loge("subname null!");
            subname = "strange subname";
        }
        if (!subname.equals(" ") && !subname.equals("")) {
            TextView tv1 = new TextView(getActivity());
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
        LinearLayout ln1 = new LinearLayout(getActivity());
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
            TextView tv1 = new TextView(getActivity());
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            p.weight = (float) 1 / sum;
            tv1.setLayoutParams(p);
            String s1 = "Средний\nбалл:" + "\n" + avg;
            Spannable spans1 = new SpannableString(s1);
            spans1.setSpan(new RelativeSizeSpan(1f), 0, s1.lastIndexOf("\n"), Spanned.SPAN_EXCLUSIVE_INCLUSIVE);
            spans1.setSpan(new ForegroundColorSpan(Color.LTGRAY), 0, s1.lastIndexOf("\n"), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            spans1.setSpan(new RelativeSizeSpan(1.5f), s1.lastIndexOf("\n"), s1.length(), Spanned.SPAN_EXCLUSIVE_INCLUSIVE);
            spans1.setSpan(new ForegroundColorSpan(Color.WHITE), s1.lastIndexOf("\n"), s1.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            tv1.setText(spans1);
            tv1.setPadding(0, 50, 0, 50);
            tv1.setGravity(Gravity.CENTER);
            tv1.setOnClickListener(v1 -> {

                FragmentTransaction transaction = getFragmentManager().beginTransaction();
                Countcoff fragment = new Countcoff();
                transaction.replace(R.id.frame, fragment);
                try {
                    fragment.periods = periods;
                    fragment.subname = subname;
                    fragment.avg = avg;
                    fragment.period = period;
                    fragment.pernum = pernum;
                } catch (Exception ignore) {}
                transaction.addToBackStack(null);
                transaction.commit();
            });
            ln1.addView(tv1);
        }
        if (totalmark != " " && totalmark != "" && totalmark != null) {
            TextView tv1 = new TextView(getActivity());
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            p.weight = (float) 1 / sum;
            tv1.setLayoutParams(p);
            String s1 = "Оценка за\nпериод:" + "\n" + totalmark;
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
            TextView tv1 = new TextView(getActivity());
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            p.weight = (float) 1 / sum;
            tv1.setLayoutParams(p);
            String s1 = "Рейтинг\nв классе:" + "\n" + rating;
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
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        super.onCreateOptionsMenu(menu, inflater);
    }
}
