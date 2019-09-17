package ru.gurhouse.sch;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
//import android.support.v4.widget.SwipeRefreshLayout;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Calendar;

public class PageFragment extends Fragment {

    static final String SAVE_PAGE_NUMBER = "save_page_number";
    TableLayout tableLayout;
    PeriodFragment.Day day;
    ArrayList<PeriodFragment.Subject> subjects;
    int pageNumber;
    Calendar c;
    int dayofweek;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_page, container, false);
        tableLayout = v.findViewById(R.id.table);
        if (day != null) {
            tableLayout.setColumnStretchable(1, true);
            tableLayout.setColumnShrinkable(1, true);
            CreateTable();
        } else {
            tableLayout.setColumnStretchable(0, true);
            tableLayout.setColumnShrinkable(0, true);
            TableRow tbrow1 = new TableRow(getContext());
            tbrow1.setLayoutParams(new TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT,
                    TableLayout.LayoutParams.WRAP_CONTENT));
            final TextView tv1 = new TextView(getContext());
            tv1.setText("Уроков нет");
            tv1.setLayoutParams(new TableRow.LayoutParams(
                    TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.MATCH_PARENT));
            tbrow1.addView(tv1);
            tv1.setTextColor(Color.GRAY);
            tv1.setGravity(Gravity.CENTER);
            tv1.setTextSize(30);
            tableLayout.addView(tbrow1);
//            final SwipeRefreshLayout refreshL = v.findViewById(R.id.refresh);
//            refreshL.setOnRefreshListener(() -> refreshL.setRefreshing(false));
        }
        return v;
    }

    public void CreateTable() {
        for (int i = 0; i < day.lessons.size(); i++) {
            final PeriodFragment.Lesson lesson = day.lessons.get(i);
            TableRow tbrow = new TableRow(getActivity());
            tbrow.setLayoutParams(new TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT,
                    TableLayout.LayoutParams.WRAP_CONTENT));

            tbrow.setBaselineAligned(false);

            TextView tv2 = new TextView(getActivity().getApplicationContext());
            tv2.setLayoutParams(new TableRow.LayoutParams(
                    TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT));

            TextView tv1 = new TextView(getActivity().getApplicationContext());
            tv1.setLayoutParams(new TableRow.LayoutParams(
                    TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.MATCH_PARENT));

            TextView tv3 = new TextView(getActivity().getApplicationContext());
            tv3.setLayoutParams(new TableRow.LayoutParams(
                    TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.MATCH_PARENT));

            tv2.setGravity(Gravity.CENTER_VERTICAL);
            tv1.setGravity(Gravity.CENTER);
            tv3.setGravity(Gravity.CENTER);

            tv1.setId(i);
            tv1.setTextColor(Color.WHITE);
            tv2.setId(i);
            tv3.setId(i);

            try {
                tbrow.setOnClickListener(v -> {
                    FragmentTransaction transaction = getFragmentManager().beginTransaction();
                    DayFragment fragment = new DayFragment();
                    transaction.replace(R.id.frame, fragment);
                    try {
                        fragment.homework = lesson.homeWork.stringwork;
                        fragment.files = lesson.homeWork.files;
                        fragment.teachername = lesson.teachername;
                        fragment.topic = lesson.topic;
                        fragment.marks = lesson.marks;
                        fragment.subjects = subjects;
                        fragment.name = lesson.name;
                    } catch (Exception ignore) {
                    }
                    transaction.addToBackStack(null);
                    transaction.commit();
                });
            } catch (Exception ignore) {
            }
            if (i - day.lessons.size() + 1 == 0) {
                tv1.setBackground(getResources().getDrawable(R.drawable.cell_phone2));
                tv2.setBackground(getResources().getDrawable(R.drawable.cell_phone2));
            } else {
                tv1.setBackground(getResources().getDrawable(R.drawable.cell_phone));
                tv2.setBackground(getResources().getDrawable(R.drawable.cell_phone));
                tv3.setBackground(getResources().getDrawable(R.drawable.cell_phone3));
            }
            System.out.println(lesson.numInDay);
            tv1.setText(String.valueOf(lesson.numInDay));
            try {
                String s = lesson.name + "\n" + lesson.homeWork.stringwork;
                Spannable spans = new SpannableString(s);
                spans.setSpan(new RelativeSizeSpan(1.5f), 0, s.indexOf("\n"), Spanned.SPAN_EXCLUSIVE_INCLUSIVE);
                spans.setSpan(new ForegroundColorSpan(Color.WHITE), 0, s.indexOf("\n"), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                spans.setSpan(new ForegroundColorSpan(Color.LTGRAY), s.indexOf("\n"), s.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                tv2.setText(spans);
            } catch (Exception ignore) {
            }
            tv1.setPadding(15, 0, 30, 0);
            tv2.setPadding(30, 30, 30, 30);
            tv3.setPadding(30, 0, 30, 0);
            tv2.setMaxLines(2);
            tv2.setEllipsize(TextUtils.TruncateAt.END);
            try {
                StringBuilder s1 = new StringBuilder();
                for (int j = 0; j < lesson.marks.size(); j++) {
                    if (lesson.marks.get(j).value != null && !lesson.marks.get(j).value.equals(" ") && !lesson.marks.get(j).value.equals("")) {
                        s1.append(lesson.marks.get(j).value);
                        if (lesson.marks.size() > 1 && j != lesson.marks.size() - 1 && lesson.marks.get(j + 1).value != null && !lesson.marks.get(j + 1).value.equals(" ") && !lesson.marks.get(j + 1).value.equals("")) {
                            s1.append("/");
                        }
                    }
                }
                s1.append("\n");
                for (int j = 0; j < lesson.marks.size(); j++) {
                    if (lesson.marks.get(j).value != null && !lesson.marks.get(j).value.equals(" ") && !lesson.marks.get(j).value.equals("")) {
                        s1.append(lesson.marks.get(j).coefficient);
                        if (lesson.marks.size() > 1 && j != lesson.marks.size() - 1 && lesson.marks.get(j + 1).value != null && !lesson.marks.get(j + 1).value.equals(" ") && !lesson.marks.get(j + 1).value.equals("")) {
                            s1.append("/");
                        }
                    }
                }
                Spannable spans1 = new SpannableString(s1.toString());
                spans1.setSpan(new RelativeSizeSpan(1.4f), 0, s1.indexOf("\n"), Spanned.SPAN_EXCLUSIVE_INCLUSIVE);
                spans1.setSpan(new ForegroundColorSpan(Color.WHITE), 0, s1.indexOf("\n"), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                spans1.setSpan(new RelativeSizeSpan(1.1f), s1.indexOf("\n"), s1.length(), Spanned.SPAN_EXCLUSIVE_INCLUSIVE);
                spans1.setSpan(new ForegroundColorSpan(Color.LTGRAY), s1.indexOf("\n"), s1.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                tv3.setText(spans1);
            } catch (Exception ignored) {
            }
            tbrow.addView(tv1);
            tbrow.addView(tv2);
            tbrow.addView(tv3);
            tableLayout.addView(tbrow);
        }
    }

    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(SAVE_PAGE_NUMBER, pageNumber);
    }


}