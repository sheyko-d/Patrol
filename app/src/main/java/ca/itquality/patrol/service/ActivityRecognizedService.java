package ca.itquality.patrol.service;

import android.app.IntentService;
import android.content.Intent;
import android.text.TextUtils;

import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;

import java.util.List;


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

        if (!TextUtils.isEmpty(activityName)) {
            sendBroadcast(new Intent(ACTIVITY_UPDATE_INTENT).putExtra(ACTIVITY_EXTRA,
                    activityName));

            BackgroundService.updateWearActivityStatus(activityName);
        }
    }
}