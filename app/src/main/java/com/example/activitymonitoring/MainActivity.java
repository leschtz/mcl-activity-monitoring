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
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.example.activitymonitoring.DataStrategy.AverageStrategy;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


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


        Button classify_start_Btn = findViewById(R.id.classify_button_test);
        classify_start_Btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (dataProcessor == null) {
                    return;
                }
                //Map<String, Map<Long, float[]>> dlData = dataLogger.collect();
                //dataProcessor.addRawData(dlData);
                //double[] knnData = dataProcessor.getKnnData(new AverageStrategy());
                //int result = classifier.classify(test);
                double[] test = {-4.1922474, -3.466804, 2.006341, -0.54023397, -0.8074875, -1.6421585};

                int result = classifier.classify(test);
                System.out.println("Classification result is : " + result);

                test = new double[]{-0.06849326, -0.08689558, -0.0388663, -1.364695, -0.7637503, 9.644144};
                result = classifier.classify(test);
                System.out.println("Classification result is : " + result);

                test = new double[]{0, 0, 0, 0.020311269909143448, 0.04688390716910362, 0.07513642311096191};
                result = classifier.classify(test);
                System.out.println("Classification result is : " + result);
            }
        });

        Button classifyBtn = findViewById(R.id.classify_start_button);
        classifyBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (dataLogger != null) {
                    if (dataProcessor != null) {
                        //Map<String, Map<Long, float[]>> dlData = dataLogger.collect();

                        //dataProcessor.addRawData(dlData);
                        // gets the last 10 seconds to classify
                        double[] knnData = dataProcessor.getKnnData(new AverageStrategy(), 10 * 1000);

                        int knnResult = -1;
                        if (classifier != null) {

                            knnResult = classifier.classify(knnData);
                            System.out.println(knnResult);
                        }

                        TextView classification_result = findViewById(R.id.man_classification_result);
                        classification_result.setText(getResources().getString(R.string.manual_classification_result, getActivityByNumber(knnResult)));
                    }

                }
            }
        });

        this.classifier = new KNNClassifier(13, 7, readFile());
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

        if (this.dataLogger != null) {
            Map<String, Map<Long, float[]>> dlData = dataLogger.collect();
            if (this.dataProcessor != null && dlData.size() > 0) {
                this.dataProcessor.addRawData(dlData);
            }
        }
        if (dummyCounter >= 10) {
            dummyCounter = 0;
            if (this.dataLogger != null) {
                Map<String, Map<Long, float[]>> dlData = dataLogger.collect();
                if (this.dataProcessor != null) {

                    this.dataProcessor.addRawData(dlData);
                    double[] knnData = this.dataProcessor.getKnnData(new AverageStrategy(), 500);
                    for (double d : knnData) {
                        System.out.print(d + "\t");
                    }
                    System.out.println();
                    int knnResult = -1;
                    if (this.classifier != null) {

                        knnResult = classifier.classify(knnData);
                        System.out.println(knnResult);
                    }

                    TextView classification_result = findViewById(R.id.str_classification_result);
                    classification_result.setText(getResources().getString(R.string.classification_result, getActivityByNumber(knnResult)));
                }
            }
        }
        dummyCounter++;

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

    public String getActivityByNumber(int activity) {
        switch (activity) {
            case 1:
                return "Walk";
            case 2:
                return "Run";
            case 3:
                return "Jump";
            case 4:
                return "Squat";
            case 5:
                return "Stand";
            case 6:
                return "Sit";

            default:
                return "None";
        }
    }

    public List<double[]> readFile() {

        List<double[]> rowList = new ArrayList<>();
        try {
            InputStream is = getResources().openRawResource(R.raw.neighbors);

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