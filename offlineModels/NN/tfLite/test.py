import numpy as np
import tensorflow as tf
import pandas as pd
from matplotlib import pyplot as plt
from sklearn.metrics import classification_report, ConfusionMatrixDisplay, confusion_matrix
from sklearn.model_selection import train_test_split


def main():
    print(tf.__version__)
    path = '../tfLite/tfLiteModelConverted/model.tflite'
    path = '../tfLite/tfLiteModel'

    # tflite_model = convertToTFLiteModelfromDisk('../savedModel')
    # evaluateGeneric()
    train_evaluateTransfer()


def basic_test(tflite_model):
    interpreter = tf.lite.Interpreter(model_content=tflite_model)
    interpreter.allocate_tensors()
    signatures = interpreter.get_signature_list()
    # signatures[next(iter(signatures))]['inputs'][0]

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


def train_evaluateTransfer():
    x = pd.read_csv("../../../offlineModels/neighbors.csv", sep=',', index_col=False, header=None)

    targets = x.iloc[:, -1]
    targets = targets - 1

    x = x.iloc[:, :-1]

    x_train, x_test, y_train, y_test = train_test_split(x, targets, test_size=0.2, random_state=33)

    x_train.reset_index(inplace=True)
    x_test.reset_index(inplace=True)

    y_train = y_train.to_numpy(dtype=np.float32)

    path = '../tfLite/tfLiteModelConverted/model.tflite'

    interpreter = tf.lite.Interpreter(model_path=path)
    interpreter.allocate_tensors()

    y_pred = []
    signatures = interpreter.get_signature_list()
    print(signatures)

    load = interpreter.get_signature_runner('load')
    train = interpreter.get_signature_runner('train')
    infer = interpreter.get_signature_runner('infer')

    print(x_train.head)

    for i in range(x_train.shape[0]):
        feature = np.array(x_train.loc[i], dtype=np.float32)
        out = load(feature=feature)
        out = out['bottleneck']
        loss = train(bottleneck=out, label=y_train[i])
        loss = loss['loss']
        print(loss)

    for i in range(x_test.shape[0]):
        feature = np.array(x_test.loc[i], dtype=np.float32)
        out = infer(feature=feature)
        out = out['output']
        out = out.round()
        out = out.argmax(1)
        y_pred.append(out[0])

    # test = np.array(
    #     [0.09823959477947034, -0.4962716015028138, -0.03393575923665419, 0.0005881721084870351, 0.00016481368831409272,
    #      0.0002508402590048587, 0.0004903845040195975, 0.00013422125607971404, 0.0001972243708840582,
    #      0.09912841625750946, -0.4960162475402238, -0.0335377943239229, 0.09748869551865891, -0.496490916118168,
    #      -0.034225042928912486], dtype=np.float32)
    # test2 = np.array(
    #     [0.04357967369149907, - 0.005970221250665042, - 0.03505434399423213, - 0.9953811604338987, - 0.9883658626633201,
    #      - 0.9373820052025755, - 0.9950070451399888, - 0.9888155772529818, - 0.9533252009660302, - 0.7947963690882799,
    #      - 0.7448928173638687, - 0.6484472451236181, 0.8417955744259182, 0.708440184117894, 0.6517164861985676],
    #     dtype=np.float32)
    #
    # out = infer(input_1=test)
    # print('Infer: ', out['dense_3'])
    # out = out['dense_3']
    # out = out.round()
    # out = out.argmax(1)
    # print(out)
    # out = infer(input_1=test2)
    # print('Infer: ', out['dense_3'])
    # out = out['dense_3']
    # out = out.round()
    # out = out.argmax(1)
    # print(out)
    #
    # print(y_pred)

    print('Classification report')
    print(classification_report(y_test, y_pred))

    figure_path = '../../../offlineModels/NN/Results/'

    cm = ConfusionMatrixDisplay(confusion_matrix(y_test, y_pred))
    cm.plot()
    plt.savefig(figure_path + 'GenericCM.png')
    plt.show()


def evaluateGeneric():
    x = pd.read_csv("../../../offlineModels/neighbors.csv", sep=',', index_col=False, header=None)

    targets = x.iloc[:, -1]
    targets = targets - 1

    x = x.iloc[:, :-1]

    print(x.head())
    print(targets.head())
    path = '../tfLite/tfLiteModelConverted/generic.tflite'

    interpreter = tf.lite.Interpreter(model_path=path)
    interpreter.allocate_tensors()

    y_pred = []

    infer = interpreter.get_signature_runner()

    test = np.array(
        [0.09823959477947034, -0.4962716015028138, -0.03393575923665419, 0.0005881721084870351, 0.00016481368831409272,
         0.0002508402590048587, 0.0004903845040195975, 0.00013422125607971404, 0.0001972243708840582,
         0.09912841625750946, -0.4960162475402238, -0.0335377943239229, 0.09748869551865891, -0.496490916118168,
         -0.034225042928912486], dtype=np.float32)
    test2 = np.array(
        [0.04357967369149907, - 0.005970221250665042, - 0.03505434399423213, - 0.9953811604338987, - 0.9883658626633201,
         - 0.9373820052025755, - 0.9950070451399888, - 0.9888155772529818, - 0.9533252009660302, - 0.7947963690882799,
         - 0.7448928173638687, - 0.6484472451236181, 0.8417955744259182, 0.708440184117894, 0.6517164861985676],
        dtype=np.float32)

    for i in range(x.shape[0]):
        out = infer(input_1=np.array(x.loc[i], dtype=np.float32))
        out = out['dense_3']
        out = out.round()
        out = out.argmax(1)
        y_pred.append(out[0])

    # testvector = np.ones(15, dtype=np.float32)
    out = infer(input_1=test)
    print('Infer: ', out['dense_3'])
    out = out['dense_3']
    out = out.round()
    out = out.argmax(1)
    print(out)
    out = infer(input_1=test2)
    print('Infer: ', out['dense_3'])
    out = out['dense_3']
    out = out.round()
    out = out.argmax(1)
    print(out)
    #
    # print(y_pred)

    print('Classification report')
    print(classification_report(targets, y_pred))

    figure_path = '../../../offlineModels/NN/Results/'

    cm = ConfusionMatrixDisplay(confusion_matrix(targets, y_pred))
    cm.plot()
    plt.savefig(figure_path + 'GenericCM.png')
    plt.show()


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
