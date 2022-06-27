
package com.example.activitymonitoring;

import android.content.Context;

import com.example.transfer_api.HaptOfflineTransferModel;
import com.example.transfer_api.ModelLoader;
import com.example.transfer_api.Prediction;

import java.io.Closeable;
import java.util.Arrays;


public class HaptOfflineTransferModelWrapper implements Closeable {
    private final HaptOfflineTransferModel model;

    HaptOfflineTransferModelWrapper(Context context) {
        model = new HaptOfflineTransferModel(
                new ModelLoader(context, "model"),
                Arrays.asList("0", "1", "2", "3", "4", "5")
        );
    }

    // This method is thread-safe, but blocking.
    public Prediction[] predict(float[] image) {
        return model.predict(image);
    }

    /**
     * Frees all model resources and shuts down all background threads.
     */
    public void close() {
        model.close();
    }
}