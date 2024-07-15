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

Input: csv file with 6-class scores produced by llnl_canes_ml_core.exe
Output: csv file with 2-class scores + Investigate/Release label
"""

import sys, string, os, math

def score( filename ):
    infile = open(filename,"r")
    outfile = open(filename+".data","w")
    c = 0
    for line in infile:
        tokens = line.rstrip().split(',')
        tokens2 = []
        if c > 0:
            isum = 0
            rsum = 0
            if len(tokens) < 4:
                isum = float(tokens[0])
                rsum = float(tokens[1])
            else:
                for i in range(4):
                    isum += math.exp(float(tokens[i]))
                for i in range(4,6):
                    rsum += math.exp(float(tokens[i]))
            tokens2.extend([isum,rsum])
            label = tokens[len(tokens)-1]
            newlabel = ""
            if label == "Contamination" or label == "Fissile" or label == "Industrial" or label == "Medical":
                newlabel = "Investigate\n"
            elif label == "NORM" or label == "NonEmitting":
                newlabel = "Release\n"
            else:
                newlabel = "Unlabeled\n"
            tokens2.extend([newlabel])
        else:
            tokens2 = ["Investigate","Release","Label\n"]
        for i in range(3):   
            outfile.write(str(tokens2[i]))
            if i < 2:
                outfile.write(',')
        c = c + 1

    infile.close()
    outfile.close()

if __name__ == '__main__':
    score( sys.argv[1] )


#
# Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
# 
# OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
# 
# This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
# between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
# See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
#