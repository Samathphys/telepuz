package ru.gurhouse.sch;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.design.internal.BottomNavigationItemView;
import android.support.design.internal.BottomNavigationMenuView;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.BackgroundColorSpan;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import static ru.gurhouse.sch.LoginActivity.connect;
import static ru.gurhouse.sch.LoginActivity.log;
import static ru.gurhouse.sch.LoginActivity.loge;

public class MessagesFragment extends Fragment {

    private int PERSON_ID;
    private String[] senders, topics;
    private int[] threadIds, newCounts;
    private int[] users = null;
    private ArrayList<String> f_senders, f_topics, s_senders = null, s_messages, s_time;
    private ArrayList<Integer> f_users = null, f_threadIds, f_newCounts, s_threadIds, s_msgIds;
    private ArrayList<Boolean> s_group;
    private String s_query = "";
    private int count = 25, s_count = 0;
    private boolean first_time = true;
    private LinearLayout container;
    private ViewTreeObserver.OnScrollChangedListener scrollListener;
    private MenuItem searchView = null;

    boolean fromNotification = false;
    int notifThreadId, notifCount;

    private View savedView = null, view;
    private int search_mode = -1;
    private Person[] olist;
    private Context context;
    private boolean READY = false, shown = false;
    private View[] fitems;
    private SwipeRefreshLayout refreshL;
    private boolean refreshing = false;

    public MessagesFragment() {}

    public void start(final Handler h) {
        PERSON_ID = TheSingleton.getInstance().getPERSON_ID();

        new Thread() {
            @Override
            public void run() {
                try {
                    download(null);
                    int count = 0;
                    for (int i = 0; i < f_newCounts.size(); i++) {
                        count+=f_newCounts.get(i);
                    }
                    h.sendMessage(h.obtainMessage(123, count, count));
                    JSONArray array = new JSONArray(connect("https://app.eschool.center/ec-server/usr/olist", null, context));
                    JSONObject obj;
                    String fio, info = "";
                    olist = new Person[array.length()];
                    for (int i = 0; i < array.length(); i++) {
                        obj = array.getJSONObject(i);
                        olist[i] = new Person();
                        if(obj.has("isExternal")) {
                            if(obj.getBoolean("isExternal")) {
                                olist[i].words = new String[0];
                                continue;
                            }
                        }
                        fio = obj.getString("fio");
                        if(fio == null) {
                            olist[i].words = new String[0];
                            continue;
                        }
                        olist[i].prsId = obj.getInt("prsId");
                        olist[i].fio = fio;
                        if(obj.has("isStudent"))
                            info = "Ученик ";
                        else if (obj.has("isParent"))
                            info = "Родитель ";
                        else if (obj.has("isEmployee")) {
                            if (obj.getBoolean("isEmployee"))
                                info = "Учитель";
                        } else {
                            info = "";
                        }
                        if(obj.has("groupName")) {
                            info += "(" + obj.getString("groupName") + ")";
                            olist[i].words = new String[fio.split(" ").length + 1];
                            for (int j = 0; j < fio.split(" ").length; j++) {
                                olist[i].words[j] = fio.split(" ")[j];
                            }
                            olist[i].words[fio.split(" ").length] = obj.getString("groupName");
                        } else
                            olist[i].words = fio.split(" ");
                        olist[i].info = info;
                    }
                    log("olist l: " + olist.length);
                    READY = true;
                    h.sendEmptyMessage(431412574);
                } catch (Exception e) {
                    loge(e.toString());
                }
            }
        }.start();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup contain,
                             Bundle savedInstanceState) {
        if(getActivity() != null)
            context = getActivity();
        if(savedView != null)
            return savedView;
        view = inflater.inflate(R.layout.messages, contain, false);
        container = view.findViewById(R.id.container);
        //((MainActivity)getActivity()).getSupportActionBar().setHomeButtonEnabled(true);
        return view;
    }

    @Override
    public void onCreateOptionsMenu(final Menu menu, MenuInflater inflate) {
        inflate.inflate(R.menu.toolbar_nav, menu);
        final MenuItem myActionMenuItem = this.searchView = menu.findItem(R.id.action_search);
        final SearchView searchView = (SearchView) myActionMenuItem.getActionView();
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            String query;
            ArrayList<Person> result;
            String error;
            @SuppressLint("HandlerLeak")
            final Handler h = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    container.removeAllViews();

                    if(msg.what == 123) {
                        getView().findViewById(R.id.loading_bar).setVisibility(View.INVISIBLE);
                        getView().findViewById(R.id.scroll).setVisibility(View.VISIBLE);
                        TextView tv = getView().findViewById(R.id.tv_error);
                        tv.setText(error);
                        tv.setVisibility(View.VISIBLE);
                        return;
                    }

                    LayoutInflater inflater = getLayoutInflater();
                    View item;
                    TextView tv;
                    ImageView img;
                    int index;
                    Spannable spannable;
                    Spanned mess;
                    String s;
                    if(search_mode == 1) {
                        log("result: " + result.size());
                        s_count = -1;
                        count = -1;
                        Person person;
                        for (int i = 0; i < (result.size() > 100? 100: result.size()); i++) {
                            person = result.get(i);
                            item = inflater.inflate(R.layout.person_item, container, false);
                            tv = item.findViewById(R.id.tv_fio);
                            index = person.fio.toLowerCase().indexOf(query.toLowerCase());

                            int start, end;
                            if(index != -1) {
                                if (index > 30)
                                    start = index - 30;
                                else
                                    start = 0;
                                if (person.fio.length() > index + query.length() + 30)
                                    end = index + query.length() + 30;
                                else
                                    end = person.fio.length() - 1;
                                s = (start == 0 ? "" : "...") + person.fio.subSequence(start, end + 1).toString() + (end == person.fio.length() - 1 ? "" : "...");
                                spannable = new SpannableString(s);
                                spannable.setSpan(new BackgroundColorSpan(getResources().getColor(R.color.colorPrimaryDark)), s.toLowerCase().indexOf(query.toLowerCase()), s.toLowerCase().indexOf(query.toLowerCase()) + query.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                                tv.setText(spannable);
                                tv = item.findViewById(R.id.tv_info);
                                tv.setText(person.info);
                            } else {
                                tv.setText(person.fio);
                                index = person.info.toLowerCase().indexOf(query.toLowerCase());
                                if (index > 30)
                                    start = index - 30;
                                else
                                    start = 0;
                                if (person.info.length() > index + query.length() + 30)
                                    end = index + query.length() + 30;
                                else
                                    end = person.info.length() - 1;
                                s = (start == 0 ? "" : "...") + person.info.subSequence(start, end + 1).toString() + (end == person.info.length() - 1 ? "" : "...");
                                spannable = new SpannableString(s);
                                spannable.setSpan(new BackgroundColorSpan(getResources().getColor(R.color.colorPrimaryDark)), s.toLowerCase().indexOf(query.toLowerCase()), s.toLowerCase().indexOf(query.toLowerCase()) + query.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                                tv = item.findViewById(R.id.tv_info);
                                tv.setText(spannable);
                            }
                            //tv.setText(person.fio);
                            final int prsId = person.prsId;
                            final String fio = person.fio;
                            item.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                     new Thread() {
                                         @Override
                                         public void run() {
                                             try {
                                                 final JSONObject threads = new JSONObject(connect("https://app.eschool.center/ec-server/chat/privateThreads", null, context));
                                                 if(threads.has(prsId + "")) {
                                                     log("has");
                                                     getActivity().runOnUiThread(new Runnable() {
                                                         @Override
                                                         public void run() {
                                                             try {
                                                                 loadChat(threads.getInt(prsId + ""), fio, -1, false);
                                                             } catch (JSONException e) {
                                                                 loge(e.toString());}
                                                         }
                                                     });
                                                 } else {
                                                     log("hasn't prsId " + prsId);
                                                     final int threadId = Integer.parseInt(connect("https://app.eschool.center/ec-server/chat/saveThread",
                                                             "{\"threadId\":null,\"senderId\":null,\"imageId\":null,\"subject\":null,\"isAllowReplay\":2,\"isGroup\":false,\"interlocutor\":" + prsId + "}",
                                                             context, true));
                                                     getActivity().runOnUiThread(() -> loadChat(threadId, fio, -1, false));
                                                 }
                                             } catch (Exception e) {
                                                 loge(e.toString());}
                                         }
                                     }.start();
                                }
                            });

                            container.addView(item, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                            container.addView(inflater.inflate(R.layout.divider, container, false));
                        }
                        getView().findViewById(R.id.loading_bar).setVisibility(View.INVISIBLE);
                        getView().findViewById(R.id.scroll).setVisibility(View.VISIBLE);
                        return;
                    }
                    for (int i = 0; i < s_senders.size(); i++) {
                        item = inflater.inflate(R.layout.thread_item, container, false);
                        tv = item.findViewById(R.id.tv_sender);
                        tv.setText(s_senders.get(i));
                        tv = item.findViewById(R.id.tv_topic);
                        mess = Html.fromHtml(s_messages.get(i));

                        index = mess.toString().toLowerCase().indexOf(query.toLowerCase());
                        log("index: " + index);
                        int start, end;
                        if(index > 30)
                            start = index-30;
                        else
                            start = 0;
                        if(mess.toString().length() > index + query.length() + 30)
                            end = index + query.length() + 30;
                        else
                            end = mess.toString().length()-1;
                        s = (start == 0?"":"...") + mess.subSequence(start, end+1).toString() + (end == mess.toString().length()-1?"":"...");
                        spannable = new SpannableString(s);
                        spannable.setSpan(new BackgroundColorSpan(getResources().getColor(R.color.colorPrimaryDark)), s.toLowerCase().indexOf(query.toLowerCase()), s.toLowerCase().indexOf(query.toLowerCase()) + query.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                        tv.setText(spannable);

                        tv = item.findViewById(R.id.tv_users);
                        tv.setText("");
                        tv = item.findViewById(R.id.tv_time);
                        tv.setText(s_time.get(i));
                        img = item.findViewById(R.id.img);
                        img.setVisibility(View.GONE);
                        final int j = i;
                        item.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                loadChat(s_threadIds.get(j), s_senders.get(j), s_msgIds.get(j), s_group.get(j));
                            }
                        });
                        container.addView(item, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                        container.addView(inflater.inflate(R.layout.divider, container, false));
                    }
                    log("shown count: " + s_senders.size());
                    if(s_count != -1)
                        s_count = s_senders.size();
                    getView().findViewById(R.id.loading_bar).setVisibility(View.INVISIBLE);
                    getView().findViewById(R.id.scroll).setVisibility(View.VISIBLE);
                }
            };

            @Override
            public boolean onQueryTextSubmit(String q) {
                if(q.replaceAll(" ", "").length() < 3)
                    return false;
                if(q.charAt(q.length()-1) == ' ')
                    q = q.substring(0, q.length()-1);
                if(q.charAt(0) == ' ')
                    q = q.substring(1, q.length()-1);
                final String query = q;
                getView().findViewById(R.id.tv_error).setVisibility(View.INVISIBLE);
                log( "query: '" + query + "'");

                s_query = query;
                this.query = query;
                getView().findViewById(R.id.loading_bar).setVisibility(View.VISIBLE);
                getView().findViewById(R.id.scroll).setVisibility(View.INVISIBLE);

                log("search mode " + search_mode);
                if(search_mode == 0) {
                    new Thread() {
                        @Override
                        public void run() {
                            try {
                                s_senders = new ArrayList<>();
                                s_messages = new ArrayList<>();
                                s_threadIds = new ArrayList<>();
                                s_time = new ArrayList<>();
                                s_msgIds = new ArrayList<>();
                                s_group = new ArrayList<>();

                                String result = connect("https://app.eschool.center/ec-server/chat/searchThreads?rowStart=1&rowsCount=15&text=" + query, null, context);
                                log("search result: " + result);
                                JSONArray array = new JSONArray(result), a, b;
                                if (array.length() == 0) {
                                    error = "Нет сообщений, удовлетворяющих условиям поиска";
                                    h.sendEmptyMessage(123);
                                    return;
                                }
                                JSONObject obj, c;
                                String A, B, C;
                                for (int i = 0; i < array.length(); i++) {
                                    obj = array.getJSONObject(i);
                                    a = obj.getJSONArray("filterNumbers");
                                    for (int j = 0; j < a.length(); j++) {
                                        result = connect("https://app.eschool.center/ec-server/chat/messages?getNew=false&isSearch=false&rowStart=0&rowsCount=1" +
                                                "&threadId=" + obj.getInt("threadId") + "&msgStart=" + (a.optInt(j) + 1), null, context);
                                        log("result: " + result);
                                        b = new JSONArray(result);
                                        c = b.getJSONObject(0);
                                        obj = array.getJSONObject(i);
                                        A = obj.getString("senderFio").split(" ")[0];
                                        B = obj.getString("senderFio").split(" ")[1];
                                        if (obj.getString("senderFio").split(" ").length <= 2) {
                                            loge("fio strange:");
                                            loge(obj.toString());
                                            C = "a";
                                        } else
                                            C = obj.getString("senderFio").split(" ")[2];
                                        s_senders.add(A + " " + B.charAt(0) + ". " + C.charAt(0) + ".");
                                        s_messages.add(c.getString("msg"));
                                        s_threadIds.add(c.getInt("threadId"));
                                        s_msgIds.add(a.optInt(j));
                                        Date date = new Date(c.getLong("createDate"));
                                        s_time.add(String.format(Locale.UK, "%02d:%02d", date.getHours(), date.getMinutes()));

                                        if(c.has("addrCnt"))
                                            s_group.add(c.getInt("addrCnt") > 2);
                                        else
                                            s_group.add(false);
                                    }
                                }
                                if (array.length() < 15)
                                    s_count = -1;
                                h.sendEmptyMessage(0);
                            } catch (Exception e) {
                                loge(e.toString());
                            }
                        }
                    }.start();
                } else if(search_mode == 1) {
                    s_count = -1;
                    new Thread() {
                        @Override
                        public void run() {
                            result = new ArrayList<>();
                            for (Person person: olist) {
                                if(person.words.length == 0)
                                    continue;
                                for (String s: person.words) {
                                    if(s.toLowerCase().contains(query.toLowerCase())) {
                                        result.add(person);
                                        break;
                                    }
                                }
                            }
                            if (result.size() == 0) {
                                error = "Нет адресатов, удовлетворяющих условиям поиска";
                                h.sendEmptyMessage(123);
                            } else
                                h.sendEmptyMessage(0);
                        }
                    }.start();
                }
                //myActionMenuItem.collapseActionView();
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                log("query changed: " + s);
                String q = s;
                if(s.replaceAll(" ", "").equals(""))
                    return false;
                if(q.charAt(q.length()-1) == ' ')
                    q = q.substring(0, q.length()-1);
                if(q.charAt(0) == ' ')
                    q = q.substring(1, q.length()-1);
                query = q;
                if (search_mode == 1) {
                    s_count = -1;
                    new Thread() {
                        @Override
                        public void run() {
                            result = new ArrayList<>();
                            for (Person person : olist) {
                                if (person.words.length == 0)
                                    continue;
                                for (String s : person.words) {
                                    if (s.toLowerCase().contains(query.toLowerCase())) {
                                        result.add(person);
                                        break;
                                    }
                                }
                            }
                            if (result.size() == 0) {
                                error = "Нет адресатов, удовлетворяющих условиям поиска";
                                h.sendEmptyMessage(123);
                            } else
                                h.sendEmptyMessage(0);
                        }
                    }.start();
                }
                /*if(s.trim().equals("")) {
//                    h.sendEmptyMessage(2);
                    onViewCreated(getView(), null);
                }*/
                return false;
            }
        });
        myActionMenuItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                log("collapsing");
                Bundle bundle = new Bundle();
                bundle.putBoolean("collapsing", true);
                //onViewCreated(getView(), bundle);
                count = 25;
                s_count = 0;
                show();
                return true;
            }
        });
        super.onCreateOptionsMenu(menu, inflate);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.refresh:
                refresh();
                break;
            case R.id.new_dialog:
                search_mode = 1;
                searchView.expandActionView();
                view.findViewById(R.id.knock_l).setVisibility(View.GONE);
                break;
            case R.id.action_search:
                search_mode = 0;
                break;
        }
        log("options item selected");
        return super.onOptionsItemSelected(item);
    }

    private boolean uploading = false;

    @Override
    public void onResume() {
        log("onResume() MessagesF");
        Toolbar toolbar = getActivity().findViewById(R.id.toolbar);
        toolbar.setSubtitle("");
        super.onResume();
    }

    @Override
    public void onViewCreated(final View view, @Nullable final Bundle savedInstanceState) {
        if(READY && !shown)
            show();
        if(READY)
            if(getActivity().getSharedPreferences("pref", 0).getBoolean("show_chat", true))
                view.findViewById(R.id.knock_l).setVisibility(View.VISIBLE);
            else
                view.findViewById(R.id.knock_l).setVisibility(View.GONE);
        if(savedView != null)
            if(savedInstanceState == null)
                return;
            else if(!savedInstanceState.getBoolean("collapsing"))
                return;
        log("onViewCreated");
        /*int I = 0;
        while(true) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException ignore) {}
            if(I % 20 == 0)
                log(".");
            I++;
            if(users != null)
                break;
            if(I==200)
                start();
        }*/
        if(view == null || f_senders == null) {
            loge("null in MessagesFragment");
            return;
        }

        if(fromNotification) {
            log("fromNotif");
            loadChat(notifThreadId, (notifCount > 2?f_topics:f_senders).get(f_threadIds.indexOf(notifThreadId)), -1, notifCount > 2);
        }
    }

    void show() {
        final LinearLayout container1 = view.findViewById(R.id.container);
        if(container1 == null) {
            loge("container null in MessagesFragment");
            return;
        }
        container1.removeAllViews();
        if(getActivity().getSharedPreferences("pref", 0).getBoolean("show_chat", true)) {
            view.findViewById(R.id.knock_l).setVisibility(View.VISIBLE);
            view.findViewById(R.id.knock_l).setOnClickListener(v -> {
                FragmentTransaction transaction = getFragmentManager().beginTransaction();
                KnockFragment fragment = new KnockFragment();
                ((MainActivity) getActivity()).set_visible(false);
                transaction.replace(R.id.chat_container, fragment);
                transaction.addToBackStack(null);
                transaction.commit();
            });
        } else
            view.findViewById(R.id.knock_l).setVisibility(View.GONE);
        TextView tv = view.findViewById(R.id.tv_error);
        tv.setText("");
        tv.setVisibility(View.INVISIBLE);

        final ScrollView scroll = view.findViewById(R.id.scroll);
//        new Thread() {
//            @Override
//            public void run() {
                View item;
//                TextView tv;
                ImageView img;
                LayoutInflater inflater = getLayoutInflater();

                fitems = new View[f_senders.size()];

                int c = 0;
                for (int i = 0; i < f_senders.size(); i++) {
                    item = inflater.inflate(R.layout.thread_item, container1, false);
                    tv = item.findViewById(R.id.tv_sender);
                    tv.setText(f_senders.get(i));
                    tv = item.findViewById(R.id.tv_topic);
                    tv.setText(Html.fromHtml(f_topics.get(i)));
                    tv = item.findViewById(R.id.tv_users);
                    img = item.findViewById(R.id.img);
                    if (f_users.get(i) == 0 || f_users.get(i) == 2) {
                        img.setImageDrawable(getResources().getDrawable(R.drawable.dialog));
                        tv.setText("");
                    } else if (f_users.get(i) == 1) {
                        img.setImageDrawable(getResources().getDrawable(R.drawable.monolog));
                        tv.setText("");
                    } else {
                        img.setImageDrawable(getResources().getDrawable(R.drawable.group));
                        tv.setText(f_users.get(i) + "");
                    }
                    final int j = i;
                    item.setTag(f_threadIds.get(j));
                    tv = item.findViewById(R.id.tv_new);
                    if (f_newCounts.get(i) > 0) {
                        tv.setVisibility(View.VISIBLE);
                        tv.setText(f_newCounts.get(i) + "");
                        loge("new msg: " + f_senders.get(i));
                    }
                    c += f_newCounts.get(i);
                    final int users = f_users.get(i);
                    //item.setTag(R.id.TAG_THREAD, f_threadIds.get(j));
                    item.setOnClickListener(v -> {
                        if(v instanceof ViewGroup) {
                            TextView textv = v.findViewById(R.id.tv_new);
                            textv.setText("");
                            textv.setVisibility(View.INVISIBLE);
                        }
                        loadChat(f_threadIds.get(j), (users > 2?f_topics:f_senders).get(j), -1, users > 2);
                    });
                    fitems[i] = item;
                }
                final int C = c;
//                getActivity().runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
                        for (View fitem : fitems) {
                            container1.addView(fitem, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                            container1.addView(getLayoutInflater().inflate(R.layout.divider, container1, false));
                        }
                        BottomNavigationView bottomnav = getActivity().findViewById(R.id.bottomnav);
                        BottomNavigationMenuView bottomNavigationMenuView =
                                (BottomNavigationMenuView) bottomnav.getChildAt(0);
                        final BottomNavigationItemView itemView = (BottomNavigationItemView)  bottomNavigationMenuView.getChildAt(2);
                        log("unread messages count: " + C);
                        tv = itemView.findViewById(R.id.tv_badge);
                        if(C > 0) {
                            tv.setVisibility(View.VISIBLE);
                            tv.setText(C + "");
                        } else
                            tv.setVisibility(View.INVISIBLE);

                        scroll.scrollTo(0, 0);
                        refreshL = view.findViewById(R.id.l_refresh);
                        refreshL.setOnRefreshListener(() -> {
                            log("refreshing rar");
                            refresh();
                        });
                        savedView = view;
//                    }
//                });
//            }
//        }.start();

        if(first_time) {
            new Thread() {
                @Override
                public void run() {
                    scrollListener = new ViewTreeObserver.OnScrollChangedListener() {
                        @Override
                        public void onScrollChanged() {
                            if (scroll.getChildAt(0).getBottom() - 200
                                    <= (scroll.getHeight() + scroll.getScrollY()) && !uploading) {
                                log("bottom");
                                if (count == -1) {
                                    log("all threads are shown");
                                    return;
                                }
                                uploading = true;
                                @SuppressLint("HandlerLeak") final Handler h = new Handler() {
                                    @Override
                                    public void handleMessage(Message msg) {
                                        if (msg.what == 0) {
                                            LinearLayout container = view.findViewById(R.id.container);

                                            View item;
                                            TextView tv;
                                            ImageView img;
                                            LayoutInflater inflater = getLayoutInflater();
                                            final int l = senders.length;
                                            final int f_count = count;
                                            for (int i = 0; i < l; i++) {
                                                item = inflater.inflate(R.layout.thread_item, container, false);
                                                tv = item.findViewById(R.id.tv_sender);
                                                tv.setText(senders[i]);
                                                tv = item.findViewById(R.id.tv_topic);
                                                tv.setText(topics[i]);
                                                tv = item.findViewById(R.id.tv_users);
                                                img = item.findViewById(R.id.img);
                                                final int u = users[i];
                                                if (users[i] == 0 || users[i] == 2) {
                                                    img.setImageDrawable(getResources().getDrawable(R.drawable.dialog));
                                                    tv.setText("");
                                                } else if (users[i] == 1) {
                                                    img.setImageDrawable(getResources().getDrawable(R.drawable.monolog));
                                                    tv.setText("");
                                                } else {
                                                    img.setImageDrawable(getResources().getDrawable(R.drawable.group));
                                                    tv.setText(users[i] + "");
                                                }
                                                final int j = i;
                                                item.setOnClickListener(new View.OnClickListener() {
                                                    @Override
                                                    public void onClick(View v) {
                                                        if (f_count != -1) {
                                                            loadChat(f_threadIds.get(f_count + 25 - (l - j)),
                                                                    (u > 2?f_topics:f_senders).get(f_count + 25 - (l - j)), -1, u > 2);
                                                        } else {
                                                            loadChat(f_threadIds.get(f_threadIds.size() - (l - j)),
                                                                    (u > 2?f_topics:f_senders).get(f_threadIds.size() - (l - j)), -1, u > 2);
                                                        }
                                                    }
                                                });
                                                container.addView(item, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                                                container.addView(inflater.inflate(R.layout.divider, container, false));
                                            }
                                            if (count != -1)
                                                count += senders.length;
                                            uploading = false;
                                        } else if (msg.what == 1) {
                                            LayoutInflater inflater = getLayoutInflater();
                                            View item;
                                            TextView tv;
                                            ImageView img;
                                            int index;
                                            Spanned mess;
                                            String s;
                                            Spannable spannable;
                                            for (int i = s_count; i < s_senders.size(); i++) {
                                                item = inflater.inflate(R.layout.thread_item, container, false);
                                                tv = item.findViewById(R.id.tv_sender);
                                                tv.setText(s_senders.get(i));
                                                tv = item.findViewById(R.id.tv_topic);
                                                mess = Html.fromHtml(s_messages.get(i));

                                                index = mess.toString().toLowerCase().indexOf(s_query.toLowerCase());
                                                log("index: " + index);
                                                int start, end;
                                                if (index > 30)
                                                    start = index - 30;
                                                else
                                                    start = 0;
                                                if (mess.toString().length() > index + s_query.length() + 30)
                                                    end = index + s_query.length() + 30;
                                                else
                                                    end = mess.toString().length() - 1;
                                                s = (start == 0 ? "" : "...") + mess.subSequence(start, end).toString() + (end == mess.toString().length() - 1 ? "" : "...");
                                                spannable = new SpannableString(s);
                                                spannable.setSpan(new BackgroundColorSpan(getResources().getColor(R.color.colorPrimaryDark)), s.toLowerCase().indexOf(s_query.toLowerCase()), s.toLowerCase().indexOf(s_query.toLowerCase()) + s_query.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                                                tv.setText(spannable);
                                                tv = item.findViewById(R.id.tv_users);
                                                tv.setText("");
//                            img = item.findViewById(R.id.img);
                                                final int j = i;
                                                item.setOnClickListener(new View.OnClickListener() {
                                                    @Override
                                                    public void onClick(View v) {
                                                        loadChat(s_threadIds.get(j), s_senders.get(j), s_msgIds.get(j), s_group.get(j));
                                                    }
                                                });
                                                container.addView(item, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                                                container.addView(inflater.inflate(R.layout.divider, container, false));
                                            }
                                            if (msg.arg1 == 0) {
                                                s_count = s_senders.size();
                                            } else
                                                s_count = -1;
                                        }
                                    }
                                };
                                new Thread() {
                                    @Override
                                    public void run() {
                                        try {
                                            if (s_senders == null) {
                                                if (count == -1)
                                                    return;
                                                JSONArray array = new JSONArray(connect("https://app.eschool.center/ec-server/chat/threads?newOnly=false&row=" + (count + 1) + "&rowsCount=25", null, context));
                                                senders = new String[array.length()];
                                                topics = new String[array.length()];
                                                users = new int[array.length()];
                                                threadIds = new int[array.length()];
                                                newCounts = new int[array.length()];
                                                JSONObject obj;
                                                String a, b, c;
                                                log(array.length() + "");
                                                if (array.length() < 25)
                                                    count = -1;
                                                for (int i = 0; i < array.length(); i++) {
                                                    obj = array.getJSONObject(i);
                                                    a = obj.getString("senderFio").split(" ")[0];
                                                    b = obj.getString("senderFio").split(" ")[1];
                                                    if (obj.getString("senderFio").split(" ").length <= 2) {
                                                        loge("fio strange:");
                                                        loge(obj.toString());
                                                        c = "a";
                                                    } else
                                                        c = obj.getString("senderFio").split(" ")[2];
                                                    senders[i] = a + " " + b.charAt(0) + ". " + c.charAt(0) + ".";
                                                    if (obj.getString("subject").replaceAll(" ", "").equals(""))
                                                        if (obj.has("msgPreview"))
                                                            topics[i] = obj.getString("msgPreview");
                                                        else
                                                            topics[i] = "";
                                                    else
                                                        topics[i] = obj.getString("subject");
                                                    users[i] = obj.getInt("addrCnt");
                                                    if (obj.getInt("senderId") == PERSON_ID) {
                                                        users[i] = 1;
                                                    }
                                                    threadIds[i] = obj.getInt("threadId");
                                                    newCounts[i] = obj.getInt("newReplayCount");
                                                }
                                                for (int i = 0; i < users.length; i++) {
                                                    f_users.add(users[i]);
                                                    f_threadIds.add(threadIds[i]);
                                                    f_senders.add(senders[i]);
                                                    f_topics.add(topics[i]);
                                                    f_newCounts.add(newCounts[i]);
                                                }
                                                log("first thread: " + senders[0]);
                                                h.sendEmptyMessage(0);
                                            } else if (s_count != -1 && search_mode != 1) {
                                                String result = connect("https://app.eschool.center/ec-server/chat/searchThreads?rowStart=" + s_count + "&rowsCount=25&text=" + s_query, null, context);
                                                log("search result: " + result);
                                                JSONArray array = new JSONArray(result), a, b;
                                                if (array.length() == 0) {
                                                    h.sendEmptyMessage(2);
                                                    return;
                                                }
                                                JSONObject obj, c;
                                                for (int i = 0; i < array.length(); i++) {
                                                    obj = array.getJSONObject(i);
                                                    a = obj.getJSONArray("filterNumbers");
                                                    for (int j = 0; j < a.length(); j++) {
                                                        result = connect("https://app.eschool.center/ec-server/chat/messages?getNew=false&isSearch=false&rowStart=0&rowsCount=1" +
                                                                "&threadId=" + obj.getInt("threadId") + "&msgStart=" + (/*obj.getInt("msgNum")*/a.optInt(j) + 1), null, context);

                                                        b = new JSONArray(result);
                                                        c = b.getJSONObject(0);
                                                        s_senders.add(c.getString("senderFio"));
                                                        s_messages.add(c.getString("msg"));
                                                        s_threadIds.add(c.getInt("threadId"));
                                                        s_msgIds.add(a.optInt(j));
                                                        s_group.add(c.getInt("addrCnt") > 2);
                                                        Date date = new Date(c.getLong("createDate"));
                                                        s_time.add(String.format(Locale.UK, "%02d:%02d", date.getHours(), date.getMinutes()));
                                                    }
                                                }
                                                h.sendMessage(h.obtainMessage(1, array.length() == 25 ? 0 : 1, 0));
                                            }
                                        } catch (Exception e) {
                                            loge(e.toString());
                                        }
                                    }
                                }.start();
                            }
                        }
                    };
                    getActivity().runOnUiThread(() -> scroll.getViewTreeObserver()
                            .addOnScrollChangedListener(scrollListener));
                }
            }.start();
            Toolbar toolbar = getActivity().findViewById(R.id.toolbar);
            toolbar.setTitle("Сообщения");
            toolbar.setOnClickListener(v -> {
                log("click on toolbar");
                if(!(((MainActivity) getActivity()).getStackTop() instanceof MessagesFragment))
                    return;
                final ScrollView scroll1 = view.findViewById(R.id.scroll);
                scroll1.post(() -> scroll1.scrollTo(0, 0));
            });
            setHasOptionsMenu(true);
            ((MainActivity)getActivity()).setSupportActionBar(toolbar);
            first_time = false;
        }

        view.findViewById(R.id.loading_bar).setVisibility(View.INVISIBLE);
        view.findViewById(R.id.l_refresh).setVisibility(View.VISIBLE);
        if(fromNotification) {
            log("fromNotif");
            loadChat(notifThreadId, f_senders.get(f_threadIds.indexOf(notifThreadId)), -1, notifCount > 2);
        }
        shown = true;
    }

    void newMessage(String text, long time, int sender_id, int thread_id) {
        log("new message in MessagesFragment");
        LinearLayout container = view.findViewById(R.id.container);
        View thread = container.findViewWithTag(thread_id);
        if(thread == null) {
            loge("this thread was not found");
            return;
        }
        TextView tv = thread.findViewById(R.id.tv_new);
        if(tv.getVisibility() == View.INVISIBLE) {
            tv.setVisibility(View.VISIBLE);
            tv.setText("1");
        } else {
            tv.setText(Integer.parseInt(tv.getText().toString()) + 1 + "");
        }
        tv = thread.findViewById(R.id.tv_topic);
        tv.setText(text);
        BottomNavigationView bottomnav = getActivity().findViewById(R.id.bottomnav);
        BottomNavigationMenuView bottomNavigationMenuView =
                (BottomNavigationMenuView) bottomnav.getChildAt(0);
        final BottomNavigationItemView itemView = (BottomNavigationItemView)  bottomNavigationMenuView.getChildAt(2);
        tv = itemView.findViewById(R.id.tv_badge);
        if(tv.getVisibility() == View.INVISIBLE) {
            tv.setVisibility(View.VISIBLE);
            tv.setText("1");
        } else
            tv.setText(Integer.parseInt(tv.getText().toString()) + 1 + "");
        ScrollView scroll = view.findViewById(R.id.scroll);
        scroll.scrollTo(0, container.getBottom());
    }

    private void download(Handler handler) throws JSONException, IOException {
        JSONArray array = new JSONArray(
                connect("https://app.eschool.center/ec-server/chat/threads?newOnly=false&row=1&rowsCount=25",
                        null, context));
        senders = new String[array.length()];
        topics = new String[array.length()];
        users = new int[array.length()];
        threadIds = new int[array.length()];
        newCounts = new int[array.length()];
        JSONObject obj;
        String a, b, c;
        log(array.length() + "");
        for (int i = 0; i < array.length(); i++) {
            obj = array.getJSONObject(i);
            String[] senderFio = obj.getString("senderFio").split(" ");
            a = "";
            b = "";
            c = "";
            if(senderFio.length > 0)
                a = senderFio[0];
            if(senderFio.length > 1)
                b = senderFio[1];
            if(senderFio.length > 2)
                c = senderFio[2];
            senders[i] = a + " " + (b.equals("")?"":b.charAt(0) + ". ") + (c.equals("")?"":c.charAt(0) + ".");
            if(obj.getString("subject").equals(" "))
                if(obj.has("msgPreview"))
                    topics[i] = obj.getString("msgPreview");
                else
                    topics[i] = "";
            else
                topics[i] = obj.getString("subject");
            users[i] = obj.getInt("addrCnt");
            if(obj.getInt("senderId") == PERSON_ID) {
                users[i] = 1;
            }
            threadIds[i] = obj.getInt("threadId");
            newCounts[i] = obj.getInt("newReplayCount");
        }
        f_users = new ArrayList<>();
        f_threadIds = new ArrayList<>();
        f_senders = new ArrayList<>();
        f_topics = new ArrayList<>();
        f_newCounts = new ArrayList<>();
        for (int i = 0; i < users.length; i++) {
            f_users.add(users[i]);
            f_threadIds.add(threadIds[i]);
            f_senders.add(senders[i]);
            f_topics.add(topics[i]);
            f_newCounts.add(newCounts[i]);
        }
        log("first thread: " + senders[0]);
        if(handler != null)
            handler.sendEmptyMessage(0);
    }

    void refresh() {
        if(refreshing)
            return;
        if(refreshL == null)
            return;
        refreshL.setRefreshing(true);

        @SuppressLint("HandlerLeak") final Handler h = new Handler() {

            View[] items;

            @Override
            public void handleMessage(Message msg) {
                log("handling");
                items = new View[f_senders.size()];
                final LinearLayout container1 = view.findViewById(R.id.container);
                if(container1 == null)
                    return;
                container1.removeAllViews();

                if(searchView != null) {
                    if(searchView.isActionViewExpanded()) {
                        searchView.collapseActionView();

                        if(getActivity().getSharedPreferences("pref", 0).getBoolean("show_chat", true)) {
                            view.findViewById(R.id.knock_l).setVisibility(View.VISIBLE);
                            view.findViewById(R.id.knock_l).setOnClickListener(v ->  {
                                FragmentTransaction transaction = getFragmentManager().beginTransaction();
                                KnockFragment fragment = new KnockFragment();
                                ((MainActivity) getActivity()).set_visible(false);
                                transaction.replace(R.id.chat_container, fragment);
                                transaction.addToBackStack(null);
                                transaction.commit();
                            });
                        } else
                            view.findViewById(R.id.knock_l).setVisibility(View.GONE);
                    }
                }

                final Handler h = new Handler() {
                    @Override
                    public void handleMessage(Message msg) {
                        TextView tv = view.findViewById(R.id.tv_error);
                        tv.setText("");
                        tv.setVisibility(View.INVISIBLE);
                        for (int i = 0; i < f_senders.size(); i++) {
                            if(i < items.length)
                                container1.addView(items[i], ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                            container1.addView(getLayoutInflater().inflate(R.layout.divider, container1, false));
                        }
                        final ScrollView scroll = view.findViewById(R.id.scroll);
                        scroll.scrollTo(0, 0);
                        BottomNavigationView bottomnav = getActivity().findViewById(R.id.bottomnav);
                        BottomNavigationMenuView bottomNavigationMenuView =
                                (BottomNavigationMenuView) bottomnav.getChildAt(0);
                        final BottomNavigationItemView itemView = (BottomNavigationItemView)  bottomNavigationMenuView.getChildAt(2);
                        tv = itemView.findViewById(R.id.tv_badge);
                        if(msg.what == 0) {
                            tv.setVisibility(View.INVISIBLE);
                        } else {
                            tv.setVisibility(View.VISIBLE);
                            tv.setText(msg.what + "");
                        }
                        refreshL.setRefreshing(false);
                        refreshing = false;
                    }
                };
                new Thread() {
                    @Override
                    public void run() {
                        View item;
                        TextView tv;
                        ImageView img;
                        int c = 0;
                        for (int i = 0; i < items.length; i++) {
                            item = getLayoutInflater().inflate(R.layout.thread_item, container1, false);
                            tv = item.findViewById(R.id.tv_sender);
                            tv.setText(f_senders.get(i));
                            tv = item.findViewById(R.id.tv_topic);
                            tv.setText(Html.fromHtml(f_topics.get(i)));
                            tv = item.findViewById(R.id.tv_users);
                            img = item.findViewById(R.id.img);

                            if(f_users.get(i) == 0 || f_users.get(i) == 2) {
                                img.setImageDrawable(getResources().getDrawable(R.drawable.dialog));
                                tv.setText("");
                            } else if(f_users.get(i) == 1) {
                                img.setImageDrawable(getResources().getDrawable(R.drawable.monolog));
                                tv.setText("");
                            } else {
                                img.setImageDrawable(getResources().getDrawable(R.drawable.group));
                                tv.setText(f_users.get(i) + "");
                            }
                            if(f_newCounts.get(i) > 0) {
                                tv = item.findViewById(R.id.tv_new);
                                tv.setVisibility(View.VISIBLE);
                                tv.setText("" + f_newCounts.get(i));
                            }
                            c+=f_newCounts.get(i);
                            final int j = i;
                            item.setTag(f_threadIds.get(j));
                            final int users = f_users.get(i);
                            //item.setTag(R.id.TAG_THREAD, f_threadIds.get(j));
                            item.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    loadChat(f_threadIds.get(j), f_senders.get(j), -1, users > 2);
                                }
                            });
                            items[i] = item;
                        }
                        h.sendEmptyMessage(c);
                    }
                }.start();


            }
        };
        new Thread() {
            @Override
            public void run() {
                try {
                    download(h);
                } catch (Exception e) {
                    loge("refreshing: " + e.toString());}
            }
        }.start();
    }

    private void loadChat(int threadId, String threadName, int searchId, boolean group) {
        fromNotification = false;
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
<<<<<<< HEAD
        chat1Fragment fragment = new chat1Fragment();
        LoginActivity.log("chat thread " + threadId);
=======
        ChatFragment fragment = new ChatFragment();
        log("chat thread " + threadId);
>>>>>>> upstream/master
        fragment.threadId = threadId;//f_threadIds.get(j);
        fragment.threadName = threadName;//f_senders.get(j);
        fragment.context = context;
        fragment.group = group;
        if(searchId != -1)
            fragment.searchMsgId = searchId;
        ((MainActivity)getActivity()).set_visible(false);
        transaction.replace(R.id.chat_container, fragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    public View getView() {
        if(view == null) {
            loge("null view!!");
            if(super.getView() != null)
                return super.getView();
            else
                return new View(getActivity());
        } else
            return view;
    }

    class Person {
        String fio;
        String info;
        String[] words;
        int prsId;
        Person() {}
    }
}

