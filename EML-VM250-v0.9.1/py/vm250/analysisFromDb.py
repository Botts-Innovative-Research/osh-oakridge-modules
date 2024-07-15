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

# start Java context which also check for ERNIE_HOME env variable
from utilities import startJPype
import jpype

from utilities.utilities import projErnie4Dir

import java.lang
from java.nio.file import Paths,Path
from java.time import Instant, LocalDateTime, ZoneOffset

from gov.llnl.utility.xml.bind import DocumentReader
from gov.llnl.ernie import Analysis
from gov.llnl.ernie.classifier import Classifier
from gov.llnl.ernie.io import RecordDatabase
from gov.llnl.ernie.vm250 import ErnieVM250Package

# on error return False
# else return True
def analysisFromDb(
        recordList,
        siteId,
        laneId,
        classifier = None,
        metrics = None,
        verbose = False,
        outputDir = None
    ):

    print("Loading records IDs and connecting to the database...")

    with open(recordList) as fin:
        recordIds = map(int, fin.read().split())

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
            analysis.setClassifier( Classifier(classifier, metrics, verbose) )

        databaseReader=DocumentReader.create(RecordDatabase)
        databaseReader.setProperty(DocumentReader.SEARCH_PATHS, searchPaths)
        database=databaseReader.loadFile(Paths.get(
            os.path.join(config_path,"vm250RecordDatabase.xml")
        ))
    except jpype.JException as ex:
        print(ex.stacktrace())
        return False


    # dictionary for analysis results:
    results = {
        'SegmentInfo.CBPID':[],
        'SegmentInfo.Location.DataSourceId': siteId,
        'SegmentInfo.Location.RpmId': laneId,
        'RPMInvestigate':[],
        'Alarm.Neutron':[],
        'versionID':str(ErnieVM250Package.getVersion()),
        'modelID':str(analysis.getClassifier().getModelName()),
        'primaryThreshold':analysis.getThreshold(),
        'vehicleClass':[],
        'vehicleLength':[],
        'maxNSigma':0,
        'grossCounts':0,
        'InvestigateScore':[],
        'Investigate':[],
        'Fallback':[],
        'Fallback Reason':[]
    }
    for sourceID in ('1','2','Overall'):
        for subset in ('NonEmitting','NORM','MED','IND','FIS','Contam','XLocation1','XLocation2','YLocation',
                'ZLocation'):
            results["Source" + sourceID + "." + subset] = []
    results['RPM_Date_Time'] = []

    # dictionary for feature table:
    features = {'SegmentInfo.CBPID':[]}
    for key in map(str, analysis.getClassifier().getFeatureNames()):
        if key.startswith('Features'):
            features[key] = []

    print("Querying data and processing it...")

    for idx, occId in enumerate(recordIds):
        try:
            record = database.getRecord(occId)
        except Exception as ex:
            print("Failed to fetch record %d" % occId)
            print(ex)
            continue

        try:
            analysis.prepare(record)
            result = analysis.processRecord(record)
        except jpype.JException as ex:
            print("Analysis failed for record %d" % occId)
            print(ex.stacktrace())
            #continue

        # populate results table
        results['SegmentInfo.CBPID'].append(record.getContextualInfo().getScanID())

        obj = record.getVendorAnalysis()
        if obj:
            gammaAlarm = obj.isGammaAlarm()
            neutronAlarm = obj.isNeutronAlarm()
        else:
            gammaAlarm = 0
            neutronAlarm = 0
        results['RPMInvestigate'].append(int(gammaAlarm or neutronAlarm))
        results['Alarm.Neutron'].append(int(neutronAlarm))

        try:
            obj = record.getVehicleInfo()
            results['vehicleClass'].append(obj.getId())
            results['vehicleLength'].append(obj.getVehicleLength())
        except:
            print("***WARNING: No Vehicle info for Occupancy ID: {}***".format(
                occId
            ))
            results['vehicleClass'].append(0)
            results['vehicleLength'].append(0)

        # skip max ratio / gross counts, computed later from feature table

        # varaibles to test for weird condition
        is_fall_back = result.isFallback()
        fallback_reason = ''
        if is_fall_back:
            fallback_reason = ','.join(map(str, record.getBadReasons()))
        num_of_sources = result.getNumberOfSources()

        results['InvestigateScore'].append( result.getProbabilityInvestigate() )
        results['Investigate'].append(
                int(result.getRecommendedAction() == analysis.RecommendedAction.INVESTIGATE))
        results['Fallback'].append(int(is_fall_back))
        results['Fallback Reason'].append(fallback_reason)

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

        # check for weird conditions
        if (is_fall_back and num_of_sources) or \
                (is_fall_back == False and num_of_sources == 0):
            print(
                "***WARNING: isFallback: {} but Number of sources: {} @ "
                "Occupancy ID: {}***".format(is_fall_back,num_of_sources,occId)
            )

        ldt = LocalDateTime.ofInstant(record.getContextualInfo().getTimestamp(), ZoneOffset.UTC)
        results['RPM_Date_Time'].append( str(record.getContextualInfo().getTimestamp().toString()) )

        # also populate feature table
        features['SegmentInfo.CBPID'].append(record.getContextualInfo().getScanID())
        features_list = result.getFeatures()
        if features_list:
            for featureSet in features_list:
                for entry in featureSet.toMap().entrySet():
                    key = 'Features.%s' % (entry.getKey())
                    if key in features:
                        features[key].append( entry.getValue() )
        else:
            print("***WARNING: No features for Occupancy ID: {}***".format(occId))
            for key, value in features.items():
                if 'SegmentInfo.CBPID' in key:
                    continue
                value.append(0)

    print("Saving outputs to csv files...")

    df1 = pandas.DataFrame(data=results)
    df1.index.name = 'ID'

    df2 = pandas.DataFrame(data=features)

    nSigmas = [c for c in df2.columns if c.startswith('Features.NSigma')]
    maxNSigma = df2[nSigmas].max(axis=1)
    df1['maxNSigma'] = maxNSigma
    df1['grossCounts'] = df2['Features.Standard.Gross.All']

    saved_path = outputDir
    if saved_path:
        # make sure it exist if not create it
        if not os.path.exists(saved_path):
            os.mkdir(saved_path)
    else:
        # save locally
        saved_path = PyPath(recordList).parent

    df1.to_csv(
        os.path.join(saved_path, "analysisUtilityOutput.csv"), index=False, float_format="%.15g"
    )
    df2.to_csv(
        os.path.join(saved_path, "analysisUtility_FT.csv"), index=False
    )

    print("Analyzing data from the database completed")
    return True

if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument("recordList", help="File containing a list of CBPids to analyze")
    parser.add_argument("siteId", type=int, help="Integer site id")
    parser.add_argument("laneId", type=int, help="Integer lane id")
    parser.add_argument("--classifier", help="Main classifier file")
    parser.add_argument("--metrics", help="Metrics file, required along with classifier")
    parser.add_argument("--verbose", action="store_true", help="Be verbose")
    parser.add_argument("-o", "--outputDir", type=str, help="Directory to save outputs to")

    args = parser.parse_args()

    analysisFromDb(
        args.recordList,
        args.siteId,
        args.laneId,
        args.classifier,
        args.metrics,
        args.verbose,
        args.outputDir
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
