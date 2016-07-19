package ca.itquality.patrol.util;

import android.preference.PreferenceManager;

import ca.itquality.patrol.library.util.app.MyApplication;

/**
 * Helper class.
 */
public class WearUtil {

    private static final String PREF_ACTIVITY_STATUS = "ActivityStatus";

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
                .getString(PREF_ACTIVITY_STATUS, "-");
    }
}
