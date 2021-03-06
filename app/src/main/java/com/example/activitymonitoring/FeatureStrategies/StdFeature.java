package com.example.activitymonitoring.FeatureStrategies;

import java.util.Arrays;
import java.util.List;

public class StdFeature implements BaseFeature {
    @Override
    public double[] execute(List<float[]> data) {
        if (data.size() == 0) {
            return null;
        }

        double[] mean = new AverageFeature().execute(data);
        if (mean == null) {
            return null;
        }

        // formula taken from https://en.wikipedia.org/wiki/Standard_deviation#Uncorrected_sample_standard_deviation
        // std = sqrt(1/N * sum((x_i - mean)^2)

        double[] result = new double[data.get(0).length];
        Arrays.fill(result, 0.0);

        for (int cnt = 0; cnt < data.size(); cnt++) {
            float[] entry = data.get(cnt);

            if(entry == null) {
                continue;
            }

            for (int i = 0; i < entry.length; i++) {
                if (i > mean.length) {
                    break;
                }
                // calculate sum((x_i - mean)^2) = v
                double intermediate = (entry[i] - mean[i]);
                result[i] += Math.pow(intermediate, 2);
            }
        }

        for (int i = 0; i < result.length; i++) {
            // calculate sqrt(1/N * v)
            result[i] = Math.sqrt(result[i] / data.size());
        }

        return result;
    }
}
