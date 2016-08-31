package ca.itquality.patrol.app;

import android.app.Application;
import android.content.Context;

import ca.itquality.patrol.R;
import uk.co.chrisjenx.calligraphy.CalligraphyConfig;

public class MyApplication extends Application {

    public static Context sContext;

    @Override
    public void onCreate() {
        super.onCreate();

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
