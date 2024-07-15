#
# Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
# 
# OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
# 
# This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
# between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
# See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
#

""" Plotting various features to visually inspect the features' characteristics"""
from __future__ import division
import os
import sys
#import matplotlib.style
#matplotlib.style.use('classic')
import matplotlib
matplotlib.use('Agg')   # non-interactive backend
import matplotlib.pyplot as plt
plt.rcParams.update({'figure.max_open_warning': 0})
from pathlib import Path as PyPath

import inspect
parent_dir = os.path.dirname(
    os.path.dirname(os.path.abspath(inspect.getfile(inspect.currentframe())))
)
sys.path.insert(0,parent_dir)

import loadSQL


def parse_args():
    from argparse import ArgumentParser
    parser = ArgumentParser( description = """
    Plotting utility for inspecting feature tables.

    If any of the options --fold, --rpmId or --dataSourceId are supplied, only records with the given
    fold / RPMid / DataSourceId will be plotted. Any combination of these arguments can be supplied.
    """,
    usage="python diagnosticPlot.py [-h] featureTableDBPath [-f FOLDID] [-r RPMID] [-d DATASOURCEID] [--dpi DOTSPERINCH]"
    )

    parser.add_argument( 'featureTableDBPath', type=str, help="The path of the feature table database" )
    parser.add_argument( '-f', '--foldId', type=int, help="Select a single fold for plotting ratio/intensity" )
    parser.add_argument( '-r', '--rpmId', type=int, help="Select one RPMid for plotting ratio/intensity" )
    parser.add_argument( '-d', '--dataSourceId', type=int, help="Select one DataSourceId for plotting ratio/intensity" )
    parser.add_argument( '--dpi', type=int, help="Set plots dpi")

    return parser.parse_args()


colors = {
        'NonEmitting': '#1f77b4',
        'NORM': '#2ca02c',
        'Medical': '#d62728',
        'Industrial': '#17becf',
        'Fissile': '#e377c2',
        'Contamination': '#7f7f7f',
        'Unlabeled': '#8c564b',
        }

colors_4class = {
        'NORM-NORM': '#2ca02c',
        'NORM-THREAT': 'orange',
        'THREAT-NORM': 'purple',
        'THREAT-THREAT': 'red',
        'Unlabeled': '#8c564b',
        }

def additionalPlot1( df, directory, step = 1, foldId=None, RPMId=None, DSId=None, dpi=None ):
    """
    Plots (y vs x):
        Folds 1-10
        - Max N-Sigma vs. Features.Statistics.Peak.Intensity
        - Max N-Sigma vs. Features.Statistics.Peak.Significance
        - Max N-Sigma vs. Features.Extent.PeakIntensity

        Folds 1-10, 1-10+20, folds 101-110
        - Max N-Sigma vs. Features.Statistics.SpreadX - need to be Linear
        - Max N-Sigma vs. Features.Statistics.SpreadI - need to be Linear
        - Max N-Sigma vs. Features.Statistics.SplitDip - need to be Linear

    Generate per plot types above:
     training only (folds 1-10)
     training + testing (folds 1-10 and 32-34)
     training + unlabeled (1-10 and fold 20)


    """

    classes = [c for c in df.columns if c.startswith('Classification.Source')]
    classes.remove( 'Classification.Source.NonEmitting' )
    classes.remove( 'Classification.Source.Mixed' )
    classes = [ 'Classification.Source.NonEmitting' ] + classes   # plot nonemitting first, so others lie on top

    maxNSigma = lambda df: df[ [c for c in df.columns if c.startswith('Features.NSigma')] ].max(axis=1)

    training = df['SegmentInfo.FoldID'].isin(range(1,11))
    testing = df['SegmentInfo.FoldID'].isin(range(32,35))
    fold20 = df['SegmentInfo.FoldID'] == 20

    yRange = (1e-2,1e+4)
    ylabel = "Max N-Sigma"
    intensityFeatureExtractor = maxNSigma

    # first pass: draw training folds and then overlay with unlabeled data
    for xColumn, plotName, title in (
            ("Features.Statistics.Peak.Intensity", "Stats2PeakIntensityVsNSigma.png", ""),
            ("Features.Statistics.Peak.Significance", "Stats2PeakSignificanceVsNSigma.png", ""),
            ("Features.Extent.PeakIntensity", "ExtentPeakIntensityVsNSigma.png", ""),
            ("Features.Statistics.SpreadX", "Stats2SpreadXVsNSigma.png", ""),
            ("Features.Statistics.SpreadI","Stats2SpreadIVsNSigma.png",""),
            ("Features.Statistics.SplitDip","Stats2SplitDipVsNSigma.png","")
            ):
        fig, ax = plt.subplots()
        for class_ in classes:
            thisClass = df.loc[ training & (df[class_] == 1) ]
            color = colors[ class_.replace('Classification.Source.','') ]

            ax.plot( thisClass[xColumn][::step],
                    intensityFeatureExtractor(thisClass)[::step], color = color,
                    marker = '.', markersize=1, linestyle='', alpha=0.5, label=class_.split('.')[-1] )
            del thisClass

        if foldId: title += "Fold %d," % foldId
        elif RPMId: title += "RPMid %d," % RPMId
        elif DSId: title += "DataSourceId %d" % DSId
        else: title += "Folds 1-10"

        ax.set_yscale("log")
        ax.set_xscale('symlog')
        ax.set_title(title)
        ax.set_xlabel(xColumn)
        ax.set_ylabel(ylabel)
        #ax.set_xlim( 1e-1, 1e+2 )
        #ax.set_xlim( 0, 30 )
        ax.set_ylim( *yRange )
        lgnd = ax.legend(numpoints=1)
        for handle in lgnd.legendHandles: handle._legmarker.set_markersize(6)

        plt.savefig( os.path.join( directory, plotName ), dpi=dpi if dpi else fig.dpi )

        # Add unlabeled from fold 20 for additional plot:
        unlabeled = df.loc[ fold20 ]
        ax.plot( unlabeled[xColumn][::step*7],
                intensityFeatureExtractor(unlabeled)[::step*7], color=colors['Unlabeled'],
                marker = '.', markersize=1, linestyle='', alpha=0.5, label='Unlabeled' )

        lgnd.remove()
        lgnd = ax.legend(numpoints=1)
        for handle in lgnd.legendHandles: handle._legmarker.set_markersize(6)

        title += ", 20"
        ax.set_yscale("log")
        ax.set_xscale('symlog')
        ax.set_title(title)

        plt.savefig( os.path.join( directory, plotName.replace( '.png', '_addUnlabeled.png') ), dpi=dpi if dpi else fig.dpi )

        plt.close( fig )


    # second pass: training folds overlaid with test folds
    for xColumn, plotName, title in (
            ("Features.Statistics.Peak.Intensity", "Stats2PeakIntensityVsNSigma.png", ""),
            ("Features.Statistics.Peak.Significance", "Stats2PeakSignificanceVsNSigma.png", ""),
            ("Features.Extent.PeakIntensity", "ExtentPeakIntensityVsNSigma.png", ""),
            ("Features.Statistics.SpreadX", "Stats2SpreadXVsNSigma.png", ""),
            ("Features.Statistics.SpreadI","Stats2SpreadIVsNSigma.png",""),
            ("Features.Statistics.SplitDip","Stats2SplitDipVsNSigma.png","")
            ):
        fig, ax = plt.subplots()
        for class_ in classes:
            thisClass = df.loc[ training & (df[class_] == 1) ]
            color = colors[ class_.replace('Classification.Source.','') ]

            ax.plot( thisClass[xColumn][::step],
                    intensityFeatureExtractor(thisClass)[::step], color = color,
                    marker = '.', markersize=1, linestyle='', alpha=0.5, label=class_.split('.')[-1] )
            del thisClass

        if foldId: title += "Fold %d, 32-34" % foldId
        elif RPMId: title += "RPMid %d, Folds 32-34" % RPMId
        elif DSId: title += "DataSourceId %d, Folds 32-34" % DSId
        else: title += "Folds 1-10, 32-34"

        ax.set_title(title)
        ax.set_yscale("log")
        ax.set_xscale('symlog')
        ax.set_xlabel(xColumn)
        ax.set_ylabel(ylabel)
        #ax.set_xlim( 1e-1, 1e+2 )
        #ax.set_xlim( 0, 30 )
        ax.set_ylim( *yRange )
        lgnd = ax.legend(numpoints=1)
        for handle in lgnd.legendHandles: handle._legmarker.set_markersize(6)

        # next add test folds:
        for class_ in ('Medical', 'Industrial', 'Fissile'):
            thisClass = df.loc[ testing & (df['Classification.Source.' + class_] == 1) ]
            if thisClass.empty: continue

            ax.plot( thisClass[xColumn][::step*10],
                intensityFeatureExtractor(thisClass)[::step*10], color=colors[class_],
                marker = 'o', markersize=1, linestyle='', label=class_+" test" )
            del thisClass

        plt.savefig( os.path.join( directory, plotName.replace('.png','_addTesting.png') ), dpi=dpi if dpi else fig.dpi )

        plt.close( fig )

def additionalPlot2( df, directory, step = 1, foldId=None, RPMId=None, DSId=None, dpi=None ):
    """
    Plots (y vs x):
        Folds 1-10, 1-10+20, folds 101-110
        - Features.Statistics.Peak.XMean vs. Features.Statistics.Split.Position - linear/linear, divide both by vehicle length

        Folds 1-10, 1-10+20
        - Features.Statistics.Peak.XMean vs. Features.Extent.XLocation - Should be linear/linear.  Divide both by vehicle length

        Folds 1-10, 1-10+20, folds 101-110
        - Features.Statistics.Spread3D vs. Features.Statistics.SpreadX - Should be linear/linear

    Generate per plot types above:
     training only (folds 1-10)
     training + testing (folds 1-10 and 32-34)
     training + unlabeled (1-10 and fold 20)
    """

    classes = [c for c in df.columns if c.startswith('Classification.Source')]
    classes.remove( 'Classification.Source.NonEmitting' )
    classes.remove( 'Classification.Source.Mixed' )
    classes = [ 'Classification.Source.NonEmitting' ] + classes   # plot nonemitting first, so others lie on top

    training = df['SegmentInfo.FoldID'].isin(range(1,11))
    testing = df['SegmentInfo.FoldID'].isin(range(32,35))
    fold20 = df['SegmentInfo.FoldID'] == 20

    # first pass: draw training folds
    for xColumn, yColumn, plotName, title in (
            ("Features.Statistics.Split.Position",
             "Features.Statistics.Peak.XMean",
             "Stats2SplitPosVsStats2PeakXMean.png",
             ""
             ),
            ("Features.Extent.XLocation",
             "Features.Statistics.Peak.XMean",
             "ExtentXLocVsStats2PeakXMean.png",
             ""
             ),
            ("Features.Statistics.SpreadX",
             "Features.Statistics.Spread3D",
             "Stats2SpreadXVsStats2Spread3D.png",
             "")
            ):
        fig, ax = plt.subplots()
        for class_ in classes:
            thisClass = df.loc[ training & (df[class_] == 1) ]
            color = colors[ class_.replace('Classification.Source.','') ]

            if "XLocation" in xColumn:
                ax.plot( thisClass[xColumn][::step] / thisClass['Features.Vehicle.Length'][::step],
                    thisClass[yColumn][::step], color = color,
                    marker = '.', markersize=1, linestyle='', alpha=0.5, label=class_.split('.')[-1] )
            else:
                ax.plot( thisClass[xColumn][::step],
                        thisClass[yColumn][::step], color = color,
                        marker = '.', markersize=1, linestyle='', alpha=0.5, label=class_.split('.')[-1] )
            del thisClass

        if foldId: title += "Fold %d," % foldId
        elif RPMId: title += "RPMid %d," % RPMId
        elif DSId: title += "DataSourceId %d" % DSId
        else: title += "Folds 1-10"

        ax.set_title(title)

        if "XLocation" in xColumn:
            ax.set_xlabel("{} / {}".format(xColumn,"Features.Vehicle.Length"))
            ax.set_yscale("symlog", linthreshy=10)
        else:
            ax.set_xlabel(xColumn)
            ax.set_yscale("symlog", linthreshy=10)
            ax.set_xscale('symlog', linthreshx=10)
        ax.set_ylabel(yColumn)
        lgnd = ax.legend(numpoints=1)
        for handle in lgnd.legendHandles: handle._legmarker.set_markersize(6)

        plt.savefig( os.path.join( directory, plotName ), dpi=dpi if dpi else fig.dpi )
        plt.close( fig )


    # draw training folds and then overlay with unlabeled data
    for xColumn, yColumn, plotName, title in (
            ("Features.Statistics.Split.Position",
             "Features.Statistics.Peak.XMean",
             "Stats2SplitPosVsStats2PeakXMean.png",
             ""
             ),
            ("Features.Extent.XLocation",
             "Features.Statistics.Peak.XMean",
             "ExtentXLocVsStats2PeakXMean.png",
             ""
             ),
            ("Features.Statistics.SpreadX",
             "Features.Statistics.Spread3D",
             "Stats2SpreadXVsStats2Spread3D.png",
             "")
            ):
        fig, ax = plt.subplots()

        # Add unlabeled from fold 20 for additional plot:
        unlabeled = df.loc[ fold20 ]
        if "XLocation" in xColumn:
            ax.plot( unlabeled[xColumn][::step] / unlabeled['Features.Vehicle.Length'][::step] ,
                    unlabeled[yColumn][::step], color=colors['Unlabeled'],
                    marker = '.', markersize=1, linestyle='', alpha=0.8, label='Unlabeled' )
        else:
            ax.plot( unlabeled[xColumn][::step] ,
                    unlabeled[yColumn][::step], color=colors['Unlabeled'],
                    marker = '.', markersize=1, linestyle='', alpha=0.8, label='Unlabeled' )

        for class_ in classes:
            thisClass = df.loc[ training & (df[class_] == 1) ]
            color = colors[ class_.replace('Classification.Source.','') ]
            if "XLocation" in xColumn:
                ax.plot( thisClass[xColumn][::step] / thisClass['Features.Vehicle.Length'][::step],
                    thisClass[yColumn][::step], color = color,
                    marker = '.', markersize=1, linestyle='', alpha=0.5, label=class_.split('.')[-1] )
            else:
                ax.plot( thisClass[xColumn][::step],
                        thisClass[yColumn][::step], color = color,
                        marker = '.', markersize=1, linestyle='', alpha=0.5, label=class_.split('.')[-1] )
            del thisClass

        if foldId: title += "Fold %d," % foldId
        elif RPMId: title += "RPMid %d," % RPMId
        elif DSId: title += "DataSourceId %d" % DSId
        else: title += "Folds 1-10"

        ax.set_title(title)
        if "XLocation" in xColumn:
            ax.set_xlabel("{} / {}".format(xColumn,"Features.Vehicle.Length"))
            ax.set_yscale("symlog", linthreshy=10)
        else:
            ax.set_xlabel(xColumn)
            ax.set_yscale("symlog", linthreshy=10)
            ax.set_xscale('symlog', linthreshx=10)
        ax.set_ylabel(yColumn)
        lgnd = ax.legend(numpoints=1)
        for handle in lgnd.legendHandles: handle._legmarker.set_markersize(6)

        title += ", 20"
        ax.set_title(title)

        plt.savefig( os.path.join( directory, plotName.replace( '.png', '_addUnlabeled.png') ), dpi=dpi if dpi else fig.dpi )
        plt.close( fig )


    # second pass: training folds overlaid with test folds
    for xColumn, yColumn, plotName, title in (
            ("Features.Statistics.Split.Position",
             "Features.Statistics.Peak.XMean",
             "Stats2SplitPosVsStats2PeakXMean.png",
             ""
             ),
            ("Features.Extent.XLocation",
             "Features.Statistics.Peak.XMean",
             "ExtentXLocVsStats2PeakXMean.png",
             ""
             ),
            ("Features.Statistics.SpreadX",
             "Features.Statistics.Spread3D",
             "Stats2SpreadXVsStats2Spread3D.png",
             "")
            ):
        fig, ax = plt.subplots()
        for class_ in classes:
            thisClass = df.loc[ training & (df[class_] == 1) ]
            color = colors[ class_.replace('Classification.Source.','') ]
            if "XLocation" in xColumn:
                ax.plot( thisClass[xColumn][::step] / thisClass['Features.Vehicle.Length'][::step],
                    thisClass[yColumn][::step], color = color,
                    marker = '.', markersize=1, linestyle='', alpha=0.5, label=class_.split('.')[-1] )
            else:
                ax.plot( thisClass[xColumn][::step],
                        thisClass[yColumn][::step], color = color,
                        marker = '.', markersize=1, linestyle='', alpha=0.5, label=class_.split('.')[-1] )
            del thisClass

        if foldId: title += "Fold %d, 32-34" % foldId
        elif RPMId: title += "RPMid %d, Folds 32-34" % RPMId
        elif DSId: title += "DataSourceId %d, Folds 32-34" % DSId
        else: title += "Folds 1-10, 32-34"

        ax.set_title(title)
        if "XLocation" in xColumn:
            ax.set_xlabel("{} / {}".format(xColumn,"Features.Vehicle.Length"))
            ax.set_yscale("symlog", linthreshy=10)
        else:
            ax.set_xlabel(xColumn)
            ax.set_yscale("symlog", linthreshy=10)
            ax.set_xscale('symlog', linthreshx=10)
        ax.set_ylabel(yColumn)
        lgnd = ax.legend(numpoints=1)
        for handle in lgnd.legendHandles: handle._legmarker.set_markersize(6)

        # next add test folds:
        for class_ in ('Medical', 'Industrial', 'Fissile'):
            thisClass = df.loc[ testing & (df['Classification.Source.' + class_] == 1) ]
            if thisClass.empty: continue
            if "XLocation" in xColumn:
                ax.plot( thisClass[xColumn][::step*5] / thisClass['Features.Vehicle.Length'][::step*5],
                        thisClass[yColumn][::step*5], color=colors[class_],
                        marker = 'o', markersize=1, linestyle='', label=class_+" test" )
            else:
                ax.plot( thisClass[xColumn][::step*5],
                        thisClass[yColumn][::step*5], color=colors[class_],
                        marker = 'o', markersize=1, linestyle='', label=class_+" test" )
            del thisClass

        plt.savefig( os.path.join( directory, plotName.replace('.png','_addTesting.png') ), dpi=dpi if dpi else fig.dpi )
        plt.close( fig )


def diagnosticPlot(
        featureTableDBPath = None,
        foldId = None,
        rpmId = None,
        dataSourceId = None,
        dpi = None
    ):

    proj_root = PyPath(PyPath(featureTableDBPath).parent).parent

    print("Loading db...")

    df, badRecords = loadSQL.loadFolds( featureTableDBPath, foldId )

    if rpmId:
        df = df.loc[ df['SegmentInfo.Location.RpmId'] == rpmId ]
    if dataSourceId:
        df = df.loc[ df['SegmentInfo.Location.DataSourceId'] == dataSourceId ]

    plotDir = os.path.join(proj_root, "plots", "diagnosticPlots")
    if not os.path.exists(plotDir):
        os.mkdir(plotDir)

    print("Generating diagnostic plots...")
    additionalPlot1(df, plotDir, 1, dpi=dpi)
    additionalPlot2(df, plotDir, 1, dpi=dpi)

    print("Finish creating diagnostic plots")

if __name__ == '__main__':
    args = parse_args()
    diagnosticPlot(
        args.featureTableDBPath,
        args.foldId,
        args.rpmId,
        args.dataSourceId,
        args.dpi
    )






#
# Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
# 
# OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
# 
# This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
# between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
# See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
#