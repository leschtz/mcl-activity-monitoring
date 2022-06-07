package com.example.activitymonitoring;


import com.example.activitymonitoring.FeatureStrategies.AverageFeature;
import com.example.activitymonitoring.FeatureStrategies.MadFeature;
import com.example.activitymonitoring.FeatureStrategies.MaxFeature;
import com.example.activitymonitoring.FeatureStrategies.MinFeature;
import com.example.activitymonitoring.FeatureStrategies.StdFeature;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DataProcessor {
    final float CONST_G = 9.80665f;
    final long PARAM_TIMING = 1000L; // 20ms timing
    final float[] PARAM_MIN = {-1.976388870971105f, -2.009722276169204f, -1.837500071636558f};
    final float[] PARAM_MAX = {2.004166708636188f , 1.716666672288882f , 1.979166654737614f };

    private long start = 0;
    private ActivityType type;
    private List<float[]> sensorData;
    private List<double[]> featureData;

    public DataProcessor() {
        this.sensorData = new ArrayList<>();
        this.featureData = new ArrayList<>();
        type = ActivityType.None;
    }

    public void addSensorData(long timestamp, float[] values) {
        for(int i = 0; i < values.length; i++) {
            values[i] = values[i] / CONST_G;
        }
        sensorData.add(values);

        if ((timestamp - this.start) >= PARAM_TIMING) {
            this.start = timestamp + 1;
            calculateFeatures();
            this.sensorData = new ArrayList<>();
        }
    }

    private List<float[]> normalize(List<float[]> sensorData) {
        // https://stats.stackexchange.com/questions/178626/how-to-normalize-data-between-1-and-1

        if (sensorData == null || sensorData.size() == 0){
            return null;
        }
        List<float[]> returnValue = new ArrayList<>();
        for(float[] data : sensorData) {
            if (data == null || data.length != 3) {
                return null;
            }
            float[] normalized = new float[3];

            for (int i = 0; i < data.length; i++) {
                normalized[i] = 2 * ((data[i] - PARAM_MIN[i])/(PARAM_MAX[i] - PARAM_MIN[i])) - 1;

                // fallback solution to keep it normalized
                if(normalized[i] > 1) {
                    normalized[i] = 1;
                } else if(normalized[i] < -1) {
                    normalized[i] = -1;
                }
            }
            returnValue.add(normalized);
        }

        return returnValue;
    }

    private void calculateFeatures() {
        for(int line = 0; line < this.sensorData.size(); line++) {
            if(this.sensorData.get(line).length != 3) {
                // data has not 3 values, so x,y,z could not be represented.
                // data is invalid.
                continue;
            }

            List<float[]> normalizedSensorData = this.normalize(this.sensorData);

            // features: {min: xyz, avg:  xyz, max: xyz, std: xyz, mad: xyz}
            List<Double> f = new ArrayList<>();

            // unnecessary complicated, I guess
            double[] avgFeature = new AverageFeature().execute(normalizedSensorData);
            f.addAll(Arrays.asList(Arrays.stream(avgFeature).boxed().toArray(Double[]::new)));

            double[] stdFeature = new StdFeature().execute(normalizedSensorData);
            f.addAll(Arrays.asList(Arrays.stream(stdFeature).boxed().toArray((Double[]::new))));

            double[] madFeature = new MadFeature().execute(normalizedSensorData);
            f.addAll(Arrays.asList(Arrays.stream(madFeature).boxed().toArray((Double[]::new))));

            double[] maxFeature = new MaxFeature().execute(normalizedSensorData);
            f.addAll(Arrays.asList(Arrays.stream(maxFeature).boxed().toArray(Double[]::new)));

            double[] minFeature = new MinFeature().execute(normalizedSensorData);
            f.addAll(Arrays.asList(Arrays.stream(minFeature).boxed().toArray(Double[]::new)));

            double[] features = f.stream().mapToDouble(Double::doubleValue).toArray();
            this.featureData.add(features);
        }
    }

    public void setClassifyAs(ActivityType type) {
        this.type = type;
    }

    public ActivityType getClassifyType() {
        return this.type;
    }
    public double[] getKnnData() {
        // returns always the earliest set of features, still in the list
        if(this.featureData.size() == 0) {
            System.out.println("got no data to send");
            return null;
        }

        double[] data = this.featureData.get(0).clone();
        this.featureData.remove(0);
        return data;
    }
}
