package ca.itquality.patrol.main;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import ca.itquality.patrol.R;
import ca.itquality.patrol.app.MyApplication;
import ca.itquality.patrol.library.util.Util;
import ca.itquality.patrol.library.util.api.ApiClient;
import ca.itquality.patrol.library.util.api.ApiInterface;
import ca.itquality.patrol.util.DeviceUtil;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ClockInDayOffActivity extends AppCompatActivity {

    public static final String EXTRA_SHIFT_ID = "ShiftId";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_clock_in_day_off);

        final String shiftId = getIntent().getStringExtra(EXTRA_SHIFT_ID);

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this,
                R.style.MaterialDialogStyle);
        dialogBuilder.setTitle("Please explain the reason");
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_clock_in, null);
        final EditText editTxt = (EditText) dialogView.findViewById(R.id.clock_in_edit_txt);
        dialogBuilder.setView(dialogView);
        dialogBuilder.setCancelable(false);
        dialogBuilder.setPositiveButton("Send", null);
        final AlertDialog dialog = dialogBuilder.create();
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                        .setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                String reason = editTxt.getText().toString();
                                if (TextUtils.isEmpty(reason)) {
                                    Toast.makeText(ClockInDayOffActivity.this, "Reason is empty",
                                            Toast.LENGTH_SHORT).show();
                                    return;
                                }

                                postClockInReason(shiftId, reason);

                                dialog.cancel();
                                Toast.makeText(ClockInDayOffActivity.this, "Thank you!",
                                        Toast.LENGTH_SHORT).show();
                                finish();
                            }
                        });
            }
        });
        dialog.show();

        // Cancel the notification
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from
                (MyApplication.getContext());
        notificationManager.cancel(Util.NOTIFICATION_ID_CLOCK_IN);
    }

    private void postClockInReason(String shiftId, String reason) {
        ApiInterface apiService = ApiClient.getClient().create(ApiInterface.class);
        Call<Void> call = apiService.postClockInReason(DeviceUtil.getToken(),
                shiftId, reason);
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Util.Log("Posted the clock in reason");
                } else {
                    if (response.code() == 400) {
                        Toast.makeText(MyApplication.getContext(),
                                "Some fields are empty.",
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
}
