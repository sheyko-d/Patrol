package ca.itquality.patrol.service.firebase;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.os.Vibrator;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.NotificationCompat;
import android.text.TextUtils;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import ca.itquality.patrol.R;
import ca.itquality.patrol.alert.AlertActivity;
import ca.itquality.patrol.app.MyApplication;
import ca.itquality.patrol.library.util.Util;
import ca.itquality.patrol.messages.ChatActivity;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final int WEAR_IMAGE_SIZE = 400;

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        try {
            if (remoteMessage.getData().get("type").equals("alert")) {
                parseAlert(remoteMessage);
            } else if (remoteMessage.getData().get("type").equals("message")) {
                parseMessage(remoteMessage);
            }
        } catch (Exception e) {
            Util.Log("Can't parse FCM message: " + e);
        }
    }

    private void parseAlert(RemoteMessage remoteMessage) {
        String name = remoteMessage.getData().get("name");
        Double latitude = Double.valueOf(remoteMessage.getData().get("latitude"));
        Double longitude = Double.valueOf(remoteMessage.getData().get("longitude"));
        /*startActivity(new Intent(getApplicationContext(), AlertActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra(AlertActivity.EXTRA_NAME, name)
                .putExtra(AlertActivity.EXTRA_LATITUDE, latitude)
                .putExtra(AlertActivity.EXTRA_LONGITUDE, longitude));*/
        showAlertNotification(name, latitude, longitude);
    }

    private void showAlertNotification(String name, Double latitude, Double longitude) {
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder
                (MyApplication.getContext());
        notificationBuilder.setContentTitle(name + " needs backup!");
        if (latitude > -1 && longitude > -1) {
            notificationBuilder.setContentText("Tap to show him on the map");
        } else {
            notificationBuilder.setContentText("The exact location is unknown, please message him");
        }
        notificationBuilder.setAutoCancel(true);
        notificationBuilder.setSmallIcon(R.drawable.alert_notification);
        notificationBuilder.setColor(ContextCompat.getColor(MyApplication.getContext(),
                R.color.colorPrimary));
        notificationBuilder.setPriority(NotificationCompat.PRIORITY_MAX);

        Intent resultIntent = new Intent(getApplicationContext(), AlertActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra(AlertActivity.EXTRA_NAME, name)
                .putExtra(AlertActivity.EXTRA_LATITUDE, latitude)
                .putExtra(AlertActivity.EXTRA_LONGITUDE, longitude);
        PendingIntent resultPendingIntent = PendingIntent.getActivity(this, 0, resultIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        notificationBuilder.setContentIntent(resultPendingIntent);

        Notification notification = notificationBuilder.build();
        notification.defaults |= Notification.DEFAULT_VIBRATE;
        notification.defaults |= Notification.DEFAULT_SOUND;
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from
                (getApplicationContext());
        notificationManager.notify(AlertActivity.NOTIFICATION_ID_ALERT, notification);
    }

    private void parseMessage(RemoteMessage remoteMessage) {
        final String threadId = remoteMessage.getData().get("thread_id");
        final String text = remoteMessage.getData().get("text");
        final String name = remoteMessage.getData().get("name");
        final String title = remoteMessage.getData().get("title");
        final String photo = remoteMessage.getData().get("photo");
        if (!ChatActivity.chatOpened || !threadId.equals(ChatActivity.threadId)) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    Bitmap photoBitmap = getBitmapFromURL(photo);
                    Bitmap phoneBitmap = createScaledBitmap(
                            photoBitmap, Util.convertDpToPixel(getApplicationContext(), 64),
                            Util.convertDpToPixel(getApplicationContext(), 64));
                    Bitmap wearBitmap = createScaledBitmap(
                            photoBitmap, WEAR_IMAGE_SIZE, WEAR_IMAGE_SIZE);
                    showMessageNotification(threadId, text, name, title, phoneBitmap, wearBitmap);
                }
            }).start();
        } else {
            ((Vibrator) getSystemService(Context.VIBRATOR_SERVICE)).vibrate(200);
        }
        sendBroadcast(new Intent(ChatActivity.INCOMING_MESSAGE_INTENT));
    }

    private Bitmap createScaledBitmap(Bitmap bitmap, int newWidth, int newHeight) {
        Bitmap scaledBitmap = Bitmap.createBitmap(newWidth, newHeight, Bitmap.Config.ARGB_8888);

        float ratioX = newWidth / (float) bitmap.getWidth();
        float ratioY = newHeight / (float) bitmap.getHeight();
        float middleX = newWidth / 2.0f;
        float middleY = newHeight / 2.0f;

        Matrix scaleMatrix = new Matrix();
        scaleMatrix.setScale(ratioX, ratioY, middleX, middleY);

        Canvas canvas = new Canvas(scaledBitmap);
        canvas.setMatrix(scaleMatrix);
        canvas.drawBitmap(bitmap, middleX - bitmap.getWidth() / 2, middleY - bitmap.getHeight()
                / 2, new Paint(Paint.FILTER_BITMAP_FLAG));

        return scaledBitmap;
    }

    private Bitmap getBitmapFromURL(String src) {
        try {
            URL url = new URL(src);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.connect();
            InputStream input = connection.getInputStream();
            return BitmapFactory.decodeStream(input);
        } catch (IOException e) {
            return BitmapFactory.decodeResource(getResources(), R.drawable.avatar_placeholder);
        }
    }

    private void showMessageNotification(String threadId, String text, String name, String title,
                                         Bitmap phoneBitmap, Bitmap wearBitmap) {
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder
                (MyApplication.getContext());
        if (TextUtils.isEmpty(title)) {
            notificationBuilder.setContentTitle(name);
            notificationBuilder.setContentText(text);
        } else {
            notificationBuilder.setContentTitle(title);
            notificationBuilder.setContentText(name + ": " + text);
        }
        notificationBuilder.setAutoCancel(true);
        notificationBuilder.setLargeIcon(phoneBitmap);
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

        NotificationCompat.WearableExtender wearableExtender = new NotificationCompat
                .WearableExtender().setBackground(wearBitmap);
        notificationBuilder.extend(wearableExtender);

        Notification notification = notificationBuilder.build();
        notification.defaults |= Notification.DEFAULT_VIBRATE;
        notification.defaults |= Notification.DEFAULT_SOUND;
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from
                (getApplicationContext());
        notificationManager.notify(ChatActivity.NOTIFICATION_ID_MESSAGE, notification);
    }
}
