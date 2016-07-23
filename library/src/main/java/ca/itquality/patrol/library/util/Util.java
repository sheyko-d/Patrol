package ca.itquality.patrol.library.util;

import android.content.res.Resources;
import android.util.DisplayMetrics;
import android.util.Log;

import ca.itquality.patrol.library.util.app.MyApplication;

/**
 * Helper class.
 */
public class Util {

    private static final String LOG_TAG = "PatrolDebug";
    public static final String PATH_LOGGED_IN = "/logged_in";
    public static final String DATA_LOGGED_IN = "LoggedIn";
    public static final String PATH_HEART_RATE = "/heart_rate";
    public static final String DATA_HEART_RATE = "HeartRate";
    public static final String PATH_ACTIVITY = "/activity";
    public static final String DATA_ACTIVITY = "Activity";
    public static final String PATH_STEPS = "/steps";
    public static final String DATA_STEPS = "Steps";
    public static final String PATH_NAME = "/name";
    public static final String DATA_NAME = "Name";

    /**
     * Adds a message to LogCat.
     */
    public static void Log(Object text) {
        Log.d(LOG_TAG, text + "");
    }

    /**
     * Converts from DP (density-independent pixels) to regular pixels.
     */
    public static int convertDpToPixel(float dp) {
        Resources resources = MyApplication.getContext().getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        return (int) (dp * (metrics.densityDpi / 160f));
    }
}
