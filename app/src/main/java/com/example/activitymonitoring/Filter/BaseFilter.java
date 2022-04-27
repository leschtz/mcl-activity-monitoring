package com.example.activitymonitoring.Filter;

import com.example.activitymonitoring.DataStrategy.Strategy;

import java.util.ArrayList;
import java.util.List;

abstract class DataFilter {
    protected Strategy strategy;
    protected List<Double[]> data;

    public DataFilter(Strategy strategy) {
        this.strategy = strategy;
    }

    public void setStrategy(Strategy strategy) {
        this.strategy = strategy;
    }

    public Strategy getStrategy() {
        return this.strategy;
    }

    public void setData(List<Double[]> data) {
        this.data = data;
    }

    public void addData(List<Double[]> data) {
        if (data == null) {
            data = new ArrayList<>();
        }

        this.data.addAll(data);
    }

    public List<Double[]> getData() {
        return this.data;
    }

    // todo: apply only when a certain threshold is met
    abstract public double[] filter();
    abstract public double[] filter(List<Double[]> data);
}
