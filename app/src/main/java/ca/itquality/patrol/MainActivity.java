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
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableStatusCodes;

import ca.itquality.patrol.auth.LoginActivity;
import ca.itquality.patrol.library.util.Util;
import ca.itquality.patrol.service.ActivityRecognizedService;
import ca.itquality.patrol.util.DeviceUtil;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private GoogleApiClient mGoogleApiClient;
    private Node mNode;
    private SensorManager mSensorManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (TextUtils.isEmpty(DeviceUtil.getUserId())) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        initGoogleClient();
        //resolveNode();
        initFloorListener();
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
    private void resolveNode() {
        Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).setResultCallback
                (new ResultCallback<NodeApi.GetConnectedNodesResult>() {
                    @Override
                    public void onResult(@NonNull NodeApi.GetConnectedNodesResult nodes) {
                        for (Node node : nodes.getNodes()) {
                            mNode = node;
                        }

                        registerActivityStatusListener();
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
                (mGoogleApiClient, mNode.getId(), "stigg-wear", status.getBytes());
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
        Intent intent = new Intent(this, ActivityRecognizedService.class);
        PendingIntent pendingIntent = PendingIntent.getService(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        ActivityRecognition.ActivityRecognitionApi.requestActivityUpdates(mGoogleApiClient, 0,
                pendingIntent);
        Util.Log("connected");
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
}
