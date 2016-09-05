package ca.itquality.patrol.settings;


import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.widget.Toast;

import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.location.places.ui.PlacePicker;

import ca.itquality.patrol.R;
import ca.itquality.patrol.library.util.api.ApiClient;
import ca.itquality.patrol.library.util.api.ApiInterface;
import ca.itquality.patrol.app.MyApplication;
import ca.itquality.patrol.auth.LoginActivity;
import ca.itquality.patrol.library.util.auth.data.User;
import ca.itquality.patrol.util.DeviceUtil;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@SuppressLint("ValidFragment")
class SettingsFragment extends PreferenceFragment {

    // Constants
    static final int PLACE_PICKER_REQUEST_CODE = 1;

    // Usual variables
    private static Preference sAssignedObjectPreference;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings);

        initAssignedObjectPreference();
        initLogOutPreference();
    }

    private void initLogOutPreference() {
        Preference logOutPreference = findPreference("setting_log_out");
        logOutPreference.setOnPreferenceClickListener(new Preference
                .OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference pref) {
                startActivity(new Intent(getActivity(), LoginActivity.class));
                getActivity().finish();
                return true;
            }
        });
    }

    private void initAssignedObjectPreference() {
        sAssignedObjectPreference = findPreference("setting_assigned_object");
        sAssignedObjectPreference.setSummary(DeviceUtil.getAssignedObjectTitle());

        sAssignedObjectPreference.setOnPreferenceClickListener(new Preference
                .OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference pref) {
                openPlacePicker();
                return true;
            }
        });
    }

    private void openPlacePicker() {
        try {
            PlacePicker.IntentBuilder intentBuilder =
                    new PlacePicker.IntentBuilder();
            Intent intent = intentBuilder.build(getActivity());
            getActivity().startActivityForResult(intent, PLACE_PICKER_REQUEST_CODE);
        } catch (GooglePlayServicesRepairableException
                | GooglePlayServicesNotAvailableException e) {
            Toast.makeText(getActivity(), "Can't open a place picker",
                    Toast.LENGTH_SHORT).show();
        }
    }

    static void createPlace(String title, double latitude, double longitude) {
        setProgressBarVisible(true);

        ApiInterface apiService = ApiClient.getClient().create(ApiInterface.class);
        Call<User.AssignedObject> call = apiService.assignUser(DeviceUtil.getUserId(), title,
                latitude, longitude);
        call.enqueue(new Callback<User.AssignedObject>() {
            @Override
            public void onResponse(Call<User.AssignedObject> call,
                                   Response<User.AssignedObject> response) {
                if (response.isSuccessful()) {
                    User.AssignedObject assignedObject = response.body();
                    DeviceUtil.updateAssignedObject(assignedObject);
                    Toast.makeText(MyApplication.getContext(), "New place is assigned!",
                            Toast.LENGTH_SHORT).show();
                } else {
                    if (response.code() == 400) {
                        Toast.makeText(MyApplication.getContext(), "Some fields are empty.",
                                Toast.LENGTH_SHORT).show();
                    } else if (response.code() == 409) {
                        Toast.makeText(MyApplication.getContext(), "Can't create a new place.",
                                Toast.LENGTH_SHORT).show();
                    }
                }
                setProgressBarVisible(false);
            }

            @Override
            public void onFailure(Call<User.AssignedObject> call, Throwable t) {
                Toast.makeText(MyApplication.getContext(), "Server error.",
                        Toast.LENGTH_SHORT).show();
                setProgressBarVisible(false);
            }
        });
    }

    private static void setProgressBarVisible(boolean visible) {
        sAssignedObjectPreference.setSummary(visible ? "Loading..."
                : DeviceUtil.getAssignedObjectTitle());
    }
}