package com.example.sch;


import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.NotificationCompat;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import static com.example.sch.LoginActivity.connect;
import static com.example.sch.LoginActivity.log;
import static com.example.sch.LoginActivity.loge;
import static java.util.Calendar.MONTH;
import static java.util.Calendar.YEAR;


/**
 * A simple {@link Fragment} subclass.
 */
public class ChatFragment extends Fragment {

    private View view;
    private String COOKIE, ROUTE;
    private int PERSON_ID;
    private String[] msg;
    private Date[] time;
    private int[] user_ids, msg_ids;
    private Attach[][] files;
    private Handler h;
    private boolean uploading = false;
    private int last_msg = -1;
    private ArrayList<Integer> first_msgs;
    private ScrollView scroll;
    private ArrayList<Uri> attach;
    private boolean scrolled = false;

    int threadId = 0;
    String threadName = "";
    int searchMsgId = -1;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        COOKIE = TheSingleton.getInstance().getCOOKIE();
        ROUTE = TheSingleton.getInstance().getROUTE();
        PERSON_ID = TheSingleton.getInstance().getPERSON_ID();

        first_msgs = new ArrayList<>();
        View view = inflater.inflate(R.layout.chat, container, false);

        Toolbar toolbar = getActivity().findViewById(R.id.toolbar);
        toolbar.setTitle(threadName);
        setHasOptionsMenu(true);
        // todo toolbar subtitle
        // toolbar.setSubtitle("subtitle");

        ((MainActivity)getActivity()).setSupActionBar(toolbar);
        // Inflate the layout for this fragment``
        ((MainActivity)getActivity()).getSupportActionBar().setHomeButtonEnabled(true);

        this.view = view;
        return view;
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

    void newMessage(String text, long time, int sender_id, int thread_id, String sender_fio) {
        log("new message in ChatFragment");
        log("notif thread: " + thread_id + ", this thread id: " + this.threadId);
        if(thread_id != this.threadId) {
            log("wrong thread, sorry");
            NotificationCompat.Builder builder = new NotificationCompat.Builder(getContext(), "1");
            builder.setContentTitle(sender_fio)
                    .setContentText(text)
                    .setSmallIcon(R.drawable.attach);
            Notification notif = builder.build();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                //  todo early versions
                getActivity().getSystemService(NotificationManager.class).notify(1, notif);
            }
            return;
        }
        final LinearLayout container = view.findViewById(R.id.container);
        LayoutInflater inflater = getLayoutInflater();
        View item;
        LinearLayout.LayoutParams params;
        TextView tv, tv_attach; // todo attach
        item = inflater.inflate(R.layout.chat_item, container, false);
        tv = item.findViewById(R.id.tv_text);
        if(Html.fromHtml(text).toString().equals("")) {
            ConstraintLayout.LayoutParams params1 = new ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.WRAP_CONTENT, ConstraintLayout.LayoutParams.WRAP_CONTENT);
            params1.setMargins(0,0,0,0);
            tv.setLayoutParams(params1);
        }
        tv.setText(Html.fromHtml(text));
        tv.setMovementMethod(LinkMovementMethod.getInstance());
        tv.setTextColor(Color.WHITE);
        tv.setMaxWidth(view.getMeasuredWidth()-300);
        Date date = new Date(time);

        tv = item.findViewById(R.id.tv_time);
        tv.setText(date.getHours() + ":" + date.getMinutes());
        //tv.setText(time);
        item.setPadding(0, 16, 4, 0);
        params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        log("person_id: " + PERSON_ID + ", sender: " + sender_id);
        ConstraintLayout l = item.findViewById(R.id.item_main);
        if(PERSON_ID != sender_id) {
            l.setBackground(getResources().getDrawable(R.drawable.chat_border));
            params.gravity = Gravity.END;
        } else {
            l.setBackground(getResources().getDrawable(R.drawable.chat_border_left));
            params.gravity = Gravity.START;
        }
        params.topMargin = 20;
        params.bottomMargin = 20;

        container.addView(item, params);
        scroll.scrollTo(0, scroll.getChildAt(0).getBottom());
    }

    @SuppressLint("HandlerLeak")
    @Override
    public void onViewCreated(@NonNull final View view, @Nullable Bundle savedInstanceState) {
        scroll = view.findViewById(R.id.scroll);
        ViewTreeObserver.OnScrollChangedListener listener = new ViewTreeObserver.OnScrollChangedListener() {
            @Override
            public void onScrollChanged() {
                if (scroll != null) {
                    if(!scrolled)
                        scrolled = true;
                    else {
                        LinearLayout l = scroll.findViewById(R.id.container);
                        if (l.findViewWithTag("result") != null && scrolled)
                            if (l.findViewWithTag("result").getTag(R.id.TAG_POSITION).equals("left"))
                                l.findViewWithTag("result").setBackground(getResources().getDrawable(R.drawable.chat_border_left));
                            else
                                l.findViewWithTag("result").setBackground(getResources().getDrawable(R.drawable.chat_border));
                    }
                    if (scroll.getScrollY() == 0 && !uploading) {
                        log("top!!");
                        uploading = true;
                        final Handler h = new Handler() {
                            @Override
                            public void handleMessage(Message yoyyoyoy) {
                                final LinearLayout container = view.findViewById(R.id.container);
                                LayoutInflater inflater = getLayoutInflater();
                                View item;
                                LinearLayout.LayoutParams params;
                                TextView tv, tv_attach;
                                for (int i = 0; i < /*(first_time ? msg : )*/msg.length; i++) {
                                    item = inflater.inflate(R.layout.chat_item, container, false);
                                    tv = item.findViewById(R.id.tv_text);
                                    if(Html.fromHtml(msg[i]).toString().equals("")) {
                                        ConstraintLayout.LayoutParams params1 = new ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.WRAP_CONTENT, ConstraintLayout.LayoutParams.WRAP_CONTENT);
                                        params1.setMargins(0,0,0,0);
                                        tv.setLayoutParams(params1);
                                    }
                                    tv.setText(Html.fromHtml(msg[i]));
                                    tv.setMovementMethod(LinkMovementMethod.getInstance());
                                    tv.setTextColor(Color.WHITE);
                                    tv.setMaxWidth(view.getMeasuredWidth()-300);
                                    tv = item.findViewById(R.id.tv_time);

                                    tv.setText(String.format(Locale.UK, "%02d:%02d", time[i].getHours(), time[i].getMinutes()));
                                    item.setPadding(0, 16, 4, 0);
                                    params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                                    params.gravity = Gravity.END;
                                    if(PERSON_ID != user_ids[i]) {
                                        ConstraintLayout l = item.findViewById(R.id.item_main);
                                        l.setBackground(getResources().getDrawable(R.drawable.chat_border_left));
                                        params.gravity = Gravity.START;
                                    }
                                    params.topMargin = 20;
                                    params.bottomMargin = 20;
                                    if(files[i] != null) {
                                        for (final Attach a: files[i]) {
                                            tv_attach = new TextView(getContext());
                                            float size = a.size;
                                            String s = "B";
                                            if  (size > 900) {
                                                s = "KB";
                                                size /= 1024;
                                            }
                                            if (size > 900) {
                                                s = "MB";
                                                size /= 1024;
                                            }
                                            tv_attach.setText(String.format(Locale.getDefault(), a.name + " (%.2f "+ s + ")", size ));
                                            tv_attach.setTextColor(getResources().getColor(R.color.two));
                                            tv_attach.setOnClickListener(new View.OnClickListener() {
                                                @Override
                                                public void onClick(View v) {
                                                    String url = "https://app.eschool.center/ec-server/files/" + a.fileId;
                                                    DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
                                                    request.setDescription("Some description");
                                                    request.setTitle(a.name);
                                                    request.addRequestHeader("Cookie", COOKIE + "; site_ver=app; route=" + ROUTE + "; _pk_id.1.81ed=de563a6425e21a4f.1553009060.16.1554146944.1554139340.");
                                                    request.allowScanningByMediaScanner();
                                                    request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                                                    request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, a.name);

                                                    // get download service and enqueue file
                                                    DownloadManager manager = (DownloadManager) getActivity().getSystemService(Context.DOWNLOAD_SERVICE);
                                                    manager.enqueue(request);
                                                }
                                            });
                                            tv_attach.setMaxWidth(view.getMeasuredWidth()-300);
                                            ((LinearLayout)item.findViewById(R.id.attach)).addView(tv_attach);
                                        }
                                    }
                                    container.addView(item, 0, params);
                                }
                                scroll.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        if(container.getChildCount() >= 25 && msg.length > 0)
                                            scroll.scrollTo(0, container.getChildAt(msg.length-1).getBottom());
                                    }
                                });
                                uploading = false;
                            }
                        };
                        new Thread() {
                            @Override
                            public void run() {
                                try {
                                    JSONArray array = new JSONArray(connect("https://app.eschool.center/ec-server/chat/messages?getNew=false&" +
                                            "isSearch=false&rowStart=1&rowsCount=25&threadId=" + threadId + "&msgStart=" + last_msg, null));
                                    msg = new String[array.length()];
                                    time = new Date[array.length()];
                                    user_ids = new int[array.length()];
                                    files = new Attach[array.length()][];
                                    msg_ids = new int[array.length()];
                                    JSONObject tmp, tmp1;
                                    for (int i = 0; i < array.length(); i++) {
                                        tmp = array.getJSONObject(i);
                                        time[i] = new Date(tmp.getLong("sendDate"));
                                        if(tmp.getInt("attachCount") <= 0) {
                                            files[i] = null;
                                        } else {
                                            files[i] = new Attach[tmp.getInt("attachCount")];
                                            for (int j = 0; j < files[i].length; j++) {
                                                tmp1 = tmp.getJSONArray("attachInfo").getJSONObject(j);
                                                files[i][j] = new Attach(tmp1.getInt("fileId"), tmp1.getInt("fileSize"),
                                                        tmp1.getString("fileName"), tmp1.getString("fileType"));
                                            }
                                        }
                                        user_ids[i] = tmp.getInt("senderId");
                                        msg_ids[i] = tmp.getInt("msgNum");
                                        if(i == array.length() - 1) {
                                            last_msg = tmp.getInt("msgNum");
                                        }
                                        if(!tmp.has("msg")) {
                                            // todo
                                            loge(tmp.toString());
                                            msg[i] = "";
                                            continue;
                                        }
                                        msg[i] = tmp.getString("msg");
                                    }
                                    h.sendEmptyMessage(0);
                                } catch (Exception e) {
                                    loge("on scroll top: " + e.toString());
                                }
                            }
                        }.start();
                    } else if(scroll.getChildAt(0).getBottom()
                            <= (scroll.getHeight() + scroll.getScrollY()) && !uploading) {
                        log("bottom");
                        if(first_msgs.size() == 0) return;
                        uploading = true;
                        final Handler h = new Handler() {
                            @Override
                            public void handleMessage(Message yoyoy) {
                                final LinearLayout container = view.findViewById(R.id.container);
                                LayoutInflater inflater = getLayoutInflater();
                                View item;
                                LinearLayout.LayoutParams params;
                                TextView tv, tv_attach;
                                for (int i = msg.length-1; i >= 0; i--) {
                                    item = inflater.inflate(R.layout.chat_item, container, false);
                                    tv = item.findViewById(R.id.tv_text);
                                    if(Html.fromHtml(msg[i]).toString().equals("")) {
                                        ConstraintLayout.LayoutParams params1 = new ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.WRAP_CONTENT, ConstraintLayout.LayoutParams.WRAP_CONTENT);
                                        params1.setMargins(0,0,0,0);
                                        tv.setLayoutParams(params1);
                                    }
                                    tv.setText(Html.fromHtml(msg[i]));
                                    tv.setMovementMethod(LinkMovementMethod.getInstance());
                                    tv.setTextColor(Color.WHITE);
                                    tv.setMaxWidth(view.getMeasuredWidth()-300);
                                    tv = item.findViewById(R.id.tv_time);
                                    tv.setText(String.format(Locale.UK, "%02d:%02d", time[i].getHours(), time[i].getMinutes()));
//todo
                                    item.setPadding(0, 16, 4, 0);
                                    params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                                    params.gravity = Gravity.END;
                                    if(PERSON_ID != user_ids[i]) {
                                        ConstraintLayout l = item.findViewById(R.id.item_main);
                                        l.setBackground(getResources().getDrawable(R.drawable.chat_border_left));
                                        params.gravity = Gravity.START;
                                    }
                                    params.topMargin = 20;
                                    params.bottomMargin = 20;
                                    if(files[i] != null) {
                                        for (final Attach a: files[i]) {
                                            tv_attach = new TextView(getContext());
                                            float size = a.size;
                                            String s = "B";
                                            if  (size > 900) {
                                                s = "KB";
                                                size /= 1024;
                                            }
                                            if (size > 900) {
                                                s = "MB";
                                                size /= 1024;
                                            }
                                            tv_attach.setText(String.format(Locale.getDefault(), a.name + " (%.2f "+ s + ")", size ));
                                            tv_attach.setTextColor(getResources().getColor(R.color.two));
                                            tv_attach.setOnClickListener(new View.OnClickListener() {
                                                @Override
                                                public void onClick(View v) {
                                                    String url = "https://app.eschool.center/ec-server/files/" + a.fileId;
                                                    DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
                                                    request.setDescription("Some description");
                                                    request.setTitle(a.name);
                                                    request.addRequestHeader("Cookie", COOKIE + "; site_ver=app; route=" + ROUTE + "; _pk_id.1.81ed=de563a6425e21a4f.1553009060.16.1554146944.1554139340.");
                                                    request.allowScanningByMediaScanner();
                                                    request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                                                    request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, a.name);

                                                    // get download service and enqueue file
                                                    DownloadManager manager = (DownloadManager) getActivity().getSystemService(Context.DOWNLOAD_SERVICE);
                                                    manager.enqueue(request);
                                                }
                                            });
                                            tv_attach.setMaxWidth(view.getMeasuredWidth()-300);
                                            ((LinearLayout)item.findViewById(R.id.attach)).addView(tv_attach);
                                        }
                                    }
                                    container.addView(item, params);
                                }
                                scroll.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        //if(container.getChildCount() >= 25 && msg.length > 0)
                                        //scroll.scrollTo(0, container.getChildAt(msg.length-1).getBottom());
                                    }
                                });
                                uploading = false;
                                first_msgs.remove(first_msgs.size()-1);
                            }
                        };
                        new Thread() {
                            @Override
                            public void run() {
                                try {
                                    JSONArray array = new JSONArray(connect("https://app.eschool.center/ec-server/chat/messages?getNew=false&" +
                                            "isSearch=false&rowStart=0&rowsCount=25&threadId=" + threadId + "&msgStart=" + (first_msgs.get(first_msgs.size()-1)+1), null));

                                    msg = new String[array.length()];
                                    time = new Date[array.length()];
                                    user_ids = new int[array.length()];
                                    files = new Attach[array.length()][];
                                    msg_ids = new int[array.length()];
                                    JSONObject tmp, tmp1;
                                    for (int i = 0; i < array.length(); i++) {
                                        tmp = array.getJSONObject(i);
                                        time[i] = new Date(tmp.getLong("sendDate"));
                                        if(tmp.getInt("attachCount") <= 0) {
                                            files[i] = null;
                                        } else {
                                            files[i] = new Attach[tmp.getInt("attachCount")];
                                            for (int j = 0; j < files[i].length; j++) {
                                                tmp1 = tmp.getJSONArray("attachInfo").getJSONObject(j);
                                                files[i][j] = new Attach(tmp1.getInt("fileId"), tmp1.getInt("fileSize"),
                                                        tmp1.getString("fileName"), tmp1.getString("fileType"));
                                            }
                                        }
                                        user_ids[i] = tmp.getInt("senderId");
                                        msg_ids[i] = tmp.getInt("msgNum");
                                        if(!tmp.has("msg")) {
                                            // todo
                                            loge(tmp.toString());
                                            msg[i] = "";
                                            continue;
                                        }
                                        msg[i] = tmp.getString("msg");
                                    }
                                    h.sendEmptyMessage(0);
                                } catch (Exception e) {
                                    loge("on scroll bottom: " + e.toString());
                                }
                            }
                        }.start();

                    }
                }
            }
        };
        scroll.getViewTreeObserver().addOnScrollChangedListener(listener);
        h = new Handler() {
            @Override
            public void handleMessage(Message yoy) {
                final LinearLayout container = view.findViewById(R.id.container);
                LayoutInflater inflater = getLayoutInflater();
                View item;
                LinearLayout.LayoutParams params;
                TextView tv, tv_attach;
                Calendar cal = Calendar.getInstance(), cal1 = Calendar.getInstance();
                for (int i = msg.length-1; i >= 0; i--) {
                    if(i != msg.length-1) {
                        cal.setTime(time[i]);
                        cal1.setTime(time[i+1]);
                        if(cal1.get(Calendar.DAY_OF_MONTH) != cal.get(Calendar.DAY_OF_MONTH)) {
                            item = inflater.inflate(R.layout.date_divider, container, false);
                            tv = item.findViewById(R.id.tv_date);
                            tv.setText(getDate(cal));
                            container.addView(item);
                        }
                    }
                    item = inflater.inflate(R.layout.chat_item, container, false);
                    tv = item.findViewById(R.id.tv_text);
                    if(Html.fromHtml(msg[i]).toString().equals("")) {
                        ConstraintLayout.LayoutParams params1 = new ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.WRAP_CONTENT, ConstraintLayout.LayoutParams.WRAP_CONTENT);
                        params1.setMargins(0,0,0,0);
                        tv.setLayoutParams(params1);
                    }
                    tv.setText(Html.fromHtml(msg[i]));
                    tv.setMovementMethod(LinkMovementMethod.getInstance());
                    tv.setTextColor(Color.WHITE);
                    tv.setMaxWidth(view.getMeasuredWidth()-300);
                    tv = item.findViewById(R.id.tv_time);
                    tv.setText(String.format(Locale.UK, "%02d:%02d", time[i].getHours(), time[i].getMinutes()));
                    item.setPadding(0, 16, 4, 0);
                    params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    params.gravity = Gravity.END;
                    if(PERSON_ID != user_ids[i]) {
                        ConstraintLayout l = item.findViewById(R.id.item_main);
                        l.setBackground(getResources().getDrawable(R.drawable.chat_border_left));
                        params.gravity = Gravity.START;
                        item.setTag(R.id.TAG_POSITION, "left");
                    } else
                        item.setTag(R.id.TAG_POSITION, "right");
                    params.topMargin = 20;
                    params.bottomMargin = 20;
                    if(files[i] != null) {
                        for (final Attach a: files[i]) {
                            tv_attach = new TextView(getContext());
                            float size = a.size;
                            String s = "B";
                            if  (size > 900) {
                                s = "KB";
                                size /= 1024;
                            }
                            if (size > 900) {
                                s = "MB";
                                size /= 1024;
                            }
                            tv_attach.setText(String.format(Locale.getDefault(), a.name + " (%.2f "+ s + ")", size ));
                            tv_attach.setTextColor(getResources().getColor(R.color.two));
                            tv_attach.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    String url = "https://app.eschool.center/ec-server/files/" + a.fileId;
                                    DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
                                    request.setDescription("Some description");
                                    request.setTitle(a.name);
                                    request.addRequestHeader("Cookie", COOKIE + "; site_ver=app; route=" + ROUTE + "; _pk_id.1.81ed=de563a6425e21a4f.1553009060.16.1554146944.1554139340.");
                                    request.allowScanningByMediaScanner();
                                    request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                                    request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, a.name);

                                    // get download service and enqueue file
                                    DownloadManager manager = (DownloadManager) getActivity().getSystemService(Context.DOWNLOAD_SERVICE);
                                    manager.enqueue(request);
                                }
                            });
                            tv_attach.setMaxWidth(view.getMeasuredWidth()-300);
                            ((LinearLayout)item.findViewById(R.id.attach)).addView(tv_attach);
                        }
                    }
                    if(msg_ids[i] == searchMsgId) {
                        log("result found");
                        item.setTag("result");
                    }
                    container.addView(item, params);
                }
                view.findViewById(R.id.scroll_container).setBackgroundColor(getResources().getColor(R.color.six));

                scroll.post(new Runnable() {
                    @Override
                    public void run() {
                        if(searchMsgId == -1)
                            scroll.fullScroll(ScrollView.FOCUS_DOWN);
                        else {
                            scroll.scrollTo(0, container.findViewWithTag("result").getTop());
                            container.findViewWithTag("result").setBackground(getResources().getDrawable(R.drawable.chat_border_highlited));
                            //scrolled = true;
                        }
                    }
                });

                final EditText et = view.findViewById(R.id.et);
                final Handler hand = new Handler() {
                    @Override
                    public void handleMessage(Message msg) {
                        String text = (String) msg.obj;
                        View item = getLayoutInflater().inflate(R.layout.chat_item, container, false);
                        TextView tv = item.findViewById(R.id.tv_text);
                        if(Html.fromHtml(text).toString().equals("")) {
                            ConstraintLayout.LayoutParams params1 = new ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.WRAP_CONTENT, ConstraintLayout.LayoutParams.WRAP_CONTENT);
                            params1.setMargins(0,0,0,0);
                            tv.setLayoutParams(params1);
                        }
                        tv.setText(Html.fromHtml(text));
                        tv.setTextColor(Color.WHITE);
                        tv.setMaxWidth(view.getMeasuredWidth()-300);
                        tv = item.findViewById(R.id.tv_time);
                        tv.setText(String.format(Locale.UK, "%02d:%02d", new Date().getHours(), new Date().getMinutes()));
                        item.setPadding(0, 16, 4, 0);
                        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                        params.gravity = Gravity.END;
                        params.topMargin = 20;
                        params.bottomMargin = 20;
                        container.addView(item, params);
                        scroll.post(new Runnable() {
                            @Override
                            public void run() {
                                scroll.fullScroll(ScrollView.FOCUS_DOWN);
                            }
                        });
                    }
                };
                scroll = view.findViewById(R.id.scroll);

                view.findViewById(R.id.btn_send).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        final String text = et.getText().toString();
                        final ArrayList<Uri> files = attach;
                        attach = null;
                        et.setText("");
                        new Thread() {
                            @Override
                            public void run() {
                                try {
                                    hand.sendMessage(hand.obtainMessage(0, text));
                                    if(files != null) {
                                        // todo sendFile(files.get(0), threadId, text);
                                    } else {
                                        HttpURLConnection con = (HttpURLConnection) new URL("https://app.eschool.center/ec-server/chat/sendNew").openConnection();
                                        con.setRequestMethod("POST");
                                        con.setRequestProperty("Cookie", COOKIE + "; site_ver=app; route=" + ROUTE + "; _pk_id.1.81ed=de563a6425e21a4f.1553009060.16.1554146944.1554139340.");// "; _pk_id.1.81ed=de563a6425e21a4f.1553009060.13.1554062260.1554051192.");
                                        con.setDoOutput(true);
                                        String msg = "threadId=" + threadId + "&msgText=" + text +
                                                "&msgUID=" + System.currentTimeMillis();
                                        con.getOutputStream().write(msg.getBytes());
                                        log(msg);
                                        log(con.getResponseMessage());
                                    }
                                    //con.connect();
                                } catch (Exception e) {
                                    loge(e.toString());
                                }
                            }
                        }.start();
                    }
                });
                view.findViewById(R.id.btn_file).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);

                        // Filter to only show results that can be "opened", such as a
                        // file (as opposed to a list of contacts or timezones)
                        intent.addCategory(Intent.CATEGORY_OPENABLE);

                        // Filter to show only images, using the image MIME data type.
                        // If one wanted to search for ogg vorbis files, the type would be "audio/ogg".
                        // To search for all documents available via installed storage providers,
                        // it would be "*/*".
                        intent.setType("*/*");

                        startActivityForResult(intent, 42);
                    }
                });
            }
        };
        new Thread() {
            @Override
            public void run() {
                try {
                    boolean found = false;
                    if(searchMsgId == -1)
                        found = true;
                    JSONObject tmp, tmp1;
                    do {
                        JSONArray array = new JSONArray(connect("https://app.eschool.center/ec-server/chat/messages?getNew=false&isSearch=false&" +
                                "rowStart=1&rowsCount=25&threadId=" + threadId + (last_msg == -1?"":"&msgStart="+last_msg), null));
                        msg = new String[array.length()];
                        time = new Date[array.length()];
                        user_ids = new int[array.length()];
                        files = new Attach[array.length()][];
                        msg_ids = new int[array.length()];
                        for (int i = 0; i < array.length(); i++) {
                            tmp = array.getJSONObject(i);
                            time[i] = new Date(tmp.getLong("sendDate"));
                            if (tmp.getInt("attachCount") <= 0) {
                                files[i] = null;
                            } else {
                                files[i] = new Attach[tmp.getInt("attachCount")];
                                for (int j = 0; j < files[i].length; j++) {
                                    tmp1 = tmp.getJSONArray("attachInfo").getJSONObject(j);
                                    files[i][j] = new Attach(tmp1.getInt("fileId"), tmp1.getInt("fileSize"),
                                            tmp1.getString("fileName"), tmp1.getString("fileType"));
                                }
                            }
                            user_ids[i] = tmp.getInt("senderId");
                            msg_ids[i] = tmp.getInt("msgNum");
                            if (i == array.length() - 1)
                                last_msg = tmp.getInt("msgNum");
                            else if (i == 0)
                                first_msgs.add(tmp.getInt("msgNum"));
                            if(tmp.getInt("msgNum") == searchMsgId)
                                found = true;
                            if (!tmp.has("msg")) {
                                // todo
                                loge(tmp.toString());
                                msg[i] = "";
                                continue;
                            }
                            msg[i] = tmp.getString("msg");
                        }
                    } while (!found);
                    if(first_msgs.size() > 0)
                        first_msgs.remove(first_msgs.size()-1);
                    h.sendEmptyMessage(1);
                } catch (Exception e) {loge("onViewCreated() run: " + e.toString());}
            }
        }.start();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        log("result");

        if(attach == null)
            attach = new ArrayList<>();
        attach.add(data.getData());
    }

    @Override
    public void onDetach() {
        scroll = null;
        super.onDetach();
    }

    /*private static void sendFile(Uri uri, int threadId, String text){
        File file = new File(uri.getPath());
        log("sending file...");
        try {
            HttpClient client = new DefaultHttpClient();
            HttpPost post = new HttpPost("https://app.eschool.center/ec-server/chat/sendNew");
            MultipartEntity entity = new MultipartEntity();
            entity.addPart("threadId", new StringBody(threadId + ""));
            entity.addPart("msgText", new StringBody(text));
            entity.addPart("msgUID", new StringBody(System.currentTimeMillis() + ""));
            entity.addPart("file", new FileBody(file));
            post.setEntity(entity);
            HttpResponse response = client.execute(post);

            String body = response.getEntity().getContent().toString();
//                String body = IOUtils.toString(response.getEntity().getContent());
            log("response to file: " +  body);


        } catch (Exception e) {
            loge(e.toString());
        }
        log("files sent");
    }*/

    private static final String[] months = {"января", "февраля", "марта", "апреля", "мая", "июня", "июля", "августа", "сентября",
        "октября", "ноября", "декабря"};

    private static String getDate(Calendar calendar) {
        int day = calendar.get(Calendar.DAY_OF_MONTH),
                month = calendar.get(MONTH) + 1,
                year = calendar.get(YEAR);
        Calendar current = Calendar.getInstance();
        if(current.get(YEAR) != year)
            return String.format(Locale.getDefault(), "%02d.%02d.%02d", day, month, year);
        if(current.get(Calendar.DAY_OF_MONTH) == day)
            return "Сегодня";
        else if(current.get(Calendar.DAY_OF_MONTH) + 1 == day)
            return "Вчера";
        return String.format(Locale.getDefault(), "%d " + months[month-1], day);
    }

    private class Attach {
        int fileId, size;
        String name, type;

        Attach(int fileId, int size, String name, String type) {
            this.fileId = fileId;
            this.size = size;
            this.name = name;
            this.type = type;
        }
    }
}
