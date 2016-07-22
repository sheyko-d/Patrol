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
    public static final String LOGGED_IN_PATH = "/logged_in";
    public static final String LOGGED_IN_DATA = "LoggedIn";
    public static final String HEART_RATE_PATH = "/heart_rate";
    public static final String HEART_RATE_DATA = "HeartRate";

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
