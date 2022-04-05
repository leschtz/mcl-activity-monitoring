package com.example.activitymonitoring;

import android.content.Context;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;


public class DataLogger {
    private String filename;
    private File filesDir;
    private DataOutputStream dataOutputStream = null;
    private String filePath;
    private Boolean is_recording = Boolean.FALSE;
    private Map<Long, float[]> dataSource = new HashMap<>();

    public DataLogger(Context context) {
        filesDir = context.getFilesDir();
    }

    public Boolean isRecording() {
        return is_recording;
    }

    public Boolean startRecording() {
        try {
            if (is_recording == Boolean.FALSE) {
                is_recording = Boolean.TRUE;

                if (dataOutputStream == null) {
                    filename = String.format("%s-data.log", Instant.now().toString());
                    File f = new File(filesDir, filename);

                    dataOutputStream = new DataOutputStream(new FileOutputStream(f));
                    filePath = f.getAbsoluteFile().toString();
                }
            }
        } catch (IOException e) {
            resetValues();
            System.out.println("Could not setup file path, see stack trace below.");
            e.printStackTrace();
            return Boolean.FALSE;
        }
        return Boolean.TRUE;
    }

    public Boolean pauseRecording() {
        is_recording = Boolean.FALSE;
        flushDataStream();
        return Boolean.TRUE;
    }


    public Boolean stopRecording() {
        is_recording = Boolean.FALSE;
        if (dataOutputStream == null) {
            return Boolean.TRUE;
        }

        flushDataStream();
        try {
            dataOutputStream.close();
            resetValues();
        } catch (IOException e) {
            //e.printStackTrace();
            resetValues();
            return Boolean.FALSE;
        }
        return Boolean.TRUE;
    }

    private void resetValues() {
        is_recording = Boolean.FALSE;
        filePath = "";
        filename = "";
        dataOutputStream = null;
    }

    private void flushDataStream() {
        if (dataOutputStream != null) {
            try {
                dataOutputStream.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public String getFilePath() {
        return filePath;
    }

    public String createDataString(long timestamp, float[] values, String sensorName) {
        // write sensorName to file
        StringBuilder output = new StringBuilder();
        output.append(sensorName);
        output.append(";");
        output.append(timestamp);
        output.append(";");
        for (float sensorValue : values) {
            // print sensorValue
            output.append(sensorValue);
            output.append(";");
        }
        output.append(System.lineSeparator());
        return output.toString();
    }

    public String createDataString(long timestamp, float[] values) {
        return this.createDataString(timestamp, values, "");
    }

    public void record(long timestamp, float[] values, String sensorName) {
        dataSource.put(timestamp, values);

        String dataString = createDataString(timestamp, values, sensorName);
        this.record(dataString);
    }

    public void record(long timestamp, float[] values) {
        dataSource.put(timestamp, values);

        String dataString = createDataString(timestamp, values);
        this.record(dataString);
    }

    public void record(String dataString) {
        if (dataOutputStream == null) {
            return;
        }
        if (is_recording) {
            try {
                dataOutputStream.write(dataString.getBytes());
                dataOutputStream.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
