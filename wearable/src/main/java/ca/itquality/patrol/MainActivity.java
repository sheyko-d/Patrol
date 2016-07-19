package ca.itquality.patrol;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.wearable.activity.WearableActivity;
import android.widget.TextView;

import com.crashlytics.android.Crashlytics;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import butterknife.Bind;
import butterknife.ButterKnife;
import ca.itquality.patrol.adapter.MainAdapter;
import ca.itquality.patrol.adapter.data.ListItem;
import ca.itquality.patrol.service.ListenerServiceFromPhone;
import ca.itquality.patrol.util.WearUtil;
import io.fabric.sdk.android.Fabric;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

public class MainActivity extends WearableActivity {

    // Constants
    private static final SimpleDateFormat AMBIENT_DATE_FORMAT =
            new SimpleDateFormat("HH:mm", Locale.US);
    private static final int MY_PERMISSIONS_REQUEST_SENSORS = 0;

    // Views
    /*@Bind(R.id.main_heart_rate_txt)
    TextView mHeartRateTxt;
    @Bind(R.id.main_steps_txt)
    TextView mStepsTxt;
    @Bind(R.id.main_activity_txt)
    TextView mActivityTxt;*/
    @Bind(R.id.main_recycler)
    RecyclerView mRecycler;
    @Bind(R.id.clock)
    TextView mClockView;

    // Usual variables
    private SensorManager mSensorManager;
    private MainAdapter mAdapter;
    private ArrayList<ListItem> mItems = new ArrayList<>();
    private int mTotalScroll = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Fabric.with(this, new Crashlytics());
        setContentView(R.layout.activity_main2);
        ButterKnife.bind(this);
        setAmbientEnabled();

        initRecycler();
        initClock();
        initActivity();
        initPermissions();
        initSensors();
        registerActivityStatusListener();
        startPhoneListenerService();
    }

    private void initRecycler() {
        mItems.add(new ListItem("Alert guards", R.drawable.alert, R.drawable.primary_circle_bg));
        mItems.add(new ListItem("1 new message", R.drawable.messages, R.drawable.blue_circle_bg));
        mItems.add(new ListItem("Activity", R.drawable.activity_walking,
                R.drawable.green_circle_bg));
        mItems.add(new ListItem("Steps", R.drawable.steps, R.drawable.orange_circle_bg));
        mItems.add(new ListItem("Heart rate", R.drawable.heart_rate, R.drawable.red_circle_bg));
        mItems.add(new ListItem("Floor", R.drawable.location, R.drawable.lime_circle_bg));

        mRecycler.setLayoutManager(new LinearLayoutManager(this));
        mRecycler.setHasFixedSize(true);
        mAdapter = new MainAdapter(this, mItems);
        mRecycler.setAdapter(mAdapter);

        mRecycler.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                mTotalScroll += dy;

                mClockView.setTranslationY(-mTotalScroll);
            }
        });
    }

    private void registerActivityStatusListener() {
        IntentFilter intentFilter = new IntentFilter
                (ListenerServiceFromPhone.INTENT_ACTIVITY_UPDATE);
        registerReceiver(mReceiver, intentFilter);
    }

    private void initActivity() {
        mAdapter.updateActivityStatus(WearUtil.getActivityStatus());
    }

    BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            mAdapter.updateActivityStatus(intent.getStringExtra(ListenerServiceFromPhone.EXTRA_ACTIVITY));
        }
    };

    private void startPhoneListenerService() {
        startService(new Intent(this, ListenerServiceFromPhone.class));
    }

    private void initPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && ContextCompat
                .checkSelfPermission(this, Manifest.permission.BODY_SENSORS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.BODY_SENSORS},
                    MY_PERMISSIONS_REQUEST_SENSORS);
        } else {
            initSensors();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_SENSORS: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    initSensors();
                }
            }
        }
    }

    private void initSensors() {
        mSensorManager = ((SensorManager) getSystemService(SENSOR_SERVICE));

        initHeartRateSensor();
        initStepCounterSensor();
    }

    private void initHeartRateSensor() {
        Sensor heartRateSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE);
        mSensorManager.registerListener(new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                if (event.values[0] != 0) {
                    mAdapter.updateHeartRate((int) event.values[0]);
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
            }
        }, heartRateSensor, SensorManager.SENSOR_DELAY_FASTEST);
    }

    private void initStepCounterSensor() {
        Sensor stepCounterSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        mSensorManager.registerListener(new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                mAdapter.updateStepsCount((int) event.values[0]);
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
            }
        }, stepCounterSensor, SensorManager.SENSOR_DELAY_FASTEST);
    }

    private void initClock() {
        mClockView.setText(AMBIENT_DATE_FORMAT.format(new Date()));
    }

    @Override
    public void onEnterAmbient(Bundle ambientDetails) {
        super.onEnterAmbient(ambientDetails);
        updateDisplay();
    }

    @Override
    public void onUpdateAmbient() {
        super.onUpdateAmbient();
        updateDisplay();
    }

    @Override
    public void onExitAmbient() {
        updateDisplay();
        super.onExitAmbient();
    }

    private void updateDisplay() {
        if (isAmbient()) {
            mClockView.setText(AMBIENT_DATE_FORMAT.format(new Date()));
        }
    }

    /**
     * Required for the font library.
     */
    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

    @Override
    protected void onStop() {
        try {
            unregisterReceiver(mReceiver);
        } catch (Exception e) {
            // Receiver not registered
        }
        super.onStop();
    }
}