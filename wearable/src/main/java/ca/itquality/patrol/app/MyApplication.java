package ca.itquality.patrol.app;

import android.app.Application;
import android.content.Context;

import com.squareup.leakcanary.LeakCanary;

import ca.itquality.patrol.R;
import uk.co.chrisjenx.calligraphy.CalligraphyConfig;

public class MyApplication extends Application {

    public static Context sContext;
    public static long lastReceivedMessageTime = -1;

    @Override
    public void onCreate() {
        super.onCreate();
        LeakCanary.install(this);
        CalligraphyConfig.initDefault(new CalligraphyConfig.Builder()
                .setDefaultFontPath("fonts/Roboto-RobotoRegular.ttf")
                .setFontAttrId(R.attr.fontPath)
                .build());

        sContext = this;
    }

    public static Context getContext() {
        return sContext;
    }
}
