package ca.itquality.patrol.service;


import android.app.Service;
import android.content.Intent;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.widget.Toast;

import com.squareup.seismic.ShakeDetector;

import ca.itquality.patrol.library.util.Util;

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
    public void hearShake() {
        Util.Log("Shaked");
        Toast.makeText(getApplicationContext(), "Shaked", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroy() {
        mShakeDetector.stop();
        super.onDestroy();
    }
}
