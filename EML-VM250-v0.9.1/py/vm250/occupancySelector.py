#
# Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
# 
# OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
# 
# This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
# between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
# See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
#

#!/usr/bin/env python3

import argparse
import numpy
import os

# switch to non-interactive backend
import matplotlib
matplotlib.use("Agg")
from matplotlib import pyplot as plt
from datetime import datetime
import shutil
import sys
import inspect
parent_dir = os.path.dirname(
    os.path.dirname(os.path.abspath(inspect.getfile(inspect.currentframe())))
)
sys.path.insert(0,parent_dir)

from utilities import utilities

# start Java context
from utilities import startJPype
import jpype

from java.nio.file import Paths, Path
from gov.llnl.utility.xml.bind import DocumentReader
from gov.llnl.ernie.vm250 import VM250Analysis as Analysis
from gov.llnl.ernie.vm250 import VM250RecordDatabase as Database
from gov.llnl.ernie.common import ExtentFeatureExtractor, GammaNSigmaFeatureExtractor


#*******************************************************************************
VM250_ANALYSIS_PATH = os.path.join(
    utilities.projErnie4Dir(),"config","vm250Analysis.xml"
)
VM250_RECORD_DB_PATH = os.path.join(
    utilities.projErnie4Dir(),"config","vm250RecordDatabase.xml"
)
#*******************************************************************************


# on error selectOccupancies returns False
# default values are test values
def selectOccupancies(
        project,
        siteId,
        nRecords = 10000,
        nFolds11_20 = 100000,
        nSigmaCutValue = 4.0,
        doCheck = True, # when script is run as stand-alone, do a check
    ):

    if doCheck:
        error = utilities.initialChecks(project, portalType='vm250')
        if error:
            return False

    searchPaths=jpype.JArray(Path) ([
        Paths.get(
            project
        ),
        Paths.get(
            os.path.join(utilities.projErnie4Dir(), "config")
        )
    ])

    reader = DocumentReader.create(Analysis)
    reader.setProperty(DocumentReader.SEARCH_PATHS, searchPaths)
    analysis = reader.loadFile(Paths.get(VM250_ANALYSIS_PATH))

    reader = DocumentReader.create(Database)
    reader.setProperty(DocumentReader.SEARCH_PATHS, searchPaths)
    dbc = reader.loadFile(Paths.get(VM250_RECORD_DB_PATH))

    extentExtractor = ExtentFeatureExtractor()
    extentExtractor.initialize()
    nsigmaExtractor = GammaNSigmaFeatureExtractor()
    nsigmaExtractor.initialize()

    # check for the correct number of port Ids in portIds.txt
    # 1.3 is a safety margin: some scans have errors that make them unsuitable
    # for use in nonemittingTesting, nonemittingTraining, etc
    # (although they can still be used in fold 20), so we need a slightly bigger
    # pool of records.
    print("Checking the number of port IDs in portIds.txt...")

    #min_records = int(((2 * nFolds11_20 + 4 * nRecords) * 1.3) + 0.5)
    min_records = int((2 * nFolds11_20 + 3 * nRecords) + 0.5)
    rids = [int(x) for x in numpy.loadtxt(os.path.join(project, "portIds.txt"))]
    rids_length = len(rids)
    if rids_length < min_records:
        print("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!")
        print("ERROR")
        print("Not enought port IDs for selecting occupancies to build a new feature table")
        print("nRecords: {}\nnFolds11_20: {}\n".format(nRecords, nFolds11_20))
        print("Minimum number of port IDs needed: {}".format(min_records))
        print("portIds.txt is short by: {}".format(min_records - rids_length))
        return False

    numpy.random.shuffle(rids)

    print("Starting the process of selecting occupancies...")

    results = []
    emitting_records = []
    max_counts = 2 * nRecords
    bad_record_counter = 100

    print("Generating emitting and non-emitting testing and training data...")
    index = 0

    while True:
        if index == rids_length:
            print("\n***POTENTIAL FATAL ERROR *** End of records reached")
            print("Total non-emitting records: {}".format(len(results)))
            print("Excpected total non-emitting records: {}".format(max_counts))
            print("Total emitting records: {}\n".format(len(emitting_records)))
            # Because the algorithm break here, the script will
            # error out with the next operation. the whole list of records is
            # eaten up which mean something went wrong which caused most of
            # the records to be emitters
            break

        rid = rids[index]
        index += 1

        try:
            record = dbc.getRecord(rid)
            analysis.prepare(record)
            if record.bad(): continue
            if record.getInternal().getVelocities().isEmpty(): continue

            etf = extentExtractor.compute(record).innerWidths[1]
            ntf = nsigmaExtractor.compute(record).getMaxAllPanels()
            vendorAnalysis = record.getVendorAnalysis()

            # emitting records
            if ntf > nSigmaCutValue:
                emitting_records.append([
                    rid,                                    # recordID
                    etf,                                    # FWHM
                    ntf,                                    # MaxNSigma
                    int(vendorAnalysis.isGammaAlarm()),     # GammaAlarm
                    int(vendorAnalysis.isNeutronAlarm())    # NeutronAlarm
                ])
                continue

            # non-emitting records
            results.append([
                rid,                                    # recordID
                etf,                                    # FWHM
                ntf,                                    # MaxNSigma
                int(vendorAnalysis.isGammaAlarm()),     # GammaAlarm
                int(vendorAnalysis.isNeutronAlarm())    # NeutronAlarm
            ])
            count = len(results)

            if not (count % 5000):
                print("Completed %d" % count)
            if count == max_counts:
                break
        except Exception as ex:
            bad_record_counter -= 1
            if not bad_record_counter:
                print("***ERROR*** Too many consecutive bad records")
                print("Assigned the correct database name to defaultDatabase in vm250Queries.txt")
                return False
            #print(ex)
            continue

        # record is good reset
        bad_record_counter = 100

    # checking for low number of emitter
    if len(emitting_records) < (nRecords * 0.05):
        print("***WARNING*** Few emitting records found: {} records".format(
            len(emitting_records)
        ))

    print("Saving data to {}".format(
        os.path.join(project,"featureTable", "inputs")
    ))

    # write emitting output to file
    file_path = os.path.join(
        project,
        "featureTable/inputs",
        "emittingTesting.txt"
    )
    with open(file_path,'w') as file:
        for line in emitting_records:
            file.write("{}\n".format(line[0]))

    # write non-emitting outputs to files
    half = len(results)//2
    file_path = os.path.join(
        project,
        "featureTable/inputs",
        "nonemittingTesting.txt"
    )
    with open(file_path,'w') as file:
        for line in results[:half]:
            file.write("{}\n".format(line[0]))

    file_path = os.path.join(
        project,
        "featureTable/inputs",
        "nonemittingTraining.txt"
    )
    with open(file_path,'w') as file:
        for line in results[half:]:
            file.write("{}\n".format(line[0]))

    # copy nonemittingTraining.txt as nonemittingTrainingWithPayload.txt
    shutil.copy2(
        file_path,
        os.path.join(
            project,
            "featureTable/inputs",
            "nonemittingTrainingWithPayload.txt"
        )
    )

    # write output summary to file
    file_path = os.path.join(
        project,
        "logs/occupancySelectorLogs",
        "occupancySelectorNonEmittingSummary.csv"
    )
    with open(file_path,'w') as file:
        file.write("recordId, FWHM, MaxNSigma, GammaAlarm, NeutronAlarm\n")
        for line in results:
            file.write(
                "{}, {}, {}, {}, {}\n".format(
                    line[0],line[1],line[2],line[3],line[4]
                )
            )

    file_path = os.path.join(
        project,
        "logs/occupancySelectorLogs",
        "occupancySelectorEmittingSummary.csv"
    )
    with open(file_path,'w') as file:
        file.write("recordId, FWHM, MaxNSigma, GammaAlarm, NeutronAlarm\n")
        for line in emitting_records:
            file.write(
                "{}, {}, {}, {}, {}\n".format(
                    line[0],line[1],line[2],line[3],line[4]
                )
            )

    # using the remaining records to populate fold11.txt and fold20.txt
    # Generate a warning if remaining records are fewer than 2 * nFolds11_20
    # if remaining records counts is less than .80 of 2 * nFolds11_20 then
    # return error
    num_remaining_recs = len(rids) - index
    folds_11_20_counts = 0
    if num_remaining_recs >= (int(1.2 * nFolds11_20 + 0.5) * 2):
        folds_11_20_counts = int(1.2 * nFolds11_20 + 0.5)
    elif num_remaining_recs >= (2 * nFolds11_20):
        folds_11_20_counts = nFolds11_20
    elif num_remaining_recs < int(0.8 * (2*nFolds11_20) + 0.5):
        print("***ERROR*** Too little remaining records for folds 11 and 20")
        print("Rerun the script with lower values for nRecords and/or nFolds11_20")
        return False
    else:
        folds_11_20_counts = num_remaining_recs // 2

    print("Saving stream-of-commerce data to {}".format(
        os.path.join(project,"featureTable", "inputs")
    ))
    max_counts = folds_11_20_counts + index
    file_path = os.path.join(
        project,
        "featureTable/inputs",
        "fold11.txt"
    )
    with open(file_path, 'w') as file:
        while True:
            file.write("{}\n".format(rids[index]))
            index += 1
            if index == max_counts:
                break

    max_counts = folds_11_20_counts + index
    file_path = os.path.join(
        project,
        "featureTable/inputs",
        "fold20.txt"
    )
    with open(file_path, 'w') as file:
        while True:
            file.write("{}\n".format(rids[index]))
            index += 1
            if index == max_counts:
                break

    print("Plotting alarms distirbution...")

    results.extend(emitting_records)
    results = numpy.array(results)

    fig, ax = plt.subplots()
    ax.loglog(
        results[:,1], results[:,2],
        '.', markersize=1,
        linestyle='',
        label="no alarm"
    )

    rgammaAlarm = results[results[:,3]==1]
    ax.loglog(
        rgammaAlarm[:,1],
        rgammaAlarm[:,2],
        '.', markersize=2,
        linestyle='',
        label="gamma"
    )

    rneutronAlarm = results[results[:,4]==1]
    ax.loglog(
        rneutronAlarm[:,1],
        rneutronAlarm[:,2],
        '.', markersize=2,
        color="r",
        linestyle='',
        label="neutron"
    )

    ax.axhline(nSigmaCutValue, color="black")
    ax.set_title("Distribution of alarming scans at %s" % siteId)
    ax.set_xlabel("Extent.Smoothed.FWHM")
    ax.set_ylabel("Max NSigma")
    ax.set_xlim( 1e-1, 1e+2 )
    ax.set_ylim( 1e-2, 1e+4 )
    lgnd = ax.legend(numpoints=1)
    for handle in lgnd.legendHandles: handle._legmarker.set_markersize(10)

    file_path = os.path.join(
        project,
        "plots",
        "NSigmaAlarms_{}.png".format(siteId)
    )
    plt.savefig(
        file_path,
        dpi=200
    )

    plt.close(fig)

    print("Selection of occupancies finished.", flush=True)

    return True

if __name__=="__main__":
    # python vm250OccupancySelector.py my_project_folder site6 -n 50000
    parser = argparse.ArgumentParser(
        description="Select occupancies for building a new feature table",
        usage="python vm250OccupancySelector.py [-h] project_dir siteId [-n NRECORDS] [-nf NFOLDS11_20] [-ns NSIGMACUTOFF]"
    )
    parser.add_argument("project", help="path to project directory")
    parser.add_argument("siteId", help="site label for use in plotting")
    parser.add_argument(
        "-n", "--nRecords",
        type=int, default=None,
        help="Max # of records to go into non-emitting testing and training text files"
    )
    parser.add_argument(
        "-nf", "--nFolds11_20",
        type=int, default=None,
        help="Max # of records to go into fold11.txt and fold20.txt"
    )
    parser.add_argument(
        "-ns", "--nSigmaCutOff",
        type=float, default=4.0,
        help="N-Sigma value to make the cut in the data between emitting and non-emitting"
    )
    args = parser.parse_args()

    selectOccupancies(
        args.project,
        args.siteId,
        args.nRecords,
        args.nFolds11_20,
        args.nSigmaCutOff
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