package com.example.activitymonitoring.DataStrategy;

import java.util.List;

public interface Strategy {
    public double[] execute(List<Double[]> data);
}
