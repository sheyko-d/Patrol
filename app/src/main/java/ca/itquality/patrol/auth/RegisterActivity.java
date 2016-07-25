package ca.itquality.patrol.auth;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import butterknife.Bind;
import butterknife.ButterKnife;
import ca.itquality.patrol.MainActivity;
import ca.itquality.patrol.R;
import ca.itquality.patrol.api.ApiClient;
import ca.itquality.patrol.api.ApiInterface;
import ca.itquality.patrol.auth.data.User;
import ca.itquality.patrol.library.util.Util;
import ca.itquality.patrol.util.DeviceUtil;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

public class RegisterActivity extends AppCompatActivity {

    // Views
    @Bind(R.id.register_name_edit_txt)
    EditText mNameEditTxt;
    @Bind(R.id.register_email_edit_txt)
    EditText mEmailEditTxt;
    @Bind(R.id.register_password_edit_txt)
    EditText mPasswordEditTxt;
    @Bind(R.id.register_password_repeat_edit_txt)
    EditText mPasswordRepeatEditTxt;
    @Bind(R.id.register_btn)
    Button mButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        ButterKnife.bind(this);

        initStatusBar();
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

    /**
     * Required for the font library.
     */
    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

    public void onLoginButtonClicked(View view) {
        finish();
        startActivity(new Intent(this, LoginActivity.class));
    }

    public void onRegisterButtonClicked(View view) {
        String name = mNameEditTxt.getText().toString();
        String email = mEmailEditTxt.getText().toString();
        String password = mPasswordEditTxt.getText().toString();
        String passwordRepeat = mPasswordRepeatEditTxt.getText().toString();
        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(email) || TextUtils.isEmpty(password)
                || TextUtils.isEmpty(passwordRepeat)) {
            Toast.makeText(RegisterActivity.this, "Some fields are empty", Toast.LENGTH_SHORT)
                    .show();
        } else if (!password.equals(passwordRepeat)) {
            Toast.makeText(RegisterActivity.this, "Passwords don't match", Toast.LENGTH_SHORT)
                    .show();
        } else if (!DeviceUtil.isValidEmail(email)) {
            Toast.makeText(RegisterActivity.this, "Email is not valid", Toast.LENGTH_SHORT)
                    .show();
        } else if (password.length() < DeviceUtil.MIN_PASSWORD_LENGTH) {
            Toast.makeText(RegisterActivity.this, "Password should be at least 6 characters long",
                    Toast.LENGTH_SHORT).show();
        } else {
            setProgressBarVisible(true);
            ApiInterface apiService = ApiClient.getClient().create(ApiInterface.class);

            Call<User> call = apiService.register(name, email, password, null);
            call.enqueue(new Callback<User>() {
                @Override
                public void onResponse(Call<User> call, Response<User> response) {
                    if (response.isSuccessful()) {
                        User user = response.body();
                        Util.Log("user id: " + user.getUserId());
                        DeviceUtil.updateProfile(user.getToken(), user.getUserId(),
                                user.getAssignedObject(), user.getName(), user.getEmail(),
                                user.getPhoto());
                        startActivity(new Intent(RegisterActivity.this, MainActivity.class));
                        finish();
                    } else {
                        if (response.code() == 403) {
                            Toast.makeText(RegisterActivity.this, "Email is already taken.",
                                    Toast.LENGTH_SHORT).show();
                        } else if (response.code() == 400) {
                            Toast.makeText(RegisterActivity.this, "Some fields are empty.",
                                    Toast.LENGTH_SHORT).show();
                        } else if (response.code() == 409) {
                            Toast.makeText(RegisterActivity.this, "Can't create a new user.",
                                    Toast.LENGTH_SHORT).show();
                        }
                        setProgressBarVisible(false);
                    }
                }

                @Override
                public void onFailure(Call<User> call, Throwable t) {
                    Toast.makeText(RegisterActivity.this, "Error, check your internet connection.",
                            Toast.LENGTH_SHORT).show();
                    setProgressBarVisible(false);
                }
            });
        }
    }

    private void setProgressBarVisible(boolean visible) {
        mButton.setText(visible ? R.string.auth_loading : R.string.register_btn);
        mButton.setEnabled(!visible);
        mNameEditTxt.setEnabled(!visible);
        mEmailEditTxt.setEnabled(!visible);
        mPasswordEditTxt.setEnabled(!visible);
        mPasswordRepeatEditTxt.setEnabled(!visible);
    }

    @Override
    public void onBackPressed() {
        finish();
        startActivity(new Intent(this, LoginActivity.class));
    }
}
