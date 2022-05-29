package com.example.activitymonitoring;


import com.example.activitymonitoring.FeatureStrategies.AverageFeature;
import com.example.activitymonitoring.FeatureStrategies.MaxFeature;
import com.example.activitymonitoring.FeatureStrategies.MinFeature;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DataProcessor {
    final int PARAM_TIMING = 20 * 1000; // 20ms timing
    final double[] PARAM_MIN = {0, 0, 0};
    final double[] PARAM_MAX = {0, 0, 0};

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
        sensorData.add(values);

        if ((timestamp - this.start) >= PARAM_TIMING) {
            this.start = timestamp + 1;
            calculateFeatures();
            this.sensorData = new ArrayList<>();
        }
    }

    private double[] normalize(double[] data) {
        // https://stats.stackexchange.com/questions/178626/how-to-normalize-data-between-1-and-1
        if (data == null || data.length != 3) {
            return null;
        }
        double[] normalized = new double[3];

        for (int i = 0; i < data.length; i++) {
            normalized[i] = 2 * ((data[i] - PARAM_MIN[i])/(PARAM_MAX[i] - PARAM_MIN[i])) - 1;
        }

        return normalized;
    }

    private void calculateFeatures() {
        for(int line = 0; line < this.sensorData.size(); line++) {
            if(this.sensorData.get(line).length != 3) {
                // data has not 3 values, so x,y,z could not be represented.
                // data is invalid.
                continue;
            }

            // features: {min: xyz, avg:  xyz, max: xyz, std: xyz, mad: xyz}
            List<Double> f = new ArrayList<>();

            // unnecessary complicated, I guess
            double[] minFeature = new MinFeature().execute(this.sensorData);
            f.addAll(Arrays.asList(Arrays.stream(minFeature).boxed().toArray(Double[]::new)));

            double[] avgFeature = new AverageFeature().execute(this.sensorData);
            f.addAll(Arrays.asList(Arrays.stream(avgFeature).boxed().toArray(Double[]::new)));

            double[] maxFeature = new MaxFeature().execute(this.sensorData);
            f.addAll(Arrays.asList(Arrays.stream(maxFeature).boxed().toArray(Double[]::new)));
            // todo: standard deviation feature
            // todo: mad feature

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
            return null;
        }

        double[] data = this.featureData.get(0).clone();
        this.featureData.remove(0);
        return data;
    }
}
