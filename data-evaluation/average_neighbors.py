import pandas as pd
import numpy as np
#!!!!!!!!!!!!
# Add this line to neighbors.csv at the top 'gx,gy,gz,ax,ay,az,label'

df = pd.read_csv('neighbors.csv')

df1 = pd.DataFrame(df.groupby(np.arange(len(df)) // 10).mean())

set = [1,2,3,4,5,6]
df1 = df1[df1['label'].isin(set)]
print(df1)

df1.to_csv('new_neighbors.csv', index=False)
