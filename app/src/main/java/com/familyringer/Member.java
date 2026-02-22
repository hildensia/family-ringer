package com.familyringer;

public class Member {
    public String uid;
    public String name;
    public String role;
    public String fcmToken;
    public boolean selected = false;

    public Member(String uid, String name, String role, String fcmToken) {
        this.uid = uid;
        this.name = name;
        this.role = role;
        this.fcmToken = fcmToken;
    }

    public boolean isChild() { return "child".equals(role); }
}
