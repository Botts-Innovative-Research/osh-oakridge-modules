#
# Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
# 
# OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
# 
# This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
# between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
# See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
#

#!/bin/env python
"""
Convert 6-class predictions into 2-class investigate/release scores

Input: csv file with 6-class scores produced by AnalysisUtility (including 2nd-source logic)
Output: csv file with 2-class scores + label (may be Investigate/Release/Unlabeled depending on fold)
"""

import sys, string, os, math

def jointscore( filename, newlabel ):
    infile = open(filename, "r")
    outfile = open(filename + ".data", "w")

    headers = infile.readline().rstrip().split(',')
    investigates = ('MED','IND','FIS','Contam')
    releases = ('NonEmitting','NORM')

    investigate1 = [ headers.index('Source1.' + sourcetype) for sourcetype in investigates ]
    release1 = [ headers.index('Source1.' + sourcetype) for sourcetype in releases ]

    investigate2 = [ headers.index('Source2.' + sourcetype) for sourcetype in investigates ]
    release2 = [ headers.index('Source2.' + sourcetype) for sourcetype in releases ]

    outfile.write("Investigate,Release,Label\n")
    for line in infile:
        tokens = line.rstrip().split(',')

        isum = 0
        rsum = 0
        for i in investigate1:
            isum += float(tokens[i])
        for i in release1:
            rsum += float(tokens[i])

        # Secondary source logic
        isum2 = 0
        rsum2 = 0
        for i in investigate2:
            isum2 += float(tokens[i])
        for i in release2:
            rsum2 += float(tokens[i])

        if isum2 > isum:
            isum = isum2
            rsum = rsum2

        outfile.write('%g,%g,%s\n' % (isum,rsum,newlabel))

    infile.close()
    outfile.close()

if __name__ == '__main__':
    jointscore( sys.argv[1], sys.argv[2] )



#
# Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
# 
# OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
# 
# This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
# between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
# See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
#