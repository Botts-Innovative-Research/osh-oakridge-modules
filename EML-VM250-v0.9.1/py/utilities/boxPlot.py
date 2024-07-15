#
# Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
# 
# OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
# 
# This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
# between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
# See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
#

import pandas
import argparse
import matplotlib.pyplot as plt
import matplotlib.patches as mpatches
import numpy as np

# FIXME add filters

def plotFeatures(frames, labelList = None, title = None, step=40):
    frames = list(frames)
    df1 = frames[0]
    colors = ['b','r','g','k']
    N = df1.columns
    for i in range(0,len(N)-1,step):
        if i+step<len(N)-1:
            e = i+step
        else:
            e = len(N)-1
        F=list()
        for j in range(len(frames)):
            F.append( pandas.DataFrame(frames[j].iloc[:, i:e]))
        #msdf1 = frames[0].mean(axis=0)
        #for j in range(len(frames)):
        #    for x in frames[0].columns:
        #        frames[j][x] = frames[j][x]-0.5*msdf1[x]

        #if len(frames[0].columns)==0:
        #    break

        fig = plt.figure(figsize=(14,8))
        fig.subplots_adjust(bottom=0.4)
        for j in range(len(frames)):
            pos = np.array(range(len(F[j].columns)))
            fliers = dict(markerfacecolor=colors[j], marker='.')
            props = F[j].boxplot(positions=i+pos+j*0.2,
                 notch=True, flierprops=fliers, return_type='dict')
            plt.setp(props['boxes'], color=colors[j])
        plt.xticks(rotation=90)
        plt.yscale("symlog")
        if title:
            plt.title(title)
        if labelList:
            plt.legend(handles=[mpatches.Patch(color, label=label) for color, label in zip(colors, labelList)])
        print("Figure",i,e,len(N))
    print(i)

# filelist is a list of csv files that contains features
def main(fileList, labelList = None, title = None):
    data = []
    print("Load datasets")
    for fd  in fileList:
        print("  Load",fd)
        d0 = pandas.read_csv(fd)
        c = [p for p,v in enumerate(d0.columns) if "Feature" in v]
        data.append(d0.iloc[:,c])

    print("Plot datasets")
    plotFeatures(data, labelList, title)
    plt.show()

if __name__=="__main__":
    parser = argparse.ArgumentParser(description='Compare feature tables')
    parser.add_argument('files', metavar='N', type=str, nargs='+',
                                help='a file to process')
    parser.add_argument('--labels', type=str, nargs='+',
                                help='labels for each file')
    parser.add_argument('-t', '--title', help="Plot title")

    args=parser.parse_args()

    if args.labels and len(args.labels) != len(args.files):
        raise RuntimeError("If labels are supplied, number of labels must match number of files")

    main(args.files, args.labels, args.title)

#
# Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
# 
# OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
# 
# This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
# between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
# See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
#