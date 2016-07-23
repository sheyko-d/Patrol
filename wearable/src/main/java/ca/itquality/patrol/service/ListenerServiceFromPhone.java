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
import com.google.android.gms.wearable.Wearable;

import ca.itquality.patrol.MainActivity;
import ca.itquality.patrol.library.util.Util;
import ca.itquality.patrol.util.WearUtil;

public class ListenerServiceFromPhone extends Service implements GoogleApiClient
        .ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    public static final String INTENT_ACTIVITY_UPDATE = "ca.itquality.patrol.ACTIVITY_UPDATE";
    public static final String EXTRA_ACTIVITY = "Activity";
    public static final String INTENT_NAME_UPDATE = "ca.itquality.patrol.NAME_UPDATE";
    public static final String EXTRA_NAME = "Name";

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
        Wearable.DataApi.addListener(mGoogleApiClient, new DataApi.DataListener() {
            @Override
            public void onDataChanged(DataEventBuffer dataEventBuffer) {
                for (DataEvent event : dataEventBuffer) {
                    if (event.getType() == DataEvent.TYPE_CHANGED) {
                        DataItem item = event.getDataItem();
                        DataMapItem dataItem = DataMapItem.fromDataItem(event.getDataItem());
                        if ((item.getUri().getPath()).
                                equals(Util.PATH_LOGGED_IN)) {
                            Boolean isLoggedIn = dataItem.getDataMap().getBoolean
                                    (Util.DATA_LOGGED_IN);
                            WearUtil.setLoggedIn(isLoggedIn);

                            sendBroadcast(new Intent(MainActivity.INTENT_LOGIN_STATE)
                                    .putExtra(MainActivity.LOGIN_STATE_EXTRA, isLoggedIn));
                        } else if ((item.getUri().getPath()).
                                equals(Util.PATH_ACTIVITY)) {
                            String activity = dataItem.getDataMap().getString(Util.DATA_ACTIVITY);
                            WearUtil.setActivityStatus(activity);
                            sendBroadcast(new Intent(INTENT_ACTIVITY_UPDATE)
                                    .putExtra(EXTRA_ACTIVITY, activity));
                        } else if ((item.getUri().getPath()).equals(Util.PATH_NAME)) {
                            Util.Log("receive name");
                            String name = dataItem.getDataMap().getString(Util.DATA_NAME);
                            WearUtil.setName(name);
                            sendBroadcast(new Intent(INTENT_NAME_UPDATE)
                                    .putExtra(EXTRA_NAME, name));
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