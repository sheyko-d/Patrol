package ca.itquality.patrol.service;

import android.app.Notification;
import android.app.Service;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.NotificationCompat;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

import ca.itquality.patrol.R;
import ca.itquality.patrol.library.util.Util;
import ca.itquality.patrol.library.util.app.MyApplication;
import ca.itquality.patrol.library.util.heartrate.HeartRate;
import ca.itquality.patrol.util.DatabaseManager;

public class SensorsService extends Service implements GoogleApiClient.ConnectionCallbacks {

    // Constants
    private static final int NOTIFICATION_ID_BACKUP = 0;
    private static final int HEART_RATE_MEASURE_INTERVAL = 1000;//
    private static final int HEART_RATE_MIN_UPLOAD_COUNT = 6;
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
        Util.Log("Start listening watch service");

        mSensorManager = ((SensorManager) getSystemService(SENSOR_SERVICE));

        setHeartRateListener();
        setStepsListener();
    }

    private void setStepsListener() {
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

    private void setHeartRateListener() {
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

                    // Store a new heart rate reading in the database
                    storeHeartRateInDb(System.currentTimeMillis(), heartRate);

                    // If there are enough values in the database, then upload the batch to server
                    uploadHeartRateHistoryToDevice();
                }
            }

            private void uploadHeartRateHistoryToDevice() {
                DatabaseManager.initializeInstance(new DatabaseManager.SitesDatabaseHelper
                        (MyApplication.getContext()));
                final SQLiteDatabase database = DatabaseManager.getInstance().openDatabase();
                Cursor cursor = database.query(DatabaseManager.HEART_RATE_TABLE, new String[]{
                                DatabaseManager.HEART_RATE_TIME_COLUMN,
                                DatabaseManager.HEART_RATE_VALUE_COLUMN},
                        DatabaseManager.HEART_RATE_IS_SENT_COLUMN + "=?", new String[]{"0"},
                        null, null, DatabaseManager.HEART_RATE_TIME_COLUMN + " ASC");
                if (cursor.getCount() >= HEART_RATE_MIN_UPLOAD_COUNT) {
                    ArrayList<HeartRate> heartRateValues = new ArrayList<>();
                    while (cursor.moveToNext()) {
                        heartRateValues.add(new HeartRate(cursor.getLong(0), cursor.getInt(1)));
                    }

                    PutDataMapRequest putDataMapReq = PutDataMapRequest.create
                            (Util.PATH_HEART_RATE_HISTORY);
                    putDataMapReq.setUrgent();
                    Util.Log("send data: " + parseJsonArray(heartRateValues).toString());
                    putDataMapReq.getDataMap().putString(Util.DATA_HEART_RATE_VALUES,
                            parseJsonArray(heartRateValues).toString());
                    PutDataRequest putDataReq = putDataMapReq.asPutDataRequest();
                    PendingResult<DataApi.DataItemResult> pendingResult = Wearable.DataApi
                            .putDataItem(mGoogleApiClient, putDataReq);
                    pendingResult.setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
                        @Override
                        public void onResult(@NonNull final DataApi.DataItemResult result) {
                            if (result.getStatus().isSuccess()) {
                                // Mark heart rate values as sent
                                ContentValues contentValues = new ContentValues();
                                contentValues.put(DatabaseManager.HEART_RATE_IS_SENT_COLUMN, true);
                                database.update(DatabaseManager.HEART_RATE_TABLE, contentValues,
                                        null, null);
                            }
                            DatabaseManager.getInstance().closeDatabase();
                        }
                    });
                }
                cursor.close();
            }

            private JSONArray parseJsonArray(ArrayList<HeartRate> heartRateValues) {
                JSONArray heartRateValuesJson = new JSONArray();
                for (HeartRate heartRateValue : heartRateValues) {
                    try {
                        heartRateValuesJson.put(new JSONObject()
                                .put("time", heartRateValue.getTime())
                                .put("value", heartRateValue.getValue())
                        );
                    } catch (Exception e) {
                        Util.Log("Can't retrieve heart rate value: " + e);
                    }
                }
                return heartRateValuesJson;
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

                mSleepStarted = !mSleepStarted && heartRate < HEART_RATE_MIN_SLEEP;
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

            private void storeHeartRateInDb(long timestamp, int value) {
                DatabaseManager.initializeInstance(new DatabaseManager.SitesDatabaseHelper
                        (MyApplication.getContext()));
                SQLiteDatabase database = DatabaseManager.getInstance().openDatabase();
                ContentValues contentValues = new ContentValues();
                contentValues.put(DatabaseManager.HEART_RATE_TIME_COLUMN, timestamp);
                contentValues.put(DatabaseManager.HEART_RATE_VALUE_COLUMN, value);
                contentValues.put(DatabaseManager.HEART_RATE_IS_SENT_COLUMN, false);
                database.insert(DatabaseManager.HEART_RATE_TABLE, null, contentValues);
                DatabaseManager.getInstance().closeDatabase();
            }

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

    private Runnable mMeasureHeartRateRunnable = new Runnable() {
        @Override
        public void run() {
            setHeartRateListener();
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
