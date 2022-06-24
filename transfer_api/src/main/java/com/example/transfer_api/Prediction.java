package com.example.transfer_api;

/*
 * Prediction for a single class produced by the model.
 */
public class Prediction {
    public final String className;
    public final float confidence;

    public Prediction(String className, float confidence) {
        this.className = className;
        this.confidence = confidence;
    }

    public String getClassName() {
        return className;
    }

    public float getConfidence() {
        return confidence;
    }
}
