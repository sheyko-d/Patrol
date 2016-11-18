package ca.itquality.patrol.service;


import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.annotation.Nullable;

import ca.itquality.patrol.library.util.Util;
import ca.itquality.patrol.util.WearUtil;

import static ca.itquality.patrol.service.ListenerServiceFromPhone.INTENT_WEAR_WATCH;

public class WearingListenerService extends Service {

    private static final String WAKELOCK_TAG = "ShakeWakelock";
    private static final int MINUTE_DURATION = 60 * 1000;
    private PowerManager.WakeLock mWakeLock;
    private SensorManager mSensorManager;
    private Handler mHandler = new Handler();
    private long mLastWearingTime = -1;
    private boolean mAskedToWearWatch = false;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mSensorManager = ((SensorManager) getSystemService(SENSOR_SERVICE));
        mWearingWatchRunnable.run();

        startWearingCheckTask();

        setWakelock(true);
    }

    private void startWearingCheckTask() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!mAskedToWearWatch && mLastWearingTime != -1
                        && System.currentTimeMillis() - mLastWearingTime
                        > WearUtil.getWatchRemovedMaxDuration()) {
                    askToWearWatch();
                    mAskedToWearWatch = true;
                }
                mHandler.postDelayed(this, MINUTE_DURATION);
            }
        }, MINUTE_DURATION);
    }

    private void askToWearWatch() {
        sendBroadcast(new Intent(INTENT_WEAR_WATCH));
    }

    private SensorEventListener mWearingWatchListener = new SensorEventListener() {

        private float mAcceleration;
        private float mAccelerationCurrent;
        private float mAccelerationLast;

        @Override
        public void onSensorChanged(SensorEvent event) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];
            mAccelerationLast = mAccelerationCurrent;
            mAccelerationCurrent = (float) Math.sqrt((double) (x * x + y * y + z * z));
            float delta = mAccelerationCurrent - mAccelerationLast;
            mAcceleration = mAcceleration * 0.9f + delta;

            if (mAcceleration > 1) {
                Util.Log("Watch started counting down inactivity");
                mLastWearingTime = System.currentTimeMillis();
                mAskedToWearWatch = false;
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {
        }
    };

    private Runnable mWearingWatchRunnable = new Runnable() {
        @Override
        public void run() {
            final Sensor accelerometerSensor = mSensorManager.getDefaultSensor
                    (Sensor.TYPE_ACCELEROMETER);
            mSensorManager.registerListener(mWearingWatchListener, accelerometerSensor,
                    SensorManager.SENSOR_DELAY_UI);
        }
    };

    private void setWakelock(boolean enabled) {
        if (enabled) {
            PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
            mWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                    WAKELOCK_TAG);
            mWakeLock.acquire();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (mWakeLock.isHeld()) mWakeLock.release();
        mHandler.removeCallbacks(mWearingWatchRunnable);
        mSensorManager.unregisterListener(mWearingWatchListener);
        super.onDestroy();
    }
}
