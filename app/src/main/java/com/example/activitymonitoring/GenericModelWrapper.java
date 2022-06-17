/* Copyright 2019 The TensorFlow Authors. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
==============================================================================*/

package com.example.activitymonitoring;

import android.content.Context;
import android.os.ConditionVariable;


import com.example.transfer_api.GenericModel;
import com.example.transfer_api.ModelLoader;
import com.example.transfer_api.TransferLearningModel;
import com.example.transfer_api.TransferLearningModel.LossConsumer;
import com.example.transfer_api.GenericModel.Prediction;


import java.io.Closeable;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;


public class GenericModelWrapper implements Closeable {

    private final GenericModel model;


    GenericModelWrapper(Context context) {
        model =
                new GenericModel(
                        new ModelLoader(context, "model"), Arrays.asList("1", "2", "3", "4","5","6"));

    }


    // This method is thread-safe, but blocking.
    public Prediction[] predict(float[] image) {
        return model.predict(image);
    }





    /** Frees all model resources and shuts down all background threads. */
    public void close() {
        model.close();
    }
}
