package ca.itquality.patrol.service;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Wearable;

import java.io.UnsupportedEncodingException;

import ca.itquality.patrol.MainActivity;
import ca.itquality.patrol.library.util.Util;
import ca.itquality.patrol.util.WearUtil;

public class ListenerServiceFromPhone extends Service implements GoogleApiClient
        .ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    public static final String INTENT_ACTIVITY_UPDATE = "ca.itquality.patrol.ACTIVITY_UPDATE";
    public static final String EXTRA_ACTIVITY = "Activity";

    private GoogleApiClient mGoogleApiClient;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        initGoogleClient();
    }

    @Override
    public void onDestroy() {
        mGoogleApiClient.disconnect();
        super.onDestroy();
    }

    private void initGoogleClient() {
        //Connect the GoogleApiClient
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        mGoogleApiClient.connect();
    }

    /*
     * Resolve the node = the connected device to send the message to
     */
    private void connectToPhone() {
        Wearable.MessageApi.addListener(mGoogleApiClient,
                new MessageApi.MessageListener() {
                    @Override
                    public void onMessageReceived(MessageEvent messageEvent) {
                        try {
                            String activityName = new String(messageEvent.getData(),
                                    "UTF-8");
                            Util.Log("received: " + activityName);
                            WearUtil.setActivityStatus(activityName);
                            sendBroadcast(new Intent(INTENT_ACTIVITY_UPDATE)
                                    .putExtra(EXTRA_ACTIVITY, activityName));
                        } catch (UnsupportedEncodingException e) {
                            // Activity type is empty
                        }
                    }
                });
        Wearable.DataApi.addListener(mGoogleApiClient, new DataApi.DataListener() {
            @Override
            public void onDataChanged(DataEventBuffer dataEventBuffer) {
                for (DataEvent event : dataEventBuffer) {
                    if (event.getType() == DataEvent.TYPE_CHANGED) {
                        DataItem item = event.getDataItem();
                        if ((item.getUri().getPath()).
                                equals(Util.LOGGED_IN_PATH)) {
                            DataMapItem dataItem = DataMapItem.fromDataItem(event.getDataItem());
                            Boolean isLoggedIn = dataItem.getDataMap().getBoolean
                                    (Util.LOGGED_IN_DATA);
                            WearUtil.setLoggedIn(isLoggedIn);

                            sendBroadcast(new Intent(MainActivity.LOGIN_STATE_INTENT)
                                    .putExtra(MainActivity.LOGIN_STATE_EXTRA, isLoggedIn));
                        }
                    }
                }
            }
        });
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        connectToPhone();
    }

    @Override
    public void onConnectionSuspended(int i) {
        Util.Log("connection suspended");
        // TODO:
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Util.Log("connection failed: " + connectionResult.getErrorCode());
        // TODO:
    }
}