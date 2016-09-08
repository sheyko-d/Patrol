package ca.itquality.patrol;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.LocationServices;
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
import com.google.firebase.iid.FirebaseInstanceId;

import java.util.ArrayList;

import butterknife.Bind;
import butterknife.ButterKnife;
import ca.itquality.patrol.app.MyApplication;
import ca.itquality.patrol.auth.LoginActivity;
import ca.itquality.patrol.library.util.Util;
import ca.itquality.patrol.library.util.api.ApiClient;
import ca.itquality.patrol.library.util.api.ApiInterface;
import ca.itquality.patrol.library.util.auth.data.User;
import ca.itquality.patrol.library.util.messages.data.Message;
import ca.itquality.patrol.messages.ThreadsActivity;
import ca.itquality.patrol.qr.IntentIntegrator;
import ca.itquality.patrol.qr.IntentResult;
import ca.itquality.patrol.service.ActivityRecognizedService;
import ca.itquality.patrol.service.BackgroundService;
import ca.itquality.patrol.service.wear.WearDataListenerService;
import ca.itquality.patrol.settings.SettingsActivity;
import ca.itquality.patrol.util.DeviceUtil;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

import static ca.itquality.patrol.messages.ChatActivity.INCOMING_MESSAGE_INTENT;
import static com.google.android.gms.location.LocationServices.FusedLocationApi;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, DataApi.DataListener {

    // Constants
    private static final int PLACE_PICKER_REQUEST_CODE = 1;
    private static final int PERMISSIONS_REQUEST_CODE = 2;
    private static final int REQUEST_RESOLVE_ERROR = 1001;
    private static final String DIALOG_ERROR = "dialog_error";
    public static final String LOCATION_CHANGED_INTENT = "ca.itquality.patrol.LOCATION_CHANGED";
    public static final String LOCATION_ADDRESS_EXTRA = "Address";
    public static final String STEPS_CHANGED_INTENT = "ca.itquality.patrol.STEPS_CHANGED";
    public static final String STEPS_EXTRA = "STEPS";
    public static final String SHIFT_CHANGED_INTENT = "ca.itquality.patrol.SHIFT_CHANGED";
    public static final String SHIFT_TITLE_EXTRA = "ShiftTitle";
    public static final String SHIFT_EXTRA = "Shift";
    public static final String BACKUP_EXTRA = "Backup";
    public static final String BACKUP_DO_NOT_ASK_EXTRA = "BackupDoNotAsk";
    public static final String CLOCK_IN_EXTRA = "ClockIn";
    public static final String CLOCK_IN_SHIFT_ID_EXTRA = "ClockInShiftId";

    // Views
    @Bind(R.id.toolbar)
    Toolbar mToolbar;
    @Bind(R.id.main_drawer_layout)
    DrawerLayout mDrawerLayout;
    @Bind(R.id.main_progress_bar)
    ProgressBar mProgressBar;
    @Bind(R.id.main_disconnected_layout)
    View mDisconnectedLayout;
    @Bind(R.id.main_messages_txt)
    TextView mMessagesTxt;
    @Bind(R.id.main_messages_title_txt)
    TextView mMessagesTitleTxt;
    @Bind(R.id.main_heart_rate_txt)
    TextView mHeartRateTxt;
    @Bind(R.id.main_steps_txt)
    TextView mStepsTxt;
    @Bind(R.id.main_activity_txt)
    TextView mActivityTxt;
    @Bind(R.id.main_location_txt)
    TextView mLocationTxt;
    @Bind(R.id.main_qr_txt)
    TextView mQrTxt;
    @Bind(R.id.main_shift_title_txt)
    TextView mShiftTitleTxt;
    @Bind(R.id.main_shift_txt)
    TextView mShiftTxt;
    @Bind(R.id.main_navigation_view)
    NavigationView mNavigationView;
    @Bind(R.id.main_layout)
    View mLayout;

    // Usual variables
    private GoogleApiClient mGoogleApiClient;
    private Node mNode;
    private GoogleMap mMap;
    private TextView mAssignedObjectTxt;
    private Marker mAssignedPlaceMarker;
    private boolean mSupportsWatch = true;
    private boolean mResolvingError = false;
    private boolean mConnectedToWatch = false;

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

        askPermissions();
        initActionBar();
        initDrawer();
        initGoogleClient();
        initSensorData();
        initMap();
        startWearDataListenerService();
        registerListener();
        initShiftTxt();
        initAddressTxt();
        checkBackupRequest();
    }

    private void checkBackupRequest() {
        if (getIntent().getBooleanExtra(BACKUP_EXTRA, false)) {
            onAlertButtonClicked(null);
        } else if (getIntent().getBooleanExtra(BACKUP_DO_NOT_ASK_EXTRA, false)) {
            DeviceUtil.setAskBackup(false);
        } else if (getIntent().getBooleanExtra(CLOCK_IN_EXTRA, false)) {
            openClockInDialog(getIntent().getStringExtra(CLOCK_IN_SHIFT_ID_EXTRA));
        }
    }

    @SuppressLint("InflateParams")
    private void openClockInDialog(final String shiftId) {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this,
                R.style.MaterialDialogStyle);
        dialogBuilder.setTitle("Please explain the reason");
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_clock_in, null);
        final EditText editTxt = (EditText) dialogView.findViewById(R.id.clock_in_edit_txt);
        dialogBuilder.setView(dialogView);
        dialogBuilder.setCancelable(false);
        dialogBuilder.setPositiveButton("Send", null);
        final AlertDialog dialog = dialogBuilder.create();
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                        .setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                String reason = editTxt.getText().toString();
                                if (TextUtils.isEmpty(reason)) {
                                    Toast.makeText(MainActivity.this, "Reason is empty",
                                            Toast.LENGTH_SHORT).show();
                                    return;
                                }

                                postClockInReason(shiftId, reason);

                                dialog.cancel();
                                Toast.makeText(MainActivity.this, "Thank you!",
                                        Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        });
        dialog.show();

        // Cancel the notification
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from
                (MyApplication.getContext());
        notificationManager.cancel(Util.NOTIFICATION_ID_CLOCK_IN);
    }

    private void postClockInReason(String shiftId, String reason) {
        ApiInterface apiService = ApiClient.getClient().create(ApiInterface.class);
        Call<Void> call = apiService.postClockInReason(DeviceUtil.getToken(),
                shiftId, reason);
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Util.Log("Posted the clock in reason");
                } else {
                    if (response.code() == 400) {
                        Toast.makeText(MyApplication.getContext(),
                                "Some fields are empty.",
                                Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(MyApplication.getContext(), "Server error.",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void initShiftTxt() {
        mShiftTitleTxt.setText(DeviceUtil.getShiftTitle());
        mShiftTxt.setText(DeviceUtil.getShift());
    }

    private void initAddressTxt() {
        mLocationTxt.setText(DeviceUtil.getAddress());
    }

    private void startWearDataListenerService() {
        startService(new Intent(this, WearDataListenerService.class));
    }

    private void registerListener() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(INCOMING_MESSAGE_INTENT);
        intentFilter.addAction(LOCATION_CHANGED_INTENT);
        intentFilter.addAction(SHIFT_CHANGED_INTENT);
        intentFilter.addAction(STEPS_CHANGED_INTENT);
        intentFilter.addAction(ActivityRecognizedService.ACTIVITY_UPDATE_INTENT);
        registerReceiver(mReceiver, intentFilter);
    }

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(INCOMING_MESSAGE_INTENT)) {
                loadUnreadMessages();
            } else if (intent.getAction().equals(LOCATION_CHANGED_INTENT)) {
                String address = intent.getStringExtra(LOCATION_ADDRESS_EXTRA);
                mLocationTxt.setText(address);
            } else if (intent.getAction().equals(SHIFT_CHANGED_INTENT)) {
                String shiftTitle = intent.getStringExtra(SHIFT_TITLE_EXTRA);
                String shift = intent.getStringExtra(SHIFT_EXTRA);
                mShiftTitleTxt.setText(shiftTitle);
                mShiftTxt.setText(shift);
                DeviceUtil.setShift(shiftTitle, shift);
            } else if (intent.getAction().equals
                    (ActivityRecognizedService.ACTIVITY_UPDATE_INTENT)) {
                String activity = intent.getStringExtra(ActivityRecognizedService.ACTIVITY_EXTRA);
                DeviceUtil.setActivity(activity);
                mActivityTxt.setText(activity);
            } else if (intent.getAction().equals(STEPS_CHANGED_INTENT)) {
                int steps = intent.getIntExtra(STEPS_EXTRA, 0);
                DeviceUtil.setSteps(steps);
                mStepsTxt.setText(String.valueOf(steps));
            }
        }
    };

    private void startBackgroundService() {
        Util.Log("Will start background service");
        startService(new Intent(this, BackgroundService.class));
    }

    private void askPermissions() {
        if (ActivityCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission
                (MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            }, PERMISSIONS_REQUEST_CODE);
        }
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
        if (!(ActivityCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission
                (MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED)) {
            mMap.setMyLocationEnabled(true);
        }
    }

    private void updateMap() {
        if (mMap != null) {
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(DeviceUtil
                    .getAssignedObjectLatitude(), DeviceUtil.getAssignedObjectLongitude()), 17));
            if (mAssignedPlaceMarker != null) {
                mAssignedPlaceMarker.remove();
            }
            mAssignedPlaceMarker = mMap.addMarker(new MarkerOptions()
                    .position(new LatLng(DeviceUtil.getAssignedObjectLatitude(),
                            DeviceUtil.getAssignedObjectLongitude()))
                    .title("My place to guard")
                    .snippet(DeviceUtil.getAssignedObjectTitle()));
        }
    }

    private void updateProfile() {
        String googleToken = FirebaseInstanceId.getInstance().getToken();

        ApiInterface apiService = ApiClient.getClient().create(ApiInterface.class);
        Call<User> call = apiService.updateProfile(DeviceUtil.getUserId(), DeviceUtil.getToken(),
                googleToken);
        call.enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                if (response.isSuccessful()) {
                    User user = response.body();
                    DeviceUtil.updateProfile(user);

                    if (!DeviceUtil.isAssigned()) {
                        openPlacePicker();
                    } else {
                        updateMap();
                    }

                    // Update assigned text in navigation drawer
                    mAssignedObjectTxt.setText(TextUtils.isEmpty(DeviceUtil
                            .getAssignedObjectId()) ? getString(R.string.drawer_not_assigned)
                            : DeviceUtil.getAssignedObjectTitle());

                    updateWearName();
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
                Toast.makeText(MainActivity.this, "Server error.",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode,
                                    int resultCode, Intent data) {
        IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode,
                data);
        if (scanResult != null) {
            String scannedTxt = scanResult.getContents();
            if (!TextUtils.isEmpty(scannedTxt)) {
                DeviceUtil.setQr(scannedTxt);
                mQrTxt.setText(getString(R.string.main_qr, scannedTxt));
            }
        }
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
                Toast.makeText(MainActivity.this, "Server error.",
                        Toast.LENGTH_SHORT).show();
                setProgressBarVisible(false);
            }
        });
    }

    private void setProgressBarVisible(boolean visible) {
        // TODO:
    }

    private void updateWearName() {
        try {
            PutDataMapRequest putDataMapReq = PutDataMapRequest.create(Util.PATH_NAME);
            putDataMapReq.setUrgent();
            putDataMapReq.getDataMap().putString(Util.DATA_NAME, DeviceUtil.getName());
            putDataMapReq.getDataMap().putLong(Util.DATA_TIME, System.currentTimeMillis());
            PutDataRequest putDataReq = putDataMapReq.asPutDataRequest();
            Wearable.DataApi.putDataItem(mGoogleApiClient, putDataReq);
        } catch (Exception e) {
            // Watch is not supported
        }
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
        mAssignedObjectTxt.setText(TextUtils.isEmpty(DeviceUtil.getAssignedObjectId())
                ? getString(R.string.drawer_not_assigned) : DeviceUtil.getAssignedObjectTitle());
        Glide.with(this).load(DeviceUtil.getPhoto()).error(R.drawable.avatar_placeholder)
                .into(photoImg);

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
        mMessagesTitleTxt.setText(DeviceUtil.getLastMessageTitle());
        mMessagesTxt.setText(DeviceUtil.getLastMessageText());
        mStepsTxt.setText(DeviceUtil.getSteps() == -1 ? "—"
                : String.valueOf(DeviceUtil.getSteps()));
        mHeartRateTxt.setText(DeviceUtil.getHeartRate() == -1 ? "—"
                : getString(R.string.main_heart_rate, DeviceUtil.getHeartRate()));
        mQrTxt.setText(getString(R.string.main_qr, TextUtils.isEmpty(DeviceUtil.getQr()) ? "Never"
                : DeviceUtil.getQr()));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!mGoogleApiClient.isConnected() && !mGoogleApiClient.isConnecting()){
            mGoogleApiClient.connect();
        }
        updateProfile();
        if (!mConnectedToWatch && mSupportsWatch) {
            connectToWatch();
        }
        loadUnreadMessages();
    }

    private void loadUnreadMessages() {
        ApiInterface apiService = ApiClient.getClient().create(ApiInterface.class);
        Call<ArrayList<Message>> call = apiService.getUnreadMessages(DeviceUtil.getToken(),
                DeviceUtil.getChatLastSeenTime());
        call.enqueue(new Callback<ArrayList<Message>>() {
            @Override
            public void onResponse(Call<ArrayList<Message>> call,
                                   Response<ArrayList<Message>> response) {
                if (response.isSuccessful()) {
                    ArrayList<Message> messages = response.body();
                    if (messages.size() > 0) {
                        String lastMessageTitle;
                        if (messages.get(0).getTime() > DeviceUtil.getChatLastSeenTime()) {
                            lastMessageTitle = getString(R.string.main_messages_title,
                                    messages.size());
                        } else {
                            lastMessageTitle = getString(R.string.main_messages_title_placeholder);
                        }
                        mMessagesTitleTxt.setText(lastMessageTitle);
                        Message message = messages.get(messages.size() - 1);
                        String lastMessageText;
                        if (message.getUserId().equals(DeviceUtil.getUserId())) {
                            lastMessageText = "Me: "
                                    + message.getText();
                        } else {
                            lastMessageText = message.getUserName() + ": "
                                    + message.getText();
                        }
                        mMessagesTxt.setText(lastMessageText);

                        DeviceUtil.setLastMessage(lastMessageTitle, lastMessageText);
                        updateWearLastMessage(lastMessageTitle, lastMessageText);
                    }
                } else {
                    Toast.makeText(MainActivity.this, "Can't retrieve messages",
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ArrayList<Message>> call, Throwable t) {
                Toast.makeText(MainActivity.this, "Server error.",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateWearLastMessage(String title, String text) {
        try {
            PutDataMapRequest putDataMapReq = PutDataMapRequest.create(Util.PATH_LAST_MESSAGE);
            putDataMapReq.setUrgent();
            putDataMapReq.getDataMap().putString(Util.DATA_LAST_MESSAGE_TITLE, title);
            putDataMapReq.getDataMap().putString(Util.DATA_LAST_MESSAGE_TEXT, text);
            putDataMapReq.getDataMap().putLong(Util.DATA_TIME, System.currentTimeMillis());
            PutDataRequest putDataReq = putDataMapReq.asPutDataRequest();
            Wearable.DataApi.putDataItem(mGoogleApiClient, putDataReq);
        } catch (Exception e) {
            // Watch is not supported
        }
    }

    private void initGoogleClient() {
        //Connect the GoogleApiClient
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(ActivityRecognition.API)
                .addApi(Wearable.API)
                .addApi(LocationServices.API)
                .addApi(Fitness.SENSORS_API)
                .addScope(new Scope(Scopes.FITNESS_ACTIVITY_READ_WRITE))
                .useDefaultAccount()
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
    }

    /*
     * Resolve the node = the connected device to send the message to
     */
    private void connectToWatch() {
        mProgressBar.setVisibility(View.VISIBLE);
        try {
            Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).setResultCallback
                    (new ResultCallback<NodeApi.GetConnectedNodesResult>() {
                        @Override
                        public void onResult(@NonNull NodeApi.GetConnectedNodesResult nodes) {
                            for (Node node : nodes.getNodes()) {
                                mNode = node;
                            }

                            mProgressBar.setVisibility(View.GONE);
                            if (mNode != null) {
                                mConnectedToWatch = true;
                                Util.Log("connected to watch");
                                mLayout.setVisibility(View.VISIBLE);
                                mDisconnectedLayout.setVisibility(View.GONE);
                            } else {
                                mDisconnectedLayout.setVisibility(View.VISIBLE);
                                mLayout.setVisibility(View.GONE);
                            }
                        }
                    });
        } catch (Exception e) {
            // Watch is not supported
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (mSupportsWatch) {
            logInOnTheWatch();
            updateWearName();
        } else {
            mLayout.setVisibility(View.VISIBLE);
            mDisconnectedLayout.setVisibility(View.GONE);
        }
        connectToWatch();
        listenForActivityStatus();
        listenForWearSensors();
        saveLastKnownLocation();
        startBackgroundService();
    }

    private void saveLastKnownLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        try {
            Location location = FusedLocationApi.getLastLocation(mGoogleApiClient);
            DeviceUtil.setMyLocation((float) location.getLatitude(), (float) location.getLongitude());
        } catch (Exception e) {
            // Location is not available
        }
    }

    private void listenForWearSensors() {
        try {
            Wearable.DataApi.addListener(mGoogleApiClient, this);
        } catch (Exception e) {
            // Watch is not supported
        }
    }

    private void logInOnTheWatch() {
        try {
            PutDataMapRequest putDataMapReq = PutDataMapRequest.create(Util.PATH_LOGGED_IN);
            putDataMapReq.setUrgent();
            putDataMapReq.getDataMap().putBoolean(Util.DATA_LOGGED_IN, true);
            PutDataRequest putDataReq = putDataMapReq.asPutDataRequest();
            Wearable.DataApi.putDataItem(mGoogleApiClient, putDataReq);
        } catch (Exception e) {
            // Watch is not supported
        }
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
        try {
            Wearable.DataApi.removeListener(mGoogleApiClient, this);
        } catch (Exception e) {
            // Watch is not supported
        }
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        try {
            unregisterReceiver(mReceiver);
        } catch (Exception e) {
            // Receiver wasn't registered
        }
        super.onDestroy();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        if (mResolvingError) {
            // Already attempting to resolve an error.
            return;
        } else if (connectionResult.hasResolution()) {
            try {
                mResolvingError = true;
                connectionResult.startResolutionForResult(this, REQUEST_RESOLVE_ERROR);
            } catch (IntentSender.SendIntentException e) {
                // There was an error with the resolution intent. Try again.
                mGoogleApiClient.connect();
            }
        } else {
            // Show dialog using GoogleApiAvailability.getErrorDialog()
            showErrorDialog(connectionResult.getErrorCode());
            mResolvingError = true;
        }

        Util.Log("connection failed: " + connectionResult.getErrorCode());
        mDisconnectedLayout.setVisibility(View.GONE);
        mLayout.setVisibility(View.GONE);

        mProgressBar.setVisibility(View.GONE);
        if (connectionResult.getErrorCode() == 16) {
            Toast.makeText(MainActivity.this, "This device doesn't support working with the watch",
                    Toast.LENGTH_LONG).show();

            if (mSupportsWatch) {
                mSupportsWatch = false;
                mGoogleApiClient = new GoogleApiClient.Builder(this)
                        .addApi(ActivityRecognition.API)
                        .addApi(LocationServices.API)
                        .addConnectionCallbacks(this)
                        .addOnConnectionFailedListener(this)
                        .build();
                mGoogleApiClient.connect();
            }
        }
    }

    // The rest of this code is all about building the error dialog

    /* Creates a dialog for an error message */
    private void showErrorDialog(int errorCode) {
        // Create a fragment for the error dialog
        ErrorDialogFragment dialogFragment = new ErrorDialogFragment();
        // Pass the error that should be displayed
        Bundle args = new Bundle();
        args.putInt(DIALOG_ERROR, errorCode);
        dialogFragment.setArguments(args);
        dialogFragment.show(getSupportFragmentManager(), "errordialog");
    }

    /* Called from ErrorDialogFragment when the dialog is dismissed. */
    public void onDialogDismissed() {
        mResolvingError = false;
    }

    /* A fragment to display an error dialog */
    public static class ErrorDialogFragment extends DialogFragment {
        public ErrorDialogFragment() {
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Get the error code and retrieve the appropriate dialog
            int errorCode = this.getArguments().getInt(DIALOG_ERROR);
            return GoogleApiAvailability.getInstance().getErrorDialog(
                    this.getActivity(), errorCode, REQUEST_RESOLVE_ERROR);
        }

        @Override
        public void onDismiss(DialogInterface dialog) {
            ((MainActivity) getActivity()).onDialogDismissed();
        }
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
                }
            }
        }
    }

    public void onAlertButtonClicked(View view) {
        ((Vibrator) getSystemService(Context.VIBRATOR_SERVICE)).vibrate(500);
        ApiInterface apiService = ApiClient.getClient().create(ApiInterface.class);
        LatLng myLocation = DeviceUtil.getMyLocation();
        Call<Void> call = apiService.sendAlert(DeviceUtil.getToken(), myLocation.latitude,
                myLocation.longitude);
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(MainActivity.this, "Alert is sent!",
                            Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "Can't send alert",
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(MainActivity.this, "Server error.",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void onMessagesButtonClicked(View view) {
        startActivity(new Intent(this, ThreadsActivity.class));
    }

    public void onQrButtonClicked(View view) {
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.initiateScan();
    }
}
