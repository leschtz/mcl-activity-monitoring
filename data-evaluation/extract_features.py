import pandas as pd
import numpy as np
#!!!!!!!!!!!!
# Add this line to neighbors.csv at the top 'gx,gy,gz,ax,ay,az,label'

CONST_G = 9.80665

#X = 2.004166708636188 -1.976388870971105
#Y = 1.716666672288882 -2.009722276169204
#Z = 1.979166654737614 -1.837500071636558

X_MAX = 2.004166708636188 
X_MIN = -1.976388870971105
Y_MAX = 1.716666672288882 
Y_MIN = -2.009722276169204
Z_MAX = 1.979166654737614 
Z_MIN = -1.837500071636558

def main():
    frame = pd.read_csv('bigger-sample.csv')

    normalized_frame = pd.DataFrame()

    normalized_frame["x"] = frame["ax"] / CONST_G
    normalized_frame["y"] = frame["ay"] / CONST_G
    normalized_frame["z"] = frame["az"] / CONST_G
    #normalized_frame["label"] = frame["label"]

    normalized_frame["x"] = 2 * ((normalized_frame["x"]  - X_MIN) / (X_MAX - X_MIN)) -1
    normalized_frame["y"] = 2* ((normalized_frame["y"] - Y_MIN) / (Y_MAX - Y_MIN)) -1
    normalized_frame["z"] = 2 * ((normalized_frame["z"] - Z_MIN) / (Z_MAX - Z_MIN)) -1

    
    group = normalized_frame.groupby(normalized_frame.index // 10)
    labels = pd.DataFrame(frame.groupby(frame.index // 10).min())["label"]

    df_min = pd.DataFrame(group.min())
    df_max = pd.DataFrame(group.max())
    df_avg = pd.DataFrame(group.mean())
    df_std = pd.DataFrame(group.std())
    df_mad = pd.DataFrame(group.mad())
    #print(df_min)

    data = pd.concat([df_min, df_max, df_avg, df_std, df_mad], axis=1)#.drop("label", 1)
    concat_data = pd.concat([data, labels], axis=1)
    print(concat_data)

    data.to_csv("final-data.csv", index=False)

if __name__ == "__main__":
    main()