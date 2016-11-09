package ca.itquality.patrol.service;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.text.format.DateFormat;
import android.util.Base64;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.util.Calendar;
import java.util.Locale;

import ca.itquality.patrol.R;
import ca.itquality.patrol.library.util.Util;
import ca.itquality.patrol.util.WearUtil;

import static ca.itquality.patrol.service.ListenerServiceFromPhone.sLastStillTime;

public class AnalogWatchFaceService extends CanvasWatchFaceService {

    private View myLayout;
    private TextView mTimeTxt;
    private TextView mAmpmTxt;
    private TextView mNameTxt;

    private TextView mWeekdayTxt;
    private TextView mDayTxt;
    private TextView mWeatherTxt;
    private ImageView mWeatherImg;
    private ImageView mCountdownImg;
    private int mRemindedActivity = 0;

    @Override
    public Engine onCreateEngine() {
        /* provide your watch face implementation */
        return new Engine();
    }

    /* implement service callback methods */
    private class Engine extends CanvasWatchFaceService.Engine {

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);
            initWatchFace();
            setWeatherListener();
            if (sLastStillTime == -1) {
                sLastStillTime = System.currentTimeMillis();
            }
            startTimer();
        }

        private void startTimer() {
            new Runnable() {
                @Override
                public void run() {
                    if (sLastStillTime != -1) {
                        if (System.currentTimeMillis() - sLastStillTime < 60 * 1000) {
                            mCountdownImg.setImageResource(R.drawable.countdown_0);
                            mRemindedActivity = 0;
                        } else if (System.currentTimeMillis() - sLastStillTime < 2 * 60 * 1000) {
                            mCountdownImg.setImageResource(R.drawable.countdown_1);
                            mRemindedActivity = 0;
                        } else if (System.currentTimeMillis() - sLastStillTime < 3 * 60 * 1000) {
                            mCountdownImg.setImageResource(R.drawable.countdown_2);
                            mRemindedActivity = 0;
                        } else if (System.currentTimeMillis() - sLastStillTime < 4 * 60 * 1000) {
                            mCountdownImg.setImageResource(R.drawable.countdown_3);
                            mRemindedActivity = 0;
                        } else if (System.currentTimeMillis() - sLastStillTime < 5 * 60 * 1000) {
                            mCountdownImg.setImageResource(R.drawable.countdown_4);
                            mRemindedActivity = 0;
                        } else if (System.currentTimeMillis() - sLastStillTime < 6 * 60 * 1000) {
                            mCountdownImg.setImageResource(R.drawable.countdown_5);
                            mRemindedActivity = 0;
                        } else if (System.currentTimeMillis() - sLastStillTime < 7 * 60 * 1000) {
                            mCountdownImg.setImageResource(R.drawable.countdown_6);
                            mRemindedActivity = 0;
                        } else if (System.currentTimeMillis() - sLastStillTime < 8 * 60 * 1000) {
                            mCountdownImg.setImageResource(R.drawable.countdown_7);
                            mRemindedActivity = 0;
                        } else if (System.currentTimeMillis() - sLastStillTime < 9 * 60 * 1000) {
                            mCountdownImg.setImageResource(R.drawable.countdown_8);
                            mRemindedActivity = 0;
                        } else if (System.currentTimeMillis() - sLastStillTime < 10 * 60 * 1000) {
                            mCountdownImg.setImageResource(R.drawable.countdown_9);
                            mRemindedActivity = 0;
                        } else if (System.currentTimeMillis() - sLastStillTime < 11 * 60 * 1000) {
                            mCountdownImg.setImageResource(R.drawable.countdown_10);
                            mRemindedActivity = 0;
                        } else if (System.currentTimeMillis() - sLastStillTime < 12 * 60 * 1000) {
                            mCountdownImg.setImageResource(R.drawable.countdown_11);
                            mRemindedActivity = 0;
                        } else if (System.currentTimeMillis() - sLastStillTime < 13 * 60 * 1000) {
                            mCountdownImg.setImageResource(R.drawable.countdown_12);
                            mRemindedActivity = 0;
                        } else if (System.currentTimeMillis() - sLastStillTime < 14 * 60 * 1000) {
                            mCountdownImg.setImageResource(R.drawable.countdown_13);
                            mRemindedActivity = 0;
                        } else if (System.currentTimeMillis() - sLastStillTime < 15 * 60 * 1000) {
                            mCountdownImg.setImageResource(R.drawable.countdown_14);
                            mRemindedActivity = 0;
                        } else {
                            mCountdownImg.setImageResource(R.drawable.countdown_15);
                            if (mRemindedActivity < 3) {
                                Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                                v.vibrate(1000);
                                Toast.makeText(getApplicationContext(), "Time to move",
                                        Toast.LENGTH_SHORT).show();
                                mRemindedActivity++;
                            }
                        }
                        Util.Log("timer check = " + (System.currentTimeMillis() - sLastStillTime));
                    }

                    new Handler().postDelayed(this, 10 * 1000);
                    // TODO: 60*1000
                }
            }.run();
        }

        private void setWeatherListener() {
            BroadcastReceiver receiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    int temperature = intent.getIntExtra
                            (ListenerServiceFromPhone.EXTRA_WEATHER_TEMPERATURE, 0);
                    mWeatherTxt.setText(getString(R.string.weather_temperature,
                            temperature));
                    mWeatherImg.setImageBitmap(ListenerServiceFromPhone.sIcon);
                    WearUtil.setWeatherIcon(encodeToBase64(ListenerServiceFromPhone.sIcon));
                    invalidate();
                }
            };
            registerReceiver(receiver, new IntentFilter
                    (ListenerServiceFromPhone.INTENT_WEATHER_UPDATE));
        }

        private String encodeToBase64(Bitmap image) {
            ByteArrayOutputStream byteArrayOS = new ByteArrayOutputStream();
            image.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOS);
            return Base64.encodeToString(byteArrayOS.toByteArray(), Base64.DEFAULT);
        }

        private Bitmap decodeBase64(String input) {
            byte[] decodedBytes = Base64.decode(input, 0);
            return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
        }

        @SuppressLint("InflateParams")
        private void initWatchFace() {
            LayoutInflater inflater = (LayoutInflater) getSystemService
                    (Context.LAYOUT_INFLATER_SERVICE);
            myLayout = inflater.inflate(R.layout.watch_face, null);
            Point displaySize = new Point();
            Display display = ((WindowManager) getSystemService(Context.WINDOW_SERVICE))
                    .getDefaultDisplay();
            display.getSize(displaySize);

            int specW = View.MeasureSpec.makeMeasureSpec(displaySize.x,
                    View.MeasureSpec.EXACTLY);
            int specH = View.MeasureSpec.makeMeasureSpec(displaySize.y,
                    View.MeasureSpec.EXACTLY);

            myLayout.measure(specW, specH);
            myLayout.layout(0, 0, myLayout.getMeasuredWidth(),
                    myLayout.getMeasuredHeight());

            mTimeTxt = (TextView) myLayout.findViewById(R.id.watch_time_txt);
            mTimeTxt.setTypeface(Typeface.createFromAsset(getAssets(),
                    "fonts/digital.ttf"));
            mAmpmTxt = (TextView) myLayout.findViewById(R.id.watch_ampm_txt);
            mAmpmTxt.setTypeface(Typeface.createFromAsset(getAssets(),
                    "fonts/digital.ttf"));

            mNameTxt = (TextView) myLayout.findViewById(R.id.watch_name_txt);
            mNameTxt.setTypeface(Typeface.createFromAsset(getAssets(),
                    "fonts/digital.ttf"));
            mNameTxt.setText(WearUtil.getInitials(WearUtil.getName()));

            mWeatherTxt = (TextView) myLayout.findViewById(R.id.watch_weather_txt);
            mWeatherTxt.setTypeface(Typeface.createFromAsset(getAssets(),
                    "fonts/digital.ttf"));
            mWeatherTxt.setText(getString(R.string.weather_temperature,
                    WearUtil.getWeatherTemperature()));

            mCountdownImg = (ImageView) myLayout.findViewById(R.id.watch_countdown_img);

            mWeatherImg = (ImageView) myLayout.findViewById(R.id.watch_weather_img);
            try {
                mWeatherImg.setImageBitmap(decodeBase64(WearUtil.getWeatherIcon()));
            } catch (Exception e) {
            }

            mDayTxt = (TextView) myLayout.findViewById(R.id.watch_day_txt);
            mDayTxt.setTypeface(Typeface.createFromAsset(getAssets(),
                    "fonts/digital.ttf"));

            mWeekdayTxt = (TextView) myLayout.findViewById(R.id.watch_weekday_txt);
            mWeekdayTxt.setTypeface(Typeface.createFromAsset(getAssets(),
                    "fonts/digital.ttf"));

            updateTime();
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            /* get device features (burn-in, low-bit ambient) */
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            updateTime();
        }

        private void updateTime() {
            mTimeTxt.setText(DateFormat.format("hh:mm", Calendar.getInstance().getTime()));
            mAmpmTxt.setText(DateFormat.format("aaa", Calendar.getInstance().getTime()));
            mWeekdayTxt.setText(DateFormat.format("EEE", Calendar.getInstance().getTime())
                    .toString().toUpperCase(Locale.getDefault()));
            mDayTxt.setText(DateFormat.format("dd", Calendar.getInstance().getTime()));
            invalidate();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            /* the wearable switched between modes */
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            canvas.drawColor(Color.BLACK);
            myLayout.draw(canvas);
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);
            /* the watch face became visible or invisible */
        }
    }
}