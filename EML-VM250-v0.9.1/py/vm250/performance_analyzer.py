#
# Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
# 
# OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
# 
# This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
# between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
# See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
#

import os
import sys
import shutil
import argparse
import inspect
import pandas
import matplotlib
matplotlib.use('Agg')   # non-interactive backend
import matplotlib.pyplot as plt
plt.rcParams.update({'figure.max_open_warning': 0})
plt.rc('legend', fontsize=7)

parent_dir = os.path.dirname(
    os.path.dirname(os.path.abspath(inspect.getfile(inspect.currentframe())))
)
sys.path.insert(0,parent_dir)

from utilities.two_class_process_insp import two_class_process_insp
from utilities.analyze_alarms import analyze_alarms
from utilities.confusionMatrix import confusionMatrix
from utilities.utilities import mkdir
from utilities.utilities import projErnie4Dir
from analysisFromCsv import analysisFromCsv


# print to cmd
def print(*args, **kwargs):
    import builtins
    if 'flush' not in kwargs: kwargs['flush'] = True
    builtins.print(*args, **kwargs)


def performanceAnalyzer(project, model = None, mandatoryInspectionRate = 0.000525, drawCurves = False):
    """
    Generate ROC plots using ERNIE decision logic
    """
    projectDir = os.path.abspath(project )
    FTdir = os.path.join( projectDir, 'featureTable' )
    finalModelDir = os.path.join( projectDir, 'finalModel' )
    modelDir = os.path.join( projectDir, 'modelBuilding' )
    mcPath = os.path.join(finalModelDir, 'SSL_allmodel.txt')
    if model is not None:
        mcPath = model
    metricsPath = os.path.join(projErnie4Dir(),'config','CMU_metric.csv')
    binDir = os.path.join(projErnie4Dir(), "bin" )

    # create folder to store data needed for gen result plots
    for fold in [
            "NonEmitting", "NORM", "Medical","Industrial","Fissile","Contamination"
        ]:
        mkdir(os.path.join(modelDir, "fold{}".format(fold)))

    featureTable = pandas.read_pickle( os.path.join(FTdir, 'test-vm250.pickle') )

    folds = ["NonEmitting", "NORM", "Medical","Industrial","Fissile","Contamination",20,32,33,34]

    featureColumns = ['SegmentInfo.CBPID'] + [c for c in featureTable.columns if c.startswith('Feature')]
    resultColumns = [c for c in featureTable.columns if c.startswith('SegmentInfo')
            or c.startswith('Classification') or c.startswith('Alarm')]

    # Run AnalysisUtility on each fold, then create summary with columns Investigate/Release/Label
    missingFolds = []
    for fold in folds:

        if str(fold).isdigit():
            rowMask = featureTable['SegmentInfo.FoldID'] == fold
        else:
            rowMask = (featureTable['SegmentInfo.FoldID'].isin(range(1,11)) &
                    featureTable["Classification.Source.{}".format(fold)] == 1)

        if not any(rowMask):
            missingFolds.append(fold)
            continue

        print("Analyzing Fold {}".format(fold))
        featuresDF = featureTable.loc[ rowMask, featureColumns ].copy().reset_index()
        resultsDF = featureTable.loc[ rowMask, resultColumns ].copy().reset_index()

        # massage resultsDF to look like analysisUtility output, including columns for handling fallback
        resultsDF['Fallback'] = resultsDF['Classification.IrregularScan'].astype(int)
        for column in ('RPMInvestigate', 'InvestigateScore', 'Investigate'):
            resultsDF[column] = resultsDF['Classification.Resolution.Alarm'].astype(int)

        nSigmas = [c for c in featuresDF.columns if c.startswith('Features.NSigma')]
        maxNSigma = featuresDF[nSigmas].max(axis=1)
        resultsDF['maxNSigma'] = maxNSigma
        resultsDF['grossCounts'] = featuresDF['Features.Standard.Gross.All']
        resultsDF['vehicleLength'] = featuresDF['Features.Vehicle.Length']

        # fill in a few other required columns with dummy values:
        for column in ('vehicleClass','Fallback Reason','RPM_Date_Time'):
            resultsDF[column] = -1

        analysisResults = analysisFromCsv( featuresDF, resultsDF,
                classifier=mcPath, metrics=metricsPath )

        output = os.path.join(modelDir, 'fold{}'.format(fold), 'predictions.csv')
        analysisResults.to_csv(output, index=False, float_format="%.15g")

        # save feature table info for result plotting
        ft_output = os.path.join(modelDir, 'fold{}'.format(fold), 'peak.intensity.csv')
        featuresDF[['SegmentInfo.CBPID','Features.Statistics.Peak.Intensity']].to_csv(
            ft_output,
            header=True,
            index=False,
            float_format="%.15g")

        # remove training
        if str(fold).isalpha():
            missingFolds.append(fold)
            continue

        # create summary file to simplify ROC plot generation, excluding fallback:
        label = 'Investigate'
        if fold == 20: label = 'Unlabeled'
        fallback = analysisResults['Fallback'] != 0
        summary = pandas.DataFrame({
                'Investigate': analysisResults[~fallback]['InvestigateScore'],
                'Release': 1-analysisResults[~fallback]['InvestigateScore'],
                'Label': label
                })
        summary.to_csv(output+'.data', index=False, float_format="%.15g")

    __resultPlot(projectDir, modelDir)

    for fold in missingFolds:
        folds.remove(fold)

    # Make ROC plots for folds 32 - 44:
    print('')
    os.chdir( modelDir )

    print("Compute NSigma investigate rates:")
    try:
        from vm250 import NSigmaScores
        NSigmaScores.NSigmaScores( featureTable, folds, settings = [6,10] )
    except:
        print("Non-fatal error: NSigma scoring failed, so points will be missing from ROC plots!")

    cmuExe = None
    if "nt" in os.name:
        cmuExe = os.path.abspath(binDir+'/llnl_canes_ml_core.exe')
    else:
        cmuExe = os.path.abspath(binDir+'/llnl_canes_ml_core')

    baselinePredictions = os.path.join( 'fold20', 'predictions.csv.data' )
    baseline_iter1 = os.path.join( 'allSSL', 'iteration_1_20_predictions.csv.data' )
    baselineFinal = os.path.join( 'fold20', 'final_predictions.csv.data' )

    output_dir = os.path.join(projectDir, "plots", "ROC")
    mkdir(output_dir)

    for fold in folds[1:]:
        print("Generating ROC for fold%d" % fold)
        predictions = os.path.join('fold%d' % fold, 'predictions.csv.data')
        if not os.path.exists(predictions):
            print("  no data found for fold%d!" % fold)
            continue

        os.system('%s option insp ds %s baseline %s'  % (cmuExe, predictions, baselinePredictions ) )
        shutil.move('thresholds.csv', '%dSSL_th.csv' % fold)
        two_class_process_insp( 'A1_roc' )

        iter1 = os.path.join('allSSL', 'iteration_1_%d_predictions.csv.data' % fold)
        os.system('%s option insp ds %s baseline %s'  % (cmuExe, iter1, baseline_iter1) )
        shutil.move('A1_roc.csv', 'A2_roc.csv')
        two_class_process_insp( 'A2_roc' )

        final = os.path.join('fold%d' % fold, 'final_predictions.csv.data')
        os.system('%s option insp ds %s baseline %s' % (cmuExe, final, baselineFinal) )
        shutil.move('A1_roc.csv', 'A3_roc.csv')
        two_class_process_insp( 'A3_roc' )

        analyze_alarms(
                fold,
                {'cmuExe':cmuExe},
                secondSource=False,
                output_dir=output_dir,
                mandatoryInspectionRate=mandatoryInspectionRate,
                drawCurves=drawCurves
        )

    # construct confusion matrices:
    print("generate confusion matrices:")
    predictions = [os.path.join('fold%d' % f, 'final_predictions.csv') for f in (34,33,32)]
    if all([os.path.exists(f) for f in predictions]):
        confusionMatrix( predictions, outputfile = 'folds323334_confusion_matrix.csv' )

    predictions = [os.path.join('fold%d' % f, 'final_predictions.csv') for f in (44,43,42)]
    if all([os.path.exists(f) for f in predictions]):
        confusionMatrix( predictions, outputfile = 'folds424344_confusion_matrix.csv' )

    confusionMatrix( [os.path.join('CV','final_predictions.csv')], outputfile = 'folds1-10_confusion_matrix.csv' )



def __resultPlot(projectDir, modelDir):
    from common.sourceColors import colors

    marker_list = [
        "o",
        "v",
        "X",
        "D",
        "<",
        "s",
        "P",
        "*",
        "^",
        "2",
        ">",
        "3",
        "4",
        "|",
        "1",
        "_"
        ]

    class Group():
        def __init__(self, foldList, colorList, labelList, makerList, groupName):
            self.foldList = foldList
            self.colorList = colorList
            self.labelList = labelList
            self.makerList = makerList
            self.groupName = groupName

    class Data_info():
        def __init__(self, ft_df, inv_df, color, label, marker, markersize, alpha):
            self.ft_df = ft_df
            self.inv_df = inv_df
            self.color = color
            self.label = label
            self.marker = marker
            self.alpha = alpha
            self.markersize = markersize

    group_list = []
    # 32 med, 33 industrial, 34 Fissile
    group_list.append(
        Group(
            [20,32,33,34],
            [colors["SoC"], colors["Medical"],  colors["Industrial"],  colors["Fissile"]],
            ["SoC", "Medical Test", "Industrial Test", "Fissile Test"],
            [marker_list[0], marker_list[1],marker_list[2],marker_list[3]],
            "Folds20_32-34"
        )
    )

    # folds 1-10
    group_list.append(
        Group(
            ["NonEmitting", "NORM", "Medical","Industrial","Fissile","Contamination"],
            [colors["NonEmitting"], colors["NORM"],  colors["Medical"],colors["Industrial"],colors["Fissile"],colors["Contamination"]],
            ["NonEmitting", "NORM", "Medical","Industrial","Fissile","Contamination"],
            [marker_list[0], marker_list[1],marker_list[2],marker_list[3],marker_list[4],marker_list[5]],
            "Folds1-10"
        )
    )

    step = 1
    yRange = (1e-3,1e+6)
    xRange = (-0.05, 1.1)
    output_dir = os.path.join(projectDir, "plots", "resultPlots")
    mkdir(output_dir)

    for group in group_list:
        dataInfoList = []

        for i in range(0, len(group.foldList)):
            fold = group.foldList[i]
            color = group.colorList[i]
            label = group.labelList[i]
            #marker = group.makerList[i]
            marker = '.'
            markersize = 1
            if "20" in str(fold):
                alpha = 0.2
            else:
                alpha = 0.5
            dataInfoList.append(Data_info(
                    pandas.read_csv(os.path.join(modelDir, 'fold{}'.format(fold), 'peak.intensity.csv')),     # ft
                    pandas.read_csv(os.path.join(modelDir, 'fold{}'.format(fold), 'predictions.csv')),  # analysis prediction
                    color,
                    label,
                    marker,
                    markersize,
                    alpha
                ))

        fig, ax = plt.subplots()

        for dataInfo in dataInfoList:
            ax.semilogy(
                    dataInfo.inv_df["InvestigateScore"][::step].to_list(),
                    dataInfo.ft_df['Features.Statistics.Peak.Intensity'][::step].to_list(),
                    color = dataInfo.color, marker = dataInfo.marker, markersize=dataInfo.markersize,
                    linestyle='', alpha=dataInfo.alpha, label=dataInfo.label )

        title = group.groupName.replace('_', ',')
        ax.set_title("{} Result Plot".format(title))
        ax.set_xlabel("Investigate Score")
        ax.set_ylabel("Peak.Intensity")
        ax.set_xlim(*xRange)
        ax.set_ylim(*yRange)
        lgnd = ax.legend(numpoints=1)
        for handle in lgnd.legendHandles: handle._legmarker.set_markersize(6)

        plt.savefig( os.path.join( output_dir, "resultPlot_folds{}.png".format(group.groupName)), dpi=200 )

        plt.close( fig )


if __name__ == '__main__':
    parser = argparse.ArgumentParser(
        description="Create ROC plots and confusion matrices to summarize classifier performance",
        usage="python performance_analyzer.py [-h] project_dir [--mandatoryInspectionRate RATE]"
    )
    parser.add_argument("project", help="path to project directory")
    parser.add_argument("--model", type=str, help="RF classifier to test. Defaults to $project/finalModel/SSL_allmodel.txt")
    parser.add_argument("--mandatoryInspectionRate", type=float, default=None, help="red line on ROC plots")
    parser.add_argument("--drawCurves", action="store_true", help="Add curves to NSigma alarm points")
    args = parser.parse_args()

    performanceAnalyzer(
        args.project,
        model = args.model,
        mandatoryInspectionRate = args.mandatoryInspectionRate,
        drawCurves = args.drawCurves
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