package ca.itquality.patrol.util;

import android.preference.PreferenceManager;

import java.text.SimpleDateFormat;
import java.util.Locale;

import ca.itquality.patrol.R;
import ca.itquality.patrol.library.util.app.MyApplication;

import static android.preference.PreferenceManager.getDefaultSharedPreferences;

/**
 * Helper class.
 */
public class WearUtil {

    public static final SimpleDateFormat AMBIENT_DATE_FORMAT =
            new SimpleDateFormat("HH:mm", Locale.US);
    private static final String PREF_ACTIVITY_STATUS = "ActivityStatus";
    private static final String PREF_LOGGED_IN = "LoggedIn";
    private static final String PREF_NAME = "Name";
    private static final String PREF_LAST_MESSAGE_TITLE = "LastMessageTitle";
    private static final String PREF_LAST_MESSAGE_TEXT = "LastMessageText";
    private static final String PREF_LOCATION = "Location";
    private static final String PREF_SHIFT_TITLE = "ShiftTitle";
    private static final String PREF_SHIFT_TEXT = "ShiftText";
    private static final String PREF_STEPS = "Steps";
    private static final String PREF_WEATHER_TEMPERATURE = "WeatherTemperature";
    private static final String PREF_WEATHER_ICON = "WeatherIcon";

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
                .getString(PREF_LAST_MESSAGE_TITLE, "—");
    }

    /**
     * Retrieves the text of the last message from preferences.
     */
    public static String getLastMessageText() {
        return PreferenceManager.getDefaultSharedPreferences(MyApplication.getContext())
                .getString(PREF_LAST_MESSAGE_TEXT, "—");
    }

    /**
     * Saves the location in preferences.
     */
    public static void setLocation(String location) {
        PreferenceManager.getDefaultSharedPreferences(MyApplication.getContext()).edit()
                .putString(PREF_LOCATION, location)
                .apply();
    }

    /**
     * Retrieves the location from preferences.
     */
    public static String getLocation() {
        return PreferenceManager.getDefaultSharedPreferences(MyApplication.getContext())
                .getString(PREF_LOCATION, "—");
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
     * Saves the steps in preferences.
     */
    public static void setSteps(int steps) {
        getDefaultSharedPreferences(MyApplication.getContext()).edit()
                .putInt(PREF_STEPS, steps)
                .apply();
    }

    /**
     * Retrieves the steps from preferences.
     */
    public static int getSteps() {
        return getDefaultSharedPreferences(MyApplication.getContext())
                .getInt(PREF_STEPS, 0);
    }

    /**
     * Saves the weather temperature in preferences.
     */
    public static void setWeatherTemperature(int temperature) {
        getDefaultSharedPreferences(MyApplication.getContext()).edit()
                .putInt(PREF_WEATHER_TEMPERATURE, temperature)
                .apply();
    }

    /**
     * Retrieves the weather temperature from preferences.
     */
    public static int getWeatherTemperature() {
        return getDefaultSharedPreferences(MyApplication.getContext())
                .getInt(PREF_WEATHER_TEMPERATURE, 0);
    }

    /**
     * Saves the weather icon in preferences.
     *
     * @param icon
     */
    public static void setWeatherIcon(String icon) {
        getDefaultSharedPreferences(MyApplication.getContext()).edit()
                .putString(PREF_WEATHER_ICON, icon)
                .apply();
    }

    /**
     * Retrieves the weather icon from preferences.
     */
    public static String getWeatherIcon() {
        return getDefaultSharedPreferences(MyApplication.getContext())
                .getString(PREF_WEATHER_ICON, null);
    }

    public static String getInitials(String name) {
        try {
            String[] nameParts = name.split(" ");
            return nameParts[0].substring(0, 1) + nameParts[1].substring(0, 1);
        } catch (Exception e) {
            try {
                return name.substring(0, 2).toUpperCase(Locale.getDefault());
            } catch (Exception e2) {
                return "-";
            }
        }
    }
}
