package com.example.activitymonitoring;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.RingtoneManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity implements SensorEventListener {
    private SensorManager sensorManager;
    private Sensor sensorAccelerometer;
    private Sensor sensorGyroscope;
    private DataLogger dataLogger;
    private KNNClassifier classifier;
    private DataProcessor dataProcessor;
    private int dummyCounter = 0;

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

        TextView textSensorAccelerometer = findViewById(R.id.timer_rest_time);
        textSensorAccelerometer.setText(R.string.seconds_left_default);

        dataLogger = new DataLogger(getApplicationContext());
        this.dataProcessor = new DataProcessor();

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


                // I'm sure this is not the nicest way to do this, but it works.
                EditText editText = findViewById(R.id.timer_value);
                long time;
                try {
                    time = Long.parseLong(editText.getText().toString()) * 1000L;
                } catch (NumberFormatException nfe) {
                    time = 10 * 1000L;
                }
                new CountDownTimer(time, 1000) {
                    @Override
                    public void onTick(long timeLeftInMillis) {

                        if (dataLogger == null || !dataLogger.isRecording()) {
                            return;
                        }
                        TextView textSensorAccelerometer = findViewById(R.id.timer_rest_time);
                        textSensorAccelerometer.setText(getResources().getString(R.string.seconds_left, (timeLeftInMillis / 1000)));
                    }

                    @Override
                    public void onFinish() {
                        if (dataLogger == null || !dataLogger.isRecording()) {
                            return;
                        }
                        if (!dataLogger.stopRecording()) {
                            failureActivity.show();
                            return;
                        }

                        RingtoneManager.getRingtone(getApplicationContext(), RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)).play();
                        Toast.makeText(getApplicationContext(), R.string.toast_stop_recording, Toast.LENGTH_SHORT).show();
                    }
                }.start();
            }

        });

        this.classifier = new KNNClassifier(25, 7, readFile());
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

            TextView textSecondsLeft = findViewById(R.id.timer_rest_time);
            textSecondsLeft.setText(R.string.seconds_left_default);
        }

        if (dummyCounter >= 5) {
            dummyCounter = 0;
            if (this.dataProcessor != null) {
                int knnResult = -1;
                if (this.classifier != null) {
                    double[] knnData = this.dataProcessor.getKnnData();
                    if (knnData != null) {
                        knnResult = classifier.classify(this.dataProcessor.getKnnData());
                        System.out.println(knnResult);
                    }
                }

                TextView classification_result = findViewById(R.id.str_classification_result);
                classification_result.setText(getResources().getString(R.string.classification_result, Util.getActivityByNumber(knnResult)));
            }
        }
        dummyCounter++;

        switch (sensorType) {
            case Sensor.TYPE_ACCELEROMETER:
                currentValue = sensorEvent.values.clone();
                dataLogger.record(timestamp, currentValue, sensorEvent.sensor.getName());

                dataProcessor.addSensorData(timestamp, currentValue);

                TextView textValueAccelerometer = findViewById(R.id.value_accelerometer);
                textValueAccelerometer.setText(getResources().getString(R.string.label_accelerometer, currentValue[0], currentValue[1], currentValue[2]));
                break;


            case Sensor.TYPE_GYROSCOPE:
                currentValue = sensorEvent.values.clone();
                break;
            default:
                // nothing
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    public List<double[]> readFile() {

        List<double[]> rowList = new ArrayList<>();
        try {
            InputStream is = getResources().openRawResource(R.raw.new_neighbors);

            BufferedReader br = new BufferedReader(new InputStreamReader(is));

            String line;
            while ((line = br.readLine()) != null) {
                String[] lineItemsStrings = line.split(",");
                double[] lineItems = new double[lineItemsStrings.length];
                for (int i = 0; i < lineItemsStrings.length; i++) {
                    lineItems[i] = Double.parseDouble(lineItemsStrings[i]);
                }
                rowList.add(lineItems);
            }
        } catch (Exception e) {
            System.out.println("Could not open neighbor file:   " + e);
        }

        return rowList;
    }


}