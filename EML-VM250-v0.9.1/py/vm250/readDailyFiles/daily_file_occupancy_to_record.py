#
# Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
# 
# OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
# 
# This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
# between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
# See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
#

import sys
import numpy
from datetime import datetime
import os
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
from java.time import Instant
from java.nio.file import Paths
from java.util import ArrayList

from gov.llnl.utility.xml.bind import DocumentReader
from gov.llnl.ernie.io import LaneDatabase
from gov.llnl.ernie.vm250.data import VM250Record
from gov.llnl.ernie.vm250.data import VM250RecordInternal
from gov.llnl.ernie.vm250.data.VM250RecordInternal import VelocityReading

class OccupancyConverter:

    def __init__(self, settings):
        self.settings = settings
        try:
            dbReader=DocumentReader.create(LaneDatabase)
            self.lanes=dbReader.loadFile(Paths.get(os.path.join(utilities.projErnie4Dir(),"config", "vm250LaneDatabase.xml")))
        except jpype.JException as ex:
            print(ex.stacktrace())
            sys.exit(-1)

    def occupancyToRecord(self, occupancy):
        record = VM250RecordInternal()

        if occupancy.lastGammaBackground is None:
            print("WARNING: missing gamma background info!")
            gammaBackground = [-1,-1,-1,-1]
        else:
            gammaBackground = occupancy.lastGammaBackground[1]

        # convert datetime.datetime to Instant
        instant = Instant.ofEpochMilli(
            int((occupancy.starttime - datetime.utcfromtimestamp(0)).total_seconds() * 1000.0)
        )
        record.setSegmentDescription(1, 1, instant, occupancy.continuation,
                gammaBackground, occupancy.neutronBackground,
                occupancy.gammaAlarm, occupancy.neutronAlarm, occupancy.realOccupancy)

        gammaCounts = numpy.array( [g[1] for g in occupancy.gammas], dtype=int )
        neutronCounts = numpy.array( [n[1] for n in occupancy.neutrons], dtype=int )

        for pidx in range(record.getNPanels()):
            panel = record.getPanelData()[pidx]
            panel.resize(len(gammaCounts))
            panel.resize_neutron(len(neutronCounts))

            if len(gammaCounts):
                for gidx, gammaCount in enumerate(gammaCounts[:,pidx]):
                    panel.gammaData[gidx][0] = int(gammaCount)

            if len(neutronCounts):
                for nidx, neutronCount in enumerate(neutronCounts[:,pidx]):
                    panel.neutronData[nidx][0] = int(neutronCount)

        velocities = ArrayList()
        for time,v in occupancy.velocities:
            instant = Instant.ofEpochMilli(int(time.timestamp()))
            velocities.add(VelocityReading(record, instant, v))
        record.setVelocities(velocities)

        self.fetchLane(record)

        record.setSetupInfo(self.settings['Intervals'], self.settings['OccupancyHoldin'], self.settings['NSigma'])

        return VM250Record( record )

    def fetchLane(self, record):
        # mock-up Database.fetchLane:
        lane = record.getLane()
        lane.setLaneType("Primary")
        lane.setLaneVector("Land Crossing")
        lane.setLaneConveyance("Cargo")
        lane.setLaneWidth(4.5)
        lane.setRpmId(1)
        lane.setPortId(1)
        lane.setPanels(4)

        self.lanes.lookup(lane)



#
# Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
# 
# OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
# 
# This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
# between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
# See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
#