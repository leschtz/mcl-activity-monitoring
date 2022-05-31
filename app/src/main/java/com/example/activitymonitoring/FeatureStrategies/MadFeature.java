package com.example.activitymonitoring.FeatureStrategies;

import java.util.Arrays;
import java.util.List;

public class MadFeature implements BaseFeature {
    @Override
    public double[] execute(List<float[]> data) {
        if (data.size() == 0) {
            return null;
        }

        double[] mean = new AverageFeature().execute(data);
        if (mean == null) {
            return null;
        }

        // formula taken from https://de.wikipedia.org/wiki/Mittlere_absolute_Abweichung_vom_Median
        // mad = 1/N * sum(x_i - mean)

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
                // calculate sum(x_i - mean) = v
                result[i] += (entry[i] - mean[i]);
            }
        }

        for (int i = 0; i < result.length; i++) {
            // calculate (1/N * v)
            result[i] = result[i] / data.size();
        }

        return result;
    }
}
