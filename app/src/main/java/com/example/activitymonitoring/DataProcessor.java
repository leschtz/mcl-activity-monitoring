package com.example.activitymonitoring;


import com.example.activitymonitoring.DataStrategy.Strategy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class DataProcessor {
    private Map<String, Map<Long, float[]>> sensorData;
    private TreeMap<Long, float[]> alignedData;

    public DataProcessor() {
        this.sensorData = null;
        this.alignedData = new TreeMap<>();
    }

    public DataProcessor(Map<String, Map<Long, float[]>> data) {
        this.sensorData = data;
        this.alignedData = new TreeMap<>();
        this.align();
    }

    public void addRawData(Map<String, Map<Long, float[]>> data) {
        if (this.sensorData == null) {
            this.sensorData = data;
        } else {
            // todo: add raw data correctly

            for (Map.Entry<String, Map<Long, float[]>> sensor : data.entrySet()) {
                if (this.sensorData.containsKey(sensor.getKey())) {
                    Map<Long, float[]> sensorSpecificData = this.sensorData.get(sensor.getKey());

                    if (sensorSpecificData == null) {
                        this.sensorData.put(sensor.getKey(), sensor.getValue());
                    } else {
                        for (Map.Entry<Long, float[]> sensorData : sensorSpecificData.entrySet()) {
                            if ((sensorData == null) || (sensorData.getKey() == null)) {
                                continue;
                            }
                            this.sensorData.get(sensor.getKey()).put(sensorData.getKey(), sensorData.getValue());
                        }
                    }
                } else {
                    this.sensorData.put(sensor.getKey(), sensor.getValue());
                }
            }
        }

        this.align();
    }

    private float[] concatArrays(float[] a, float[] b) {
        float[] both = Arrays.copyOf(a, a.length + b.length);
        System.arraycopy(b, 0, both, a.length, b.length);
        return both;
    }

    private void updateMap(String key) {
        if (this.sensorData == null) {
            return;
        }

        Map<Long, float[]> data = this.sensorData.get(key);
        if (data == null) {
            return;
        }
        float[] last_know_values = {0, 0, 0};

        // iterate over timestamp-to-sens_values
        for (Map.Entry<Long, float[]> d :
                data.entrySet()) {
            if (d == null) {
                continue;
            }
            if (this.alignedData.containsKey(d.getKey())) {
                last_know_values = this.alignedData.get(d.getKey());

            }

            /*
            for(float val : d.getValue()) {
                System.out.print(val + "\t");
            }
            for (float val : last_know_values) {
                System.out.print(val + "\t");
            }
            System.out.println();
            */
            if (last_know_values.length == 6) {
                last_know_values[3] = d.getValue()[0];
                last_know_values[4] = d.getValue()[1];
                last_know_values[5] = d.getValue()[2];
                this.alignedData.put(d.getKey(), last_know_values);
                return;
            }
            // todo: last_known_values: entferne die 0.0f werte, wenn man sie überschreibt
            float[] new_array = concatArrays(last_know_values, d.getValue());
            this.alignedData.put(d.getKey(), new_array);
        }
    }

    private void align() {
        if (this.sensorData == null || this.alignedData == null) {
            return;
        }

        String gyro_key = "";
        String acc_key = "";

        // todo: make the key the Android Sensor.TYPE_ACCELEROMETER // Sensor.TYPE_GYROSCOPE
        // this is not nice code.
        for (String key : sensorData.keySet()) {
            if (key.contains("Gyroscope") || key.contains("gyroscope")) {
                gyro_key = key;
            }
            if (key.contains("Accelerometer") || key.contains("accelerometer")) {
                acc_key = key;
            }
        }

        if (gyro_key.length() > 0) {
            updateMap(gyro_key);
        }
        if (acc_key.length() > 0) {
            updateMap(acc_key);
        }
    }

    public Map<Long, float[]> getData() {
        return this.alignedData;
    }

    public Map<String, Map<Long, float[]>> getRawData() {
        return this.sensorData;
    }

    //return the last n logged elements
    public List<float[]> get_last_n_elements(int n) {
        List<float[]> sublist = new ArrayList<>();

        if (this.alignedData == null) {
            return sublist;
        }

        Set<Long> descendingKeys = this.alignedData.descendingKeySet();
        int i = 0;

        for (Long key : descendingKeys) {
            float[] d = this.alignedData.get(key);

            if (i >= this.alignedData.size() || i >= n) {
                break;
            }
            sublist.add(d);
            i++;
        }

        return sublist;
    }

    // return all elements from $NOW until ($NOW - t)
    // or should it return the last $t ms from the latest entry?
    public List<float[]> get_last_t_ms_elements(long t) {
        if (t <= 0) {
            return null;
        }

        List<float[]> sublist = new ArrayList<>();

        if (this.alignedData == null) {
            return sublist;
        }

        Set<Long> descendingKeys = this.alignedData.descendingKeySet();
        long threshold_time = 0;

        for (Long key : descendingKeys) {
            if (threshold_time == 0) {
                threshold_time = key - t;

            }
            if (threshold_time >= key) {
                break;
            }
            sublist.add(this.alignedData.get(key));
        }

        return sublist;
    }

    public double[] getKnnData(Strategy strategy, long t_in_ms) {
        List<float[]> data = get_last_t_ms_elements(t_in_ms);
        return strategy.execute(data);
    }

    public double[] getKnnData(Strategy strategy) {
        return this.getKnnData(strategy, 100);

    }
}