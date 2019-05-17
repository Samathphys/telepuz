package com.example.sch;

import android.content.Context;
import android.content.Intent;
import android.annotation.SuppressLint;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
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
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import static java.lang.Thread.sleep;

public class ScheduleFragment extends Fragment {

    ArrayList<PeriodFragment.Subject> subjects;
    ArrayList<PeriodFragment.Day> days;
    ArrayList<PeriodFragment.Cell> cells;
    boolean ready = false;
    boolean first = true;
    private String COOKIE, ROUTE;
    private int USER_ID;
    static int pageCount = 101;
    TextView []tv = new TextView[7];
    int day;
    Date datenow;
    String[] days1 = {"пн", "вт", "ср", "чт", "пт", "сб", "вс"};
    ViewPager pager;
    PagerAdapter pagerAdapter;
    LinearLayout linear1;
    int daynum;
    int index;
    int lastposition;
    ArrayList<PageFragment> pageFragments;

    public ScheduleFragment () {
        datenow = new Date();
        Calendar c = Calendar.getInstance();
        c.setTime(datenow);
        day = c.get(Calendar.DAY_OF_WEEK);
        sasha("Day of week:" + day + "  " + datenow);
        pageFragments = new ArrayList<>();
        for (int i = 0; i < pageCount; i++) {
            pageFragments.add(new PageFragment());
        }

    }

    static void sasha(String s) {
        Log.v("sasha", s);
    }

    void start() {
        cells = new ArrayList<>();
        days = new ArrayList<>();
        subjects = new ArrayList<>();
        COOKIE = TheSingleton.getInstance().getCOOKIE();
        ROUTE = TheSingleton.getInstance().getROUTE();
        USER_ID = TheSingleton.getInstance().getUSER_ID();
        Download();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_schedule, container, false);
        if (first) {
            while (true) {
                try {
                    sleep(10);
                    if (ready)
                        break;
                } catch (InterruptedException e) {
                }

            }
            for (int i = 0; i < pageCount; i++) {
                pageFragments.get(i).subjects = subjects;
            }
            SimpleDateFormat dateFormat = new SimpleDateFormat("D", Locale.ENGLISH);
            int A = Integer.parseInt(dateFormat.format(datenow));
            pageFragments.get(pageCount / 2).dayofyear = A;
            Long daymsec = days.get(0).daymsec;
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(daymsec);
            int B = calendar.get(Calendar.DAY_OF_YEAR);
            for (int i = pageCount / 2 + 1; i < pageCount; i++) {
                pageFragments.get(i).dayofyear = pageFragments.get(i - 1).dayofyear + 1;
                pageFragments.get(pageCount - i - 1).dayofyear = pageFragments.get(pageCount - i).dayofyear - 1;
            }
            for (int i = 0; i < pageCount; i++) {
                if (pageFragments.get(i).dayofyear == B) {
                    daynum = i;
                    break;
                }
            }
            index = 0;
            for (int i = daynum; i < pageCount; i++) {
                Calendar calendar1 = Calendar.getInstance();
                calendar1.setTimeInMillis(days.get(index).daymsec);
                int C = calendar1.get(Calendar.DAY_OF_YEAR);
                if (C == pageFragments.get(i).dayofyear) {
                    pageFragments.get(i).day = days.get(index);
                    if (index + 1 == days.size()) {
                        break;
                    } else {
                        index++;
                    }
                }
            }
        }
        linear1 = v.findViewById(R.id.liner1);
        linear1.setWeightSum(1);
        pager = v.findViewById(R.id.pager);
        pagerAdapter = new MyFragmentPagerAdapter(getFragmentManager());
        pager.setAdapter(pagerAdapter);
        pager.setCurrentItem(0);
        for (int i = 0; i < 7; i++) {
            tv[i] = new TextView(getActivity().getApplicationContext());
            tv[i].setId(i);
            tv[i].setGravity(Gravity.CENTER);
            tv[i].setTextColor(Color.WHITE);
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(datenow);
            calendar.add(Calendar.DAY_OF_MONTH, i + 1 - day);
            String s = days1[i] + "\n" + calendar.get(Calendar.DAY_OF_MONTH);
            Spannable spans = new SpannableString(s);
            spans.setSpan(new RelativeSizeSpan(3f), 0, s.indexOf("\n"), Spanned.SPAN_EXCLUSIVE_INCLUSIVE);
            spans.setSpan(new ForegroundColorSpan(Color.WHITE), 0, s.indexOf("\n"), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            spans.setSpan(new ForegroundColorSpan(Color.WHITE), s.indexOf("\n"), s.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            tv[i].setText(days1[i]);
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            p.weight = (float) 1 / 7;
            tv[i].setLayoutParams(p);
            final int finalI = i;
            tv[i].setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    pager.setCurrentItem(pager.getCurrentItem() - (day - finalI - 1));
                    day = finalI + 1;
                    okras(tv);
                    tv[finalI].setBackground(getResources().getDrawable(R.drawable.cell_phone1));
                    tv[finalI].setTextColor(Color.BLACK);
                }
            });
            linear1.addView(tv[i]);
        }
        tv[day - 1].setBackground(getResources().getDrawable(R.drawable.cell_phone1));
        tv[day - 1].setTextColor(Color.BLACK);
        first = false;
        pager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {

                okras(tv);
                day -= lastposition - position;
                if (day < 1) {
                    day = 7;
                }
                if (day > 7) {
                    day = 1;
                }
                tv[day - 1].setBackground(getResources().getDrawable(R.drawable.cell_phone1));
                tv[day - 1].setTextColor(Color.BLACK);
                lastposition = pager.getCurrentItem();

                sasha(String.valueOf(position));
            }
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });

        Toolbar toolbar = getActivity().findViewById(R.id.toolbar);
        toolbar.setTitle("Schedule");
        setHasOptionsMenu(true);
        ((MainActivity)getActivity()).setSupportActionBar(toolbar);
        return v;
    }

    public void okras(TextView [] tv){
        for (int i = 0; i < 7; i++) {
            tv[i].setBackground(null);
            tv[i].setTextColor(Color.WHITE);
        }
    }

    private class MyFragmentPagerAdapter extends FragmentPagerAdapter {

        public MyFragmentPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {

            return pageFragments.get(position);
        }

        @Override
        public int getCount() {
            return pageCount;
        }

    }

    void Download() {

        new Thread() {
            @SuppressLint("SimpleDateFormat")
            @RequiresApi(api = Build.VERSION_CODES.KITKAT)
            @Override
            public void run() {
                try {
                    //------------------------------------------------------------------------------------------------
                    URL url = new URL("https://app.eschool.center/ec-server/student/getDiaryUnits?userId=" + USER_ID + "&eiId=97932");
                    HttpURLConnection con = (HttpURLConnection) url.openConnection();
                    con.setRequestMethod("GET");
                    con.setRequestProperty("Cookie", COOKIE + "; route=" + ROUTE + "; _pk_ses.1.81ed=*; site_ver=app; _pk_id.1.81ed=de563a6425e21a4f.1553009060.16.1554146944.1554139340.");
                    StringBuilder result = new StringBuilder();

                    BufferedReader rd = new BufferedReader(new InputStreamReader(con.getInputStream()));
                    String line;
                    while ((line = rd.readLine()) != null) {
                        result.append(line);
                    }
                    rd.close();

                    JSONObject object = new JSONObject(result.toString());
                    JSONArray array = object.getJSONArray("result");
                    for (int i = 0; i < array.length(); i++) {
                        subjects.add(new PeriodFragment.Subject());
                        JSONObject obj = array.getJSONObject(i);
                        if (obj.has("overMark")) {
                            double d = obj.getDouble("overMark");
                            String s = String.valueOf(d);
                            if (s.length() > 4) {
                                s = String.format(Locale.UK, "%.2f", d);
                            }
                            subjects.get(i).avg = Double.valueOf(s);
                        }
                        if (obj.has("totalMark"))
                            subjects.get(i).totalmark = obj.getString("totalMark");
                        if (obj.has("unitName"))
                            subjects.get(i).name = obj.getString("unitName");
                        if (obj.has("rating"))
                            subjects.get(i).rating = obj.getString("rating");
                        if (obj.has("unitId"))
                            subjects.get(i).unitid = obj.getInt("unitId");
                        subjects.get(i).cells = new ArrayList<>();
                    }
                    URL url1 = new URL("https://app.eschool.center/ec-server/student/getDiaryPeriod?userId=" + USER_ID + "&eiId=97932");
                    HttpURLConnection con1 = (HttpURLConnection) url1.openConnection();
                    con1.setRequestMethod("GET");
                    con1.setRequestProperty("Cookie", COOKIE + "; route=" + ROUTE + "; _pk_ses.1.81ed=*; site_ver=app; _pk_id.1.81ed=de563a6425e21a4f.1553009060.16.1554146944.1554139340.");
                    StringBuilder result1 = new StringBuilder();
                    BufferedReader rd1 = new BufferedReader(new InputStreamReader(con1.getInputStream()));
                    while ((line = rd1.readLine()) != null) {
                        result1.append(line);
                    }
                    rd1.close();
                    JSONObject object1 = new JSONObject(result1.toString());
                    JSONArray arraydaylessons = object1.getJSONArray("result");
                    for (int i = 0; i < arraydaylessons.length(); i++) {
                        object1 = arraydaylessons.getJSONObject(i);
                        PeriodFragment.Cell call = new PeriodFragment.Cell();
                        if (object1.has("lptName"))
                            call.lptname = object1.getString("lptName");
                        if (object1.has("markDate"))
                            call.markdate = object1.getString("markDate");
                        if (object1.has("lessonId"))
                            call.lessonid = object1.getLong("lessonId");
                        if (object1.has("markVal"))
                            call.markvalue = object1.getString("markVal");
                        if (object1.has("mktWt"))
                            call.mktWt = object1.getDouble("mktWt");
                        if (object1.has("teachFio"))
                            call.teachFio = object1.getString("teachFio");
                        if (object1.has("startDt"))
                            call.date = object1.getString("startDt");
                        if (object1.has("unitId"))
                            call.unitid = object1.getInt("unitId");
                        cells.add(call);
                    }
                    COOKIE = TheSingleton.getInstance().getCOOKIE();
                    ROUTE = TheSingleton.getInstance().getROUTE();
                    USER_ID = TheSingleton.getInstance().getUSER_ID();

                    String s1 = cells.get(0).date;
                    String s2 = cells.get(cells.size() - 1).date;
                    DateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ENGLISH);
                    Long d1 = format.parse(s1).getTime();
                    Long d2 = format.parse(s2).getTime();

                    URL url2 = new URL("https://app.eschool.center/ec-server/student/diary?" +
                            "userId=" + USER_ID + "&d1=" + d1 + "&d2=" + d2);
                    HttpURLConnection con2 = (HttpURLConnection) url2.openConnection();
                    con2.setRequestMethod("GET");

                    con2.setRequestProperty("Cookie", COOKIE + "; route=" + ROUTE + "; _pk_ses.1.81ed=*;" +
                            " site_ver=app; _pk_id.1.81ed=de563a6425e21a4f.1553009060." +
                            "16.1554146944.1554139340.");

                    StringBuilder result2 = new StringBuilder();

                    BufferedReader rd2 = new BufferedReader(new InputStreamReader(con2.getInputStream()));

                    while ((line = rd2.readLine()) != null) {
                        result2.append(line);
                    }
                    rd2.close();
                    JSONObject object2 = new JSONObject(result2.toString());
                    JSONArray array2 = object2.getJSONArray("lesson");

                    Long day1 = 0l;
                    Long date1;
                    int isODOD;
                    int index = -1;
                    for (int i = 0; i < array2.length(); i++) {
                        object2 = array2.getJSONObject(i);
                        date1 = Long.valueOf(String.valueOf(object2.getString("date")));
                        isODOD = object2.getInt("isODOD");
                        if (isODOD != 1) {
                            if (!date1.equals(day1) || date1 - day1 != 0) {
                                index++;
                                Date date = new Date(date1);
                                PeriodFragment.Day thisday = new PeriodFragment.Day();
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
                                days.add(thisday);
                            }
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
                                                for (int l = 0; l < ar2.length(); l++) {
                                                    //
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            days.get(index).lessons.add(lesson);
                        }
                        day1 = date1;
                    }

                    for (int i = 0; i < days.size(); i++) {
                        for (int j = 0; j < cells.size(); j++) {
                            s1 = cells.get(j).date;
                            format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ENGLISH);
                            d1 = format.parse(s1).getTime();
                            if (cells.get(j).mktWt != 0) {
                                if (days.get(i).daymsec - d1 == 0 || days.get(i).daymsec.equals(d1)) {
                                    for (int k = 0; k < days.get(i).lessons.size(); k++) {
                                        if (days.get(i).lessons.get(k).id.equals(cells.get(j).lessonid)) {
                                            PeriodFragment.Mark mark = new PeriodFragment.Mark();
                                            mark.cell = cells.get(j);
                                            mark.idlesson = cells.get(j).lessonid;
                                            mark.coefficient = cells.get(j).mktWt;
                                            if (cells.get(j).markvalue != null)
                                                mark.value = cells.get(j).markvalue;
                                            else
                                                mark.value = "";
                                            mark.teachFio = cells.get(j).teachFio;
                                            mark.date = cells.get(j).markdate;

                                            mark.topic = cells.get(j).lptname;
                                            mark.unitid = cells.get(j).unitid;
                                            for (int l = 0; l < subjects.size() - 1; l++) {
                                                if (subjects.get(l).unitid == mark.unitid) {
                                                    subjects.get(l).cells.add(cells.get(j));
                                                    if (subjects.get(l).shortname != null)
                                                        subjects.get(l).shortname = days.get(i).lessons.get(k).shortname;
                                                    if (days.get(i).lessons.get(k).shortname.equals("Обществозн."))
                                                        subjects.get(l).shortname = "Общест.";
                                                    if (days.get(i).lessons.get(k).shortname.equals("Физ. культ."))
                                                        subjects.get(l).shortname = "Физ-ра";
                                                    if (days.get(i).lessons.get(k).shortname.equals("Инф. и ИКТ"))
                                                        subjects.get(l).shortname = "Информ.";
                                                }
                                            }
                                            days.get(i).lessons.get(k).marks.add(mark);
                                        }
                                    }
                                }
                            }
                        }
                    }
                    ready = true;
                    ((MainActivity) getActivity()).set(subjects, days);
                    //---------------------------------------------------------------------------------------------------------------------------------
                } catch (Exception e) {
                    Download();
                    System.out.println("dfgdhtjfuykgujdyhstgarfEDWFEGRHTSKUFILG;OHOLIGKUJTYHRGEFdfrthJYKUILGO;HULIKVUJCYHTXEGZRdwrTHSJYDKUFLIG;UOLIKUCJYXHTDZGRSFEaGRHTJYKUILB;ONLBIHKVUGJYCFTXDGRZSFESRGTHYJKUGLIH;OLIBKUVJYCHTXDGRZSTXHYJFKUGIL///////////////////////////////////////////////////                    System.out.println(\"dfgdhtjfuykgujdyhstgarfEDWFEGRHTSKUFILG;OHOLIGKUJTYHRGEFdfrthJYKUILGO;HULIKVUJCYHTXEGZRdwrTHSJYDKUFLIG;UOLIKUCJYXHTDZGRSFEaGRHTJYKUILB;ONLBIHKVUGJYCFTXDGRZSFESRGTHYJKUGLIH;OLIBKUVJYCHTXDGRZSTXHYJFKUGIL///////////////////////////////////////////////////\");\n                    System.out.println(\"dfgdhtjfuykgujdyhstgarfEDWFEGRHTSKUFILG;OHOLIGKUJTYHRGEFdfrthJYKUILGO;HULIKVUJCYHTXEGZRdwrTHSJYDKUFLIG;UOLIKUCJYXHTDZGRSFEaGRHTJYKUILB;ONLBIHKVUGJYCFTXDGRZSFESRGTHYJKUGLIH;OLIBKUVJYCHTXDGRZSTXHYJFKUGIL///////////////////////////////////////////////////\");\n                    System.out.println(\"dfgdhtjfuykgujdyhstgarfEDWFEGRHTSKUFILG;OHOLIGKUJTYHRGEFdfrthJYKUILGO;HULIKVUJCYHTXEGZRdwrTHSJYDKUFLIG;UOLIKUCJYXHTDZGRSFEaGRHTJYKUILB;ONLBIHKVUGJYCFTXDGRZSFESRGTHYJKUGLIH;OLIBKUVJYCHTXDGRZSTXHYJFKUGIL///////////////////////////////////////////////////\");\n                    System.out.println(\"dfgdhtjfuykgujdyhstgarfEDWFEGRHTSKUFILG;OHOLIGKUJTYHRGEFdfrthJYKUILGO;HULIKVUJCYHTXEGZRdwrTHSJYDKUFLIG;UOLIKUCJYXHTDZGRSFEaGRHTJYKUILB;ONLBIHKVUGJYCFTXDGRZSFESRGTHYJKUGLIH;OLIBKUVJYCHTXDGRZSTXHYJFKUGIL///////////////////////////////////////////////////\");\n");
                }
            }
        }.start();
    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        menu.add(0, 1, 0, "Quit");
        MenuItem item = menu.add(0, 2, 0, "Settings");
        item.setIcon(R.drawable.settings);
        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == 1) {
            ((MainActivity) getActivity()).quit();
        } else if(item.getItemId() == 2) {
            startActivity(new Intent(getContext(), SettingsActivity.class));
        }
        return super.onOptionsItemSelected(item);
    }
}