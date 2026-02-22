package com.familyringer;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Single source of truth for local session state.
 * Stores groupId, role, member name after setup is complete.
 */
public class SessionManager {

    private static final String PREFS = "FamilyRingerSession";
    private static final String KEY_GROUP_ID   = "group_id";
    private static final String KEY_GROUP_NAME = "group_name";
    private static final String KEY_ROLE       = "role";
    private static final String KEY_NAME       = "name";
    private static final String KEY_ALERTS     = "alerts";

    private final SharedPreferences prefs;

    public SessionManager(Context ctx) {
        prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

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

    public String getGroupId()   { return prefs.getString(KEY_GROUP_ID, null); }
    public String getGroupName() { return prefs.getString(KEY_GROUP_NAME, ""); }
    public String getRole()      { return prefs.getString(KEY_ROLE, "child"); }
    public String getName()      { return prefs.getString(KEY_NAME, ""); }
    public boolean isParent()    { return "parent".equals(getRole()); }

    public void saveAlerts(String alertsJson) {
        prefs.edit().putString(KEY_ALERTS, alertsJson).apply();
    }

    public String getAlertsJson() {
        return prefs.getString(KEY_ALERTS, null);
    }

    public void clear() {
        prefs.edit().clear().apply();
    }

    public void saveAlertSoundUri(String uri) {
        if (uri == null) {
            prefs.edit().remove("alert_sound_uri").apply();
        } else {
            prefs.edit().putString("alert_sound_uri", uri).apply();
        }
    }

    public String getAlertSoundUri() {
        return prefs.getString("alert_sound_uri", null);
    }

    public void saveAlertVolume(int percent) {
        prefs.edit().putInt("alert_volume", percent).apply();
    }

    public int getAlertVolume() {
        return prefs.getInt("alert_volume", 100); // default 100%
    }

    public void saveAlertDuration(int seconds) {
        // 0 = don't stop
        prefs.edit().putInt("alert_duration", seconds).apply();
    }

    public int getAlertDuration() {
        return prefs.getInt("alert_duration", 0); // default: don't stop
    }
}
