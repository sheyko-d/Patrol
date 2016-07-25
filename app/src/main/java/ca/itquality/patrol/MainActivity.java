package ca.itquality.patrol;

import android.Manifest;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import butterknife.Bind;
import butterknife.ButterKnife;
import ca.itquality.patrol.api.ApiClient;
import ca.itquality.patrol.api.ApiInterface;
import ca.itquality.patrol.auth.LoginActivity;
import ca.itquality.patrol.auth.data.User;
import ca.itquality.patrol.library.util.Util;
import ca.itquality.patrol.service.ActivityRecognizedService;
import ca.itquality.patrol.service.ListenerServiceFromWear;
import ca.itquality.patrol.settings.SettingsActivity;
import ca.itquality.patrol.util.DeviceUtil;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, DataApi.DataListener {

    // Constants
    private static final int PLACE_PICKER_REQUEST_CODE = 1;
    private static final int PERMISSIONS_REQUEST_CODE = 2;

    // Views
    @Bind(R.id.toolbar)
    Toolbar mToolbar;
    @Bind(R.id.main_drawer_layout)
    DrawerLayout mDrawerLayout;
    @Bind(R.id.main_progress_bar)
    ProgressBar mProgressBar;
    @Bind(R.id.main_disconnected_layout)
    View mDisconnectedLayout;
    @Bind(R.id.main_heart_rate_txt)
    TextView mHeartRateTxt;
    @Bind(R.id.main_steps_txt)
    TextView mStepsTxt;
    @Bind(R.id.main_activity_txt)
    TextView mActivityTxt;
    @Bind(R.id.main_navigation_view)
    NavigationView mNavigationView;
    @Bind(R.id.main_scroll_view)
    NestedScrollView mScrollView;
    @Bind(R.id.main_layout)
    View mLayout;

    // Usual variables
    private GoogleApiClient mGoogleApiClient;
    private Node mNode;
    private SensorManager mSensorManager;
    private GoogleMap mMap;
    private TextView mAssignedObjectTxt;
    private Marker mAssignedPlaceMarker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        if (!DeviceUtil.isLoggedIn()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        initActionBar();
        initDrawer();
        initGoogleClient();
        initSensorData();
        connectToWatch();
        initFloorListener();
        initMap();
    }

    private void initMap() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.main_map);
        mapFragment.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap map) {
                mMap = map;
                enableMyLocationOnMap();
                if (DeviceUtil.isAssigned()) {
                    updateMap();
                }
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_CODE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    enableMyLocationOnMap();
                }
            }
        }
    }

    private void enableMyLocationOnMap() {
        if (ActivityCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission
                (MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            }, PERMISSIONS_REQUEST_CODE);
            return;
        }
        mMap.setMyLocationEnabled(true);
    }

    private void updateMap() {
        if (mMap != null) {
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(DeviceUtil
                    .getGetAssignedLatitude(), DeviceUtil.getGetAssignedLongitude()), 17));
            if (mAssignedPlaceMarker != null) {
                mAssignedPlaceMarker.remove();
            }
            mAssignedPlaceMarker = mMap.addMarker(new MarkerOptions()
                    .position(new LatLng(DeviceUtil.getGetAssignedLatitude(),
                            DeviceUtil.getGetAssignedLongitude()))
                    .title("My place to guard")
                    .snippet(DeviceUtil.getGetAssignedObjectTitle()));
        }
    }

    private void updateProfile() {
        ApiInterface apiService = ApiClient.getClient().create(ApiInterface.class);
        Call<User> call = apiService.getProfile(DeviceUtil.getUserId(), DeviceUtil.getToken());
        call.enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                if (response.isSuccessful()) {
                    User user = response.body();
                    DeviceUtil.updateProfile(user.getToken(), user.getUserId(),
                            user.getAssignedObject(), user.getName(), user.getEmail(),
                            user.getPhoto());

                    if (!DeviceUtil.isAssigned()) {
                        openPlacePicker();
                    } else {
                        updateMap();
                    }

                    // Update assigned text in navigation drawer
                    mAssignedObjectTxt.setText(TextUtils.isEmpty(DeviceUtil
                            .getGetAssignedObjectId()) ? getString(R.string.drawer_not_assigned)
                            : DeviceUtil.getGetAssignedObjectTitle());
                } else {
                    if (response.code() == 403) {
                        finish();
                        startActivity(new Intent(MainActivity.this, LoginActivity.class));
                        Toast.makeText(MainActivity.this, "Please log in again",
                                Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(MainActivity.this, "Check your internet connection",
                                Toast.LENGTH_SHORT).show();
                    }
                }
            }

            private void openPlacePicker() {
                AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(MainActivity.this,
                        R.style.MaterialDialogStyle);
                dialogBuilder.setTitle("Welcome " + Util.parseFirstName(DeviceUtil.getName()) + "!");
                dialogBuilder.setMessage("Please select what place you were assigned to guard.");
                dialogBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        try {
                            PlacePicker.IntentBuilder intentBuilder =
                                    new PlacePicker.IntentBuilder();
                            Intent intent = intentBuilder.build(MainActivity.this);
                            startActivityForResult(intent, PLACE_PICKER_REQUEST_CODE);

                        } catch (GooglePlayServicesRepairableException
                                | GooglePlayServicesNotAvailableException e) {
                            Toast.makeText(MainActivity.this, "Can't open a place picker",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
                dialogBuilder.setNegativeButton("Later", null);
                dialogBuilder.create().show();
            }

            @Override
            public void onFailure(Call<User> call, Throwable t) {
                Toast.makeText(MainActivity.this, "Error, check your internet connection",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode,
                                    int resultCode, Intent data) {
        if (requestCode == PLACE_PICKER_REQUEST_CODE
                && resultCode == Activity.RESULT_OK) {

            final Place place = PlacePicker.getPlace(this, data);

            createPlace(place.getName().toString(), place.getLatLng().latitude,
                    place.getLatLng().longitude);
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void createPlace(String title, double latitude, double longitude) {
        setProgressBarVisible(true);

        ApiInterface apiService = ApiClient.getClient().create(ApiInterface.class);
        Call<User.AssignedObject> call = apiService.assignUser(DeviceUtil.getUserId(), title,
                latitude, longitude);
        call.enqueue(new Callback<User.AssignedObject>() {
            @Override
            public void onResponse(Call<User.AssignedObject> call,
                                   Response<User.AssignedObject> response) {
                if (response.isSuccessful()) {
                    User.AssignedObject assignedObject = response.body();
                    DeviceUtil.updateAssignedObject(assignedObject);
                    Toast.makeText(MainActivity.this, "New place is assigned!",
                            Toast.LENGTH_SHORT).show();

                    // Update assigned object title in the navigation drawer
                    mAssignedObjectTxt.setText(assignedObject.getTitle());

                    // Update map position with the new coordinates
                    updateMap();
                } else {
                    if (response.code() == 400) {
                        Toast.makeText(MainActivity.this, "Some fields are empty.",
                                Toast.LENGTH_SHORT).show();
                    } else if (response.code() == 409) {
                        Toast.makeText(MainActivity.this, "Can't create a new place.",
                                Toast.LENGTH_SHORT).show();
                    }
                }
                setProgressBarVisible(false);
            }

            @Override
            public void onFailure(Call<User.AssignedObject> call, Throwable t) {
                Toast.makeText(MainActivity.this, "Error, check your internet connection.",
                        Toast.LENGTH_SHORT).show();
                setProgressBarVisible(false);
            }
        });
    }

    private void setProgressBarVisible(boolean visible) {
        // TODO:
    }

    private void updateWearName() {
        PutDataMapRequest putDataMapReq = PutDataMapRequest.create(Util.PATH_NAME);
        putDataMapReq.setUrgent();
        putDataMapReq.getDataMap().putString(Util.DATA_NAME, DeviceUtil.getName());
        PutDataRequest putDataReq = putDataMapReq.asPutDataRequest();
        Wearable.DataApi.putDataItem(mGoogleApiClient, putDataReq);
    }

    private void initDrawer() {
        // Initialize a Drawer Layout and an ActionBarToggle
        ActionBarDrawerToggle actionBarDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout,
                mToolbar, R.string.app_name, R.string.app_name) {

            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
            }
        };
        // Set the actionbarToggle to drawer layout
        mDrawerLayout.addDrawerListener(actionBarDrawerToggle);

        // Calling sync state is necessary or else your hamburger icon wont show up
        actionBarDrawerToggle.syncState();

        View headerView = mNavigationView.getHeaderView(0);
        TextView nameTxt = (TextView) headerView.findViewById(R.id.drawer_name_txt);
        mAssignedObjectTxt = (TextView) headerView.findViewById
                (R.id.drawer_assigned_object_txt);
        ImageView photoImg = (ImageView) headerView.findViewById(R.id.drawer_photo_img);
        nameTxt.setText(DeviceUtil.getName());
        mAssignedObjectTxt.setText(TextUtils.isEmpty(DeviceUtil.getGetAssignedObjectId())
                ? getString(R.string.drawer_not_assigned) : DeviceUtil.getGetAssignedObjectTitle());
        Glide.with(this).load(DeviceUtil.getPhoto()).into(photoImg);

        mNavigationView.setNavigationItemSelectedListener(
                new NavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(final MenuItem menuItem) {
                        mDrawerLayout.closeDrawers();
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                if (menuItem.getItemId() == R.id.drawer_settings) {
                                    startActivity(new Intent(MainActivity.this,
                                            SettingsActivity.class));
                                    finish();
                                }
                            }
                        }, 300);
                        return false;
                    }
                });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Util.Log("Log selected: ");
        return super.onOptionsItemSelected(item);
    }

    private void initActionBar() {
        setSupportActionBar(mToolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowTitleEnabled(false);
            actionBar.setDisplayShowCustomEnabled(true);
            actionBar.setCustomView(R.layout.action_bar_logo);
        }
    }

    private void initSensorData() {
        mActivityTxt.setText(TextUtils.isEmpty(DeviceUtil.getActivity()) ? "—"
                : DeviceUtil.getActivity());
        mStepsTxt.setText(DeviceUtil.getSteps() == -1 ? "—"
                : String.valueOf(DeviceUtil.getSteps()));
        mHeartRateTxt.setText(DeviceUtil.getHeartRate() == -1 ? "—"
                : getString(R.string.main_heart_rate, DeviceUtil.getHeartRate()));
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateProfile();
        connectToWatch();
    }

    private void initFloorListener() {
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        Sensor sensor = mSensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
        //mSensorManager.registerListener
        //        (mBarometerListener, sensor, SensorManager.SENSOR_DELAY_GAME);
    }

    SensorEventListener mBarometerListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            Util.Log("barometer: " + event.values[0]);
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    };

    private void initGoogleClient() {
        //Connect the GoogleApiClient
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(ActivityRecognition.API)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
    }

    /*
     * Resolve the node = the connected device to send the message to
     */
    private void connectToWatch() {
        mProgressBar.setVisibility(View.VISIBLE);
        Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).setResultCallback
                (new ResultCallback<NodeApi.GetConnectedNodesResult>() {
                    @Override
                    public void onResult(@NonNull NodeApi.GetConnectedNodesResult nodes) {
                        for (Node node : nodes.getNodes()) {
                            mNode = node;
                        }

                        Wearable.MessageApi.addListener(mGoogleApiClient,
                                new ListenerServiceFromWear());

                        mProgressBar.setVisibility(View.GONE);
                        if (mNode != null) {
                            registerActivityStatusListener();

                            mLayout.setVisibility(View.VISIBLE);
                            mDisconnectedLayout.setVisibility(View.GONE);
                        } else {
                            mDisconnectedLayout.setVisibility(View.VISIBLE);
                            mLayout.setVisibility(View.GONE);
                        }
                    }
                });
    }

    private void registerActivityStatusListener() {
        IntentFilter intentFilter = new IntentFilter
                (ActivityRecognizedService.INTENT_ACTIVITY_UPDATE);
        registerReceiver(mActivityStatusReceiver, intentFilter);
    }

    BroadcastReceiver mActivityStatusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String activity = intent.getStringExtra(ActivityRecognizedService.EXTRA_ACTIVITY);
            updateWearActivityStatus(activity);
            DeviceUtil.setActivity(activity);
            mActivityTxt.setText(activity);
        }
    };

    private void updateWearActivityStatus(final String activity) {
        PutDataMapRequest putDataMapReq = PutDataMapRequest.create(Util.PATH_ACTIVITY);
        putDataMapReq.setUrgent();
        putDataMapReq.getDataMap().putString(Util.DATA_ACTIVITY, activity);
        PutDataRequest putDataReq = putDataMapReq.asPutDataRequest();
        Wearable.DataApi.putDataItem(mGoogleApiClient, putDataReq);
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        logInOnTheWatch();
        listenForActivityStatus();
        listenForWearSensors();
        updateWearName();
    }

    private void listenForWearSensors() {
        Wearable.DataApi.addListener(mGoogleApiClient, this);
    }

    private void logInOnTheWatch() {
        PutDataMapRequest putDataMapReq = PutDataMapRequest.create(Util.PATH_LOGGED_IN);
        putDataMapReq.setUrgent();
        putDataMapReq.getDataMap().putBoolean(Util.DATA_LOGGED_IN, true);
        PutDataRequest putDataReq = putDataMapReq.asPutDataRequest();
        Wearable.DataApi.putDataItem(mGoogleApiClient, putDataReq);
    }

    private void listenForActivityStatus() {
        Intent intent = new Intent(this, ActivityRecognizedService.class);
        PendingIntent pendingIntent = PendingIntent.getService(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        ActivityRecognition.ActivityRecognitionApi.requestActivityUpdates(mGoogleApiClient, 0,
                pendingIntent);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        Wearable.DataApi.removeListener(mGoogleApiClient, this);
        mGoogleApiClient.disconnect();
        try {
            unregisterReceiver(mActivityStatusReceiver);
        } catch (Exception e) {
            // Receiver wasn't registered
        }
        mSensorManager.unregisterListener(mBarometerListener);
        super.onStop();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Util.Log("connection failed");
    }

    /**
     * Required for the font library.
     */
    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

    public void onDownloadButtonClicked(View view) {
        final String appPackageName = "com.google.android.wearable.app";
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id="
                    + appPackageName)));
        } catch (android.content.ActivityNotFoundException e) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/" +
                    "store/apps/details?id=" + appPackageName)));
        }
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEventBuffer) {
        for (DataEvent event : dataEventBuffer) {
            if (event.getType() == DataEvent.TYPE_CHANGED) {
                DataItem item = event.getDataItem();
                if ((item.getUri().getPath()).
                        equals(Util.PATH_HEART_RATE)) {
                    DataMapItem dataItem = DataMapItem.fromDataItem(event.getDataItem());
                    int heartRate = dataItem.getDataMap().getInt(Util.DATA_HEART_RATE);
                    DeviceUtil.setHeartRate(heartRate);
                    mHeartRateTxt.setText(getString(R.string.main_heart_rate, heartRate));
                } else if ((item.getUri().getPath()).equals(Util.PATH_STEPS)) {
                    DataMapItem dataItem = DataMapItem.fromDataItem(event.getDataItem());
                    int steps = dataItem.getDataMap().getInt(Util.DATA_STEPS);
                    DeviceUtil.setSteps(steps);
                    mStepsTxt.setText(String.valueOf(steps));
                }
            }
        }
    }
}
