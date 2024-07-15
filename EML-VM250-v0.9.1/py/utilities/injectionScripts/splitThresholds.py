#
# Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
# 
# OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
# 
# This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
# between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
# See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
#

import sys
import os
from subprocess import call

with open(sys.argv[1], 'r') as f:
    settings = f.read().splitlines()

header=settings[0]
tokens = header.rstrip().split(',')
tokens2 = []

#Skip first 3 tokens in Settings file
for i in range(3,len(tokens)):
  tokens2.extend(tokens[i])

settings=settings[1:]

c = 1
for line in settings:
  fname = "thresholds"+str(c)+".csv"
  outfile = open(fname,"w")
  
  for i in xrange(len(tokens2)):
    outfile.write(str(tokens2[i]))
    if i < len(tokens2) - 1:
      outfile.write(',')

  outfile.write('\n')

  tokens = line.rstrip().split(',')

#Skip first 3 tokens in Settings file
  for i in range(3,len(tokens)):
    outfile.write(str(tokens[i]))
    if i < len(tokens) - 1:
      outfile.write(',')

  outfile.close()

  name = tokens[0] + tokens[2]

  alarmname = "alarm"+name+".csv"
  call(["perl", "alarmscore.pl", "injectionUtilityOutput.csv", fname])
  os.system("move alarm.csv " + alarmname)

  c = c + 1


#
# Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
# 
# OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
# 
# This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
# between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
# See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
#