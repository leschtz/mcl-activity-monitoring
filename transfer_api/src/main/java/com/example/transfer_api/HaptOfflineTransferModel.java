package com.example.transfer_api;

import org.tensorflow.lite.Interpreter;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/** Represents a "partially" trainable model that is based on some other, base model. */
public final class HaptOfflineTransferModel implements Closeable {

    private final Interpreter interpreter;
    private final int numClasses;




    // Setting this to a higher value allows to calculate bottlenecks for more samples while
    // adding them to the bottleneck collection is blocked by an active training thread.
    private static final int NUM_THREADS =
            Math.max(1, Runtime.getRuntime().availableProcessors() - 1);

    private final Map<String, Integer> classes;
    private final String[] classesByIdx;
    private final Map<String, float[]> oneHotEncodedClass;




    // Used to spawn background threads.
    private final ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);

    // This lock guarantees that only one thread is performing training and inference at
    // any point in time. It also protects the sample collection from being modified while
    // in use by a training thread.
    private final Lock trainingInferenceLock = new ReentrantLock();

    // This lock guards access to trainable parameters.
    private final ReadWriteLock parameterLock = new ReentrantReadWriteLock();

    // Set to true when [close] has been called.
    private volatile boolean isTerminating = false;

    public HaptOfflineTransferModel(ModelLoader modelLoader, Collection<String> classes) {
        try {
            this.interpreter = new Interpreter(modelLoader.loadMappedFile("Hapt_neighbors.tflite"));
            this.numClasses = classes.size();


        } catch (IOException e) {
            throw new RuntimeException("Couldn't read underlying model for HaptOfflineTransferModel", e);
        }
        classesByIdx = classes.toArray(new String[0]);
        this.classes = new TreeMap<>();
        oneHotEncodedClass = new HashMap<>();
        for (int classIdx = 0; classIdx < classes.size(); classIdx++) {
            String className = classesByIdx[classIdx];
            this.classes.put(className, classIdx);
            oneHotEncodedClass.put(className, oneHotEncoding(classIdx));
        }
    }


    /**
     * Runs model inference on a given image.
     *
     * @param image image RGB data.
     * @return predictions sorted by confidence decreasing. Can be null if model is terminating.
     */
    public Prediction[] predict(float[] image) {
        checkNotTerminating();
        trainingInferenceLock.lock();

        try {
            if (isTerminating) {
                return null;
            }

            float[] confidences;
            parameterLock.readLock().lock();
            try {
                confidences = runInference(image);
            } finally {
                parameterLock.readLock().unlock();
            }

            Prediction[] predictions = new Prediction[classes.size()];
            for (int classIdx = 0; classIdx < classes.size(); classIdx++) {
                predictions[classIdx] = new Prediction(classesByIdx[classIdx], confidences[classIdx]);
            }


            return predictions;
        } finally {
            trainingInferenceLock.unlock();
        }
    }

    private float[] oneHotEncoding(int classIdx) {
        float[] oneHot = new float[6];
        oneHot[classIdx] = 1;
        return oneHot;
    }

    float[] runInference(float[] testImage) {
        // Run the inference.
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("input_1", new float[][] {testImage});

        Map<String, Object> outputs = new HashMap<>();
        float[][] output = new float[1][numClasses];
        outputs.put("dense_4", output);
        this.interpreter.runSignature(inputs, outputs);
        return output[0];
    }



    private void checkNotTerminating() {
        if (isTerminating) {
            throw new IllegalStateException("Cannot operate on terminating model");
        }
    }

    /**
     * Terminates all model operation safely. Will block until current inference request is finished
     * (if any).
     *
     * <p>Calling any other method on this object after [close] is not allowed.
     */
    @Override
    public void close() {
        isTerminating = true;
        executor.shutdownNow();

        // Make sure that all threads doing inference are finished.
        trainingInferenceLock.lock();

        try {
            boolean ok = executor.awaitTermination(5, TimeUnit.SECONDS);
            if (!ok) {
                throw new RuntimeException("Model thread pool failed to terminate");
            }
            this.interpreter.close();

        } catch (InterruptedException e) {
            // no-op
        } finally {
            trainingInferenceLock.unlock();
        }
    }
}
