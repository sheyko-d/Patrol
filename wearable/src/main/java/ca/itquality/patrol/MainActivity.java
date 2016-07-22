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
import android.os.Handler;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
import android.support.wearable.activity.WearableActivity;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.util.ArrayList;
import java.util.Date;

import butterknife.Bind;
import butterknife.ButterKnife;
import ca.itquality.patrol.adapter.MainAdapter;
import ca.itquality.patrol.adapter.data.ListItem;
import ca.itquality.patrol.library.util.Util;
import ca.itquality.patrol.service.ListenerServiceFromPhone;
import ca.itquality.patrol.util.WearUtil;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

public class MainActivity extends WearableActivity implements GoogleApiClient.ConnectionCallbacks {

    // Constants
    private static final int MY_PERMISSIONS_REQUEST_SENSORS = 0;
    private static final int HEART_RATE_MEASURE_INTERVAL = 10000;
    public static final String LOGIN_STATE_INTENT = "LoginState";
    public static final String LOGIN_STATE_EXTRA = "LoggedIn";
    private static final int BACKUP_DISMISS_DURATION = 10;
    private static final int SECOND_DURATION = 1000;
    private static final int HEART_RATE_MIN_BACKUP = 120;

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
    @Bind(R.id.main_backup_layout)
    View mBackupLayout;
    @Bind(R.id.main_backup_dismiss_txt)
    TextView mBackupDismissTxt;

    // Usual variables
    private SensorManager mSensorManager;
    private MainAdapter mAdapter;
    private ArrayList<ListItem> mItems = new ArrayList<>();
    private int mTotalScroll = 0;
    private Handler mHandler = new Handler();
    private GoogleApiClient mGoogleApiClient;
    private boolean mBackupAsked = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(0, 0);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        setAmbientEnabled();

        initGoogleClient();
        initRecycler();
        updateTime();
        initActivity();
        initPermissions();
        registerActivityStatusListener();
        registerLoginStateListener();
    }

    private void initGoogleClient() {
        //Connect the GoogleApiClient
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .build();
    }

    private void registerLoginStateListener() {
        IntentFilter filter = new IntentFilter(LOGIN_STATE_INTENT);
        registerReceiver(mLoginStateReceiver, filter);
    }

    BroadcastReceiver mLoginStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // If user logged out on the phone, log out on the watch as well
            if (!intent.getBooleanExtra(LOGIN_STATE_EXTRA, true)) {
                finish();
                startActivity(new Intent(MainActivity.this, LaunchActivity.class));
            }
        }
    };

    private void initRecycler() {
        mItems.add(new ListItem("Alert guards", R.drawable.alert, R.drawable.primary_circle_bg));
        mItems.add(new ListItem("1 new message", R.drawable.messages, R.drawable.blue_circle_bg));
        mItems.add(new ListItem("Activity", R.drawable.activity_walking,
                R.drawable.green_circle_bg));
        mItems.add(new ListItem("Steps", R.drawable.steps, R.drawable.orange_circle_bg));
        mItems.add(new ListItem("Heart rate", R.drawable.heart_rate, R.drawable.red_circle_bg));
        mItems.add(new ListItem("Floor", R.drawable.location, R.drawable.purple_circle_bg));

        mRecycler.setLayoutManager(new LinearLayoutManager(this));
        mRecycler.setHasFixedSize(true);
        mAdapter = new MainAdapter(this, mItems);
        mRecycler.setAdapter(mAdapter);
        RecyclerView.ItemAnimator animator = mRecycler.getItemAnimator();
        if (animator instanceof SimpleItemAnimator) {
            ((SimpleItemAnimator) animator).setSupportsChangeAnimations(false);
        }

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

        getHeartRateSensorData();
        getStepsSensorData();
    }

    private void getHeartRateSensorData() {
        final Sensor heartRateSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE);
        mSensorManager.registerListener(new SensorEventListener() {
            public int mDismissRemainingSec;
            private Handler mHandler = new Handler();

            @Override
            public void onSensorChanged(SensorEvent event) {
                if (event.values[0] != 0) {
                    int heartRate = (int) event.values[0];
                    mAdapter.updateHeartRate((int) event.values[0]);
                    mSensorManager.unregisterListener(this, heartRateSensor);
                    mHandler.postDelayed(mMeasureHeartRateRunnable, HEART_RATE_MEASURE_INTERVAL);
                    updateHeartRateOnDevice(heartRate);
                    checkHeartRate(heartRate);
                }
            }

            // Checks if the guard's heart rate is within the normal range.
            // If it's too high, then call the backup.
            private void checkHeartRate(int heartRate) {
                if (heartRate > HEART_RATE_MIN_BACKUP) {
                    if (!mBackupAsked) {
                        mBackupAsked = true;
                        askForBackUp();
                    }
                } else {
                    mBackupAsked = false;
                    dismissBackup();
                }
            }

            private void dismissBackup() {
                // Hide the backup layout
                mBackupLayout.setVisibility(View.GONE);

                // Cancel the dismiss the timer
                mHandler.removeCallbacks(mBackupDismissRunnable);
            }

            private void askForBackUp() {
                // Vibrate for 0.5 sec
                ((Vibrator) getSystemService(VIBRATOR_SERVICE)).vibrate(1000);

                mDismissRemainingSec = BACKUP_DISMISS_DURATION;
                mBackupDismissRunnable.run();

                // Show the backup layout
                mBackupLayout.setVisibility(View.VISIBLE);
            }

            Runnable mBackupDismissRunnable = new Runnable() {
                @Override
                public void run() {
                    if (mDismissRemainingSec > 0) {
                        mBackupDismissTxt.setText(getString(R.string.main_backup_dismiss,
                                mDismissRemainingSec));
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
                mAdapter.updateStepsCount(steps);
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

    Runnable mMeasureHeartRateRunnable = new Runnable() {
        @Override
        public void run() {
            getHeartRateSensorData();
        }
    };

    private void updateTime() {
        mClockView.setText(WearUtil.AMBIENT_DATE_FORMAT.format(new Date()));
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
            updateTime();
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
            mHandler.removeCallbacks(mMeasureHeartRateRunnable);
            unregisterReceiver(mReceiver);
        } catch (Exception e) {
            // Receiver not registered
        }
        try {
            unregisterReceiver(mLoginStateReceiver);
        } catch (Exception e) {
            // Receiver not registered
        }
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        initSensors();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }
}