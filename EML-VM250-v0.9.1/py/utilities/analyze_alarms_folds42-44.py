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
import fnmatch
import re
from subprocess import call

fold = sys.argv[1]

if fold == "44":
  label = "nuclear"
elif fold == "43":
  label = "industrial"
else:
  label = "medical"

# Loop through all the different alarms for 44-vs-20
i = 5
dir = "fold"+fold

alarms = []

p = re.compile('ROS|SOR')

for file in os.listdir(dir):
  if fnmatch.fnmatch(file, 'alarm*.csv'):
    m = p.search(file)
    if m:
      name = m.group()
      print (name) 
      alarms.append(name)
      pointfile = fold + "_alarm_point"+str(i)+".csv"
      new = "A" + str(i) + "_roc"
      call(["llnl_canes_ml_core.exe", "option", "insp", "ds", "fold"+fold+"\\"+file, "baseline", "fold20\\"+file, "op_point", pointfile])
      os.system("move A1_roc.csv " + new + ".csv")
      call(["python", "two_class_process_insp.py", new])
      i = i + 1

alarmsstring = ""
for j in range(len(alarms)):
  alarmsstring += " " + alarms[j]

newimage3 = fold + "roc4.png"
os.system("R --vanilla --slave --args " + label + " " + str(i-5) + alarmsstring + " < plotROC4.R")
os.system("move test.png " + newimage3)



#
# Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
# 
# OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
# 
# This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
# between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
# See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
#