package ca.itquality.patrol.service;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;

import java.util.List;

import ca.itquality.patrol.R;

public class BackgroundService extends Service implements GoogleApiClient.ConnectionCallbacks {

    private static final long CHECK_OUT_OF_RANGE_INTERVAL = 1000 * 60;
    private static final int NOTIFICATION_ID_OUT_OF_RANGE = 10;

    private GoogleApiClient mGoogleApiClient;
    private Handler mHandler = new Handler();

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

    private void initGoogleClient() {
        // Create an instance of GoogleAPIClient.
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addApi(Wearable.API)
                    .build();
        }
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        startOutOfRangeTask();
    }

    private void startOutOfRangeTask() {
        mOutOfRangeRunnable.run();
    }

    private Runnable mOutOfRangeRunnable = new Runnable() {
        @Override
        public void run() {
            checkOutOfRange();
            mHandler.postDelayed(this, CHECK_OUT_OF_RANGE_INTERVAL);
        }
    };

    private void checkOutOfRange() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                List<Node> connectedNodes = Wearable.NodeApi.getConnectedNodes(mGoogleApiClient)
                        .await().getNodes();
                if (connectedNodes == null || connectedNodes.size() == 0) {
                    showCarryPhoneReminder();
                }
            }
        }).start();
    }

    private void showCarryPhoneReminder() {
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder
                (getApplicationContext());
        notificationBuilder.setContentTitle("Phone is disconnected");
        notificationBuilder.setContentText("Please carry it with you or pair it.");
        notificationBuilder.setAutoCancel(true);
        notificationBuilder.setSmallIcon(R.mipmap.ic_launcher);
        notificationBuilder.setColor(Color.parseColor("#f80000"));

        Bitmap bitmap = Bitmap.createBitmap(320, 320, Bitmap.Config.ARGB_8888);
        bitmap.eraseColor(Color.parseColor("#f80000"));
        notificationBuilder.extend(new NotificationCompat.WearableExtender().setBackground(bitmap));

        Notification notification = notificationBuilder.build();
        notification.defaults |= Notification.DEFAULT_VIBRATE;
        notification.defaults |= Notification.DEFAULT_SOUND;
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from
                (getApplicationContext());
        notificationManager.notify(NOTIFICATION_ID_OUT_OF_RANGE, notification);
    }

    @Override
    public void onConnectionSuspended(int i) {
        mHandler.removeCallbacks(mOutOfRangeRunnable);
        stopSelf();
    }

    @Override
    public void onDestroy() {
        mHandler.removeCallbacks(mOutOfRangeRunnable);
        super.onDestroy();
    }
}
