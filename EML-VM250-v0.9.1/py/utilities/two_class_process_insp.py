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

import sys

def two_class_process_insp( prefix ):
    minval = 0.000001

    infile = open(prefix + ".csv", "r")
    outfile = open(prefix + "_std.csv", "w")

    c = 0
    for line in infile:
        tokens = line.split(',')
        if len(tokens) > 1:
            if c > 0:
                tokens = [float(a) for a in tokens]
                tokens2 = []
                for i in range(1):
                    FP = tokens[4 * i]
                    TP = tokens[4 * i + 1]
                    FPstd = tokens[4 * i + 2]
                    TPstd = tokens[4 * i + 3]
                    tokens2.extend([FP, TP, FP - FPstd, TP + TPstd, FP + FPstd, TP - TPstd])
                tokens2.extend([tokens[4], tokens[5]])
                tokens2 = [min(a, 1) for a in tokens2]
                tokens2 = [max(a, minval) for a in tokens2]
            else:
                tokens2 = ["FP1", "TP1", "FP1UB", "TP1UB", "FP1LB", "TP1LB", "RANDOM_FP", "RANDOM_TP"]
            # if c == 2:
            #  print(tokens2)
            for i in range(len(tokens2)):
                outfile.write(str(tokens2[i]))
                if i < len(tokens2) - 1:
                    outfile.write(',')
                else:
                    outfile.write('\n')
            c = c + 1

    infile.close()
    outfile.close()


if __name__ == '__main__':
    two_class_process_insp(sys.argv[1])


#
# Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
# 
# OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
# 
# This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
# between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
# See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
#