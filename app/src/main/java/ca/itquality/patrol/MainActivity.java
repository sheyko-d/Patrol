package ca.itquality.patrol;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableStatusCodes;

import butterknife.Bind;
import butterknife.ButterKnife;
import ca.itquality.patrol.auth.LoginActivity;
import ca.itquality.patrol.library.util.Util;
import ca.itquality.patrol.service.ActivityRecognizedService;
import ca.itquality.patrol.service.ListenerServiceFromWear;
import ca.itquality.patrol.util.DeviceUtil;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, DataApi.DataListener {

    @Bind(R.id.main_progress_bar)
    ProgressBar mProgressBar;
    @Bind(R.id.main_disconnected_layout)
    View mDisconnectedLayout;
    @Bind(R.id.main_heart_rate_txt)
    TextView mHeartRateTxt;

    private GoogleApiClient mGoogleApiClient;
    private Node mNode;
    private SensorManager mSensorManager;

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

        initGoogleClient();
        connectToWatch();
        initFloorListener();
    }

    @Override
    protected void onResume() {
        super.onResume();
        connectToWatch();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Wearable.DataApi.removeListener(mGoogleApiClient, this);
        mGoogleApiClient.disconnect();
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

                            //mWelcomeTxt.setVisibility(View.VISIBLE);
                            mDisconnectedLayout.setVisibility(View.GONE);
                        } else {
                            mDisconnectedLayout.setVisibility(View.VISIBLE);
                            //mWelcomeTxt.setVisibility(View.GONE);
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
            String status = intent.getStringExtra(ActivityRecognizedService.EXTRA_ACTIVITY);
            updateWearActivityStatus(status);
        }
    };

    private void updateWearActivityStatus(final String status) {
        PendingResult<MessageApi.SendMessageResult> messageResult = Wearable.MessageApi.sendMessage
                (mGoogleApiClient, mNode.getId(), "stigg-login"/**TODO Change to stigg-actvity?*/, status.getBytes());
        Util.Log("update status on the watch");
        messageResult.setResultCallback(new ResultCallback<MessageApi.SendMessageResult>() {
            @Override
            public void onResult(@NonNull MessageApi.SendMessageResult sendMessageResult) {
                Util.Log("message status: " + sendMessageResult.getStatus().getStatusMessage());
                Status status = sendMessageResult.getStatus();
                Util.Log("Status: " + status.toString());
                if (status.getStatusCode() != WearableStatusCodes.SUCCESS) {
                    Util.Log("Tap to retry. Alert not sent :(");
                }
            }
        });
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        logInOnTheWatch();
        listenForActivityStatus();
        listenForWearSensors();
    }

    private void listenForWearSensors() {
        Wearable.DataApi.addListener(mGoogleApiClient, this);
    }

    private void logInOnTheWatch() {
        PutDataMapRequest putDataMapReq = PutDataMapRequest.create(Util.LOGGED_IN_PATH);
        putDataMapReq.setUrgent();
        putDataMapReq.getDataMap().putBoolean(Util.LOGGED_IN_DATA, true);
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

    public void onRetryButtonClicked(View view) {
        connectToWatch();
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEventBuffer) {
        for (DataEvent event : dataEventBuffer) {
            if (event.getType() == DataEvent.TYPE_CHANGED) {
                DataItem item = event.getDataItem();
                if ((item.getUri().getPath()).
                        equals(Util.HEART_RATE_PATH)) {
                    DataMapItem dataItem = DataMapItem.fromDataItem(event.getDataItem());
                    int heartRate = dataItem.getDataMap().getInt(Util.HEART_RATE_DATA);
                    mHeartRateTxt.setText(getString(R.string.main_heart_rate, heartRate));
                }
            }
        }
    }
}
