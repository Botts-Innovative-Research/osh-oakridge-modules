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
RPMscores: meant to be run inside the modelBuilding directory of a SAIC RPM-8 project.
Uses a ROS threshold settings file to determine which records in each fold would have been flagged by RPM system.
"""
import os
import sys
import numpy
import pandas

def RPMscores( featureTable, settingsFile, foldIds, scoreAllLanes = False ):
    """
    Read ROS settings file and score all test folds to determine original investigate score.
    foldIds is a list of folds,  i.e. [20, 32,33,34]
    """
    with open(settingsFile, 'r', encoding='utf-8') as f:
        settings = f.read().splitlines()

    header = settings[0]
    features = [c for c in header.split(',') if c.startswith('Feature')]
    settings = settings[1:] # skip the header

    thresholds = {}
    for line in settings:

        tokens = line.rstrip().split(',')

        name = tokens[0] + tokens[2]    #  'ROS3' for example
        siteId = int(tokens[1])
        laneId = int(tokens[2])

        if 'SOR' in name:
            print("  WARNING: skipping SOR thresholds for site %d lane %d" % (siteId, laneId))
            continue

        thresholdVals = numpy.array( list(map(float, tokens[3:])) )
        thresholds[ (siteId, laneId) ] = (name, thresholdVals)

    for fold in foldIds:
        df = featureTable.loc[ featureTable['SegmentInfo.FoldID'] == fold ]
        alarmscore( df, fold, thresholds, features, scoreAllLanes )


def alarmscore( df, fold, threshold_vals, threshold_columns, scoreAllLanes ):
    """
    From a feature table + dictionary of lane-specific ROS thresholds,
    reproduce the RPM Investigate/Release decision for each lane. Write result to outfile
    
    df is a full feature table (including metadata about site and lane)
    fold is the integer fold number (determines whether label is Investigate, Release or Unlabled)
    threshold_vals is a dict containing lane-specific thresholds for all Features.Standard.* columns
    threshold_columns is the list of column headers (ensure consistency between ROS_settings and feature table)
    If scoreAllLanes, use every row in feature tables to generate ROS points rather than selecting only for that lane.
    """

    print ("Determining ROS investigate scores for fold %d" % fold)
    if fold in (32, 33, 34, 42, 43, 44, 52, 53, 54):
        label = "Investigate"
    elif fold in (50,):
        label = "Release"
    elif fold in (20,):
        label = "Unlabelad"

    for (site,lane) in threshold_vals:
        if scoreAllLanes:
            subset = df
        else:
            subset = df.loc[ (df['SegmentInfo.Location.DataSourceId'] == site) & (df['SegmentInfo.Location.RpmId']==lane) ]

        if len(subset) < 100:
            print("Skipping site %d, lane %d (only %d scans found in feature table)" % (site, lane, len(subset)))
            continue

        standards = subset[threshold_columns]
        name, thresholds = threshold_vals[(site, lane)]

        maxscore = (standards / thresholds).max(axis=1)
        result = pandas.DataFrame()
        result['Investigate'] = maxscore
        result['Release'] = -maxscore
        result['LABEL'] = label

        alarmname = os.path.join('fold%d' % fold, 'alarm%s.csv' % name)

        result.to_csv(alarmname, header=True, index=False)


if __name__ == '__main__':
    RPMscores( sys.argv[1], foldIds = [20, 32,33,34] )



#
# Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
# 
# OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
# 
# This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
# between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
# See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
#