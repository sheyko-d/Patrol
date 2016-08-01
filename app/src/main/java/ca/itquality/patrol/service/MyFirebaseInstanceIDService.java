package ca.itquality.patrol.service;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;

import ca.itquality.patrol.library.util.Util;

public class MyFirebaseInstanceIDService extends FirebaseInstanceIdService {
    @Override
    public void onTokenRefresh() {
        String refreshedToken = FirebaseInstanceId.getInstance().getToken();
        Util.Log("Refreshed token: " + refreshedToken);

        sendMessagingTokenToServer(refreshedToken);
    }

    private void sendMessagingTokenToServer(String refreshedToken) {

    }
}
