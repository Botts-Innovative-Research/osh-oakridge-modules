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

Two environment variables need to be set first:
    JAVA_HOME (pointing to the same JDK that JPype was compiled against), and
    ERNIE_HOME (pointing to the directory containing all ERNIE repositories)

THis version is modified so that it uses development versions only.
"""

import os
import glob
import platform
import jpype
import jpype.imports

def ernieRoot():
    return os.path.join(os.path.dirname(__file__),'..','..')


def javaClasspath():
    """
    Set the classpath for unix or windows.
    """
    classes = []
    root = "../.." #ernieRoot()

    # didn't find any jars in proj-ernie4 so grab jars from projects
    if not classes:
        for repo in (
                'edu.cmu.cs.auton.classifiers',
                'gov.llnl.utility',
                'gov.llnl.math',
                'gov.llnl.ernie',
                'gov.llnl.ernie.vm250',
                'gov.llnl.ernie.tools',
        ):
            path = os.path.join(root, "src", repo, 'dist', repo+'.jar')
            if not path:
                print("%s not found" % path)
                continue
            classes.append(path)

        # also add external dependencies:
        classes.extend(glob.glob(os.path.join(
            root, 'src', 'gov.llnl.ernie', 'dependency', '*.jar')))

    # ant build system placed compiled jars inside proj-ernie4/jars
    # check this first
    #for jar_path in glob.glob(os.path.join(root, "jars", "*.jar")):
    #    if jar_path:
    #        classes.append(jar_path)

    return classes

if not jpype.isJVMStarted():
    jpype.startJVM(jpype.getDefaultJVMPath(),
            #"-Xint", "-Xdebug", "-Xnoagent", "-Xrunjdwp:transport=dt_socket,server=y,address=12999,suspend=n",
            classpath = javaClasspath(), convertStrings=False
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