package ca.itquality.patrol.util;

import android.preference.PreferenceManager;

import java.text.SimpleDateFormat;
import java.util.Locale;

import ca.itquality.patrol.library.util.app.MyApplication;

/**
 * Helper class.
 */
public class WearUtil {

    public static final SimpleDateFormat AMBIENT_DATE_FORMAT =
            new SimpleDateFormat("HH:mm", Locale.US);
    private static final String PREF_ACTIVITY_STATUS = "ActivityStatus";
    private static final String PREF_LOGGED_IN = "LoggedIn";
    private static final String PREF_NAME = "Name";

    /**
     * Saves an activity status in shared preferences.
     */
    public static void setActivityStatus(String status) {
        PreferenceManager.getDefaultSharedPreferences(MyApplication.getContext()).edit()
                .putString(PREF_ACTIVITY_STATUS, status).apply();
    }

    /**
     * Retrieves an activity status from shared preferences.
     */
    public static String getActivityStatus() {
        return PreferenceManager.getDefaultSharedPreferences(MyApplication.getContext())
                .getString(PREF_ACTIVITY_STATUS, null);
    }

    public static void setLoggedIn(boolean loggedIn) {
        PreferenceManager.getDefaultSharedPreferences(MyApplication.getContext()).edit()
                .putBoolean(PREF_LOGGED_IN, loggedIn).apply();
    }

    public static boolean isLoggedIn() {
        return PreferenceManager.getDefaultSharedPreferences(MyApplication.getContext())
                .getBoolean(PREF_LOGGED_IN, false);
    }

    /**
     * Saves a user full name in preferences.
     */
    public static void setName(String name) {
        PreferenceManager.getDefaultSharedPreferences(MyApplication.getContext()).edit()
                .putString(PREF_NAME, name).apply();
    }

    /**
     * Retrieves user full name from preferences.
     */
    public static String getName() {
        return PreferenceManager.getDefaultSharedPreferences(MyApplication.getContext())
                .getString(PREF_NAME, null);
    }
}
