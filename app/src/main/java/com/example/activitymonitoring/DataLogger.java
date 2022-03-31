package com.example.activitymonitoring;

import android.content.Context;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;


public class DataLogger {

    String filename = "data.log";
    DataOutputStream dataOutputStream = null;
    private String filePath;

    public DataLogger (Context context) {
        try {

            File f = new File(context.getFilesDir(), filename);
            FileOutputStream file = new FileOutputStream(f);
            dataOutputStream = new DataOutputStream(file);
            filePath = f.getAbsolutePath();
        } catch (IOException e) {
            filePath = "could not setup filepath";
            System.out.println("Could not setup filepathing, see stack trace below.");
            e.printStackTrace();
        }
    }

    public String getFilePath() {
        return filePath;
    }
    public String createDataString(String sensorName, float[] values) {
        // write sensorName to file
        StringBuilder output = new StringBuilder();
        output.append(sensorName);
        output.append(";");
        for(float sensorValue : values) {
            // print sensorValue
            output.append(sensorValue);
            output.append(";");
        }
        output.append(System.lineSeparator());
        return output.toString();
    }

    public void writeToFile(String dataString) {
        if (dataOutputStream == null) {
            return;
        }
        try {
            dataOutputStream.write(dataString.getBytes());
            dataOutputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
