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

import sys, string, os, math

infile = open(sys.argv[1],"r")
outfile = open(sys.argv[1]+".data","w")
newlabel = sys.argv[2]

c = 0
for line in infile:
 tokens = line.rstrip().split(',')
 tokens2 = []
 if c > 0:
   isum = float(tokens[236])
   rsum = -isum

   tokens2.extend([isum,rsum])
   tokens2.extend([newlabel])
 else:
   tokens2 = ["Investigate","Release","Label\n"]
 for i in range(3):   
   outfile.write(str(tokens2[i]))
   if i < 2:
     outfile.write(',')
 outfile.write('\n')
 c = c + 1

infile.close()
outfile.close()


#
# Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
# 
# OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
# 
# This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
# between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
# See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
#