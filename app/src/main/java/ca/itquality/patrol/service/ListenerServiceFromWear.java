package ca.itquality.patrol.service;

import android.content.Intent;

import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

import ca.itquality.patrol.auth.LoginActivity;
import ca.itquality.patrol.library.util.Util;
import ca.itquality.patrol.util.DeviceUtil;

public class ListenerServiceFromWear extends WearableListenerService {

    private static final String LOGIN_WEAR_PATH = "/stigg-login";

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        Util.Log("message received on phone");
        /*
         * Receive the message from wear
         */
        if (messageEvent.getPath().equals(LOGIN_WEAR_PATH)) {
            if (!DeviceUtil.isLoggedIn()) {
                Intent startIntent = new Intent(this, LoginActivity.class);
                startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(startIntent);
            }
        }
    }
}