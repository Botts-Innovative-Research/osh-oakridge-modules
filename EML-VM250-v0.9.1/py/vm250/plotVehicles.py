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
import matplotlib.pyplot as plt
import pandas

if __name__ != "__main__":
    print("Excute as stand alone excutable")
    exit(-1)

if len(sys.argv) <= 1:
    print("Usage:")
    print("   plotVehicles extractname")
    print()
    exit(-1)


vehicles = pandas.read_csv(sys.argv[1], header=0, index_col=0)

vehicles = vehicles.sort_values(by="length")

# Grab the columns for the features
fc0 = vehicles.columns.get_loc("F0")
fcN = vehicles.columns.get_loc("F399")+1
features = vehicles.iloc[:,fc0:fcN]
aspect= 0.25*features.shape[0]/features.shape[1]
#plt.plot(vehicles["length"])
plt.imshow(features.T, aspect=aspect)
plt.show()
print(features.shape)



#
# Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
# 
# OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
# 
# This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
# between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
# See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
#