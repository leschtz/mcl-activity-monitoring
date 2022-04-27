package com.example.activitymonitoring;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

public class DataProcessor {
    private Map<String, Map<Long, float[]>> sensorData;
    private List<Double[]> data;
    private SortedMap<Long, float[]> correct_data;

    public DataProcessor(List<Double[]> data) {
        this.data = data;
    }

    private float[] concatArrays(float[] a, float[] b) {
        float[] both = Arrays.copyOf(a, a.length + b.length);
        System.arraycopy(b, 0, both, a.length, b.length);
        return both;
    }

    private void updateMap(String key) {
        Map<Long, float[]> data = this.sensorData.get(key);

        float[] last_know_values = {0, 0, 0};

        // iterate over timestamp-to-sens_values
        for (Map.Entry<Long, float[]> d :
                data.entrySet()) {
            if (d == null) {
                continue;
            }
            if (this.correct_data.containsKey(d.getKey())) {
                last_know_values = this.correct_data.get(d.getKey());
            }

            float[] new_array = concatArrays(last_know_values, d.getValue());
            this.correct_data.put(d.getKey(), new_array);
        }
    }

    private void process() {
        if (sensorData == null || this.correct_data == null) {
            return;
        }

        String gyro_key = "";
        String acc_key = "";
        for (String key : sensorData.keySet()) {
            if (key.contains("Gyroscope") || key.contains("gyroscope")) {
                gyro_key = key;
            }
            if (key.contains("Accelerometer") || key.contains("accelerometer")) {
                acc_key = key;
            }
        }

        // this is not nice code.
        if (gyro_key.length() > 0) {
            updateMap(gyro_key);
        }

        if (acc_key.length() > 0) {
            updateMap(acc_key);
        }
    }

    private List<Double[]> remove_timestamp() {
        if (this.data == null) {
            return null;
        }
        List<Double[]> data = new ArrayList<>();
        for (Double[] d : this.data) {
            Double[] array = Arrays.copyOfRange(d, 1, d.length);
            data.add(array);
        }
        return data;
    }

    // return the last n logged elements
    public List<Double[]> get_last_n_elements(int n) {
        if (n > this.data.size()) {
            return this.data;
        }
        List<Double[]> sublist = this.data.subList(this.data.size() - n, this.data.size());

        return sublist;
    }

    // return all elements from $NOW until ($NOW - t)
    public List<Double[]> get_last_t_ms_elements(long t) {
        if (t <= 0) {
            return null;
        }
        long now = System.currentTimeMillis();
        List<Double[]> sublist = new ArrayList<>();

        for (int i = this.data.size() - 1; i > 0; i--) {
            Double[] entry = this.data.get(i);
            if (entry.length < 2) {
                continue;
            }

            if (entry[0] < (now - t)) {
                break;
            }
            sublist.add(entry);
        }

        return sublist;
    }


}
