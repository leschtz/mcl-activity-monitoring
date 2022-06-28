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
import java.util.concurrent.atomic.AtomicInteger;


public class MainActivity extends AppCompatActivity implements SensorEventListener {
    private SensorManager sensorManager;
    private Sensor sensorAccelerometer;
    private KNNClassifier classifier;
    private DataProcessor dataProcessor;
    private Set<double[]> trainingData;
    private GenericModelWrapper genericModel;
    private HaptOfflineTransferModelWrapper haptOfflineModel;
    private MobileTransferModelWrapper mobileTransferModel;
    private Boolean isKnnLearning = false;
    private float prevLoss = 0.0f;
    private int stopTrain = 0;

    FloatingActionButton mainMenuBtn, knnAddSampleBtn, transferAddSampleBtn, enableTrainingBtn;
    TextView trainKnnText, transferLearningText, enableTrainingText;
    Boolean fabIsVisible;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mainMenuBtn = findViewById(R.id.main_fab);
        transferAddSampleBtn = findViewById(R.id.transfer_learn_fab);
        transferLearningText = findViewById(R.id.transfer_learning_text);
        enableTrainingText = findViewById(R.id.enable_training);
        enableTrainingBtn = findViewById(R.id.start_transfer_training_fab);

        knnAddSampleBtn = findViewById(R.id.train_fab);
        trainKnnText = findViewById(R.id.train_model_text);

        transferAddSampleBtn.setVisibility(View.GONE);
        transferLearningText.setVisibility(View.GONE);
        enableTrainingBtn.setVisibility(View.GONE);
        enableTrainingText.setVisibility(View.GONE);

        knnAddSampleBtn.setVisibility(View.GONE);
        trainKnnText.setVisibility(View.GONE);

        fabIsVisible = Boolean.FALSE;

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensorAccelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        trainingData = new HashSet<>();

        mainMenuBtn.setOnClickListener(this::fabViewButtonHandler);
        knnAddSampleBtn.setOnClickListener(this::startKnnLearning);
        transferAddSampleBtn.setOnClickListener(this::startAddingSamplesTransferLearning);
        //transferAddSampleBtn.setOnLongClickListener(this::testTransferLearningModelWithEvaluationDataset);

        enableTrainingBtn.setOnClickListener(this::trainWithSamples);
        enableTrainingBtn.setOnLongClickListener(this::trainWithRecordedData);

        if (this.dataProcessor == null) {
            this.dataProcessor = new DataProcessor();
        }

        if (this.genericModel == null) {
            this.genericModel = new GenericModelWrapper(getApplicationContext());
        }

        if (this.haptOfflineModel == null) {
            this.haptOfflineModel = new HaptOfflineTransferModelWrapper(getApplicationContext());
        }

        if (this.mobileTransferModel == null) {
            this.mobileTransferModel = new MobileTransferModelWrapper(getApplicationContext());
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
                outputStream.close();
                bufferedReader.close();
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

    private AlertDialog.Builder createTransferActivityDialog(String dialogTitle, String toastMessage, long millisInFuture) {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);

        builder.setTitle(dialogTitle);
        builder.setItems(R.array.activityArray, (dialog, which) -> {

            String activityString = getResources().getStringArray(R.array.activityArray)[which];
            ActivityType activity = ActivityType.valueOf(activityString);
            Toast.makeText(getApplicationContext(), toastMessage, Toast.LENGTH_SHORT).show();

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

                    if (mobileTransferModel == null || !mobileTransferModel.getIsLearning()) {
                        return;
                    }

                    addFeatureSetToCustomFeaturesFile(trainingData, "transfer_learning_features.txt");
                    mobileTransferModel.clearIsLearning();

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
            Toast.makeText(getApplicationContext(), toastString, Toast.LENGTH_SHORT).show();

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
        mainMenuBtn.performClick();

    }

    private void startAddingSamplesTransferLearning(View view) {

        if (mobileTransferModel != null && !mobileTransferModel.getIsLearning()) {
            mobileTransferModel.setIsLearning();
            AlertDialog.Builder builder = this.createTransferActivityDialog(getString(R.string.train_title), getString(R.string.toast_start_transfer_learning), 10 * 1000L);
            builder.show();
        } else {
            Toast.makeText(getApplicationContext(), R.string.toast_failed_stop_transfer_learning, Toast.LENGTH_SHORT).show();
        }

        mainMenuBtn.performClick();
    }

    public void classification() {
        if (this.dataProcessor == null) {
            return;
        }

        double[] knnData = this.dataProcessor.getKnnData();
        if (knnData == null) {
            return;
        }

        // neural networks need their data as floats
        float[] f_knnData = new float[knnData.length];
        for (int i = 0; i < knnData.length; i++) {
            f_knnData[i] = (float) knnData[i];
        }

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

        if (mobileTransferModel != null && this.dataProcessor.getClassifyType() != ActivityType.None) {
            String className = String.valueOf(this.dataProcessor.getClassifyType().ordinal());
            //System.out.println(className);
            try {
                double[] features = new double[knnData.length + 1];

                // manual copy is needed, as for the features, the last element has to be the className
                // with a `Array.clone()` the array would have the wrong dimension
                for (int i = 0; i < knnData.length; i++) {
                    features[i] = knnData[i];
                }

                features[knnData.length] = this.dataProcessor.getClassifyType().ordinal();
                this.trainingData.add(features.clone());

                this.mobileTransferModel.addSample(f_knnData, className).get();
            } catch (ExecutionException | InterruptedException e) {
                System.err.println("addSample raised an issue: " + e.getCause());
            }
        }


        if (this.classifier != null) {
            int knnResult = classifier.classify(knnData);
            if (System.currentTimeMillis() % 20L == 0L) {
                TextView classification_result = findViewById(R.id.knn_model);
                classification_result.setText(getResources().getString(R.string.classification_result, Util.getActivityByNumber(knnResult)));
            }
        }

        if (this.mobileTransferModel != null) {
            Prediction[] possibleResults = this.mobileTransferModel.predict(f_knnData);
            Prediction predictionResult = Util.getMostLikelyPrediction(possibleResults);
            // Util.debugPredictions(possibleResults);

            if (predictionResult == null) {
                return;
            }

            String activity = predictionResult.getClassName();
            if (activity == null) {
                return;
            }

            if (System.currentTimeMillis() % 20L == 0L) {

                TextView predictionProbabilities = findViewById(R.id.mobile_model_predictions);
                predictionProbabilities.setText(Util.buildPredictionProbabilityString(getResources(), possibleResults));


                TextView confidencePrediction = findViewById(R.id.mobile_model_confidence_text);
                confidencePrediction.setText(getResources().getString(R.string.prediction_confidence, predictionResult.confidence));

                ActivityType act = ActivityType.values()[Integer.parseInt(activity)];
                TextView base_classification_result = findViewById(R.id.mobile_model);
                base_classification_result.setText(getResources().getString(R.string.classification_result, act.name()));
            }
        }

        if (this.genericModel != null) {
            Prediction[] possibleResults = this.genericModel.predict(f_knnData);
            Prediction genericResult = Util.getMostLikelyPrediction(possibleResults);
            // Util.debugPredictions(possibleResults);

            if (genericResult == null) {
                return;
            }

            String activity = genericResult.getClassName();
            if (activity == null) {
                return;
            }
            if (System.currentTimeMillis() % 20L == 0L) {
                TextView predictionProbabilities = findViewById(R.id.generic_model_predictions);
                predictionProbabilities.setText(Util.buildPredictionProbabilityString(getResources(), possibleResults));

                TextView confidencePrediction = findViewById(R.id.generic_model_confidence_text);
                confidencePrediction.setText(getResources().getString(R.string.prediction_confidence, genericResult.confidence));

                ActivityType act = ActivityType.values()[Integer.parseInt(activity)];
                TextView base_classification_result = findViewById(R.id.generic_model);
                base_classification_result.setText(getResources().getString(R.string.classification_result, act.name()));
            }
        }

        if (this.haptOfflineModel != null) {
            Prediction[] possibleResults = this.haptOfflineModel.predict(f_knnData.clone());

            Prediction haptResult = Util.getMostLikelyPrediction(possibleResults);
            //Util.debugPredictions(possibleResults);
            //System.out.println(haptResult + "\t" + haptResult.className + "\t" + haptResult.getConfidence() + "");

            if (haptResult == null) {
                return;
            }

            String activity = haptResult.getClassName();
            if (activity == null) {
                return;
            }
            if (System.currentTimeMillis() % 20L == 0L) {

                TextView predictionProbabilities = findViewById(R.id.hapt_offline_model_predictions);
                predictionProbabilities.setText(Util.buildPredictionProbabilityString(getResources(), possibleResults));

                TextView confidencePrediction = findViewById(R.id.hapt_offline_model_confidence_text);
                confidencePrediction.setText(getResources().getString(R.string.prediction_confidence, haptResult.confidence));

                ActivityType act = ActivityType.values()[Integer.parseInt(activity)];
                TextView base_classification_result = findViewById(R.id.hapt_offline_model);
                base_classification_result.setText(getResources().getString(R.string.classification_result, act.name()));
            }
        }
    }


    public List<double[]> readFile() {
        return readFile("custom_features.txt");
    }

    public List<double[]> readFile(String filename) {
        List<double[]> rowList = new ArrayList<>();
        try {
            File f = new File(this.getFilesDir(), filename);
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

            br.close();
            is.close();
        } catch (Exception e) {
            System.err.println("Could not open neighbor file:   " + e);
        }
        System.out.println("# of loaded features: " + rowList.size());
        return rowList;
    }

    private void fabViewButtonHandler(View view) {
        if (!fabIsVisible) {
            transferAddSampleBtn.show();
            transferLearningText.setVisibility(View.VISIBLE);

            enableTrainingBtn.show();
            enableTrainingText.setVisibility(View.VISIBLE);

            knnAddSampleBtn.show();
            trainKnnText.setVisibility(View.VISIBLE);

            fabIsVisible = true;
        } else {
            enableTrainingBtn.hide();
            enableTrainingText.setVisibility(View.GONE);
            knnAddSampleBtn.hide();
            transferAddSampleBtn.hide();
            trainKnnText.setVisibility(View.GONE);
            transferLearningText.setVisibility(View.GONE);

            fabIsVisible = false;
        }
    }


    // usage:
    private Boolean trainWithRecordedData(View view) {
        // close the menu
        this.mainMenuBtn.performClick();

        // informing the user about the data learning
        Toast.makeText(getApplicationContext(), "Learning with pre-recorded data.", Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            List<double[]> trainingData = readFile("transfer-test-data.csv");

            // knnData is a big file, with a lot features of the same class consecutively.
            // to mix this up, it has to be shuffled
            Collections.shuffle(trainingData);

            if (mobileTransferModel == null) {
                return;
            }

            // No samples were loaded, it can't train the Model and should not do so
            if (trainingData.size() == 0) {
                runOnUiThread(() -> Toast.makeText(getApplicationContext(), "No pre-recorded data available to train.", Toast.LENGTH_SHORT).show());
                return;
            }

            // add training samples to the transferModel.
            // starts by taking the first 20 samples on the first execution
            // assumes that data is shuffled randomly each time and the first 20 elements
            // in `knnData` are different every time.
            int sampleSize = 0;
            for (double[] data : trainingData) {
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
                mobileTransferModel.addSample(f_data, className);
                if (sampleSize++ > 100) break;
            }

            if (0 == sampleSize) {
                return;
            }
            System.out.println("Samples: " + sampleSize);

            AtomicInteger epochs = new AtomicInteger();
            mobileTransferModel.enableTraining((epoch, loss) -> {
                System.out.println("Epoch: " + epochs.get() + " Loss: " + loss + " with error: " + Math.abs(loss - prevLoss));
                if (Float.isNaN(loss)) {
                    runOnUiThread(() -> Toast.makeText(getApplicationContext(), "Finished training due to invalid data.", Toast.LENGTH_SHORT).show());
                    this.mobileTransferModel.disableTraining();
                    Thread.currentThread().interrupt();
                }

                if (epochs.get() > 500) {
                    runOnUiThread(() -> Toast.makeText(getApplicationContext(), "Finished training due to too many epochs.", Toast.LENGTH_SHORT).show());
                    this.mobileTransferModel.disableTraining();
                    Thread.currentThread().interrupt();
                }

                if (Math.abs(loss - prevLoss) < 0.001) {
                    if (stopTrain++ == 5) {
                        runOnUiThread(() -> Toast.makeText(getApplicationContext(), "Finished training due to too convergence.", Toast.LENGTH_SHORT).show());
                        this.mobileTransferModel.disableTraining();
                        Thread.currentThread().interrupt();
                        stopTrain = 0;
                    }
                } else {
                    if (stopTrain > 0) {
                        stopTrain--;
                    }
                }
                prevLoss = loss;
                epochs.getAndIncrement();
            });
        }).start();

        return true;
    }

    private void trainWithSamples(View view) {
        this.mainMenuBtn.performClick();

        if (trainingData == null || trainingData.size() == 0) {
            runOnUiThread(() -> Toast.makeText(getApplicationContext(), "No training samples available.", Toast.LENGTH_SHORT).show());
            System.err.println("No training samples available.");
            return;
        }

        trainingData.clear();

        AtomicInteger epochs = new AtomicInteger();
        mobileTransferModel.enableTraining((epoch, loss) -> {
            System.out.println("Epoch: " + epochs.get() + " Loss: " + loss + " with error: " + Math.abs(loss - prevLoss));
            if (Float.isNaN(loss)) {
                runOnUiThread(() -> Toast.makeText(getApplicationContext(), "Finished training due to invalid data.", Toast.LENGTH_SHORT).show());
                this.mobileTransferModel.disableTraining();
                Thread.currentThread().interrupt();
            }

            if (epochs.get() > 500) {
                runOnUiThread(() -> Toast.makeText(getApplicationContext(), "Finished training due to too many epochs.", Toast.LENGTH_SHORT).show());
                this.mobileTransferModel.disableTraining();
                Thread.currentThread().interrupt();
            }

            if (Math.abs(loss - prevLoss) < 0.001) {
                if (stopTrain++ == 5) {
                    runOnUiThread(() -> Toast.makeText(getApplicationContext(), "Finished training due to too convergence.", Toast.LENGTH_SHORT).show());
                    this.mobileTransferModel.disableTraining();
                    Thread.currentThread().interrupt();
                    stopTrain = 0;
                }
            } else {
                if (stopTrain > 0) {
                    stopTrain--;
                }
            }
            prevLoss = loss;
            epochs.getAndIncrement();
        });
    }

    public void addFeatureSetToCustomFeaturesFile(Set<double[]> featureSet, String
            filename) {
        try {
            File f = new File(this.getFilesDir(), filename);
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

    public void addFeatureSetToCustomFeaturesFile(Set<double[]> featureSet) {
        addFeatureSetToCustomFeaturesFile(featureSet, "custom_features.txt");
    }

    // todo: quick function to compare the data with

    private Boolean testTransferLearningModelWithEvaluationDataset(View view) {
        List<double[]> data = readFile("evaluation-data.txt");


        StringJoiner s_pred = new StringJoiner(", ");
        StringJoiner s_real = new StringJoiner(", ");
        System.out.println("evaluation");
        System.out.println(data.size());
        for (double[] d : data) {
            float[] f_data = new float[d.length - 1];
            for (int i = 0; i < (d.length - 1); i++) {
                f_data[i] = (float) d[i];
            }

            Double className = Double.valueOf(d[d.length - 1]);

            Prediction[] pred = this.mobileTransferModel.predict(f_data);
            Prediction result = null;
            for (Prediction p : pred) {
                if (result == null) {
                    result = p;
                }

                if (result.getConfidence() < p.getConfidence()) {
                    result = p;
                }
            }

            if (result != null) {
                s_real.add(String.valueOf(className.intValue()));
                s_pred.add(result.getClassName());
            }
        }

        System.out.println(s_pred.length());
        System.out.println(s_real.length());
        File f = new File(this.getFilesDir(), "evaluation_results.txt");
        if (!f.exists()) {
            // on first access, base features are copied to a writeable place
            try {
                OutputStreamWriter outputStream = new OutputStreamWriter(new FileOutputStream(f));
                outputStream.write("x_real = [");
                outputStream.write(s_real.toString());
                outputStream.write("]");
                outputStream.write(System.lineSeparator());

                outputStream.write("x_pred = [");
                outputStream.write(s_pred.toString());
                outputStream.write("]");
                outputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return true;

    }
}