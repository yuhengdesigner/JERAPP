package com.example.jerapp;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public final class SessionUtils {
    private static final String USER_PREFS = "UserPrefs";
    private static final String KEY_GUEST_MODE = "guest_mode";
    private static final String KEY_GUEST_UID = "guest_uid";

    private SessionUtils() {}

    public static boolean isGuest(Context context) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        // REQUIREMENT: A user is ONLY a guest if there is an active anonymous Firebase session.
        // We do NOT trust SharedPreferences alone because it can be restored from old backups.
        return user != null && user.isAnonymous();
    }

    public static void markGuest(Context context, String uid) {
        SharedPreferences.Editor editor = context.getSharedPreferences(USER_PREFS, Context.MODE_PRIVATE).edit();
        editor.putBoolean(KEY_GUEST_MODE, true);
        if (uid != null) editor.putString(KEY_GUEST_UID, uid);
        editor.apply();
    }

    public static String getStoredGuestUid(Context context) {
        return context.getSharedPreferences(USER_PREFS, Context.MODE_PRIVATE).getString(KEY_GUEST_UID, null);
    }

    public static void markRegistered(Context context) {
        SharedPreferences.Editor editor = context.getSharedPreferences(USER_PREFS, Context.MODE_PRIVATE).edit();
        editor.putBoolean(KEY_GUEST_MODE, false);
        editor.apply();
    }

    public static void clearGuestSession(Context context) {
        // Clear User Identity Prefs
        context.getSharedPreferences(USER_PREFS, Context.MODE_PRIVATE)
                .edit()
                .remove(KEY_GUEST_MODE)
                .remove(KEY_GUEST_UID)
                .apply();

        // Clear any stale emergency tracking state
        context.getSharedPreferences("OngoingEmergencyPrefs", Context.MODE_PRIVATE)
                .edit()
                .clear()
                .apply();
    }
}
