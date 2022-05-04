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
    private float[] sensorValues = new float[]{0, 0, 0, 0, 0, 0};

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
        if (data == null) {
            return;
        }

        if (this.sensorData == null) {
            this.sensorData = data;
        } else {
            // todo: add raw data correctly

            for (Map.Entry<String, Map<Long, float[]>> newSensorData : data.entrySet()) {
                if (this.sensorData.containsKey(newSensorData.getKey())) {
                    Map<Long, float[]> sensorSpecificData = this.sensorData.get(newSensorData.getKey());

                    if (sensorSpecificData == null) {
                        this.sensorData.put(newSensorData.getKey(), newSensorData.getValue());
                    } else {
                        //data.get(newSensorData.getKey()).put(data.get(newSensorData.getKey()))
                        // entrySet() is the mapping {timestamp: values[]}
                        for (Map.Entry<Long, float[]> sensorData : newSensorData.getValue().entrySet()) {
                            if ((sensorData == null) || (sensorData.getKey() == null)) {
                                continue;
                            }
                            // sensorDate = {timestamp : values[]}
                            this.sensorData.get(newSensorData.getKey()).put(sensorData.getKey(), sensorData.getValue());
                        }
                    }
                } else {
                    this.sensorData.put(newSensorData.getKey(), newSensorData.getValue());
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

        if (key == null) {
            return;
        }

        Map<Long, float[]> data = this.sensorData.get(key);
        if (data == null) {
            return;
        }

        // iterate over timestamp-to-sens_values
        for (Map.Entry<Long, float[]> d : data.entrySet()) {
            if (d == null) {
                continue;
            }

            float[] last_know_values;
            last_know_values = new float[]{0, 0, 0, 0, 0, 0};

            if (this.alignedData.containsKey(d.getKey())) {
                float[] test = this.alignedData.get(d.getKey());
                if (test != null) {
                    last_know_values = test;
                }
            }
            boolean gyro_data = key.contains("Gyroscope") || key.contains("gyroscope") || key.contains("Gyro") || key.contains("gyro");
            if (gyro_data) {
                last_know_values[0] = d.getValue()[0];
                last_know_values[1] = d.getValue()[1];
                last_know_values[2] = d.getValue()[2];
            }

            boolean acc_data = key.contains("Accelerometer") || key.contains("accelerometer") || key.contains("Acc") || key.contains("acc");
            if (acc_data) {
                last_know_values[3] = d.getValue()[0];
                last_know_values[4] = d.getValue()[1];
                last_know_values[5] = d.getValue()[2];
            }

            // todo: get values from previous sensor readings
            // as we are using the timestamp, I simply look for the next smaller timestamp in the alignedData
            // and use it's value there.
            Long timestamp = d.getKey();
            if (this.alignedData.size() > 0) {
                //System.out.println("Working with Timestamp: " + timestamp + " first Key: " + this.alignedData.firstKey());
            }
            timestamp--;
            float[] prevValue = this.alignedData.get(timestamp);

            while (prevValue == null) {
                timestamp--;
                if (this.alignedData.containsKey(timestamp)) {
                    //System.out.println("Found a value!!!");
                    prevValue = this.alignedData.get(timestamp);
                    //for(float v : prevValue) {
                    //    System.out.print(v + "\t");
                    //}
                    break;
                }
                if (this.alignedData.size() == 0) {
                    //System.out.println("No data in this.alignedData");
                    break;
                }

                if (this.alignedData.firstKey() > timestamp) {
                    break;
                }
            }

            // now we have the previous sensor readings of all sensors.
            // we need to write those to the current timestamp, which we did not change.
            if (prevValue == null) {
                this.alignedData.put(d.getKey(), last_know_values);
                return;
            }

            if (gyro_data) {
                //System.out.println("Fixing Acceleration in Gyro sensor.");
                last_know_values[3] = prevValue[3];
                last_know_values[4] = prevValue[4];
                last_know_values[5] = prevValue[5];
            }

            if (acc_data) {
                //System.out.println("Fixing Gyro in Acceleration sensor.");
                last_know_values[0] = prevValue[0];
                last_know_values[1] = prevValue[1];
                last_know_values[2] = prevValue[2];
            }
            //for(float v : last_know_values) {
            //    System.out.print(v + "\t");
            //}
            this.alignedData.put(d.getKey(), last_know_values);
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
            if (key.contains("Gyroscope") || key.contains("gyroscope") || key.contains("Gyro") || key.contains("gyro")) {
                gyro_key = key;
            } else if (key.contains("Accelerometer") || key.contains("accelerometer") || key.contains("Acc") || key.contains("acc")) {
                acc_key = key;

            }
        }

        updateMap(gyro_key);
        updateMap(acc_key);

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
        //List<float[]> data = get_last_n_elements(1000);
        List<float[]> data = get_last_t_ms_elements(2 * 1000);

        return strategy.execute(data); //this.getKnnData(strategy, 100);
    }
}
