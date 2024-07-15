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
trainer.py: builds an ERNIE classifier for a new port.
 - generates a feature table for training / testing
 - builds one or more random forest classifiers
 - tests performance and generates plots
 - compiles results into a summary
"""
import argparse
import time
import sys
import os
import inspect
parent_dir = os.path.dirname(
    os.path.dirname(os.path.abspath(inspect.getfile(inspect.currentframe())))
)
sys.path.insert(0,parent_dir)

# start Java context
from utilities import startJPype
from utilities.utilities import initialChecks

from occupancySelector import selectOccupancies
from featureBuilder import featureBuilder
from testFeatureTable import testFeatureTable
from model_builder import model_builder
from performance_analyzer import performanceAnalyzer

def trainERNIE(
        project,
        siteId,
        nRecords = 10000,
        nFolds11_20 = 100000,
        nSigmaCutValue = 4.0,
        modelName=None,
        skipSSL=False,
        mandatoryInspectionRate = 0.000525,
        startWithFeatureTable=False
    ):

    initialStart = time.time()

    # check for required structures in the project folder
    print("Checking project directory and required files...")
    error = initialChecks(project, portalType='vm250', verbose=False)
    if error:
        return False

    if not startWithFeatureTable:
        # Occupancy Selector
        start = time.time()
        # delegate sanity check to occupancy selector since it does it
        if not selectOccupancies(project, siteId, nRecords, nFolds11_20, nSigmaCutValue, doCheck = False):
            return
        print("Finished selecting occupancies. Elapsed time: %.2f s" % (time.time() - start))

        # Building feature table
        start = time.time()
        if not featureBuilder(project, nRecords, nFolds11_20):
            return
        print("Finished building feature table. Elapsed time: %.2f s" % (time.time() - start))
    else:
        print("Using feature table from previous run")

    # Testing the feature table
    start = time.time()
    # db_path is tightly couple to featureBuilder.py
    # if the name of the db is changed in featureBuilder.py it need to be change
    # here or the trainer will error out
    db_path = os.path.join(project,"featureTable","test-vm250")
    testFeatureTable(db_path)
    print("Finished testing feature table. Elapsed time: %.2f s" % (time.time() - start))

    # Build random forest classifiers:
    print("Build classifier models:")
    start = time.time()
    model_builder(project, modelName, skipSSL)
    print("Finished building classifier models. Elapsed time: %.2f s" % (time.time() - start))

    # Do performance analysis:
    print("Analyze performance and generate ROC plots:")
    start = time.time()
    performanceAnalyzer(project, mandatoryInspectionRate=mandatoryInspectionRate)
    print("Finished performance analysis. Elapsed time: %.2f s\n" % (time.time() - start))

    print("Model building for project %s completed successfully!" % project)
    print("Total elapsed time: %.2f s" % (time.time() - initialStart))


if __name__ == '__main__':
    parser = argparse.ArgumentParser(
        description="Generate ERNIE classifier at a new port",
        usage="python trainer.py [-h] project_dir siteId [-n NRECORDS] " \
        "[-nf NFOLDS11_20] [-ns NSIGMACUTOFF] [-m MODELNAME] [--skip-ssl] " \
        "[--mandatoryInspectionRate RATE]"
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
    parser.add_argument("-m", "--modelName", help="Name to give the machine-learning model")
    parser.add_argument("--skipssl", dest="skipSSL", action="store_true", help="skip SSL training")
    parser.add_argument("--mandatoryInspectionRate", type=float, default=None, help="red line on ROC plots")
    parser.add_argument("--startWithFeatureTable", action="store_true", help="Continue model building with existing feature table")

    args = parser.parse_args()

    trainERNIE(
        args.project, args.siteId, args.nRecords, args.nFolds11_20,
        args.nSigmaCutOff, args.modelName, args.skipSSL,
        args.mandatoryInspectionRate,
        args.startWithFeatureTable
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