package ca.itquality.patrol.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Vibrator;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.NotificationCompat;
import android.text.TextUtils;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import ca.itquality.patrol.R;
import ca.itquality.patrol.alert.AlertActivity;
import ca.itquality.patrol.app.MyApplication;
import ca.itquality.patrol.library.util.Util;
import ca.itquality.patrol.messages.ChatActivity;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        try {
            if (remoteMessage.getData().get("type").equals("alert")) {
                String name = remoteMessage.getData().get("name");
                Double latitude = Double.valueOf(remoteMessage.getData().get("latitude"));
                Double longitude = Double.valueOf(remoteMessage.getData().get("longitude"));
                startActivity(new Intent(getApplicationContext(), AlertActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        .putExtra(AlertActivity.EXTRA_NAME, name)
                        .putExtra(AlertActivity.EXTRA_LATITUDE, latitude)
                        .putExtra(AlertActivity.EXTRA_LONGITUDE, longitude));
            } else if (remoteMessage.getData().get("type").equals("message")) {
                String threadId = remoteMessage.getData().get("thread_id");
                String text = remoteMessage.getData().get("text");
                String name = remoteMessage.getData().get("name");
                String title = remoteMessage.getData().get("title");
                if (!ChatActivity.chatOpened || !ChatActivity.threadId.equals(threadId)) {
                    showMessageNotification(threadId, text, name, title);
                } else {
                    ((Vibrator) getSystemService(Context.VIBRATOR_SERVICE)).vibrate(200);
                }
                sendBroadcast(new Intent(ChatActivity.INCOMING_MESSAGE_INTENT));
            }
        } catch (Exception e) {
            Util.Log("Can't parse FCM message: " + e);
        }
    }

    private void showMessageNotification(String threadId, String text, String name, String title) {
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder
                (MyApplication.getContext());
        if (TextUtils.isEmpty(title)) {
            notificationBuilder.setContentTitle(name);
            notificationBuilder.setContentText(text);
        } else {
            notificationBuilder.setContentTitle(title);
            notificationBuilder.setContentText(name + ": " + text);
        }
        notificationBuilder.setSmallIcon(R.drawable.message_notification);
        notificationBuilder.setColor(ContextCompat.getColor(MyApplication.getContext(),
                R.color.colorPrimary));
        notificationBuilder.setPriority(NotificationCompat.PRIORITY_HIGH);

        Intent resultIntent = new Intent(this, ChatActivity.class)
                .putExtra(ChatActivity.EXTRA_THREAD_ID, threadId)
                .putExtra(ChatActivity.EXTRA_THREAD_TITLE, TextUtils.isEmpty(title) ? name : title);
        PendingIntent resultPendingIntent = PendingIntent.getActivity(this, 0, resultIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        notificationBuilder.setContentIntent(resultPendingIntent);

        Notification notification = notificationBuilder.build();
        notification.defaults |= Notification.DEFAULT_VIBRATE;
        notification.defaults |= Notification.DEFAULT_SOUND;
        NotificationManager notificationManager = (NotificationManager) getSystemService
                (NOTIFICATION_SERVICE);
        notificationManager.notify(ChatActivity.NOTIFICATION_ID_MESSAGE, notification);
    }
}
