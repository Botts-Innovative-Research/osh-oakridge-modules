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
import glob
import re
import argparse
from datetime import date, datetime
import pandas
from pathlib import Path as PyPath
import inspect
parent_dir = os.path.dirname(
    os.path.dirname(os.path.abspath(inspect.getfile(inspect.currentframe())))
)
sys.path.insert(0,parent_dir)

from utilities import utilities

# start Java context
from utilities import startJPype
import jpype

import java.lang
from java.nio.file import Paths,Path
from java.time import Instant, LocalDate, ZoneId

from gov.llnl.utility.xml.bind import DocumentReader
from gov.llnl.ernie import Analysis
from gov.llnl.ernie.classifier import Classifier
from gov.llnl.ernie.io import RecordDatabase
from gov.llnl.ernie.vm250 import ErnieVM250Package

from gov.llnl.ernie.api import DailyFileLoader
from vm250.readDailyFiles import VM250_daily, daily_file_occupancy_to_record

def analysisFromDailyFile(
        dailyFile,
        analysis,
        verbose = False,
        date_string = None,
        lane_width = None,
        log = sys.stdout
    ):

    if date_string:
        Date=datetime.strptime(date_string, '%Y-%m-%d')
        javaDate=LocalDate.parse(date_string)
    else:
        Date=datetime.combine(date.today(), datetime.min.time())
        javaDate=LocalDate.parse(str(date.today()))
    # Don't know actual time zone, so pretend we're in UTC
    javaInstant = javaDate.atStartOfDay(ZoneId.of("UTC")).toInstant()

    dailyFileLoader = DailyFileLoader()
    try:
        recordList = dailyFileLoader.load( dailyFile, javaInstant, os.path.join("config","vm250LaneDatabase.xml"))
    except jpype.JException as ex:
        print("Error while loading daily file:")
        print(ex.stacktrace())
        raise
    results = {
        'SegmentInfo.ID':[],
        'RPM_Date_Time':[],
        'RPMInvestigate':[],
        'Alarm.Neutron':[],
        'versionID':str(ErnieVM250Package.getVersion()),
        'modelID':str(analysis.getClassifier().getModelName()),
        'vehicleClass':[],
        'velocity':[],
        'vehicleLength':[],
        'maxNSigma':0,
        'grossCounts':0,
        'InvestigateScore':[],
        'Fallback':[],
        'Fallback Reason':[]
    }

    # dictionary for feature table:
    features = {'SegmentInfo.ID':[]}
    for key in map(str, analysis.getClassifier().getFeatureNames()):
        if key.startswith('Features'):
            features[key] = []

    for idx, record in enumerate(recordList):
        if lane_width:
            record.getLane().setWidth(lane_width)

        result = None
        try:
            analysis.prepare(record)
            result = analysis.processRecord(record)
        except Exception as ex:
            print("Analysis failed for record %d" % idx, file=log)
            print(ex.stacktrace(), file=log)


        # populate results table
        results['SegmentInfo.ID'].append(idx)
        results['RPM_Date_Time'].append( str(record.getContextualInfo().getTimestamp().toString()) )

        obj = record.getVendorAnalysis()
        if obj:
            gammaAlarm = obj.isGammaAlarm()
            neutronAlarm = obj.isNeutronAlarm()
        else:
            print("***WARNING: missing vendor analysis for data with timestamp: {}***"
                    .format(record.getContextualInfo().getTimestamp()), file=log)
            gammaAlarm = 0
            neutronAlarm = 0
        results['RPMInvestigate'].append(int(gammaAlarm or neutronAlarm))
        results['Alarm.Neutron'].append(int(neutronAlarm))

        try:
            obj = record.getVehicleInfo()
            results['vehicleClass'].append(obj.getId())
            results['vehicleLength'].append(obj.getVehicleLength())
        except:
            print("***WARNING: No Vehicle info for data with timestamp: {}***"
                    .format(record.getContextualInfo().getTimestamp()), file=log)
            results['vehicleClass'].append(0)
            results['vehicleLength'].append(0)

        try:
            results['velocity'].append(record.getVehicleMotion().getVelocityInitial())
        except:
            print("***WARNING: No velocity info for data with timestamp: {}***"
                    .format(record.getContextualInfo().getTimestamp()), file=log)
            results['velocity'].append(0)

        # skip max nSigma / gross counts, they are computed later from feature table

        if result:
            is_fall_back = result.isFallback()
            investigateScore = result.getProbabilityInvestigate()
            num_of_sources = result.getNumberOfSources()
        else:
            is_fall_back = True
            investigateScore = 1 if (gammaAlarm or neutronAlarm) else 0
            num_of_sources = 0

        fallback_reason = ''
        if is_fall_back:
            fallback_reason = ', '.join(map(str, record.getBadReasons()))

        results['InvestigateScore'].append( investigateScore )
        results['Fallback'].append(int(is_fall_back))
        results['Fallback Reason'].append(fallback_reason)


        # also populate feature table
        features['SegmentInfo.ID'].append(idx)
        if result:
            features_list = result.getFeatures()
            if not features_list:
                for key in features:
                    if key == 'SegmentInfo.ID': continue
                    features[key].append(0)
            for featureSet in features_list:
                 for entry in featureSet.toMap().entrySet():
                    key = 'Features.%s' % (entry.getKey())
                    if key in features:
                        features[key].append( entry.getValue() )
        else:
            print("***WARNING: No features for data with timestamp: {}***"
                    .format(record.getContextualInfo().getTimestamp()), file=log)
            for key, value in features.items():
                if 'SegmentInfo.ID' in key:
                    continue
                value.append(0)

    df1 = pandas.DataFrame(data=results)
    df1.index.name = 'ID'

    df2 = pandas.DataFrame(data=features)

    # add maxNSigma and Standard.Gross.All to output:
    nSigmas = [c for c in df2.columns if c.startswith('Features.NSigma')]
    maxNSigma = df2[nSigmas].max(axis=1)
    df1['maxNSigma'] = maxNSigma
    df1['grossCounts'] = df2['Features.Standard.Gross.All']

    return df1, df2


if __name__ == '__main__':
    print("ERNIE 4 VM250 Daily file processor ")
    parser = argparse.ArgumentParser()
    parser.add_argument("dailyFiles", type=str, nargs="+", help="VM250 daily file(s) to analyze")
    parser.add_argument("--classifier", help="Main classifier file")
    parser.add_argument("--verbose", action="store_true", help="Be verbose")
    parser.add_argument("-o", "--outputDir", type=str, help="Directory to save outputs")
    parser.add_argument("-d", "--date", type=str, help="Date in the form YYYY-MM-DD")
    parser.add_argument("--features", action="store_true", help="Write features file")
    parser.add_argument("--laneWidth", type=float, help="Supply lane width (in m)")

    args = parser.parse_args()

    saved_path = args.outputDir
    if saved_path:
        if not os.path.exists(saved_path):
            os.mkdir(saved_path)
    else:
        saved_path = '.'

    # CMD.exe and kin don't expand glob patterns
    dailyFiles = []
    for fstring in args.dailyFiles:
        dailyFiles.extend( glob.glob(fstring) )
    dailyFiles.sort()

    datePattern = re.compile('\d{4}-\d{2}-\d{2}')

    laneWidths = {}
    with open(os.path.join(utilities.projErnie4Dir(), "config", "vm250LaneConfigurations.csv")) as fin:
        for line in fin:
            if line.startswith('#'): continue
            siteLabel,siteNumber,laneNumber,laneWidth,collimated = line.strip().split(',')
            laneWidths.setdefault(siteLabel, {})[int(laneNumber)] = float(laneWidth)

    lanePattern = re.compile('_([A-Za-z])_L(\d{3})_')

    searchPaths=jpype.JArray(Path) ([
        Paths.get( os.path.join(utilities.projErnie4Dir(), "config") ),
        ])

    try:
        analysisReader=DocumentReader.create(Analysis)
        analysisReader.setProperty(DocumentReader.SEARCH_PATHS, searchPaths)
        analysis=analysisReader.loadFile(Paths.get(
            os.path.join(utilities.projErnie4Dir(), "config","vm250Analysis.xml")
            ))

        if args.classifier:
            metrics = os.path.join(utilities.projErnie4Dir(), "config","CMU_metric.csv")
            analysis.setClassifier( Classifier(args.classifier, metrics, args.verbose) )

    except jpype.JException as ex:
        print("Fatal error in configuration")
        print(ex.stacktrace())
        raise

    errors = []
    for dailyFile in dailyFiles:
        base, etc = os.path.splitext(os.path.basename(dailyFile))
        resultFile = os.path.join(saved_path, base+"-result.csv")
        featureFile = os.path.join(saved_path, base+"-features.csv")
        logFile = os.path.join(saved_path, base+"-log.txt")
        print("Processing %s"%dailyFile)

        date_ = args.date
        if not date_:
            matches = datePattern.findall(dailyFile)
            if matches:
                date_ = matches[-1]

        laneWidth = args.laneWidth
        if laneWidth is None:
            matches = lanePattern.findall(dailyFile)
            if matches:
                siteId, laneId = matches[0]
                try:
                    laneWidth = laneWidths[siteId][int(laneId)]
                except KeyError:
                    print("WARNING: no lane width found for site %s lane %s, using default width" % (siteId, laneId))

        with open(logFile,"w") as log:
            if args.verbose:
                print("Writing log to %s"%logFile)
            try:
                results, features = analysisFromDailyFile(
                    dailyFile,
                    analysis,
                    args.verbose,
                    date_string = date_,
                    lane_width = laneWidth,
                    log = log
                )
            except Exception as ex:
                errors.append(dailyFile)
                print(ex)


        if args.verbose:
            print("Writing results to %s"%resultFile)
        results.to_csv( resultFile, index=False, float_format="%.15g")

        if args.features:
            print("Writing features to %s"%featureFile)
            features.to_csv(featureFile, index=False)

    if errors:
        print("\n**** Failed to process %d daily file(s). Problem files:" % len(errors))
        for badFile in errors:
            print("  %s" % badFile)



#
# Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
# 
# OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
# 
# This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
# between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
# See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
#