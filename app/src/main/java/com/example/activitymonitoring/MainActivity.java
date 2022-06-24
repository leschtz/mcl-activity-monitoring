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

import com.example.transfer_api.Prediction;
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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;
import java.util.concurrent.ExecutionException;


public class MainActivity extends AppCompatActivity implements SensorEventListener {
    private SensorManager sensorManager;
    private Sensor sensorAccelerometer;
    private KNNClassifier classifier;
    private DataProcessor dataProcessor;
    private Set<double[]> trainingData;
    private GenericModelWrapper baseModel;
    private TransferLearningModelWrapper transferModel;
    private Boolean isKnnLearning = false;

    FloatingActionButton menuFab, trainKnnFab, transferLearnFab, stopTransferLearningFab,
            stopTransferLearningMainFab, enableTrainingFab;
    TextView trainKnnText, transferLearningText, stopTransferLearningText, enableTrainingText;
    Boolean fabIsVisible;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        menuFab = findViewById(R.id.main_fab);
        transferLearnFab = findViewById(R.id.transfer_learn_fab);
        //stopTransferLearningFab = findViewById(R.id.stop_log_fab);
        //stopTransferLearningMainFab = findViewById(R.id.stop_data_log_fab);
        transferLearningText = findViewById(R.id.transfer_learning_text);
        //stopTransferLearningText = findViewById(R.id.stop_transfer_learning_text);
        enableTrainingText = findViewById(R.id.enable_training);
        enableTrainingFab = findViewById(R.id.start_transfer_training_fab);

        trainKnnFab = findViewById(R.id.train_fab);
        trainKnnText = findViewById(R.id.train_model_text);

        transferLearnFab.setVisibility(View.GONE);
        transferLearningText.setVisibility(View.GONE);
        //stopTransferLearningFab.setVisibility(View.GONE);
        //stopTransferLearningMainFab.setVisibility(View.GONE);
        //stopTransferLearningText.setVisibility(View.GONE);
        enableTrainingFab.setVisibility(View.GONE);
        enableTrainingText.setVisibility(View.GONE);

        trainKnnFab.setVisibility(View.GONE);
        trainKnnText.setVisibility(View.GONE);

        fabIsVisible = Boolean.FALSE;

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensorAccelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        trainingData = new HashSet<>();

        menuFab.setOnClickListener(this::fabViewButtonHandler);

        trainKnnFab.setOnClickListener(this::startKnnLearning);

        transferLearnFab.setOnClickListener(this::startAddingSamplesTransferLearning);
        //stopTransferLearningFab.setOnClickListener(this::stopAddingSamplesTransferLearning);
        //stopTransferLearningMainFab.setOnClickListener(this::stopAddingSamplesTransferLearning);

        enableTrainingFab.setOnLongClickListener(this::hiddenTrainingKnnData);

        if (this.transferModel == null) {
            this.transferModel = new TransferLearningModelWrapper(getBaseContext());
        }

        if (this.dataProcessor == null) {
            this.dataProcessor = new DataProcessor();
        }

        if (this.baseModel == null) {
            this.baseModel = new GenericModelWrapper(getApplicationContext());
        }

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
            sensorManager.registerListener(this, sensorAccelerometer, SensorManager.SENSOR_DELAY_GAME);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (this.sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        int sensorType = sensorEvent.sensor.getType();
        long timestamp = System.currentTimeMillis();

        try {
            this.classification();
        } catch (IllegalStateException | NullPointerException exception) {
            System.err.println("classification() went wrong.");
        }

        float[] currentValue;

        if (sensorType == Sensor.TYPE_ACCELEROMETER) {
            currentValue = sensorEvent.values.clone();
            dataProcessor.addSensorData(timestamp, currentValue);

            if (System.currentTimeMillis() % 100L == 0L) {
                TextView debugAccView = findViewById(R.id.debug_acc);
                debugAccView.setText(getResources().getString(R.string.three_axis_sensor, currentValue[0], currentValue[1], currentValue[2]));

                TextView debugGravityView = findViewById(R.id.debug_gravity);
                debugGravityView.setText(getResources().getString(R.string.three_axis_sensor, currentValue[0] / 9.80665, currentValue[1] / 9.80665, currentValue[2] / 9.80665));
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
    }

    private AlertDialog.Builder createActivityDialog(String dialogTitle, String toastMessage, long millisInFuture) {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);

        builder.setTitle(dialogTitle);
        builder.setItems(R.array.activityArray, (dialog, which) -> {

            String activityString = getResources().getStringArray(R.array.activityArray)[which];
            ActivityType activity = ActivityType.valueOf(activityString);
            Toast.makeText(getApplicationContext(), toastMessage, Toast.LENGTH_LONG).show();

            dataProcessor.setClassifyAs(activity);

            new CountDownTimer(millisInFuture, 1000) {
                @Override
                public void onTick(long timeLeftInMillis) {
                }

                @Override
                public void onFinish() {
                    if (dataProcessor == null) {
                        return;
                    }
                    dataProcessor.setClassifyAs(ActivityType.None);

                    if (transferModel == null || !transferModel.getIsLearning()) {
                        return;
                    }
                    transferModel.clearIsLearning();

                    RingtoneManager.getRingtone(getApplicationContext(), RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)).play();
                    Toast.makeText(getApplicationContext(), R.string.toast_stop_transfer_learning, Toast.LENGTH_SHORT).show();
                }
            }.start();
        });

        return builder;
    }

    private AlertDialog.Builder createTimedActivityDialog(String dialogTitle, long millisInFuture) {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);

        builder.setTitle(dialogTitle);
        builder.setItems(R.array.activityArray, (dialog, which) -> {
            String activityString = getResources().getStringArray(R.array.activityArray)[which];
            ActivityType activity = ActivityType.valueOf(activityString);
            String toastString = getResources().getString(R.string.automatic_classify, activity.name(), 10);
            Toast.makeText(getApplicationContext(), toastString, Toast.LENGTH_LONG).show();

            dataProcessor.setClassifyAs(activity);

            new CountDownTimer(millisInFuture, 1000) {
                @Override
                public void onTick(long timeLeftInMillis) {
                }

                @Override
                public void onFinish() {
                    if (dataProcessor == null) {
                        return;
                    }

                    if (trainingData != null && trainingData.size() > 0) {
                        addFeatureSetToCustomFeaturesFile(trainingData);
                        trainingData.clear();
                    }

                    isKnnLearning = false;
                    dataProcessor.setClassifyAs(ActivityType.None);
                    classifier.neighbors = readFile();

                    RingtoneManager.getRingtone(getApplicationContext(), RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)).play();
                    Toast.makeText(getApplicationContext(), R.string.toast_stop_knn_learning, Toast.LENGTH_SHORT).show();
                }
            }.start();
        });

        return builder;
    }

    private void startKnnLearning(View view) {
        if (!isKnnLearning) {
            AlertDialog.Builder builder = this.createTimedActivityDialog(getString(R.string.train_title), 10 * 1000L);
            builder.show();

            this.isKnnLearning = true;
        } else {
            Toast.makeText(getApplicationContext(), R.string.error_already_learning, Toast.LENGTH_SHORT).show();
        }
        menuFab.performClick();

    }

    private void disableTransferLearning(View view) {
        if (this.transferModel == null) {
            return;
        }

        this.transferModel.disableTraining();
    }

    private void enableLearning(View view) {
        if (this.transferModel == null) {
            return;
        }

        if (this.transferModel.getSamples() == 0) {
            return;
        }

        System.out.println("Enabling Training.");
        this.transferModel.enableTraining((epoch, loss) -> {
            System.out.println(loss);
            if (loss < 0.01) {
                this.transferModel.disableTraining();
            }
        });
    }

    private void startAddingSamplesTransferLearning(View view) {
        if (transferModel != null && !transferModel.getIsLearning()) {
            transferModel.setIsLearning();
            AlertDialog.Builder builder = this.createActivityDialog(getString(R.string.train_title), getString(R.string.toast_start_transfer_learning), 10 * 1000L);

            builder.show();
        } else {
            Toast.makeText(getApplicationContext(), R.string.toast_failed_stop_transfer_learning, Toast.LENGTH_SHORT).show();
        }

        menuFab.performClick();
    }

    private void stopAddingSamplesTransferLearning(View view) {
        if (transferModel != null && transferModel.getIsLearning()) {
            dataProcessor.setClassifyAs(ActivityType.None);
            this.transferModel.clearIsLearning();

            Toast.makeText(getApplicationContext(), R.string.toast_stop_transfer_learning, Toast.LENGTH_SHORT).show();
            //stopTransferLearningFab.hide();
            //stopTransferLearningMainFab.hide();
            //stopTransferLearningText.setVisibility(View.GONE);
        } else {
            Toast.makeText(getApplicationContext(), R.string.toast_failed_stop_transfer_learning, Toast.LENGTH_SHORT).show();
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

        // Inference
        // Train the kNN and the TransferLearning Model
        if (this.isKnnLearning && this.dataProcessor.getClassifyType() != ActivityType.None) {
            double[] features = new double[knnData.length + 1];

            // manual copy is needed, as for the features, the last element has to be the className
            // with a `Array.clone()` the array would have the wrong dimension
            for (int i = 0; i < knnData.length; i++) {
                features[i] = knnData[i];
            }

            features[knnData.length] = this.dataProcessor.getClassifyType().ordinal();
            this.trainingData.add(features.clone());
        }

        if (transferModel != null && transferModel.getIsLearning() && this.dataProcessor.getClassifyType() != ActivityType.None) {
            String className = String.valueOf(this.dataProcessor.getClassifyType().ordinal());
            System.out.println(className);
            try {
                this.transferModel.addSample(f_knnData, className).get();
            } catch (ExecutionException | InterruptedException e) {
                System.err.println("addSample raised an issue: " + e.getCause());
            }
        }

        if (this.classifier != null) {
            int knnResult = classifier.classify(knnData);
            if (System.currentTimeMillis() % 100L == 0L) {
                TextView classification_result = findViewById(R.id.knn_model);
                classification_result.setText(getResources().getString(R.string.classification_result, Util.getActivityByNumber(knnResult)));
            }
        }

        if (this.baseModel != null) {
            Prediction[] possibleResults = this.baseModel.predict(f_knnData);
            Prediction baseResult = getMostLikelyPrediction(possibleResults);
            // Util.debugPredictions(possibleResults);

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

        if (this.transferModel != null) {
            Prediction[] possibleResults = this.transferModel.predict(f_knnData);
            Prediction transferResult = getMostLikelyPrediction(possibleResults);
            // Util.debugPredictions(possibleResults);

            TextView predictionProbabilities = findViewById(R.id.predictions);
            predictionProbabilities.setText(buildPredictionProbabilityString(possibleResults));

            if (transferResult == null) {
                return;
            }

            String activity = transferResult.getClassName();
            if (activity == null) {
                return;
            }

            ActivityType act = ActivityType.values()[Integer.parseInt(activity)];
            TextView base_classification_result = findViewById(R.id.transfer_model);
            base_classification_result.setText(getResources().getString(R.string.classification_result, act.name()));
        }
    }

    private Prediction getMostLikelyPrediction(Prediction[] possiblePredictions) {
        Prediction mostLikely = null;

        for (Prediction prediction : possiblePredictions) {
            if (mostLikely == null) {
                mostLikely = prediction;
            }
            if (prediction.getConfidence() > mostLikely.getConfidence()) {
                mostLikely = prediction;
            }
        }

        return mostLikely;
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

    private void fabViewButtonHandler(View view) {
        if (!fabIsVisible) {
            if (transferModel != null && transferModel.getIsLearning()) {
                transferLearnFab.hide();
                transferLearningText.setVisibility(View.GONE);
                //stopTransferLearningMainFab.show();
                //stopTransferLearningText.setVisibility(View.VISIBLE);
            } else {

                //stopTransferLearningMainFab.hide();
                //stopTransferLearningText.setVisibility(View.GONE);
                transferLearnFab.show();
                transferLearningText.setVisibility(View.VISIBLE);
            }

            enableTrainingFab.show();
            enableTrainingText.setVisibility(View.VISIBLE);

            trainKnnFab.show();
            trainKnnText.setVisibility(View.VISIBLE);
            //stopTransferLearningFab.hide();

            fabIsVisible = true;
        } else {
            if (transferModel != null && transferModel.getIsLearning()) {
                //stopTransferLearningFab.show();
            }

            enableTrainingFab.hide();
            enableTrainingText.setVisibility(View.GONE);
            trainKnnFab.hide();
            transferLearnFab.hide();
            //stopTransferLearningMainFab.hide();
            //stopTransferLearningText.setVisibility(View.GONE);
            trainKnnText.setVisibility(View.GONE);
            transferLearningText.setVisibility(View.GONE);

            fabIsVisible = false;
        }
    }

    private String buildPredictionProbabilityString(Prediction[] predictions) {
        if (predictions.length != 6) {
            return "";
        }

        return getResources()
                .getString(
                        R.string.prediction_templates,
                        predictions[0].getConfidence(),
                        predictions[1].getConfidence(),
                        predictions[2].getConfidence(),
                        predictions[3].getConfidence(),
                        predictions[4].getConfidence(),
                        predictions[5].getConfidence()
                );
    }

    private Boolean hiddenTrainingKnnData(View view) {
        // stopping the transferModel learning after `millisInFuture` time
        new CountDownTimer(3 * 1000L, 1000) {
            @Override
            public void onTick(long timeLeftInMillis) {
            }

            @Override
            public void onFinish() {
                if (transferModel == null) {
                    return;
                }
                transferModel.disableTraining();
            }
        }.start();

        Toast.makeText(getApplicationContext(), "Learning with kNN data.", Toast.LENGTH_LONG).show();
        new Thread(() -> {
            List<double[]> knnData = readFile();

            // knnData is a big file, with a lot features of the same class consecutively.
            // to mix this up, it has to be shuffled
            Collections.shuffle(knnData);

            if (transferModel == null) {
                return;
            }

            // add training samples to the transferModel.
            // starts by taking the first 20 samples on the first execution
            // assumes that data is shuffled randomly each time and the first 20 elements
            // in `knnData` are different every time.
            int sampleSize = 0;
            for (double[] data : knnData) {
                float[] f_data = new float[data.length - 1];
                for (int i = 0; i < f_data.length; i++) {
                    f_data[i] = (float) data[i];
                }

                int classInt = (int) data[data.length - 1];
                String className = String.valueOf(classInt);
                ///*
                System.out.print("Adding Sample: ");
                for (float d : f_data) {
                    System.out.print(d + ", ");
                }
                System.out.println("class: " + className);
                //*/
                transferModel.addSample(f_data, className);
                if (sampleSize++ > 20) break;
            }

            if (transferModel.getSamples() == 0) {
                return;
            }
            System.out.println("Samples: " + sampleSize);

            transferModel.enableTraining((epoch, loss) -> {
                System.out.println("Loss per Epoch: " + loss);
                if (loss < 0.1) {
                    transferModel.disableTraining();
                    System.out.println("Trained for " + epoch + " epochs with loss " + loss + ".");
                }
            });
        }).start();

        return true;
    }

    public void addFeatureSetToCustomFeaturesFile(Set<double[]> featureSet) {
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