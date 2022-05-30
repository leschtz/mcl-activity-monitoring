import pandas as pd


def main():
    df = pd.read_csv("../offlineModels/HAPT_Data_Set/RawData/acc_exp19_user10.txt", sep=' ', index_col=False,
                     header=None)

    for i in range(1, 10):

        index1 = 2 * i - 1
        if index1 < 10:
            index1 = "0" + str(index1)
            print(index1)
        else:
            index1 = str(index1)
            print(index1)

        index2 = 2 * i
        if index2 < 10:
            index2 = "0" + str(index2)
            print(index2)
        else:
            index2 = str(index2)
            print(index2)

        index3 = "0" + str(i)
        path = "../offlineModels/HAPT_Data_Set/RawData/acc_exp" + index1 + "_user" + index3 + ".txt"
        df = df.append(pd.read_csv(path, sep=' ', index_col=False, header=None), ignore_index=True)
        # print(df.head)
        path = "../offlineModels/HAPT_Data_Set/RawData/acc_exp" + index2 + "_user" + index3 + ".txt"
        df = df.append(pd.read_csv(path, sep=' ', index_col=False, header=None), ignore_index=True)
        # print(df.head)

    for i in range(11, 31):
        index1 = 2 * i
        index1 = str(index1)
        print(index1)

        index2 = 2 * i + 1
        index2 = str(index2)
        print(index2)

        index3 = str(i)
        path = "../offlineModels/HAPT_Data_Set/RawData/acc_exp" + index1 + "_user" + index3 + ".txt"
        df = df.append(pd.read_csv(path, sep=' ', index_col=False, header=None), ignore_index=True)
        # print(df.head)
        path = "../offlineModels/HAPT_Data_Set/RawData/acc_exp" + index2 + "_user" + index3 + ".txt"
        df = df.append(pd.read_csv(path, sep=' ', index_col=False, header=None), ignore_index=True)
        # print(df.head)

    df.columns = ['x', 'y', 'z']
    print(df.head)
    print(df.head)
    x = df['x']
    y = df['y']
    z = df['z']

    print(x.max(), x.min())
    print(y.max(), y.min())
    print(z.max(), z.min())


if __name__ == '__main__':
    main()
