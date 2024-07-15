#
# Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
# 
# OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
# 
# This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
# between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
# See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
#

#!/bin/python

import sys
import argparse
import pandas
import os
from scipy import stats
import numpy
import ntpath

import matplotlib as mpl
mpl.use('Agg')
import matplotlib.pyplot as plt

def extractFilename(filepath):
    head, tail = ntpath.split(filepath)
    return tail or ntpath.basename(head)

def get_args():
    """This function parses and return arguments passed in"""
    # Assign description to the help doc
    parser = argparse.ArgumentParser(
            description='Perform Feature Table Regression Analysis')

    # Add arguments
    parser.add_argument('-d', '--directory', type=str, metavar='dir', help='Save Directory', required=False, default="fig")
    parser.add_argument('-p', '--pdf', help='Compile PDF', type=str, metavar='output.pdf')
    parser.add_argument('-o', '--output_file', help='Write text output', metavar='output.txt', default="regression_test_log.txt")
    parser.add_argument('-f','--fold',metavar='1_10',type=str,help='fold to process', default='1_10')
    parser.add_argument('-l1', '--label1', help="Label for first dataset")
    parser.add_argument('-l2', '--label2', help="Label for second dataset")

    # Array for all arguments passed to script
    parser.add_argument('tables', metavar='tableX', type=str, nargs='+',
                       help='Feature table to use in comparison')


    #args = parser.parse_args(["-p","foo.pdf","../../ernie_saq/PureJava","../../ernie_saq/PureJava_noAnisotropy","-f","34"])

    return parser.parse_args()

def loadFold(table_dirs, fold):
        tables={}

        if (fold == "1_10"): folds = list(range(1,11))
        else: folds = [int(fold)]

        for tbl in table_dirs:
            df = pandas.read_pickle(tbl)
            df2 = df.loc[ df['SegmentInfo.FoldID'].isin(folds) ]
            tables[tbl] = df2
        return tables

def compareTwo(dataframes,savedir="fig", label1=None, label2=None):
    if (not os.path.exists(savedir)): os.makedirs(savedir)

    # check for mismatch in columns
    c1, c2 = [df.columns.tolist() for df in list(dataframes.values())]
    if c1 == c2: columns = c1
    else:
        # find subset where they agree
        columns = [val for val in c1 if val in c2]
        c1only = [val for val in c1 if val not in c2]
        c2only = [val for val in c2 if val not in c1]
        print( """Encountered mismatch between available features:
    %i features in common
    Only in %s: %s
    Only in %s: %s""" % (len(columns), list(dataframes.keys())[0], c1only, list(dataframes.keys())[1], c2only) )

    results = []
    fig=plt.figure()
    if len(dataframes) < 2: return
    if label1 is None:
        label1 = list(dataframes.keys())[0]
    if label2 is None:
        label2 = list(dataframes.keys())[1]
    print("Comparing columns:")
    for col in columns:
        if "SNum" in col:
            print("   %s skipped (can't compare strings)" % col)
            continue

        # only look at the first two datasets
        data1 = list(dataframes.values())[0][col]
        data2 = list(dataframes.values())[1][col]

        # may have datatype mismatches when comparing to old feature tables
        if (data1.dtype != data2.dtype):
            # several possibilities:
            if data1.dtype == bool and data2.dtype == int:
                data1 = data1.astype(int)
            elif data1.dtype == int and data2.dtype == bool:
                data2 = data2.astype(int)
            else:
                print("   %s skipped due to mismatched data types: %s vs %s" %
                        (col, data1.dtype, data2.dtype))
                continue

        print("  ", col)
        ax=fig.add_subplot(1,1,1)

        colname=col
        d,p=stats.ks_2samp(data1, data2)
        ml=min(len(data1),len(data2))
        #S=0.1
        with numpy.errstate(invalid='ignore'):
            S = 0.5 * ( stats.entropy(data1[0:ml], data2[0:ml]) + stats.entropy(data2[0:ml], data1[0:ml]) )
        if (("chi" in col.lower()) or ("distcounts" in col.lower())) and (data1.min() > 0 and data2.min() > 0):
            # FIXME add warning message when 0 encoutered in chisq or distcounts?
            data1=numpy.log10(data1)
            data2=numpy.log10(data2)
            colname="Log( %s )"%col

        ax.hist([data1,data2], 100, histtype='step',color=['r','b'],label=[label1,label2],lw=3)
        ax.set_title(col)
        ax.set_xlabel(colname)
        plt.yscale('log', nonpositive='clip')

        leg=ax.legend(loc='upper right',title='k-s p-value = %.2e'%(d),ncol=2,fancybox=True)
        #leg=ax.legend(loc='upper right',title='k-s = %.2e : S = %2e'%(d,S),ncol=2,fancybox=True)

        leg.get_frame().set_facecolor('wheat')
        leg.get_frame().set_alpha(0.5)
        figname='%s/%s.pdf'%(savedir,col);
        fig.savefig(figname)
        fig.clf()

        results.append((d,figname,col))

    return results

def compilePDF(listorder,results,outputfile="regression_analysis.pdf",savefir="./"):
    from PyPDF2 import PdfFileReader, PdfFileMerger

    merger = PdfFileMerger()
    sl=sorted(results,key=lambda tup:tup[0]) # reverse=True
    page=0
    toc={}
    for d,fname,col in sl[::-1]:
        merger.append(PdfFileReader(fname,'rb'))
        toc[col]=(page,col,fname)
        page+=1
    for l in listorder:
        if l not in toc: continue
        listing = toc[l]
        page=listing[0]
        name=listing[1]
        fname=listing[2]
        merger.addBookmark(name,page,parent=None)
    merger.setPageMode('/UseOutlines')
    merger.write(outputfile)

def printSummary(listorder,results,outputfile="regression_summary.txt"):
    streams=[sys.stdout]
    if (outputfile != None): streams.append( open(outputfile,'w') )
    sl=sorted(results,key=lambda tup:tup[0]) # reverse=True
    for d,fname,col in sl[::-1]:
        for s in streams: s.write("%.3e   %s\n"%(d, col))


if __name__ == '__main__':
    args = get_args()
    #print args
    dataframes=loadFold(table_dirs=args.tables, fold=args.fold)
    results = compareTwo(dataframes,savedir=args.directory,label1=args.label1,label2=args.label2)

    printSummary(list(dataframes.values())[0].columns,
                 results,
                 args.output_file)

    if (args.pdf != None): 
        compilePDF(list(dataframes.values())[0].columns,
                   results,
                   args.pdf)



#
# Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
# 
# OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
# 
# This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
# between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
# See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
#