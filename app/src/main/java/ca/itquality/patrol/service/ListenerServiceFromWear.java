package ca.itquality.patrol.service;

import android.content.Intent;

import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

import ca.itquality.patrol.auth.LoginActivity;
import ca.itquality.patrol.library.util.Util;

public class ListenerServiceFromWear extends WearableListenerService {

    private static final String HELLO_WORLD_WEAR_PATH = "/stigg-wear";

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        Util.Log("message received");
        /*
         * Receive the message from wear
         */
        if (messageEvent.getPath().equals(HELLO_WORLD_WEAR_PATH)) {
            Intent startIntent = new Intent(this, LoginActivity.class);
            startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(startIntent);
        }
    }

}