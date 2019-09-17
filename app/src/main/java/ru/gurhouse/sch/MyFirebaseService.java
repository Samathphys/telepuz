package ru.gurhouse.sch;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.ComponentName;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

import static ru.gurhouse.sch.LoginActivity.log;
import static ru.gurhouse.sch.LoginActivity.loge;


public class MyFirebaseService extends FirebaseMessagingService {

    public MyFirebaseService() {
        super();
    }

    private final static String MESSAGES_GROUP = "1.RAArArrrAAAArr";

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        log("message received");
//        log("from: " + remoteMessage.getFrom());
//        log("msg id: " + remoteMessage.getMessageId());
        Intent data = remoteMessage.toIntent();

//        long time = System.currentTimeMillis();
        switch (data.getStringExtra("type")) {
            case "mark":
                log("new mark");
                int val = Integer.parseInt(data.getStringExtra("val")),
                        unitId = Integer.parseInt(data.getStringExtra("unitId"));
                double coef = Double.parseDouble(data.getStringExtra("coef"));
                NotificationManager notificationManager;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    notificationManager = getSystemService(NotificationManager.class);
                    if (notificationManager.getNotificationChannel("1") == null) {
                        CharSequence ch_name = "New messages";
                        String description = "CHANNEL_DESCRIPTION";

                        int importance = NotificationManager.IMPORTANCE_HIGH;
                        NotificationChannel channel = new NotificationChannel("1", ch_name, importance);
                        channel.setDescription(description);
                        // Register the channel with the system; you can't change the importance
                        // or other notification behaviors after this

                        notificationManager.createNotificationChannel(channel);
                    }
                }
                ArrayList<PeriodFragment.Subject> subjects = TheSingleton.getInstance().getSubjects();
                if(subjects == null) {
                    return;
                }
                PeriodFragment.Subject s;
                PendingIntent res = TaskStackBuilder.create(this)
                        .addNextIntentWithParentStack(new Intent(this, LoginActivity.class))
                        .getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
                for (int i = 0; i < subjects.size(); i++) {
                    s = subjects.get(i);
                    if (s.unitid == unitId) {
                        NotificationManagerCompat compat = NotificationManagerCompat.from(this);

                        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "1");
                        builder.setContentText(s.name + ": " + val + " с коэф. " + coef)
                                .setContentTitle("Новая оценка")
                                .setSmallIcon(R.drawable.alternative)
                                .setContentIntent(res);
                        Notification notif = builder.build();
                        compat.notify(TheSingleton.getInstance().notification_id++, notif);
                        TheSingleton.getInstance().setHasNotifications();
                        break;
                    }
                }
                break;
            case "msg":
                final int thread_id = Integer.parseInt(remoteMessage.toIntent().getStringExtra("threadId"));
                if(getSharedPreferences("pref", 0).getString("muted", "[]").contains("" + thread_id)) {
                    log("received muted thread message, sender: " + remoteMessage.toIntent().getStringExtra("senderFio"));
                    break;
                }
                log("new message");
                log("sender: " + remoteMessage.toIntent().getStringExtra("senderFio"));
                log("text: " + remoteMessage.toIntent().getStringExtra("text"));
                log("threadId: " + thread_id);

                int attachCount = 0;
                String attach_s = "";
                if(remoteMessage.toIntent().hasExtra("attachInfo")) {
                    attach_s = remoteMessage.toIntent().getStringExtra("attachInfo");
                    log("attach: " + attach_s);
                    try {
                        attachCount = new JSONArray(attach_s).length();
                    } catch (JSONException e) {
                        loge(e.toString());
                    }
                }
                int addrCnt = 0;
                if(remoteMessage.toIntent().hasExtra("addrCnt")) {
                    addrCnt = Integer.parseInt(remoteMessage.toIntent().getStringExtra("addrCnt"));
                }
                long time = Long.parseLong(data.getStringExtra("date"));
                String text = remoteMessage.toIntent().getStringExtra("text");
                String sender_fio = remoteMessage.toIntent().getStringExtra("senderFio");
                int sender = Integer.parseInt(remoteMessage.toIntent().getStringExtra("senderId"));
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    notificationManager = getSystemService(NotificationManager.class);
                    if (notificationManager.getNotificationChannel("1") == null) {
                        CharSequence ch_name = "New messages & marks";
                        String description = "CHANNEL_DESCRIPTION";

                        int importance = NotificationManager.IMPORTANCE_DEFAULT;
                        NotificationChannel channel = new NotificationChannel("1", ch_name, importance);
                        channel.setDescription(description);

                        notificationManager.createNotificationChannel(channel);
                    }
                }
                if (isBackground()) {
                    // background/not running
                    // Create an Intent for the activity you want to start
                    Intent resultIntent = new Intent(this, LoginActivity.class).putExtra("notif", true)
                            .putExtra("type", "msg").putExtra("threadId", thread_id).putExtra("count", addrCnt);
                    // Create the TaskStackBuilder and add the intent, which inflates the back stack
                    TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
                    stackBuilder.addNextIntentWithParentStack(resultIntent);
                    // Get the PendingIntent containing the entire back stack
                    PendingIntent resultPendingIntent =
                            stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
                    stackBuilder.editIntentAt(0).putExtra("notif", true)
                            .putExtra("type", "msg").putExtra("threadId", thread_id).putExtra("count", addrCnt);

                    PendingIntent actionIntent = PendingIntent.getBroadcast(this, thread_id,
                            new Intent(this, MyBroadcastReceiver.class).putExtra("action", MyBroadcastReceiver.ACTION_READ)
                                    .putExtra("threadId", thread_id), 0),
                            saveIntent = PendingIntent.getBroadcast(this, 465,
                                    new Intent(this, MyBroadcastReceiver.class).putExtra("action", MyBroadcastReceiver.ACTION_DOWNLOAD)
                                    .putExtra("data", attach_s), 0);

//                    log("groupId: " + groupId);
                    if(TheSingleton.getInstance().summary == null) {
                        log("creating summary");
                        Notification notification = new NotificationCompat.Builder(this, "1")
                                .setContentTitle("rar").setContentText("RAR").setSmallIcon(R.drawable.alternative)
                                .setGroup(MESSAGES_GROUP)
                                .setGroupSummary(true).build();
                        log("creating summary");
                        TheSingleton.getInstance().summary = notification;
                    }
                    NotificationManagerCompat.from(this).notify(0, TheSingleton.getInstance().summary);

                    NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "1");
                    builder.setContentTitle(sender_fio)
                            .setContentText(text)
                            .setSmallIcon(R.drawable.alternative)
                            .setContentIntent(resultPendingIntent)
                            .setWhen(time)
                            .setStyle(new NotificationCompat.BigTextStyle()
                                    .bigText(text + (attachCount>0?"\nВложений: " + attachCount:"")))
                            .setGroup(MESSAGES_GROUP)
                            .addAction(R.drawable.alternative, "Прочитано", actionIntent);
                    if(attachCount > 0 && !attach_s.equals(""))
                        builder.addAction(R.drawable.alternative, "Скачать (" + attachCount + ")", saveIntent);
                    Notification notif = builder.build();
                    int nId = TheSingleton.getInstance().notification_id;
                    log("nId " + nId);
                    NotificationManagerCompat.from(this).notify(nId, notif);
                    TheSingleton.getInstance().getNotifications().add(
                            new TheSingleton.Notification(thread_id, TheSingleton.getInstance().notification_id++));
                } else {
                    // application is in foreground
                    log("sending to broadcast");
                    sendBroadcast(new Intent("ru.gurhouse.sch.action").putExtra("text", text).putExtra("sender_fio", sender_fio)
                            .putExtra("time", time).putExtra("sender_id", sender).putExtra("thread_id", thread_id)
                            .putExtra("attach", attach_s));
                }
                if (remoteMessage.getNotification() != null) {
                    log(remoteMessage.getNotification().getTitle() + "");
                    log(remoteMessage.getNotification().getBody() + "");
                }
                break;
            case "lesson":
                log("new lesson");
                String subject = data.getStringExtra("subject");
                        //event = data.getStringExtra("event");
                coef = Integer.parseInt(data.getStringExtra("coef"));
                unitId = Integer.parseInt(data.getStringExtra("unitId"));
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    notificationManager = getSystemService(NotificationManager.class);
                    if (notificationManager.getNotificationChannel("1") == null) {
                        CharSequence ch_name = "New messages & marks";
                        String description = "CHANNEL_DESCRIPTION";

                        int importance = NotificationManager.IMPORTANCE_HIGH;
                        NotificationChannel channel = new NotificationChannel("1", ch_name, importance);
                        channel.setDescription(description);
                        // Register the channel with the system

                        notificationManager.createNotificationChannel(channel);
                    }
                }
                subjects = TheSingleton.getInstance().getSubjects();
                for (int i = 0; i < subjects.size(); i++) {
                    s = subjects.get(i);
                    if (s.unitid == unitId) {
                        NotificationManagerCompat compat = NotificationManagerCompat.from(this);

                        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "1");
                        builder.setContentTitle("Новая клетка")
                                .setContentText(subject + "/" + s.name + ": коэф " + coef)
                                .setSmallIcon(R.drawable.alternative);
                        Notification notif = builder.build();
                        compat.notify(TheSingleton.getInstance().notification_id++, notif);
                        break;
                    }
                }
                break;
        }


        //long time = Integer.valueOf(remoteMessage.toIntent().getStringExtra("date"));


        //Toast.makeText(getApplicationContext(), remoteMessage.getNotification().getTitle() + ":\n" + remoteMessage.getNotification().getBody(), Toast.LENGTH_LONG).show();
    }

    private boolean isBackground() {
        ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> runningTaskInfo = manager.getRunningTasks(1);
        ComponentName componentInfo = runningTaskInfo.get(0).topActivity;
        return !componentInfo.getPackageName().equals("ru.gurhouse.sch");
    }

    @Override
    public void onDeletedMessages() {
        log("onDeletedMessages");
    }
}
