package com.example.activitymonitoring.FeatureStrategies;

import java.util.List;
import java.util.Arrays;

public class AverageFeature implements BaseFeature {

    @Override
    public double[] execute(List<float[]> data) {
        if (data.size() == 0) {
            return null;
        }

        double[] result = new double[data.get(0).length];
        Arrays.fill(result, 0);

        // calculate the average for every entry and return
        for (int cnt = 0; cnt < data.size(); cnt++) {

            //for (float[] entry : data) {
            float[] entry = data.get(cnt);
            if (entry == null) {
                continue;
            }

            for (int i = 0; i < entry.length; i++) {
                if (cnt == 0) {
                    result[i] = ((double) entry[i]);
                } else {
                    result[i] = (result[i] + (double) entry[i]) / (double) 2.0;

                }
                //System.out.println("excute loop: " + result[i]);
            }
        }

        return result;
    }
}
