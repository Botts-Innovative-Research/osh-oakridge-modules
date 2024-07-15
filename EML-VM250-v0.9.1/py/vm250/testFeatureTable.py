#
# Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
# 
# OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
# 
# This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
# between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
# See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
#

""" After constructing a feature table, do some basic testing + draw some plots to look for anything unusual """
from __future__ import division
import os
import sys
import numpy
import pandas
#import matplotlib.style
#matplotlib.style.use('classic')
import matplotlib
matplotlib.use('Agg')   # non-interactive backend
import matplotlib.pyplot as plt
import matplotlib.patches as mpatches
plt.rcParams.update({'figure.max_open_warning': 0})
from pathlib import Path as PyPath
import inspect
parent_dir = os.path.dirname(
    os.path.dirname(os.path.abspath(inspect.getfile(inspect.currentframe())))
)
sys.path.append(parent_dir)
import loadSQL
from common.sourceColors import colors, colors_4class


def parse_args():
    from argparse import ArgumentParser
    parser = ArgumentParser( description = """
    Testing / plotting utility for inspecting feature tables.
    By default, loads features for all folds and checks for any possible errors.

    If any of the options --fold, --rpmId or --dataSourceId are supplied, only records with the given
    fold / RPMid / DataSourceId will be plotted. Any combination of these arguments can be supplied.
    """,
    usage="python testFeatureTable.py [-h] featureTableDBPath [-f FOLDID] [-r RPMID] [-d DATASOURCEID] [--dpi DOTSPERINCH]"
    )

    parser.add_argument( 'featureTableDBPath', type=str, help="The path of the feature table database" )
    parser.add_argument( '-f', '--foldId', type=int, help="Select a single fold for plotting ratio/intensity" )
    parser.add_argument( '-r', '--rpmId', type=int, help="Select one RPMid for plotting ratio/intensity" )
    parser.add_argument( '-d', '--dataSourceId', type=int, help="Select one DataSourceId for plotting ratio/intensity" )
    parser.add_argument( '-v', '--verbose', action="store_true", help="Make verbose" )
    parser.add_argument( '--dpi', type=int, help="Set plots dpi")

    return parser.parse_args()


def runTests( df ):

    errors = False

    actions = [c for c in df.columns if c.startswith('Classification.Action')]
    manipulations = [c for c in df.columns if c.startswith('Manipulation.')]
    manipulations2 = [c for c in df.columns if c.startswith('Manipulation2.')]
    vehicle = [c for c in df.columns if c.startswith('Features.Vehicle')]

    nanRows = df[df.isnull().any(axis=1)]
    if len(nanRows) > 0:
        print("Training tables contain %d NaN values, in fold(s) %s!" %
                (len(nanRows), ','.join([str(fold) for fold in nanRows['SegmentInfo.FoldID']])) )
        errors = True

    for fold in (11,20):    # folds containing 'stream of commerce' data
        subset = df[ df['SegmentInfo.FoldID'] == fold ]
        if subset.empty:
            print("No records found for fold %i" % fold)
            errors = True
            continue

        # Folds 11 and 20 should have no manipulated records:
        if subset[ manipulations ].sum().sum() != 0:
            print("Unexpected manipulations found in fold %i" % fold)
            errors = True

        # these folds shouldn't have many alarms:
        alarmFraction = (subset['Classification.Action.Investigate'].sum() /
                len(subset))
        if alarmFraction > 0.005:
            print("More investigates than expected for fold %i (%.2f%%)" % (fold, alarmFraction*100))
            errors = True


    for fold in (32,33,34,  # test folds for Medical, Industrial and Fissile sources respectively
                 42,43,44): # injections into 'emitting' records
        subset = df[ df['SegmentInfo.FoldID'] == fold ]
        if subset.empty:
            print("No records found for fold %i" % fold)
            errors = True
            continue

        # All records in 32,33,34,42,43,44 should be manipulated:
        if not all( subset[ manipulations ].T.sum() > 0 ):
            print("Expected manipulations not found for %i records in fold %i" % (
                sum( subset[ manipulations ].T.sum() <= 0 ), fold ) )
            errors = True

    for fold in range(1,11):    # training folds. First ensure that each fold in 1-10 is present
        subset = df[ df['SegmentInfo.FoldID'] == fold ]
        if subset.empty:
            print("No records found for fold %i" % fold)
            errors = True

    # only folds 101-110 should have two sources injected:
    subset = df.loc[ df['SegmentInfo.FoldID'] < 100 ]
    if any( subset[ manipulations2 ].T.sum() > 0 ):
        print("Unexpected 2nd manipulation discovered outside of folds 101-110")
        errors = True

    subset = df.loc[ df['SegmentInfo.FoldID'] > 100 ]
    if not ( all( subset[ manipulations ].T.sum() > 0 ) and all( subset[ manipulations2 ].T.sum() > 0 ) ):
        print("Entries in folds 101-110 are missing 2nd injection!")
        errors = True


    # next test is run on folds 1-10 in aggregate.
    # check for issue reported by Ken: stuff labeled 'fissile', etc. in folds 1-10 w/no injection
    folds1_10 = df[ (df['SegmentInfo.FoldID'] < 11) ]

    subset = folds1_10[  (folds1_10['Manipulation.PointSourceInjected'] == 0 ) &
            (folds1_10['Manipulation.DistSourceInjected'] == 0 ) ]
    classesRequiringInjection = ['Classification.Source.Medical','Classification.Source.Industrial',
            'Classification.Source.Fissile', 'Classification.Source.NORM', 'Classification.Source.Contamination']
    if subset[ classesRequiringInjection ].sum().sum() != 0:
        print( "Encountered records with classification info but no manipulations:" )
        print( subset[ classesRequiringInjection ].sum() )
        errors = True

    # Sanity check: most injected sources should pass the 'isEmitting' test, none of the original non-emitting set should
    subset = folds1_10[ (folds1_10['Classification.Source.NonEmitting'] == 1) ]
    nEmitting = subset['Classification.IsEmitting'].sum()
    if nEmitting > 0:
        print( "%.2f%% of records considered 'emitting' in folds 1-10 NonEmitting" % (100. * nEmitting / len(subset)) )
        errors = True

    for sourceClass in classesRequiringInjection:
        subset = folds1_10[ (folds1_10[sourceClass] == 1) ]
        if len(subset)==0:
            print( "No records with %s found in folds 1-10" % sourceClass )
            errors = True
            continue

        nEmitting = subset['Classification.IsEmitting'].sum()
        if float(nEmitting) / len(subset) < 0.75:
            print( "Only %.2f%% of records in folds 1-10 %s injections are considered 'emitting'" % (
                100 * float(nEmitting) / len(subset), sourceClass.split('.')[-1] ) )
            # FIXME treat as an error?


    # NORM injections (and NonEmitting) should have CargoModel == 0,  other injections should have CargoModel in range [1,4]
    for classification in ('NonEmitting', 'NORM'):
        if set( folds1_10[ folds1_10['Classification.Source.'+classification] == 1 ]['Manipulation.CargoModel'] ) != set([0]):
            print( "Encountered %s NORM training sources with non-zero cargo model!" % classification )
            errors = True
    for classification in ('Medical', 'Industrial', 'Fissile', 'Contamination'):
        if set( folds1_10[ folds1_10['Classification.Source.'+classification] == 1 ]['Manipulation.CargoModel'] ) != set(range(0,5)):
            print( "Encountered %s non-NORM training sources with unknown cargo model!" % classification )
            errors = True


    for fold in range(12,17):    # additional training folds with real Med, Ind, FIS, CONTAM and NORM records
        subset = df[ df['SegmentInfo.FoldID'] == fold ]
        if subset.empty:
            print("No records found for fold %i" % fold)
            # FIXME treat as error?

    # Irregular scans should only appear in Fold20:
    irregularFolds = set(df[ df['Classification.IrregularScan'] == 1 ][ 'SegmentInfo.FoldID' ])
    if 20 not in irregularFolds:
        print( "WARNING: No irregular records found in fold 20 stream-of-commerce. Error in feature building?" )
    irregularFolds.discard(20)
    if irregularFolds:
        print( "Irregular records found in folds %s" % irregularFolds )
        errors = True

    return errors


def injectionLocationPlots( df, directory, step = 1, dpi = None):
    """ For each type of source, plot injected and detected source locations """

    for (source,fold) in (('Medical',32),('Industrial',33),('Fissile',34)):
        fig, ax = plt.subplots( 2,1 )

        for adx, axis in enumerate(('Y','Z')):
            subplot = ax[adx]
            xaxis = 'Manipulation.PointX'
            xaxisden = 'Features.Vehicle.Length'
            yaxis = 'Manipulation.Point'+axis

            mask = df['Classification.Source.' + source] == 1
            subplot.plot(
                    df[ mask ][ xaxis ] / df[ mask ][ xaxisden ],
                    df[ mask ][ yaxis ],
                    marker = '.', linestyle = '', label='Folds1-10' )
            mask = df['SegmentInfo.FoldID'] == fold
            subplot.plot(
                    df[ mask ][ xaxis ] / df[ mask ][ xaxisden ],
                    df[ mask ][ yaxis ],
                    marker = '.', linestyle = '', label='Fold%i' % fold )
            subplot.set_ylabel( '.'.join( yaxis.split('.')[-2:] ) )
            if adx==0:
                subplot.legend( loc=2 )
            elif adx==1:
                subplot.set_xlabel( '%s / %s' % (xaxis, xaxisden) )

        #fig.set_tight_layout(True)
        fig.suptitle("%s Injections" % source)
        plt.savefig( os.path.join( directory, "%s_injections.png" % source ) , dpi= dpi if dpi else fig.dpi)
        plt.close( fig )


def widthVsIntensityPlots( df, directory, step = 1, foldId=None, RPMId=None, DSId=None, dpi = None ):
    """
    Plot distribution of intensity vs assumed source FWHM for non-emitting plus various injections.
    Generates three plots:
     training only (folds 1-10)
     training + testing (folds 1-10 and 32-34)
     training + unlabeled (1-10 and fold 20)
    """

    classes = [c for c in df.columns if c.startswith('Classification.Source')]
    classes.remove( 'Classification.Source.NonEmitting' )
    classes.remove( 'Classification.Source.Mixed' )
    classes = [ 'Classification.Source.NonEmitting' ] + classes   # plot nonemitting first, so others lie on top

    peakPortionIntensity = lambda df: df['Features.Statistics.Peak.Intensity']
    maxNSigma = lambda df: df[ [c for c in df.columns if c.startswith('Features.NSigma')] ].max(axis=1)

    training = df['SegmentInfo.FoldID'].isin(range(1,11))
    testing = df['SegmentInfo.FoldID'].isin(range(32,35))
    fold20 = df['SegmentInfo.FoldID'] == 20

    # first pass: draw training folds and then overlay with unlabeled data
    for intensityFeatureExtractor, ylabel, plotName, yRange, title in (
            (peakPortionIntensity,'PeakPortion.Intensity','WidthVsIntensity.png',(1,1e+5), 'Width vs. Intensity'),
            (maxNSigma, "Max N-Sigma",'WidthVsNSigma.png',(1e-2,1e+4), 'Width vs. NSigma')):
        fig, ax = plt.subplots()
        for class_ in classes:
            thisClass = df.loc[ training & (df[class_] == 1) ]
            color = colors[ class_.replace('Classification.Source.','') ]
            ax.semilogy( thisClass['Features.Extent.FWHM.In'][::step],
                    intensityFeatureExtractor(thisClass)[::step], color = color,
                    marker = '.', markersize=1, linestyle='', alpha=0.5, label=class_.split('.')[-1] )
            del thisClass

        if foldId: title += ": Fold %d," % foldId
        elif RPMId: title += ": RPMid %d," % RPMId
        elif DSId: title += ": DataSourceId %d" % DSId
        else: title += ": Folds 1-10"

        ax.set_title(title)
        ax.set_xlabel("Extent.FWHM.In")
        ax.set_ylabel(ylabel)
        #ax.set_xlim( 1e-1, 1e+2 )
        ax.set_xlim( 0, 30 )
        ax.set_ylim( *yRange )
        lgnd = ax.legend(numpoints=1)
        for handle in lgnd.legendHandles: handle._legmarker.set_markersize(10)

        plt.savefig( os.path.join( directory, plotName ) , dpi=dpi if dpi else fig.dpi)

        # Add unlabeled from fold 20 for additional plot:
        unlabeled = df.loc[ fold20 ]
        ax.semilogy( unlabeled['Features.Extent.FWHM.In'][::step],
                intensityFeatureExtractor(unlabeled)[::step], color=colors['Unlabeled'],
                marker = '.', markersize=1, linestyle='', alpha=0.5, label='SoC' )

        lgnd.remove()
        lgnd = ax.legend(numpoints=1)
        for handle in lgnd.legendHandles: handle._legmarker.set_markersize(10)

        title += ", 20"
        ax.set_title(title)
        plt.savefig( os.path.join( directory, plotName.replace( '.png', '_addSoC.png') ) , dpi=dpi if dpi else fig.dpi)

        plt.close( fig )


    # second pass: training folds overlaid with test folds
    for intensityFeatureExtractor, ylabel, plotName, yRange, title in (
            (peakPortionIntensity,'PeakPortion.Intensity','WidthVsIntensity.png',(1,1e+5), 'Width vs. Intensity'),
            (maxNSigma, "Max N-Sigma",'WidthVsNSigma.png',(1e-2,1e+4), 'Width vs. NSigma')):
        fig, ax = plt.subplots()
        for class_ in classes:
            thisClass = df.loc[ training & (df[class_] == 1) ]
            color = colors[ class_.replace('Classification.Source.','') ]
            ax.semilogy( thisClass['Features.Extent.FWHM.In'][::step],
                    intensityFeatureExtractor(thisClass)[::step], color=color,
                    marker = '.', markersize=1, linestyle='', alpha=0.5, label=class_.split('.')[-1] )
            del thisClass

        if foldId: title += ": Fold %d, 32-34" % foldId
        elif RPMId: title += ": RPMid %d, Folds 32-34" % RPMId
        elif DSId: title += ": DataSourceId %d, Folds 32-34" % DSId
        else: title += ": Folds 1-10, 32-34"

        ax.set_title(title)
        ax.set_xlabel("Extent.FWHM.In")
        ax.set_ylabel(ylabel)
        #ax.set_xlim( 1e-1, 1e+2 )
        ax.set_xlim( 0, 30 )
        ax.set_ylim( *yRange )
        lgnd = ax.legend(numpoints=1)
        for handle in lgnd.legendHandles: handle._legmarker.set_markersize(10)

        # next add test folds:
        for class_ in ('Medical', 'Industrial', 'Fissile'):
            thisClass = df.loc[ testing & (df['Classification.Source.' + class_] == 1) ]
            if thisClass.empty: continue
            ax.semilogy( thisClass['Features.Extent.FWHM.In'][::step*10],
                    intensityFeatureExtractor(thisClass)[::step*10], color=colors[class_],
                    marker = 'o', markersize=2, linestyle='', label=class_+" test" )
            del thisClass

        plt.savefig( os.path.join( directory, plotName.replace('.png','_addTesting.png') ) , dpi=dpi if dpi else fig.dpi)

        plt.close( fig )


def originalAlarmPlots( df, directory, step = 1, foldId=None, RPMId=None, DSId=None, dpi=None):
    """ Plot width vs max(NSigma) for alarming / non-alarming based on N-Sigma. """

    uninjected = df.loc[ df['SegmentInfo.FoldID'].isin([11,20]) & (df['Classification.IrregularScan'] == 0) ][::step]
    alarms = uninjected['Classification.Resolution.Alarm'] == 1
    neutron = uninjected['Alarm.Neutron'] == 1

    nSigmas = [c for c in df.columns if c.startswith("Features.NSigma")]
    maxNSigma = uninjected[nSigmas].max(axis=1)

    fig, ax = plt.subplots()
    ax.loglog( uninjected['Features.Extent.FWHM.In'],
            maxNSigma, marker = '.', markersize=1, linestyle='', label='no alarm' )
    ax.loglog( uninjected['Features.Extent.FWHM.In'][alarms],
            maxNSigma[alarms], marker = '.', markersize=2, linestyle='', label='gamma' )
    ax.loglog( uninjected['Features.Extent.FWHM.In'][neutron],
            maxNSigma[neutron], marker = '.', markersize=2, color='r', linestyle='', label='neutron' )

    title = "Distribution of alarming scans"
    ax.set_title(title)
    ax.set_xlabel("Extent.FWHM.In")
    ax.set_ylabel("Max NSigma")
    ax.set_xlim( 1e-1, 1e+2 )
    ax.set_ylim( 1e-2, 1e+4 )
    lgnd = ax.legend(numpoints=1)   # lower right
    for handle in lgnd.legendHandles: handle._legmarker.set_markersize(10)

    #fig.set_size_inches( [s*2 for s in fig.get_size_inches()] )
    plt.savefig( os.path.join( directory, "NSigmaAlarms.png" ), dpi=dpi if dpi else fig.dpi )
    plt.close( fig )


def locationFeaturePlots( df, directory, step = 1, foldId=None, dpi=None):
    """
    Plots showing how the new extent and statistics features are working for multi-source injections.
    Generates plots of the following sets of features (x VS y) for folds 1-10 and 101-110:
    Features.Extent.FWHM.In  vs.  Features.Extent.FWHM.Out
    Features.Extent.PeakVsFWHM  vs. Features.Extent.FWTopBottom.Ratio
    Features.Statistics.Split.Position  vs. Features.Statistics.Split.Intensity
    Features.Statistics.Front.XMean  vs.  Features.Statistics.Rear.XMean
    """

    axis_list = [["Features.Extent.FWHM.In","Features.Extent.FWHM.Out", "FWHMIn_Vs_FWHMOut.png", "", "linear"],
                      ["Features.Extent.PeakVsFWHM","Features.Extent.FWTopBottom.Ratio","PeakVsFWHM_Vs_FWTopBottomRatio.png", "", "linear" ],
                      ["Features.Statistics.Split.Position","Features.Statistics.Split.Intensity","SplitPosition_Vs_SplitIntensity.png","", "log"],
                      ["Features.Statistics.Front.XMean","Features.Statistics.Rear.XMean","FrontXMean_Vs_RearXMean.png", "", "linear"]]

    training = df['SegmentInfo.FoldID'].isin(range(101,111))
    fold20 = df['SegmentInfo.FoldID'] == 20

    # modify labels for folds 101-110:
    labels = df.loc[training]['LABEL']
    pattern = '|'.join(('Medical','Industrial','Fissile','Contamination'))
    df.loc[training, 'LABEL'] = labels.str.replace(pattern, 'THREAT')
    fourClasses = ('NORM-NORM', 'NORM-THREAT', 'THREAT-NORM', 'THREAT-THREAT')

    for x_axis, y_axis, plotName, title, plot_type in axis_list:
        fig, ax = plt.subplots()
        for class_ in fourClasses:
            thisClass = df.loc[ training & (df['LABEL'] == class_) ]
            color = colors_4class[ class_ ]

            if "linear" in plot_type:
                ax.plot( thisClass[x_axis][::step],
                        thisClass[y_axis][::step], color = color,
                        marker = '.', markersize=1, linestyle='', alpha=0.5, label=class_ )
                subdf = thisClass[thisClass[x_axis]==thisClass[y_axis]].copy()
                ax.plot(subdf[x_axis][::step],
                        subdf[y_axis][::step], 'k-', alpha=0.75, zorder=0)
                del subdf
            else:
                ax.semilogy( thisClass[x_axis][::step],
                        thisClass[y_axis][::step], color = color,
                        marker = '.', markersize=1, linestyle='', alpha=0.5, label=class_ )
            del thisClass

        #yRange = (1,1e+5)
        if foldId: title += "fold %d," % foldId
        else: title += "Multi-source"
        ax.set_title(title)
        ax.set_xlabel(x_axis)
        ax.set_ylabel(y_axis)
        #ax.set_xlim( 1e-1, 1e+2 )
        #ax.set_ylim( *yRange )
        lgnd = ax.legend(numpoints=1)
        for handle in lgnd.legendHandles: handle._legmarker.set_markersize(10)

        plt.savefig( os.path.join( directory, plotName ), dpi=dpi if dpi else fig.dpi )

        # Add unlabeled from fold 20 for additional plot:
        unlabeled = df.loc[ fold20 ]

        if "linear" in plot_type:
            ax.plot( unlabeled[x_axis][::step*7],
                    unlabeled[y_axis][::step*7], color=colors_4class['Unlabeled'],
                    marker = '.', markersize=1, linestyle='', alpha=0.5, label='SoC' )
        else:
            ax.loglog( unlabeled[x_axis][::step*7],
                    unlabeled[y_axis][::step*7], color=colors_4class['Unlabeled'],
                    marker = '.', markersize=1, linestyle='', alpha=0.5, label='SoC' )

        lgnd.remove()
        lgnd = ax.legend(numpoints=1)
        for handle in lgnd.legendHandles: handle._legmarker.set_markersize(10)

        plt.savefig( os.path.join( directory, plotName.replace( '.png', '_addSoC.png') ), dpi=dpi if dpi else fig.dpi )

        plt.close( fig )

def multiSourceScatterPlots( df, directory, step = 1, foldId=None, dpi=None ):
    """
    Plot distribution of intensity vs assumed source FWHM for multi-source folds
    Generates three plots:
     training only (folds 101-110)
     training + testing (folds 101-110 and 42-44)
     training + testing + unlabeled (101-110, 42-44 and fold 20)
    """

    peakPortionIntensity = lambda df: df['Features.Statistics.Peak.Intensity']
    maxNSigma = lambda df: df[ [c for c in df.columns if c.startswith('Features.NSigma')] ].max(axis=1)

    training = df['SegmentInfo.FoldID'].isin(range(101,111))
    testing = df['SegmentInfo.FoldID'].isin(range(42,45))
    fold20 = df['SegmentInfo.FoldID'] == 20

    # modify labels for folds 101-110:
    labels = df.loc[training]['LABEL']
    pattern = '|'.join(('Medical','Industrial','Fissile','Contamination'))
    df.loc[training, 'LABEL'] = labels.str.replace(pattern, 'THREAT')
    fourClasses = ('NORM-NORM', 'NORM-THREAT', 'THREAT-NORM', 'THREAT-THREAT')

    # first pass: draw training folds and then overlay with unlabeled data
    for intensityFeatureExtractor, ylabel, plotName, yRange in (
            (peakPortionIntensity,'PeakPortion.Intensity','WidthVsIntensity_multiSource.png',(1,1e+5)),
            (maxNSigma, "Max N-Sigma",'WidthVsNSigma_multiSource.png',(1e-2,1e+4))):
        fig, ax = plt.subplots()
        for class_ in fourClasses:
            thisClass = df.loc[ training & (df['LABEL'] == class_) ]
            color = colors_4class[ class_ ]
            ax.semilogy( thisClass['Features.Extent.FWHM.In'][::step],
                    intensityFeatureExtractor(thisClass)[::step], color = color,
                    marker = '.', markersize=1, linestyle='', alpha=0.5, label=class_ )
            del thisClass

        title = "Multi-Source Records"
        if foldId: title += " fold %d," % foldId
        ax.set_title(title)
        ax.set_xlabel("Extent.FWHM.In")
        ax.set_ylabel(ylabel)
        #ax.set_xlim( 1e-1, 1e+2 )
        ax.set_xlim( 0, 30 )
        ax.set_ylim( *yRange )
        lgnd = ax.legend(numpoints=1)
        for handle in lgnd.legendHandles: handle._legmarker.set_markersize(10)

        plt.savefig( os.path.join( directory, plotName ), dpi=dpi if dpi else fig.dpi)

        # Add unlabeled from fold 20 for additional plot:
        unlabeled = df.loc[ fold20 ]
        ax.semilogy( unlabeled['Features.Extent.FWHM.In'][::step*7],
                intensityFeatureExtractor(unlabeled)[::step*7], color=colors_4class['Unlabeled'],
                marker = '.', markersize=1, linestyle='', alpha=0.5, label='SoC' )

        lgnd.remove()
        lgnd = ax.legend(numpoints=1)
        for handle in lgnd.legendHandles: handle._legmarker.set_markersize(10)

        plt.savefig( os.path.join( directory, plotName.replace( '.png', '_addSoC.png') ), dpi=dpi if dpi else fig.dpi )

        plt.close( fig )


    # second pass: training folds overlaid with test folds
    for intensityFeatureExtractor, ylabel, plotName, yRange in (
            (peakPortionIntensity,'PeakPortion.Intensity','WidthVsIntensity_multiSource.png',(1,1e+5)),
            (maxNSigma, "Max N-Sigma",'WidthVsNSigma_multiSource.png',(1e-2,1e+4))):
        fig, ax = plt.subplots()
        for class_ in fourClasses:
            thisClass = df.loc[ training & (df['LABEL'] == class_) ]
            color = colors_4class[ class_ ]
            ax.semilogy( thisClass['Features.Extent.FWHM.In'][::step],
                    intensityFeatureExtractor(thisClass)[::step], color=color,
                    marker = '.', markersize=1, linestyle='', alpha=0.5, label=class_.split('.')[-1] )
            del thisClass

        title = "Multi-Source Records"
        if foldId: title += " fold %d," % foldId
        ax.set_title(title)
        ax.set_xlabel("Extent.FWHM.In")
        ax.set_ylabel(ylabel)
        #ax.set_xlim( 1e-1, 1e+2 )
        ax.set_xlim( 0, 30 )
        ax.set_ylim( *yRange )
        lgnd = ax.legend(numpoints=1)
        for handle in lgnd.legendHandles: handle._legmarker.set_markersize(10)

        # next add test folds:
        for class_ in ('Medical', 'Industrial', 'Fissile'):
            thisClass = df.loc[ testing & (df['Classification.Source.' + class_] == 1) ]
            if thisClass.empty: continue
            ax.semilogy( thisClass['Features.Extent.FWHM.In'][::step*10],
                    intensityFeatureExtractor(thisClass)[::step*10], color=colors[class_],
                    marker = 'o', markersize=2, linestyle='', label=class_+" test" )
            del thisClass

        plt.savefig( os.path.join( directory, plotName.replace('.png','_addTesting.png') ), dpi=dpi if dpi else fig.dpi )

        plt.close( fig )

# df - Feature Table in pandas Dataframe form
# directory - The root saved directory
# step - the stride in the data when plotting
# dpi - Dots Per Inch, higher the dots the better the resolution and larger the size of the image
def peakIntensityHistogram( df, directory, step = 1, dpi=None):
    _alpha=0.75

    for feature_column, title_prefix, x_label, file_prefix in (
            ("Features.Statistics.Peak.Intensity", "Feature Table", "Peak.Intensity", "peakIntensityHistogram_folds"),
            ("Features.Standard.Gross.All", "Feature Table", "Standard.Gross.All", "standard.Gross.All_Histogram_folds")
    ):
        training = df['SegmentInfo.FoldID'].isin(range(1,11))
        testing = df['SegmentInfo.FoldID'].isin(range(32,35))
        fold20 = df['SegmentInfo.FoldID'] == 20

        classes = [c for c in df.columns if c.startswith('Classification.Source')]
        classes.remove( 'Classification.Source.NonEmitting' )
        classes.remove( 'Classification.Source.Mixed' )
        classes = [ 'Classification.Source.NonEmitting' ] + classes   # plot nonemitting first, so others lie on top

        # use a dictionary first to make sure data belongs to the correct class
        # it's another level of keeping the date integrity
        plotDataDic = {}

        for class_ in classes:
            thisClass = df.loc[ training & (df[class_] == 1) ]
            key = class_.replace("Classification.Source.", "")
            plotDataDic[key] = thisClass[feature_column].values.tolist()
            del thisClass

        fig, ax = plt.subplots()

        ax.set_title("{} Folds 1-10".format(title_prefix))
        ax.set_xlabel(x_label)

        ax.set_ylabel("Counts")
        ax.set_yscale("log")
        ax.set_ylim(1e+0, 1e+4)
        ax.set_xscale("log")

        colorList = []
        colorDict = {}
        dataList = []

        for key, value in plotDataDic.items():
            colorList.append(colors[key])
            dataList.append(value)
            colorDict[key] = colors[key]

        # n is counts in each bin
        n, bins, patches = plt.hist(
            dataList,
            bins=numpy.logspace(numpy.log10(1), numpy.log10(1e7), 50),
            color=colorList,
            histtype="step",
            alpha=_alpha)

        labels = [mpatches.Patch(color=v, label=k, alpha=_alpha) for k,v in colorDict.items()]
        plt.legend(handles=labels, loc="best", prop={'size': 6})

        plt.savefig( os.path.join( directory, "{}1-10.png".format(file_prefix) ), dpi=dpi if dpi else 200)
        plt.close( fig )

        line_style = "--"
        legend_list = []

        ################################################################################
        # fold 20 + test set
        fig, ax = plt.subplots()

        ax.set_title("{} Folds 20,32-34".format(title_prefix))
        ax.set_xlabel(x_label)

        ax.set_ylabel("Counts")
        ax.set_yscale("log")
        ax.set_ylim(1e+0, 1e+5)
        ax.set_xscale("log")

        # use a dictionary first to make sure data belongs to the correct class
        # it's another level of keeping the date integrity
        plotDataDic = {}
        colorDict = {}

        sub_df = df.loc[ fold20 ]

        n, bins, patches = plt.hist(
            sub_df[feature_column].values.tolist(),
            bins=numpy.logspace(numpy.log10(1), numpy.log10(1e7), 50),
            color=colors["SoC"],
            histtype="step",
            hatch='.',
            fill=True,
            alpha=0.1
            )

        legend_list.append(mpatches.Patch(color=colors["SoC"], alpha=0.1, hatch='.', label="Soc"))

        sub_df = df.loc[ testing ]
        for class_ in ('Medical', 'Industrial', 'Fissile'):
            thisClass = sub_df.loc[ sub_df['Classification.Source.' + class_] == 1 ]
            if thisClass.empty: continue
            plotDataDic[class_] = thisClass[feature_column].values.tolist()
            del thisClass

        colorList = []
        dataList = []

        for key, value in plotDataDic.items():
            colorList.append(colors[key])
            dataList.append(value)
            colorDict[key] = colors[key]

        # n is counts in each bin
        n, bins, patches = plt.hist(
            dataList,
            bins=numpy.logspace(numpy.log10(1), numpy.log10(1e7), 50),
            color=colorList,
            histtype="step",
            linestyle=line_style,
            alpha=_alpha
            )

        for k, v in colorDict.items():
            legend_list.append(
                mpatches.Patch(
                    color=v,
                    alpha=_alpha,
                    linestyle=line_style,
                    label=k)
            )
        plt.legend(handles=legend_list, loc="best", prop={'size': 6})

        plt.savefig( os.path.join( directory, "{}20_32-34.png".format(file_prefix) ), dpi=dpi if dpi else 200)
        plt.close( fig )

        ################################################################################
        # Subplot 1 : Non-emitting, NORM, Medical from 1-10, SoC folds 20, Medical test from 32
        fig, ax = plt.subplots()

        ax.set_title("{} Folds 1-10, 20, 32".format(title_prefix))
        ax.set_xlabel(x_label)

        ax.set_ylabel("Counts")
        ax.set_yscale("log")
        ax.set_ylim(1e+0, 1e+5)
        ax.set_xscale("log")

        plotDataDic = {}
        legend_list = []

        sub_df = df.loc[ fold20 ]

        n, bins, patches = plt.hist(
            sub_df[feature_column].values.tolist(),
            bins=numpy.logspace(numpy.log10(1), numpy.log10(1e7), 50),
            color=colors["SoC"],
            histtype="step",
            hatch='.',
            fill=True,
            alpha=0.1
            )

        legend_list.append(mpatches.Patch(color=colors["SoC"], alpha=0.1, hatch='.', label="Soc"))

        sub_df = df.loc[ training ]

        classes = [ 'Classification.Source.NonEmitting', 'Classification.Source.NORM', 'Classification.Source.Medical' ]

        for class_ in classes:
            thisClass = sub_df.loc[ sub_df[class_] == 1 ]
            key = class_.replace("Classification.Source.", "")
            plotDataDic[key] = thisClass[feature_column].values.tolist()
            del thisClass

        colorList = []
        dataList = []

        for key, value in plotDataDic.items():
            colorList.append(colors[key])
            dataList.append(value)
            legend_list.append(
                mpatches.Patch(
                    color=colors[key],
                    label=key,
                    alpha=_alpha)
            )

        # n is counts in each bin
        n, bins, patches = plt.hist(dataList, bins=numpy.logspace(numpy.log10(1), numpy.log10(1e7), 50), color=colorList, histtype="step", alpha=_alpha)

        sub_df = df.loc[ testing ]
        thisClass = sub_df.loc[ sub_df['Classification.Source.Medical'] == 1 ]
        n, bins, patches = plt.hist(
            thisClass[feature_column].values.tolist(),
            bins=numpy.logspace(numpy.log10(1), numpy.log10(1e7), 50),
            color=colors["Medical Test"],
            histtype="step",
            linestyle=line_style
            )
        del thisClass

        legend_list.append(
            mpatches.Patch(
                color=colors["Medical Test"],
                alpha=1.0,
                linestyle=line_style,
                label="Medical Test")
        )

        plt.legend(handles=legend_list, loc="best", prop={'size': 6})

        plt.savefig( os.path.join( directory, "{}1-10_20_32.png".format(file_prefix) ), dpi=dpi if dpi else 200)
        plt.close( fig )

        ################################################################################
        # Subplot 2 : Non-emitting, NORM, Industrial from 1-10, SoC folds 20, Industrial test from 33
        fig, ax = plt.subplots()

        ax.set_title("{} Folds 1-10, 20, 33".format(title_prefix))
        ax.set_xlabel(x_label)

        ax.set_ylabel("Counts")
        ax.set_yscale("log")
        ax.set_ylim(1e+0, 1e+5)
        ax.set_xscale("log")

        plotDataDic = {}
        legend_list = []

        sub_df = df.loc[ fold20 ]

        n, bins, patches = plt.hist(
            sub_df[feature_column].values.tolist(),
            bins=numpy.logspace(numpy.log10(1), numpy.log10(1e7), 50),
            color=colors["SoC"],
            histtype="step",
            hatch='.',
            fill=True,
            alpha=0.1
            )

        legend_list.append(mpatches.Patch(color=colors["SoC"], alpha=0.1, hatch='.', label="Soc"))

        sub_df = df.loc[ training ]

        classes = [ 'Classification.Source.NonEmitting', 'Classification.Source.NORM', 'Classification.Source.Industrial' ]

        for class_ in classes:
            thisClass = sub_df.loc[ sub_df[class_] == 1 ]
            key = class_.replace("Classification.Source.", "")
            plotDataDic[key] = thisClass[feature_column].values.tolist()
            del thisClass

        colorList = []
        dataList = []

        for key, value in plotDataDic.items():
            colorList.append(colors[key])
            dataList.append(value)
            legend_list.append(
                mpatches.Patch(
                    color=colors[key],
                    label=key,
                    alpha=_alpha)
            )

        # n is counts in each bin
        n, bins, patches = plt.hist(dataList, bins=numpy.logspace(numpy.log10(1), numpy.log10(1e7), 50), color=colorList, histtype="step", alpha=_alpha)

        sub_df = df.loc[ testing ]
        thisClass = sub_df.loc[ sub_df['Classification.Source.Industrial'] == 1 ]
        n, bins, patches = plt.hist(
            thisClass[feature_column].values.tolist(),
            bins=numpy.logspace(numpy.log10(1), numpy.log10(1e7), 50),
            color=colors["Industrial Test"],
            histtype="step",
            linestyle=line_style
            )
        del thisClass

        legend_list.append(
            mpatches.Patch(
                color=colors["Industrial Test"],
                alpha=1.0,
                linestyle=line_style,
                label="Industrial Test")
        )

        plt.legend(handles=legend_list, loc="best", prop={'size': 6})

        plt.savefig( os.path.join( directory, "{}1-10_20_33.png".format(file_prefix) ), dpi=dpi if dpi else 200)
        plt.close( fig )

        ################################################################################
        # Subplot 3 : Non-emitting, NORM, Fissile from 1-10, SoC folds 20, Fissile test from 34
        fig, ax = plt.subplots()

        ax.set_title("{} Folds 1-10, 20, 34".format(title_prefix))
        ax.set_xlabel(x_label)

        ax.set_ylabel("Counts")
        ax.set_yscale("log")
        ax.set_ylim(1e+0, 1e+5)
        ax.set_xscale("log")

        plotDataDic = {}
        legend_list = []

        sub_df = df.loc[ fold20 ]

        n, bins, patches = plt.hist(
            sub_df[feature_column].values.tolist(),
            bins=numpy.logspace(numpy.log10(1), numpy.log10(1e7), 50),
            color=colors["SoC"],
            histtype="step",
            hatch='.',
            fill=True,
            alpha=0.1
            )

        legend_list.append(mpatches.Patch(color=colors["SoC"], alpha=0.1, hatch='.', label="Soc"))

        sub_df = df.loc[ training ]

        classes = [ 'Classification.Source.NonEmitting', 'Classification.Source.NORM', 'Classification.Source.Fissile' ]

        for class_ in classes:
            thisClass = sub_df.loc[ sub_df[class_] == 1 ]
            key = class_.replace("Classification.Source.", "")
            plotDataDic[key] = thisClass[feature_column].values.tolist()
            del thisClass

        colorList = []
        dataList = []

        for key, value in plotDataDic.items():
            colorList.append(colors[key])
            dataList.append(value)
            legend_list.append(
                mpatches.Patch(
                    color=colors[key],
                    label=key,
                    alpha=_alpha)
            )

        # n is counts in each bin
        n, bins, patches = plt.hist(dataList, bins=numpy.logspace(numpy.log10(1), numpy.log10(1e7), 50), color=colorList, histtype="step", alpha=_alpha)

        sub_df = df.loc[ testing ]
        thisClass = sub_df.loc[ sub_df['Classification.Source.Fissile'] == 1 ]
        n, bins, patches = plt.hist(
            thisClass[feature_column].values.tolist(),
            bins=numpy.logspace(numpy.log10(1), numpy.log10(1e7), 50),
            color=colors["Fissile Test"],
            histtype="step",
            linestyle=line_style
            )

        del thisClass

        legend_list.append(
            mpatches.Patch(
                color=colors["Fissile Test"],
                alpha=1.0,
                linestyle=line_style,
                label="Fissile Test")
        )

        plt.legend(handles=legend_list, loc="best", prop={'size': 6})

        plt.savefig( os.path.join( directory, "{}1-10_20_34.png".format(file_prefix) ), dpi=dpi if dpi else 200)
        plt.close( fig )


def laneWidthBoxPlots(df, directory, minPeakCutoff=300, step = 1, dpi=None):
    """
    Plots to see the impact of Features.Vehicle.LaneWidth.
    Generate boxplot for folds 1-10 and Fold 20 using features:
        Features.Statistics.Peak.XStdDev
        Features.Extent.FWHM.In

    Group by vehicle lane width bands with bin size of 0.25;
    restricting to records with Features.Statistics.Peak.Intensity > minPeakCutoff
    """

    # Get the Source type
    classes = [c for c in df.columns if c.startswith('Classification.Source')]
    classes.remove( 'Classification.Source.NonEmitting' )
    classes.remove( 'Classification.Source.Mixed' )
    classes = [ 'Classification.Source.NonEmitting' ] + classes

    # Unlabeled first
    predicate = df['SegmentInfo.FoldID'] == 20
    title = "Fold20"
    for columnInfo, subTitle in (
            ("Features.Statistics.Peak.XStdDev", "XStdDev"),
            ("Features.Extent.FWHM.In", "FWHM"),
        ):
        bin_width = 0.25
        boxplotData = []
        boxplotLabel = []
        for laneWidthValue in [4, 4.25,4.5,4.75,5.0,5.25,5.5,5.75]:
            upperBound = laneWidthValue + bin_width

            subDf = df.loc[
                # Selecting the folds
                (predicate) &
                # Selecting the lane width band
                ((df["Features.Vehicle.LaneWidth"] >= laneWidthValue) & (df["Features.Vehicle.LaneWidth"] < upperBound)) &
                # Cut out the records with peak intensity that are below a threshold
                (df["Features.Statistics.Peak.Intensity"] > minPeakCutoff)]

            boxplotData.append(subDf[columnInfo].values)
            boxplotLabel.append("{}".format(laneWidthValue))

        # Plot
        plt.figure(figsize=[12, 10], constrained_layout=True)

        plt.boxplot(
            boxplotData,
            flierprops=dict(markersize=1.5),
            meanprops=dict(color="red"),
            medianprops=dict(color="green"),
            showmeans=True,
            meanline=True,
            labels=boxplotLabel
            )

        plt.title("{} - Lane Width bands".format(title))
        plt.ylabel(columnInfo)
        plt.grid(b=True, which="major", axis="y")
        plt.minorticks_on()
        plt.savefig( os.path.join( directory, "laneWidth{}Boxplot_SoC.png".format(subTitle)), dpi=dpi if dpi else 200)
        plt.close()


    # For each type of folds, plot according to source type
    for predicate, title, filenameFormat in (
        (df['SegmentInfo.FoldID'].isin(range(1,11)), "Folds1-10", "laneWidth{}Boxplot_training_{}.png"),
        (df['SegmentInfo.FoldID'].isin(range(32,33)), "Fold 32", "laneWidth{}Boxplot_Fold32_{}.png"),
        (df['SegmentInfo.FoldID'].isin(range(33,34)), "Fold 33", "laneWidth{}Boxplot_Fold33_{}.png"),
        (df['SegmentInfo.FoldID'].isin(range(34,35)), "Fold 34", "laneWidth{}Boxplot_Fold34_{}.png"),
    ):
        for columnInfo, subTitle in (
            ("Features.Statistics.Peak.XStdDev", "XStdDev"),
            ("Features.Extent.FWHM.In", "FWHM"),
        ):
            for class_ in classes:
                sourceType = class_.split('.')[-1]

                # For folds 32-34,
                if "1-10" not in title:
                    # We can skip Contamination, NORM, or Non-emitting
                    if sourceType in ["Contamination","NORM","NonEmitting"]:
                        continue
                    # For fold 32, we want Medical
                    if "32" in title and "Medical" not in sourceType:
                        continue
                    # For fold 33, we want Industrial
                    if "33" in title and "Industrial" not in sourceType:
                        continue
                    # For fold 34, we want Fissile
                    if "34" in title and "Fissile" not in sourceType:
                        continue

                bin_width = 0.25
                boxplotData = []
                boxplotLabel = []
                for laneWidthValue in [4, 4.25,4.5,4.75,5.0,5.25,5.5,5.75]:
                    upperBound = laneWidthValue + bin_width

                    subDf = df.loc[
                        # Selecting the folds
                        (predicate) &
                        # Select the source
                        (df[class_] == 1) &
                        # Selecting the lane width band
                        ((df["Features.Vehicle.LaneWidth"] >= laneWidthValue) & (df["Features.Vehicle.LaneWidth"] < upperBound)) &
                        # Cut out the records with peak intensity that are below a threshold
                        (df["Features.Statistics.Peak.Intensity"] > minPeakCutoff)]

                    boxplotData.append(subDf[columnInfo].values)
                    boxplotLabel.append("{}".format(laneWidthValue))

                # Plot
                plt.figure(figsize=[12, 10], constrained_layout=True)

                plt.boxplot(
                    boxplotData,
                    flierprops=dict(markersize=1.5),
                    meanprops=dict(color="red"),
                    medianprops=dict(color="green"),
                    showmeans=True,
                    meanline=True,
                    labels=boxplotLabel
                    )

                plt.title("{} - {} - Lane Width bands".format(title, sourceType))
                plt.ylabel(columnInfo)
                plt.grid(b=True, which="major", axis="y")
                plt.minorticks_on()
                plt.savefig( os.path.join( directory, filenameFormat.format(subTitle, sourceType)), dpi=dpi if dpi else 200)
                plt.close()


# df - Feature Table in pandas Dataframe form
# directory - The root saved directory
# step - the stride in the data when plotting
# dpi - Dots Per Inch, higher the dots the better the resolution and larger the size of the image
def peakClusterPlots( df, directory, step = 1, dpi=None):
# =============================================================================
#     Plotting:
#         Features.Statistics.Peak.Intensity vs Features.Extent.FWHM.Out
#         Features.Statistics.Peak.Intensity vs Features.Extent.XLocation
#         Features.Statistics.Peak.Intensity vs Features.Statistics.Peak.XMean
# =============================================================================

    classes = [c for c in df.columns if c.startswith('Classification.Source')]
    classes.remove( 'Classification.Source.NonEmitting' )
    classes.remove( 'Classification.Source.Mixed' )
    classes = [ 'Classification.Source.NonEmitting' ] + classes   # plot nonemitting first, so others lie on top

    # Removed all rows that has peak intensity < 1
    subdf = df.loc[ df["Features.Statistics.Peak.Intensity"] >= 1 ]

    training = subdf['SegmentInfo.FoldID'].isin(range(1,11))
    fold20 = subdf['SegmentInfo.FoldID'] == 20

    trainingDf = subdf.loc[training]
    unlabeledDf = subdf.loc[ fold20 ]

    for feature in (
            ("Features.Extent.FWHM.Out"),
            ("Features.Extent.XLocation"),
            ("Features.Statistics.Peak.XMean"),
    ):
        # Create the sub directory
        saveDir = os.path.join(directory, feature.replace(".", "_"))
        if not os.path.exists(saveDir):
            os.mkdir(saveDir)

        ########################################################################
        # Plot all the sources in one plot for training
        fig, ax = plt.subplots()
        for class_ in classes:
            thisClass = trainingDf.loc[ (trainingDf[class_] == 1) ]
            color = colors[ class_.replace('Classification.Source.','') ]

            ax.plot( thisClass[feature][::step],
                thisClass["Features.Statistics.Peak.Intensity"][::step], color = color,
                marker = '.', markersize=1, linestyle='', alpha=0.5, label=class_.split('.')[-1] )
            del thisClass

        ax.set_title("Peak Clusters Training")
        ax.set_xlabel(feature)
        ax.set_ylabel("Features.Statistics.Peak.Intensity")
        ax.set_yscale("log")
        ax.set_xscale("log")

        lgnd = ax.legend(numpoints=1)
        for handle in lgnd.legendHandles: handle._legmarker.set_markersize(6)

        plt.savefig( os.path.join( saveDir, f"{feature}.training.all.png" ), dpi=dpi if dpi else 200 )
        plt.close( fig )


        ########################################################################
        # Add in fold 20
        fig, ax = plt.subplots()

        # SoC
        ax.plot(unlabeledDf[feature][::step],
                unlabeledDf["Features.Statistics.Peak.Intensity"][::step], color=colors['Unlabeled'],
                marker = '.', markersize=1, linestyle='', alpha=0.5, label='SoC' )

        # Training
        for class_ in classes:
            thisClass = trainingDf.loc[ (trainingDf[class_] == 1) ]
            color = colors[ class_.replace('Classification.Source.','') ]

            ax.plot( thisClass[feature][::step],
                thisClass["Features.Statistics.Peak.Intensity"][::step], color = color,
                marker = '.', markersize=1, linestyle='', alpha=0.5, label=class_.split('.')[-1] )
            del thisClass

        ax.set_title("Peak Clusters Training + SoC")
        ax.set_xlabel(feature)
        ax.set_ylabel("Features.Statistics.Peak.Intensity")
        ax.set_yscale("log")
        ax.set_xscale("log")

        lgnd = ax.legend(numpoints=1)
        for handle in lgnd.legendHandles: handle._legmarker.set_markersize(6)

        plt.savefig( os.path.join( saveDir, f"{feature}.training_SoC.all.png" ), dpi=dpi if dpi else 200 )
        plt.close( fig )

        #######################################################################
        # Individual sources
        for class_ in classes:
            fig, ax = plt.subplots()

            sourceType = class_.replace('Classification.Source.','')

            thisClass = trainingDf.loc[ (trainingDf[class_] == 1) ]
            color = colors[ sourceType ]

            # Plot fold 20
            ax.plot(unlabeledDf[feature][::step],
                    unlabeledDf["Features.Statistics.Peak.Intensity"][::step], color=colors['Unlabeled'],
                    marker = '.', markersize=1, linestyle='', alpha=0.5, label='SoC' )

            # Plot training source type
            ax.plot( thisClass[feature][::step],
                thisClass["Features.Statistics.Peak.Intensity"][::step], color = color,
                marker = '.', markersize=1, linestyle='', alpha=0.5, label=sourceType )


            ax.set_title(f"Peak Clusters {sourceType} Training + SoC")
            ax.set_xlabel(feature)
            ax.set_ylabel("Features.Statistics.Peak.Intensity")
            ax.set_yscale("log")
            ax.set_xscale("log")

            lgnd = ax.legend(numpoints=1)
            for handle in lgnd.legendHandles: handle._legmarker.set_markersize(6)

            plt.savefig( os.path.join( saveDir, f"{feature}.train_SoC_{sourceType}.png" ), dpi=dpi if dpi else 200 )
            plt.close( fig )


# make the script callable
# on error return False
def testFeatureTable(
        featureTableDBPath = None,
        foldId = None,
        rpmId = None,
        dataSourceId = None,
        verbose = False,
        dpi = None
    ):

    proj_root = PyPath(PyPath(featureTableDBPath).parent).parent

    # if we have a pickle file, use it
    if ".pickle" in featureTableDBPath:
        print("Loading feature table from pickle...")
        badRecords = pandas.DataFrame()
        df = pandas.read_pickle( featureTableDBPath )
    else:
        print("Loading db...")
        df, badRecords = loadSQL.loadFolds( featureTableDBPath, foldId, verbose=verbose )
        # feature tables can be huge. Save as pickle for faster load times later on:
        pandas.to_pickle(df, featureTableDBPath + '.pickle')

    if not badRecords.empty:  # NaN or Infinity encountered while reading in feature table
        # write the bad records to the log dir
        filename = os.path.join(proj_root, "logs", "testFeatureTableLogs")
        if not os.path.exists(filename):
            os.mkdir(filename)

        filename = os.path.join(filename, "badRecords.csv")
        badRecords.to_csv(filename, header=True, index = False)

    if rpmId:
        df = df.loc[ df['SegmentInfo.Location.RpmId'] == rpmId ]
    if dataSourceId:
        df = df.loc[ df['SegmentInfo.Location.DataSourceId'] == dataSourceId ]

    plotDir = os.path.join(proj_root, "plots", "testFeatureTablePlots")
    if not os.path.exists(plotDir):
        os.mkdir(plotDir)

    ratioError = False
    testError = runTests( df )
    badRecordsRatio = float(len(badRecords)) / float(len(df))
    if badRecordsRatio and badRecordsRatio > 0.0001:
        ratioError = True

    # save plots
    print("Generating plots...")
    widthVsIntensityPlots( df, plotDir, 1, dpi=dpi )
    originalAlarmPlots( df, plotDir, 1, dpi=dpi)
    injectionLocationPlots( df, plotDir, 1, dpi=dpi )
    peakIntensityHistogram(df, plotDir, 1, dpi=dpi)
    laneWidthBoxPlots(df, plotDir, minPeakCutoff=300, step = 1, dpi=dpi)

    # Peak Clusters plots
    plotDir = os.path.join(plotDir,"PeakClusters")
    if not os.path.exists(plotDir):
        os.mkdir(plotDir)
    peakClusterPlots(df, plotDir, 1)

    # fix LABEL column
    col_select_list = ["SegmentInfo.FoldID"]
    col_select_list.extend([column for column in df if column.startswith("Features")])
    classifications = [column for column in df if column.startswith("Classification.Source.")]
    manipulations = [column for column in df if column.startswith("Manipulation")]
    col_select_list.extend(classifications)
    col_select_list.extend(manipulations)

    new_df = df[col_select_list].copy()
    new_df["LABEL"] = "Unlabeled"

    # label each row according to Classification.Source
    for index, row in new_df.iterrows():
        seg_foldId = row["SegmentInfo.FoldID"]
        if seg_foldId == 11 or seg_foldId == 20:
            continue

        if seg_foldId in range(101,111):
            # special logic, need to determine which injection is in front / back
            source1 = row['Manipulation.PointSNum'] or row['Manipulation.DistSNum']
            x1 = row['Manipulation.PointX'] or 0.5 * (row['Manipulation.DistX1'] + row['Manipulation.DistX2'])
            source2 = row['Manipulation2.PointSNum'] or row['Manipulation2.DistSNum']
            x2 = row['Manipulation2.PointX'] or 0.5 * (row['Manipulation2.DistX1'] + row['Manipulation2.DistX2'])

            def standardize_source_label(label):
                # ugly: need to convert strings like 'training-medical.xml.gz#31' into 'Medical'
                label2 = label.split('.xml')[0].split('-')[1].title()
                if label2 == 'Norm':
                    label2 = 'NORM'
                return label2

            source1 = standardize_source_label(source1)
            source2 = standardize_source_label(source2)

            if x1 > x2:
                source1, source2 = source2, source1
            new_df.at[index,"LABEL"] = source1 + "-" + source2

        else:
            for key, value in row.to_dict().items():
                if "Classification.Source." in key and value == 1:
                    new_df.at[index,"LABEL"] = key.replace("Classification.Source.", "")
                    break

    # additional plots, easier to generate after sorting multi-source injections front-to-back:
    multiSourcePlotDir = os.path.join(proj_root, "plots", "multiSource")
    if not os.path.exists(multiSourcePlotDir):
        os.mkdir(multiSourcePlotDir)

    locationFeaturePlots( new_df, multiSourcePlotDir, 1, dpi=dpi)
    multiSourceScatterPlots( new_df, multiSourcePlotDir, 1, dpi=dpi)

    # save fold information to modelBuilding folder
    print("Saving folds data...")

    # get rid of extra columns
    new_df = new_df.drop( columns = classifications + manipulations )
    metadata_columns = [column for column in df if not column.startswith('Features')]
    metadata = df[metadata_columns]

    save_dir = os.path.join(proj_root, "modelBuilding")

    grouped_df = new_df.groupby("SegmentInfo.FoldID")
    grouped_metadata = metadata.groupby("SegmentInfo.FoldID")
    keys_list = [k for k in grouped_df.groups.keys()]

    # saving folds 1-10
    __saveFoldsRange__(1,11,keys_list, grouped_df, grouped_metadata, save_dir, "CV")

    # saving folds 101-110
    __saveFoldsRange__(101,111,keys_list, grouped_df, grouped_metadata, save_dir, "multiSource")

    # save the rest of the folds
    for foldID in keys_list[:]:
        # create the fold directory
        fold_path = os.path.join(save_dir,"fold{}".format(foldID))
        if not os.path.exists(fold_path):
            os.mkdir(fold_path)
        __saveFoldsRange__(foldID,foldID,keys_list, grouped_df, grouped_metadata, save_dir, "fold{}".format(foldID))


    audit_over_fail_threshold = createSummaryLog(proj_root)

    auditError = False
    if audit_over_fail_threshold:
        auditError = True
        print("***AUDIT_ERROR: Failed injection rate over 0.05***")
        print("See {} for audit details".format(
            os.path.join(proj_root, "logs","summary.log")))

    if ratioError:
        print("***RATIO_ERROR: badRecords/goodRecords > 0.0001; ratio: {}***".format(
            badRecordsRatio
        ))
    if testError:
        print("***TEST_ERROR: Bad feature table structure***")

    if auditError or ratioError or testError:
        return False

    print("PASSED: Test feature table completed")
    return True


# start inclusive
# end exclusive
def __saveFoldsRange__(start, end, keys_list, grouped_df, grouped_metadata, save_dir, custom_dir = None):
    sub_df = None
    sub_metadata = None
    if start==end:
        file_prefix = "fold{}".format(start)
        sub_df = grouped_df.get_group(start)
        sub_metadata = grouped_metadata.get_group(start)
    else:
        file_prefix = "folds{}-{}".format(start, end-1)
        for i in range(start, end):
            if i in keys_list:
                if isinstance(sub_df, pandas.DataFrame):
                    sub_df = pandas.concat([sub_df,grouped_df.get_group(i)], ignore_index = True)
                    sub_metadata = pandas.concat([sub_metadata,grouped_metadata.get_group(i)], ignore_index = True)
                else:
                    sub_df = grouped_df.get_group(i)
                    sub_metadata = grouped_metadata.get_group(i)
                keys_list.remove(i)

    if isinstance(sub_df, pandas.DataFrame):
        if custom_dir:
            fold_path = os.path.join(save_dir, custom_dir)
        else:
            fold_path = os.path.join(save_dir,file_prefix)
        if not os.path.exists(fold_path):
            os.mkdir(fold_path)

        # any feature labeled as Features.Joint.Joint2 should NOT be put in the same files.
        # They need to be put in a separate .csv file in the same directory
        # extracting out Features.Joint.Joint2
        secondary_col = [column for column in sub_df if column.startswith("Features.Joint.Joint2")]
        sec_sub_df = None
        if secondary_col:
            secondary_col.insert(0, "SegmentInfo.FoldID")
            sec_sub_df = sub_df[secondary_col].copy()

        # get rid of Features.Joint.Joint2 column from sub df
        sub_df =  sub_df[[
            column for column in sub_df if not "Features.Joint.Joint2" in column
        ]].copy()

        sub_df.to_csv(
            os.path.join(fold_path, "{}_processed.csv".format(file_prefix)),
            header=True,
            index=False
        )
        sub_metadata.to_csv(
            os.path.join(fold_path, "{}_metadata.csv".format(file_prefix)),
            header=True,
            index=False
        )
        if isinstance(sec_sub_df, pandas.DataFrame):
            sec_sub_df.to_csv(
                os.path.join(fold_path, "secondary.csv"),
                header=True,
                index=False
            )
        sub_df = None

# return a list of audit files that has a failure rate of over 5%
def createSummaryLog(proj_root):
    # collect and process proj_root/logs/featureBuilderLogs/*.audit files into
    # a summary.log file
    print("Generating summary log...")
    sum_log_dict = {}
    FAIL_INDEX = 0
    SUCCESS_INDEX = 1
    FAIL_THRESHOLD = 0.05

    for root, dirs, files in os.walk(
            os.path.join(proj_root,"logs","featureBuilderLogs")
        ):

        for file in files:
            if ".audit" in file:
                key = file.replace("fold.",'')
                key = key.replace(".audit",'')
                sum_log_dict[key] = [0]*2
                with open(os.path.join(root,file),'r') as f:
                    for line in f:
                        if "fail" in line.lower():
                            sum_log_dict[key][FAIL_INDEX] = int(line.split(':')[1].strip())
                        else: # success
                            sum_log_dict[key][SUCCESS_INDEX] = int(line.split(':')[1].strip())

    # grab the keys and put them in order
    Keys_info = __createKeysInfo__(list(sum_log_dict.keys()))

    # parsed the audit data
    audit_over_fail_threshold = []
    summary_data = []
    fail_count = 0
    success_count = 0
    net_fail_count = 0
    net_success_count = 0
    grand_fail_count = 0
    grand_success_count = 0
    stars = "".join(['*']*80) + '\n\n'
    dashes = "".join(['-']*80) + '\n\n'

    # first is training info
    summary_data.append(stars)

    for i in range(0, 4):
        info_list = Keys_info[i]

        # summary title
        summary_data.append("{}\n\n".format(info_list[0]))

        # summary data
        for key in info_list[1]:
           fail = sum_log_dict[key][FAIL_INDEX]
           success = sum_log_dict[key][SUCCESS_INDEX]
           fail_rate = fail / (success+fail)

           what_type = (key.split('.')[1]).replace('_',' ')
           summary_data.append("{} failed: {}\n".format(
                what_type, fail))

           if fail_rate > FAIL_THRESHOLD:
               summary_data.append("{} succeed: {}\n".format(
                    what_type,success))
               summary_data.append("WARNING: Failure rate: {}\n".format(fail_rate))
               summary_data.append("Audit Filename: fold.{}.audit\n\n".format(key))
               audit_over_fail_threshold.append("fold.{}.audit".format(key))
           else:
               summary_data.append("{} succeed: {}\n\n".format(
                    what_type,success))
           fail_count += fail
           success_count += success

        summary_data.append("{} {}\n".format(
                info_list[0].replace("Summary","Total Failed"),
                fail_count
                ))
        summary_data.append("{} {}\n\n".format(
                info_list[0].replace("Summary","Total Succeed"),
                success_count
                ))
        summary_data.append(dashes)

        net_fail_count += fail_count
        fail_count = 0
        net_success_count += success_count
        success_count = 0

        if i == 1:
            summary_data.append("Total Training Failed: {}\n".format(net_fail_count))
            summary_data.append("Total Trianing Succeed: {}\n\n".format(net_success_count))
            grand_fail_count += net_fail_count
            grand_success_count += net_success_count
            net_fail_count = 0
            net_success_count = 0
            summary_data.append(stars)
        elif i == 3:
            # testing net fail and success counts
            summary_data.append("Total Testing Failed: {}\n".format(net_fail_count))
            summary_data.append("Total Testing Succeed: {}\n\n".format(net_success_count))
            grand_fail_count += net_fail_count
            grand_success_count += net_success_count
            net_fail_count = 0
            net_success_count = 0
            summary_data.append(stars)

    # final summary
    summary_data.append("Grand total failed: {}\n".format(grand_fail_count))
    summary_data.append("Grand total succeed: {}\n".format(grand_success_count))
    summary_data.append("Fail ratio: {}\n".format(
            grand_fail_count/(grand_fail_count + grand_success_count)))
    summary_data.append("Success ratio: {}\n".format(
            grand_success_count/(grand_fail_count + grand_success_count)))

    with open(os.path.join(proj_root, "logs","summary.log"),'w') as f:
        f.write("".join(summary_data))

    return audit_over_fail_threshold

# helper methods to organized summary log keys
def __createKeysInfo__(keys):
    train_stand_list = []
    train_mix_list = []
    test_stand_list = []
    test_mix_list = []

    for key in keys:
        if "test" in key:
            if "mixed" in key:
                test_mix_list.append(key)
            else:
                test_stand_list.append(key)
        else: # training
            if "mixed" in key:
                train_mix_list.append(key)
            else:
                train_stand_list.append(key)

    keys = []

    keys.append(["Standard Training Summary:", train_stand_list])
    keys.append(["Mixed-Source Training Summary:",train_mix_list])
    keys.append(["Standard Testing Summary:",test_stand_list])
    keys.append(["Mixed-Source Testing Summary:",test_mix_list])
    return keys



if __name__ == '__main__':
    args = parse_args()

    is_passing = testFeatureTable(
        args.featureTableDBPath,
        args.foldId,
        args.rpmId,
        args.dataSourceId,
        args.verbose,
        args.dpi
    )

    # logic for CI
    returnCode = 0
    if not is_passing:
        returnCode = -1

    sys.exit(returnCode)




#
# Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
# 
# OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
# 
# This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
# between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
# See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
#