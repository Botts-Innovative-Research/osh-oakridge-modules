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
Write one VM250 scan out in "daily file" format
"""
import os

from utilities import startJPype
import jpype

from utilities.utilities import projErnie4Dir

import java.util
from java.io import File
from java.nio.file import Paths,Path

from gov.llnl.ernie.vm250.tools import DailyFileWriter

writer = DailyFileWriter()


def recordToDailyFile(vm250Record, filename):
    # third argument is for manipulations, leaving empty for now:
    writer.writeRecord(vm250Record, File(filename), java.util.ArrayList()) 


if __name__ == '__main__':
    from gov.llnl.utility.xml.bind import DocumentReader
    from gov.llnl.ernie.io import RecordDatabase

    config_path = os.path.join(projErnie4Dir(), "config")
    searchPaths=jpype.JArray(Path)([
        Paths.get(config_path)
    ])    

    try:
        reader = DocumentReader.create(RecordDatabase)
        reader.setProperty(DocumentReader.SEARCH_PATHS, searchPaths)
        dbc = reader.loadFile(Paths.get(os.path.join(config_path,"vm250RecordDatabase.xml")))
    except Exception as ex:
        print(ex.stacktrace())
        raise

    record = dbc.getRecord(10000005)
    recordToDailyFile(record, "sampleDailyFile.txt")



#
# Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
# 
# OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
# 
# This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
# between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
# See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
#