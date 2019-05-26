package com.example.sch;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.ArrayList;
import java.util.List;

import static com.example.sch.LoginActivity.log;


public class MyFirebaseService extends FirebaseMessagingService {

    public MyFirebaseService() {
        super();
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        log("message received");
        log("from: " + remoteMessage.getFrom()); 
        log("msg id: " + remoteMessage.getMessageId());
        Intent data = remoteMessage.toIntent();
        long time = 15551882447791L;
        switch (data.getStringExtra("type")) {
            case "mark":
                int val = Integer.parseInt(data.getStringExtra("val")),
                        unitId = Integer.parseInt(data.getStringExtra("unitId"));
                double coef = Double.parseDouble(data.getStringExtra("coef"));
                ArrayList<PeriodFragment.Subject> subjects = TheSingleton.getInstance().getSubjects();
                PeriodFragment.Subject subject;
                for (int i = 0; i < subjects.size(); i++) {
                    subject = subjects.get(i);
                    if (subject.unitid == unitId) {
                        NotificationManager notificationManager;
                        NotificationManagerCompat compat = NotificationManagerCompat.from(this);
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

                        // background/not running
                        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "1");
                        builder.setContentTitle(subject.name + ": " + val + " с коэф. " + coef)
                                .setContentText("Новая оценка")
                                .setSmallIcon(R.drawable.attach);
                        Notification notif = builder.build();
                        compat.notify(0, notif);
                        break;
                    }
                }
                break;
            case "msg":
                log("sender: " + remoteMessage.toIntent().getStringExtra("senderFio"));
                log("text: " + remoteMessage.toIntent().getStringExtra("text"));
                String text = remoteMessage.toIntent().getStringExtra("text");
                String sender_fio = remoteMessage.toIntent().getStringExtra("senderFio");
                int sender = Integer.parseInt(remoteMessage.toIntent().getStringExtra("senderId"));
                int thread_id = Integer.parseInt(remoteMessage.toIntent().getStringExtra("threadId"));
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

                    if (isBackground()) {
                        // background/not running
                        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "1");
                        builder.setContentTitle(remoteMessage.toIntent().getStringExtra("senderFio"))
                                .setContentText(remoteMessage.toIntent().getStringExtra("text"))
                                .setSmallIcon(R.drawable.attach);
                        Notification notif = builder.build();
                        notificationManager.notify(0, notif);
                    } else {
                        // application is in foreground
                        log("sending to broadcast");
                        this.sendBroadcast(new Intent("com.example.sch.action").putExtra("text", text).putExtra("sender_fio", sender_fio)
                                .putExtra("time", time).putExtra("sender_id", sender).putExtra("thread_id", thread_id));
                    }
                    if (remoteMessage.getNotification() != null) {
                        log(remoteMessage.getNotification().getTitle() + "");
                        log(remoteMessage.getNotification().getBody() + "");
                    }
                }

                break;
            case "lesson":

                break;
        }


        //long time = Integer.valueOf(remoteMessage.toIntent().getStringExtra("date"));


        //Toast.makeText(getApplicationContext(), remoteMessage.getNotification().getTitle() + ":\n" + remoteMessage.getNotification().getBody(), Toast.LENGTH_LONG).show();
    }

    boolean isBackground() {
        ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> runningTaskInfo = manager.getRunningTasks(1);
        ComponentName componentInfo = runningTaskInfo.get(0).topActivity;
        return !componentInfo.getPackageName().equals("com.example.sch");
    }

    @Override
    public void onDeletedMessages() {
        log("onDeletedMessages");
    }
}
