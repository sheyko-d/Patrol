package ca.itquality.patrol.service;

import android.Manifest;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Address;
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
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.NotificationCompat;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.widget.Toast;

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
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;
import com.google.firebase.iid.FirebaseInstanceId;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import ca.itquality.patrol.MainActivity;
import ca.itquality.patrol.R;
import ca.itquality.patrol.app.MyApplication;
import ca.itquality.patrol.library.util.Util;
import ca.itquality.patrol.library.util.api.ApiClient;
import ca.itquality.patrol.library.util.api.ApiInterface;
import ca.itquality.patrol.library.util.auth.data.User;
import ca.itquality.patrol.library.util.heartrate.DataValue;
import ca.itquality.patrol.service.wear.WearMessageListenerService;
import ca.itquality.patrol.util.DatabaseManager;
import ca.itquality.patrol.util.DeviceUtil;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BackgroundService extends Service implements GoogleApiClient.ConnectionCallbacks {

    // Constants
    private static final long STEPS_REFRESH_TIME = 1000 * 60 * 5;
    private static final long LOCATION_REFRESH_TIME = 1000 * 60 * 5;
    private static final float LOCATION_REFRESH_DISTANCE = 10;
    private static final long DATA_UPDATE_INTERVAL = 5 * 60 * 1000;
    private static final float AT_WORK_RADIUS = 100;
    private static final int HOUR_DURATION = 1000 * 60 * 60;

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
                    .addScope(Fitness.SCOPE_ACTIVITY_READ_WRITE)
                    .build();
        }
        mGoogleApiClient.connect();

        startUpdateDataTask();
        setWatchMessagesListener(true);
    }

    private void startUpdateDataTask() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                updateProfile();
                uploadStepsHistory();
                uploadActivityHistory();
                uploadQr();
                uploadLocation();
                mHandler.postDelayed(this, DATA_UPDATE_INTERVAL);
            }
        });
    }

    private void setWatchMessagesListener(boolean enabled) {
        if (enabled) {
            Wearable.MessageApi.addListener(mGoogleApiClient,
                    new WearMessageListenerService());
        } else {
            Wearable.MessageApi.removeListener(mGoogleApiClient,
                    new WearMessageListenerService());
        }
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
                .setSamplingRate(3, TimeUnit.SECONDS)
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

                    if (steps == -1) return;

                    if (DeviceUtil.getSteps() == -1 || DeviceUtil.getSteps() != steps) {
                        sendBroadcast(new Intent(MainActivity.STEPS_CHANGED_INTENT)
                                .putExtra(MainActivity.STEPS_EXTRA, steps));

                        storeStepsInDb(steps);
                    }

                    updateWearSteps(steps);
                }
            }
        }
    };

    private void storeStepsInDb(int steps) {
        DatabaseManager.initializeInstance(new DatabaseManager.SitesDatabaseHelper
                (MyApplication.getContext()));
        DatabaseManager databaseManager = DatabaseManager.getInstance();
        SQLiteDatabase database = databaseManager.openDatabase();
        if (!database.isOpen()) return;
        ContentValues contentValues = new ContentValues();
        contentValues.put(DatabaseManager.STEPS_VALUE_COLUMN, steps);
        contentValues.put(DatabaseManager.STEPS_TIME_COLUMN, System.currentTimeMillis());
        contentValues.put(DatabaseManager.STEPS_IS_SENT_COLUMN, false);
        database.insert(DatabaseManager.STEPS_TABLE, null, contentValues);
        databaseManager.closeDatabase();
    }

    private void uploadStepsHistory() {
        DatabaseManager.initializeInstance(new DatabaseManager.SitesDatabaseHelper
                (MyApplication.getContext()));
        DatabaseManager databaseManager = DatabaseManager.getInstance();
        SQLiteDatabase database = databaseManager.openDatabase();
        if (!database.isOpen()) return;
        Cursor cursor = database.query(DatabaseManager.STEPS_TABLE, new String[]{
                        DatabaseManager.STEPS_TIME_COLUMN,
                        DatabaseManager.STEPS_VALUE_COLUMN
                }, DatabaseManager.STEPS_IS_SENT_COLUMN + "=?", new String[]{"0"}, null, null,
                DatabaseManager.STEPS_TIME_COLUMN);
        ArrayList<DataValue> values = new ArrayList<>();
        while (cursor.moveToNext()) {
            values.add(new DataValue(cursor.getLong(0), cursor.getString(1)));
        }

        if (cursor.getCount() == 0) return;
        postStepsToServer(values);

        // Mark values as sent
        ContentValues contentValues = new ContentValues();
        contentValues.put(DatabaseManager.STEPS_IS_SENT_COLUMN, true);
        database.update(DatabaseManager.STEPS_TABLE, contentValues, null, null);
        cursor.close();
        databaseManager.closeDatabase();
    }

    private void postStepsToServer(ArrayList<DataValue> values) {
        ApiInterface apiService = ApiClient.getClient().create(ApiInterface.class);
        Call<Void> call = apiService.uploadSteps(DeviceUtil.getToken(),
                Util.parseJsonArray(values).toString());
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Util.Log("Uploaded steps data");
                } else {
                    if (response.code() == 400) {
                        Toast.makeText(MyApplication.getContext(), "Some fields are empty.",
                                Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(MyApplication.getContext(), "Server error.",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void uploadActivityHistory() {
        DatabaseManager.initializeInstance(new DatabaseManager.SitesDatabaseHelper(this));
        SQLiteDatabase database = DatabaseManager.getInstance().openDatabase();
        if (!database.isOpen()) return;
        Cursor cursor = database.query(DatabaseManager.ACTIVITY_TABLE, new String[]{
                        DatabaseManager.ACTIVITY_TIME_COLUMN,
                        DatabaseManager.ACTIVITY_VALUE_COLUMN
                }, DatabaseManager.ACTIVITY_IS_SENT_COLUMN + "=?", new String[]{"0"}, null, null,
                DatabaseManager.ACTIVITY_TIME_COLUMN);
        ArrayList<DataValue> values = new ArrayList<>();
        while (cursor.moveToNext()) {
            values.add(new DataValue(cursor.getLong(0), cursor.getString(1)));
        }

        if (cursor.getCount() == 0) return;
        postActivityToServer(values);

        // Mark values as sent
        ContentValues contentValues = new ContentValues();
        contentValues.put(DatabaseManager.ACTIVITY_IS_SENT_COLUMN, true);
        database.update(DatabaseManager.ACTIVITY_TABLE, contentValues, null, null);
        cursor.close();
        DatabaseManager.getInstance().closeDatabase();
    }

    private void postActivityToServer(ArrayList<DataValue> values) {
        ApiInterface apiService = ApiClient.getClient().create(ApiInterface.class);
        Call<Void> call = apiService.uploadActivity(DeviceUtil.getToken(),
                Util.parseJsonArray(values).toString());
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Util.Log("Uploaded activity data");
                } else {
                    if (response.code() == 400) {
                        Toast.makeText(MyApplication.getContext(), "Some fields are empty.",
                                Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(MyApplication.getContext(), "Server error.",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void uploadQr() {
        DatabaseManager.initializeInstance(new DatabaseManager.SitesDatabaseHelper(this));
        SQLiteDatabase database = DatabaseManager.getInstance().openDatabase();
        if (!database.isOpen()) return;
        Cursor cursor = database.query(DatabaseManager.QR_TABLE, new String[]{
                        DatabaseManager.QR_TIME_COLUMN,
                        DatabaseManager.QR_VALUE_COLUMN
                }, DatabaseManager.QR_IS_SENT_COLUMN + "=?", new String[]{"0"}, null, null,
                DatabaseManager.QR_TIME_COLUMN);
        ArrayList<DataValue> values = new ArrayList<>();
        while (cursor.moveToNext()) {
            values.add(new DataValue(cursor.getLong(0), cursor.getString(1)));
        }

        if (cursor.getCount() == 0) return;
        postQrToServer(values);

        // Mark values as sent
        ContentValues contentValues = new ContentValues();
        contentValues.put(DatabaseManager.QR_IS_SENT_COLUMN, true);
        database.update(DatabaseManager.QR_TABLE, contentValues, null, null);
        cursor.close();
        DatabaseManager.getInstance().closeDatabase();
    }

    private void postQrToServer(ArrayList<DataValue> values) {
        ApiInterface apiService = ApiClient.getClient().create(ApiInterface.class);
        Call<Void> call = apiService.uploadQr(DeviceUtil.getToken(),
                Util.parseJsonArray(values).toString());
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Util.Log("Uploaded qr");
                } else {
                    if (response.code() == 400) {
                        Toast.makeText(MyApplication.getContext(), "Some fields are empty.",
                                Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(MyApplication.getContext(), "Server error.",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void uploadLocation() {
        DatabaseManager.initializeInstance(new DatabaseManager.SitesDatabaseHelper(this));
        SQLiteDatabase database = DatabaseManager.getInstance().openDatabase();
        if (!database.isOpen()) return;
        Cursor cursor = database.query(DatabaseManager.LOCATION_TABLE, new String[]{
                        DatabaseManager.LOCATION_TIME_COLUMN,
                        DatabaseManager.LOCATION_VALUE_COLUMN
                }, DatabaseManager.LOCATION_IS_SENT_COLUMN + "=?", new String[]{"0"}, null, null,
                DatabaseManager.LOCATION_TIME_COLUMN);
        ArrayList<DataValue> values = new ArrayList<>();
        while (cursor.moveToNext()) {
            values.add(new DataValue(cursor.getLong(0), cursor.getString(1)));
        }

        if (cursor.getCount() == 0) return;
        postLocationToServer(values);

        // Mark values as sent
        ContentValues contentValues = new ContentValues();
        contentValues.put(DatabaseManager.LOCATION_IS_SENT_COLUMN, true);
        database.update(DatabaseManager.LOCATION_TABLE, contentValues, null, null);
        cursor.close();
        DatabaseManager.getInstance().closeDatabase();
    }

    private void postLocationToServer(ArrayList<DataValue> values) {
        ApiInterface apiService = ApiClient.getClient().create(ApiInterface.class);
        Call<Void> call = apiService.uploadLocation(DeviceUtil.getToken(),
                Util.parseJsonArray(values).toString());
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Util.Log("Uploaded location");
                } else {
                    if (response.code() == 400) {
                        Toast.makeText(MyApplication.getContext(), "Some fields are empty.",
                                Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(MyApplication.getContext(), "Server error.",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateProfile() {
        String googleToken = FirebaseInstanceId.getInstance().getToken();

        ApiInterface apiService = ApiClient.getClient().create(ApiInterface.class);
        Call<User> call = apiService.updateProfile(DeviceUtil.getUserId(), DeviceUtil.getToken(),
                googleToken);
        call.enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                if (response.isSuccessful()) {
                    User user = response.body();
                    DeviceUtil.updateProfile(user);
                    updateShift();
                }
            }

            @Override
            public void onFailure(Call<User> call, Throwable t) {
                Toast.makeText(MyApplication.getContext(), "Server error.",
                        Toast.LENGTH_SHORT).show();
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

            // Show a clock in alert notification if user is not at work in time
            if (!userAtWork() && !DeviceUtil.clockInShown(weekStartTime, currentShift)
                    && System.currentTimeMillis() - weekStartTime + currentShift.getStartTime()
                    < HOUR_DURATION) {
                showClockInNotification(weekStartTime, currentShift);
            } else if (!DeviceUtil.startConfirmed(weekStartTime, currentShift)) {
                String shiftStarted = DateUtils.getRelativeDateTimeString
                        (MyApplication.getContext(), weekStartTime + currentShift.getStartTime(),
                                System.currentTimeMillis(), 0, 0).toString()
                        .toLowerCase(Locale.getDefault());
                sendBroadcast(new Intent(MainActivity.CONFIRM_SHIFT_START_INTENT)
                        .putExtra(MainActivity.SHIFT_STARTED_EXTRA, shiftStarted)
                        .putExtra(MainActivity.CLOCK_IN_SHIFT_ID_EXTRA,
                                currentShift.getAssignedShiftId()));
                DeviceUtil.setStartConfirmed(weekStartTime, currentShift);
            }
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

        // Check if some shift is added and user didn't clock out
        if (currentShift == null) {
            User.AssignedShift previousShift = DeviceUtil.getPreviousShift(timeSinceWeekStart);
            if (!DeviceUtil.endConfirmed(weekStartTime, previousShift)) {
                if (previousShift != null) {
                    String shiftEnded = DateUtils.getRelativeDateTimeString
                            (MyApplication.getContext(), weekStartTime + previousShift.getEndTime(),
                                    System.currentTimeMillis(), 0, 0).toString()
                            .toLowerCase(Locale.getDefault());
                    sendBroadcast(new Intent(MainActivity.CONFIRM_SHIFT_END_INTENT)
                            .putExtra(MainActivity.SHIFT_ENDED_EXTRA, shiftEnded)
                            .putExtra(MainActivity.CLOCK_IN_SHIFT_ID_EXTRA,
                                    previousShift.getAssignedShiftId()));

                    DeviceUtil.setEndConfirmed(weekStartTime, previousShift);
                }
            }
        }

        if (!TextUtils.isEmpty(shiftTitleTxt) && !TextUtils.isEmpty(shiftTxt)) {
            sendBroadcast(new Intent(MainActivity.SHIFT_CHANGED_INTENT)
                    .putExtra(MainActivity.SHIFT_TITLE_EXTRA, shiftTitleTxt)
                    .putExtra(MainActivity.SHIFT_EXTRA, shiftTxt));

            updateWearShift(shiftTitleTxt, shiftTxt);
        }
    }

    /**
     * Checks if user has an active shift and is close enough to the assigned object.
     */
    private boolean userAtWork() {
        LatLng myLatLng = DeviceUtil.getMyLocation();
        Location myLocation = new Location("MyLocation");
        myLocation.setLatitude(myLatLng.latitude);
        myLocation.setLongitude(myLatLng.longitude);

        Location workLocation = new Location("WorkLocation");
        User.AssignedObject assignedObject = DeviceUtil.getUser().getAssignedObject();
        if (assignedObject != null) {
            workLocation.setLatitude(assignedObject.getLatitude());
            workLocation.setLongitude(assignedObject.getLongitude());

            return myLocation.distanceTo(workLocation) < AT_WORK_RADIUS;
        } else {
            return true;
        }
    }

    public static void showClockInNotification(Long weekStartTime,
                                               User.AssignedShift currentShift) {
        final NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder
                (MyApplication.getContext());
        notificationBuilder.setContentTitle("Your shift started " + DateUtils
                .getRelativeTimeSpanString(weekStartTime + currentShift.getStartTime(),
                        System.currentTimeMillis(), 0).toString().toLowerCase(Locale.getDefault()));
        notificationBuilder.setContentText("Please let us know why you're not at work...");
        notificationBuilder.setAutoCancel(true);
        notificationBuilder.setSmallIcon(R.drawable.backup_notification);
        notificationBuilder.setColor(ContextCompat.getColor(MyApplication.getContext(),
                R.color.colorPrimary));
        notificationBuilder.setPriority(NotificationCompat.PRIORITY_HIGH);

        Intent notifyIntent = new Intent(MyApplication.getContext(), MainActivity.class);
        notifyIntent.putExtra(MainActivity.CLOCK_IN_EXTRA, true);
        notifyIntent.putExtra(MainActivity.CLOCK_IN_SHIFT_ID_EXTRA,
                currentShift.getAssignedShiftId());
        // Sets the Activity to start in a new, empty task
        notifyIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_CLEAR_TASK);
        // Creates the PendingIntent
        PendingIntent pendingIntent =
                PendingIntent.getActivity(
                        MyApplication.getContext(),
                        0,
                        notifyIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        notificationBuilder.setContentIntent(pendingIntent);

        Notification notification = notificationBuilder.build();
        notification.defaults |= Notification.DEFAULT_VIBRATE;
        notification.defaults |= Notification.DEFAULT_SOUND;
        final NotificationManagerCompat notificationManager = NotificationManagerCompat.from
                (MyApplication.getContext());
        notificationManager.notify(Util.NOTIFICATION_ID_CLOCK_IN, notification);

        DeviceUtil.setClockInShown(weekStartTime, currentShift);
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

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        listenForSteps();
        setLocationListener();
    }

    private void setLocationListener() {
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                LOCATION_REFRESH_TIME, LOCATION_REFRESH_DISTANCE, new LocationListener() {
                    @Override
                    public void onLocationChanged(final Location location) {
                        DeviceUtil.setMyLocation((float) location.getLatitude(),
                                (float) location.getLongitude());

                        storeLocationInDb(location);

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

    private void storeLocationInDb(Location location) {
        try {
            DatabaseManager.initializeInstance(new DatabaseManager.SitesDatabaseHelper
                    (MyApplication.getContext()));
            DatabaseManager databaseManager = DatabaseManager.getInstance();
            SQLiteDatabase database = databaseManager.openDatabase();
            if (!database.isOpen()) return;
            ContentValues contentValues = new ContentValues();
            contentValues.put(DatabaseManager.LOCATION_VALUE_COLUMN, new JSONObject()
                    .put("latitude", location.getLatitude())
                    .put("longitude", location.getLongitude()).toString());
            contentValues.put(DatabaseManager.LOCATION_TIME_COLUMN, System.currentTimeMillis());
            contentValues.put(DatabaseManager.LOCATION_IS_SENT_COLUMN, false);
            database.insert(DatabaseManager.LOCATION_TABLE, null, contentValues);
            databaseManager.closeDatabase();
        } catch (Exception e) {
            Util.Log("Can't save my location: " + e);
        }
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
                DeviceUtil.setAddress(addressTxt);
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
            Util.Log("update wear shift: " + shift);
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
            Util.Log("update wear steps: " + steps);
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
        setWatchMessagesListener(false);
        super.onDestroy();
    }
}
