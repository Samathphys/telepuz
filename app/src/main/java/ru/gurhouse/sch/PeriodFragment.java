package ru.gurhouse.sch;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
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
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Locale;

import static ru.gurhouse.sch.LoginActivity.log;

public class PeriodFragment extends Fragment {

    ArrayList<TextView> txts;
    View view;
    String[] period;
    ScheduleFragment.Period[] periods = new ScheduleFragment.Period[7];
    int pernum = 6;
    Toolbar toolbar;
    AlertDialog.Builder alr;
    boolean shown = false;
    String periodname;
    boolean first_time = true;

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

    DialogInterface.OnClickListener myClickListener = new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int which) {
            ListView lv = ((AlertDialog) dialog).getListView();
            if (which == Dialog.BUTTON_POSITIVE) {
                TheSingleton.getInstance().t1 = System.currentTimeMillis();
                log("start");
                pernum = lv.getCheckedItemPosition();
                if (periods[pernum].subjects == null) {
                    ((MainActivity) getActivity()).scheduleFragment.Download2(periods[pernum].id, pernum, false, true);
                    periodname = periods[pernum].name;
                    alr.setSingleChoiceItems(period, pernum, myClickListener);
                    toolbar.setTitle(periodname);
                    view.findViewById(R.id.progress).setVisibility(View.VISIBLE);
                    view.findViewById(R.id.scrollView2).setVisibility(View.INVISIBLE);
                } else {
                    periodname = periods[pernum].name;
                    ((MainActivity) getActivity()).set(periods, pernum, 2);
                    alr.setSingleChoiceItems(period, pernum, myClickListener);
                    toolbar.setTitle(periodname);

                    show();
                }
                sasha("------------------------");
            } /*else {
            }*/
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        first_time = false;
        if (view == null)
            view = inflater.inflate(R.layout.diary, container, false);
        if (period == null)
            return view;
        toolbar = getActivity().findViewById(R.id.toolbar);
        periodname = period[pernum];
        alr = new AlertDialog.Builder(getContext());
        alr.create();
        alr.setSingleChoiceItems(period, pernum, myClickListener);
        alr.setTitle("Выберите период");
        alr.setPositiveButton("ok", myClickListener);
        toolbar.setTitle(periodname);
        toolbar.setOnClickListener(v -> alr.show());
        setHasOptionsMenu(true);
        ((MainActivity) getActivity()).setSupportActionBar(toolbar);
        sasha("sasa");
        if (periods[pernum] != null && !shown)
            show();
        return view;
    }

    void show() {
        layout = view.findViewById(R.id.linear);
        layout1 = view.findViewById(R.id.linear1);
        layout1.removeAllViews();
        layout2 = view.findViewById(R.id.linear2);
        layout2.removeAllViews();
        layout3 = view.findViewById(R.id.linear3);
        layout3.removeAllViews();
        sasha("subjects: " + periods[pernum].subjects.size());
        for (int i = 0; i < periods[pernum].subjects.size() - 1; i++) {
            TextView txt1 = new TextView(getActivity());
            TextView txt2 = new TextView(getActivity());
            LinearLayout linearLayout = new LinearLayout(getActivity());
            txt1.setTextColor(Color.WHITE);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 0, 40, 10);
            txt1.setLayoutParams(lp);
            txt1.setGravity(Gravity.CENTER);
            txt1.setTextSize(9 * getResources().getDisplayMetrics().density);
            txt2.setTextSize(9 * getResources().getDisplayMetrics().density);
            txt2.setLayoutParams(lp);
            txt2.setTextColor(getResources().getColor(R.color.two));
            txt1.setText(periods[pernum].subjects.get(i).shortname);

            if (periods[pernum].subjects.get(i).avg > 0) {
                txt2.setText(String.valueOf(periods[pernum].subjects.get(i).avg));
            } else {
                sasha(periods[pernum].subjects.get(i).shortname + " " + periods[pernum].subjects.get(i).cells.size());
                periods[pernum].subjects.get(i).avg = 0;
                Double d = 0.;
                Double f = 0.;
                int c = 0;
                for (int g = 0; g < periods[pernum].subjects.get(i).cells.size(); g++) {
                    if (periods[pernum].subjects.get(i).cells.get(g).markvalue != null)
                        if (periods[pernum].subjects.get(i).cells.get(g).markvalue.equals("1") || periods[pernum].subjects.get(i).cells.get(g).markvalue.equals("2") || periods[pernum].subjects.get(i).cells.get(g).markvalue.equals("3")
                                || periods[pernum].subjects.get(i).cells.get(g).markvalue.equals("4") || periods[pernum].subjects.get(i).cells.get(g).markvalue.equals("5")) {
                            d += Double.valueOf(periods[pernum].subjects.get(i).cells.get(g).markvalue) * periods[pernum].subjects.get(i).cells.get(g).mktWt;
                            f += periods[pernum].subjects.get(i).cells.get(g).mktWt;
                            c++;
                        }
                }
                sasha("here1 " + c);
                if (c > 0) {
                    sasha("here2");
                    String s = String.valueOf(d / f);
                    if (s.length() > 4) {
                        s = String.format(Locale.UK, "%.2f", d / f);
                    }
                    periods[pernum].subjects.get(i).avg = Double.valueOf(s);
                    sasha("here3");
                    txt2.setText(String.valueOf(periods[pernum].subjects.get(i).avg));
                } else
                    txt2.setText(" ");
            }
            final int finalI1 = i;
            txt2.setOnClickListener(v -> SwitchToSubjectFragment(periods[pernum].subjects.get(finalI1).avg, periods[pernum].subjects.get(finalI1).cells, periods[pernum].subjects.get(finalI1).name, periods[pernum].subjects.get(finalI1).rating, periods[pernum].subjects.get(finalI1).totalmark));
            txt1.setOnClickListener(v -> SwitchToSubjectFragment(periods[pernum].subjects.get(finalI1).avg, periods[pernum].subjects.get(finalI1).cells, periods[pernum].subjects.get(finalI1).name, periods[pernum].subjects.get(finalI1).rating, periods[pernum].subjects.get(finalI1).totalmark));
            layout2.addView(txt2);
            layout1.addView(txt1);
            if (periods[pernum].subjects.get(i).cells == null)
                continue;
            for (int j = 0; j < periods[pernum].subjects.get(i).cells.size(); j++) {
                if (periods[pernum].subjects.get(i).cells.get(j).markvalue != null && !periods[pernum].subjects.get(i).cells.get(j).markvalue.equals("")) {
                    double d = periods[pernum].subjects.get(i).cells.get(j).mktWt;
                    txts.add(new TextView(getActivity()));
                    LinearLayout.LayoutParams lp1 = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                    lp1.setMargins(0, 0, 10, 10);
                    txts.get(txts.size() - 1).setLayoutParams(lp1);
                    try {
                        final int finalI = i;
                        final int finalJ = j;
                        txts.get(txts.size() - 1).setOnClickListener(v -> {

                            FragmentTransaction transaction = getFragmentManager().beginTransaction();
                            MarkFragment fragment = new MarkFragment();
                            transaction.replace(R.id.frame, fragment);
                            try {
                                fragment.coff = periods[pernum].subjects.get(finalI).cells.get(finalJ).mktWt;
                                fragment.data = periods[pernum].subjects.get(finalI).cells.get(finalJ).date;
                                fragment.markdata = periods[pernum].subjects.get(finalI).cells.get(finalJ).markdate;
                                fragment.teachname = periods[pernum].subjects.get(finalI).cells.get(finalJ).teachFio;
                                fragment.topic = periods[pernum].subjects.get(finalI).cells.get(finalJ).lptname;
                                fragment.value = periods[pernum].subjects.get(finalI).cells.get(finalJ).markvalue;
                                fragment.subject = periods[pernum].subjects.get(finalI).name;
                            } catch (Exception ignore) {
                            }
                            transaction.addToBackStack(null);
                            transaction.commit();
                        });
                    } catch (Exception ignore) {
                    }
                    txts.get(txts.size() - 1).setTextSize(9 * getResources().getDisplayMetrics().density);
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

                    if (periods[pernum].subjects.get(i).cells.get(j).markvalue != null)
                        txts.get(txts.size() - 1).setText(periods[pernum].subjects.get(i).cells.get(j).markvalue);
                    else {
                        txts.get(txts.size() - 1).setText("7");
                        txts.get(txts.size() - 1).setTextColor(Color.TRANSPARENT);
                    }
                    linearLayout.addView(txts.get(txts.size() - 1));
                }
            }
            layout3.addView(linearLayout);
        }

        view.findViewById(R.id.progress).setVisibility(View.INVISIBLE);
        view.findViewById(R.id.scrollView2).setVisibility(View.VISIBLE);
    }

    public void SwitchToSubjectFragment(Double avg, ArrayList<Cell> cells, String name, String rating, String totalmark) {
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        SubjectFragment fragment = new SubjectFragment();
        transaction.replace(R.id.frame, fragment);
        try {
            fragment.avg = avg;
            fragment.subname = name;
            fragment.rating = rating;
            fragment.totalmark = totalmark;
            fragment.period = period;
            fragment.pernum = pernum;
            fragment.periods = periods;
        } catch (Exception ignore) {
        }
        transaction.addToBackStack(null);
        transaction.commit();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        MenuItem item;
        item = menu.add(0, 3, 0, "Settings");
        item.setIcon(R.drawable.settings);
        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        super.onCreateOptionsMenu(menu, inflater);
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
        ArrayList<File> files;
        String stringwork = "";
        HomeWork() {
        }
    }


    static class File {
        String name;
        int id;
    }

    static class Mark {
        int unitid;
        String value, teachFio, date, topic, markdate;
        double coefficient;
        Long idlesson;
        Cell cell;

        Mark() {
        }
    }

    static class Subject {
        String name, rating = "", shortname = "", totalmark;
        double avg = 0;
        int unitid;
        ArrayList<Cell> cells;

        Subject() {
        }
    }

    static class Cell {
        String lptname, markvalue, date;
        double mktWt = 0;
        Long lessonid;
        String markdate, teachFio;
        int unitid;

        Cell() {
        }

        Cell(Cell cell) {
            if (cell.markvalue != null)
                markvalue = cell.markvalue;
            mktWt = cell.mktWt;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // under construction (disabled)
            case 2:
                FragmentTransaction transaction = getFragmentManager().beginTransaction();
                TotalMarks fragment = new TotalMarks();
                transaction.replace(R.id.frame, fragment);
                transaction.addToBackStack(null);
                transaction.commit();
                break;
            case 3:
                Intent intent = new Intent(getContext(), SettingsActivity.class);
                startActivityForResult(intent, 0);
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}