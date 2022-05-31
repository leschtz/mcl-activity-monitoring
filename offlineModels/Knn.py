import matplotlib.pyplot as plt

from sklearn.model_selection import GridSearchCV, train_test_split

import numpy as np
from sklearn.base import BaseEstimator
from sklearn.metrics import pairwise_distances, ConfusionMatrixDisplay
from sklearn.metrics import confusion_matrix
import pandas as pd


def eucledian_dist(p1, p2):
    dist = np.sqrt(np.sum((p1 - p2) ** 2))
    return dist


def plot_dataset(X_train, X_test, y_train, y_test):
    plt.scatter(x=X_train[:, 0], y=X_train[:, 1], c=y_train, label='train', cmap=plt.cm.RdYlBu)
    plt.scatter(x=X_test[:, 0], y=X_test[:, 1], c=y_test, label='test', cmap=plt.cm.RdYlBu, marker='x')
    plt.legend()


class KNearestNeighborsClassifier(BaseEstimator):

    def __init__(self, k=1):
        self.k = k

    def fit(self, X, y):
        self._X = X
        self._y = y

        return

    def score(self, X, y):
        y_pred = self.predict(X)
        return np.mean(y_pred == y)

    def predict(self, X):

        if (self.k >= self._X.shape[0]):
            print("k must be smaller than the number of data samples")
            return

        # print("_________")
        # print(eucledian_dist(self._X[0], X[0]))
        # print(eucledian_dist(self._X[0], X[1]))
        # print('!!!!!!!')
        # print(eucledian_dist(self._X[1], X[0]))
        # print(eucledian_dist(self._X[1], X[1]))

        # print(dist)

        predictions = np.array([])
        y_train = self._y.reshape(-1, 1)
        for x in X:  # iterating through every test data point
            dist = pairwise_distances(self._X, x.reshape(1, -1), metric='euclidean')
            neighbors = np.concatenate((dist, y_train), axis=1)
            neighbors_sorted = neighbors[neighbors[:, 0].argsort()]  # sorts training points on the basis of distance

            k_neighbors = neighbors_sorted[:self.k]  # selects k-nearest neighbors

            labels, occurences = np.unique(k_neighbors[:, -1], return_counts=True)

            predicted_y = labels[np.argmax(occurences)]

            predictions = np.append(predictions, predicted_y)

        return predictions


if __name__ == '__main__':
    df = pd.read_csv('neighbors.csv')
    print(df.head())
    #df = df.to_numpy()
    Y_col = 'label'
    X_cols = df.loc[:, df.columns != Y_col].columns
    X_train, X_test,y_train,y_test = train_test_split(df[X_cols],df[Y_col], test_size=0.2)


    X_train = X_train.to_numpy()
    X_test = X_test.to_numpy()
    y_train = y_train.to_numpy()
    y_test = y_test.to_numpy()

    knn = KNearestNeighborsClassifier()

    parameters = {'k': [1, 3, 5, 9, 25, 49]}
    clf = GridSearchCV(knn, parameters, return_train_score=True)

    clf.fit(X_train, y_train)
    test_score = clf.score(X_test, y_test)
    y_pred = clf.predict(X_test)
    print(f"Test Score: {test_score}")
    print(f"Dataset : {clf.best_params_}")

    cm = ConfusionMatrixDisplay(confusion_matrix(y_test,y_pred))
    cm.plot()
    plt.show()

    plt.figure()

    plot_dataset(X_train, X_test, y_train, y_test)
    plt.title(f'Dataset')
    #plt.savefig(f'images/Dataset.png')

    plt.show()
    plt.figure()
    plt.plot(clf.cv_results_['mean_train_score'], label="mean_train_score")
    plt.plot(clf.cv_results_['mean_test_score'], label="mean_test_score")
    plt.title(f'Dataset')
    plt.legend()
    #plt.savefig(f'images/Dataset_cvresults.png')
    plt.show()
