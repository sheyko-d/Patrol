package ca.itquality.patrol.util;

import android.annotation.SuppressLint;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import com.google.android.gms.maps.model.LatLng;

import ca.itquality.patrol.R;
import ca.itquality.patrol.app.MyApplication;
import ca.itquality.patrol.auth.data.User;
import ca.itquality.patrol.library.util.Util;


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
    private static final String PREF_MY_LATITUDE = "Latitude";
    private static final String PREF_MY_LONGITUDE = "Longitude";
    private static final String PREF_CHAT_LAST_SEEN = "ChatLastSeen";
    private static final String PREF_LAST_MESSAGE_TITLE = "LastMessageTitle";
    private static final String PREF_LAST_MESSAGE_TEXT = "LastMessageText";
    private static final String PREF_ORIGIN_FLOOR = "OriginFloor";
    private static final String PREF_ORIGIN_PRESSURE = "OriginPressure";
    public static final int MAP_PADDING = Util.convertDpToPixel(MyApplication.getContext(), 64);

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
        } else {
            PreferenceManager.getDefaultSharedPreferences(MyApplication.getContext()).edit()
                    .remove(PREF_ASSIGNED_OBJECT_ID)
                    .remove(PREF_ASSIGNED_OBJECT_TITLE)
                    .remove(PREF_ASSIGNED_OBJECT_LATITUDE)
                    .remove(PREF_ASSIGNED_OBJECT_LONGITUDE)
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
     * Retrieves the activity status from preferences.
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
     * Retrieves the activity status from preferences.
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
     * Retrieves the steps count from preferences.
     */
    public static int getSteps() {
        return PreferenceManager.getDefaultSharedPreferences(MyApplication.getContext())
                .getInt(PREF_STEPS, -1);
    }

    public static void updateAssignedObject(User.AssignedObject assignedObject) {
        PreferenceManager.getDefaultSharedPreferences(MyApplication.getContext()).edit()
                .putString(PREF_ASSIGNED_OBJECT_ID, assignedObject.getAssignedObjectId())
                .putString(PREF_ASSIGNED_OBJECT_TITLE, assignedObject.getTitle())
                .putString(PREF_ASSIGNED_OBJECT_LATITUDE, String.valueOf(assignedObject
                        .getLatitude()))
                .putString(PREF_ASSIGNED_OBJECT_LONGITUDE, String.valueOf(assignedObject
                        .getLongitude()))
                .apply();
    }

    /**
     * Saves my location in preferences.
     */
    public static void setMyLocation(float latitude, float longitude) {
        PreferenceManager.getDefaultSharedPreferences(MyApplication.getContext()).edit()
                .putFloat(PREF_MY_LATITUDE, latitude).putFloat(PREF_MY_LONGITUDE, longitude)
                .apply();
    }

    /**
     * Retrieves my location from preferences.
     */
    public static LatLng getMyLocation() {
        return new LatLng(PreferenceManager.getDefaultSharedPreferences(MyApplication.getContext())
                .getFloat(PREF_MY_LATITUDE, 0), PreferenceManager.getDefaultSharedPreferences
                (MyApplication.getContext()).getFloat(PREF_MY_LONGITUDE, 0));
    }

    /**
     * Saves the time when chat screen was last opened in preferences.
     */
    public static void updateChatLastSeenTime() {
        PreferenceManager.getDefaultSharedPreferences(MyApplication.getContext()).edit()
                .putLong(PREF_CHAT_LAST_SEEN, System.currentTimeMillis()).apply();
    }

    /**
     * Retrieves the time when chat screen was last opened from preferences.
     */
    public static Long getChatLastSeenTime() {
        return PreferenceManager.getDefaultSharedPreferences(MyApplication.getContext())
                .getLong(PREF_CHAT_LAST_SEEN, System.currentTimeMillis());
    }

    /**
     * Saves the title and text of the last message in preferences.
     */
    public static void setLastMessage(String title, String text) {
        PreferenceManager.getDefaultSharedPreferences(MyApplication.getContext()).edit()
                .putString(PREF_LAST_MESSAGE_TITLE, title)
                .putString(PREF_LAST_MESSAGE_TEXT, text)
                .apply();
    }

    /**
     * Retrieves the title of the last message from preferences.
     */
    public static String getLastMessageTitle() {
        return PreferenceManager.getDefaultSharedPreferences(MyApplication.getContext())
                .getString(PREF_LAST_MESSAGE_TITLE, MyApplication.getContext()
                        .getString(R.string.main_messages_title_placeholder));
    }

    /**
     * Retrieves the text of the last message from preferences.
     */
    public static String getLastMessageText() {
        return PreferenceManager.getDefaultSharedPreferences(MyApplication.getContext())
                .getString(PREF_LAST_MESSAGE_TEXT, MyApplication.getContext()
                        .getString(R.string.main_value_placeholder));
    }

    /**
     * Saves the floor and pressure when user calibrated the barometer to preferences.
     */
    public static void setOriginFloor(int floor, int pressure) {
        PreferenceManager.getDefaultSharedPreferences(MyApplication.getContext()).edit()
                .putInt(PREF_ORIGIN_FLOOR, floor)
                .putInt(PREF_ORIGIN_PRESSURE, pressure).apply();
    }

    /**
     * Retrieves the floor user was on when he calibrated the barometer from preferences.
     */
    public static int getOriginFloor() {
        return PreferenceManager.getDefaultSharedPreferences(MyApplication.getContext())
                .getInt(PREF_ORIGIN_FLOOR, -2);
    }

    /**
     * Retrieves the pressure when user calibrated the barometer from preferences.
     */
    public static Integer getOriginPressure() {
        return PreferenceManager.getDefaultSharedPreferences(MyApplication.getContext())
                .getInt(PREF_ORIGIN_PRESSURE, -1);
    }
}
