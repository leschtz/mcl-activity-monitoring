import os
import random

import keras
import pandas as pd
import tensorflow as tf
import matplotlib.pyplot as plt
from kerashypetune import KerasGridSearchCV, KerasGridSearch
from sklearn.model_selection import train_test_split, GridSearchCV
from sklearn.metrics import confusion_matrix, classification_report, ConfusionMatrixDisplay
from keras.models import Sequential
from keras.layers import Dense, Dropout
from keras.losses import sparse_categorical_crossentropy, categorical_crossentropy
from keras.metrics import categorical_accuracy
from keras.callbacks import EarlyStopping
from keras.optimizers import Adam
from keras.optimizers.schedules.learning_rate_schedule import ExponentialDecay
from keras.utils import np_utils
import numpy as np
import seaborn as sns
from sklearn.preprocessing import LabelEncoder

from keras.utils import np_utils
from sklearn.model_selection import cross_val_score
from sklearn.model_selection import KFold
from scikeras.wrappers import KerasClassifier

# https://tugraz.webex.com/meet/saukh
# https://tugraz.webex.com/tugraz-de/url.php?frompanel=false&gourl=https%3A%2F%2Fgithub.com%2Fosaukh%2Fmobile_computing_lab%2Fblob%2Fmaster%2Fcolab%2FWS04_TransferLearning_Personalization.ipynb
# https://tugraz.webex.com/tugraz-de/url.php?frompanel=false&gourl=https%3A%2F%2Fgithub.com%2Fosaukh%2Fmobile_computing_lab%2Ftree%2Fmaster%2Fcode%2FModelPersonalization
fixedData = False
num_features = None


def main():
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

    #   MIN,MAX,AV ACC+Gyro
    wantedFeatures = ['tBodyAcc-Mean-1', 'tBodyAcc-Mean-2', 'tBodyAcc-Mean-3', 'tBodyAcc-Max-1', 'tBodyAcc-Max-2',
                      'tBodyAcc-Max-3', 'tBodyAcc-Min-1', 'tBodyAcc-Min-2', 'tBodyAcc-Min-3', 'tBodyGyro-Mean-1',
                      'tBodyGyro-Mean-2', 'tBodyGyro-Mean-3', 'tBodyGyro-Max-1', 'tBodyGyro-Max-2', 'tBodyGyro-Max-3',
                      'tBodyGyro-Min-1', 'tBodyGyro-Min-2', 'tBodyGyro-Min-3']
    #   MIN,MAX,AV ACC
    wantedFeatures = ['tBodyAcc-Mean-1', 'tBodyAcc-Mean-2', 'tBodyAcc-Mean-3', 'tBodyAcc-Max-1', 'tBodyAcc-Max-2',
                      'tBodyAcc-Max-3', 'tBodyAcc-Min-1', 'tBodyAcc-Min-2', 'tBodyAcc-Min-3']

    #   MIN,MAX,AV,Sigma ACC
    wantedFeatures = ['tBodyAcc-Mean-1', 'tBodyAcc-Mean-2', 'tBodyAcc-Mean-3', 'tBodyAcc-Max-1', 'tBodyAcc-Max-2',
                      'tBodyAcc-Max-3', 'tBodyAcc-Min-1', 'tBodyAcc-Min-2', 'tBodyAcc-Min-3', 'tBodyAcc-STD-1',
                      'tBodyAcc-STD-2', 'tBodyAcc-STD-3']

    #   MIN,MAX,AV,Sigma,en ACC
    wantedFeatures = ['tBodyAcc-Mean-1', 'tBodyAcc-Mean-2', 'tBodyAcc-Mean-3', 'tBodyAcc-Max-1', 'tBodyAcc-Max-2',
                      'tBodyAcc-Max-3', 'tBodyAcc-Min-1', 'tBodyAcc-Min-2', 'tBodyAcc-Min-3', 'tBodyAcc-STD-1',
                      'tBodyAcc-STD-2', 'tBodyAcc-STD-3', 'tBodyAcc-Energy-1', 'tBodyAcc-Energy-2', 'tBodyAcc-Energy-3']

    #   MIN,MAX,AV,Sigma,MAD ACC
    wantedFeatures = ['tBodyAcc-Mean-1', 'tBodyAcc-Mean-2', 'tBodyAcc-Mean-3', 'tBodyAcc-Max-1', 'tBodyAcc-Max-2',
                      'tBodyAcc-Max-3', 'tBodyAcc-Min-1', 'tBodyAcc-Min-2', 'tBodyAcc-Min-3', 'tBodyAcc-STD-1',
                      'tBodyAcc-STD-2', 'tBodyAcc-STD-3', 'tBodyAcc-Mad-1', 'tBodyAcc-Mad-2', 'tBodyAcc-Mad-3']

    x_train = x_train[x_train.columns.intersection(wantedFeatures)]
    x_test = x_test[x_test.columns.intersection(wantedFeatures)]
    x_complete = x_complete[x_complete.columns.intersection(wantedFeatures)]

    if not fixedData:
        x_train, x_test, y_train, y_test = train_test_split(x_complete, targets, test_size=0.2, random_state=33)
        print('Create random train-test-split')
    else:
        print('fix train-test-split is used')
    global num_features
    num_features = x_train.shape[1]

    encoder = LabelEncoder()
    encoder.fit(y_train)
    encoded_Y_train = encoder.transform(y_train)
    # convert integers to dummy variables (i.e. one hot encoded)
    y_train = np_utils.to_categorical(encoded_Y_train)
    encoded_Y_test = encoder.transform(y_test)
    y_test_hot = np_utils.to_categorical(encoded_Y_test)

    #
    # print(model.summary())
    # callback = tf.keras.callbacks.EarlyStopping(monitor='loss', patience=10)
    #
    # # estimator = KerasClassifier(model=model, epochs=100, batch_size=10, verbose=1, callbacks=[callback])
    # # kfold = KFold(n_splits=5, shuffle=True)
    # # results = cross_val_score(estimator, x_train, y_train, cv=kfold)
    # # print("Baseline: %.2f%% (%.2f%%)" % (results.mean() * 100, results.std() * 100))
    #

    ### ????????????????????????????
    ### Single Train and test run

    es = EarlyStopping(patience=5, verbose=1, min_delta=0.001, monitor='loss', mode='auto',
                       restore_best_weights=True)

    param = {'activation': 'relu', 'optimizer': 'Adamax', 'dropout_rate': 0.0, 'nodecount': 128, 'nodecount2': 256}
    model = create_model(param=param)
    # print(model.summary())
    print(x_train.columns)
    history = model.fit(x_train, y_train, epochs=200, batch_size=50, verbose=0, callbacks=[es])

    print()
    print('Score from evaluate:')
    score = model.evaluate(x_test, y_test_hot)

    print(score)
    print()

    y_pred = model.predict(x_test)
    y_pred = y_pred.round()
    y_pred = y_pred.argmax(1)

    print('Classification report')
    print(classification_report(y_test, y_pred))

    cm = ConfusionMatrixDisplay(confusion_matrix(y_test, y_pred))
    cm.plot()
    plt.show()

    ### ????????????????????????????

    ### !!!!!!!!!!!!!!
    ### Gridsearch Part

    #
    # param_grid = {'activation': 'relu',  # ['relu', 'tanh', 'sigmoid', 'linear', 'softmax'],
    #               'optimizer': ['Adam', 'Adamax', 'Nadam'],  # 'SGD', 'RMSprop', 'Adagrad', 'Adadelta',
    #               'dropout_rate': [0.0, 0.1, 0.2],
    #               'epochs': 150,
    #               'batch_size': [25, 50],
    #               'nodecount': [ 64, 128, 256, 512],
    #               'nodecount2': [ 64, 128, 256, 512]
    #               }
    #
    # learning_rate = [0.01, 0.001, 0.0001]
    #
    # cv = KFold(n_splits=3, random_state=33, shuffle=True)
    # es = EarlyStopping(patience=5, verbose=1, min_delta=0.001, monitor='val_loss', mode='auto',
    #                    restore_best_weights=True)
    #
    # kgs = KerasGridSearch(create_model, param_grid, monitor='val_categorical_accuracy', greater_is_better=True,
    #                       tuner_verbose=1)
    # grid_result = kgs.search(x_train, y_train, validation_data=(x_test, y_test_hot), callbacks=[es])
    #
    # print("Best: %f using %s" % (kgs.best_score, kgs.best_params))

    ### !!!!!!!!!!!!!!!

    # kgs.search(x=x_train, y=y_train)

    # model = KerasClassifier(model=create_model(x_train.shape[1]), epochs=100, batch_size=10, verbose=1,
    #                         activation='relu',
    #                         optimizer='SGD',
    #                         dropout_rate=0.0)

    # param_grid = dict(optimizer=optimizer, dropout_rate=dropout_rate, activation=activation)
    # grid = GridSearchCV(estimator=model, param_grid=param_grid, n_jobs=-1, cv=3)
    # grid_result = grid.fit(x_train, y_train)
    # summarize results
    # print("Best: %f using %s" % (grid_result.best_score_, grid_result.best_params_))
    # means = grid_result.cv_results_['mean_test_score']
    # stds = grid_result.cv_results_['std_test_score']
    # params = grid_result.cv_results_['params']
    # for mean, stdev, param in zip(means, stds, params):
    #     print("%f (%f) with: %r" % (mean, stdev, param))
    #

    # print('§§§§§§§§§§§§§§§§§§§§§§§§§§§§§§§§§§§§§§§§§§§§§§§§§§§§§§§§§§§§§§§§§§§§')

    # Only ACC
    # Best: 0.690780
    # using
    # {'activation': 'relu', 'optimizer': 'RMSprop', 'dropout_rate': 0.1, 'epochs': 30, 'batch_size': 25,
    #  'steps_per_epoch': 323}
    # model = Sequential()
    # model.add(keras.Input(shape=(9,)))
    # model.add(keras.layers.Dense(256, activation=param['activation']))
    # # , kernel_regularizer=tf.keras.regularizers.L1(0.001)
    # # model.add(keras.layers.Dropout(0.1))
    # # model.add(keras.layers.Dense(256, activation=activation))
    # model.add(Dropout(param['dropout_rate']))
    # model.add(Dense(128, activation=param['activation']))
    # model.add(Dense(6, activation='softmax'))


def create_model(param=None):
    if param is None:
        param = {'activation': 'relu', 'optimizer': 'Adamax', 'dropout_rate': 0.0, 'nodecount': 64, 'nodecount2': 64}
    set_seed(11)
    model = Sequential()
    model.add(keras.Input(shape=(num_features,)))
    model.add(keras.layers.Dense(param['nodecount'], activation=param['activation']))
    model.add(Dropout(param['dropout_rate']))
    model.add(keras.layers.Dense(param['nodecount2'], activation=param['activation']))
    model.add(Dropout(param['dropout_rate']))
    model.add(Dense(param['nodecount'] / 2, activation=param['activation']))
    model.add(Dense(6, activation='softmax'))
    #

    # lr_schedule = ExponentialDecay(
    #     initial_learning_rate=1e-2,
    #     decay_steps=10000,
    #     decay_rate=0.9)
    #
    # optimizer = Adam(learning_rate=lr_schedule)
    # optimizer = Adam(learning_rate=0.001)
    #
    model.compile(optimizer=param['optimizer'], loss=categorical_crossentropy, metrics=[categorical_accuracy])
    return model


def set_seed(seed):
    tf.random.set_seed(seed)
    os.environ['PYTHONHASHSEED'] = str(seed)
    np.random.seed(seed)
    random.seed(seed)


if __name__ == '__main__':
    main()
