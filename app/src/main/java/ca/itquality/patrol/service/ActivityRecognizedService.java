package ca.itquality.patrol.service;

import android.app.IntentService;
import android.content.Intent;

import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;

import java.util.List;

import ca.itquality.patrol.library.util.Util;

public class ActivityRecognizedService extends IntentService {

    public static final String INTENT_ACTIVITY_UPDATE = "ca.itquality.patrol.ACTIVITY_UPDATE";
    public static final String EXTRA_ACTIVITY = "Activity";

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

    private void handleDetectedActivities(List<DetectedActivity> probableActivities) {
        String activityName = null;
        for (DetectedActivity activity : probableActivities) {
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
                    activityName = "In Vehicle";
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
                case DetectedActivity.TILTING: {
                    activityName = "Tilting";
                    break;
                }
            }
        }
        sendBroadcast(new Intent(INTENT_ACTIVITY_UPDATE).putExtra(EXTRA_ACTIVITY,
                activityName));

        Util.Log("activityName = "+activityName);
    }
}