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

    /**
     * Checks if provided email address has a valid format.
     */
    public static boolean isValidEmail(String email) {
        return !TextUtils.isEmpty(email) && android.util.Patterns.EMAIL_ADDRESS.matcher(email)
                .matches();
    }

    public static void logIn(String userId, String name, String email, String photo) {
        PreferenceManager.getDefaultSharedPreferences(MyApplication.getContext()).edit()
                .putString(PREF_USER_ID, userId)
                .putString(PREF_NAME, name)
                .putString(PREF_EMAIL, email)
                .putString(PREF_PHOTO, photo)
                .apply();
    }

    public static String getUserId() {
        return PreferenceManager.getDefaultSharedPreferences(MyApplication.getContext())
                .getString(PREF_USER_ID, null);
    }
}
