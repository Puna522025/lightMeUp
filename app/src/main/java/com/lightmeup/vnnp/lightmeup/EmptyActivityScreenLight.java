package com.lightmeup.vnnp.lightmeup;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.lightmeup.vnnp.lightmeup.analytics.AnalyticsApplication;

/**
 * Created by DELL on 8/20/2016.
 */
public class EmptyActivityScreenLight extends AppCompatActivity {

    private static final String TAG = "LightUpEmptyScreen";
    RelativeLayout background;
    ImageView imgClose;
    String colorDefault = "#FFFFFF";
    SeekBar seekbar;
    Context context;
    int Brightness;
    private Tracker mTracker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().hide();
        getWindow().setStatusBarColor((ContextCompat.getColor(this, android.R.color.black)));

        setContentView(R.layout.empty_activity);
        background = (RelativeLayout) findViewById(R.id.background);
        imgClose = (ImageView) findViewById(R.id.imgClose);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        checkSystemWritePermission();
        if (getIntent() != null) {
            colorDefault = getIntent().getStringExtra("color");
            getWindow().setStatusBarColor(Color.parseColor(colorDefault));
        }

        background.setBackgroundColor(Color.parseColor(colorDefault));

        imgClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigateBack();
            }
        });
        seekbar = (SeekBar) findViewById(R.id.seekBar1);
        context = getApplicationContext();
        Brightness = Settings.System.getInt(context.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, 0);

        seekbar.setProgress(Brightness);

        seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                if (checkSystemWritePermission()) {
                    Settings.System.putInt(context.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, i);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        if (colorDefault.equalsIgnoreCase("#000000")) {
            seekbar.getProgressDrawable().setColorFilter(Color.RED, PorterDuff.Mode.SRC_IN);
            seekbar.getThumb().setColorFilter(Color.RED, PorterDuff.Mode.SRC_IN);
        } else {
            seekbar.getProgressDrawable().setColorFilter(Color.BLACK, PorterDuff.Mode.SRC_IN);
            seekbar.getThumb().setColorFilter(Color.BLACK, PorterDuff.Mode.SRC_IN);
        }
        AnalyticsApplication application = (AnalyticsApplication) getApplication();
        mTracker = application.getDefaultTracker();
    }

    private boolean checkSystemWritePermission() {
        boolean retVal = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            retVal = Settings.System.canWrite(this);
            if (!retVal) {
                openAndroidPermissionsMenu();
            }
        }
        return retVal;
    }

    private void openAndroidPermissionsMenu() {
        Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
        intent.setData(Uri.parse("package:" + this.getPackageName()));
        startActivity(intent);
    }

    private void navigateBack() {
        Intent intent = new Intent(getApplicationContext(), LightMeUpHome.class);
        intent.putExtra("screenLight", true);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        navigateBack();
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mTracker.setScreenName(TAG);
        mTracker.send(new HitBuilders.ScreenViewBuilder().build());
    }
}
