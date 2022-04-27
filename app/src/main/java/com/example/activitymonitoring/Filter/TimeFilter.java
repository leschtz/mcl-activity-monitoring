package com.example.activitymonitoring.Filter;

import com.example.activitymonitoring.DataStrategy.Strategy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class TimeFilter extends DataFilter {
    private double time_threshold;

    public TimeFilter(Strategy strategy) {
        this(strategy, 2 * 1000);
    }

    public TimeFilter(Strategy strategy, double time_threshold_in_ms) {
        super(strategy);
        if (time_threshold_in_ms < 0) {
            throw new UnsupportedOperationException();
        }
        this.time_threshold = time_threshold_in_ms;
    }

    public void set_time_threshold(double time_threshold_in_ms) {
        if (time_threshold_in_ms < 0) {
            throw new UnsupportedOperationException();
        }
        this.time_threshold = time_threshold_in_ms;
    }

    @Override
    public double[] filter() {
        return this.filter(this.data);
    }

    @Override
    public double[] filter(List<Double[]> data) {
        double initial_timestamp = -1;

        List<Double[]> reversed_data = new ArrayList<>(data);
        Collections.reverse(reversed_data);

        List<Double[]> filter_data = new ArrayList<>();

        for (Double[] d : data) {
            // at least 2 entries are needed: timestamp + 1 sensor value
            if (d.length < 2) {
                throw new IllegalArgumentException();
            }
            if (initial_timestamp == -1) {
                initial_timestamp = d[0];
            }

            if ((initial_timestamp - this.time_threshold) <= d[0]) {
                break;
            }

            filter_data.add(Arrays.copyOfRange(d, 1, d.length));
        }

        if (data == this.data) {
            this.data.clear();
        }
        return this.strategy.execute(filter_data);
    }


}
