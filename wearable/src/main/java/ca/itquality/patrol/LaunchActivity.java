package ca.itquality.patrol;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.wearable.activity.WearableActivity;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.util.Date;

import butterknife.Bind;
import butterknife.ButterKnife;
import ca.itquality.patrol.library.util.Util;
import ca.itquality.patrol.service.ListenerServiceFromPhone;
import ca.itquality.patrol.util.WearUtil;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

public class LaunchActivity extends WearableActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    @Bind(R.id.launch_disconnected_layout)
    View mDisconnectedLayout;
    @Bind(R.id.launch_login_layout)
    View mLoginLayout;
    @Bind(R.id.launch_progress_bar)
    ProgressBar mProgressBar;
    @Bind(R.id.main_clock_txt)
    TextView mClockView;

    // Constants
    private static final int REQUEST_RESOLVE_ERROR = 1001;
    private static final String LOGIN_WEAR_PATH = "/stigg-login";
    private static final String DIALOG_ERROR = "dialog_error";

    // Usual variables
    private Node mNode;
    private GoogleApiClient mGoogleApiClient;
    private boolean mResolvingError = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launch);
        ButterKnife.bind(this);

        initGoogleClient();
        updateTime();
        startPhoneListenerService();
        registerLoginStateListener();


        // TODO: Improve login listener
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    private void registerLoginStateListener() {
        IntentFilter filter = new IntentFilter(MainActivity.INTENT_LOGIN_STATE);
        registerReceiver(mLoginStateReceiver, filter);
    }

    BroadcastReceiver mLoginStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // If user logged in on the phone, log in on the watch as well
            if (intent.getBooleanExtra(MainActivity.LOGIN_STATE_EXTRA, true)) {
                startActivity(new Intent(LaunchActivity.this, MainActivity.class));
                finish();
            }
        }
    };

    private void startPhoneListenerService() {
        startService(new Intent(this, ListenerServiceFromPhone.class));
    }

    private void initGoogleClient() {
        //Connect the GoogleApiClient
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
    }

    /**
     * Send message to mobile handheld
     */
    private void openLoginOnPhone() {
        if (mNode != null && mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            Wearable.MessageApi.sendMessage(
                    mGoogleApiClient, mNode.getId(), LOGIN_WEAR_PATH, null).setResultCallback(

                    new ResultCallback<MessageApi.SendMessageResult>() {
                        @Override
                        public void onResult(@NonNull MessageApi.SendMessageResult
                                                     sendMessageResult) {

                            if (!sendMessageResult.getStatus().isSuccess()) {
                                Util.Log("Failed to send message with status code: "
                                        + sendMessageResult.getStatus().getStatusCode());
                            } else {
                                Util.Log("sent message");
                            }
                        }
                    }
            );
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!mResolvingError) {
            mGoogleApiClient.connect();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        mGoogleApiClient.disconnect();
    }

    /*
         * Resolve the node = the connected device to send the message to
         */
    private void connectToPhone() {
        mProgressBar.setVisibility(View.VISIBLE);
        Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).setResultCallback
                (new ResultCallback<NodeApi.GetConnectedNodesResult>() {
                    @Override
                    public void onResult(@NonNull NodeApi.GetConnectedNodesResult nodes) {
                        for (Node node : nodes.getNodes()) {
                            mNode = node;
                        }

                        Util.Log("Check node");
                        if (mNode != null) {
                            if (!WearUtil.isLoggedIn()) {
                                mDisconnectedLayout.setVisibility(View.GONE);
                                mLoginLayout.setVisibility(View.VISIBLE);
                                openLoginOnPhone();
                            } else {
                                startActivity(new Intent(LaunchActivity.this, MainActivity.class));
                                finish();
                            }
                        } else {
                            mDisconnectedLayout.setVisibility(View.VISIBLE);
                            mLoginLayout.setVisibility(View.GONE);
                        }
                        mProgressBar.setVisibility(View.GONE);
                    }
                });
    }

    @Override
    public void onConnected(Bundle bundle) {
        Util.Log("connected");
        connectToPhone();
    }

    @Override
    public void onConnectionSuspended(int i) {
        Util.Log("Google connection suspended");
        // TODO:
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult result) {
        Util.Log("Google connection failed: " + result.getErrorCode());
        if (mResolvingError) {
            // Already attempting to resolve an error.
            return;
        } else if (result.hasResolution()) {
            try {
                mResolvingError = true;
                result.startResolutionForResult(this, REQUEST_RESOLVE_ERROR);
            } catch (IntentSender.SendIntentException e) {
                // There was an error with the resolution intent. Try again.
                mGoogleApiClient.connect();
            }
        } else {
            // Show dialog using GoogleApiAvailability.getErrorDialog()
            showErrorDialog(result.getErrorCode());
            mResolvingError = true;
        }
    }

    /**
     * Creates a dialog for an error message
     */
    private void showErrorDialog(int errorCode) {
        // Create a fragment for the error dialog
        ErrorDialogFragment dialogFragment = new ErrorDialogFragment();
        // Pass the error that should be displayed
        Bundle args = new Bundle();
        args.putInt(DIALOG_ERROR, errorCode);
        dialogFragment.setArguments(args);
        dialogFragment.show(getFragmentManager(), "errordialog");
    }

    /**
     * Called from ErrorDialogFragment when the dialog is dismissed.
     */
    public void onDialogDismissed() {
        mResolvingError = false;
    }

    /**
     * A fragment to display an error dialog
     */
    public static class ErrorDialogFragment extends DialogFragment {
        public ErrorDialogFragment() {
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Get the error code and retrieve the appropriate dialog
            int errorCode = this.getArguments().getInt(DIALOG_ERROR);
            return GoogleApiAvailability.getInstance().getErrorDialog(
                    this.getActivity(), errorCode, REQUEST_RESOLVE_ERROR);
        }

        @Override
        public void onDismiss(DialogInterface dialog) {
            ((LaunchActivity) getActivity()).onDialogDismissed();
        }
    }

    /**
     * Required for the font library.
     */
    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

    private void updateTime() {
        mClockView.setText(WearUtil.AMBIENT_DATE_FORMAT.format(new Date()));
    }

    @Override
    public void onEnterAmbient(Bundle ambientDetails) {
        super.onEnterAmbient(ambientDetails);
        updateDisplay();
    }

    @Override
    public void onUpdateAmbient() {
        super.onUpdateAmbient();
        updateDisplay();
    }

    @Override
    public void onExitAmbient() {
        updateDisplay();
        super.onExitAmbient();
    }

    private void updateDisplay() {
        if (isAmbient()) {
            updateTime();
        }
    }

    @Override
    protected void onDestroy() {
        try {
            unregisterReceiver(mLoginStateReceiver);
        } catch (Exception e) {
            // Receiver not registered
        }
        super.onDestroy();
    }
}