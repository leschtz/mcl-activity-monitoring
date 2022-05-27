package com.example.activitymonitoring;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
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

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity implements SensorEventListener {
    private SensorManager sensorManager;
    private Sensor sensorAccelerometer;
    private DataLogger dataLogger;
    private KNNClassifier classifier;
    private DataProcessor dataProcessor;
    private int dummyCounter = 0;

    FloatingActionButton menuFab, trainKnnFab, logDataFab;
    TextView trainKnnText, logDataText;
    Boolean fabIsVisible;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        menuFab = findViewById(R.id.main_fab);
        trainKnnFab = findViewById(R.id.train_fab);
        logDataFab = findViewById(R.id.data_log_fab);

        trainKnnText = findViewById(R.id.train_model_text);
        logDataText = findViewById(R.id.data_log_text);

        trainKnnFab.setVisibility(View.GONE);
        trainKnnText.setVisibility(View.GONE);
        logDataFab.setVisibility(View.GONE);
        logDataText.setVisibility(View.GONE);
        fabIsVisible = Boolean.FALSE;

        menuFab.setOnClickListener(
                view -> {
                    if (!fabIsVisible) {
                        trainKnnFab.show();
                        logDataFab.show();
                        trainKnnText.setVisibility(View.VISIBLE);
                        logDataText.setVisibility(View.VISIBLE);

                        fabIsVisible = true;
                    } else {

                        trainKnnFab.hide();
                        logDataFab.hide();
                        trainKnnText.setVisibility(View.GONE);
                        logDataText.setVisibility(View.GONE);

                        fabIsVisible = false;
                    }
                }
        );

        trainKnnFab.setOnClickListener(
                view -> {
                        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                        builder.setTitle(R.string.train_title);
                        builder.setItems(R.array.activityArray, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                // The 'which' argument contains the index position
                                // of the selected item
                                // todo: add logic to classify the next 10s automatically
                                String activityString = getResources().getStringArray(R.array.activityArray)[which];
                                ActivityType activity = ActivityType.valueOf(activityString);
                                String toastString = getResources().getString(R.string.automatic_classify, activity.name(), 10);
                                Toast.makeText(MainActivity.this, toastString, Toast.LENGTH_LONG).show();

                            }
                        });
                        builder.show();
                });

        logDataFab.setOnClickListener(
                view -> {
                    Toast.makeText(MainActivity.this, "Alarm Added", Toast.LENGTH_LONG).show();
                }
        );

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        sensorAccelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
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


        this.classifier = new KNNClassifier(25, 9, readFile());
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (sensorAccelerometer != null) {
            sensorManager.registerListener(this, sensorAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
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

        if (dummyCounter >= 5) {
            dummyCounter = 0;
            if (this.dataProcessor != null) {
                int knnResult = -1;
                if (this.classifier != null) {
                    double[] knnData = this.dataProcessor.getKnnData();
                    if (knnData != null) {
                        // todo: enable knn classifier
                        //knnResult = classifier.classify(knnData);
                        //System.out.println(knnResult);
                    }
                }

                //TextView classification_result = findViewById(R.id.str_classification_result);
                //classification_result.setText(getResources().getString(R.string.classification_result, Util.getActivityByNumber(knnResult)));
            }
        }
        dummyCounter++;

        switch (sensorType) {
            case Sensor.TYPE_ACCELEROMETER:
                currentValue = sensorEvent.values.clone();
                dataLogger.record(timestamp, currentValue, sensorEvent.sensor.getName());

                dataProcessor.addSensorData(timestamp, currentValue);
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

    public void addFeatureSetToRawFile(int activity, double[] features) {
        try {
            StringBuilder stringBuilder = new StringBuilder();
            for(int i = 0; i < features.length; i++) {
                stringBuilder.append(features[i]);
                stringBuilder.append(",");
            }
            stringBuilder.append(activity);

            OutputStreamWriter streamWriter = new OutputStreamWriter(null);
            streamWriter.write(stringBuilder.toString());
            streamWriter.flush();
            streamWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}