#
# Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
# 
# OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
# 
# This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
# between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
# See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
#

"""
Draw ROC plots for a single fold. Should be run inside project/modelBuilding directory
"""
import os
import sys
import shutil
import fnmatch
import re
from subprocess import call

from utilities.two_class_process_insp import two_class_process_insp
from utilities import ROCplots

def analyze_alarms(fold, info, secondSource = True, mandatoryInspectionRate = None, output_dir = None, plotROC1 = False, drawCurves = False):
    """
    fold = 32-34 or 42-44
    info is a dictionary, contains full path to llnl_canes_ml_core executable
    mandatoryInspectionRate (if supplied) controls location of the vertical red line
    output_dir: where to write plots
    plotROC1: if true, also create plot with only the final ERNIE curve, mandatory inspection and chance curve
    drawCurves: if true, draw curves as well as points for original hardware alarms
    """
    if fold not in (32,33,34,42,43,44):
        raise KeyError("Unknown fold: " + str(fold))

    # Loop through all the different alarms for {fold}-vs-20
    i = 4
    dir = "fold%d" % fold

    alarms = []

    p = re.compile('ROS|NSigma')

    for file in os.listdir(dir):
        if fnmatch.fnmatch(file, 'alarm*.csv'):
            m = p.search(file)
            if m:
                f1 = os.path.join( dir, file )
                f2 = os.path.join( 'fold20', file )
                name = m.group()
                print ("Analyze alarms: Fold: {}, Type: {}".format(fold, name))
                alarms.append(name)
                pointfile = str(fold) + "_alarm_point"+str(i)+".csv"
                new = "A" + str(i) + "_roc"
                call([info['cmuExe'], "option", "insp", "ds", f1, "baseline", f2, "op_point", pointfile])
                shutil.move("A1_roc.csv", new + ".csv")
                two_class_process_insp( new )
                i = i + 1

    if plotROC1:
        filename1 = "{}roc1.png".format(fold)
        if output_dir:
            filename1 = os.path.join(output_dir, filename1)
        ROCplots.plotROC1(fold, *alarms, outputFile = filename1, mandatoryInspectionRate = mandatoryInspectionRate, drawCurves=drawCurves)

    filename2 = "{}roc2.png".format(fold)
    if output_dir:
        filename2 = os.path.join(output_dir, filename2)
    ROCplots.plotROC2(fold, *alarms, outputFile = filename2, mandatoryInspectionRate = mandatoryInspectionRate,
            secondSource = secondSource, drawCurves=drawCurves)

if __name__ == '__main__':
    fold = sys.argv[1]
    analyze_alarms( int(fold) )



#
# Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
# 
# OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
# 
# This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
# between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
# See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
#