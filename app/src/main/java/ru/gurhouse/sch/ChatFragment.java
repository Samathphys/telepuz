package ru.gurhouse.sch;


import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.http.AndroidHttpClient;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.widget.Toolbar;
import android.text.Html;
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
import android.widget.Toast;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import static android.support.v4.content.ContextCompat.checkSelfPermission;
import static android.view.View.GONE;
import static java.util.Calendar.MONTH;
import static java.util.Calendar.YEAR;
import static java.util.Calendar.getInstance;
import static ru.gurhouse.sch.LoginActivity.connect;
import static ru.gurhouse.sch.LoginActivity.log;
import static ru.gurhouse.sch.LoginActivity.loge;

public class ChatFragment extends Fragment {

    private View view;
    private int PERSON_ID;
    private LayoutInflater inflater;
    private LinearLayout container;

    private Msg[] messages;
    private Handler h;
    private boolean uploading = false;
    private int last_msg = -1;
    private ArrayList<Integer> first_msgs;
    private ScrollView scroll;
    private ArrayList<File> attach = new ArrayList<>();
    private boolean scrolled = false, first_time = true;
    private MenuItem itemToEnable = null;

    Context context;
    int threadId = 0;
    String threadName = "";
    int searchMsgId = -1;
    boolean group = false;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        getActivity().findViewById(R.id.btn_file).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            startActivityForResult(intent, 43);
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        PERSON_ID = TheSingleton.getInstance().getPERSON_ID();

        this.inflater = inflater;

        first_msgs = new ArrayList<>();
        View view = inflater.inflate(R.layout.chat, container, false);

        Toolbar toolbar = getActivity().findViewById(R.id.toolbar);
        toolbar.setTitle(threadName);
        setHasOptionsMenu(true);
        // t/odo toolbar subtitle
        // toolbar.setSubtitle("subtitle");

        ((MainActivity)getActivity()).setSupActionBar(toolbar);
        // Inflate the layout for this fragment``
        ((MainActivity)getActivity()).getSupportActionBar().setHomeButtonEnabled(true);

        this.container = view.findViewById(R.id.main_container);
        this.view = view;
        return view;
    }

    File pinned = null;
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 124);
            pinned = new File(ImageFilePath.getPath(getContext(), data.getData()));
        } else {
            System.out.println("result");
            uploadFile(new File(ImageFilePath.getPath(getContext(), data.getData())));
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(grantResults[0] == PackageManager.PERMISSION_GRANTED && requestCode == 124) {
            uploadFile(pinned);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if(getContext() != null) {
            menu.clear();
            MenuItem ref = menu.add(0, 3, 0, "Refresh");
            ref.setIcon(getResources().getDrawable(R.drawable.refresh));
            ref.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
            menu.add(0, 1, 0,
                    getContext().getSharedPreferences("pref", 0).getString("muted", "[]")
                            .contains("" + threadId)?"Включить уведомления":"Отключить уведомления")
                    .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
            if(group)
                menu.add(0, 2, 1, "Покинуть беседу");
        }
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == 1 && getContext() != null) {
            SharedPreferences pref = getContext().getSharedPreferences("pref", 0);
            if(item.getTitle().equals("Отключить уведомления")) {
                try {
                    JSONArray array = new JSONArray(pref.getString("muted", "[]"));
                    array.put(threadId);
                    pref.edit().putString("muted", array.toString()).apply();
                } catch (Exception e) {loge(e.toString());}
                item.setTitle("Включить уведомления");
            } else {
                try {
                    JSONArray array = new JSONArray(pref.getString("muted", "[]")), a = new JSONArray();
                    for (int i = 0; i < array.length(); i++) {
                        if(!(array.getInt(i) == threadId)) {
                            a.put(array.getInt(i));
                        }
                    }
                    pref.edit().putString("muted", a.toString()).apply();
                    item.setTitle("Отключить уведомления");
                } catch (Exception e) {loge(e.toString());}
            }
        } else if(item.getItemId() == 2) {
            new Thread() {
                @Override
                public void run() {
                    try {
                        connect("https://app.eschool.center/ec-server/chat/leave?threadId=" + threadId, null);
                    } catch (LoginActivity.NoInternetException e) {
                        Toast.makeText(getContext(), "Нет интернета", Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {loge(e);}
                }
            }.start();
            getActivity().onBackPressed();
        } else if (item.getItemId() == 3) {
            log("refreshing chat");
            item.setEnabled(false);
            itemToEnable = item;
            new Thread() {
                @Override
                public void run() {
                    try {
                        download(h);
                    } catch (LoginActivity.NoInternetException e) {
                        getActivity().runOnUiThread(() ->
                                Toast.makeText(getContext(), "Нет доступа к интернету", Toast.LENGTH_SHORT).show());
                    } catch (Exception e) {
                        loge(e.toString());
                    }
                }
            }.start();
        }
        return super.onOptionsItemSelected(item);
    }

    void newMessage(String text, long time, int sender_id, int thread_id, String sender_fio, String attach) {
        log("new message in ChatFragment");
        log("notif thread: " + thread_id + ", this thread id: " + this.threadId);
        if(thread_id != this.threadId) {
            log("wrong thread, sorry");
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "1");
            builder.setContentTitle(sender_fio)
                    .setContentText(text)
                    .setSmallIcon(R.drawable.alternative);
            Notification notif = builder.build();
            NotificationManagerCompat manager = NotificationManagerCompat.from(context);
            manager.notify(TheSingleton.getInstance().notification_id++, notif);
            return;
        }
        final LinearLayout container = view.findViewById(R.id.main_container);
        View item;
        TextView tv, tv_attach;
        if(PERSON_ID == sender_id) {
            item = inflater.inflate(R.layout.chat_item, container, false);
        } else {
            item = inflater.inflate(R.layout.chat_item_left, container, false);
        }
        tv = item.findViewById(R.id.chat_tv_sender);
        if(!group && tv != null) {
            tv.setVisibility(GONE);
        } else if (tv != null){
            tv.setText(sender_fio);
            tv.setVisibility(View.VISIBLE);
        }
        tv = item.findViewById(R.id.tv_text);
        if(Html.fromHtml(text).toString().equals("")) {
            tv.setVisibility(GONE);
        }
        tv.setText(Html.fromHtml(text));
        JSONArray array;
        try {
            array = new JSONArray(attach);
            for (int i = 0; i < array.length(); i++) {
                final JSONObject a = array.getJSONObject(i);
                tv_attach = new TextView(getContext());
                float size = a.getInt("fileSize");
                String s = "B";
                if (size > 900) {
                    s = "KB";
                    size /= 1024;
                }
                if (size > 900) {
                    s = "MB";
                    size /= 1024;
                }
                tv_attach.setText(String.format(Locale.getDefault(), a.getString("fileName") + " (%.2f " + s + ")", size));
                tv_attach.setTextColor(getResources().getColor(R.color.two));
                tv_attach.setOnClickListener(v -> {
                    try {
                        String url = "https://app.eschool.center/ec-server/files/" + a.getInt("fileId");
                        ((MainActivity) getActivity()).saveFile(url, a.getString("fileName"), true);
                    } catch (JSONException e) {loge(e.toString());}
                });
                ((LinearLayout) item.findViewById(R.id.attach)).addView(tv_attach);
            }
        } catch (JSONException e) {
            loge(e.toString());
        }
        Date date = new Date(time);

        tv = item.findViewById(R.id.tv_time);
        tv.setText(date.getHours() + ":" + date.getMinutes());
        log("person_id: " + PERSON_ID + ", sender: " + sender_id);

        container.addView(item);
        scroll.post(() -> scroll.scrollTo(0, scroll.getChildAt(0).getBottom()));
    }

    @SuppressLint("HandlerLeak")
    @Override
    public void onViewCreated(@NonNull final View view, @Nullable Bundle savedInstanceState) {
        if(first_time) {
            scroll = view.findViewById(R.id.scroll);
            view.findViewById(R.id.btn_refresh).setOnClickListener((v)->{
                view.findViewById(R.id.tv_error).setVisibility(View.INVISIBLE);
                v.setVisibility(View.INVISIBLE);
                new Thread(() -> {
                    try {
                        download(h);
                    } catch (LoginActivity.NoInternetException e) {
                        getActivity().runOnUiThread(() -> {
                            v.setVisibility(View.VISIBLE);
                            view.findViewById(R.id.tv_error).setVisibility(View.VISIBLE);
                        });
                    } catch (Exception e) {
                        loge(e.toString());
                    }}).start();
            });

            ViewTreeObserver.OnScrollChangedListener listener = () -> {
                if (scroll != null) {
                    if (!scrolled)
                        scrolled = true;
                    else {
                        LinearLayout l = scroll.findViewById(R.id.main_container);
                        if (l.findViewWithTag("result") != null && scrolled)
                            if (l.findViewWithTag("result").getTag(R.id.TAG_POSITION).equals("left"))
                                l.findViewWithTag("result").setBackground(getResources().getDrawable(R.drawable.chat_border_left));
                            else
                                l.findViewWithTag("result").setBackground(getResources().getDrawable(R.drawable.chat_border));
                    }
                    if (scroll.getScrollY() == 0 && !uploading && last_msg != 0) {
                        log("top!!");
                        uploading = true;
                        final Handler h = new Handler() {
                            @Override
                            public void handleMessage(Message yoyyoyoy) {
                                final LinearLayout container = view.findViewById(R.id.main_container);
                                View item;
                                TextView tv, tv_attach;
                                Calendar cal = getInstance(), cal1 = getInstance();
                                int i = 0;
                                for (Msg msg : messages) {
                                    if(i != 0) {
                                        cal1.setTime(msg.time);
                                        cal.setTime(messages[i-1].time);
                                        //log("comparing day " + сal1.get(Calendar.DAY_OF_MONTH) + " and " + cal.get(Calendar.DAY_OF_MONTH));
                                        if(cal1.get(Calendar.DAY_OF_MONTH) != cal.get(Calendar.DAY_OF_MONTH)) {
                                            item = inflater.inflate(R.layout.date_divider, container, false);
                                            tv = item.findViewById(R.id.tv_date);
                                            tv.setText(getDate(cal));
                                            container.addView(item, 0);
                                        }
                                    }
                                    if(PERSON_ID == msg.user_id) {
                                        item = inflater.inflate(R.layout.chat_item, container, false);
                                    } else {
                                        item = inflater.inflate(R.layout.chat_item_left, container, false);
                                    }
                                    tv = item.findViewById(R.id.chat_tv_sender);
                                    if(!group && tv != null) {
                                        tv.setVisibility(GONE);
                                    } else if(tv != null) {
                                        tv.setText(msg.sender);
                                        tv.setVisibility(View.VISIBLE);
                                    }
                                    tv = item.findViewById(R.id.tv_text);
                                    if (Html.fromHtml(msg.text).toString().equals("")) {
                                        tv.setVisibility(GONE);
                                    }
                                    tv.setText(Html.fromHtml(msg.text));
                                    tv = item.findViewById(R.id.tv_time);

                                    tv.setText(String.format(Locale.UK, "%02d:%02d", msg.time.getHours(), msg.time.getMinutes()));
                                    if (msg.files != null) {
                                        for (final Attach a : msg.files) {
                                            tv_attach = new TextView(getContext());
                                            float size = a.size;
                                            String s = "B";
                                            if (size > 900) {
                                                s = "KB";
                                                size /= 1024;
                                            }
                                            if (size > 900) {
                                                s = "MB";
                                                size /= 1024;
                                            }
                                            tv_attach.setText(String.format(Locale.getDefault(), a.name + " (%.2f " + s + ")", size));
                                            tv_attach.setTextColor(getResources().getColor(R.color.two));
                                            tv_attach.setOnClickListener(v -> {
                                                String url = "https://app.eschool.center/ec-server/files/" + a.fileId;
                                                ((MainActivity) getActivity()).saveFile(url, a.name, true);
                                            });
                                            ((LinearLayout) item.findViewById(R.id.attach)).addView(tv_attach);
                                        }
                                    } else
                                        item.findViewById(R.id.attach).setVisibility(GONE);
                                    container.addView(item, 0);
                                    i++;
                                }
                                if(scroll != null)
                                    scroll.post(() -> {
                                        if (container.getChildCount() >= 25 && messages.length > 0)
                                            scroll.scrollTo(0, container.getChildAt(messages.length - 1).getBottom());
                                    });
                                uploading = false;
                            }
                        };
                        new Thread() {
                            @Override
                            public void run() {
                                try {
                                    JSONArray array;
                                    try {
                                        array = new JSONArray(connect("https://app.eschool.center/ec-server/chat/messages?getNew=false&" +
                                                "isSearch=false&rowStart=1&rowsCount=25&threadId=" + threadId + "&msgStart=" + last_msg, null));
                                    } catch (LoginActivity.NoInternetException e) {
                                        Toast.makeText(getContext(), "Нет интернета", Toast.LENGTH_SHORT).show();
                                        return;
                                    }
                                    if(array.length() == 0) {
                                        last_msg = 0;
                                    }
                                    messages = new Msg[array.length()];
                                    for (int i = 0; i < messages.length; i++) {
                                        messages[i] = new Msg();
                                    }
                                    JSONObject tmp, tmp1;
                                    Msg msg;
                                    for (int i = 0; i < array.length(); i++) {
                                        msg = messages[i];
                                        tmp = array.getJSONObject(i);
                                        msg.time = new Date(tmp.getLong("sendDate"));
                                        if (tmp.getInt("attachCount") <= 0) {
                                            msg.files = null;
                                        } else {
                                            msg.files = new Attach[tmp.getInt("attachCount")];
                                            log(tmp.getString("attachInfo"));
                                            for (int j = 0; j < msg.files.length; j++) {
                                                tmp1 = tmp.getJSONArray("attachInfo").getJSONObject(j);
                                                msg.files[j] = new Attach(tmp1.getInt("fileId"), tmp1.getInt("fileSize"),
                                                        tmp1.getString("fileName"), tmp1.getString("fileType"));
                                            }
                                        }
                                        msg.user_id = tmp.getInt("senderId");
                                        msg.msg_id = tmp.getInt("msgNum");
                                        msg.sender = tmp.getString("senderFio");
                                        if (i == array.length() - 1) {
                                            last_msg = tmp.getInt("msgNum");
                                        }
                                        if (!tmp.has("msg")) {
                                            loge("no msg tag: " + tmp.toString());
                                            msg.text = "";
                                            continue;
                                        }
                                        msg.text = tmp.getString("msg");
                                    }
                                    h.sendEmptyMessage(0);
                                } catch (Exception e) {
                                    loge("on scroll top: " + e.toString());
                                    e.printStackTrace();
                                }
                            }
                        }.start();
                    }
                    else if (scroll.getChildAt(0).getBottom()
                            <= (scroll.getHeight() + scroll.getScrollY()) && !uploading) {
                        if (first_msgs.size() == 0) return;
                        log("bottom");
                        uploading = true;
                        final Handler h = new Handler() {
                            @Override
                            public void handleMessage(Message yoyoy) {
                                final LinearLayout container = view.findViewById(R.id.container);
                                View item;
                                TextView tv, tv_attach;
                                Calendar cal = getInstance(), cal1 = getInstance();
                                Msg msg;
                                for (int i = messages.length - 1; i >= 0; i--) {
                                    msg = messages[i];
                                    if(i != messages.length-1) {
                                        cal.setTime(msg.time);
                                        cal1.setTime(messages[i+1].time);
                                        if(cal1.get(Calendar.DAY_OF_MONTH) != cal.get(Calendar.DAY_OF_MONTH)) {
                                            item = inflater.inflate(R.layout.date_divider, container, false);
                                            tv = item.findViewById(R.id.tv_date);
                                            tv.setText(getDate(cal));
                                            container.addView(item);
                                        }
                                    }
                                    if(PERSON_ID == msg.user_id) {
                                        item = inflater.inflate(R.layout.chat_item, container, false);
                                    } else {
                                        item = inflater.inflate(R.layout.chat_item_left, container, false);
                                    }
                                    tv = item.findViewById(R.id.chat_tv_sender);
                                    if(!group && tv != null) {
                                        tv.setVisibility(GONE);
                                    } else if(tv != null) {
                                        tv.setText(msg.sender);
                                        tv.setVisibility(View.VISIBLE);
                                    }
                                    tv = item.findViewById(R.id.tv_text);
                                    if (Html.fromHtml(msg.text).toString().equals("")) {
                                        tv.setVisibility(GONE);
                                    }
                                    tv.setText(Html.fromHtml(msg.text));
                                    tv = item.findViewById(R.id.tv_time);
                                    tv.setText(String.format(Locale.UK, "%02d:%02d", msg.time.getHours(), msg.time.getMinutes()));

                                    if (msg.files != null) {
                                        for (final Attach a : msg.files) {
                                            tv_attach = new TextView(getContext());
                                            float size = a.size;
                                            String s = "B";
                                            if (size > 900) {
                                                s = "KB";
                                                size /= 1024;
                                            }
                                            if (size > 900) {
                                                s = "MB";
                                                size /= 1024;
                                            }
                                            tv_attach.setText(String.format(Locale.getDefault(), a.name + " (%.2f " + s + ")", size));
                                            tv_attach.setTextColor(getResources().getColor(R.color.two));
                                            tv_attach.setOnClickListener(v -> {
                                                String url = "https://app.eschool.center/ec-server/files/" + a.fileId;
                                                ((MainActivity) getActivity()).saveFile(url, a.name, true);
                                            });
                                            ((LinearLayout) item.findViewById(R.id.attach)).addView(tv_attach);
                                        }
                                    } else
                                        item.findViewById(R.id.attach).setVisibility(GONE);
                                    container.addView(item);
                                }
                                // todo scroll ?
                                uploading = false;
                                first_msgs.remove(first_msgs.size() - 1);
                            }
                        };
                        new Thread() {
                            @Override
                            public void run() {
                                try {
                                    JSONArray array = new JSONArray(connect("https://app.eschool.center/ec-server/chat/messages?getNew=false&" +
                                            "isSearch=false&rowStart=0&rowsCount=25&threadId=" + threadId + "&msgStart=" + (first_msgs.get(first_msgs.size() - 1) + 1), null));

                                    messages = new Msg[array.length()];
                                    for (int i = 0; i < messages.length; i++) {
                                        messages[i] = new Msg();
                                    }
                                    JSONObject tmp, tmp1;
                                    Msg msg;
                                    for (int i = 0; i < array.length(); i++) {
                                        msg = messages[i];
                                        tmp = array.getJSONObject(i);
                                        msg.time = new Date(tmp.getLong("sendDate"));
                                        if (tmp.getInt("attachCount") <= 0) {
                                            msg.files = null;
                                        } else {
                                            msg.files = new Attach[tmp.getInt("attachCount")];
                                            for (int j = 0; j < msg.files.length; j++) {
                                                tmp1 = tmp.getJSONArray("attachInfo").getJSONObject(j);
                                                msg.files[j] = new Attach(tmp1.getInt("fileId"), tmp1.getInt("fileSize"),
                                                        tmp1.getString("fileName"), tmp1.getString("fileType"));
                                            }
                                        }
                                        msg.user_id = tmp.getInt("senderId");
                                        msg.msg_id = tmp.getInt("msgNum");
                                        msg.sender = tmp.getString("senderFio");
                                        if (!tmp.has("msg")) {
                                            loge("no msg tag: " + tmp.toString());
                                            msg.text = "";
                                            continue;
                                        }
                                        msg.text = tmp.getString("msg");
                                    }
                                    h.sendEmptyMessage(0);
                                } catch (LoginActivity.NoInternetException e) {
                                    Toast.makeText(getContext(), "Нет интернета", Toast.LENGTH_SHORT).show();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    loge("on scroll bottom: " + e.toString());
                                }
                            }
                        }.start();

                    }
                }
            };
            scroll.getViewTreeObserver().addOnScrollChangedListener(listener);
            h = new Handler() {
                @Override
                public void handleMessage(Message yoy) {
                    //final LinearLayout container = view.findViewById(R.id.main_container);
                    if(container == null)
                        container = view.findViewById(R.id.main_container);
                    container.removeAllViews();
                    View item;
                    TextView tv, tv_attach;
                    Calendar cal = Calendar.getInstance(), cal1 = Calendar.getInstance();
                    log(messages.length + "");
                    Msg msg;
                    for (int i = messages.length-1; i >= 0; i--) {
                        msg = messages[i];
                        if (msg.text.equals(""))
                            continue;
                        if(i != messages.length-1) {
                            cal.setTime(msg.time);//tg
                            cal1.setTime(messages[i+1].time);
                            if(cal1.get(Calendar.DAY_OF_MONTH) != cal.get(Calendar.DAY_OF_MONTH)) {
                                item = inflater.inflate(R.layout.date_divider, container, false);
                                tv = item.findViewById(R.id.tv_date);
                                tv.setText(getDate(cal));
                                container.addView(item);
                            }
                        }
                        if(PERSON_ID == msg.user_id) {
                            item = inflater.inflate(R.layout.chat_item, container, false);
                        } else {
                            item = inflater.inflate(R.layout.chat_item_left, container, false);
                        }
                        tv = item.findViewById(R.id.chat_tv_sender);
                        if(!group && tv != null) {
                            tv.setVisibility(GONE);
                        } else if(tv != null) {
                            tv.setText(msg.sender);
                            tv.setVisibility(View.VISIBLE);
                        }
                        tv = item.findViewById(R.id.tv_text);
                        if(Html.fromHtml(msg.text).toString().equals("")) {
                            tv.setVisibility(GONE);
                        }
                        tv.setText(Html.fromHtml(msg.text));
                        tv = item.findViewById(R.id.tv_time);
                        tv.setText(String.format(Locale.UK, "%02d:%02d", msg.time.getHours(), msg.time.getMinutes()));
//                    item.setPadding(0, 16, 4, 0);
                        if(PERSON_ID != msg.user_id) {
                            item.setTag(R.id.TAG_POSITION, "left");
                        } else
                            item.setTag(R.id.TAG_POSITION, "right");
                        if(msg.files != null) {
                            for (final Attach a: msg.files) {
                                tv_attach = new TextView(context);
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
                                tv_attach.setText(String.format(Locale.getDefault(), "%s (%.2f "+ s + ")", a.name, size));
                                tv_attach.setTextColor(getResources().getColor(R.color.two));
                                tv_attach.setOnClickListener(v -> {
                                    String url = "https://app.eschool.center/ec-server/files/" + a.fileId;
                                    ((MainActivity) getActivity()).saveFile(url, a.name, true);
                                });
                                tv_attach.setMaxWidth(view.getMeasuredWidth()-300);
                                ((LinearLayout)item.findViewById(R.id.attach)).addView(tv_attach);
                            }
                        }  else
                            item.findViewById(R.id.attach).setVisibility(GONE);
                        if(msg.msg_id == searchMsgId) {
                            log("result found");
                            item.setTag("result");
                        }
                        container.addView(item);
                    }
                    view.findViewById(R.id.scroll_container).setBackgroundColor(getResources().getColor(R.color.six));

                    if(scroll == null)
                        scroll = view.findViewById(R.id.scroll);
                    if(scroll == null)
                        scroll = ChatFragment.this.view.findViewById(R.id.scroll);
                    scroll.post(() -> {
                        if(searchMsgId == -1)
                            scroll.fullScroll(ScrollView.FOCUS_DOWN);
                        else {
                            scroll.scrollTo(0, container.findViewWithTag("result").getTop());
                            container.findViewWithTag("result").setBackground(getResources().getDrawable(R.drawable.chat_border_highlited));
                            //scrolled = true;
                        }
                    });

                    final EditText et = view.findViewById(R.id.et);
                    scroll = view.findViewById(R.id.scroll);

                    view.findViewById(R.id.btn_send).setOnClickListener(v -> {
                        final String text = et.getText().toString();
                        final ArrayList<File> files = attach;
                        //attach = null;
                        et.setText("");
                        new Thread() {
                            @Override
                            public void run() {
                                try {
                                    getActivity().runOnUiThread(() -> {
                                        View item1 = inflater.inflate(R.layout.chat_item, container, false);
                                        TextView tv1 = item1.findViewById(R.id.tv_text);
                                        if (Html.fromHtml(text).toString().equals("")) {
                                            tv1.setVisibility(GONE);
                                        }
                                        tv1.setText(Html.fromHtml(text));
                                        tv1 = item1.findViewById(R.id.tv_time);
                                        tv1.setText(String.format(Locale.UK, "%02d:%02d", new Date().getHours(), new Date().getMinutes()));
                                        container.addView(item1);
                                        scroll.post(() -> scroll.fullScroll(ScrollView.FOCUS_DOWN));
                                    });
                                    if(!files.isEmpty()) {
                                        //uploadFile(new File(files.get(0).getPath()));
                                        ChatFragment.this.sendMessage(threadId, text);
                                    } else {
                                        connect("https://app.eschool.center/ec-server/chat/sendNew",  "threadId=" + threadId + "&msgText=" + text +
                                                "&msgUID=" + System.currentTimeMillis());
                                    }
                                } catch (LoginActivity.NoInternetException e) {
                                    Toast.makeText(getContext(), "Нет интернета", Toast.LENGTH_SHORT).show();
                                    container.removeViewAt(container.getChildCount()-1);
                                } catch (Exception e) {
                                    loge("rar: " + e.toString());
                                    e.printStackTrace();
                                }
                            }
                        }.start();
                    });
                    first_time = false;
                    if(itemToEnable != null)
                        itemToEnable.setEnabled(true);
                }
            };
            new Thread() {
                @Override
                public void run() {
                    try {
                        download(h);
                    } catch (LoginActivity.NoInternetException e) {
                        getActivity().runOnUiThread(()->{
                            TextView tv = view.findViewById(R.id.tv_error);
                            tv.setText("Нет доступа к интернету");
                            tv.setVisibility(View.VISIBLE);
                            view.findViewById(R.id.btn_refresh).setVisibility(View.VISIBLE);
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                        loge("onViewCreated() run: " + e.toString());
                    }
                }
            }.start();
        }
    }

    @Override
    public void onDetach() {
        scroll = null;
        super.onDetach();
    }

    private void download(Handler h) throws IOException, JSONException, LoginActivity.NoInternetException {
        new Thread() {
            @Override
            public void run() {
                try {
                    connect("https://app.eschool.center/ec-server/chat/readAll?threadId=" + threadId, null);
                } catch (LoginActivity.NoInternetException ignore) {
                } catch (Exception e) {
                    e.printStackTrace();
                    loge(e.toString());
                }
            }
        }.start();

        boolean found = false;
        if(searchMsgId == -1)
            found = true;
        JSONObject tmp, tmp1;
        last_msg = -1;
        do {
            JSONArray array = new JSONArray(connect("https://app.eschool.center/ec-server/chat/messages?getNew=false&isSearch=false&" +
                    "rowStart=1&rowsCount=25&threadId=" + threadId + (last_msg == -1?"":"&msgStart="+last_msg), null));
            messages = new Msg[array.length()];
            for (int i = 0; i < messages.length; i++) {
                messages[i] = new Msg();
            }
            Msg msg;
            for (int i = 0; i < array.length(); i++) {
                msg = messages[i];
                tmp = array.getJSONObject(i);
                msg.time = new Date(tmp.getLong("sendDate"));
                if (tmp.getInt("attachCount") <= 0) {
                    msg.files = null;
                } else {
                    msg.files = new Attach[tmp.getInt("attachCount")];
                    for (int j = 0; j < msg.files.length; j++) {
                        tmp1 = tmp.getJSONArray("attachInfo").getJSONObject(j);
                        msg.files[j] = new Attach(tmp1.getInt("fileId"), tmp1.getInt("fileSize"),
                                tmp1.getString("fileName"), tmp1.getString("fileType"));
                    }
                }
                msg.user_id = tmp.getInt("senderId");
                msg.msg_id = tmp.getInt("msgNum");
                msg.sender = tmp.getString("senderFio");
                if (i == array.length() - 1)
                    last_msg = tmp.getInt("msgNum");
                else if (i == 0)
                    first_msgs.add(tmp.getInt("msgNum"));
                if(tmp.getInt("msgNum") == searchMsgId)
                    found = true;
                if (!tmp.has("msg")) {
                    log("no msg tag: " + tmp.toString());
                    msg.text = "";
                    continue;
                }
                msg.text = tmp.getString("msg");
            }
        } while (!found);
        if(first_msgs.size() > 0)
            first_msgs.remove(first_msgs.size()-1);
        if(h != null)
            h.sendEmptyMessage(1);
    }

    // testing, trying to send a file (not working)
    public void uploadFile(File file) {
        attach.add(file);
    }

    private void sendMessage(int threadId, String text) {
        new Thread(() -> {
            try {
                HttpPost post = new HttpPost("https://app.eschool.center/ec-server/chat/sendNew");
                HttpClient httpAsyncClient = AndroidHttpClient.newInstance("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/51.0.2704.79 Safari/537.36 Edge/14.14393", getContext());
                MultipartEntityBuilder reqEntity = MultipartEntityBuilder.create();
                reqEntity.setBoundary("----WebKitFormBoundaryfgXAnWy3pntveyQZ");
                for (File f : attach)
                    reqEntity.addBinaryBody("file", f, ContentType.create("image/jpeg"), f.getName());
                attach.clear();
                reqEntity.addTextBody("threadId", "" + threadId);
                reqEntity.addTextBody("msgUID", "" + System.currentTimeMillis());
                reqEntity.addTextBody("msgText", text, ContentType.parse("text/plain; charset=utf-8"));
                post.setHeader("Cookie", TheSingleton.getInstance().getCOOKIE() + "; site_ver=app; route=" + TheSingleton.getInstance().getROUTE() + "; _pk_id.1.81ed=de563a6425e21a4f.1553009060.16.1554146944.1554139340.");
                post.setHeader("Content-Type", "multipart/form-data; boundary=----WebKitFormBoundaryfgXAnWy3pntveyQZ ");
                post.setEntity(reqEntity.build());
                int code = httpAsyncClient.execute(post).getStatusLine().getStatusCode();
                System.out.println(code);
                log("sending file code " + code);
            } catch (UnknownHostException e) {
                Toast.makeText(getContext(), "Нет интернета", Toast.LENGTH_SHORT).show();
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            } catch (ProtocolException e) {
                e.printStackTrace();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private static final String[] months = {"января", "февраля", "марта", "апреля", "мая", "июня", "июля", "августа", "сентября",
            "октября", "ноября", "декабря"};

    private static String getDate(Calendar calendar) {
        int day = calendar.get(Calendar.DAY_OF_MONTH),
                month = calendar.get(MONTH),
                year = calendar.get(YEAR);
        Calendar current = Calendar.getInstance();
        if(current.get(YEAR) != year)
            return String.format(Locale.getDefault(), "%02d.%02d.%02d", day, month+1, year);
        if(current.get(Calendar.DAY_OF_MONTH) == day)
            return "Сегодня";
        else if(current.get(Calendar.DAY_OF_MONTH) + 1 == day)
            return "Вчера";
        return String.format(Locale.getDefault(), "%d " + months[month], day);
    }

    private class Msg {
        Date time;
        Attach[] files;
        int user_id, msg_id;
        String text, sender;
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

    public Context getContext() {return context;}
}
