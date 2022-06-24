package com.example.activitymonitoring;

import com.example.transfer_api.Prediction;

import java.util.stream.DoubleStream;

public class Util {
    public static double[] join(double[] a, double[] b)
    {
        return DoubleStream.concat(DoubleStream.of(a), DoubleStream.of(b))
                .toArray();
    }

    public static String getActivityByNumber(int activity) {
        switch (activity) {
            case 1:
                return "Walk";
            case 2:
                return "Run";
            case 3:
                return "Jump";
            case 4:
                return "Squat";
            case 5:
                return "Stand";
            case 6:
                return "Sit";

            default:
                return "None";
        }
    }

    public static void debugPredictions(Prediction[] predictions) {
        System.out.print("TransferModel Predictions: ");
        for (Prediction predict : predictions) {
            System.out.print(predict.getClassName() + ": " + predict.getConfidence() + "  ;  ");
        }
        System.out.println();
    }
}
