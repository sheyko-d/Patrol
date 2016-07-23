package ca.itquality.patrol.util;

import android.annotation.SuppressLint;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import ca.itquality.patrol.app.MyApplication;
import ca.itquality.patrol.auth.data.User;


public class DeviceUtil {

    public static final int MIN_PASSWORD_LENGTH = 6;
    private static final String PREF_TOKEN = "Token";
    private static final String PREF_USER_ID = "UserId";
    private static final String PREF_ASSIGNED_OBJECT_ID = "AssignedObjectId";
    private static final String PREF_ASSIGNED_OBJECT_TITLE = "AssignedObjectTitle";
    private static final String PREF_ASSIGNED_OBJECT_LATITUDE = "AssignedObjectLatitude";
    private static final String PREF_ASSIGNED_OBJECT_LONGITUDE = "AssignedObjectLongitude";
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
    @SuppressLint("CommitPrefEdits")
    public static void updateProfile(String token, String userId, User.AssignedObject assignedObject,
                                     String name, String email, String photo) {
        PreferenceManager.getDefaultSharedPreferences(MyApplication.getContext()).edit()
                .putString(PREF_TOKEN, token)
                .putString(PREF_USER_ID, userId)
                .putString(PREF_NAME, name)
                .putString(PREF_EMAIL, email)
                .putString(PREF_PHOTO, photo)
                .commit();
        if (assignedObject != null) {
            PreferenceManager.getDefaultSharedPreferences(MyApplication.getContext()).edit()
                    .putString(PREF_ASSIGNED_OBJECT_ID, assignedObject.getAssignedObjectId())
                    .putString(PREF_ASSIGNED_OBJECT_TITLE, assignedObject.getTitle())
                    .putString(PREF_ASSIGNED_OBJECT_LATITUDE, String.valueOf(assignedObject
                            .getLatitude()))
                    .putString(PREF_ASSIGNED_OBJECT_LONGITUDE, String.valueOf(assignedObject
                            .getLongitude()))
                    .commit();
        }
    }

    /**
     * Checks if user is logged in.
     */
    public static boolean isLoggedIn() {
        return !TextUtils.isEmpty(DeviceUtil.getToken());
    }

    /**
     * Retrieves token from preferences.
     */
    public static String getToken() {
        return PreferenceManager.getDefaultSharedPreferences(MyApplication.getContext())
                .getString(PREF_TOKEN, null);
    }

    /**
     * Retrieves user id from preferences.
     */
    public static String getUserId() {
        return PreferenceManager.getDefaultSharedPreferences(MyApplication.getContext())
                .getString(PREF_USER_ID, null);
    }

    /**
     * Checks if user is already assigned to the specific object to guard.
     */
    public static Boolean isAssigned() {
        return !TextUtils.isEmpty(PreferenceManager.getDefaultSharedPreferences
                (MyApplication.getContext()).getString(PREF_ASSIGNED_OBJECT_ID, null));
    }

    /**
     * Retrieves user's assigned object id from preferences.
     */
    public static String getGetAssignedObjectId() {
        return PreferenceManager.getDefaultSharedPreferences(MyApplication.getContext())
                .getString(PREF_ASSIGNED_OBJECT_ID, null);
    }

    /**
     * Retrieves user's assigned object title from preferences.
     */
    public static String getGetAssignedObjectTitle() {
        return PreferenceManager.getDefaultSharedPreferences(MyApplication.getContext())
                .getString(PREF_ASSIGNED_OBJECT_TITLE, null);
    }

    /**
     * Retrieves user's assigned object latitude from preferences.
     */
    public static Double getGetAssignedLatitude() {
        return Double.parseDouble(PreferenceManager.getDefaultSharedPreferences
                (MyApplication.getContext()).getString(PREF_ASSIGNED_OBJECT_LATITUDE, "-1"));
    }

    /**
     * Retrieves user's assigned object longitude from preferences.
     */
    public static Double getGetAssignedLongitude() {
        return Double.parseDouble(PreferenceManager.getDefaultSharedPreferences
                (MyApplication.getContext()).getString(PREF_ASSIGNED_OBJECT_LONGITUDE, "-1"));
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
