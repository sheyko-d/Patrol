package ca.itquality.patrol.service;

import android.app.IntentService;
import android.app.Notification;
import android.content.ContentValues;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.NotificationCompat;
import android.text.TextUtils;

import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;

import java.util.List;

import ca.itquality.patrol.R;
import ca.itquality.patrol.app.MyApplication;
import ca.itquality.patrol.library.util.Util;
import ca.itquality.patrol.util.DatabaseManager;
import ca.itquality.patrol.util.DeviceUtil;


public class ActivityRecognizedService extends IntentService {

    public static final String ACTIVITY_UPDATE_INTENT = "ca.itquality.patrol.ACTIVITY_UPDATE";
    public static final String ACTIVITY_EXTRA = "Activity";
    private static long sLastActivityTime = System.currentTimeMillis();
    private boolean mRunning = false;

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

        String mActivity = null;
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
                    showRunningNotification();
                    break;
                }
                case DetectedActivity.STILL: {
                    activityName = "Still";
                    break;
                }
            }

            mActivity = activityName;
        }

        if (!TextUtils.isEmpty(mActivity) && !mActivity.equals("Still")) {
            sLastActivityTime = System.currentTimeMillis();
        }

        if (!TextUtils.isEmpty(mActivity) && mActivity.equals("Running")) {
            if (!mRunning) {
                showRunningNotification();
                mRunning = true;
            }
        } else {
            mRunning = false;
        }

        Util.Log("activity changed: " + activityName + " " + System.currentTimeMillis());
        if (!TextUtils.isEmpty(activityName)) {
            sendBroadcast(new Intent(ACTIVITY_UPDATE_INTENT).putExtra(ACTIVITY_EXTRA,
                    activityName));

            if (DeviceUtil.getActivity() == null
                    || !DeviceUtil.getActivity().equals(activityName)) {
                storeActivityInDb(activityName);
            }
            DeviceUtil.setActivity(activityName);

            BackgroundService.updateWearActivityStatus(activityName);
        }
    }

    private void showRunningNotification() {
        final NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder
                (MyApplication.getContext());
        notificationBuilder.setContentTitle("Activity notification");
        notificationBuilder.setContentText("You just did a short run.");
        notificationBuilder.setAutoCancel(true);
        notificationBuilder.setSmallIcon(R.drawable.backup_notification);
        notificationBuilder.setColor(ContextCompat.getColor(MyApplication.getContext(),
                R.color.colorPrimary));
        notificationBuilder.setPriority(NotificationCompat.PRIORITY_MAX);

        notificationBuilder.setVibrate(new long[]{200});
        Notification notification = notificationBuilder.build();
        notification.defaults |= Notification.DEFAULT_SOUND;
        final NotificationManagerCompat notificationManager = NotificationManagerCompat.from
                (MyApplication.getContext());
        notificationManager.notify(Util.NOTIFICATION_ID_STRETCH, notification);
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

    public static long getLastStillTime() {
        return sLastActivityTime;
    }

    public static void resetLastActivityTime() {
        sLastActivityTime = System.currentTimeMillis();
    }
}