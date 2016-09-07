package ca.itquality.patrol.service;


import android.app.Service;
import android.content.Intent;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.annotation.Nullable;

import com.squareup.seismic.ShakeDetector;

import ca.itquality.patrol.app.MyApplication;

public class ShakeListenerService extends Service implements ShakeDetector.Listener {

    private static final long MIN_BACKUP_INTERVAL = 10 * 1000;
    private static final String WAKELOCK_TAG = "ShakeWakelock";
    private ShakeDetector mShakeDetector;
    private PowerManager.WakeLock mWakeLock;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        SensorManager sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mShakeDetector = new ShakeDetector(this);
        mShakeDetector.setSensitivity(ShakeDetector.SENSITIVITY_MEDIUM);
        mShakeDetector.start(sensorManager);

        setWakelock(true);
    }

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
    public void hearShake() {
        if (System.currentTimeMillis() - MyApplication.lastReceivedMessageTime
                > MIN_BACKUP_INTERVAL) {
            MyApplication.lastReceivedMessageTime = System.currentTimeMillis();
            // Show backup notification on a phone
            sendBroadcast(new Intent(ListenerServiceFromPhone.INTENT_BACKUP));
        }
    }

    @Override
    public void onDestroy() {
        mShakeDetector.stop();
        if (mWakeLock.isHeld()) mWakeLock.release();
        super.onDestroy();
    }
}
