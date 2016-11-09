package ca.itquality.patrol.auth;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import org.json.JSONObject;

import java.util.Arrays;

import butterknife.Bind;
import butterknife.ButterKnife;
import ca.itquality.patrol.main.MainActivity;
import ca.itquality.patrol.R;
import ca.itquality.patrol.app.MyApplication;
import ca.itquality.patrol.library.util.auth.data.User;
import ca.itquality.patrol.library.util.Util;
import ca.itquality.patrol.library.util.api.ApiClient;
import ca.itquality.patrol.library.util.api.ApiInterface;
import ca.itquality.patrol.service.ActivityRecognizedService;
import ca.itquality.patrol.service.BackgroundService;
import ca.itquality.patrol.util.DeviceUtil;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

public class LoginActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    // Views
    @Bind(R.id.login_email_edit_txt)
    EditText mEmailEditTxt;
    @Bind(R.id.login_password_edit_txt)
    EditText mPasswordEditTxt;
    @Bind(R.id.login_btn)
    Button mButton;
    @Bind(R.id.login_facebook_btn)
    Button mFacebookBtn;

    // Usual variables
    private CallbackManager mCallbackManager;
    private GoogleApiClient mGoogleApiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        ButterKnife.bind(this);

        logOut();
        stopServices();
        initStatusBar();
        initFacebookBtn();
        initGoogleClient();
    }

    private void stopServices() {
        stopService(new Intent(this, BackgroundService.class));
        stopService(new Intent(this, ActivityRecognizedService.class));
    }

    private void logOut() {
        // Log out from Facebook
        LoginManager.getInstance().logOut();

        // Clear all saved shared preferences
        PreferenceManager.getDefaultSharedPreferences(MyApplication.getContext()).edit().clear()
                .apply();
    }

    private void initGoogleClient() {
        //Connect the GoogleApiClient
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(ActivityRecognition.API)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
    }

    /**
     * Makes the status bar transparent.
     */
    private void initStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        }
    }

    private void initFacebookBtn() {
        mCallbackManager = CallbackManager.Factory.create();

        LoginManager.getInstance().registerCallback(mCallbackManager,
                new FacebookCallback<LoginResult>() {
                    @Override
                    public void onSuccess(LoginResult loginResult) {
                        GraphRequest request = GraphRequest.newMeRequest(
                                loginResult.getAccessToken(),
                                new GraphRequest.GraphJSONObjectCallback() {
                                    @Override
                                    public void onCompleted(JSONObject object,
                                                            GraphResponse response) {
                                        Util.Log("object: " + object.toString());

                                        String email;
                                        try {
                                            email = object.getString("email");
                                        } catch (Exception e) {
                                            email = null;
                                        }

                                        if (TextUtils.isEmpty(email)) {
                                            Toast.makeText(LoginActivity.this, "Please set up" +
                                                            "an email address for your Facebook" +
                                                            "account",
                                                    Toast.LENGTH_SHORT).show();
                                            setFacebookProgressBarVisible(false);
                                            return;
                                        }

                                        try {
                                            String userId = object.getString("id");
                                            String name = object.getString("name");
                                            String photo = "https://graph.facebook.com/" + userId
                                                    + "/picture?type=large";

                                            signInOnServer(userId, name, email, photo);
                                        } catch (Exception e) {
                                            Toast.makeText(LoginActivity.this, "Some data is" +
                                                            "missing from your Facebook account",
                                                    Toast.LENGTH_SHORT).show();
                                            setFacebookProgressBarVisible(false);
                                        }
                                    }
                                });
                        Bundle parameters = new Bundle();
                        parameters.putString("fields", "id,name,email,gender,birthday");
                        request.setParameters(parameters);
                        request.executeAsync();
                    }

                    @Override
                    public void onCancel() {
                        setFacebookProgressBarVisible(false);
                    }

                    @Override
                    public void onError(FacebookException exception) {
                        Toast.makeText(LoginActivity.this, "Can't log in with Facebook",
                                Toast.LENGTH_SHORT).show();
                        Util.Log("Facebook error: " + exception.getMessage());
                        setFacebookProgressBarVisible(false);
                    }
                });
    }

    private void signInOnServer(String id, String name, String email, String photo) {
        ApiInterface apiService = ApiClient.getClient().create(ApiInterface.class);
        Call<User> call = apiService.signInFacebook(id, name, email, photo);
        call.enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                if (response.isSuccessful()) {
                    User user = response.body();
                    Util.Log("assigned_shifts size: " + user.getAssignedShifts().size());
                    DeviceUtil.updateProfile(user);
                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                    finish();
                } else {
                    if (response.code() == 400) {
                        Toast.makeText(LoginActivity.this, "Some fields are empty.",
                                Toast.LENGTH_SHORT).show();
                    } else if (response.code() == 409) {
                        Toast.makeText(LoginActivity.this, "Can't create a new user.",
                                Toast.LENGTH_SHORT).show();
                    }
                    setFacebookProgressBarVisible(false);
                }
            }

            @Override
            public void onFailure(Call<User> call, Throwable t) {
                Toast.makeText(LoginActivity.this, "Server error.",
                        Toast.LENGTH_SHORT).show();
                setFacebookProgressBarVisible(false);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mCallbackManager.onActivityResult(requestCode, resultCode, data);
    }

    /**
     * Required for the font library.
     */
    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

    public void onRegisterButtonClicked(View view) {
        startActivity(new Intent(this, RegisterActivity.class));
        finish();
    }

    public void onLoginButtonClicked(View view) {
        String email = mEmailEditTxt.getText().toString();
        String password = mPasswordEditTxt.getText().toString();
        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(LoginActivity.this, "Some fields are empty", Toast.LENGTH_SHORT)
                    .show();
        } else {
            setProgressBarVisible(true);

            ApiInterface apiService = ApiClient.getClient().create(ApiInterface.class);
            Call<User> call = apiService.login(email, password);
            call.enqueue(new Callback<User>() {
                @Override
                public void onResponse(Call<User> call, Response<User> response) {
                    if (response.isSuccessful()) {
                        User user = response.body();
                        DeviceUtil.updateProfile(user);
                        startActivity(new Intent(LoginActivity.this, MainActivity.class));
                        finish();
                    } else {
                        if (response.code() == 403) {
                            Toast.makeText(LoginActivity.this, "Incorrect email or password.",
                                    Toast.LENGTH_SHORT).show();
                        } else if (response.code() == 400) {
                            Toast.makeText(LoginActivity.this, "Some fields are empty",
                                    Toast.LENGTH_SHORT).show();
                        }
                        setProgressBarVisible(false);
                    }
                }

                @Override
                public void onFailure(Call<User> call, Throwable t) {
                    Toast.makeText(LoginActivity.this, "Server error.",
                            Toast.LENGTH_SHORT).show();
                    setProgressBarVisible(false);
                }
            });
        }
    }

    private void setProgressBarVisible(boolean visible) {
        mButton.setText(visible ? R.string.auth_loading : R.string.login_btn);
        mButton.setEnabled(!visible);
        mEmailEditTxt.setEnabled(!visible);
        mPasswordEditTxt.setEnabled(!visible);
    }

    private void setFacebookProgressBarVisible(boolean visible) {
        mFacebookBtn.setText(visible ? R.string.login_facebook_loading : R.string.login_facebook);
        mFacebookBtn.setEnabled(!visible);
    }

    public void onFacebookButtonClicked(View view) {
        setFacebookProgressBarVisible(true);
        LoginManager.getInstance().logInWithReadPermissions(this, Arrays.asList("public_profile",
                "email"));
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        logOutOnTheWatch();
    }

    private void logOutOnTheWatch() {
        PutDataMapRequest putDataMapReq = PutDataMapRequest.create(Util.PATH_LOGGED_IN);
        putDataMapReq.setUrgent();
        putDataMapReq.getDataMap().putBoolean(Util.DATA_LOGGED_IN, false);
        PutDataRequest putDataReq = putDataMapReq.asPutDataRequest();
        Wearable.DataApi.putDataItem(mGoogleApiClient, putDataReq);
    }

    @Override
    public void onConnectionSuspended(int i) {
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Util.Log("connection failed");
    }
}
