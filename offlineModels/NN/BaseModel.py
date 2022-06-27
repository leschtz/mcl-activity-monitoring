import os
import random

import keras
import pandas as pd
import tensorflow as tf
import matplotlib.pyplot as plt
from kerashypetune import KerasGridSearch
from sklearn.model_selection import train_test_split
from sklearn.metrics import confusion_matrix, classification_report, ConfusionMatrixDisplay
from keras.models import Sequential
from keras.layers import Dense, Dropout
from keras.losses import categorical_crossentropy
from keras.metrics import categorical_accuracy
from keras.callbacks import EarlyStopping
import numpy as np
from sklearn.preprocessing import LabelEncoder

from keras.utils import np_utils
from sklearn.model_selection import KFold

# https://tugraz.webex.com/meet/saukh
# https://tugraz.webex.com/tugraz-de/url.php?frompanel=false&gourl=https%3A%2F%2Fgithub.com%2Fosaukh%2Fmobile_computing_lab%2Fblob%2Fmaster%2Fcolab%2FWS04_TransferLearning_Personalization.ipynb
# https://tugraz.webex.com/tugraz-de/url.php?frompanel=false&gourl=https%3A%2F%2Fgithub.com%2Fosaukh%2Fmobile_computing_lab%2Ftree%2Fmaster%2Fcode%2FModelPersonalization
from offlineModels.NN.tflitwrapper import TransferLearningModel

num_features = None


def main():
    saved_basemodel_dir = '../offlineModels/NN/Hapt_neighbors'

    model = keras.models.load_model(saved_basemodel_dir)
    # print(model.summary())

    #model = train_base_model(HAPT=True)
    model.summary()
    #
    #model.save(saved_basemodel_dir)

    model = cutOffHead(model)
    model.summary()

    #createTransferModel(model,  modelname='Hapt_neighbors.tflite')

    #
    convert_and_save(model, '../offlineModels/NN/tfLite/tfLiteModel' , tfmodel_name='custom.tflite')
    # convert_and_save(model, '../offlineModels/NN/tfLite/test')


def train_base_model(noGrid=True, fixedData=False, HAPT=False):
    x_train = pd.read_csv("../offlineModels/HAPT_Data_Set/Train/X_train.txt", sep=' ', index_col=False, header=None)
    y_train = pd.read_csv("../offlineModels/HAPT_Data_Set/Train/y_train.txt", sep=' ', index_col=False, header=None)
    x_test = pd.read_csv("../offlineModels/HAPT_Data_Set/Test/X_test.txt", sep=' ', index_col=False, header=None)
    y_test = pd.read_csv("../offlineModels/HAPT_Data_Set/Test/y_test.txt", sep=' ', index_col=False, header=None)

    frames_x = [x_train, x_test]
    frames_y = [y_train, y_test]
    x_complete = pd.concat(frames_x)
    targets = pd.concat(frames_y)

    # let activity labels start at 0
    y_train = y_train - 1
    y_test = y_test - 1
    targets = targets - 1

    x_train['label'] = y_train
    x_test['label'] = y_test
    x_complete['label'] = targets

    # drop rows classified as activity transitions
    x_train.drop(x_train[(x_train['label'] > 5)].index, inplace=True, )
    x_train.reset_index()
    x_test.drop(x_test[(x_test['label'] > 5)].index, inplace=True, )
    x_test.reset_index()
    x_complete.drop(x_complete[(x_complete['label'] > 5)].index, inplace=True, )
    x_complete.reset_index()

    y_train = x_train.iloc[:, -1]
    y_test = x_test.iloc[:, -1]
    targets = x_complete.iloc[:, -1]

    x_train.drop('label', inplace=True, axis=1)
    x_test.drop('label', inplace=True, axis=1)
    x_complete.drop('label', inplace=True, axis=1)

    # read in feature names as list
    file = open("../offlineModels/HAPT_Data_Set/features.txt")
    file_contents = file.read()
    features = file_contents.splitlines()

    for x in range(len(features)):
        features[x] = features[x].strip()

    file.close()

    # set feature names as header
    x_train.columns = features
    x_test.columns = features
    x_complete.columns = features
    # print(x_complete.columns)

    # #   MIN,MAX,AV ACC+Gyro
    # wantedFeatures = ['tBodyAcc-Mean-1', 'tBodyAcc-Mean-2', 'tBodyAcc-Mean-3', 'tBodyAcc-Max-1', 'tBodyAcc-Max-2',
    #                   'tBodyAcc-Max-3', 'tBodyAcc-Min-1', 'tBodyAcc-Min-2', 'tBodyAcc-Min-3', 'tBodyGyro-Mean-1',
    #                   'tBodyGyro-Mean-2', 'tBodyGyro-Mean-3', 'tBodyGyro-Max-1', 'tBodyGyro-Max-2', 'tBodyGyro-Max-3',
    #                   'tBodyGyro-Min-1', 'tBodyGyro-Min-2', 'tBodyGyro-Min-3']
    # #   MIN,MAX,AV ACC
    # wantedFeatures = ['tBodyAcc-Mean-1', 'tBodyAcc-Mean-2', 'tBodyAcc-Mean-3', 'tBodyAcc-Max-1', 'tBodyAcc-Max-2',
    #                   'tBodyAcc-Max-3', 'tBodyAcc-Min-1', 'tBodyAcc-Min-2', 'tBodyAcc-Min-3']
    #
    # #   MIN,MAX,AV,Sigma ACC
    # wantedFeatures = ['tBodyAcc-Mean-1', 'tBodyAcc-Mean-2', 'tBodyAcc-Mean-3', 'tBodyAcc-Max-1', 'tBodyAcc-Max-2',
    #                   'tBodyAcc-Max-3', 'tBodyAcc-Min-1', 'tBodyAcc-Min-2', 'tBodyAcc-Min-3', 'tBodyAcc-STD-1',
    #                   'tBodyAcc-STD-2', 'tBodyAcc-STD-3']
    #
    # #   MIN,MAX,AV,Sigma,en ACC
    # wantedFeatures = ['tBodyAcc-Mean-1', 'tBodyAcc-Mean-2', 'tBodyAcc-Mean-3', 'tBodyAcc-Max-1', 'tBodyAcc-Max-2',
    #                   'tBodyAcc-Max-3', 'tBodyAcc-Min-1', 'tBodyAcc-Min-2', 'tBodyAcc-Min-3', 'tBodyAcc-STD-1',
    #                   'tBodyAcc-STD-2', 'tBodyAcc-STD-3', 'tBodyAcc-Energy-1', 'tBodyAcc-Energy-2', 'tBodyAcc-Energy-3']

    #   MIN,MAX,AV,Sigma,MAD ACC
    wantedFeatures = ['tBodyAcc-Mean-1', 'tBodyAcc-Mean-2', 'tBodyAcc-Mean-3', 'tBodyAcc-Max-1', 'tBodyAcc-Max-2',
                      'tBodyAcc-Max-3', 'tBodyAcc-Min-1', 'tBodyAcc-Min-2', 'tBodyAcc-Min-3', 'tBodyAcc-STD-1',
                      'tBodyAcc-STD-2', 'tBodyAcc-STD-3', 'tBodyAcc-Mad-1', 'tBodyAcc-Mad-2', 'tBodyAcc-Mad-3']

    x_train = x_train[x_train.columns.intersection(wantedFeatures)]
    x_test = x_test[x_test.columns.intersection(wantedFeatures)]
    x_complete = x_complete[x_complete.columns.intersection(wantedFeatures)]

    if (not HAPT):
        df = pd.read_csv('../offlineModels/NN/data.csv', sep=',', index_col=False, header=None)
        df.dropna(inplace=True)
        df.to_csv('../offlineModels/NN/data.csv', sep=',', index=False, header=False)

        targets = df.iloc[:, -1]
        targets = targets - 1
        x_complete = df.iloc[:, :-1]
        #
    df = pd.read_csv('../offlineModels/NN/data.csv', sep=',', index_col=False, header=None)
    df.dropna(inplace=True)
    y_val = df.iloc[:, -1]
    y_val = y_val - 1
    x_val = df.iloc[:, :-1]
    # print(x_complete.head())
    # print(list(targets))

    if not fixedData:
        x_train, x_test, y_train, y_test = train_test_split(x_complete, targets, test_size=0.2, random_state=33)
        print('Create random train-test-split')
    else:
        print('fix train-test-split is used')
    global num_features
    num_features = x_train.shape[1]

    # convert to one hot encoded)
    encoder = LabelEncoder()
    encoder.fit(y_train)
    encoded_Y_train = encoder.transform(y_train)
    encoded_Y_test = encoder.transform(y_test)

    y_train = np_utils.to_categorical(encoded_Y_train)
    y_test_hot = np_utils.to_categorical(encoded_Y_test)

    ### Single Train and test run

    if (noGrid):

        es = EarlyStopping(patience=5, verbose=1, min_delta=0.001, monitor='loss', mode='auto',
                           restore_best_weights=True)

        param = {'activation': 'relu', 'optimizer': 'Adamax', 'dropout_rate': 0.1, 'nodecount': 256, 'nodecount2': 256,
                 'nodecount3': 128}
        model = create_model(param=param)

        history = model.fit(x_train, y_train, epochs=300, batch_size=50, validation_data=(x_test, y_test_hot),
                            verbose=1)  # , callbacks=[es]

        print()
        print('Score from model evaluation:')
        score = model.evaluate(x_test, y_test_hot, verbose=1)

        print()

        y_pred = model.predict(x_test)
        y_pred = y_pred.round()
        y_pred = y_pred.argmax(1)

        cm = ConfusionMatrixDisplay(confusion_matrix(y_test, y_pred))
        cm.plot()
        # plt.savefig(figure_path + 'CM.png')
        plt.show()

        print('Classification report')
        print(classification_report(y_test, y_pred))

        y_pred = model.predict(x_val)
        y_pred = y_pred.round()
        y_pred = y_pred.argmax(1)

        print('Classification report')
        print(classification_report(y_val, y_pred))

        cm = ConfusionMatrixDisplay(confusion_matrix(y_val, y_pred))
        cm.plot()
        # plt.savefig(figure_path + 'CM.png')
        plt.show()

        # x = pd.read_csv("../offlineModels/neighbors.csv", sep=',', index_col=False, header=None)
        #
        # targets = x.iloc[:, -1]
        # targets = targets - 1
        # x = x.iloc[:, :-1]
        #
        # y_pred = model.predict(x)
        # y_pred = y_pred.round()
        # y_pred = y_pred.argmax(1)
        #
        # print('Classification report')
        # print(classification_report(targets, y_pred))

        return model
        figure_path = '../offlineModels/NN/Results/'

        tf.keras.utils.plot_model(model, figure_path + 'Modelplot.png', show_shapes=True)

        cm = ConfusionMatrixDisplay(confusion_matrix(y_test, y_pred))
        cm.plot()
        plt.savefig(figure_path + 'CM.png')
        plt.show()

        best_epoch = es.best_epoch

        print(history.history.keys())

        # print(epochs)
        plt.plot(history.history['loss'], 'g', label='Training loss')
        plt.plot(history.history['val_loss'], 'b', label='validation loss')
        plt.axvline(x=best_epoch, color='r', label='best_epoch')
        plt.title('Training and Validation loss')
        plt.xlabel('Epochs')
        plt.ylabel('Loss')
        plt.legend()
        plt.savefig(figure_path + 'loss.png')
        plt.show()

        # epochs = range(1, num_epochs+1)
        plt.plot(history.history['precision'], 'g', label='Training precision')
        plt.plot(history.history['val_precision'], 'b', label='validation precision')
        plt.axvline(x=best_epoch, color='r', label='best_epoch')
        plt.title('Training and Validation precision')
        plt.xlabel('Epochs')
        plt.ylabel('Loss')
        plt.legend()
        # plt.savefig('/content/drive/MyDrive/NLP/Bert_' + dataset + '/figures/bert' + dataset + '_precision.png')
        plt.savefig(figure_path + 'precision.png')
        plt.show()

        plt.plot(history.history['categorical_accuracy'], 'g', label='Training categorical_accuracy')
        plt.plot(history.history['val_categorical_accuracy'], 'b', label='validation categorical_accuracy')
        plt.axvline(x=best_epoch, color='r', label='best_epoch')
        plt.title('Training and Validation accuracy')
        plt.xlabel('Epochs')
        plt.ylabel('Loss')
        plt.legend()
        # plt.savefig('/content/drive/MyDrive/NLP/Bert_' + dataset + '/figures/bert' + dataset + '_accuracy.png')
        plt.savefig(figure_path + 'accuracy.png')
        plt.show()

        return model

        ### Gridsearch Part
    else:

        param_grid = {'activation': ['relu', 'tanh', 'sigmoid', 'linear', 'softmax'],
                      'optimizer': ['Adam', 'Adamax', 'Nadam', 'SGD', 'RMSprop', 'Adagrad', 'Adadelta'],
                      'dropout_rate': [0.0, 0.1, 0.2, 0.3, 0.4, 0.5],
                      'epochs': 150,
                      'batch_size': [25, 50],
                      'nodecount': [64, 128, 256, 512],
                      'nodecount2': [64, 128, 256, 512],
                      'nodecount3': [64, 128, 256, 512]
                      }

        learning_rate = [0.01, 0.001, 0.0001]

        cv = KFold(n_splits=3, random_state=33, shuffle=True)
        es = EarlyStopping(patience=5, verbose=1, min_delta=0.001, monitor='loss', mode='auto',
                           restore_best_weights=True)

        kgs = KerasGridSearch(create_model, param_grid, monitor='categorical_accuracy', greater_is_better=True,
                              tuner_verbose=1)
        grid_result = kgs.search(x_train, y_train, validation_data=(x_test, y_test_hot), callbacks=[es])

        print("Best: %f using %s" % (kgs.best_score, kgs.best_params))


def create_model(param=None):
    if param is None:
        param = {'activation': 'relu', 'optimizer': 'Adamax', 'dropout_rate': 0.1, 'nodecount': 128, 'nodecount2': 256,
                 'nodecount3': 128}
    set_seed(11)
    model = Sequential()
    model.add(keras.Input(shape=(num_features,)))
    model.add(keras.layers.Dense(param['nodecount'], activation=param['activation']))
    model.add(Dropout(param['dropout_rate']))
    model.add(keras.layers.Dense(param['nodecount2'], activation=param['activation']))
    model.add(Dropout(param['dropout_rate']))
    model.add(Dense(param['nodecount3'], activation=param['activation']))
    model.add(Dropout(param['dropout_rate']))
    model.add(Dense(param['nodecount3'], activation=param['activation'], name='base'))
    model.add(Dense(6, activation='softmax'))

    model.compile(optimizer=param['optimizer'], loss=categorical_crossentropy,
                  metrics=[categorical_accuracy, tf.keras.metrics.Precision(name='precision'),
                           tf.keras.metrics.Recall(name='recall')])
    return model


def cutOffHead(model):
    model2 = tf.keras.models.Model(model.input, model.get_layer('base').output)
    model2.trainable = False
    return model2


def createTransferModel(model, modelname):
    model.trainable = False

    x = model(model.inputs, training=False)
    # Convert features of shape `base_model.output_shape[1:]` to vectors
    outputs = Dense(6, activation='softmax')(x)
    # A Dense classifier with a single unit (binary classification)

    model = keras.Model(model.inputs, outputs)

    model.compile(optimizer='adam', loss=categorical_crossentropy,
                  metrics=[categorical_accuracy, tf.keras.metrics.Precision(name='precision'),
                           tf.keras.metrics.Recall(name='recall')])

    print(model.summary())

    df = pd.read_csv('../offlineModels/Data/right-front-pocket-jean.csv', sep=',', index_col=False, header=None)
    df.dropna(inplace=True)

    targets = df.iloc[:, -1]
    #targets = targets - 1
    x_complete = df.iloc[:, :-1]

    x_train, x_test, y_train, y_test = train_test_split(x_complete, targets, test_size=0.2, random_state=33)

    encoder = LabelEncoder()
    encoder.fit(y_train)
    encoded_Y_train = encoder.transform(y_train)
    encoded_Y_test = encoder.transform(y_test)

    y_train = np_utils.to_categorical(encoded_Y_train)
    y_test_hot = np_utils.to_categorical(encoded_Y_test)

    es = EarlyStopping(patience=5, verbose=1, min_delta=0.001, monitor='loss', mode='auto',
                       restore_best_weights=True)

    model.fit(x_train, y_train, epochs=250, batch_size=25, validation_data=(x_test, y_test_hot),
              verbose=0, callbacks=[es])

    print('Score from model evaluation:')
    score = model.evaluate(x_test, y_test_hot, verbose=1)

    print()

    y_pred = model.predict(x_test)
    y_pred = y_pred.round()
    y_pred = y_pred.argmax(1)

    print('Classification report')
    print(classification_report(y_test, y_pred))

    convertToTFLiteModelfromKeras(model,modelname)


def convertToTFLiteModelfromKeras(model, modelname):
    converter = tf.lite.TFLiteConverter.from_keras_model(model)
    converter.target_spec.supported_ops = [
        tf.lite.OpsSet.TFLITE_BUILTINS,  # enable TensorFlow Lite ops.
        tf.lite.OpsSet.SELECT_TF_OPS  # enable TensorFlow ops.
    ]
    tflite_model = converter.convert()

    # Save the model.
    with open('../offlineModels/NN/tfLite/tfLiteModelConverted/' + modelname, 'wb') as f:
        f.write(tflite_model)


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


def set_seed(seed):
    tf.random.set_seed(seed)
    os.environ['PYTHONHASHSEED'] = str(seed)
    np.random.seed(seed)
    random.seed(seed)


def convert_and_save(model, saved_model_dir, tfmodel_name='model.tflite'):
    """Converts and saves the TFLite Transfer Learning model.

  Args:
    saved_model_dir: A directory path to save a converted model.
    model: model to convert
    :param tfmodel_name:
  """
    tl_model = TransferLearningModel(model)

    tf.saved_model.save(
        tl_model,
        saved_model_dir,
        signatures={
            'load': tl_model.load.get_concrete_function(),
            'train': tl_model.train.get_concrete_function(),
            'infer': tl_model.infer.get_concrete_function(),
            'save': tl_model.save.get_concrete_function(),
            'restore': tl_model.restore.get_concrete_function(),
            'initialize': tl_model.initialize_weights.get_concrete_function(),
        })

    # Convert the model
    converter = tf.lite.TFLiteConverter.from_saved_model(saved_model_dir)
    converter.target_spec.supported_ops = [
        tf.lite.OpsSet.TFLITE_BUILTINS,  # enable TensorFlow Lite ops.
        tf.lite.OpsSet.SELECT_TF_OPS  # enable TensorFlow ops.
    ]
    converter.experimental_enable_resource_variables = True
    tflite_model = converter.convert()

    # model_file_path = os.path.join('model.tflite')
    model_file_path = '../offlineModels/NN/tfLite/tfLiteModelConverted/' + tfmodel_name
    with open(model_file_path, 'wb') as model_file:
        model_file.write(tflite_model)


if __name__ == '__main__':
    main()
