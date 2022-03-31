package com.example.activitymonitoring;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Environment;
import android.widget.TextView;


public class MainActivity extends AppCompatActivity implements SensorEventListener {
    private SensorManager sensorManager;
    private Sensor sensorAccelerometer;
    private DataLogger dataLogger;

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

        dataLogger = new DataLogger(getBaseContext());
        if (dataLogger == null) {

            TextView textSensorAccelerometer = (TextView) findViewById(R.id.sensor_accelerometer);
            textSensorAccelerometer.setText(getResources().getString(R.string.error_accelerometer_unavailable));
        }

        // data gets written to /data/data/com.example.activitymonitoring/files
        TextView textFilePath = (TextView) findViewById(R.id.label_file_path);
        String path = dataLogger.getFilePath();
        textFilePath.setText(getResources().getString(R.string.file_path, path));
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
                String data = dataLogger.createDataString(sensorEvent.sensor.getName(), currentValue);
                dataLogger.writeToFile(data);
                TextView textValueAccelerometer = (TextView) findViewById(R.id.value_accelerometer);
                textValueAccelerometer.setText(getResources().getString(R.string.label_accelerometer, currentValue[0], currentValue[1], currentValue[2]));
                break;
            default:
                // nothing
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }


}