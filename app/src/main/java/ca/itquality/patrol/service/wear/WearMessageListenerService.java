package ca.itquality.patrol.service.wear;

import android.app.Notification;
import android.content.Intent;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.NotificationCompat;

import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

import ca.itquality.patrol.R;
import ca.itquality.patrol.app.MyApplication;
import ca.itquality.patrol.auth.LoginActivity;
import ca.itquality.patrol.library.util.Util;
import ca.itquality.patrol.util.DeviceUtil;

public class WearMessageListenerService extends WearableListenerService {

    private static final String LOGIN_WEAR_PATH = "/stigg-login";
    private static final String BACKUP_WEAR_PATH = "/stigg-backup";
    private static final String STRETCH_WEAR_PATH = "/stigg-stretch";
    private static final String WEAR_WATCH_WEAR_PATH = "/stigg-wear-watch";

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        /*
         * Receive the message from wear
         */
        if (messageEvent.getPath().equals(LOGIN_WEAR_PATH)) {
            if (!DeviceUtil.isLoggedIn()) {
                Intent startIntent = new Intent(this, LoginActivity.class);
                startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(startIntent);
            }
        } else if (messageEvent.getPath().equals(BACKUP_WEAR_PATH)) {
            if (DeviceUtil.askBackup()) MyApplication.showBackupNotification();
        } else if (messageEvent.getPath().equals(STRETCH_WEAR_PATH)) {
            showStretchNotification();
        } else if (messageEvent.getPath().equals(WEAR_WATCH_WEAR_PATH)){
            showWearWatchNotification();
        }
    }

    private void showWearWatchNotification() {
        final NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder
                (MyApplication.getContext());
        notificationBuilder.setContentTitle("Please wear the watch");
        notificationBuilder.setContentText("It was removed for more than 5 min.");
        notificationBuilder.setAutoCancel(true);
        notificationBuilder.setSmallIcon(R.drawable.alert_notification);
        notificationBuilder.setColor(ContextCompat.getColor(MyApplication.getContext(),
                R.color.colorPrimary));
        notificationBuilder.setPriority(NotificationCompat.PRIORITY_MAX);

        Notification notification = notificationBuilder.build();
        notification.defaults |= Notification.DEFAULT_VIBRATE;
        notification.defaults |= Notification.DEFAULT_SOUND;
        final NotificationManagerCompat notificationManager = NotificationManagerCompat.from
                (MyApplication.getContext());
        notificationManager.notify(Util.NOTIFICATION_ID_STRETCH, notification);
    }


    public static void showStretchNotification() {
        if (!PreferenceManager.getDefaultSharedPreferences(MyApplication.getContext())
                .getBoolean("setting_stretching_alert", true)) {
            return;
        }

        final NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder
                (MyApplication.getContext());
        notificationBuilder.setContentTitle("Heartbeat is too low");
        notificationBuilder.setContentText("Please stretch or walk around.");
        notificationBuilder.setAutoCancel(true);
        notificationBuilder.setSmallIcon(R.drawable.backup_notification);
        notificationBuilder.setColor(ContextCompat.getColor(MyApplication.getContext(),
                R.color.colorPrimary));
        notificationBuilder.setPriority(NotificationCompat.PRIORITY_MAX);

        notificationBuilder.setVibrate(new long[]{1000});
        Notification notification = notificationBuilder.build();
        notification.defaults |= Notification.DEFAULT_SOUND;
        final NotificationManagerCompat notificationManager = NotificationManagerCompat.from
                (MyApplication.getContext());
        notificationManager.notify(Util.NOTIFICATION_ID_STRETCH, notification);
    }
}