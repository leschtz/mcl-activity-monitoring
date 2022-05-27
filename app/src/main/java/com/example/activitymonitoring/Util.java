package com.example.activitymonitoring;

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


}
