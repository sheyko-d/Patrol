package ca.itquality.patrol.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import ca.itquality.patrol.service.ListenerServiceFromPhone;
import ca.itquality.patrol.service.SensorsService;
import ca.itquality.patrol.service.ShakeListenerService;

public class BootReceiver extends BroadcastReceiver{
    @Override
    public void onReceive(Context context, Intent intent) {
        context.startService(new Intent(context, SensorsService.class));
        context.startService(new Intent(context, ShakeListenerService.class));
        context.startService(new Intent(context, ListenerServiceFromPhone.class));
    }
}
