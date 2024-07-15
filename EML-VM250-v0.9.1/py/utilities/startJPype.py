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
Configure the CLASSPATH and start up JPype.

One environment variable need to be set first:
    JAVA_HOME (pointing to the same JDK that JPype was compiled against), 
    
Optional environment variable to set up:
    ERNIE_HOME (pointing to the directory containing all ERNIE's Java repositories)
    
    - ERNIE_HOME is required if there is no jars folder in the proj-ernie4 directory.
"""

import os
import glob
import platform
from utilities import utilities
import jpype
import jpype.imports


def javaClasspath():
    """
    Set the classpath for unix or windows.
    """
    classes = []

    # ANT build system placed compiled jars inside proj-ernie4/jars
    # check this first
    for jar_path in glob.glob(os.path.join(utilities.projErnie4Dir(), "jars", "*.jar")):
        if jar_path:
            classes.append(jar_path)

    # Didn't find any jars in proj-ernie4 so grab jars from projects
    if not classes:
        if 'ERNIE_HOME' not in os.environ:
            print("***ERROR***")
            print("Need to set up environment or build jars:\n"
                  "1) Run 'env.bat' (or 'source env.sh' on Cygwin)\n"
                  "or\n"
                  "2) On proj-ernie4 root run the two python commands:\n"
                  "\ta) python build.py src\n\tb) python build.py jar")
            raise Exception("Cannot find required jar files")

        ERNIE_HOME = os.environ['ERNIE_HOME']
        for repo in (
                'edu.cmu.cs.auton.classifiers',
                'gov.llnl.utility',
                'gov.llnl.math',
                'gov.llnl.ernie',
                'gov.llnl.ernie.vm250',
                'gov.llnl.ernie.raven',
        ):
            path = glob.glob(os.path.join(ERNIE_HOME, repo, 'dist', '*.jar'))
            if not path:
                print("%s not found" % repo)
                continue
            classes.append(path[0])

        # Also add external dependencies:
        classes.extend(glob.glob(os.path.join(
            ERNIE_HOME, 'gov.llnl.ernie', 'lib', '*.jar')))
            
        classes.extend(glob.glob(os.path.join(
            ERNIE_HOME, 'gov.llnl.ernie.vm250', 'lib', '*.jar')))
            
        classes.extend(glob.glob(os.path.join(
            ERNIE_HOME, 'gov.llnl.ernie.raven', 'lib', '*.jar')))            

    sep = ':'
    if platform.system() == 'Windows' or 'CYGWIN' in platform.system():
        sep = ';'

    return sep.join(classes)

if not jpype.isJVMStarted():
    jpype.startJVM(jpype.getDefaultJVMPath(),
            #"-Xint", "-Xdebug", "-Xnoagent", "-Xrunjdwp:transport=dt_socket,server=y,address=12999,suspend=n",
            '-Djava.class.path=%s' % javaClasspath(), convertStrings=False
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