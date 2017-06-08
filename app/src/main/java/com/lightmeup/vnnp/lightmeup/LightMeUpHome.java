package com.lightmeup.vnnp.lightmeup;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.SurfaceTexture;
import android.graphics.Typeface;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.text.TextUtils;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.lightmeup.vnnp.lightmeup.analytics.AnalyticsApplication;

import java.util.ArrayList;
import java.util.List;

import yuku.ambilwarna.AmbilWarnaDialog;


public class LightMeUpHome extends AppCompatActivity implements View.OnClickListener {

    public static final String MyPREFERENCES = "myPreference";
    private static final String TAG = "LightUpHome";
    private static final int MY_REQUEST_CODE = 1;
    private static final int MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 11;
    private static final int MY_PERMISSIONS_REQUEST_CAMERA = 2;
    public RoundedView roundedView;
    public SwitchCompat switchComponent;
    public TextView txtFlashLight;
    public TextView txtScreenLight;
    public ImageView imageControl;
    private String colorDefault = "#526A84";
    private SharedPreferences prefs;
    private CameraManager cameraManager;
    private CameraCharacteristics cameraCharacteristics;
    private CameraDevice mCameraDevice;
    private CameraCaptureSession mSession;
    private boolean hasFlash;
    private boolean isFlashOn, isScreenLight, shakeObserved = false;
    private CaptureRequest.Builder mBuilder;
    private SurfaceTexture mSurfaceTexture;
    private boolean isSwitchChecked = false;

    //private boolean onCreateRan = false;


    private Tracker mTracker;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().hide();
        getWindow().setStatusBarColor(Color.parseColor("#0F1C2C"));
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_light_me_up_home);
        roundedView = (RoundedView) findViewById(R.id.roundedView);
        mSurfaceTexture = new SurfaceTexture(1);
        AnalyticsApplication application = (AnalyticsApplication) getApplication();
        mTracker = application.getDefaultTracker();
        Log.d("onCreate","called");
      //  onCreateRan = true;
        txtFlashLight = (TextView) findViewById(R.id.txtflashlight);
        txtScreenLight = (TextView) findViewById(R.id.txtScreenLight);
        txtFlashLight.setOnClickListener(this);
        txtScreenLight.setOnClickListener(this);

        Typeface type = Typeface.createFromAsset(getAssets(), "fonts/" + "Xoxoxa.ttf");
        txtFlashLight.setTypeface(type);
        txtScreenLight.setTypeface(type);

        switchComponent = (SwitchCompat) findViewById(R.id.switchComponent);
        imageControl = (ImageView) findViewById(R.id.imageControl);
        prefs = getSharedPreferences(MyPREFERENCES, Context.MODE_PRIVATE);

        colorDefault = prefs.getString("color", colorDefault);
        roundedView.setCircleColor(Color.parseColor(colorDefault));

        if (getIntent() != null) {
            isScreenLight = getIntent().getBooleanExtra("screenLight", false);
            shakeObserved = getIntent().getBooleanExtra("shakeObserved", false);
        }

        if (isScreenLight) {
            switchComponent.setChecked(true);
            isSwitchChecked = true;
            txtScreenLight.setTextColor(Color.parseColor(colorDefault));
            txtFlashLight.setTextColor(ContextCompat.getColor(this, android.R.color.white));
        } else {
            txtFlashLight.setTextColor(Color.parseColor(colorDefault));
            txtScreenLight.setTextColor(ContextCompat.getColor(this, android.R.color.white));
        }
        switchComponent.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                isSwitchChecked = isChecked;

                if (isFlashOn) {
                    turnOffFlashLight();
                }

                if (isChecked) {
                    setTextStyle(true);
                } else {
                    setTextStyle(false);
                }
            }
        });

        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA,
                        Manifest.permission.READ_EXTERNAL_STORAGE},
                MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
        initCamera();

        hasFlash = getApplicationContext().getPackageManager()
                .hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);

        if (!hasFlash) {
            AlertDialog alert = new AlertDialog.Builder(this)
                    .create();
            alert.setTitle("Error !!");
            alert.setMessage("Your device doesn't support flash light!");
            alert.setButton(DialogInterface.BUTTON_POSITIVE, "OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    // closing the application
                    finish();
                    System.exit(0);
                }
            });
            alert.show();
            return;
        }
        setImageBulb(colorDefault);
        // Switch button click event to toggle flash on/off
        imageControl.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                onActionFlash();
            }
        });
    }

    private void onActionFlash() {
        setImageBulb(colorDefault);
        if (!isSwitchChecked) {
           // initCamera();
            if (isFlashOn) {
                turnOffFlashLight();
                // turn off flash
            } else {
                turnOnFlashLight();
                // turn on flash
            }
        } else {
            turnOffFlashLight();
            showColorDialog();
        }
    }

    private void showColorDialog() {
        int newcolor = Integer.parseInt(prefs.getString("colorHex", "00ff00"), 16);
        AmbilWarnaDialog dialog = new AmbilWarnaDialog(this, 0xff + newcolor, false, new AmbilWarnaDialog.OnAmbilWarnaListener() {
            @Override
            public void onOk(AmbilWarnaDialog dialog, int color) {

                String colorFormat = String.format("0x%08x", color);
                String[] colorCode = colorFormat.split("0xff");
                if (colorCode.length > 0) {
                    String finalColor = colorCode[1];

                    final SharedPreferences.Editor editor = getSharedPreferences(MyPREFERENCES, MODE_PRIVATE).edit();
                    editor.putString("colorHex", finalColor);
                    editor.commit();

                    colorDefault = "#" + finalColor;
                    editor.putString("color", colorDefault);
                    editor.commit();
                    roundedView.setCircleColor(Color.parseColor(colorDefault));
                    setImageBulb(colorDefault);
                    setTextStyle(isSwitchChecked);
                    Intent intent = new Intent(getApplicationContext(), EmptyActivityScreenLight.class);
                    intent.putExtra("color", colorDefault);
                    startActivity(intent);

                }
            }

            @Override
            public void onCancel(AmbilWarnaDialog dialog) {

            }
        });
        dialog.show();
    }

    private void setImageBulb(String colorDefault) {
        if (colorDefault.equalsIgnoreCase("#000000")) {
            imageControl.setImageResource(R.drawable.offstateblue);
        } else {
            imageControl.setImageResource(R.drawable.offstateblack);
        }
    }

 @Override
 public void onRequestPermissionsResult(int requestCode,
                                        String permissions[], int[] grantResults) {
     switch (requestCode) {
         case MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE: {
             // If request is cancelled, the result arrays are empty.
             if (grantResults.length > 0
                     && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                 // permission was granted, yay! Do the
                 // contacts-related task you need to do.
                 if (ContextCompat.checkSelfPermission(this,
                         Manifest.permission.WRITE_EXTERNAL_STORAGE)
                         != PackageManager.PERMISSION_GRANTED) {

                     // Should we show an explanation?
                     if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                             Manifest.permission.CAMERA)) {

                         // Show an expanation to the user *asynchronously* -- don't block
                         // this thread waiting for the user's response! After the user
                         // sees the explanation, try again to request the permission.

                     } else {

                         // No explanation needed, we can request the permission.

                         ActivityCompat.requestPermissions(this,
                                 new String[]{Manifest.permission.CAMERA},
                                 MY_PERMISSIONS_REQUEST_CAMERA);

                         // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                         // app-defined int constant. The callback method gets the
                         // result of the request.
                     }
                 }
             } else {

                 if (ContextCompat.checkSelfPermission(this,
                         Manifest.permission.WRITE_EXTERNAL_STORAGE)
                         != PackageManager.PERMISSION_GRANTED) {

                     // Should we show an explanation?
                     if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                             Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

                         // Show an expanation to the user *asynchronously* -- don't block
                         // this thread waiting for the user's response! After the user
                         // sees the explanation, try again to request the permission.

                     } else {

                         // No explanation needed, we can request the permission.

                         ActivityCompat.requestPermissions(this,
                                 new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE},
                                 MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);

                         // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                         // app-defined int constant. The callback method gets the
                         // result of the request.
                     }
                     // permission denied, boo! Disable the
                     // functionality that depends on this permission.
                 }
             }
             return;
         }
     }
 }
    private void initCamera() {
        cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            String[] id = cameraManager.getCameraIdList();
            if (id != null && id.length > 0) {
                cameraCharacteristics = cameraManager.getCameraCharacteristics(id[0]);
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                cameraManager.openCamera(id[0], new MyCameraDeviceStateCallback(), null);
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private Size getSmallestSize(String cameraId) throws CameraAccessException {
        Size[] outputSizes = cameraManager.getCameraCharacteristics(cameraId).get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(SurfaceTexture.class);
        if (outputSizes == null || outputSizes.length == 0) {
            throw new IllegalStateException("Camera " + cameraId + "doesn't support any outputSize.");
        }
        Size chosen = outputSizes[0];
        for (Size s : outputSizes) {
            if (chosen.getWidth() >= s.getWidth() && chosen.getHeight() >= s.getHeight()) {
                chosen = s;
            }
        }
        return chosen;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    public void turnOnFlashLight() {
        try {
            mBuilder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_TORCH);
            mSession.setRepeatingRequest(mBuilder.build(), null, null);
            isFlashOn = true;
            toggleButtonImage();
        } catch (Exception e) {
            Log.d("CameraException" , e.toString());
        }
    }

    public void turnOffFlashLight() {
        try {
            mBuilder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_OFF);
            mSession.setRepeatingRequest(mBuilder.build(), null, null);
            isFlashOn = false;
            toggleButtonImage();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void close() {
        if (mCameraDevice == null || mSession == null) {
            return;
        }
        mSession.close();
        mCameraDevice.close();
        mCameraDevice = null;
        mSession = null;
    }

    private void toggleButtonImage() {
        if (isFlashOn) {
            imageControl.setImageResource(R.drawable.onstate);
        } else {
            imageControl.setImageResource(R.drawable.offstateblack);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isFlashOn) {
            turnOffFlashLight();
            close();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (isFlashOn) {
            turnOffFlashLight();
        }
        close();
    }

    @Override
    protected void onPause() {
        super.onPause();
       //stopService(new Intent(this, ShakeService.class));
       // mSensorManager.unregisterListener(mShakeDetector);
        if (isFlashOn) {
            turnOffFlashLight();
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();
    }

    @Override
    protected void onResume() {
        super.onResume();


        if (isFlashOn) {
            turnOffFlashLight();
        }
       //
        /*if(onCreateRan){
            onCreateRan = false;
        } else {
            //flashOnshake = true;
            initCamera();
        }*/
        mTracker.setScreenName(TAG);
        mTracker.send(new HitBuilders.ScreenViewBuilder().build());
    }

    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        super.onSaveInstanceState(outState, outPersistentState);

    }

   /* @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (savedInstanceState != null) {
            shakeObserved = savedInstanceState.getBoolean("shakeObserved");
        }
    }*/

    @Override
    protected void onStart() {
        super.onStart();
        // on starting the app get the camera params
        //initCamera();
    }

    private void setTextStyle(boolean checked) {
        if (checked) {
            txtScreenLight.setTextColor(Color.parseColor(colorDefault));
            txtFlashLight.setTextColor(ContextCompat.getColor(this, android.R.color.white));
        } else {
            txtFlashLight.setTextColor(Color.parseColor(colorDefault));
            txtScreenLight.setTextColor(ContextCompat.getColor(this, android.R.color.white));
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.txtflashlight:
                changeSwitch(false);
                break;
            case R.id.txtScreenLight:
                changeSwitch(true);
                break;
        }
    }

    private void changeSwitch(boolean switchState) {
        switchComponent.setChecked(switchState);
    }

    class MyCameraDeviceStateCallback extends CameraDevice.StateCallback {

        @Override
        public void onOpened(CameraDevice camera) {
            mCameraDevice = camera;
            // get builder
            try {
                mBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                List<Surface> list = new ArrayList<Surface>();
                Size size = getSmallestSize(mCameraDevice.getId());
                mSurfaceTexture.setDefaultBufferSize(size.getWidth(), size.getHeight());
                Surface mSurface = new Surface(mSurfaceTexture);
                list.add(mSurface);
                mBuilder.addTarget(mSurface);
                camera.createCaptureSession(list, new MyCameraCaptureSessionStateCallback(), null);

            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onDisconnected(CameraDevice camera) {

        }

        @Override
        public void onError(CameraDevice camera, int error) {

        }
    }

    class MyCameraCaptureSessionStateCallback extends CameraCaptureSession.StateCallback {
        @Override
        public void onConfigured(CameraCaptureSession session) {
            mSession = session;
            try {
                mSession.setRepeatingRequest(mBuilder.build(), null, null);
                onActionFlash();
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onConfigureFailed(CameraCaptureSession session) {

        }
    }

}
