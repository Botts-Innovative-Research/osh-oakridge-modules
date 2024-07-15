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
from pathlib import Path as PyPath
import inspect
import glob


def mkdir( path ):
    """Ignore directory if it already exists."""
    if not os.path.isdir( path ):
        os.mkdir( path )

def initialChecks( projname, portalType, verbose=True):
    """
    Test that project is initialized properly before starting to build feature table + machine learning model.
    Return True if error(s) encountered.
    """

    error = False
    if portalType not in ('rpm8', 'vm250'):
        raise NotImplementedError("Portal type '%s' is not yet supported!" % portalType)

    # check if there are jars. Users ran python build.py script
    classes = []
    for jar_path in glob.glob(os.path.join(projErnie4Dir(), "jars", "*.jar")):
        if jar_path:
            classes.append(jar_path)

    if not classes:
        if os.environ.get('ERNIE_HOME') is None:
            print("***ERROR***")
            print("Need to set up environment or build jars:\n"
                  "1) Run 'env.bat' (or 'source env.sh' on Cygwin)\n"
                  "or\n"
                  "2) On proj-ernie4 root run the two python commands:\n"
                  "\ta) python build.py src\n\tb) python build.py jar")
            error = True
        else:
            # make sure user actually has all required java projects
            classes = []
            for repo in (
                'edu.cmu.cs.auton.classifiers',
                'gov.llnl.utility',
                'gov.llnl.math',
                'gov.llnl.ernie',
                'gov.llnl.ernie.rpm8',
                'gov.llnl.ernie.vm250',
                'gov.llnl.ernie.tools',
                'gov.llnl.ernie.extractor',
                'gov.llnl.ernie.reinjector'
            ):
                path = glob.glob(os.path.join(ERNIE_HOME, repo, 'dist', '*.jar'))
                if not path:
                    classes.append(repo)

            if classes:
                print("***ERROR***")
                print("ERNIE_HOME has missing Java project jar:")
                for path in classes:
                    print("\t%s" % path)
                error = True
    classes = []

    # check for modules
    try:
        import pandas
        import matplotlib
    except ImportError:
        print("***ERROR***")
        print("One or more required Python packages (pandas and matplotlib) are missing")
        print("Installation commands:\n pip install pandas or pip3 install pandas ")
        print("pip install matplotlib or pip3 install matplotlib")
        error = True

    # exe is for Windows OS
    # non-exe is for Unix based OS
    if not (
        os.path.exists( os.path.join( projErnie4Dir(), 'bin', 'llnl_canes_ml_core.exe' ))
        or
        os.path.exists( os.path.join( projErnie4Dir(), 'bin', 'llnl_canes_ml_core' ))
        ):
        print("llnl_canes_ml_core[.exe] must be compiled and placed in the bin directory")
        error = True

    queriesFile = '%sQueries.txt' % portalType
    featureBuilderFile = 'featureBuilder%s.xml' % portalType.upper()
    if not os.path.exists(projname):
        print("***ERROR***")
        print("Project '%s' does not exist" % projname)
        print("Create the project directory and placed required files in it.")
        print("Required files:\n\tportIds.txt\n\t%s\n\t%s" % (queriesFile, featureBuilderFile))
        error = True

    # check required files
    missing_files = []
    if 'rpm8' in portalType:
        required_files = ('portIds.txt', queriesFile, featureBuilderFile, 'ROSsettings.csv')
    elif 'vm250' in portalType:
        required_files = ('portIds.txt', queriesFile, featureBuilderFile)

    for fname in required_files:
        if not os.path.exists(os.path.join(projname, fname)):
            missing_files.append(fname)

    if missing_files:
        print('***ERROR***')
        print("Required files are missing from the project directory:")
        for fname in missing_files:
            print("\t%s" % fname)
        error = True

    # check for optional files
    missing_files.clear()
    for filename in (
            'NORMids.txt',
            'MEDids.txt',
            'INDids.txt',
            'FISids.txt',
            'CONTAMids.txt'
            ):
        if not os.path.exists(os.path.join(projname, filename)):
            missing_files.append(filename)

    if verbose and missing_files:
        print("***WARNING***")
        for filename in missing_files:
            print("{} not found".format(filename))
        print("*************")

    # check for directories, if not found then create it
    for dirname in (
            'featureTable',
            'featureTable/inputs',
            'logs',
            'logs/occupancySelectorLogs',
            'logs/featureBuilderLogs',
            'modelBuilding',
            'modelBuilding/CV',
            'modelBuilding/allSSL',
            'modelBuilding/secondarySSL',
            'modelBuilding/fallbackSSL',
            'plots',
            'finalModel'
            ):
        fullname = os.path.join(projname, dirname)
        if not os.path.exists(fullname):
            try:
                os.mkdir(fullname)
            except:
                print("Could not create required directory '%s'" % fullname)
                error = True

    return error

def projErnie4Dir():
    parent_dir = os.path.dirname(
        os.path.dirname(os.path.abspath(inspect.getfile(inspect.currentframe())))
    )
    return str(PyPath(parent_dir).parent)


#
# Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
# 
# OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
# 
# This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
# between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
# See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
#