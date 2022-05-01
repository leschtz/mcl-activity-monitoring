package com.example.activitymonitoring;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ProcessorUnitTest {
    @Test
    public void process_checkRawDataMatches() {
        Map<String, Map<Long, float[]>> data = new HashMap<>();

        Map<Long, float[]> sensor_data = new HashMap<>();
        long time = System.currentTimeMillis();
        for(int i = 0; i < 151; i++) {
            float[] sensor_value = {0.0f, 0.0f, 0.0f};
            sensor_data.put(time + i, sensor_value);
        }

        data.put("gyroscope", sensor_data);
        DataProcessor dp = new DataProcessor(data);
        assertNotNull(dp.getRawData());
        assertEquals(data, dp.getRawData());
    }

    @Test
    public void process_checkAlignedDataHasCorrectSize() {
        Map<String, Map<Long, float[]>> data = new HashMap<>();

        Map<Long, float[]> sensor_data = new HashMap<>();
        long time = System.currentTimeMillis();
        for(int i = 0; i < 151; i++) {
            float[] sensor_value = {0.0f, 0.0f, 0.0f};
            sensor_data.put(time + i, sensor_value);
        }

        data.put("gyroscope", sensor_data);

        DataProcessor dp = new DataProcessor(data);
        assertNotNull(dp.getData());

        assertEquals(151, dp.getData().size());
    }

    @Test
    public void process_checkGetLastNElementsBigger() {
        Map<String, Map<Long, float[]>> data = new HashMap<>();

        Map<Long, float[]> sensor_data = new HashMap<>();
        long time = System.currentTimeMillis();
        for(int i = 0; i < 151; i++) {
            float[] sensor_value = {0.0f, 0.0f, 0.0f};
            sensor_data.put(time + i, sensor_value);
        }

        data.put("gyroscope", sensor_data);

        DataProcessor dp = new DataProcessor(data);
        assertNotNull(dp.getData());

        assertEquals(50, dp.get_last_n_elements(50).size());
    }

    @Test
    public void process_checkGetLastNElementsSmaller() {
        Map<String, Map<Long, float[]>> data = new HashMap<>();

        Map<Long, float[]> sensor_data = new HashMap<>();
        long time = System.currentTimeMillis();
        for(int i = 0; i < 43; i++) {
            float[] sensor_value = {0.0f, 0.0f, 0.0f};
            sensor_data.put(time + i, sensor_value);
        }

        data.put("gyroscope", sensor_data);

        DataProcessor dp = new DataProcessor(data);
        assertNotNull(dp.getData());

        assertEquals(43, dp.get_last_n_elements(50).size());
    }

    @Test
    public void process_checkGetLastTMsElements() {
        Map<String, Map<Long, float[]>> data = new HashMap<>();

        Map<Long, float[]> sensor_data = new HashMap<>();
        long time = System.currentTimeMillis();
        for(int i = 0; i < 151; i++) {
            float[] sensor_value = {0.0f, 0.0f, 0.0f};
            sensor_data.put((time + i), sensor_value);
        }

        data.put("gyroscope", sensor_data);

        DataProcessor dp = new DataProcessor(data);
        assertNotNull(dp.getData());

        assertEquals(50, dp.get_last_t_ms_elements(50).size());
    }

    @Test
    public void process_getLastTMsElementsWhereTGreaterThanData() {
        Map<String, Map<Long, float[]>> data = new HashMap<>();

        Map<Long, float[]> sensor_data = new HashMap<>();
        long time = System.currentTimeMillis();
        for(int i = 0; i < 43; i++) {
            float[] sensor_value = {0.0f, 0.0f, 0.0f};
            sensor_data.put(time + i, sensor_value);
        }

        data.put("gyroscope", sensor_data);

        DataProcessor dp = new DataProcessor(data);
        assertNotNull(dp.getData());

        assertEquals(43, dp.get_last_t_ms_elements(50).size());
    }

    @Test
    public void process_checkNull(){
        DataProcessor dp = new DataProcessor(null);

        assertNull(dp.getRawData());
        assertNotNull(dp.getData());
        assertTrue(dp.getData().isEmpty());
        assertEquals(0, dp.get_last_n_elements(10).size());
        assertEquals(0, dp.get_last_t_ms_elements(10).size());
    }
}