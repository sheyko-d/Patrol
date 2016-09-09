package ca.itquality.patrol.service;

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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.util.ArrayList;

import ca.itquality.patrol.library.util.Util;
import ca.itquality.patrol.library.util.app.MyApplication;
import ca.itquality.patrol.library.util.heartrate.DataValue;
import ca.itquality.patrol.util.DatabaseManager;

public class SensorsService extends Service implements GoogleApiClient.ConnectionCallbacks {

    // Constants
    private static final int HEART_RATE_MEASURE_INTERVAL = 1000*60;
    private static final int HEART_RATE_MIN_UPLOAD_COUNT = 5;
    private static final int HEART_RATE_MAX_BACKUP = 120;
    private static final int HEART_RATE_MIN_SLEEP = 60;
    public static final String INTENT_HEART_RATE = "ca.itquality.patrol.HEART_RATE";
    public static final String EXTRA_HEART_RATE = "DataValue";

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
    }

    private void setHeartRateListener() {
        final Sensor heartRateSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE);
        mSensorManager.registerListener(new SensorEventListener() {
            private Handler mHandler = new Handler();

            @Override
            public void onSensorChanged(SensorEvent event) {
                if (event.values[0] != 0) {
                    int heartRate = (int) event.values[0];

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
                    ArrayList<DataValue> heartRateValues = new ArrayList<>();
                    while (cursor.moveToNext()) {
                        heartRateValues.add(new DataValue(cursor.getLong(0), cursor.getString(1)));
                    }

                    PutDataMapRequest putDataMapReq = PutDataMapRequest.create
                            (Util.PATH_HEART_RATE_HISTORY);
                    putDataMapReq.setUrgent();
                    putDataMapReq.getDataMap().putString(Util.DATA_HEART_RATE_VALUES,
                            Util.parseJsonArray(heartRateValues).toString());
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

            // Checks if the guard's heart rate is within the normal range.
            // If it's too high, then call the backup.
            private void checkHeartRate(int heartRate) {
                if (!mBackupAsked && heartRate > HEART_RATE_MAX_BACKUP) {
                    mBackupAsked = true;
                    askForBackup();
                } else {
                    mBackupAsked = false;
                }

                mSleepStarted = !mSleepStarted && heartRate < HEART_RATE_MIN_SLEEP;
            }

            private void askForBackup() {
                // Show backup notification on a phone
                sendBroadcast(new Intent(ListenerServiceFromPhone.INTENT_BACKUP));
            }

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
