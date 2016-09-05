package ca.itquality.patrol.service;

import android.Manifest;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.text.TextUtils;
import android.text.format.DateUtils;

import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataSource;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.request.DataSourcesRequest;
import com.google.android.gms.fitness.request.OnDataPointListener;
import com.google.android.gms.fitness.request.SensorRequest;
import com.google.android.gms.fitness.result.DailyTotalResult;
import com.google.android.gms.fitness.result.DataSourcesResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import ca.itquality.patrol.MainActivity;
import ca.itquality.patrol.R;
import ca.itquality.patrol.library.util.Util;
import ca.itquality.patrol.library.util.auth.data.User;
import ca.itquality.patrol.util.DeviceUtil;

public class BackgroundService extends Service implements GoogleApiClient.ConnectionCallbacks {

    // Constants
    private static final long STEPS_REFRESH_TIME = 1000 * 60 * 5;
    private static final long LOCATION_REFRESH_TIME = 1000 * 60 * 5;
    private static final float LOCATION_REFRESH_DISTANCE = 0;
    private static final long SHIFT_UPDATE_INTERVAL = 60 * 1000;

    // Usual variables
    private static GoogleApiClient mGoogleApiClient;
    private Handler mHandler = new Handler();

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        // Create an instance of GoogleAPIClient.
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addApi(Wearable.API)
                    .addApi(LocationServices.API)
                    .addApi(Fitness.SENSORS_API)
                    .addApi(Fitness.HISTORY_API)
                    .addScope(new Scope(Scopes.FITNESS_ACTIVITY_READ_WRITE))
                    .build();
        }
        mGoogleApiClient.connect();

        startShiftUpdateTask();
        listenForSteps();
    }

    private void listenForSteps() {
        DataSourcesRequest dataSourceRequest = new DataSourcesRequest.Builder()
                .setDataTypes(DataType.TYPE_STEP_COUNT_CUMULATIVE)
                .setDataSourceTypes(DataSource.TYPE_RAW)
                .build();

        ResultCallback<DataSourcesResult> dataSourcesResultCallback
                = new ResultCallback<DataSourcesResult>() {
            @Override
            public void onResult(@NonNull DataSourcesResult dataSourcesResult) {
                for (DataSource dataSource : dataSourcesResult.getDataSources()) {
                    if (DataType.TYPE_STEP_COUNT_CUMULATIVE.equals(dataSource.getDataType())) {
                        registerFitnessDataListener(dataSource, DataType.TYPE_STEP_COUNT_CUMULATIVE);
                    }
                }
            }
        };

        Fitness.SensorsApi.findDataSources(mGoogleApiClient, dataSourceRequest)
                .setResultCallback(dataSourcesResultCallback);
    }

    private void registerFitnessDataListener(DataSource dataSource, DataType dataType) {
        SensorRequest request = new SensorRequest.Builder()
                .setDataSource(dataSource)
                .setDataType(dataType)
                .setSamplingRate(STEPS_REFRESH_TIME, TimeUnit.MICROSECONDS)
                .build();

        Fitness.SensorsApi.add(mGoogleApiClient, request, mStepsListener);
    }

    private OnDataPointListener mStepsListener = new OnDataPointListener() {
        @Override
        public void onDataPoint(DataPoint dataPoint) {
            PendingResult<DailyTotalResult> result = Fitness.HistoryApi.readDailyTotal
                    (mGoogleApiClient, DataType.AGGREGATE_STEP_COUNT_DELTA);
            DailyTotalResult totalResult = result.await(30, TimeUnit.SECONDS);
            if (totalResult.getStatus().isSuccess()) {
                DataSet totalSet = totalResult.getTotal();
                if (totalSet != null) {
                    int steps = totalSet.isEmpty() ? -1 : totalSet.getDataPoints().get(0)
                            .getValue(Field.FIELD_STEPS).asInt();
                    sendBroadcast(new Intent(MainActivity.STEPS_CHANGED_INTENT)
                            .putExtra(MainActivity.STEPS_EXTRA, steps));

                    updateWearSteps(steps);
                }
            }
        }
    };

    private void startShiftUpdateTask() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                updateShift();
                mHandler.postDelayed(this, SHIFT_UPDATE_INTERVAL);
            }
        });
    }

    private void updateShift() {
        Calendar calendar = Calendar.getInstance();
        long currentTime = calendar.getTimeInMillis();
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.HOUR_OF_DAY, 24);
        calendar.set(Calendar.DAY_OF_WEEK, calendar.getFirstDayOfWeek());
        long weekStartTime = calendar.getTimeInMillis();
        calendar.add(Calendar.WEEK_OF_MONTH, 1);
        long nextWeekStartTime = calendar.getTimeInMillis();
        long timeSinceWeekStart = currentTime - weekStartTime;

        Util.Log(DateUtils.formatDateTime(this, weekStartTime,
                DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_TIME));

        User.AssignedShift currentShift = DeviceUtil.getCurrentShift(timeSinceWeekStart);
        User.AssignedShift nextShift = DeviceUtil.getNextShift(timeSinceWeekStart);
        User.AssignedShift nextWeekShift = DeviceUtil.getNextWeekShift();

        String shiftTitleTxt = null;
        String shiftTxt = null;
        if (currentShift != null) {
            // Display the current shift
            if (currentShift.getName().toLowerCase(Locale.getDefault()).contains("shift")) {
                shiftTitleTxt = currentShift.getName();
            } else {
                shiftTitleTxt = getString(R.string.main_shift, currentShift.getName());
            }
            shiftTxt = getString(R.string.main_shift_current,
                    DateUtils.getRelativeTimeSpanString(weekStartTime + currentShift.getEndTime(),
                            System.currentTimeMillis(), 0).toString()
                            .toLowerCase(Locale.getDefault()));
        } else if (nextShift != null) {
            // Display the next shift in a week
            if (nextShift.getName().toLowerCase(Locale.getDefault()).contains("shift")) {
                shiftTitleTxt = nextShift.getName();
            } else {
                shiftTitleTxt = getString(R.string.main_shift, nextShift.getName());
            }
            shiftTxt = getString(R.string.main_shift_next,
                    DateUtils.getRelativeTimeSpanString(weekStartTime + nextShift.getStartTime(),
                            System.currentTimeMillis(), 0).toString()
                            .toLowerCase(Locale.getDefault()));
        } else if (nextWeekShift != null) {
            // Display the next shift in a week
            if (nextWeekShift.getName().toLowerCase(Locale.getDefault()).contains("shift")) {
                shiftTitleTxt = nextWeekShift.getName();
            } else {
                shiftTitleTxt = getString(R.string.main_shift, nextWeekShift.getName());
            }
            shiftTxt = getString(R.string.main_shift_next,
                    DateUtils.getRelativeTimeSpanString(nextWeekStartTime
                            + nextWeekShift.getStartTime(), System.currentTimeMillis(), 0)
                            .toString().toLowerCase(Locale.getDefault()));
        }
        if (!TextUtils.isEmpty(shiftTitleTxt) && !TextUtils.isEmpty(shiftTxt)) {
            sendBroadcast(new Intent(MainActivity.SHIFT_CHANGED_INTENT)
                    .putExtra(MainActivity.SHIFT_TITLE_EXTRA, shiftTitleTxt)
                    .putExtra(MainActivity.SHIFT_EXTRA, shiftTxt));

            updateWearShift(shiftTitleTxt, shiftTxt);
        }
    }

    private void updateWearLocation(String address) {
        try {
            PutDataMapRequest putDataMapReq = PutDataMapRequest.create(Util.PATH_LOCATION);
            putDataMapReq.setUrgent();
            putDataMapReq.getDataMap().putString(Util.DATA_LOCATION, address);
            putDataMapReq.getDataMap().putLong(Util.DATA_TIME, System.currentTimeMillis());
            PutDataRequest putDataReq = putDataMapReq.asPutDataRequest();
            Wearable.DataApi.putDataItem(mGoogleApiClient, putDataReq);
            Util.Log("Update wear location: " + address);
        } catch (Exception e) {
            // Watch is not supported
        }
    }

    private void getDailySteps() {
        // TODO:
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        getDailySteps();
        setLocationListener();
    }

    private void setLocationListener() {
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        locationManager.requestLocationUpdates(locationManager.getBestProvider(criteria, true),
                LOCATION_REFRESH_TIME, LOCATION_REFRESH_DISTANCE, new LocationListener() {
                    @Override
                    public void onLocationChanged(final Location location) {
                        DeviceUtil.setMyLocation((float) location.getLatitude(),
                                (float) location.getLongitude());

                        getAddress(location);
                    }

                    @Override
                    public void onStatusChanged(String s, int i, Bundle bundle) {
                    }

                    @Override
                    public void onProviderEnabled(String s) {
                    }

                    @Override
                    public void onProviderDisabled(String s) {
                    }
                });
    }

    private void getAddress(Location location) {
        // Get the location passed to this service through an extra.
        List<Address> addresses = null;

        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            addresses = geocoder.getFromLocation(
                    location.getLatitude(),
                    location.getLongitude(),
                    // In this sample, get just a single address.
                    1);
        } catch (Exception exception) {
            //return null;
        }

        // Handle case where no address was found.
        if (!(addresses == null || addresses.size() == 0)) {
            Address address = addresses.get(0);
            ArrayList<String> addressFragments = new ArrayList<>();

            // Fetch the address lines using getAddressLine,
            // join them, and send them to the thread.
            for (int i = 0; i < address.getMaxAddressLineIndex(); i++) {
                addressFragments.add(address.getAddressLine(i));
            }

            String addressTxt = addressFragments.get(0);
            if (!TextUtils.isEmpty(addressTxt)) {
                sendBroadcast(new Intent(MainActivity.LOCATION_CHANGED_INTENT)
                        .putExtra(MainActivity.LOCATION_ADDRESS_EXTRA, addressFragments.get(0)));
                updateWearLocation(addressTxt);
            }
        }
    }



    private void updateWearShift(String shiftTitle, String shift) {
        try {
            PutDataMapRequest putDataMapReq = PutDataMapRequest.create(Util.PATH_SHIFT);
            putDataMapReq.setUrgent();
            putDataMapReq.getDataMap().putString(Util.DATA_SHIFT_TITLE, shiftTitle);
            putDataMapReq.getDataMap().putString(Util.DATA_SHIFT, shift);
            putDataMapReq.getDataMap().putLong(Util.DATA_TIME, System.currentTimeMillis());
            PutDataRequest putDataReq = putDataMapReq.asPutDataRequest();
            Util.Log("update wear shift: "+shift);
            Wearable.DataApi.putDataItem(mGoogleApiClient, putDataReq);
        } catch (Exception e) {
            // Watch is not supported
        }
    }

    private void updateWearSteps(int steps) {
        try {
            PutDataMapRequest putDataMapReq = PutDataMapRequest.create(Util.PATH_STEPS);
            putDataMapReq.setUrgent();
            putDataMapReq.getDataMap().putInt(Util.DATA_STEPS, steps);
            putDataMapReq.getDataMap().putLong(Util.DATA_TIME, System.currentTimeMillis());
            PutDataRequest putDataReq = putDataMapReq.asPutDataRequest();
            Wearable.DataApi.putDataItem(mGoogleApiClient, putDataReq);
            Util.Log("update wear steps: "+steps);
        } catch (Exception e) {
            // Watch is not supported
        }
    }

    public static void updateWearActivityStatus(final String activity) {
        try {
            PutDataMapRequest putDataMapReq = PutDataMapRequest.create(Util.PATH_ACTIVITY);
            putDataMapReq.setUrgent();
            putDataMapReq.getDataMap().putString(Util.DATA_ACTIVITY, activity);
            putDataMapReq.getDataMap().putLong(Util.DATA_TIME, System.currentTimeMillis());
            PutDataRequest putDataReq = putDataMapReq.asPutDataRequest();
            Wearable.DataApi.putDataItem(mGoogleApiClient, putDataReq);
        } catch (Exception e) {
            // Watch is not supported
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
    }

    @Override
    public void onDestroy() {
        Fitness.SensorsApi.remove(mGoogleApiClient, mStepsListener)
                .setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(@NonNull Status status) {
                        if (status.isSuccess()) {
                            try {
                                mGoogleApiClient.disconnect();
                            } catch (Exception e) {
                                // Google client is already destroyed
                            }
                        }
                    }
                });
        super.onDestroy();
    }
}
