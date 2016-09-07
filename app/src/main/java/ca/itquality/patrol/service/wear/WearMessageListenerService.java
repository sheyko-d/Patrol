package ca.itquality.patrol.service.wear;

import android.content.Intent;

import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

import ca.itquality.patrol.app.MyApplication;
import ca.itquality.patrol.auth.LoginActivity;
import ca.itquality.patrol.util.DeviceUtil;

public class WearMessageListenerService extends WearableListenerService {

    private static final String LOGIN_WEAR_PATH = "/stigg-login";
    private static final String BACKUP_WEAR_PATH = "/stigg-backup";
    private static final long MIN_RECEIVED_INTERVAL = 1000 * 10;

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
        }
    }
}