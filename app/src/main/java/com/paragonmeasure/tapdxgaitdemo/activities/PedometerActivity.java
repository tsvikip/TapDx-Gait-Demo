package com.paragonmeasure.tapdxgaitdemo.activities;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.session.AppKeyPair;

import com.paragonmeasure.config.TapDxConst;
import com.paragonmeasure.tapdxgaitdemo.R;
import com.paragonmeasure.tapdxgaitdemo.services.PedometerService;

import java.util.ArrayList;
import java.util.Collections;


public class PedometerActivity extends Activity implements SensorEventListener {

    SharedPreferences sharedPreferences;

    private TextView gaitDemo;
    private TextView totalSteps;
    private Button btnOnOff;
    private Button btnSave;

    private SensorManager mSensorManager;

    private Sensor mStepCounterSensor;

    private Sensor mStepDetectorSensor;

    // In the class declaration section:
    private DropboxAPI<AndroidAuthSession> mDBApi;

    PedometerService mBoundService;
    boolean mIsBound = false;
    boolean zeroSteps = true;
    int previousSteps = 0;
    Typeface futuraHeavyTypeface;
    Typeface futuraBookTypeface;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        setContentView(R.layout.activity_my);
        gaitDemo = (TextView) findViewById(R.id.gait_demo);
        totalSteps = (TextView) findViewById(R.id.TotalSteps);
        btnOnOff = (Button) findViewById(R.id.btn_on_off);
        btnSave = (Button) findViewById(R.id.save_to_dropbox);

        // Dropbox key and secret
        AppKeyPair appKeys = new AppKeyPair(TapDxConst.DROPBOX_KEY, TapDxConst.DROPBOX_SECRET);
        AndroidAuthSession session = new AndroidAuthSession(appKeys);
        mDBApi = new DropboxAPI<AndroidAuthSession>(session);


        mSensorManager = (SensorManager)
                getSystemService(this.SENSOR_SERVICE);
        mStepCounterSensor = mSensorManager
                .getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        mStepDetectorSensor = mSensorManager
                .getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);

        futuraHeavyTypeface = Typeface.createFromAsset(this.getAssets(),
                "fonts/futura_heavy.ttf");
        futuraBookTypeface = Typeface.createFromAsset(this.getAssets(),
                "fonts/futura_book.ttf");

        // Set Typeface
        setTextTypeface();


        // Start oAuth2.0 authentication for Dropbox
        mDBApi.getSession().startOAuth2Authentication(PedometerActivity.this);

        // Shared pref for saving Dropbox access token
        sharedPreferences = this.getSharedPreferences(TapDxConst.TAPDX_SHAREDPREFS,
                Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        // Save Dropbox access token
        editor.putString(TapDxConst.TAPDX_SHAREDPREFS, mDBApi.getSession().getOAuth2AccessToken());

        btnOnOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pedometerOn(v);
            }
        });

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveToDropbox(v);
            }
        });

    }

    @Override
    protected void onStart() {
        super.onStart();
        // Bind to LocalService
        Intent intent = new Intent(this, PedometerService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

   protected void onResume() {
        super.onResume();
//        btnOnOff.setText("ON");
//        btnSave.setClickable(false);
        //Dropbox - Required for finishing authentication
        if (mDBApi.getSession().authenticationSuccessful()) {
            try {
                // Required to complete auth, sets the access token on the session
                mDBApi.getSession().finishAuthentication();

                String accessToken = mDBApi.getSession().getOAuth2AccessToken();
            } catch (IllegalStateException e) {
                Log.i("DbAuthLog", "Error authenticating", e);
            }
        }

    }

    protected void onStop() {
        super.onStop();
//        btnOnOff.setText("ON");
//        btnSave.setClickable(false);
        // Unbind from the service
        if (mIsBound) {
            unbindService(mConnection);
            mIsBound = false;
        }
        mSensorManager.unregisterListener(this, mStepCounterSensor);
        mSensorManager.unregisterListener(this, mStepDetectorSensor);
    }


//    public void onButtonClick(View v) {
//        ArrayList<Long> steps = new ArrayList<>(Collections.singletonList(0L));
//        if (mIsBound) {
//            // Call a method from the LocalService.
//            // However, if this call were something that might hang, then this request should
//            // occur in a separate thread to avoid slowing down the activity performance.
//            steps = mBoundService.pedometerOn();
//            Toast.makeText(this, "Total steps: " + steps, Toast.LENGTH_SHORT).show();
//        }
//    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        Sensor sensor = event.sensor;
        float[] values = event.values;
        ArrayList<Long> steps = new ArrayList<>(Collections.singletonList(0L));
        int value = -1;

        if (values.length > 0) {
            value = (int) values[0];
        }

        if (sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
            if (zeroSteps) {
                zeroSteps = false;
                previousSteps = value;
            }
            totalSteps.setText("Total Steps: " + (value - previousSteps));
        } else if (sensor.getType() == Sensor.TYPE_STEP_DETECTOR) {
            steps.add(event.timestamp);
        }
    }

    public void pedometerOn(View view) {

        PowerManager powerManager = (PowerManager)getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wL = powerManager.
                newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "TapDx Pedometer Demo");
        if (btnOnOff.getText() == "ON") {
            btnOnOff.setText("OFF");
            zeroSteps = true;
            mSensorManager.registerListener(this, mStepCounterSensor,
                    SensorManager.SENSOR_DELAY_FASTEST);
            mSensorManager.registerListener(this, mStepDetectorSensor,
                    SensorManager.SENSOR_DELAY_FASTEST);
            btnSave.setClickable(false);
//            wL.acquire();
        } else {
            btnOnOff.setText("ON");
            mSensorManager.unregisterListener(this, mStepCounterSensor);
            mSensorManager.unregisterListener(this, mStepDetectorSensor);
            btnSave.setClickable(true);
//            wL.release();
        }

    }

    public void saveToDropbox(View view) {
        }
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
    private void setTextTypeface() {
        gaitDemo.setTypeface(futuraHeavyTypeface);
        totalSteps.setTypeface(futuraBookTypeface);
        btnOnOff.setTypeface(futuraBookTypeface);
        btnSave.setTypeface(futuraBookTypeface);
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            // This is called when the connection with the service has been
            // established, giving us the service object we can use to
            // interact with the service.  Because we have bound to a explicit
            // service that we know is running in our own process, we can
            // cast its IBinder to a concrete class and directly access it.
            mBoundService = ((PedometerService.PedometerBinder)service).getService();
            mIsBound = true;
            // Tell the user about this for our demo.
            Toast.makeText(PedometerActivity.this, R.string.pedometer_service_connected,
                    Toast.LENGTH_SHORT).show();
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            // Because it is running in our same process, we should never
            // see this happen.
            mBoundService = null;
            mIsBound = false;
            Toast.makeText(PedometerActivity.this, R.string.local_service_disconnected,
                    Toast.LENGTH_SHORT).show();
        }
    };

    void doBindService() {
        // Establish a connection with the service.  We use an explicit
        // class name because we want a specific service implementation that
        // we know will be running in our own process (and thus won't be
        // supporting component replacement by other applications).
        bindService(new Intent(PedometerActivity.this,
                PedometerService.class), mConnection, Context.BIND_AUTO_CREATE);
        mIsBound = true;
    }

    void doUnbindService() {
        if (mIsBound) {
            // Detach our existing connection.
            unbindService(mConnection);
            mIsBound = false;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        doUnbindService();
    }

}
