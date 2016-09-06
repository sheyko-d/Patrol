package ca.itquality.patrol.app;

import android.app.Application;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
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

    public static void showBackupNotification() {
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder
                (getContext());
        notificationBuilder.setContentTitle("Need help?");
        notificationBuilder.setContentText("Tap to request backup!");
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
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from
                (getContext());
        notificationManager.notify(Util.NOTIFICATION_ID_BACKUP, notification);
    }
}
