package ca.itquality.patrol.settings;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlacePicker;

import butterknife.Bind;
import butterknife.ButterKnife;
import ca.itquality.patrol.MainActivity;
import ca.itquality.patrol.R;

import static ca.itquality.patrol.settings.SettingsFragment.PLACE_PICKER_REQUEST_CODE;

public class SettingsActivity extends AppCompatActivity {

    // Views
    @Bind(R.id.toolbar)
    Toolbar mToolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        ButterKnife.bind(this);

        initActionBar();
        initSettings();
    }

    private void initSettings() {
        getFragmentManager().beginTransaction().replace(R.id.settings_layout,
                new SettingsFragment()).commit();
    }

    private void initActionBar() {
        setSupportActionBar(mToolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    protected void onActivityResult(int requestCode,
                                    int resultCode, Intent data) {
        if (requestCode == PLACE_PICKER_REQUEST_CODE
                && resultCode == Activity.RESULT_OK) {

            final Place place = PlacePicker.getPlace(this, data);

            SettingsFragment.createPlace(place.getName().toString(), place.getLatLng().latitude,
                    place.getLatLng().longitude);
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onBackPressed() {
        finish();
        startActivity(new Intent(this, MainActivity.class));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        finish();
        startActivity(new Intent(this, MainActivity.class));
        return true;
    }
}
