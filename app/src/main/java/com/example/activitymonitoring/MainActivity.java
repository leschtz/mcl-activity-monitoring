package com.example.activitymonitoring;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.RingtoneManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.transfer_api.TransferLearningModel;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;


public class MainActivity extends AppCompatActivity implements SensorEventListener {
    private SensorManager sensorManager;
    private Sensor sensorAccelerometer;
    private DataLogger dataLogger;
    private KNNClassifier classifier;
    private DataProcessor dataProcessor;
    private Set<double[]> trainingData;
    private TransferLearningModelWrapper baseModel;
    private TransferLearningModelWrapper transferModel;

    FloatingActionButton menuFab, trainKnnFab, logDataFab, stopLogFab, stopLogMainFab;
    TextView trainKnnText, logDataText, stopLogText;
    Boolean fabIsVisible;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        menuFab = findViewById(R.id.main_fab);
        trainKnnFab = findViewById(R.id.train_fab);
        logDataFab = findViewById(R.id.data_log_fab);
        stopLogFab = findViewById(R.id.stop_log_fab);
        stopLogMainFab = findViewById(R.id.stop_data_log_fab);

        trainKnnText = findViewById(R.id.train_model_text);
        logDataText = findViewById(R.id.data_log_text);
        stopLogText = findViewById(R.id.stop_data_log_text);

        stopLogFab.setVisibility(View.GONE);
        trainKnnFab.setVisibility(View.GONE);
        trainKnnText.setVisibility(View.GONE);
        logDataFab.setVisibility(View.GONE);
        logDataText.setVisibility(View.GONE);
        stopLogMainFab.setVisibility(View.GONE);
        stopLogText.setVisibility(View.GONE);
        fabIsVisible = Boolean.FALSE;

        Toast failureActivity = Toast.makeText(getApplicationContext(), R.string.toast_activity_failed, Toast.LENGTH_SHORT);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensorAccelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        dataLogger = new DataLogger(getApplicationContext());
        this.dataProcessor = new DataProcessor();
        this.transferModel = new TransferLearningModelWrapper(getApplicationContext());

        trainingData = new HashSet<>();

        menuFab.setOnClickListener(
                view -> {
                    if (!fabIsVisible) {
                        if (dataLogger != null && dataLogger.isRecording()) {
                            logDataFab.hide();
                            logDataText.setVisibility(View.GONE);
                            stopLogMainFab.show();
                            stopLogText.setVisibility(View.VISIBLE);
                        } else {
                            stopLogMainFab.hide();
                            stopLogText.setVisibility(View.GONE);
                            logDataFab.show();
                            logDataText.setVisibility(View.VISIBLE);
                        }
                        trainKnnFab.show();
                        trainKnnText.setVisibility(View.VISIBLE);
                        stopLogFab.hide();

                        fabIsVisible = true;
                    } else {
                        if (dataLogger != null && dataLogger.isRecording()) {
                            stopLogFab.show();
                        }
                        trainKnnFab.hide();
                        logDataFab.hide();
                        stopLogMainFab.hide();
                        stopLogText.setVisibility(View.GONE);
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
                    builder.setItems(R.array.activityArray, (dialog, which) -> {
                        String activityString = getResources().getStringArray(R.array.activityArray)[which];
                        ActivityType activity = ActivityType.valueOf(activityString);
                        String toastString = getResources().getString(R.string.automatic_classify, activity.name(), 10);
                        Toast.makeText(MainActivity.this, toastString, Toast.LENGTH_LONG).show();

                        if (dataProcessor == null) {
                            failureActivity.show();
                            return;
                        }
                        Toast.makeText(getApplicationContext(), R.string.toast_start_recording, Toast.LENGTH_SHORT).show();
                        dataProcessor.setClassifyAs(activity);

                        new CountDownTimer(10 * 1000L, 1000) {
                            @Override
                            public void onTick(long timeLeftInMillis) {
                            }

                            @Override
                            public void onFinish() {
                                if (dataProcessor == null) {
                                    return;
                                }
                                if (trainingData != null && trainingData.size() > 0) {
                                    addFeatureSetToRawFile(trainingData);
                                    trainingData.clear();
                                }
                                dataProcessor.setClassifyAs(ActivityType.None);

                                // todo: check if training is enabled and possible like this.
                                transferModel.enableTraining((null));
                                classifier.neighbors = readFile();

                                RingtoneManager.getRingtone(getApplicationContext(), RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)).play();
                                Toast.makeText(getApplicationContext(), R.string.toast_stop_recording, Toast.LENGTH_SHORT).show();
                            }
                        }.start();
                        // setting new data for neighbours.


                    });
                    menuFab.performClick();
                    builder.show();
                });

        logDataFab.setOnClickListener(
                view -> {
                    if (dataLogger != null && dataLogger.startRecording()) {
                        Toast.makeText(getApplicationContext(), R.string.toast_start_recording, Toast.LENGTH_SHORT).show();
                    } else {
                        failureActivity.show();
                    }
                    this.transferModel.enableTraining(null);
                    menuFab.performClick();
                }
        );

        stopLogFab.setOnClickListener(this::stopLogging);
        stopLogMainFab.setOnClickListener(this::stopLogging);

        File f = new File(this.getFilesDir(), "custom_features.txt");
        if (!f.exists()) {
            // on first access, base features are copied to a writeable place
            InputStream is = getResources().openRawResource(R.raw.neighbors);
            try {
                OutputStreamWriter outputStream = new OutputStreamWriter(new FileOutputStream(f));
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(is));

                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    String[] d = line.split(",");
                    if (d.length != 16) {
                        // todo: this is definitely a bug!
                        // bug description: when writing to the new file, the bufferedReader is not able to read beyond 401 lines.
                        //                  and stops in the middle of the line. as a result, the last lines do not get copied over correctly.
                        //                  For the last line it writes, it does not write the full line.
                        //                  Therefore not the correct amount of features will be in this line.
                        continue;
                    }
                    outputStream.write(line);
                    outputStream.write(System.lineSeparator());
                }
                outputStream.write(System.lineSeparator());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        this.classifier = new KNNClassifier(31, 15, readFile());
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
            dataLogger.stopRecording();
        }

        if (this.transferModel != null) {
            this.transferModel.close();
        }

        if (this.baseModel != null) {
            this.baseModel.close();
        }
        if (this.sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        int sensorType = sensorEvent.sensor.getType();
        long timestamp = System.currentTimeMillis();

        this.classification();

        float[] currentValue;

        switch (sensorType) {
            case Sensor.TYPE_ACCELEROMETER:
                currentValue = sensorEvent.values.clone();
                dataLogger.record(timestamp, currentValue, sensorEvent.sensor.getName());

                TextView debugAccView = findViewById(R.id.debug_acc);
                debugAccView.setText(getResources().getString(R.string.three_axis_sensor, currentValue[0], currentValue[1], currentValue[2]));

                dataProcessor.addSensorData(timestamp, currentValue);
                TextView debugGravityView = findViewById(R.id.debug_gravity);
                debugGravityView.setText(getResources().getString(R.string.three_axis_sensor, currentValue[0] / 9.80665, currentValue[1] / 9.80665, currentValue[2] / 9.80665));
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

    public void stopLogging(View view) {
        this.transferModel.disableTraining();
        if (dataLogger != null && dataLogger.stopRecording()) {
            Toast.makeText(getApplicationContext(), R.string.toast_stop_recording, Toast.LENGTH_SHORT).show();
            stopLogFab.hide();
            stopLogMainFab.hide();
            stopLogText.setVisibility(View.GONE);
        } else {
            Toast.makeText(getApplicationContext(), R.string.toast_activity_failed, Toast.LENGTH_SHORT).show();
        }

        if (fabIsVisible) {
            trainKnnFab.hide();
            trainKnnText.setVisibility(View.GONE);
            fabIsVisible = Boolean.FALSE;
        }
    }

    public void classification() {

        if (this.dataProcessor == null) {
            return;
        }


        double[] knnData = this.dataProcessor.getKnnData();
        if (knnData == null) {
            return;
        }

        float[] f_knnData = new float[knnData.length];
        for (int i = 0; i < knnData.length; i++) {
            f_knnData[i] = (float) knnData[i];
            System.out.print(f_knnData[i] + " : " + knnData[i] + "\t");
        }
        System.out.println();


        // Inference Step
        // todo: add inference step for transfer learning model
        if (this.dataProcessor.getClassifyType() != ActivityType.None) {
            double[] features = new double[knnData.length + 1];
            for (int i = 0; i < knnData.length; i++) {
                features[i] = knnData[i];
            }
            features[knnData.length] = this.dataProcessor.getClassifyType().ordinal();
            this.trainingData.add(features.clone());


            String className = String.valueOf(this.dataProcessor.getClassifyType().ordinal());
            this.transferModel.addSample(f_knnData, className);
        }

        if (this.classifier != null) {
            //System.out.println("Got a new data sample");
            int knnResult = classifier.classify(knnData);

            //System.out.println(knnResult);
            TextView classification_result = findViewById(R.id.knn_model);
            classification_result.setText(getResources().getString(R.string.classification_result, Util.getActivityByNumber(knnResult)));

        }

        // todo: implement for base model
        if (this.baseModel != null) {
            TransferLearningModel.Prediction[] possibleResults = this.baseModel.predict(f_knnData);
            TransferLearningModel.Prediction baseResult = null;
            for (TransferLearningModel.Prediction prediction : possibleResults) {
                if (baseResult == null) {
                    baseResult = prediction;
                }
                if (prediction.getConfidence() > baseResult.getConfidence()) {
                    baseResult = prediction;
                }
            }

            if (baseResult == null) {
                return;
            }
            String activity = baseResult.getClassName();
            if (activity == null) {
                return;
            }

            ActivityType act = ActivityType.values()[Integer.parseInt(activity)];
            TextView base_classification_result = findViewById(R.id.base_model);
            base_classification_result.setText(getResources().getString(R.string.classification_result, act.name()));
        }

        // todo: implement for transfer learning model
        if (this.transferModel != null) {
            TransferLearningModel.Prediction[] possibleResults = this.transferModel.predict(f_knnData);
            TransferLearningModel.Prediction transferResult = null;
            for (TransferLearningModel.Prediction prediction : possibleResults) {
                if (transferResult == null) {
                    transferResult = prediction;
                }
                if (prediction.getConfidence() > transferResult.getConfidence()) {
                    transferResult = prediction;
                }
                //System.out.println(prediction.getClassName() + ": " + prediction.getConfidence());
            }

            if (transferResult == null) {
                return;
            }

            String activity = transferResult.getClassName();
            if (activity == null) {
                return;
            }
            //System.out.println(transferResult.getClassName() + ": " + transferResult.getConfidence());
            ActivityType act = ActivityType.values()[Integer.parseInt(activity)];
            TextView base_classification_result = findViewById(R.id.transfer_model);
            base_classification_result.setText(getResources().getString(R.string.classification_result, act.name()));
        }
    }

    public List<double[]> readFile() {
        List<double[]> rowList = new ArrayList<>();
        try {
            File f = new File(this.getFilesDir(), "custom_features.txt");
            InputStream is = new DataInputStream(new FileInputStream(f));

            BufferedReader br = new BufferedReader(new InputStreamReader(is));

            String line;
            while ((line = br.readLine()) != null) {
                //System.out.println("Reading: "+ line);
                String[] lineItemsStrings = line.split(",");
                double[] lineItems = new double[lineItemsStrings.length];
                if (lineItems.length != 16) {
                    System.err.println("Found an invalid line.");
                    continue;
                }
                for (int i = 0; i < lineItemsStrings.length; i++) {
                    lineItems[i] = Double.parseDouble(lineItemsStrings[i]);
                }
                rowList.add(lineItems);
            }
        } catch (Exception e) {
            System.err.println("Could not open neighbor file:   " + e);
        }
        System.out.println("# of loaded features: " + rowList.size());
        return rowList;
    }

    public void addFeatureSetToRawFile(Set<double[]> featureSet) {
        try {
            File f = new File(this.getFilesDir(), "custom_features.txt");
            OutputStreamWriter streamWriter = new OutputStreamWriter(new FileOutputStream(f, true));
            streamWriter.write(System.lineSeparator());
            for (double[] features : featureSet) {
                StringJoiner stringJoiner = new StringJoiner(",", "", System.lineSeparator());
                for (double feature : features) {
                    stringJoiner.add(String.valueOf(feature));
                }
                streamWriter.write(stringJoiner.toString());
            }

            streamWriter.flush();
            streamWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}