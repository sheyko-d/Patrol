package ca.itquality.patrol.alert;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import butterknife.Bind;
import butterknife.ButterKnife;
import ca.itquality.patrol.R;
import ca.itquality.patrol.util.DeviceUtil;

public class AlertActivity extends AppCompatActivity {

    // Constants
    public static final String EXTRA_NAME = "Name";
    public static final String EXTRA_LATITUDE = "Latitude";
    public static final String EXTRA_LONGITUDE = "Longitude";
    public static final int NOTIFICATION_ID_ALERT = 1;

    // Views
    @Bind(R.id.toolbar)
    Toolbar mToolbar;

    // Usual variables
    private String mName;
    private Double mLatitude;
    private Double mLongitude;
    private Marker mBackupMarker;
    private GoogleMap mMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alert);
        ButterKnife.bind(this);

        getExtras();
        initActionBar();
        initMap();
    }

    private void getExtras() {
        mName = getIntent().getStringExtra(EXTRA_NAME);
        mLatitude = getIntent().getDoubleExtra(EXTRA_LATITUDE, 0);
        mLongitude = getIntent().getDoubleExtra(EXTRA_LONGITUDE, 0);
    }

    private void initActionBar() {
        setSupportActionBar(mToolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(mName + " needs backup!");
        }
    }

    private void initMap() {
        mToolbar.post(new Runnable() {
            @Override
            public void run() {
                initMapFragment();
            }
        });
    }

    private void initMapFragment() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.alert_map);
        mapFragment.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap map) {
                mMap = map;
                enableMyLocationOnMap();

                if (mBackupMarker != null) {
                    mBackupMarker.remove();
                }
                mBackupMarker = map.addMarker(new MarkerOptions()
                        .position(new LatLng(mLatitude, mLongitude))
                        .title(mName)
                        .snippet("I need help!"));

                LatLngBounds.Builder builder = new LatLngBounds.Builder();
                builder.include(mBackupMarker.getPosition());
                builder.include(DeviceUtil.getMyLocation());
                LatLngBounds bounds = builder.build();
                int padding = DeviceUtil.MAP_PADDING; // offset from edges of the map in pixels
                CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, padding);
                map.moveCamera(cu);
            }
        });
    }

    private void enableMyLocationOnMap() {
        if (!(ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission
                (this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED)) {
            mMap.setMyLocationEnabled(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        finish();
        return true;
    }
}
