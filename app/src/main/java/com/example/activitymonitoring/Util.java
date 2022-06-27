package com.example.activitymonitoring;

import android.content.res.Resources;

import com.example.transfer_api.Prediction;

import java.util.stream.DoubleStream;

public class Util {
    public static double[] join(double[] a, double[] b) {
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
        System.out.print("Predictions: ");
        for (Prediction predict : predictions) {
            System.out.print(predict.getClassName() + ": " + predict.getConfidence() + "  ;  ");
        }
        System.out.println();
    }

    public static String buildPredictionProbabilityString(Resources resources, Prediction[] predictions) {
        if (predictions.length != 6) {
            return "";
        }

        return resources
                .getString(
                        R.string.prediction_templates,
                        predictions[0].getConfidence(),
                        predictions[1].getConfidence(),
                        predictions[2].getConfidence(),
                        predictions[3].getConfidence(),
                        predictions[4].getConfidence(),
                        predictions[5].getConfidence()
                );
    }

    public static Prediction getMostLikelyPrediction(Prediction[] possiblePredictions) {
        Prediction mostLikely = null;

        for (Prediction prediction : possiblePredictions) {
            if (mostLikely == null) {
                mostLikely = prediction;
            }
            if (prediction.getConfidence() > mostLikely.getConfidence()) {
                mostLikely = prediction;
            }
        }

        return mostLikely;
    }
}
