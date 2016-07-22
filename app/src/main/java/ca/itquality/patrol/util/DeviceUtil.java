package ca.itquality.patrol.util;

import android.preference.PreferenceManager;
import android.text.TextUtils;

import ca.itquality.patrol.app.MyApplication;


public class DeviceUtil {

    public static final int MIN_PASSWORD_LENGTH = 6;
    private static final String PREF_USER_ID = "UserId";
    private static final String PREF_NAME = "Name";
    private static final String PREF_EMAIL = "Email";
    private static final String PREF_PHOTO = "Photo";
    private static final String PREF_ACTIVITY = "Activity";
    private static final String PREF_HEART_RATE = "HeartRate";
    private static final String PREF_STEPS = "Steps";

    public static boolean isValidEmail(String email) {
        return !TextUtils.isEmpty(email) && android.util.Patterns.EMAIL_ADDRESS.matcher(email)
                .matches();
    }

    /**
     * Save user login data in preferences.
     */
    public static void logIn(String userId, String name, String email, String photo) {
        PreferenceManager.getDefaultSharedPreferences(MyApplication.getContext()).edit()
                .putString(PREF_USER_ID, userId)
                .putString(PREF_NAME, name)
                .putString(PREF_EMAIL, email)
                .putString(PREF_PHOTO, photo)
                .apply();
    }

    /**
     * Checks if user is logged in.
     */
    public static boolean isLoggedIn() {
        return !TextUtils.isEmpty(DeviceUtil.getUserId());
    }

    /**
     * Retrieves user id from preferences.
     */
    public static String getUserId() {
        return PreferenceManager.getDefaultSharedPreferences(MyApplication.getContext())
                .getString(PREF_USER_ID, null);
    }

    /**
     * Retrieves user full name from preferences.
     */
    public static String getName() {
        return PreferenceManager.getDefaultSharedPreferences(MyApplication.getContext())
                .getString(PREF_NAME, null);
    }

    /**
     * Retrieves user email from preferences.
     */
    public static String getEmail() {
        return PreferenceManager.getDefaultSharedPreferences(MyApplication.getContext())
                .getString(PREF_EMAIL, null);
    }

    /**
     * Retrieves user photo from preferences.
     */
    public static String getPhoto() {
        return PreferenceManager.getDefaultSharedPreferences(MyApplication.getContext())
                .getString(PREF_PHOTO, null);
    }

    /**
     * Saves the activity status in preferences.
     */
    public static void setActivity(String activity) {
        PreferenceManager.getDefaultSharedPreferences(MyApplication.getContext()).edit()
                .putString(PREF_ACTIVITY, activity).apply();
    }

    /**
     * Saves the activity status in preferences.
     */
    public static String getActivity() {
        return PreferenceManager.getDefaultSharedPreferences(MyApplication.getContext())
                .getString(PREF_ACTIVITY, null);
    }

    /**
     * Saves the heart rate in preferences.
     */
    public static void setHeartRate(int heartRate) {
        PreferenceManager.getDefaultSharedPreferences(MyApplication.getContext()).edit()
                .putInt(PREF_HEART_RATE, heartRate).apply();
    }

    /**
     * Saves the activity status in preferences.
     */
    public static int getHeartRate() {
        return PreferenceManager.getDefaultSharedPreferences(MyApplication.getContext())
                .getInt(PREF_HEART_RATE, -1);
    }

    /**
     * Saves the steps count in preferences.
     */
    public static void setSteps(int steps) {
        PreferenceManager.getDefaultSharedPreferences(MyApplication.getContext()).edit()
                .putInt(PREF_STEPS, steps).apply();
    }

    /**
     * Saves the steps count in preferences.
     */
    public static int getSteps() {
        return PreferenceManager.getDefaultSharedPreferences(MyApplication.getContext())
                .getInt(PREF_STEPS, -1);
    }
}
