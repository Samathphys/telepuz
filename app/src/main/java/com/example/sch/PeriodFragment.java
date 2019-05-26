package com.example.sch;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.crashlytics.android.Crashlytics;

import java.util.ArrayList;

public class PeriodFragment extends Fragment {


    ArrayList<Subject> subjects;
    ArrayList<TextView> txts;
    public PeriodFragment() {
        txts = new ArrayList<>();
    }

    static void sasha(String s) {
        Log.v("sasha", s);
    }

    static void sasha(Boolean s) {
        Log.v("sasha", String.valueOf(s));
    }

    static void sasha(Long s) {
        Log.v("sasha", String.valueOf(s));
    }

    LinearLayout layout1, layout2, layout3, layout;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.diary, container, false);
        StringBuilder y = new StringBuilder();
        layout = view.findViewById(R.id.linear);
        layout1 = view.findViewById(R.id.linear1);
        layout2 = view.findViewById(R.id.linear2);
        layout3 = view.findViewById(R.id.linear3);
        sasha("subjects: " + subjects.size());
        for (int i = 0; i < subjects.size() - 1; i++) {
            TextView txt1 = new TextView(getActivity().getApplicationContext());
            TextView txt2 = new TextView(getActivity().getApplicationContext());
            LinearLayout linearLayout = new LinearLayout(getActivity().getApplicationContext());
            txt1.setTextColor(Color.WHITE);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 0, 40, 10);
            txt1.setLayoutParams(lp);
            txt1.setGravity(Gravity.CENTER);
            txt1.setTextSize(20);
            txt2.setTextSize(20);
            txt2.setLayoutParams(lp);
            txt2.setTextColor(getResources().getColor(R.color.two));
            txt1.setText(subjects.get(i).shortname);
            txt2.setText(String.valueOf(subjects.get(i).avg));
            final int finalI1 = i;
            try {
                txt2.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        SwitchToSubjectFragment(subjects.get(finalI1).avg, subjects.get(finalI1).cells, subjects.get(finalI1).name, subjects.get(finalI1).rating, subjects.get(finalI1).totalmark);
                    }
                });
            } catch (Exception e) {
            }
            try {
                txt1.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        SwitchToSubjectFragment(subjects.get(finalI1).avg, subjects.get(finalI1).cells, subjects.get(finalI1).name, subjects.get(finalI1).rating, subjects.get(finalI1).totalmark);
                    }
                });
            } catch (Exception e) {
            }
            layout2.addView(txt2);
            layout1.addView(txt1);
            try {
                for (int j = 0; j < subjects.get(i).cells.size(); j++) {
                    if (subjects.get(i).cells.get(j).markvalue != null && subjects.get(i).cells.get(j).markvalue != "") {
                        Double d = subjects.get(i).cells.get(j).mktWt;
                        txts.add(new TextView(getActivity().getApplicationContext()));
                        LinearLayout.LayoutParams lp1 = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                        lp1.setMargins(0, 0, 10, 10);
                        txts.get(txts.size() - 1).setLayoutParams(lp1);
                        try {
                            final int finalI = i;
                            final int finalJ = j;
                            txts.get(txts.size() - 1).setOnClickListener(new View.OnClickListener() {

                                @Override
                                public void onClick(View v) {

                                    FragmentTransaction transaction = getFragmentManager().beginTransaction();
                                    MarkFragment fragment = new MarkFragment();
                                    transaction.replace(R.id.frame, fragment);
                                    try {
                                        fragment.coff = subjects.get(finalI).cells.get(finalJ).mktWt;
                                        fragment.data = subjects.get(finalI).cells.get(finalJ).date;
                                        fragment.markdata = subjects.get(finalI).cells.get(finalJ).markdate;
                                        fragment.teachname = subjects.get(finalI).cells.get(finalJ).teachFio;
                                        fragment.topic = subjects.get(finalI).cells.get(finalJ).lptname;
                                        fragment.value = subjects.get(finalI).cells.get(finalJ).markvalue;
                                        fragment.subject = subjects.get(finalI).name;
                                    } catch (Exception e) {
                                    }
                                    transaction.addToBackStack(null);
                                    transaction.commit();
                                }
                            });
                        } catch (Exception e) {
                        }
                        txts.get(txts.size() - 1).setTextSize(20);
                        txts.get(txts.size() - 1).setTextColor(Color.WHITE);
                        txts.get(txts.size() - 1).setBackground(getResources().getDrawable(R.drawable.gradient_list));
                        txts.get(txts.size() - 1).setPadding(15, 0, 15, 0);
//
                        if (d <= 0.5)
                            txts.get(txts.size() - 1).setBackgroundColor(getResources().getColor(R.color.coff1));
                        else if (d <= 1)
                            txts.get(txts.size() - 1).setBackgroundColor(getResources().getColor(R.color.coff2));
                        else if (d <= 1.25)
                            txts.get(txts.size() - 1).setBackgroundColor(getResources().getColor(R.color.coff3));
                        else if (d <= 1.35)
                            txts.get(txts.size() - 1).setBackgroundColor(getResources().getColor(R.color.coff4));
                        else if (d <= 1.5)
                            txts.get(txts.size() - 1).setBackgroundColor(getResources().getColor(R.color.coff5));
                        else if (d <= 1.75)
                            txts.get(txts.size() - 1).setBackgroundColor(getResources().getColor(R.color.coff6));
                        else if (d <= 2)
                            txts.get(txts.size() - 1).setBackgroundColor(getResources().getColor(R.color.coff7));
                        else
                            txts.get(txts.size() - 1).setBackgroundColor(getResources().getColor(R.color.coff8));

                        if (subjects.get(i).cells.get(j).markvalue != null)
                            txts.get(txts.size() - 1).setText(subjects.get(i).cells.get(j).markvalue);
                        else {
                            txts.get(txts.size() - 1).setText("7");
                            txts.get(txts.size() - 1).setTextColor(Color.TRANSPARENT);
                        }
                        linearLayout.addView(txts.get(txts.size() - 1));
                    }
                }
            } catch (Exception e) {
            }
            layout3.addView(linearLayout);
        }

        Toolbar toolbar = getActivity().findViewById(R.id.toolbar);
        toolbar.setTitle("Period");
        setHasOptionsMenu(true);
        ((MainActivity)getActivity()).setSupportActionBar(toolbar);
        return view;
    }

    public void SwitchToSubjectFragment(Double avg, ArrayList<Cell> cells, String name, String rating, String totalmark) {
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        SubjectFragment fragment = new SubjectFragment();
        transaction.replace(R.id.frame, fragment);
        try {
            fragment.avg = avg;
            fragment.cells = cells;
            fragment.subname = name;
            fragment.rating = rating;
            fragment.totalmark = totalmark;
        } catch (Exception e) {
        }
        transaction.addToBackStack(null);
        transaction.commit();
    }
    static class Cell {
        String lptname, markvalue, date;
        double mktWt = 0;
        Long lessonid;
        String markdate, teachFio;
        int unitid;

        Cell() {
        }
    }

    static class Day {
        Long daymsec;
        String day;
        int numday;
        ArrayList<Lesson> lessons;

        Day() {
        }
    }

    static class Lesson {
        int numInDay, numDay;
        String name = "", teachername = "", topic = "", shortname = "";
        HomeWork homeWork;
        ArrayList<Mark> marks = new ArrayList<>();
        Long id;
        long unitId = 0;

        Lesson() {
        }

    }

    static class HomeWork {
        ArrayList<Integer> idfils;
        String stringwork = "";
        HomeWork() {
        }
    }

    static class Mark {
        public int unitid;
        String value, teachFio, date, topic, markdate;
        double coefficient;
        Long idlesson;
        Cell cell;

        Mark() {
        }
    }

    public static class Subject {
        String name, rating = "", shortname = "", totalmark;
        double avg;
        int unitid;
        ArrayList<Cell> cells;

        Subject() {
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        menu.add(0, 1, 0, "Quit");
        menu.add(0, 2, 0, "CRASH");
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == 1) {
            ((MainActivity) getActivity()).quit();
        } else if(item.getItemId() == 2) {
            Crashlytics.getInstance().crash();
        }
        return super.onOptionsItemSelected(item);
    }
}