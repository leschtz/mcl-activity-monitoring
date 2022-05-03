package com.example.activitymonitoring.DataStrategy;

import java.util.List;
import java.util.Arrays;

public class AverageStrategy implements Strategy {
    /*
    @Override
    public double[] execute(List<float[]> data) {
        if (data.size() == 0) {
            return null;
        }

        double[] result = new double[data.get(0).length];
        Arrays.fill(result, 0);

        for (float[] entry : data) {
            if (entry == null) {
                continue;
            }
            for (int i = 0; i < entry.length; i++) {
                result[i] = (result[i] + (double)entry[i]) / 2;
            }
        }

        return result;
    }
        */
    @Override
    public double[] execute(List<float[]> data) {
        if (data.size() == 0) {
            return null;
        }

        //test
        for (float[] x:data
             ) {

        }

        double[] result = new double[data.get(0).length];
        Arrays.fill(result, 0);

        // calculate the average for every entry and return
        for (float[] entry : data) {

            if (entry == null) {
                continue;
            }

            for (int i = 0; i < entry.length; i++) {

                result[i] = (result[i] + (double)entry[i]) / 2;
                System.out.println("excute loop: "+ result[i]);
            }
        }

        return result;
    }
}
