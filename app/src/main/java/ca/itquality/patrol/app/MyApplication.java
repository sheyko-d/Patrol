package ca.itquality.patrol.app;

import android.app.Application;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.NotificationCompat;

import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.core.BuildConfig;
import com.crashlytics.android.core.CrashlyticsCore;
import com.facebook.FacebookSdk;
import com.facebook.appevents.AppEventsLogger;
import com.squareup.leakcanary.LeakCanary;

import ca.itquality.patrol.MainActivity;
import ca.itquality.patrol.R;
import ca.itquality.patrol.library.util.Util;
import io.fabric.sdk.android.Fabric;

public class MyApplication extends Application {

    private static final int SECOND_DURATION = 1000;
    private static MyApplication sContext;

    @Override
    public void onCreate() {
        super.onCreate();
        LeakCanary.install(this);
        Fabric.with(this, new Crashlytics.Builder().core(new CrashlyticsCore.Builder()
                .disabled(BuildConfig.DEBUG).build()).build());

        FacebookSdk.sdkInitialize(getApplicationContext());
        AppEventsLogger.activateApp(this);

        sContext = this;
    }

    public static Context getContext() {
        return sContext;
    }

    private static int sDismissSec;
    public static void showBackupNotification() {
        Util.Log("Show backup notification");

        sDismissSec = 10;
        final NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder
                (getContext());
        notificationBuilder.setContentTitle("Need a backup?");
        notificationBuilder.setContentText("Dismissing in "+ sDismissSec +" sec...");
        notificationBuilder.setAutoCancel(true);
        notificationBuilder.setSmallIcon(R.drawable.backup_notification);
        notificationBuilder.setColor(ContextCompat.getColor(MyApplication.getContext(),
                R.color.colorPrimary));
        notificationBuilder.setPriority(NotificationCompat.PRIORITY_MAX);

        Intent notifyIntent = new Intent(getContext(), MainActivity.class);
        notifyIntent.putExtra(MainActivity.BACKUP_EXTRA, true);
        // Sets the Activity to start in a new, empty task
        notifyIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_CLEAR_TASK);
        // Creates the PendingIntent
        PendingIntent pendingIntent =
                PendingIntent.getActivity(
                        getContext(),
                        0,
                        notifyIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        notificationBuilder.setContentIntent(pendingIntent);

        Intent doNotAskIntent = new Intent(getContext(), MainActivity.class);
        doNotAskIntent.putExtra(MainActivity.BACKUP_DO_NOT_ASK_EXTRA, true);
        // Sets the Activity to start in a new, empty task
        doNotAskIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_CLEAR_TASK);
        // Creates the PendingIntent
        PendingIntent doNotAskPendingIntent =
                PendingIntent.getActivity(
                        getContext(),
                        0,
                        doNotAskIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );

        notificationBuilder.addAction(new android.support.v4.app.NotificationCompat.Action
                (R.drawable.cancel, "Don't ask again", doNotAskPendingIntent));

        Notification notification = notificationBuilder.build();
        notification.defaults |= Notification.DEFAULT_VIBRATE;
        notification.defaults |= Notification.DEFAULT_SOUND;
        final NotificationManagerCompat notificationManager = NotificationManagerCompat.from
                (getContext());
        notificationManager.notify(Util.NOTIFICATION_ID_BACKUP, notification);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                sDismissSec--;
                notificationBuilder.setContentText("Dismissing in "+ sDismissSec +" sec...");
                Notification notification = notificationBuilder.build();
                notificationManager.notify(Util.NOTIFICATION_ID_BACKUP, notification);
                if (sDismissSec>0) {
                    new Handler().postDelayed(this, SECOND_DURATION);
                } else {
                    notificationManager.cancel(Util.NOTIFICATION_ID_BACKUP);
                }
            }
        }, SECOND_DURATION);
    }
}
