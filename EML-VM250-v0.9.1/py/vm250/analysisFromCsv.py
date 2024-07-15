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
import argparse
import pandas
from pathlib import Path as PyPath

import inspect
parent_dir = os.path.dirname(
    os.path.dirname(os.path.abspath(inspect.getfile(inspect.currentframe())))
)
sys.path.insert(0,parent_dir)

from utilities import utilities

# start Java context which also check for ERNIE_HOME env variable
from utilities import startJPype
import jpype

from utilities.utilities import projErnie4Dir

import java.lang
from java.nio.file import Paths,Path
from java.time import Instant, LocalDateTime, ZoneOffset

from gov.llnl.utility.xml.bind import DocumentReader
from gov.llnl.ernie import Analysis
from gov.llnl.ernie.impl import AnalysisResultImpl
from gov.llnl.ernie.classifier import Classifier
from gov.llnl.ernie.vm250 import ErnieVM250Package
from gov.llnl.ernie.vm250.data import VM250AnalysisSource

# on error return False
# else return a dataframe
def analysisFromCsv(
        featuresDF,             # Analysis Utility Feature Table Dataframe
        originalResultsDF,      # Analysis Utility Ouptut Dataframe
        # next three arguments override default values in vm250Analysis.xml
        classifier = None,      # specify which RF classifier to use
        verbose = False
    ):
    """
    Apply ERNIE analysis to each row in a feature table (passed in as pandas dataframe)
    In addition to the feature table, this function requires an additional table containing
    information about fallback scans, etc.

    Returns another pandas dataframe containing analysis results
    """

    # some sanity checks:
    assert len(featuresDF) == len(originalResultsDF)
    assert all(featuresDF['SegmentInfo.ID'] == originalResultsDF['SegmentInfo.ID'])

    if verbose:
        print("Total rows loaded: {}".format(len(featuresDF)))

    config_path=os.path.join(projErnie4Dir(), "config")

    searchPaths=jpype.JArray(Path)([
        Paths.get(config_path)
    ])

    try:
        analysisReader=DocumentReader.create(Analysis)
        analysisReader.setProperty(DocumentReader.SEARCH_PATHS, searchPaths)
        analysis=analysisReader.loadFile(Paths.get(
            os.path.join(config_path,"vm250Analysis.xml")
        ))

        if classifier:
            metrics = os.path.join(utilities.projErnie4Dir(), "config","CMU_metric.csv")
            analysis.setClassifier(Classifier(classifier, metrics, verbose))

    except jpype.JException as ex:
        print(ex.stacktrace())
        return False

    classifierObj = analysis.getClassifier()

    # dictionary for analysis results:
    results = {
        'SegmentInfo.ID':-1,
        'RPM_Date_Time':-1,
        'RPMInvestigate': -1,
        'Alarm.Neutron': -1,
        'versionID':str(ErnieVM250Package.getVersion()),
        'modelID':str(classifierObj.getModelName()),
        'vehicleClass':-1,
        'velocity':-1,
        'vehicleLength':-1,
        'maxNSigma': -1,
        'grossCounts': -1,
        'InvestigateScore':[],
        'Fallback':0,
        'Fallback Reason':-1
    }
    """
    for sourceID in ('1','2','Overall'):
        for subset in ('NonEmitting','NORM','MED','IND','FIS','Contam','XLocation1','XLocation2','YLocation',
                'ZLocation'):
            results["Source" + sourceID + "." + subset] = []
    """


    featureList = [str(s) for s in classifierObj.getFeatureNames()]
    featureList[0] = 'SegmentInfo.ID'    # instead of SegmentInfo.FoldID
    featureTable = featuresDF[featureList].values

    if verbose:
        print("Running prediction and generating outputs...")

    for idx, features in enumerate(featureTable):

        # feed into the classifer
        CMU_result = classifierObj.predict(features);

        # wrap result in AnalysisResult
        result = AnalysisResultImpl()
        fwhm = featuresDF.at[idx, "Features.Extent.FWHM.In"]
        X = featuresDF.at[idx, "Features.Extent.XLocation"]
        Y = featuresDF.at[idx, "Features.Statistics.Peak.MeanY"]
        Z = featuresDF.at[idx, "Features.Statistics.Peak.MeanZ"]
        result.sourceFull = VM250AnalysisSource(classifierObj.getOutputLabels(),
                CMU_result.getClassLogLikelihoods(), X-fwhm/2, X+fwhm/2, Y, Z)
        result.addSource( result.sourceFull )
        result.setProbabilityInvestigate(result.sourceFull.getProbabilityInvestigate())
        result.setProbabilityRelease(result.sourceFull.getProbabilityRelease())

        results['InvestigateScore'].append( result.getProbabilityInvestigate() )

        """
        num_of_sources = result.getNumberOfSources()
        if num_of_sources:
            sources = [result.getSource(0), None, result.getFullSource()]
            if num_of_sources == 2:
                sources[1] = result.getSource(1)
        else:
            sources = [None, None, None]

        for sourceID, source in zip(('Source1','Source2','SourceOverall'),sources):
            if source:
                results[sourceID+'.NonEmitting'].append( source.getProbabilityNonEmitting() )
                results[sourceID+'.NORM'].append( source.getProbabilityNORM() )
                results[sourceID+'.MED'].append( source.getProbabilityMedical() )
                results[sourceID+'.IND'].append( source.getProbabilityIndustrial() )
                results[sourceID+'.FIS'].append( source.getProbabilityFissile() )
                results[sourceID+'.Contam'].append( source.getProbabilityContamination() )
                results[sourceID+'.XLocation1'].append( source.getPositionX1() )
                results[sourceID+'.XLocation2'].append( source.getPositionX2() )
                results[sourceID+'.YLocation'].append( source.getPositionY() )
                results[sourceID+'.ZLocation'].append( source.getPositionZ() )
            else:
                for description in ('NonEmitting','NORM','MED','IND','FIS','Contam',
                        'XLocation1','XLocation2','YLocation','ZLocation'):
                    results[sourceID + "." + description].append(0)
        """


    df1 = pandas.DataFrame(data=results)

    # fallback cases need special handling:
    fallbackMask = originalResultsDF['Fallback'] == 1
    for column in ('Fallback', 'InvestigateScore'):
        df1.loc[fallbackMask, column] = originalResultsDF.loc[fallbackMask][column]

    # discard classifier results when going to fallback:
    probabilities = [c for c in df1 if c.startswith('Source')]
    df1.loc[fallbackMask, probabilities] = 0

    # copy other details from the original results (CBPID, DataSourceId, original RPM decision, etc.):
    for key in results:
        if results[key] == -1:
            df1[key] = originalResultsDF[key]

    # Neutron alarms force an ERNIE investigate:
    neutronAlarm = df1['Alarm.Neutron'] == 1

    return df1


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "featuresCsvFile",
        help="The analysisUtility_FT.csv file; generated by the analysisFromDb.py script"
    )
    parser.add_argument(
        "outputCsvFile",
        help="The analysisUtilityOutput.csv file; generated by the analysisFromDb.py script"
    )
    parser.add_argument("--classifier", help="Main classifier file")
    parser.add_argument("--verbose", action="store_true", help="Be verbose")
    parser.add_argument("-o", "--outputDir", type=str, help="The directory to saved csv outputs too")

    args = parser.parse_args()

    results = analysisFromCsv(
        pandas.read_csv( args.featuresCsvFile ),
        pandas.read_csv( args.outputCsvFile ),
        classifier = args.classifier,
        verbose = args.verbose
    )

    if args.verbose:
        print("Saving outputs to csv files...")

    # write output to file
    saved_path = args.outputDir
    if saved_path:
        # make sure it exist if not create it
        if not os.path.exists(saved_path):
            os.mkdir(saved_path)
    else:
        # save locally
        saved_path = PyPath(args.featuresCsvFile).parent

    results.to_csv(os.path.join(saved_path, 'analysisUtilityOutput.csv'), index=False, float_format="%.15g")

    if args.verbose:
        print("Analyzing data from csv completed")



#
# Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
# 
# OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
# 
# This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
# between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
# See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
#