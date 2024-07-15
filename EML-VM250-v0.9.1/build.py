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

import os
import sys
import inspect
currentDir = os.path.dirname(os.path.abspath(inspect.getfile(inspect.currentframe())))

# Bootstrap for a fresh pull
#exec(open(os.path.join(currentDir, ".bootstrap.py")).read())

# Import the project resources
from project import *

packages = [
    # Output to display,             # Repo project name             # branch     # build tool 
    ('gov.llnl.utility',             'gov.llnl.utility',             'ernie',     'ant'),
    ('gov.llnl.math',                'gov.llnl.math',                'ernie',     'ant'),
    ('edu.cmu.cs.auton.classifiers', 'edu.cmu.cs.auton.classifiers', 'master',    'ant'),
    ('gov.llnl.ernie',               'gov.llnl.ernie',               'EML-VM250', 'ant'),
    ('gov.llnl.ernie.vm250',         'gov.llnl.ernie.vm250',         'EML-VM250', 'ant'),
    ('gov.llnl.ernie.raven',         'ernie/gov.llnl.ernie.raven',   'master',    'ant'),
]

project = Project()
project.property("repo", "ssh://git@czgitlab.llnl.gov:7999/rda")
project.property("src", "src")
project.property("project", "ernie4")
project.property("build", "build")
project.property("stage", "$build/$project")
project.property("dist", "dist")

# FIXME these need to be platform dependent
if platform.system()=='Windows':
    project.property("ant", "ant.bat")
    project.property("mvn", 'cmd /c mvn')
else:
    project.property("ant", "ant")
    project.property("mvn", "mvn")

project.property("date", datetime.date.today().strftime("%Y%m%d"))

clones = project.group("src")
for package in packages:
    clones.append(GitClone(package[1], branch=package[2], output=package[0]))
clones.append(GitArchive("ernie-vm250-source", output="config/vm250"))

checkout = project.group("checkout")
for package in packages:
    checkout.append(GitCheckout(package[0], branch=package[2]))

pulls = project.group("pull")
pulls.append(GitPull('.', directory='.'))
for package in packages:
    pulls.append(GitPull(package[0]))

status = project.group("status")
status.append(GitStatus('.', directory='.'))
for package in packages:
    status.append(GitStatus(package[0]))

diff = project.group("diff")
for package in packages:
    diff.append(GitDiff(package[0]))


adds = project.group("add")
for package in packages:
    adds.append(GitAdd(package[0]))

commits = project.group("commit")
for package in packages:
    commits.append(GitCommit(package[0]))

pushs = project.group("push")
for package in packages:
    pushs.append(GitPush(package[0]))

jar = project.group("jar")
for package in packages:
    dep = [project.getTarget("clone-%s" % package[1])]
    if package[3]=="ant":
        jar.append(Ant("jar", package[0], depends=dep))
    elif package[3]=="mvn":
        jar.append(Mvn("package", package[0], depends=dep))
jar.append(CopyJars("copy", [i[0] for i in packages], srcdir="$src", destdir="jars"))


stages = project.group("stage")
stages.append(CopyJars("stage", [i[0] for i in packages], srcdir="$src", destdir="$stage/jars"))
stages.append(StageCopy("py", srcdir='py', destdir='$stage/py', includes=['*.py'], recursive=True))
stages.append(StageCopy("project", srcdir='project', destdir='$stage/project', includes=['*.py'], recursive=True))
stages.append(StageCopy("config", srcdir='config', destdir='$stage/config', includes=['*.xml', '*.xml.gz'], recursive=True))
stages.append(StageExtra(files=["Notice", "build.py", 'Readme.*', '.bootstrap.py'], destdir="$stage"))
for package in packages:
    stages.append(StageSrc(package[0], srcdir="$src", destdir="$stage/src"))

pkg = project.append(
    Tar(srcdir="$stage", dest="$dist/$project-$date.tar.gz", depends=[stages]))

project.append(EncryptGPG(src="$dist/$project-$date.tar.gz", depends=[pkg]))
project.append(CleanDir("stage", directory="$stage"))
project.append(CleanDir("build", directory="$build"))
project.append(CleanDir("src", directory="$src"))

cleanjars = project.group("clean")
for package in packages:
    if package[3]=="ant":
        cleanjars.append(Ant("clean", package[0]))
    elif package[3]=="mvn":
        cleanjars.append(Mvn("clean", package[0]))

###################################################################################

project.parse()

if project.args.list:
    print("All possible targets:")
    for targ in project.targets:
        print(targ)
    sys.exit()

project.execute()


#
# Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
# 
# OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
# 
# This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
# between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
# See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
#
