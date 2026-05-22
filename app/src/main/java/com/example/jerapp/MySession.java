package com.example.jerapp;

import android.content.Context;
import android.content.SharedPreferences;

public class MySession {
    private static final String PREF_NAME = "AdminSession";

    public static String getAdminDeptId(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString("dept_id", ""); // Returns empty if not logged in
    }

    public static void saveAdminDeptId(Context context, String deptId) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit().putString("dept_id", deptId).apply();
    }
}