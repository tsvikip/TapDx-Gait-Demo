package com.paragonmeasure.tapdxgaitdemo.services;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.paragonmeasure.tapdxgaitdemo.R;
import com.paragonmeasure.tapdxgaitdemo.activities.PedometerActivity;

import java.util.ArrayList;
import java.util.Collections;

/**
 * Created by Tsviki on 2015-06-17.
 */
public class PedometerService extends Service {
    private NotificationManager mNM;

    private int NOTIFICATION = R.string.pedometer_service_started;

    // This is the object that receives interactions from clients.  See
    // RemoteService for a more complete example.
    private final IBinder mBinder = new PedometerBinder();

    private SensorManager mSensorManager;

    private Sensor mStepCounterSensor;

    private Sensor mStepDetectorSensor;

    public class PedometerBinder extends Binder {
        public PedometerService getService() {
            // Return this instance of PedometerService so clients can call public methods
            return PedometerService.this;
        }
    }

    @Override
    public void onCreate() {
        mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        // Display a notification about us starting.  We put an icon in the status bar.
        showNotification();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("PedometerService", "Received start id " + startId + ": " + intent);
        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        // Cancel the persistent notification.
        mNM.cancel(NOTIFICATION);

        // Tell the user we stopped.
        Toast.makeText(this, R.string.pedometer_service_stopped, Toast.LENGTH_SHORT).show();
    }


    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    /**
     * Show a notification while this service is running.
     */
    private void showNotification() {
        // In this sample, we'll use the same text for the ticker and the expanded notification
        CharSequence text = getText(R.string.pedometer_service_started);

        // Set the icon, scrolling text and timestamp
        Notification notification = new Notification(R.mipmap.ic_notification, text,
                System.currentTimeMillis());

        // The PendingIntent to launch our activity if the user selects this notification
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, PedometerActivity.class), 0);

        // Set the info for the views that show in the notification panel.
        notification.setLatestEventInfo(this, getText(R.string.pedometer_service_label),
                text, contentIntent);

        // Send the notification.
        mNM.notify(NOTIFICATION, notification);
    }


//    public ArrayList pedometerOn() {
//        mSensorManager.registerListener(this, mStepCounterSensor,
//                SensorManager.SENSOR_DELAY_FASTEST);
//        mSensorManager.registerListener(this, mStepDetectorSensor,
//                SensorManager.SENSOR_DELAY_FASTEST);
//    }


    public ArrayList onSensorChanged(SensorEvent event) {
        Sensor sensor = event.sensor;
        float[] values = event.values;
        ArrayList<Long> steps = new ArrayList<>(Collections.singletonList(0L));
        int value = -1;

        if (values.length > 0) {
            value = (int) values[0];
        }

        if (sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
//            totalSteps.setText("Total Steps: " + value);
        } else if (sensor.getType() == Sensor.TYPE_STEP_DETECTOR) {
            steps.add(System.currentTimeMillis());
        }
        return steps;
    }
}
