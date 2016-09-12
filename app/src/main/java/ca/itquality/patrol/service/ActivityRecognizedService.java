package ca.itquality.patrol.service;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;

import java.util.List;

import ca.itquality.patrol.app.MyApplication;
import ca.itquality.patrol.library.util.Util;
import ca.itquality.patrol.util.DatabaseManager;
import ca.itquality.patrol.util.DeviceUtil;


public class ActivityRecognizedService extends IntentService {

    public static final String ACTIVITY_UPDATE_INTENT = "ca.itquality.patrol.ACTIVITY_UPDATE";
    public static final String ACTIVITY_EXTRA = "Activity";

    public ActivityRecognizedService() {
        super("ActivityRecognizedService");
    }

    public ActivityRecognizedService(String name) {
        super(name);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (ActivityRecognitionResult.hasResult(intent)) {
            ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);
            handleDetectedActivities(result.getProbableActivities());
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    private void handleDetectedActivities(List<DetectedActivity> probableActivities) {
        String activityName = null;
        int maxConfidence = 0;
        for (DetectedActivity activity : probableActivities) {
            if (activity.getConfidence() <= maxConfidence) continue;

            maxConfidence = activity.getConfidence();

            switch (activity.getType()) {
                case DetectedActivity.IN_VEHICLE: {
                    activityName = "In Vehicle";
                    break;
                }
                case DetectedActivity.ON_BICYCLE: {
                    activityName = "On Bicycle";
                    break;
                }
                case DetectedActivity.ON_FOOT: {
                    activityName = "On Foot";
                    break;
                }
                case DetectedActivity.RUNNING: {
                    activityName = "Running";
                    break;
                }
                case DetectedActivity.STILL: {
                    activityName = "Still";
                    break;
                }
            }
        }

        Util.Log("activity changed: "+activityName+" "+System.currentTimeMillis());
        if (!TextUtils.isEmpty(activityName)) {
            sendBroadcast(new Intent(ACTIVITY_UPDATE_INTENT).putExtra(ACTIVITY_EXTRA,
                    activityName));

            Util.Log("store activity: "+DeviceUtil.getActivity()+" equals "+activityName);
            if (DeviceUtil.getActivity() == null
                    || !DeviceUtil.getActivity().equals(activityName)) {
                Util.Log("store activity: "+activityName);
                storeActivityInDb(activityName);
            }
            DeviceUtil.setActivity(activityName);

            BackgroundService.updateWearActivityStatus(activityName);
        }
    }

    private void storeActivityInDb(String activity) {
        DatabaseManager.initializeInstance(new DatabaseManager.SitesDatabaseHelper
                (MyApplication.getContext()));
        DatabaseManager databaseManager = DatabaseManager.getInstance();
        SQLiteDatabase database = databaseManager.openDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(DatabaseManager.ACTIVITY_VALUE_COLUMN, activity);
        contentValues.put(DatabaseManager.ACTIVITY_TIME_COLUMN, System.currentTimeMillis());
        contentValues.put(DatabaseManager.ACTIVITY_IS_SENT_COLUMN, false);
        database.insert(DatabaseManager.ACTIVITY_TABLE, null, contentValues);
        databaseManager.closeDatabase();
    }
}