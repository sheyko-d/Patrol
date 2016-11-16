package ca.itquality.patrol.library.util;

import android.content.Context;
import android.content.res.Resources;
import android.text.format.DateUtils;
import android.util.DisplayMetrics;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

import ca.itquality.patrol.library.util.heartrate.DataValue;

/**
 * Helper class.
 */
public class Util {

    private static final String LOG_TAG = "StiggDebug";
    public static final String PATH_LOGGED_IN = "/logged_in";
    public static final String DATA_LOGGED_IN = "LoggedIn";
    public static final String PATH_HEART_RATE = "/heart_rate";
    public static final String DATA_HEART_RATE = "DataValue";
    public static final String PATH_HEART_RATE_HISTORY = "/heart_rate_history";
    public static final String DATA_HEART_RATE_VALUES = "HeartRateValues";
    public static final String PATH_ACTIVITY = "/activity";
    public static final String DATA_ACTIVITY = "Activity";
    public static final String PATH_STEPS = "/steps";
    public static final String DATA_STEPS = "Steps";
    public static final String PATH_NAME = "/name";
    public static final String DATA_NAME = "Name";
    public static final String PATH_LAST_MESSAGE = "/last_message";
    public static final String DATA_LAST_MESSAGE_TITLE = "LastMessageTitle";
    public static final String DATA_LAST_MESSAGE_TEXT = "LastMessageText";
    public static final String PATH_LOCATION = "/location";
    public static final String DATA_LOCATION = "Location";
    public static final String PATH_SHIFT = "/shift";
    public static final String DATA_SHIFT_TITLE = "ShiftTitle";
    public static final String DATA_SHIFT = "Shift";
    public static final String DATA_TIME = "Time";
    public static final String PATH_WEATHER = "/weather";
    public static final String DATA_ICON = "Icon";
    public static final String DATA_TEMPERATURE = "Temperature";
    public static final int NOTIFICATION_ID_BACKUP = 3;
    private static final int DAY_DURATION = 1000 * 60 * 60 * 24;
    public static final int NOTIFICATION_ID_CLOCK_IN = 4;
    public static final int NOTIFICATION_ID_STRETCH = 5;
    public static final int NOTIFICATION_ID_NOT_AT_WORK = 6;

    /**
     * Adds a message to LogCat.
     */
    public static void Log(Object text) {
        Log.d(LOG_TAG, text + "");
    }

    /**
     * Converts from DP (density-independent pixels) to regular pixels.
     */
    public static int convertDpToPixel(Context context, float dp) {
        Resources resources = context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        return (int) (dp * (metrics.densityDpi / 160f));
    }

    public static String parseFirstName(String name) {
        if (name.contains(" ")) {
            return name.substring(0, name.indexOf(" "));
        } else {
            return name;
        }
    }

    public static String formatTime(Context context, Long time) {
        if (System.currentTimeMillis() - time < DAY_DURATION) {
            return DateUtils.formatDateTime(context, time, DateUtils.FORMAT_ABBREV_ALL
                    | DateUtils.FORMAT_SHOW_TIME);
        } else {
            return DateUtils.formatDateTime(context, time, DateUtils.FORMAT_ABBREV_ALL
                    | DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_SHOW_DATE);
        }
    }

    /**
     * Parses a json array from an array list.
     */
    public static JSONArray parseJsonArray(ArrayList<DataValue> values) {
        JSONArray valuesJson = new JSONArray();
        for (DataValue heartRateValue : values) {
            try {
                valuesJson.put(new JSONObject()
                        .put("time", heartRateValue.getTime())
                        .put("value", heartRateValue.getValue())
                );
            } catch (Exception e) {
                Util.Log("Can't retrieve value: " + e);
            }
        }
        return valuesJson;
    }
}
