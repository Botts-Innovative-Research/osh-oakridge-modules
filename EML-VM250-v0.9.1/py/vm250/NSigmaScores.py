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
NSigmaScores: meant to be run inside the modelBuilding directory of a VM250 project.
Uses one or more NSigma threshold settings to determine which records in each fold would have been flagged as investigate
"""
import os
import sys
import numpy

def NSigmaScores( featureTable, foldIds, settings ):
    """
    Use one or more NSigma set points to score all test folds and determine original investigate score.
    foldIds is a list of folds,  i.e. [20, 32,33,34]
    """
    nsigmas = [c for c in featureTable.columns if c.startswith('Features.NSigma')]

    for fold in foldIds:
        maxNSigma = featureTable.loc[ featureTable['SegmentInfo.FoldID'] == fold ][nsigmas].max(axis=1)
        for idx, threshold in enumerate(settings):

            alarmname = os.path.join('fold%d' % fold, 'alarmNSigma%d.csv' % idx)
            alarmscore( maxNSigma, threshold, fold, alarmname )

def alarmscore( maxNSigma, threshold, fold, outfile ):
    """
    Reproduce the NSigma Investigate/Release decision for each row of the table. Write result to outfile
    """
    scores = maxNSigma / threshold
    label = 'Unlabeled'
    if fold in (32,33,34,42,43,44,52,53,54):
        label = 'Investigate'
    with open(outfile,'w') as fout:
        fout.write("Investigate,Release,LABEL\n")
        for score in scores:
            fout.write("%.15f,%.15f,%s\n" % (score,-score,label))



#
# Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
# 
# OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
# 
# This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
# between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
# See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
#