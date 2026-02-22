package com.familyringer;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {

    private static final String PREFS = "FamilyRingerSession";
    private static final String KEY_GROUP_ID   = "group_id";
    private static final String KEY_GROUP_NAME = "group_name";
    private static final String KEY_ROLE       = "role";
    private static final String KEY_NAME       = "name";
    private static final String KEY_ALERTS     = "alerts";
    private static final String KEY_SOUND_URI  = "alert_sound_uri";
    private static final String KEY_VOLUME     = "alert_volume";
    private static final String KEY_DURATION   = "alert_duration";

    private final SharedPreferences prefs;

    public SessionManager(Context ctx) {
        prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    // ── Setup ─────────────────────────────────────────────────────────────────

    public boolean isSetupComplete() {
        return prefs.contains(KEY_GROUP_ID);
    }

    public void saveSession(String groupId, String groupName, String role, String name) {
        prefs.edit()
                .putString(KEY_GROUP_ID, groupId)
                .putString(KEY_GROUP_NAME, groupName)
                .putString(KEY_ROLE, role)
                .putString(KEY_NAME, name)
                .apply();
    }

    public void clear() {
        prefs.edit().clear().apply();
    }

    // ── Group info ────────────────────────────────────────────────────────────

    public String getGroupId()   { return prefs.getString(KEY_GROUP_ID, null); }
    public String getGroupName() { return prefs.getString(KEY_GROUP_NAME, ""); }
    public String getRole()      { return prefs.getString(KEY_ROLE, "child"); }
    public String getName()      { return prefs.getString(KEY_NAME, ""); }
    public boolean isParent()    { return "parent".equals(getRole()); }

    // ── Alert messages ────────────────────────────────────────────────────────

    public void saveAlerts(String alertsJson) {
        prefs.edit().putString(KEY_ALERTS, alertsJson).apply();
    }

    public String getAlertsJson() {
        return prefs.getString(KEY_ALERTS, null);
    }

    // ── Sound ─────────────────────────────────────────────────────────────────

    public void saveAlertSoundUri(String uri) {
        if (uri == null) {
            prefs.edit().remove(KEY_SOUND_URI).apply();
        } else {
            prefs.edit().putString(KEY_SOUND_URI, uri).apply();
        }
    }

    public String getAlertSoundUri() {
        return prefs.getString(KEY_SOUND_URI, null);
    }

    // ── Volume ────────────────────────────────────────────────────────────────

    public void saveAlertVolume(int percent) {
        prefs.edit().putInt(KEY_VOLUME, percent).apply();
    }

    public int getAlertVolume() {
        return prefs.getInt(KEY_VOLUME, 100);
    }

    // ── Duration ──────────────────────────────────────────────────────────────

    public void saveAlertDuration(int seconds) {
        prefs.edit().putInt(KEY_DURATION, seconds).apply();
    }

    public int getAlertDuration() {
        return prefs.getInt(KEY_DURATION, 0); // 0 = don't stop automatically
    }
}