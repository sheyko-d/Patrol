package ca.itquality.patrol.service;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Vibrator;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.NotificationCompat;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import ca.itquality.patrol.R;
import ca.itquality.patrol.library.util.Util;

public class SensorsService extends Service implements GoogleApiClient.ConnectionCallbacks {

    // Constants
    private static final int NOTIFICATION_ID_BACKUP = 0;
    private static final int HEART_RATE_MEASURE_INTERVAL = 1000;
    private static final int BACKUP_DISMISS_DURATION = 10;
    private static final int SECOND_DURATION = 1000;
    private static final int HEART_RATE_MAX_BACKUP = 120;
    private static final int HEART_RATE_MIN_SLEEP = 60;
    public static final String INTENT_HEART_RATE = "ca.itquality.patrol.HEART_RATE";
    public static final String EXTRA_HEART_RATE = "HeartRate";
    public static final String INTENT_STEPS = "ca.itquality.patrol.STEPS";
    public static final String EXTRA_STEPS = "Steps";

    // Usual variables
    private GoogleApiClient mGoogleApiClient;
    private SensorManager mSensorManager;
    private boolean mBackupAsked = false;
    private Handler mHandler = new Handler();
    private boolean mSleepStarted = false;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        initGoogleClient();
    }

    private void initGoogleClient() {
        //Connect the GoogleApiClient
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .build();
        mGoogleApiClient.connect();
    }

    private void startListening() {
        mSensorManager = ((SensorManager) getSystemService(SENSOR_SERVICE));

        getHeartRateSensorData();
        getStepsSensorData();
    }

    private void getHeartRateSensorData() {
        final Sensor heartRateSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE);
        mSensorManager.registerListener(new SensorEventListener() {
            int mDismissRemainingSec;
            private Handler mHandler = new Handler();

            @Override
            public void onSensorChanged(SensorEvent event) {
                if (event.values[0] != 0) {
                    int heartRate = (int) event.values[0];
                    Util.Log("heart rate changed: " + heartRate);
                    //TODO:mAdapter.updateHeartRate((int) event.values[0]);
                    sendBroadcast(new Intent(INTENT_HEART_RATE).putExtra(EXTRA_HEART_RATE,
                            heartRate));
                    mSensorManager.unregisterListener(this, heartRateSensor);
                    mHandler.postDelayed(mMeasureHeartRateRunnable, HEART_RATE_MEASURE_INTERVAL);
                    updateHeartRateOnDevice(heartRate);
                    checkHeartRate(heartRate);
                }
            }

            // Checks if the guard's heart rate is within the normal range.
            // If it's too high, then call the backup.
            private void checkHeartRate(int heartRate) {
                if (!mBackupAsked && heartRate > HEART_RATE_MAX_BACKUP) {
                    mBackupAsked = true;
                    askForBackup();
                } else {
                    mBackupAsked = false;
                    dismissBackup();
                }

                if (!mSleepStarted && heartRate < HEART_RATE_MIN_SLEEP) {
                    mSleepStarted = true;
                } else {
                    mSleepStarted = false;
                }
            }

            private void dismissBackup() {
                // TODO: Hide notification

                // Cancel the dismiss the timer
                mHandler.removeCallbacks(mBackupDismissRunnable);
            }

            private void askForBackup() {
                // Vibrate for 0.5 sec
                ((Vibrator) getSystemService(VIBRATOR_SERVICE)).vibrate(1000);

                mDismissRemainingSec = BACKUP_DISMISS_DURATION;
                mBackupDismissRunnable.run();

                showBackupNotification();
            }

            Runnable mBackupDismissRunnable = new Runnable() {
                @Override
                public void run() {
                    if (mDismissRemainingSec > 0) {
                        // TODO: Update notification content with counter
                        // mBackupDismissTxt.setText(getString(R.string.main_backup_dismiss,
                        //        mDismissRemainingSec));
                        mDismissRemainingSec--;
                        mHandler.postDelayed(this, SECOND_DURATION);
                    } else {
                        dismissBackup();
                    }
                }
            };

            private void updateHeartRateOnDevice(int heartRate) {
                PutDataMapRequest putDataMapReq = PutDataMapRequest.create(Util.PATH_HEART_RATE);
                putDataMapReq.setUrgent();
                putDataMapReq.getDataMap().putInt(Util.DATA_HEART_RATE, heartRate);
                PutDataRequest putDataReq = putDataMapReq.asPutDataRequest();
                Wearable.DataApi.putDataItem(mGoogleApiClient, putDataReq);
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
            }
        }, heartRateSensor, SensorManager.SENSOR_DELAY_FASTEST);
    }

    private void getStepsSensorData() {
        Sensor stepCounterSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        mSensorManager.registerListener(new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                int steps = (int) event.values[0];
                updateStepsOnDevice(steps);
                //TODO: mAdapter.updateStepsCount(steps);
            }

            private void updateStepsOnDevice(int steps) {
                PutDataMapRequest putDataMapReq = PutDataMapRequest.create(Util.PATH_STEPS);
                putDataMapReq.setUrgent();
                putDataMapReq.getDataMap().putInt(Util.DATA_STEPS, steps);
                PutDataRequest putDataReq = putDataMapReq.asPutDataRequest();
                Wearable.DataApi.putDataItem(mGoogleApiClient, putDataReq);
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
            }
        }, stepCounterSensor, SensorManager.SENSOR_DELAY_FASTEST);
    }

    private Runnable mMeasureHeartRateRunnable = new Runnable() {
        @Override
        public void run() {
            getHeartRateSensorData();
        }
    };

    private void showBackupNotification() {
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder
                (this);
        notificationBuilder.setContentTitle("Do you need backup?");
        notificationBuilder.setAutoCancel(true);
        notificationBuilder.setSmallIcon(R.drawable.backup_notification);
        notificationBuilder.setColor(ContextCompat.getColor(this,
                R.color.colorPrimary));
        notificationBuilder.setPriority(NotificationCompat.PRIORITY_HIGH);

        Notification notification = notificationBuilder.build();
        notification.defaults |= Notification.DEFAULT_VIBRATE;
        notification.defaults |= Notification.DEFAULT_SOUND;
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from
                (getApplicationContext());
        notificationManager.notify(NOTIFICATION_ID_BACKUP, notification);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mHandler.removeCallbacks(mMeasureHeartRateRunnable);
        mGoogleApiClient.disconnect();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        startListening();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }
}
