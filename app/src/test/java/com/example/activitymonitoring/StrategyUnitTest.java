package com.example.activitymonitoring;

import org.junit.Test;

import static org.junit.Assert.*;

import android.widget.ArrayAdapter;

import com.example.activitymonitoring.DataStrategy.AverageStrategy;
import com.example.activitymonitoring.DataStrategy.Strategy;

import java.util.ArrayList;
import java.util.List;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class StrategyUnitTest {
    @Test
    public void averageStrategy_resultIsCorrect() {
        List<double[]> data = new ArrayList<>();
        double[] entry = {0.0, 0.2, 0.4, 0.6};
        for(int i = 0; i < 10; i++) {
            data.add(entry);
        }

        double[] expected = {0.0, 0.2, 0.4, 0.6};
        Strategy strategy = new AverageStrategy();
        double[] result = strategy.execute(data);
        for(int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], result[i], 0.01);
        }
    }

    @Test
    public void averageStrategy_canHandleNull() {
        List<double[]> data = new ArrayList<>();
        double[] e_1 = {0.0, 0.2, 0.4, 0.6};
        for(int i = 0; i < 10; i++) {
            data.add(e_1);
            data.add(null);
        }

        double[] expected = {0.0, 0.2, 0.4, 0.6};

        Strategy strategy = new AverageStrategy();
        double[] result = strategy.execute(data);
        for(int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], result[i], 0.01);
        }
    }
}