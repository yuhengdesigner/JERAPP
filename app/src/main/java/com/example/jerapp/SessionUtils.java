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
        if (user != null && user.isAnonymous()) return true;
        return context.getSharedPreferences(USER_PREFS, Context.MODE_PRIVATE)
                .getBoolean(KEY_GUEST_MODE, false);
    }

    public static void markGuest(Context context, String uid) {
        SharedPreferences.Editor editor = context.getSharedPreferences(USER_PREFS, Context.MODE_PRIVATE).edit();
        editor.putBoolean(KEY_GUEST_MODE, true);
        if (uid != null) editor.putString(KEY_GUEST_UID, uid);
        editor.apply();
    }

    public static void markRegistered(Context context) {
        context.getSharedPreferences(USER_PREFS, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_GUEST_MODE, false)
                .apply();
    }

    public static void clearGuestSession(Context context) {
        context.getSharedPreferences(USER_PREFS, Context.MODE_PRIVATE)
                .edit()
                .remove(KEY_GUEST_MODE)
                .remove(KEY_GUEST_UID)
                .apply();
    }
}
