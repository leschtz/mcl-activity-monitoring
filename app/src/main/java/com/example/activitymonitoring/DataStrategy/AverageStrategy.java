package com.example.activitymonitoring.DataStrategy;

import java.util.List;
import java.util.Arrays;

public class AverageStrategy implements Strategy {


    @Override
    public double[] execute(List<Double[]> data) {
        if (data.size() == 0) {
            return null;
        }

        double[] result = new double[data.get(0).length];
        Arrays.fill(result, 0);

        // calculate the average for every entry and return
        for (Double[] entry : data) {
            for (int i = 0; i < entry.length; i++) {
                result[i] = (result[i] + entry[i]) / 2;
            }
        }

        return result;
    }
}
