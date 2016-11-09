package ca.itquality.patrol.service.wear;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Wearable;

import ca.itquality.patrol.main.MainActivity;
import ca.itquality.patrol.app.MyApplication;
import ca.itquality.patrol.library.util.Util;
import ca.itquality.patrol.library.util.api.ApiClient;
import ca.itquality.patrol.library.util.api.ApiInterface;
import ca.itquality.patrol.util.DeviceUtil;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class WearDataListenerService extends Service implements DataApi.DataListener,
        GoogleApiClient.ConnectionCallbacks {

    private GoogleApiClient mGoogleApiClient;

    @Override
    public void onCreate() {
        super.onCreate();
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .build();
        mGoogleApiClient.connect();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mGoogleApiClient.disconnect();
        Wearable.DataApi.removeListener(mGoogleApiClient, this);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEventBuffer) {
        for (DataEvent event : dataEventBuffer) {
            if (event.getType() == DataEvent.TYPE_CHANGED) {
                DataItem item = event.getDataItem();
                if ((item.getUri().getPath()).
                        equals(Util.PATH_HEART_RATE_HISTORY)) {
                    DataMapItem dataItem = DataMapItem.fromDataItem(event.getDataItem());
                    String heartRateValues = dataItem.getDataMap()
                            .getString(Util.DATA_HEART_RATE_VALUES);
                    uploadHeartRateValuesToServer(heartRateValues);
                } else if ((item.getUri().getPath()).equals(Util.PATH_HEART_RATE)) {
                    DataMapItem dataItem = DataMapItem.fromDataItem(event.getDataItem());
                    int heartRate = dataItem.getDataMap().getInt(Util.DATA_HEART_RATE);
                    DeviceUtil.setHeartRate(heartRate);
                    sendBroadcast(new Intent(MainActivity.HEART_RATE_CHANGED_INTENT)
                            .putExtra(MainActivity.HEART_RATE_EXTRA, heartRate));
                }
            }
        }

    }

    private void uploadHeartRateValuesToServer(String heartRateValues) {
        ApiInterface apiService = ApiClient.getClient().create(ApiInterface.class);
        Call<Void> call = apiService.uploadHeartRate(DeviceUtil.getToken(), heartRateValues);
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Util.Log("Uploaded heart rate data");
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

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Wearable.DataApi.addListener(mGoogleApiClient, this);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }
}