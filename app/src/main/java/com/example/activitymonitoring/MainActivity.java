package com.example.activitymonitoring;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;


public class MainActivity extends AppCompatActivity implements SensorEventListener {
    private SensorManager sensorManager;
    private Sensor sensorAccelerometer;
    private Sensor sensorGyroscope;
    private DataLogger dataLogger;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        sensorAccelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (sensorAccelerometer == null) {
            TextView textSensorAccelerometer = findViewById(R.id.value_accelerometer);
            textSensorAccelerometer.setText(getResources().getString(R.string.error_accelerometer_unavailable));
        }

        sensorGyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        if (sensorGyroscope == null) {
            TextView textSensorGyroscope = findViewById(R.id.value_gyroscope);
            textSensorGyroscope.setText(getResources().getString(R.string.error_gyroscope_unavailable));
        }

        dataLogger = new DataLogger(getApplicationContext());

        Toast failureActivity = Toast.makeText(getApplicationContext(), R.string.toast_activity_failed, Toast.LENGTH_SHORT);
        ImageButton playBtn = findViewById(R.id.play_button);
        playBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                if (dataLogger.startRecording()) {
                    Toast.makeText(getApplicationContext(), R.string.toast_start_recording, Toast.LENGTH_SHORT).show();
                } else {
                    failureActivity.show();
                }
            }
        });

        ImageButton pauseBtn = findViewById(R.id.pause_button);
        pauseBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                if (dataLogger.pauseRecording()) {
                    Toast.makeText(getApplicationContext(), R.string.toast_pause_recording, Toast.LENGTH_SHORT).show();
                } else {
                    failureActivity.show();
                }
            }
        });
        ImageButton stopBtn = findViewById(R.id.stop_button);
        stopBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                if (dataLogger.stopRecording()) {
                    Toast.makeText(getApplicationContext(), R.string.toast_stop_recording, Toast.LENGTH_SHORT).show();
                } else {
                    failureActivity.show();
                }
            }
        });

        ImageButton timerBtn = findViewById(R.id.timed_log_button);
        timerBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                if (!dataLogger.startRecording()) {
                    failureActivity.show();
                    return;
                }
                Toast.makeText(getApplicationContext(), R.string.toast_start_recording, Toast.LENGTH_SHORT).show();

                // todo: wait for t Seconds

                if (!dataLogger.stopRecording()) {
                    failureActivity.show();
                    return;
                }
                Toast.makeText(getApplicationContext(), R.string.toast_stop_recording, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (sensorAccelerometer != null) {
            sensorManager.registerListener(this, sensorAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        }

        if (sensorGyroscope != null) {
            sensorManager.registerListener(this, sensorGyroscope, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (dataLogger != null) {
            // ensure no data is lost and the logger is nicely ending.
            dataLogger.stopRecording();
        }
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        int sensorType = sensorEvent.sensor.getType();
        long timestamp = System.currentTimeMillis();

        float[] currentValue;

        // data gets written to /data/data/com.example.activitymonitoring/files
        if (dataLogger.isRecording()) {
            TextView textFilePath = findViewById(R.id.label_file_path);
            String path = dataLogger.getFilePath();
            textFilePath.setText(getResources().getString(R.string.file_path, path));
        } else {
            TextView textFilePath = findViewById(R.id.label_file_path);
            textFilePath.setText(getResources().getString(R.string.file_path, "NOT_RECORDING"));
        }

        switch (sensorType) {
            case Sensor.TYPE_ACCELEROMETER:
                currentValue = sensorEvent.values;
                dataLogger.record(timestamp, currentValue, sensorEvent.sensor.getName());

                TextView textValueAccelerometer = findViewById(R.id.value_accelerometer);
                textValueAccelerometer.setText(getResources().getString(R.string.label_accelerometer, currentValue[0], currentValue[1], currentValue[2]));
                break;

            case Sensor.TYPE_GYROSCOPE:
                currentValue = sensorEvent.values;
                dataLogger.record(timestamp, currentValue, sensorEvent.sensor.getName());

                TextView textValueGyroscope = findViewById(R.id.value_gyroscope);
                textValueGyroscope.setText(getResources().getString(R.string.label_gyroscope, currentValue[0], currentValue[1], currentValue[2]));
                break;
            default:
                // nothing
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }


}