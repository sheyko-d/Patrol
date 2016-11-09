package ca.itquality.patrol.service;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import ca.itquality.patrol.MainActivity;
import ca.itquality.patrol.library.util.Util;
import ca.itquality.patrol.util.WearUtil;

public class ListenerServiceFromPhone extends Service implements GoogleApiClient
        .ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    public static final String INTENT_ACTIVITY_UPDATE = "ca.itquality.patrol.ACTIVITY_UPDATE";
    public static final String EXTRA_ACTIVITY = "Activity";
    public static final String INTENT_NAME_UPDATE = "ca.itquality.patrol.NAME_UPDATE";
    public static final String EXTRA_NAME = "Name";
    public static final String INTENT_LAST_MESSAGE_UPDATE
            = "ca.itquality.patrol.LAST_MESSAGE_UPDATE";
    public static final String EXTRA_LAST_MESSAGE_TITLE = "LastMessageTitle";
    public static final String EXTRA_LAST_MESSAGE_TEXT = "LastMessageText";
    public static final String INTENT_LOCATION_UPDATE = "ca.itquality.patrol.LOCATION_UPDATE";
    public static final String EXTRA_LOCATION = "Location";
    public static final String INTENT_SHIFT_UPDATE = "ca.itquality.patrol.SHIFT_UPDATE";
    public static final String EXTRA_SHIFT_TITLE = "ShiftTitle";
    public static final String EXTRA_SHIFT = "Shift";
    public static final String INTENT_STEPS_UPDATE = "ca.itquality.patrol.STEPS_UPDATE";
    public static final String EXTRA_STEPS = "Steps";
    public static final String INTENT_BACKUP = "ca.itquality.patrol.BACKUP";
    private static final String BACKUP_WEAR_PATH = "/stigg-backup";
    public static final String INTENT_WEATHER_UPDATE = "ca.itquality.patrol.WEATHER_UPDATE";
    public static final String EXTRA_WEATHER_TEMPERATURE = "WeatherTemperature";
    private static final long TIMEOUT_MS = 10000;

    private GoogleApiClient mGoogleApiClient;
    public static Bitmap sIcon;
    public static long sLastStillTime = -1;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        initGoogleClient();
        setBackupListener();
    }

    private void setBackupListener() {
        registerReceiver(mBackupReceiver, new IntentFilter(INTENT_BACKUP));
    }

    private BroadcastReceiver mBackupReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            showBackupNotificationOnPhone();
        }

        private Node mNode = null;

        private void showBackupNotificationOnPhone() {
            Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).setResultCallback
                    (new ResultCallback<NodeApi.GetConnectedNodesResult>() {
                        @Override
                        public void onResult(@NonNull NodeApi.GetConnectedNodesResult nodes) {
                            for (Node node : nodes.getNodes()) {
                                mNode = node;
                            }
                            if (mNode != null && mGoogleApiClient != null
                                    && mGoogleApiClient.isConnected()) {
                                Wearable.MessageApi.sendMessage(
                                        mGoogleApiClient, mNode.getId(), BACKUP_WEAR_PATH, null)
                                        .setResultCallback(

                                                new ResultCallback<MessageApi.SendMessageResult>() {
                                                    @Override
                                                    public void onResult(@NonNull MessageApi.SendMessageResult
                                                                                 sendMessageResult) {

                                                        if (!sendMessageResult.getStatus().isSuccess()) {
                                                            Util.Log("Failed to send message with status code: "
                                                                    + sendMessageResult.getStatus().getStatusCode());
                                                        }
                                                    }
                                                }
                                        );
                            }
                        }
                    });
        }
    };

    @Override
    public void onDestroy() {
        mGoogleApiClient.disconnect();
        unregisterReceiver(mBackupReceiver);
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
                        if (item.getUri().getPath().equals(Util.PATH_LOGGED_IN)) {
                            Boolean isLoggedIn = dataItem.getDataMap().getBoolean
                                    (Util.DATA_LOGGED_IN);
                            WearUtil.setLoggedIn(isLoggedIn);
                            sendBroadcast(new Intent(MainActivity.INTENT_LOGIN_STATE)
                                    .putExtra(MainActivity.LOGIN_STATE_EXTRA, isLoggedIn));
                        } else if (item.getUri().getPath().equals(Util.PATH_ACTIVITY)) {
                            String activity = dataItem.getDataMap().getString(Util.DATA_ACTIVITY);
                            WearUtil.setActivityStatus(activity);

                            if (activity.equals("Still")) {
                                sLastStillTime = System.currentTimeMillis();
                            } else {
                                sLastStillTime = -1;
                            }
                            sendBroadcast(new Intent(INTENT_ACTIVITY_UPDATE)
                                    .putExtra(EXTRA_ACTIVITY, activity));
                        } else if (item.getUri().getPath().equals(Util.PATH_SHIFT)) {
                            String shiftTitle = dataItem.getDataMap()
                                    .getString(Util.DATA_SHIFT_TITLE);
                            String shift = dataItem.getDataMap().getString(Util.DATA_SHIFT);
                            WearUtil.setShift(shiftTitle, shift);
                            sendBroadcast(new Intent(INTENT_SHIFT_UPDATE)
                                    .putExtra(EXTRA_SHIFT_TITLE, shiftTitle)
                                    .putExtra(EXTRA_SHIFT, shift));
                        } else if (item.getUri().getPath().equals(Util.PATH_NAME)) {
                            String name = dataItem.getDataMap().getString(Util.DATA_NAME);
                            WearUtil.setName(name);
                            sendBroadcast(new Intent(INTENT_NAME_UPDATE)
                                    .putExtra(EXTRA_NAME, name));
                        } else if (item.getUri().getPath().equals(Util.PATH_LAST_MESSAGE)) {
                            parseLastMessage(dataItem);
                        } else if (item.getUri().getPath().equals(Util.PATH_LOCATION)) {
                            parseLocation(dataItem);
                        } else if (item.getUri().getPath().equals(Util.PATH_STEPS)) {
                            parseSteps(dataItem);
                        } else if (item.getUri().getPath().equals(Util.PATH_WEATHER)) {
                            parseWeather(dataItem);
                        }
                    }
                }
            }
        });
    }

    private void parseWeather(DataMapItem dataItem) {
        Util.Log("receiver weather");
        int temperature = (dataItem.getDataMap().getInt(Util.DATA_TEMPERATURE));
        loadBitmapFromAsset(dataItem.getDataMap().getAsset(Util.DATA_ICON), temperature);
    }

    public void loadBitmapFromAsset(final Asset asset, final int temperature) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (asset == null) {
                    throw new IllegalArgumentException("Asset must be non-null");
                }
                ConnectionResult result =
                        mGoogleApiClient.blockingConnect(TIMEOUT_MS, TimeUnit.MILLISECONDS);
                if (!result.isSuccess()) {
                    sIcon = null;
                }
                // convert asset into a file descriptor and block until it's ready
                InputStream assetInputStream = Wearable.DataApi.getFdForAsset(
                        mGoogleApiClient, asset).await().getInputStream();
                mGoogleApiClient.disconnect();

                if (assetInputStream == null) {
                    Util.Log("Requested an unknown Asset.");
                    sIcon = null;
                }
                // decode the stream into a bitmap
                sIcon = BitmapFactory.decodeStream(assetInputStream);

                sendBroadcast(new Intent(INTENT_WEATHER_UPDATE)
                        .putExtra(EXTRA_WEATHER_TEMPERATURE, temperature));
            }
        }).start();
    }

    private void parseLastMessage(DataMapItem dataItem) {
        String title = dataItem.getDataMap().getString(Util.DATA_LAST_MESSAGE_TITLE);
        String text = dataItem.getDataMap().getString(Util.DATA_LAST_MESSAGE_TEXT);
        WearUtil.setLastMessage(title, text);
        sendBroadcast(new Intent(INTENT_LAST_MESSAGE_UPDATE)
                .putExtra(EXTRA_LAST_MESSAGE_TITLE, title)
                .putExtra(EXTRA_LAST_MESSAGE_TEXT, text));
    }

    private void parseLocation(DataMapItem dataItem) {
        String location = dataItem.getDataMap().getString(Util.DATA_LOCATION);
        WearUtil.setLocation(location);
        sendBroadcast(new Intent(INTENT_LOCATION_UPDATE)
                .putExtra(EXTRA_LOCATION, location));
    }

    private void parseSteps(DataMapItem dataItem) {
        int steps = dataItem.getDataMap().getInt(Util.DATA_STEPS);
        WearUtil.setSteps(steps);
        sendBroadcast(new Intent(INTENT_STEPS_UPDATE)
                .putExtra(EXTRA_STEPS, steps));
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        connectToPhone();
    }

    @Override
    public void onConnectionSuspended(int i) {
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
    }
}