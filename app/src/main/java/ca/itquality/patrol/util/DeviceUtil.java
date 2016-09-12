package ca.itquality.patrol.util;

import android.preference.PreferenceManager;
import android.text.TextUtils;

import com.google.android.gms.maps.model.LatLng;
import com.google.gson.Gson;

import java.util.ArrayList;

import ca.itquality.patrol.R;
import ca.itquality.patrol.app.MyApplication;
import ca.itquality.patrol.library.util.Util;
import ca.itquality.patrol.library.util.auth.data.User;

import static android.preference.PreferenceManager.getDefaultSharedPreferences;


public class DeviceUtil {

    public static final int MIN_PASSWORD_LENGTH = 6;
    private static final String PREF_ACTIVITY = "Activity";
    private static final String PREF_USER = "User";
    private static final String PREF_HEART_RATE = "DataValue";
    private static final String PREF_STEPS = "Steps";
    private static final String PREF_MY_LATITUDE = "Latitude";
    private static final String PREF_MY_LONGITUDE = "Longitude";
    private static final String PREF_CHAT_LAST_SEEN = "ChatLastSeen";
    private static final String PREF_LAST_MESSAGE_TITLE = "LastMessageTitle";
    private static final String PREF_LAST_MESSAGE_TEXT = "LastMessageText";
    private static final String PREF_QR = "QR";
    private static final String PREF_SHIFT_TITLE = "ShiftTitle";
    private static final String PREF_SHIFT_TEXT = "ShiftText";
    private static final String PREF_ADDRESS = "Address";
    private static final String PREF_ASK_BACKUP = "AskBackup";

    public static final int MAP_PADDING = Util.convertDpToPixel(MyApplication.getContext(), 64);
    public static final String HELP_URL = "http://www.stigg.ca/contactUs.html";

    public static boolean isValidEmail(String email) {
        return !TextUtils.isEmpty(email) && android.util.Patterns.EMAIL_ADDRESS.matcher(email)
                .matches();
    }

    /**
     * Save user login data in preferences.
     */
    public static void updateProfile(User user) {
        String json = new Gson().toJson(user);

        PreferenceManager.getDefaultSharedPreferences(MyApplication.getContext()).edit()
                .putString(PREF_USER, json).apply();
    }

    /**
     * Checks if user is logged in.
     */
    public static boolean isLoggedIn() {
        return getUser() != null;
    }

    /**
     * Retrieves current user from preferences.
     */
    public static User getUser() {
        String user = getDefaultSharedPreferences(MyApplication.getContext())
                .getString(PREF_USER, null);
        return new Gson().fromJson(user, User.class);
    }

    /**
     * Retrieves token from preferences.
     */
    public static String getToken() {
        return getUser() != null ? getUser().getToken() : null;
    }

    /**
     * Retrieves user id from preferences.
     */
    public static String getUserId() {
        return getUser() != null ? getUser().getUserId() : null;
    }

    /**
     * Checks if user is already assigned to the specific object to guard.
     */
    public static Boolean isAssigned() {
        return getUser() != null && getUser().getAssignedObject() != null;
    }

    /**
     * Retrieves user's assigned object id from preferences.
     */
    public static String getAssignedObjectId() {
        return getUser() != null ? getUser().getAssignedObject().getAssignedObjectId() : null;
    }

    /**
     * Retrieves user's assigned object title from preferences.
     */
    public static String getAssignedObjectTitle() {
        return getUser() != null ? getUser().getAssignedObject().getTitle() : null;
    }

    /**
     * Retrieves user full name from preferences.
     */
    public static String getName() {
        return getUser() != null ? getUser().getName() : null;
    }

    /**
     * Retrieves user photo from preferences.
     */
    public static String getPhoto() {
        return getUser() != null ? getUser().getPhoto() : null;
    }

    /**
     * Saves the activity status in preferences.
     */
    public static void setActivity(String activity) {
        getDefaultSharedPreferences(MyApplication.getContext()).edit()
                .putString(PREF_ACTIVITY, activity).apply();
    }

    /**
     * Retrieves the activity status from preferences.
     */
    public static String getActivity() {
        return getDefaultSharedPreferences(MyApplication.getContext())
                .getString(PREF_ACTIVITY, null);
    }

    /**
     * Saves the heart rate in preferences.
     */
    public static void setHeartRate(int heartRate) {
        getDefaultSharedPreferences(MyApplication.getContext()).edit()
                .putInt(PREF_HEART_RATE, heartRate).apply();
    }

    /**
     * Retrieves the activity status from preferences.
     */
    public static int getHeartRate() {
        return getDefaultSharedPreferences(MyApplication.getContext())
                .getInt(PREF_HEART_RATE, -1);
    }

    /**
     * Saves the steps count in preferences.
     */
    public static void setSteps(int steps) {
        getDefaultSharedPreferences(MyApplication.getContext()).edit()
                .putInt(PREF_STEPS, steps).apply();
    }

    /**
     * Retrieves the steps count from preferences.
     */
    public static int getSteps() {
        return getDefaultSharedPreferences(MyApplication.getContext())
                .getInt(PREF_STEPS, -1);
    }

    public static void updateAssignedObject(User.AssignedObject assignedObject) {
        if (getUser() != null) {
            updateProfile(getUser().setAssignedObject(assignedObject));
        }
    }

    /**
     * Saves my location in preferences.
     */
    public static void setMyLocation(float latitude, float longitude) {
        getDefaultSharedPreferences(MyApplication.getContext()).edit()
                .putFloat(PREF_MY_LATITUDE, latitude).putFloat(PREF_MY_LONGITUDE, longitude)
                .apply();
    }

    /**
     * Retrieves my location from preferences.
     */
    public static LatLng getMyLocation() {
        return new LatLng(getDefaultSharedPreferences(MyApplication.getContext())
                .getFloat(PREF_MY_LATITUDE, 0), getDefaultSharedPreferences
                (MyApplication.getContext()).getFloat(PREF_MY_LONGITUDE, 0));
    }

    /**
     * Saves the time when chat screen was last opened in preferences.
     */
    public static void updateChatLastSeenTime() {
        getDefaultSharedPreferences(MyApplication.getContext()).edit()
                .putLong(PREF_CHAT_LAST_SEEN, System.currentTimeMillis()).apply();
    }

    /**
     * Retrieves the time when chat screen was last opened from preferences.
     */
    public static Long getChatLastSeenTime() {
        return getDefaultSharedPreferences(MyApplication.getContext())
                .getLong(PREF_CHAT_LAST_SEEN, System.currentTimeMillis());
    }

    /**
     * Saves the title and text of the last message in preferences.
     */
    public static void setLastMessage(String title, String text) {
        getDefaultSharedPreferences(MyApplication.getContext()).edit()
                .putString(PREF_LAST_MESSAGE_TITLE, title)
                .putString(PREF_LAST_MESSAGE_TEXT, text)
                .apply();
    }

    /**
     * Retrieves the title of the last message from preferences.
     */
    public static String getLastMessageTitle() {
        return getDefaultSharedPreferences(MyApplication.getContext())
                .getString(PREF_LAST_MESSAGE_TITLE, MyApplication.getContext()
                        .getString(R.string.main_messages_title_placeholder));
    }

    /**
     * Retrieves the text of the last message from preferences.
     */
    public static String getLastMessageText() {
        return getDefaultSharedPreferences(MyApplication.getContext())
                .getString(PREF_LAST_MESSAGE_TEXT, MyApplication.getContext()
                        .getString(R.string.main_value_placeholder));
    }

    public static void setQr(String qr) {
        getDefaultSharedPreferences(MyApplication.getContext()).edit()
                .putString(PREF_QR, qr)
                .apply();
    }

    public static String getQr() {
        return getDefaultSharedPreferences(MyApplication.getContext())
                .getString(PREF_QR, null);
    }

    public static User.AssignedShift getCurrentShift(long timeSinceWeekStart) {
        ArrayList<User.AssignedShift> assignedShifts = DeviceUtil.getUser().getAssignedShifts();
        User.AssignedShift currentShift = null;
        if (assignedShifts != null) {
            for (User.AssignedShift assignedShift : assignedShifts) {
                if (timeSinceWeekStart > assignedShift.getStartTime()
                        && timeSinceWeekStart < assignedShift.getEndTime()) {
                    currentShift = assignedShift;
                }
            }
        }
        return currentShift;
    }

    public static User.AssignedShift getNextShift(long timeSinceWeekStart) {
        ArrayList<User.AssignedShift> assignedShifts = DeviceUtil.getUser().getAssignedShifts();
        User.AssignedShift nextShift = null;
        if (assignedShifts != null) {
            for (User.AssignedShift assignedShift : assignedShifts) {
                if (timeSinceWeekStart < assignedShift.getStartTime()) {
                    nextShift = assignedShift;
                }
            }
        }
        return nextShift;
    }

    public static User.AssignedShift getNextWeekShift() {
        // TODO: Sort shifts by time on server side
        ArrayList<User.AssignedShift> assignedShifts = DeviceUtil.getUser().getAssignedShifts();
        User.AssignedShift nextShift = null;
        if (assignedShifts != null) {
            nextShift = assignedShifts.get(0);
        }
        return nextShift;
    }

    public static User.AssignedShift getPreviousShift(long timeSinceWeekStart) {
        ArrayList<User.AssignedShift> assignedShifts = DeviceUtil.getUser().getAssignedShifts();
        User.AssignedShift previousShift = null;
        if (assignedShifts != null) {
            for (User.AssignedShift assignedShift : assignedShifts) {
                if (timeSinceWeekStart > assignedShift.getEndTime()) {
                    previousShift = assignedShift;
                    break;
                }
            }
        }
        return previousShift;
    }

    /**
     * Saves the shift in preferences.
     */
    public static void setShift(String shift, String shiftTitle) {
        getDefaultSharedPreferences(MyApplication.getContext()).edit()
                .putString(PREF_SHIFT_TITLE, shift)
                .putString(PREF_SHIFT_TEXT, shiftTitle)
                .apply();
    }

    /**
     * Retrieves the shift title from preferences.
     */
    public static String getShiftTitle() {
        return getDefaultSharedPreferences(MyApplication.getContext())
                .getString(PREF_SHIFT_TITLE, MyApplication.getContext()
                        .getString(R.string.main_shift_title_placeholder));
    }

    /**
     * Retrieves the shift text from preferences.
     */
    public static String getShift() {
        return getDefaultSharedPreferences(MyApplication.getContext())
                .getString(PREF_SHIFT_TEXT, MyApplication.getContext()
                        .getString(R.string.main_shift_placeholder));
    }

    /**
     * Saves the address in preferences.
     */
    public static void setAddress(String address) {
        getDefaultSharedPreferences(MyApplication.getContext()).edit()
                .putString(PREF_ADDRESS, address)
                .apply();
    }

    /**
     * Retrieves the address from preferences.
     */
    public static String getAddress() {
        return getDefaultSharedPreferences(MyApplication.getContext())
                .getString(PREF_ADDRESS, MyApplication.getContext()
                        .getString(R.string.main_shift_placeholder));
    }

    /**
     * Saves the backup notification enabled state in preferences.
     */
    public static void setAskBackup(boolean ask) {
        getDefaultSharedPreferences(MyApplication.getContext()).edit()
                .putBoolean(PREF_ASK_BACKUP, ask)
                .apply();
    }

    /**
     * Retrieves the backup notification enabled state from preferences.
     */
    public static boolean askBackup() {
        return getDefaultSharedPreferences(MyApplication.getContext())
                .getBoolean(PREF_ASK_BACKUP, true);
    }

    /**
     * Saves the clock in notification shown state in preferences.
     */
    public static void setClockInShown(long weekStartTime, User.AssignedShift currentShift) {
        getDefaultSharedPreferences(MyApplication.getContext()).edit()
                .putBoolean("ClockIn" + weekStartTime + currentShift.getAssignedShiftId(), true)
                .apply();
    }

    /**
     * Retrieves the clock in notification shown state from preferences.
     */
    public static boolean clockInShown(long weekStartTime, User.AssignedShift currentShift) {
        return getDefaultSharedPreferences(MyApplication.getContext())
                .getBoolean("ClockIn" + weekStartTime + currentShift.getAssignedShiftId(), false);
    }

    /**
     * Saves the shift start confirmation state in preferences.
     */
    public static void setStartConfirmed(long weekStartTime, User.AssignedShift currentShift) {
        getDefaultSharedPreferences(MyApplication.getContext()).edit()
                .putBoolean("StartConfirmed" + weekStartTime + currentShift.getAssignedShiftId(),
                        true)
                .apply();
    }

    /**
     * Retrieves the shift start confirmation state from preferences.
     */
    public static boolean startConfirmed(long weekStartTime, User.AssignedShift currentShift) {
        return getDefaultSharedPreferences(MyApplication.getContext())
                .getBoolean("StartConfirmed" + weekStartTime + currentShift.getAssignedShiftId(),
                        false);
    }

    /**
     * Saves the shift end confirmation state in preferences.
     */
    public static void setEndConfirmed(long weekStartTime, User.AssignedShift currentShift) {
        getDefaultSharedPreferences(MyApplication.getContext()).edit()
                .putBoolean("EndConfirmed" + weekStartTime + currentShift.getAssignedShiftId(),
                        true)
                .apply();
    }

    /**
     * Retrieves the shift end confirmation state from preferences.
     */
    public static boolean endConfirmed(long weekStartTime, User.AssignedShift currentShift) {
        return getDefaultSharedPreferences(MyApplication.getContext())
                .getBoolean("EndConfirmed" + weekStartTime + currentShift.getAssignedShiftId(),
                        false);
    }
}
