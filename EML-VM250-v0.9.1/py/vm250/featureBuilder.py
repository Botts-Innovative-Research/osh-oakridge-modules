#
# Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
# 
# OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
# 
# This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
# between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
# See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
#

import argparse
import shutil
import os
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
import jpype.imports

# Import Java packages
from java.nio.file import Paths,Path
from java.util.logging import Level
from gov.llnl.utility.xml.bind import DocumentReader
from gov.llnl.ernie.tools.builder import FeatureBuilder
from gov.llnl.ernie.tools import ErnieToolsPackage


# on error featureBuilder returns False
def featureBuilder(project = "", nRecords = None, nFolds11_20 = None):
    error = utilities.initialChecks(project, portalType='vm250', verbose=False)
    if error:
        return False

    # Create a feature table builder
    searchPaths=jpype.JArray(Path) ([
        Paths.get(
            project
        ),
        Paths.get(
            os.path.join(utilities.projErnie4Dir(), "config")
        ),
        Paths.get(
            os.path.join(project,"featureTable/inputs")
        )
    ])

    try:
        ErnieToolsPackage.getInstance().enableLog(Level.ALL)

        # Load the feature builder from disk
        featureBuilderReader = DocumentReader.create(FeatureBuilder)
        featureBuilderReader.setProperty(DocumentReader.SEARCH_PATHS, searchPaths)

        xml_file = os.path.join(project,"featureBuilderVM250.xml")
        db_path = os.path.join('.',project,"featureTable","test-vm250")

        elem_Rec_string = '<util:integer id="records">'
        elem_RecSOC_poststr = "</util:integer>"
        elem_SOC_string = '<util:integer id="SOCrecords">'
        elem_dailyFileWriter = '<ernie-vm250:dailyFileWriter dirname="'

        update_records = False
        update_SOCrecords = False
        if nRecords != None:
            update_records = True
        if nFolds11_20 != None:
            update_SOCrecords = True

        update_dailyFiles = True

        # check if featureBuilderVM250.xml alrady exists
        if not os.path.exists(xml_file):
            # update records and SOCrecords if need too
            # update db path in featureBuilderVM250.xml
            # update dailyFiles path
            # if the xml internal element ever changed elem_DB_string need to be updated
            elem_DB_string = "<connection>jdbc:h2:"
            elem_DB_str_end =";DATABASE_TO_UPPER=FALSE</connection>\n"


            original_xml = os.path.join(utilities.projErnie4Dir(), "config","featureBuilderVM250.xml")

            update_db_path = True

            with open(original_xml, 'r') as org_file:
                with open(xml_file, 'w') as new_file:
                    for line in org_file:
                        if update_records and elem_Rec_string in line:
                            index = line.find('<')
                            new_file.write(
                                "{}{}{}{}\n".format(
                                    line[:index],
                                    elem_Rec_string,
                                    nRecords,
                                    elem_RecSOC_poststr
                                )
                            )
                            update_records = False
                        elif update_SOCrecords and elem_SOC_string in line:
                            index = line.find('<')
                            new_file.write(
                                "{}{}{}{}\n".format(
                                    line[:index],
                                    elem_SOC_string,
                                    nFolds11_20,
                                    elem_RecSOC_poststr
                                )
                            )
                            update_SOCrecords = False
                        elif update_db_path and elem_DB_string in line:
                            outStr = "{}{}{}{}".format(
                                line[:line.index(elem_DB_string)], # perserved tabs
                                elem_DB_string,
                                db_path,
                                elem_DB_str_end
                            )
                            new_file.write(outStr)
                            update_db_path = False
                        elif update_dailyFiles and elem_dailyFileWriter in line:
                            index = line.find('<')
                            new_file.write(
                                "{}{}{}{}\n".format(
                                    line[:index],
                                    elem_dailyFileWriter,
                                    os.path.join(project,"featureTable","dailyFiles"),
                                    '"/>'
                                )
                            )
                            update_dailyFiles = False
                        else:
                            new_file.write(line)
        else:
            # check if the connection element is pointing to the current project dir
            # prevent overwriting of db from other project folders
            # also check if user's supplied records and SOC values


            file_data = []
            is_dirty = False
            elem_str = "jdbc:h2:"
            with open(xml_file, 'r') as file:
                for line in file:
                    if update_records and elem_Rec_string in line:
                        index = line.find('<')
                        line = "{}{}{}{}\n".format(
                            line[:index],
                            elem_Rec_string,
                            nRecords,
                            elem_RecSOC_poststr
                        )
                        update_records = False
                        is_dirty = True
                    elif update_SOCrecords and elem_SOC_string in line:
                        index = line.find('<')
                        line = "{}{}{}{}\n".format(
                            line[:index],
                            elem_SOC_string,
                            nFolds11_20,
                            elem_RecSOC_poststr
                        )
                        update_SOCrecords = False
                        is_dirty = True

                    elif update_dailyFiles and elem_dailyFileWriter in line:
                            dailyPath = os.path.join(project,"featureTable","dailyFiles")
                            if not dailyPath in line:
                                index = line.find('<')
                                line = "{}{}{}{}\n".format(
                                    line[:index],
                                    elem_dailyFileWriter,
                                    os.path.join(project,"featureTable","dailyFiles"),
                                    '"/>'
                                )
                                is_dirty = True
                            update_dailyFiles = False
                    if elem_str in line and not db_path in line:
                        start_index = line.find(elem_str) + len(elem_str)
                        end_index = line.find(";")
                        line = line.replace(
                            line[start_index:end_index],
                            db_path
                        )
                        is_dirty = True

                    file_data.append(line)
            if is_dirty:
                os.remove(xml_file)
                with open(xml_file,'w') as file:
                    file.write("".join(file_data))

        # FIXME for some reason loading feature builder fails unless I load this first:
        from gov.llnl.ernie import Analysis
        analysisFile = Paths.get(os.path.join(utilities.projErnie4Dir(),"config","vm250Analysis.xml"))
        analysisReader = DocumentReader.create(Analysis)
        analysisReader.setProperty(DocumentReader.SEARCH_PATHS, searchPaths)
        analysis = analysisReader.loadFile(analysisFile)
        # end FIXME

        featureBuilder = featureBuilderReader.loadFile(Paths.get(xml_file))
        featureBuilder.getRecordDatabase().getLaneDatabase().loadLanes(Paths.get(
            os.path.join(utilities.projErnie4Dir(),"config","vm250LaneConfigurations.csv")))

        # Redirect the logs
        featureBuilder.setLoggingPath(Paths.get(
             os.path.join(project,"logs","featureBuilderLogs")
        ))

        # Execute build of features
        featureBuilder.execute()

        return True

    except Exception as ex:
        print(ex.stacktrace())
        return False


if __name__=="__main__":
    parser = argparse.ArgumentParser(
            description="Building a feature table",
            usage="python featureBuilder.py [-h] project_dir [-n NRECORDS] [-nf NFOLDS11_20]"
    )
    parser.add_argument("project", help="path to project directory")
    parser.add_argument(
        "-n", "--nRecords",
        type=int, default=None,
        help="The number of non-emitting records. It Should match the number used "
             "in occupancySelect.py. If not sure, can leave this argument alone."
    )
    parser.add_argument(
        "-nf", "--nFolds11_20",
        type=int, default=None,
        help="The number of stream-of-commerce records. It should match the number "
             "used in occupancySelect.py. If not sure, can leave this argument alone."
    )
    args = parser.parse_args()
    featureBuilder(
        args.project,
        args.nRecords,
        args.nFolds11_20
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