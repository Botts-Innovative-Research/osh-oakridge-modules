#
# Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
# 
# OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
# 
# This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
# between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
# See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
#

import argparse
import numpy
import pandas
from matplotlib import pyplot as plt

parser = argparse.ArgumentParser("""Plot max ratio vs. intensity for a sample of records,
along with non-emitting criteria""")
parser.add_argument("sampleFile", help="Summary file written by FeatureBuilderSetup")
parser.add_argument("nonEmittingCoefs", help="Non-emitting coefficients file written by FeatureBuilderSetup")
parser.add_argument("-o","--output", default="nonEmittingSelection.png", help="file name for saving plot")
args = parser.parse_args()

df = pandas.read_csv( args.sampleFile )

fig, ax = plt.subplots(figsize=(10,5))
fig.subplots_adjust(left=0.08, right=0.96, )

plt.loglog( df['maxRatio'], df['intensity'], '.', color='k', markersize=1, label='no alarm' )
alarms = df.loc[ df['alarm']==1 ]
plt.loglog( alarms['maxRatio'], alarms['intensity'], '.', color='r', markersize=2, label='alarm' )

with open( args.nonEmittingCoefs ) as fin:
    C1 = float( fin.readline().split(':')[-1] )
    C2 = float( fin.readline().split(':')[-1] )

# vertical line for C1
plt.axvline(C1, color='b', linewidth=1)

# diagonal line for C2
x1,x2 = ax.get_xlim()
x1 = min(x1, 0.1)   # puts non-emitting cluster in the center, at least for Charleston data
y1,y2 = ax.get_ylim()
xvals = numpy.logspace(numpy.log(x1/10.),numpy.log(x2*10),300)   # go beyond current x limits just in case...
yvals = C2 * xvals**-1.2
plt.loglog( xvals, yvals, color='b', linewidth=1 )

# don't let C2 line change the x/y limits:
ax.set_xlim(x1,x2)
ax.set_ylim(y1,y2)

plt.xlabel("RatioMetric")
plt.ylabel("PeakIntensity")
plt.title("Max ratio vs. intensity")
lgnd = plt.legend()
for handle in lgnd.legendHandles: handle._legmarker.set_markersize(8)

plt.savefig( args.output )



#
# Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
# 
# OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
# 
# This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
# between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
# See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
#