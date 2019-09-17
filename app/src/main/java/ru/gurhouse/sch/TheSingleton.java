package ru.gurhouse.sch;

import java.util.ArrayList;

class TheSingleton {
    private static final TheSingleton ourInstance = new TheSingleton();
    private String COOKIE, ROUTE, fb_id;
    String login, hash;
    private int USER_ID, PERSON_ID;
    private ArrayList<PeriodFragment.Subject> subjects;
    private boolean hasNotifications = false;
    int notification_id = 1;
    private ArrayList<Notification> notifications = new ArrayList<>();
    android.app.Notification summary = null;
    long t1 = 0;

    static TheSingleton getInstance() {
        return ourInstance;
    }

    private TheSingleton() {}

    String getCOOKIE() {
        return COOKIE;
    }

    void setCOOKIE(String COOKIE) {
        this.COOKIE = COOKIE;
    }

    int getUSER_ID() {
        return USER_ID;
    }

    void setUSER_ID(int USER_ID) {
        this.USER_ID = USER_ID;
    }

    String getROUTE() {
        return ROUTE;
    }

    void setROUTE(String ROUTE) {
        this.ROUTE = ROUTE;
    }

    int getPERSON_ID() {
        return PERSON_ID;
    }

    void setPERSON_ID(int PERSON_ID) {
        this.PERSON_ID = PERSON_ID;
    }

    String getFb_id() {
        return fb_id;
    }

    void setFb_id(String fb_id) {
        this.fb_id = fb_id;
    }

    ArrayList<PeriodFragment.Subject> getSubjects() {
        return subjects;
    }

    void setSubjects(ArrayList<PeriodFragment.Subject> subjects) {
        this.subjects = subjects;
    }

    public boolean isHasNotifications() {
        return hasNotifications;
    }

    void setHasNotifications() {
        this.hasNotifications = true;
    }

    ArrayList<Notification> getNotifications() {return notifications;}

    static class Notification {
        int threadId;
        int notificationId;

        Notification(int threadId, int notificationId) {
            this.threadId = threadId;
            this.notificationId = notificationId;
        }

    }
}