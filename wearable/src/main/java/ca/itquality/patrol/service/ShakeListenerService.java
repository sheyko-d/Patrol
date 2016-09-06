package ca.itquality.patrol.service;


import android.app.Service;
import android.content.Intent;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.support.annotation.Nullable;

import com.squareup.seismic.ShakeDetector;

public class ShakeListenerService extends Service implements ShakeDetector.Listener {

    private ShakeDetector mShakeDetector;

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
        mShakeDetector.setSensitivity(ShakeDetector.SENSITIVITY_HARD);
        mShakeDetector.start(sensorManager);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void hearShake() {
        sendBroadcast(new Intent(ListenerServiceFromPhone.INTENT_BACKUP));
    }

    @Override
    public void onDestroy() {
        mShakeDetector.stop();
        super.onDestroy();
    }
}
