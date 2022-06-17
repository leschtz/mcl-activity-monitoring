import numpy as np
import tensorflow as tf


def main():
    print(tf.__version__)
    path = '../tfLite/tfLiteModelConverted/model.tflite'
    path = '../tfLite/tfLiteModel'

    tflite_model = convertToTFLiteModelfromDisk('../savedModel',save=True)

    interpreter = tf.lite.Interpreter(model_content=tflite_model)
    interpreter.allocate_tensors()
    signatures = interpreter.get_signature_list()
    print(signatures)
    print()

    input_details = interpreter.get_input_details()
    output_details = interpreter.get_output_details()
    print('Input_details')
    print(input_details)
    print('Output_details')
    print(output_details)

    # Test the model without signature
    input_shape = input_details[0]['shape']
    input_data = np.array(np.ones(input_shape), dtype=np.float32)
    interpreter.set_tensor(input_details[0]['index'], input_data)

    interpreter.invoke()
    output_data = interpreter.get_tensor(output_details[0]['index'])
    print('Infer: ', output_data)
    print()

    # Test the model with default signature
    infer = interpreter.get_signature_runner()

    testvector = np.ones(15, dtype=np.float32)
    out = infer(input_1=testvector)
    print('Infer: ', out['dense_3'])
    return


def convertToTFLiteModelfromDisk(saved_model_dir, save=False):
    converter = tf.lite.TFLiteConverter.from_saved_model(saved_model_dir)  # path to the SavedModel directory
    converter.target_spec.supported_ops = [
        tf.lite.OpsSet.TFLITE_BUILTINS,  # enable TensorFlow Lite ops.
        tf.lite.OpsSet.SELECT_TF_OPS  # enable TensorFlow ops.
    ]
    tflite_model = converter.convert()

    if (save):
        with open('../tfLite/tfLiteModelConverted/generic.tflite', 'wb') as f:
            f.write(tflite_model)

    return tflite_model


if __name__ == '__main__':
    main()
