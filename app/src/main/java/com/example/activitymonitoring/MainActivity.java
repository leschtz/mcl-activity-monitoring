package com.example.activitymonitoring;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    private SensorManager sensorManager;
    private Sensor sensorAccelerometer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        sensorAccelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (sensorAccelerometer == null) {
            TextView textSensorAccelerometer = (TextView) findViewById(R.id.sensor_accelerometer);
            textSensorAccelerometer.setText(getResources().getString(R.string.error_accelerometer_unavailable));
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        if(sensorAccelerometer != null) {
            sensorManager.registerListener(this, sensorAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        int sensorType = sensorEvent.sensor.getType();
        switch (sensorType) {
            case Sensor.TYPE_ACCELEROMETER:
                float[] currentValue = sensorEvent.values;
                writeDataToFile(sensorEvent.sensor.getName(), currentValue);
                break;
            default:
                // nothing
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    protected void writeDataToFile(String sensorName, float[] values) {
        // write sensorName to file
        StringBuilder output = new StringBuilder();
        output.append(sensorName);
        output.append(";");
        for(float sensorValue : values) {

            // print sensorValue
            output.append(sensorValue);
            output.append(";");
        }
    }
}