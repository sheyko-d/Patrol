package ca.itquality.patrol;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
import android.support.wearable.activity.WearableActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Date;

import butterknife.Bind;
import butterknife.ButterKnife;
import ca.itquality.patrol.adapter.MainAdapter;
import ca.itquality.patrol.adapter.data.ListItem;
import ca.itquality.patrol.library.util.Util;
import ca.itquality.patrol.service.BackgroundService;
import ca.itquality.patrol.service.ListenerServiceFromPhone;
import ca.itquality.patrol.service.SensorsService;
import ca.itquality.patrol.service.ShakeListenerService;
import ca.itquality.patrol.util.WearUtil;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

public class MainActivity extends WearableActivity {

    // Constants
    private static final int MY_PERMISSIONS_REQUEST_SENSORS = 0;
    public static final String INTENT_LOGIN_STATE = "LoginState";
    public static final String LOGIN_STATE_EXTRA = "LoggedIn";

    // Views
    /*@Bind(R.id.main_heart_rate_txt)
    TextView mHeartRateTxt;
    @Bind(R.id.main_steps_txt)
    TextView mStepsTxt;
    @Bind(R.id.main_activity_txt)
    TextView mActivityTxt;*/
    @Bind(R.id.main_recycler)
    RecyclerView mRecycler;
    @Bind(R.id.main_clock_txt)
    TextView mClockView;
    @Bind(R.id.main_backup_layout)
    View mBackupLayout;
    @Bind(R.id.main_backup_dismiss_txt)
    TextView mBackupDismissTxt;
    @Bind(R.id.main_name_txt)
    TextView mNameTxt;
    @Bind(R.id.main_header_layout)
    View mHeaderLayout;

    // Usual variables
    private MainAdapter mAdapter;
    private ArrayList<ListItem> mItems = new ArrayList<>();
    private int mTotalScroll = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(0, 0);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        setAmbientEnabled();

        initRecycler();
        updateTime();
        initName();
        initActivity();
        initLastMessage();
        initLastLocation();
        initPermissions();
        initSteps();
        registerWearListener();
        registerShakeListener();
        startOutOfRangeService();
    }

    private void startOutOfRangeService() {
        startService(new Intent(this, BackgroundService.class));
    }

    private void initSteps() {
        mAdapter.updateStepsCount(WearUtil.getSteps());
    }

    private void registerShakeListener() {
        startService(new Intent(this, ShakeListenerService.class));
    }

    private void initLastMessage() {
        mAdapter.updateLastMessage(WearUtil.getLastMessageTitle(), WearUtil.getLastMessageText());
    }

    private void initLastLocation() {
        mAdapter.updateLocation(WearUtil.getLocation());
    }

    private void initName() {
        mNameTxt.setText(TextUtils.isEmpty(WearUtil.getName()) ? "—" : WearUtil.getName());
    }

    private void initRecycler() {
        mItems.add(new ListItem("Alert guards", R.drawable.alert, R.drawable.primary_circle_bg));
        mItems.add(new ListItem("No new messages", R.drawable.messages, R.drawable.blue_circle_bg));
        mItems.add(new ListItem("No shifts found", R.drawable.shift, R.drawable.teal_circle_bg));
        mItems.add(new ListItem("Activity", R.drawable.activity_walking,
                R.drawable.yellow_circle_bg));
        mItems.add(new ListItem("Steps", R.drawable.steps, R.drawable.orange_circle_bg));
        mItems.add(new ListItem("Heart rate", R.drawable.heart_rate, R.drawable.red_circle_bg));
        mItems.add(new ListItem("Location", R.drawable.location, R.drawable.purple_circle_bg));

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

                mHeaderLayout.setTranslationY(-mTotalScroll);
            }
        });
    }

    private void registerWearListener() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ListenerServiceFromPhone.INTENT_NAME_UPDATE);
        intentFilter.addAction(ListenerServiceFromPhone.INTENT_ACTIVITY_UPDATE);
        intentFilter.addAction(ListenerServiceFromPhone.INTENT_SHIFT_UPDATE);
        intentFilter.addAction(ListenerServiceFromPhone.INTENT_LAST_MESSAGE_UPDATE);
        intentFilter.addAction(ListenerServiceFromPhone.INTENT_LOCATION_UPDATE);
        intentFilter.addAction(ListenerServiceFromPhone.INTENT_STEPS_UPDATE);
        intentFilter.addAction(SensorsService.INTENT_HEART_RATE);
        intentFilter.addAction(INTENT_LOGIN_STATE);
        registerReceiver(mReceiver, intentFilter);
    }

    private void initActivity() {
        mAdapter.updateActivityStatus(WearUtil.getActivityStatus());
    }

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(INTENT_LOGIN_STATE)) {
                // If user logged out on the phone, log out on the watch as well
                if (!intent.getBooleanExtra(LOGIN_STATE_EXTRA, true)) {
                    finish();
                    startActivity(new Intent(MainActivity.this, LaunchActivity.class));
                }
            } else if (intent.getAction().equals(ListenerServiceFromPhone.INTENT_NAME_UPDATE)) {
                mNameTxt.setText(intent.getStringExtra(ListenerServiceFromPhone
                        .EXTRA_NAME));
            } else if (intent.getAction().equals(ListenerServiceFromPhone.INTENT_ACTIVITY_UPDATE)) {
                mAdapter.updateActivityStatus(intent.getStringExtra(ListenerServiceFromPhone
                        .EXTRA_ACTIVITY));
            } else if (intent.getAction().equals(ListenerServiceFromPhone.INTENT_SHIFT_UPDATE)) {
                mAdapter.updateShift(intent.getStringExtra(ListenerServiceFromPhone
                        .EXTRA_SHIFT_TITLE), intent.getStringExtra(ListenerServiceFromPhone
                        .EXTRA_SHIFT));
            } else if (intent.getAction().equals
                    (ListenerServiceFromPhone.INTENT_LAST_MESSAGE_UPDATE)) {
                mAdapter.updateLastMessage(intent.getStringExtra(ListenerServiceFromPhone
                        .EXTRA_LAST_MESSAGE_TITLE), intent.getStringExtra(ListenerServiceFromPhone
                        .EXTRA_LAST_MESSAGE_TEXT));
            } else if (intent.getAction().equals(ListenerServiceFromPhone.INTENT_LOCATION_UPDATE)) {
                mAdapter.updateLocation(intent.getStringExtra(ListenerServiceFromPhone
                        .EXTRA_LOCATION));
            } else if (intent.getAction().equals(SensorsService.INTENT_HEART_RATE)) {
                mAdapter.updateHeartRate(intent.getIntExtra(SensorsService.EXTRA_HEART_RATE, 0));
            } else if (intent.getAction().equals(ListenerServiceFromPhone.INTENT_STEPS_UPDATE)) {
                mAdapter.updateStepsCount(intent.getIntExtra(ListenerServiceFromPhone.EXTRA_STEPS,
                        0));
            }
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
        Util.Log("Launch sensors service");
        startService(new Intent(this, SensorsService.class));
    }

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

    @Override
    protected void onDestroy() {
        try {
            unregisterReceiver(mReceiver);
        } catch (Exception e) {
            // Receiver not registered
        }
        super.onDestroy();
    }

    /**
     * Required for the font library.
     */
    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }
}