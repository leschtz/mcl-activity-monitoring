package com.example.activitymonitoring.FeatureStrategies;

import static java.lang.Math.max;

import java.util.Arrays;
import java.util.List;

public class MaxFeature implements BaseFeature {
    @Override
    public double[] execute(List<float[]> data) {
        if (data.size() == 0) {
            return null;
        }

        double[] result = new double[data.get(0).length];
        Arrays.fill(result, -100);

        for (int cnt = 0; cnt < data.size(); cnt++) {
            float[] entry = data.get(cnt);

            if(entry == null) {
                continue;
            }

            for (int i = 0; i < entry.length; i++) {
                result[i] = max(((double) entry[i]), result[i]);
            }
        }
        return result;
    }
}
