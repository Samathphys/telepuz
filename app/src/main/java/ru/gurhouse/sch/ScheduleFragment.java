package ru.gurhouse.sch;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.Toolbar;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;

import static ru.gurhouse.sch.LoginActivity.connect;
import static ru.gurhouse.sch.LoginActivity.log;
import static ru.gurhouse.sch.LoginActivity.loge;

public class ScheduleFragment extends Fragment implements DatePickerDialog.OnDateSetListener {

    boolean READY = false;
    boolean shown = false;
    static int pageCount = 5001;
    boolean ready = false;
    String[] period;
    boolean first = true;
    private int USER_ID;
    private Context context;

    TextView []tv = new TextView[7];
    int day;
    Date datenow;
    int pernum;
    String[] days1 = {"пн", "вт", "ср", "чт", "пт", "сб", "вс"};
    ViewPager pager;
    PagerAdapter pagerAdapter;
    int lastposition;
    boolean autoChangingDate = true;
    ArrayList<PageFragment> pageFragments;
    View v;
    DatePickerDialog datePickerDialog;
    int[] week;
    int yearname = Calendar.getInstance().get(Calendar.MONTH) < Calendar.JULY ? Calendar.getInstance().get(Calendar.YEAR)-1 : Calendar.getInstance().get(Calendar.YEAR);
    KindaList[] s = new KindaList[11];
    String[] name = {"1 четверть", "2 четверть", "1 полугодие", "3 четверть", "4 четверть",
            "2 полугодие", "Годовая оценка", "Экзамен", "Оценка за ОГЭ", "Оценка в аттестат"};
    Period[] periods = new Period[7];

    public ScheduleFragment () {
        s[0] = new KindaList();
        s[0].add("");
        for (int i = 1; i < s.length; i++) {
            s[i] = new KindaList();
            s[i].add(name[i - 1]);
        }
        datenow = new Date();
        period = new String[7];
        Calendar c = Calendar.getInstance();
        c.setTime(datenow);
        c.add(Calendar.DAY_OF_WEEK, -1);
        day = c.get(Calendar.DAY_OF_WEEK);
        c.add(Calendar.DAY_OF_WEEK, 1);
        week = new int[7];

        for (int i = 0; i < 7; i++) {
            periods[i] = new Period();
        }

        pageFragments = new ArrayList<>();
        for (int i = 0; i < pageCount; i++) {
            pageFragments.add(new PageFragment());
            Calendar[] calendar = {Calendar.getInstance()};
            calendar[0] = Calendar.getInstance();
            calendar[0].setTime(datenow);
            calendar[0].add(Calendar.DAY_OF_MONTH, (i - (pageCount / 2) + 1));
            calendar[0].set(Calendar.HOUR_OF_DAY, 0);
            calendar[0].set(Calendar.MINUTE, 0);
            calendar[0].set(Calendar.SECOND, 0);
            calendar[0].set(Calendar.MILLISECOND, 0);
            pageFragments.get(i).c = calendar[0];
            calendar[0].add(Calendar.DAY_OF_WEEK, -1);
            pageFragments.get(i).dayofweek = calendar[0].get(Calendar.DAY_OF_WEEK);
        }
    }

    static void normallog(String s) {
        Log.v("normallog", s);
    }

    void start() {
        USER_ID = TheSingleton.getInstance().getUSER_ID();
        Download1();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if(super.getContext() != null)
            context = super.getContext();
        if(v == null)
            v = inflater.inflate(R.layout.fragment_schedule, container, false);
        if(!READY) {
            v = inflater.inflate(R.layout.fragment_schedule, container, false);
            return v;
        }
        if(!shown && periods[pernum].days != null)
            show();
        return v;
    }

    void show() {
        shown = true;
        log("show");
        try {
            for (int i = 0; i < pageCount; i++) {
                pageFragments.get(i).subjects = periods[pernum].subjects;
            }
            int y = 0;
            try {
                log("size: " +  periods[pernum].days.size());
                long daymsec = periods[pernum].days.get(y).daymsec;
                boolean z = false;
                for (int i = 0; i < pageCount; i++) {
                    //log("page " + periods[pernum].days.get(y));
                    if (pageFragments.get(i).c.getTimeInMillis() == daymsec || z) {
                        //z = true;
                        pageFragments.get(i).day = periods[pernum].days.get(y);
                        if (y + 1 - periods[pernum].days.size() == 0) {
                            log("diary drawing ended");
                            break;
                        } else {
                            y++;
                            daymsec = periods[pernum].days.get(y).daymsec;
                        }
                    }
//                    log("daymsec " + new Date(pageFragments.get(i).c.getTimeInMillis()).toString() + "; " + new Date(daymsec).toString());
                }
                log("y: " + y);
            } catch (Exception e) {
                loge("show " + e);
            }
            pager = v.findViewById(R.id.pager);
            if(pager.getAdapter() == null) {
                pagerAdapter = new MyFragmentPagerAdapter(getFragmentManager());
                pager.setAdapter(pagerAdapter);
            }
            pager.setCurrentItem(pageCount / 2 + 1);
            lastposition = pager.getCurrentItem();

            Calendar date = Calendar.getInstance();
            if(autoChangingDate && date.get(Calendar.HOUR_OF_DAY) >= 16) {
                date.add(Calendar.DATE, 1);
                pager.setCurrentItem(++lastposition);
            } else if(autoChangingDate && date.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
                date.add(Calendar.DATE, 1);
                pager.setCurrentItem(++lastposition);
            }
            datePickerDialog = new DatePickerDialog(getContext(), ScheduleFragment.this, date.get(Calendar.YEAR), date.get(Calendar.MONTH), date.get(Calendar.DATE));
            ViewPager.OnPageChangeListener onPageChangeListener = new ViewPager.OnPageChangeListener() {
                @Override
                public void onPageSelected(int position) {
                    int w;
                    if(pager.getCurrentItem() == -1) {
                        loge("current item is -1");
                        return;
                    }
                    if(pager.getCurrentItem() == 0)
                        pager.setCurrentItem(pageCount/2 + 1);
                    if (pageFragments.get(pager.getCurrentItem() - 1).c.get(Calendar.DAY_OF_WEEK) - 1 == 0) {
                        w = 7;
                    } else
                        w = pageFragments.get(pager.getCurrentItem() - 1).c.get(Calendar.DAY_OF_WEEK) - 1;

                    for (int i = w - 1; i < 7; i++) {
                        week[i] = pager.getCurrentItem() - 1 + i - w + 1 + 1;
                    }
                    for (int i = w - 2; i > -1; i--) {
                        week[i] = pager.getCurrentItem() - 1 + i - w + 1 + 1;
                    }
                    Calendar date = Calendar.getInstance();
                    if(autoChangingDate && date.get(Calendar.HOUR_OF_DAY) >= 16)
                        date.add(Calendar.DATE, 1);
                    else if(autoChangingDate && date.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY)
                        date.add(Calendar.DATE, 1);
                    date.add(Calendar.DATE, position-lastposition);
                    datePickerDialog.updateDate(date.get(Calendar.YEAR), date.get(Calendar.MONTH), date.get(Calendar.DATE));
                    Calendar MaxOrMinDate = Calendar.getInstance();
                    MaxOrMinDate.add(Calendar.YEAR,1);
                    datePickerDialog.getDatePicker().setMaxDate(MaxOrMinDate.getTimeInMillis());
                    MaxOrMinDate.add(Calendar.YEAR,-2);
                    datePickerDialog.getDatePicker().setMinDate(MaxOrMinDate.getTimeInMillis());
                    datePickerDialog.getDatePicker().setSpinnersShown(true);
                    day = w;
                    date.add(Calendar.DAY_OF_WEEK, -1);
                    okras(tv, date.get(Calendar.DAY_OF_WEEK)-1);
                }

                @Override
                public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                }

                @Override
                public void onPageScrollStateChanged(int state) {
                }
            };
            pager.setOnPageChangeListener(onPageChangeListener);
            onPageChangeListener.onPageSelected(pager.getCurrentItem());

//            for (int i = 0; i < 7; i++) {
//                tv[i].setTextColor(Color.WHITE);
//
//                String s = days1[i] + "\n" + pageFragments.get(week[i] - 1).c.get(Calendar.DAY_OF_MONTH);
//                Spannable spans = new SpannableString(s);
//                spans.setSpan(new RelativeSizeSpan(1.3f), 0, s.indexOf("\n"), Spanned.SPAN_EXCLUSIVE_INCLUSIVE);
//                spans.setSpan(new ForegroundColorSpan(Color.WHITE), 0, s.indexOf("\n"), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
//                spans.setSpan(new RelativeSizeSpan(1.2f), s.indexOf("\n"), s.length(), Spanned.SPAN_EXCLUSIVE_INCLUSIVE);
//                spans.setSpan(new ForegroundColorSpan(Color.WHITE), s.indexOf("\n"), s.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
//                tv[i].setText(spans);
//
//            }
            first = false;
            Toolbar toolbar = getActivity().findViewById(R.id.toolbar);
            toolbar.setTitle("Дневник");
            setHasOptionsMenu(true);
            ((MainActivity) getActivity()).setSupportActionBar(toolbar);
            v.findViewById(R.id.progressBar).setVisibility(View.INVISIBLE);
            v.findViewById(R.id.layout_fragment_bp_list).setVisibility(View.VISIBLE);
            if(TheSingleton.getInstance().t1 > 0) {
                log("loading ended in " + (System.currentTimeMillis() - TheSingleton.getInstance().t1) + " ms");
                //TheSingleton.getInstance().t1 = 0;
            }

        } catch (Exception e) {
            e.printStackTrace();
            loge("show: " + e.toString());
            Toolbar toolbar = getActivity().findViewById(R.id.toolbar);
            toolbar.setTitle("Дневник");
            toolbar.setClickable(false);
            setHasOptionsMenu(true);
            ((MainActivity) getActivity()).setSupportActionBar(toolbar);
            v.findViewById(R.id.progressBar).setVisibility(View.INVISIBLE);
            v.findViewById(R.id.layout_fragment_bp_list).setVisibility(View.VISIBLE);
        }
    }

    public void okras(TextView [] tv, int selected){
        for (int i = 0; i < 7; i++) {
            if(tv[i] == null) {
                tv[i] = new TextView(getContext());
                tv[i].setId(i);
                tv[i].setGravity(Gravity.CENTER);
                LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                p.weight = (float) 1 / 7;
                tv[i].setLayoutParams(p);
                final int finalI = i;
                tv[i].setOnClickListener(v -> {
                    pager.setCurrentItem(pager.getCurrentItem() - (day - finalI - 1));
                    day = finalI + 1;
                });
                LinearLayout linear1 = v.findViewById(R.id.liner1);
                linear1.setWeightSum(1);
                linear1.addView(tv[i]);
            }
            ForegroundColorSpan color;

            if(i == selected){
                tv[i].setBackground(getResources().getDrawable(R.drawable.cell_phone1));
                tv[i].setTextColor(Color.parseColor("#38423B"));
                color = new ForegroundColorSpan(Color.parseColor("#38423B"));
            }else {
                tv[i].setBackground(null);
                tv[i].setTextColor(Color.WHITE);
                color = new ForegroundColorSpan(Color.WHITE);
            }

            String s = days1[i] + "\n" + pageFragments.get(week[i] - 1).c.get(Calendar.DAY_OF_MONTH);
            Spannable spans = new SpannableString(s);
            spans.setSpan(new RelativeSizeSpan(1.3f), 0, s.indexOf("\n"), Spanned.SPAN_EXCLUSIVE_INCLUSIVE);
            spans.setSpan(color, 0, s.indexOf("\n"), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            spans.setSpan(new RelativeSizeSpan(1.2f), s.indexOf("\n"), s.length(), Spanned.SPAN_EXCLUSIVE_INCLUSIVE);
            spans.setSpan(color, s.indexOf("\n"), s.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            tv[i].setText(spans);
        }
    }

    void Download1() {
        log("Download1()");
        new Thread() {
            @Override
            public void run() {
                try {
                    JSONArray array3 = new JSONArray(
                            connect("https://app.eschool.center/ec-server/dict/periods2?year=" + yearname,
                            null));
                    for (int i = 0; i < array3.length(); i++) {
                        if (array3.getJSONObject(i).getInt("typeId") == 1) {
                            JSONArray array4 = array3.getJSONObject(i).getJSONArray("items");
                            for (int j = 0; j < array4.length(); j++) {
                                JSONObject ob = array4.getJSONObject(j);
                                switch (ob.getString("typeCode")) {
                                    case "Y":
                                        periods[0].datefinish = ob.getLong("date2");
                                        periods[0].datestart = ob.getLong("date1");
                                        periods[0].name = ob.getString("name");
                                        periods[0].id = ob.getInt("id");
                                        period[0] = periods[0].name;
                                        break;
                                    case "HY":
                                        if (ob.getInt("num") - 1 == 0) {
                                            periods[1].datefinish = ob.getLong("date2");
                                            periods[1].datestart = ob.getLong("date1");
                                            periods[1].name = ob.getString("name");
                                            periods[1].id = ob.getInt("id");
                                            period[1] = periods[1].name;
                                        } else {
                                            periods[2].datefinish = ob.getLong("date2");
                                            periods[2].datestart = ob.getLong("date1");
                                            periods[2].name = ob.getString("name");
                                            periods[2].id = ob.getInt("id");
                                            period[2] = periods[2].name;
                                        }
                                        break;
                                    case "Q":
                                        if (ob.getInt("num") - 1 == 0) {
                                            periods[3].datefinish = ob.getLong("date2");
                                            periods[3].datestart = ob.getLong("date1");
                                            periods[3].name = ob.getString("name");
                                            periods[3].id = ob.getInt("id");
                                            period[3] = periods[3].name;
                                        } else if (ob.getInt("num") - 2 == 0) {
                                            periods[4].datefinish = ob.getLong("date2");
                                            periods[4].datestart = ob.getLong("date1");
                                            periods[4].name = ob.getString("name");
                                            periods[4].id = ob.getInt("id");
                                            period[4] = periods[4].name;
                                        } else if (ob.getInt("num") - 3 == 0) {
                                            periods[5].datefinish = ob.getLong("date2");
                                            periods[5].datestart = ob.getLong("date1");
                                            periods[5].name = ob.getString("name");
                                            periods[5].id = ob.getInt("id");
                                            period[5] = periods[5].name;
                                        } else {
                                            periods[6].datefinish = ob.getLong("date2");
                                            periods[6].datestart = ob.getLong("date1");
                                            periods[6].name = ob.getString("name");
                                            periods[6].id = ob.getInt("id");
                                            period[6] = periods[6].name;
                                        }
                                        break;
                                }
                            }
                            break;
                        }
                    }

                    if (datenow.getTime() >= periods[3].datestart && datenow.getTime() <= periods[3].datefinish) {
                        Download2(periods[3].id, 3, true, true);
                    } else if (datenow.getTime() >= periods[4].datestart && datenow.getTime() <= periods[4].datefinish) {
                        Download2(periods[4].id, 4, true, true);
                    } else if (datenow.getTime() >= periods[5].datestart && datenow.getTime() <= periods[5].datefinish) {
                        Download2(periods[5].id, 5, true, true);
                    } else {
                        Download2(periods[6].id, 6, true, true);
                    }
                } catch (LoginActivity.NoInternetException e) {
                    getActivity().runOnUiThread(() ->
                            Toast.makeText(context, "Нет интернета", Toast.LENGTH_SHORT).show());
                } catch (Exception e) {
                    loge("Download1() " + e.toString());
                }
            }
        }.start();
    }

    JSONObject object1 = null;
    boolean first_downl = true;
    void Download2(final int id, final int h, final boolean isone, final boolean istwo) {
        shown = false;
        normallog("SchF: Download2()");
        if(first_downl && TheSingleton.getInstance().t1 == 0) {
            log("start");
            TheSingleton.getInstance().t1 = System.currentTimeMillis();
            first_downl = false;
        }
        if(periods.length == 0)
            normallog("SchF/Download2: periods are empty");
        pernum = h;
        periods[pernum].days = new ArrayList<>();
        periods[pernum].subjects = new ArrayList<>();
        periods[pernum].lins = new ArrayList<>();
        periods[pernum].cells = new ArrayList<>();
        new Thread() {
            @SuppressLint("SimpleDateFormat")
            @Override
            public void run() {
                try {
                    //------------------------------------------------------------------------------------------------

                    new Thread() {
                        @Override
                        public void run() {
                            try {
                                object1 = new JSONObject(
                                        connect("https://app.eschool.center/ec-server/student/getDiaryPeriod?userId=" + USER_ID + "&eiId=" + id,
                                                null));
                            } catch (LoginActivity.NoInternetException ignore) {
                            } catch (Exception e) {
                                e.printStackTrace();
                                loge(e.toString());
                            }
                        }
                    }.start();

                    JSONObject object = new JSONObject(
                            connect("https://app.eschool.center/ec-server/student/getDiaryUnits?userId=" + USER_ID + "&eiId=" + id,
                                    null));
                    if(!object.has("result"))
                        log("lol no result: " + object.toString());
                    JSONArray array = object.getJSONArray("result");
                    for (int i = 0; i < array.length(); i++) {
                        PeriodFragment.Subject subject = new PeriodFragment.Subject();
                        JSONObject obj = array.getJSONObject(i);
                        if (obj.has("overMark")) {
                            double d = obj.getDouble("overMark");
                            String s = String.valueOf(d);
                            if (s.length() > 4) {
                                s = String.format(Locale.UK, "%.2f", d);
                            }
                            subject.avg = Double.valueOf(s);
                        }
                        if (obj.has("totalMark"))
                            subject.totalmark = obj.getString("totalMark");
                        if (obj.has("unitName"))
                            subject.name = obj.getString("unitName");
                        if (obj.has("rating"))
                            subject.rating = obj.getString("rating");
                        if (obj.has("unitId"))
                            subject.unitid = obj.getInt("unitId");
                        subject.cells = new ArrayList<>();
                        normallog((i + 1) + ". name (" + subject.name + "), avg (" + subject.avg + "), tm (" + subject.totalmark + "), rating (" + subject.rating + ")");
                        periods[pernum].subjects.add(subject);
                    }

                    while (object1 == null) {
                        Thread.sleep(10);
                    }

                    JSONArray arraydaylessons = object1.getJSONArray("result");
                    normallog("SchF/Downloud2: arraydaylessons.length = " + arraydaylessons.length());
                        for (int i = 0; i < arraydaylessons.length(); i++) {
                            object1 = arraydaylessons.getJSONObject(i);
                            PeriodFragment.Cell cell = new PeriodFragment.Cell();
                            if (object1.has("lptName"))
                                cell.lptname = object1.getString("lptName");
                            if (object1.has("markDate"))
                                cell.markdate = object1.getString("markDate");
                            if (object1.has("lessonId"))
                                cell.lessonid = object1.getLong("lessonId");
                            if (object1.has("markVal"))
                                cell.markvalue = object1.getString("markVal");
                            if (object1.has("mktWt"))
                                cell.mktWt = object1.getDouble("mktWt");
                            if (object1.has("teachFio"))
                                cell.teachFio = object1.getString("teachFio");
                            if (object1.has("startDt"))
                                cell.date = object1.getString("startDt");
                            if (object1.has("unitId"))
                                cell.unitid = object1.getInt("unitId");
                            periods[pernum].cells.add(cell);
                        }
                    Date date = new Date();
                    if (periods[pernum].cells.size() == 0) {
                        if (periods[pernum].datestart <= date.getTime() && periods[pernum].datefinish >= date.getTime()) {
                            normallog("SchF/Download2: eschool - какашечка, жиденькая такая ");
                            periods[pernum].days = new ArrayList<>();
                            periods[pernum].subjects = new ArrayList<>();
                            periods[pernum].lins = new ArrayList<>();
                            periods[pernum].cells = new ArrayList<>();
                            Download2(id, h, isone, istwo);
                        } else {
                            periods[pernum].nullsub = true;
                            ((MainActivity) getActivity()).nullsub(periods, pernum);
                        }
                        normallog("SchF/Download2: nullsub, pernum - " + pernum);
                    } else {

                    String s1 = periods[pernum].cells.get(0).date;
                    String s2 = periods[pernum].cells.get(periods[pernum].cells.size() - 1).date;
                    DateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ENGLISH);
                    long d1 = format.parse(s1).getTime();
                    long d2 = format.parse(s2).getTime();
                    JSONObject object2 = new JSONObject(
                            connect("https://app.eschool.center/ec-server/student/diary?" +
                                    "userId=" + USER_ID + "&d1=" + d1 + "&d2=" + d2, null));
                    JSONArray array2 = object2.getJSONArray("lesson");

                    Long day1 = 0L;
                    Long date1;
                    int isODOD;
                    PeriodFragment.Day thisday = new PeriodFragment.Day();
                    thisday.odods = new ArrayList<>();
                    int index = -1;
                    log(0);
                    for (int i = 0; i < array2.length(); i++) {
                        object2 = array2.getJSONObject(i);
                        date1 = Long.valueOf(String.valueOf(object2.getString("date")));
                        date = new Date(date1);
                        Calendar c = Calendar.getInstance();
                        Calendar c1 = Calendar.getInstance();
                        c.setTime(date);
                        date = new Date(day1);
                        c1.setTime(date);
                        date1 = c.getTime().getTime();
                        date = new Date(date1);
                        isODOD = object2.getInt("isODOD");
                        if (c.get(Calendar.DAY_OF_YEAR) != c1.get(Calendar.DAY_OF_YEAR) ) {
                            index++;
                            thisday = new PeriodFragment.Day();
                            thisday.odods = new ArrayList<>();
                            thisday.day = String.valueOf(date);
                            thisday.daymsec = date1;
                            Date datathis = new Date();
                            datathis.setTime(date1);
                            SimpleDateFormat dateFormat2 = new SimpleDateFormat("EEE", Locale.ENGLISH);
                            String dayOfTheWeek = dateFormat2.format(datathis);
                            switch (dayOfTheWeek) {
                                case "Mon":
                                    thisday.numday = 1;
                                    break;
                                case "Tue":
                                    thisday.numday = 2;
                                    break;
                                case "Wed":
                                    thisday.numday = 3;
                                    break;
                                case "Thu":
                                    thisday.numday = 4;
                                    break;
                                case "Fri":
                                    thisday.numday = 5;
                                    break;
                                case "Sat":
                                    thisday.numday = 6;
                                    break;
                                case "Sun":
                                    thisday.numday = 7;
                                    break;
                                }
                            thisday.lessons = new ArrayList<>();
                            periods[pernum].days.add(thisday);
                        }
                        if (isODOD == 0) {
                            PeriodFragment.Lesson lesson = new PeriodFragment.Lesson();
                            lesson.id = object2.getLong("id");
                            lesson.numInDay = object2.getInt("numInDay");
                            if (object2.getJSONObject("unit").has("id"))
                                lesson.unitId = object2.getJSONObject("unit").getLong("id");
                            if (object2.getJSONObject("unit").has("name"))
                                lesson.name = object2.getJSONObject("unit").getString("name");
                            if (object2.getJSONObject("unit").has("short"))
                                lesson.shortname = object2.getJSONObject("unit").getString("short");
                            if (object2.getJSONObject("tp").has("topicName"))
                                lesson.topic = object2.getJSONObject("tp").getString("topicName");
                            if (object2.getJSONObject("teacher").has("factTeacherIN"))
                                lesson.teachername = object2.getJSONObject("teacher").getString("factTeacherIN");
                            JSONArray ar = object2.getJSONArray("part");
                            lesson.homeWork = new PeriodFragment.HomeWork();
                            lesson.homeWork.stringwork = "";
                            lesson.homeWork.files = new ArrayList<>();
                            for (int j = 0; j < ar.length(); j++) {
                                if (ar.getJSONObject(j).getString("cat").equals("DZ")) {
                                    if (ar.getJSONObject(j).has("variant")) {
                                        JSONArray ar1 = ar.getJSONObject(j).getJSONArray("variant");
                                        for (int k = 0; k < ar1.length(); k++) {
                                            if (ar1.getJSONObject(k).has("text")) {
                                                lesson.homeWork.stringwork += ar1.getJSONObject(k).getString("text") + "\n";
                                            }
                                            if (ar1.getJSONObject(k).has("file")) {
                                                JSONArray ar2 = ar1.getJSONObject(k).getJSONArray("file");
                                                PeriodFragment.File file;
                                                for (int l = 0; l < ar2.length(); l++) {
                                                    file = new PeriodFragment.File();
                                                    file.name = ar2.getJSONObject(l).getString("fileName");
                                                    file.id = ar2.getJSONObject(l).getInt("id");
                                                    lesson.homeWork.files.add(file);
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            periods[pernum].days.get(index).lessons.add(lesson);
                        }
                        else{
                            PeriodFragment.ODOD odod = new PeriodFragment.ODOD();
                            odod.duration = object2.getInt("duration");
                            odod.ODODid = object2.getInt("isODOD");
                            odod.daymsec = object2.getLong("date");
                            odod.day = object2.getString("date_d");
                            odod.name = object2.getJSONObject("clazz").getString("name");
                            thisday.odods.add(odod);
                        }
                        day1 = date1;
                    }

                    log(1);
                    for (int i = 0; i < periods[pernum].days.size(); i++) {
                        for (int j = 0; j < periods[pernum].cells.size(); j++) {
                            PeriodFragment.Cell cell = periods[pernum].cells.get(j);
                            s1 = cell.date;
                            format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ENGLISH);
                            d1 = format.parse(s1).getTime();
                            if (cell.mktWt != 0) {
                                if (periods[pernum].days.get(i).daymsec - d1 == 0 || periods[pernum].days.get(i).daymsec.equals(d1)) {
                                    for (int k = 0; k < periods[pernum].days.get(i).lessons.size(); k++) {
                                        if (periods[pernum].days.get(i).lessons.get(k).id.equals(cell.lessonid)) {
                                            PeriodFragment.Mark mark = new PeriodFragment.Mark();
                                            mark.cell = cell;
                                            mark.idlesson = cell.lessonid;
                                            mark.coefficient = cell.mktWt;
                                            if (cell.markvalue != null)
                                                mark.value = cell.markvalue;
                                            else
                                                mark.value = "";
                                            mark.teachFio = cell.teachFio;
                                            mark.markdate = cell.markdate;
                                            mark.date = cell.date;

                                            mark.topic = cell.lptname;
                                            mark.unitid = cell.unitid;
                                            for (int l = 0; l < periods[pernum].subjects.size(); l++) {
                                                if (periods[pernum].subjects.get(l).unitid == mark.unitid) {
                                                    periods[pernum].subjects.get(l).cells.add(cell);
                                                }
                                                if (periods[pernum].subjects.get(l).shortname == null || periods[pernum].subjects.get(l).shortname.isEmpty()) {
                                                    PeriodFragment.Subject subject = periods[pernum].subjects.get(l);
                                                    switch (subject.name) {
                                                        case "Физика":
                                                        case "Химия":
                                                        case "История":
                                                        case "Алгебра":
                                                            subject.shortname = subject.name;
                                                            break;
                                                        case "БЕСЕДЫ КЛ РУК":
                                                            subject.shortname = "Кл. Час";
                                                            break;
                                                        case "Иностранный язык":
                                                            subject.shortname = "Ин. Яз.";
                                                            break;
                                                        case "Алгебра и начала анализа":
                                                            subject.shortname = "Алгебра";
                                                            break;
                                                        case "Информатика и ИКТ":
                                                            subject.shortname = "Информ.";
                                                            break;
                                                        case "Биология":
                                                            subject.shortname = "Биолог.";
                                                            break;
                                                        case "География":
                                                            subject.shortname = "Геогр.";
                                                            break;
                                                        case "Геометрия":
                                                            subject.shortname = "Геометр.";
                                                            break;
                                                        case "Литература":
                                                            subject.shortname = "Лит-ра";
                                                            break;
                                                        case "Обществознание":
                                                            subject.shortname = "Общ.";
                                                            break;
                                                        case "Русский язык":
                                                            subject.shortname = "Рус. Яз.";
                                                            break;
                                                        case "Физическая культура":
                                                            subject.shortname = "Физ-ра";
                                                            break;
                                                        default:
                                                            periods[pernum].subjects.get(l).shortname = periods[pernum].subjects.get(l).name.substring(0, 3);
                                                    }

                                                }
//                                                if (periods[pernum].days.get(i).lessons.get(k).shortname.equals("Обществозн."))
//                                                    periods[pernum].subjects.get(l).shortname = "Общест.";
//                                                else if (periods[pernum].days.get(i).lessons.get(k).shortname.equals("Физ. культ."))
//                                                    periods[pernum].subjects.get(l).shortname = "Физ-ра";
//                                                else if (periods[pernum].days.get(i).lessons.get(k).shortname.equals("Инф. и ИКТ"))
//                                                    periods[pernum].subjects.get(l).shortname = "Информ.";
//                                                else if (periods[pernum].subjects.get(l).shortname != null)
//                                                    periods[pernum].subjects.get(l).shortname = periods[pernum].days.get(i).lessons.get(k).shortname;
//                                                else
//                                                    periods[pernum].subjects.get(l).shortname = periods[pernum].days.get(i).lessons.get(l).name.substring(0,6);
//                                            }
                                            }
                                            periods[pernum].days.get(i).lessons.get(k).marks.add(mark);
                                        }
                                    }
                                }
                            }
                        }
                    }
                    ready = true;

                    if (getActivity() != null) {
                        if (((MainActivity) getActivity()).getStackTop() instanceof ScheduleFragment)
                            getActivity().runOnUiThread(() -> show());
                    }

                    // цикл на ~3 секунд
                    log(2);
                    long matvey = 0, a = 0, b = 0, c = 0, de = 0, ef = 0, t1;
                    TextView txt, txt1;
                    LinearLayout.LayoutParams lp, lp3;
                    LinearLayout lin, lin1, lin2;
                    String s;
                    for (int i = 0; i < periods[pernum].days.size(); i++) {
                        if (periods[pernum].days.get(i) != null) {
                            lin = new LinearLayout(getContext());
                            boolean ask = true;
                            lin.setOrientation(LinearLayout.VERTICAL);
                            lin.setGravity(Gravity.CENTER);
                            if (i + 1 - periods[pernum].days.size() != 0) {
                                if (i == 0) {
                                    lin.setBackground(getResources().getDrawable(R.drawable.cell_phone6));
                                } else {
                                    lin.setBackground(getResources().getDrawable(R.drawable.cell_phone4));
                                }
                            } else {
                                lin.setBackground(getResources().getDrawable(R.drawable.cell_phone5));
                            }
                            lin.setPadding(30, 0, 30, 0);

                            lin2 = new LinearLayout(getContext());
                            lin2.setOrientation(LinearLayout.HORIZONTAL);
                            lin2.setGravity(Gravity.CENTER);
                            txt = new TextView(getContext());
                            txt.setGravity(Gravity.CENTER);
                            txt.setTextSize(20);
                            s = "";
                            lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                            lp.setMargins(0, 0, 0, 10);
                            txt.setLayoutParams(lp);
                            try {
                                date = new Date();
                                date.setTime(periods[pernum].days.get(i).daymsec);
                                format = new SimpleDateFormat("dd.MM", Locale.ENGLISH);
                                s = format.format(date);
                            } catch (Exception e) {
                            }
                            txt.setText(s);
                            txt.setTextColor(Color.LTGRAY);
                            lin2.addView(txt);
                            lin.addView(lin2);
                            for (int j = 0; j < periods[pernum].subjects.size(); j++) {
                                lin1 = new LinearLayout(getContext());
                                lin1.setOrientation(LinearLayout.HORIZONTAL);
                                lin1.setGravity(Gravity.CENTER);
                                normallog(periods[pernum].days.get(i).lessons.size() + " " + periods[pernum].subjects.get(j).name);
                                for (int k = 0; k < periods[pernum].days.get(i).lessons.size(); k++) {
                                    final PeriodFragment.Lesson less = periods[pernum].days.get(i).lessons.get(k);
                                    if (periods[pernum].subjects.get(j).unitid - less.unitId == 0) {
                                        if (less.marks != null) {
                                            for (int l = 0; l < less.marks.size(); l++) {
                                                t1 = System.currentTimeMillis();
                                                matvey++;
                                                txt1 = new Kletka(getContext());
                                                a += System.currentTimeMillis() - t1;
                                                t1 = System.currentTimeMillis();
                                                final double d = less.marks.get(l).coefficient;
                                                if (d <= 0.5)
                                                    txt1.setBackgroundColor(getResources().getColor(R.color.coff1));
                                                else if (d <= 1)
                                                    txt1.setBackgroundColor(getResources().getColor(R.color.coff2));
                                                else if (d <= 1.25)
                                                    txt1.setBackgroundColor(getResources().getColor(R.color.coff3));
                                                else if (d <= 1.35)
                                                    txt1.setBackgroundColor(getResources().getColor(R.color.coff4));
                                                else if (d <= 1.5)
                                                    txt1.setBackgroundColor(getResources().getColor(R.color.coff5));
                                                else if (d <= 1.75)
                                                    txt1.setBackgroundColor(getResources().getColor(R.color.coff6));
                                                else if (d <= 2)
                                                    txt1.setBackgroundColor(getResources().getColor(R.color.coff7));
                                                else
                                                    txt1.setBackgroundColor(getResources().getColor(R.color.coff8));
                                                txt1.setTextSize(20);
                                                b += System.currentTimeMillis() - t1;
                                                t1 = System.currentTimeMillis();
                                                try {
                                                    final int finalJ = j;
                                                    final int finalL = l;
                                                    txt1.setOnClickListener(v -> {
                                                        FragmentTransaction transaction = getFragmentManager().beginTransaction();
                                                        MarkFragment fragment = new MarkFragment();
                                                        transaction.replace(R.id.frame, fragment);
                                                        try {
                                                            fragment.coff = d;
                                                            fragment.data = less.marks.get(finalL).date;
                                                            fragment.markdata = less.marks.get(finalL).markdate;
                                                            fragment.teachname = less.marks.get(finalL).teachFio;
                                                            fragment.topic = less.marks.get(finalL).topic;
                                                            fragment.value = less.marks.get(finalL).value;
                                                            fragment.subject = periods[pernum].subjects.get(finalJ).name;
                                                        } catch (Exception ignore) {
                                                        }
                                                        transaction.addToBackStack(null);
                                                        transaction.commit();
                                                    });
                                                } catch (Exception ignore) {
                                                }
                                                c += System.currentTimeMillis() - t1;
                                                t1 = System.currentTimeMillis();
                                                ask = false;
                                                lp3 = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                                                lp3.setMargins(0, 0, 0, 10);
                                                txt1.setLayoutParams(lp3);
                                                if (less.marks.get(l).value == null || less.marks.get(l).value.equals("")) {
                                                    txt1.setText("  ");
                                                } else {
                                                    txt1.setText(less.marks.get(l).value);
                                                }
                                                lin1.addView(txt1);
                                                de += System.currentTimeMillis() - t1;
                                            }
                                        }
                                    }
                                }
                                t1 = System.currentTimeMillis();
                                if (ask) {
                                    lin1 = new LinearLayout(getContext());
                                    lin1.setOrientation(LinearLayout.HORIZONTAL);
                                    lin1.setGravity(Gravity.CENTER);
                                    txt1 = new TextView(getContext());
                                    txt1.setGravity(Gravity.CENTER);
                                    txt1.setTextSize(20);
                                    lp3 = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                                    lp3.setMargins(0, 0, 0, 10);
                                    txt1.setLayoutParams(lp3);
                                    txt1.setText("—");
                                    txt1.setTextColor(Color.LTGRAY);
                                    lin1.addView(txt1);
                                    lin.addView(lin1);
                                }
                                ef += System.currentTimeMillis() - t1;
                                ask = true;
                                if (lin1.getParent() != null)
                                    ((ViewGroup) lin1.getParent()).removeView(lin1);
                                lin.addView(lin1);
                            }
                            periods[pernum].lins.add(lin);
                        }
                    }
                    log("a " + a);
                    log("b " + b);
                    log("c " + c);
                    log("e " + de);
                    log("f " + ef);
                    log(3);
                    log("matvey: " + matvey);
                    if (isone)
                        ((MainActivity) getActivity()).set(periods, pernum, 1);
                    if (istwo)
                        ((MainActivity) getActivity()).set(periods, pernum, 2);
                    READY = true;

                    log("Download2() ended");
                    if (TheSingleton.getInstance().t1 > 0) {
                        final long t = System.currentTimeMillis() - TheSingleton.getInstance().t1;
                        log("loading ended in " + t + " ms");
                        TheSingleton.getInstance().t1 = 0;
                    }
                    first_downl = true;
                    }


                    //---------------------------------------------------------------------------------------------------------------------------------
                } catch (LoginActivity.NoInternetException e) {
                    getActivity().runOnUiThread(() -> {
                         Toast.makeText(context, "Нет интернета", Toast.LENGTH_SHORT).show();
                        ((SwipeRefreshLayout) v.findViewById(R.id.refresh)).setRefreshing(false);
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    loge("Download2() " + e.toString());
                    periods[pernum].days = new ArrayList<>();
                    periods[pernum].subjects = new ArrayList<>();
                    periods[pernum].lins = new ArrayList<>();
                    periods[pernum].cells = new ArrayList<>();
                    Download2(id, h, isone, istwo);
                }
            }
        }.start();
    }

    @Override
    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
        Calendar c1 = (Calendar) pageFragments.get(pager.getCurrentItem()).c.clone();
        Calendar c2 = Calendar.getInstance();
        c1.setTime(datenow);
        c1.set(Calendar.YEAR, year);
        c1.set(Calendar.MONTH, month);
        c1.set(Calendar.DAY_OF_MONTH, dayOfMonth);
        c1.set(Calendar.HOUR_OF_DAY, 0);
        c1.set(Calendar.MINUTE, 0);
        c1.set(Calendar.SECOND, 0);
        c1.set(Calendar.MILLISECOND, 0);
        c2.setTime(datenow);
        c2.set(Calendar.HOUR_OF_DAY, 0);
        c2.set(Calendar.MINUTE, 0);
        c2.set(Calendar.SECOND, 0);
        c2.set(Calendar.MILLISECOND, 0);

        int s = (int)((c1.getTimeInMillis() - c2.getTimeInMillis()) / 86400000);
        pager.setCurrentItem(pageCount / 2 + 1 + s);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        MenuItem item = menu.add(0, 2, 0, "Календарь");
        item.setIcon(R.drawable.calendar);
        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        item = menu.add(0, 3, 0, "Настройки");
        item.setIcon(R.drawable.settings);
        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        item = menu.add(0, 4, 2, "Обновить");
        item.setIcon(R.drawable.refresh);
        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case 2:
                datePickerDialog.show();
                break;
            case 3:
                Intent intent = new Intent(getContext(), SettingsActivity.class);
                startActivityForResult(intent, 0);
                break;
            case 4:
                Download2(periods[pernum].id, pernum, false, true);
        }
        return super.onOptionsItemSelected(item);
    }



    @Override
    public void onResume() {
        if(!shown && periods[pernum].days != null) show();
        if(getActivity() != null) {
            Toolbar toolbar = getActivity().findViewById(R.id.toolbar);
            if(toolbar != null)
                toolbar.setTitle("Дневник");
        }
        super.onResume();
    }

    public Context getContext() {
        return context;
    }

    private class MyFragmentPagerAdapter extends FragmentPagerAdapter {

        MyFragmentPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            log("position " + position);
            if(position == 0)
                return pageFragments.get(pageFragments.size()-1);
            return pageFragments.get(position - 1);
        }

        @Override
        public int getCount() {
            return pageCount;
        }
    }

    class Period {
        Long datestart;
        Long datefinish;
        String name;
        boolean nullsub = false;
        int num;
        int id;
        ArrayList<PeriodFragment.Subject> subjects;
        ArrayList<PeriodFragment.Day> days;
        ArrayList<PeriodFragment.Cell> cells;
        ArrayList<LinearLayout> lins;

        Period() {
        }
    }

    class Kletka extends android.support.v7.widget.AppCompatTextView {

        public Kletka(Context context) {
            super(context);
            this.setGravity(Gravity.CENTER);
            this.setTextColor(Color.WHITE);
            this.setPadding(15, 0, 15, 0);
        }
    }

    class KindaList extends ArrayList<String> {}
}