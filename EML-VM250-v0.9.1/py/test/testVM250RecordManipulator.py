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
from utilities import startJPype
import jpype

import numpy as np
import matplotlib.pyplot as plt

from gov.llnl.ernie import Analysis
from java.util.logging import ConsoleHandler, SimpleFormatter
from gov.llnl.ernie import ErniePackage
from gov.llnl.ernie.io import RecordDatabase as Database
from gov.llnl.utility.xml.bind import DocumentReader
from java.nio.file import Paths
from gov.llnl.ernie.data import SensorPosition
from gov.llnl.ernie.manipulator import InjectionSourceLibrary
from gov.llnl.ernie.manipulator import RecordManipulator
from gov.llnl.ernie.common import StatisticalFeatureExtractor
from gov.llnl.ernie.common import PeakFeatureExtractor
from gov.llnl.ernie.common import ExtentFeatureExtractor


try:
    analysisReader = DocumentReader.create(Analysis)
    analysis = analysisReader.loadFile(Paths.get("config/vm250Analysis.xml"))

    manipulatorReader = DocumentReader.create(RecordManipulator)
    manipulator = manipulatorReader.loadFile(Paths.get("config/vm250RecordManipulator.xml"))

    library = manipulator.getLibrary()

    print("\n".join(["%s %s" % (i.getKey(), i.getValue())
                     for i in analysis.getSettings().entrySet()]))

except Exception as ex:
    print(ex.stacktrace())
    exit(-1)

# Produce the settings for auditing of the result file.
# u=analysisReader.getProperty(DocumentReader.RESULT_MD5SUM)
#print("\n".join([str(i) for i in u.entrySet()]))

#################################################################

try:
    pfe = PeakFeatureExtractor()
    sfe = StatisticalFeatureExtractor()
    efe = ExtentFeatureExtractor()
    sfe.setPeakFeatureExtractor(pfe)
    sfe.initialize()

    dbr = DocumentReader.create(Database)
    database = dbr.loadFile(Paths.get('config/vm250RecordDatabase.xml'))

    # Load a record
    recordId = 80000005
    record = database.getRecord( recordId )
    ar = analysis.processRecord(record)


    for idx, lanewidth in enumerate(np.linspace(4,8,9)):
        record2 = database.getRecord( recordId )
        analysis.prepare(record2)
        record2.getLane().setLaneWidth(lanewidth)

        manip1 = manipulator.createManipulation("test%d" % idx)
        manip1.setInjectCompact(True)
        manip1.setCompactSourceId("testing-fissile.xml.gz#2")
        manip1.setCargoModelId(0)
        manip1.setCompactIntensity(1000)
        manip1.setCompactLocation(3, 0, 1)
        record2 = manipulator.applyManipulation(record2, manip1).getRecord()

        analysis.prepare(record2)
        extentFeatures = efe.compute(record2)
        statFeatures = sfe.compute(record2)
        statDesc = statFeatures.getDescription()

        print("Lane width %s, FWHM.In %.3f, Peak.Intensity %.3f, Peak.IMean %.3f" % (
                lanewidth, extentFeatures.innerWidths[1],
                statDesc.get(statFeatures, "Peak.Intensity"),
                statDesc.get(statFeatures, "Peak.IMean"),
                ))


    # Done for now
    sys.exit(0)

    if False:
        manip1 = manipulator.createManipulation("test")
        #manip1.setExternalCompactSource(library.getSource("testing-fissile.xml.gz#1"))
        manip1.setCompactSourceId("testing-fissile.xml.gz#1")
        manip1.setCargoModelId(0)
        manip1.setCompactIntensity(1000)
        manip1.setCompactLocation(3, -1, 1)
        record2 = manipulator.applyManipulation(record2, manip1).getRecord()
        analysis.prepare(record2)

    if True:
        manip2 = manipulator.createManipulation("test")
        #manip2.setExternalCompactSource(library.getSource("testing-fissile.xml.gz#2"))
        manip2.setCompactSourceId("testing-fissile.xml.gz#2")
        manip2.setCargoModelId(0)
        manip2.setCompactIntensity(10)
        manip2.setCompactLocation(15, 0, 0.5)
        record2 = manipulator.applyManipulation(record2, manip2).getRecord()
        analysis.prepare(record2)

    if True:
        manip2 = manipulator.createManipulation("test")
        #manip2.setExternalDistributedSource(library.getSource("testing-fissile.xml.gz#2"))
        manip2.setDistributedSourceId("testing-fissile.xml.gz#2")
        manip2.setCargoModelId(0)
        manip2.setDistributedIntensity1(1)
        manip2.setDistributedIntensity2(1)
        manip2.setDistributedPx1(8)
        manip2.setDistributedPx2(18)
        manip2.setDistributedPy(0.9)
        manip2.setDistributedPz(2.5)
        record2 = manipulator.applyManipulation(record2, manip2).getRecord()
        analysis.prepare(record2)

    if False:
        manip2 = manipulator.createManipulation("test")
        originalVelocity = record.getVehicleMotion().getVelocityInitial()
        manip2.setAlterVelocity(True)
        manip2.setSpeedStart(originalVelocity * 1.25)
        manip2.setSpeedEnd(originalVelocity * 1.25)     # only constant factor supported right now
        record2 = manipulator.applyManipulation(record2, manip2).getRecord()
        analysis.prepare(record2)

#    record2= record

    dt = record2.getGammaMeasurements()[0].getSensorProperties().getSamplePeriodSeconds()

    sf = sfe.compute(record2)
    ef = efe.compute(record2)

    print(sf.frontPeakFeatures.peakStartTime)
    print(sf.frontPeakFeatures.peakEndTime)
    print(sf.rearPeakFeatures.peakStartTime)
    print(sf.rearPeakFeatures.peakEndTime)

    # Extract relevent datums
    peakIntensity = (dt*sf.get("Peak.Intensity", None))
    peakMean = sf.get("Peak.XMean", None)
    peakStd = sf.get("Peak.XStdDev", None)

    frontIntensity = (dt*sf.get("Front.Intensity", None))
    frontMean = sf.get("Front.XMean", None)
    frontStd = sf.get("Front.XStdDev", None)

    rearIntensity = (dt*sf.get("Rear.Intensity", None))
    rearMean = sf.get("Rear.XMean", None)
    rearStd = sf.get("Rear.XStdDev", None)

    splitPosition = sf.get("Split.Position", None)
    splitIntensity = dt*sf.get("Split.Intensity", None)

    frontIMean = dt*sf.get("Front.IMean", None)
    rearIMean = dt*sf.get("Rear.IMean", None)
    allIMean = dt*sf.get("All.IMean", None)

    frontIStdDev = dt*sf.get("Front.IStdDev", None)
    rearIStdDev = dt*sf.get("Rear.IStdDev", None)
    allIStdDev = dt*sf.get("All.IStdDev", None)

    # Dump the features
    desc = sf.getDescription()
    for feature in desc.getFeatureDescriptions():
        print(feature.getName(),feature.get(sf))
    desc = ef.getDescription()
    for feature in desc.getFeatureDescriptions():
        print(feature.getName(),feature.get(ef))

    sm = record.getCombinedGammaMeasurement(SensorPosition.ALL)
    sb = record.getCombinedGammaBackground(SensorPosition.ALL)
    position = record.getVehicleMotion().getPositions(sm)
    plt.plot(position, np.array(sm.getCountsRange(0, sm.size())) - np.array(sb.getCountsRange(0, sb.size())))

    sm = record2.getCombinedGammaMeasurement(SensorPosition.ALL)
    sb = record2.getCombinedGammaBackground(SensorPosition.ALL)
    position = record2.getVehicleMotion().getPositions(sm)
    plt.plot(position, np.array(sm.getCountsRange(0, sm.size())) - np.array(sb.getCountsRange(0, sb.size())))
    plt.axvline(splitPosition, c='k')
    plt.plot([splitPosition-0.5, splitPosition+0.5], [splitIntensity, splitIntensity], c="k")

    pend=position[len(position)-1]
    pmid=(position[0]+pend)/2

    plt.plot([position[0], pend],[allIMean, allIMean], c='y')
    plt.plot([position[0], splitPosition],[frontIMean, frontIMean], c='y')
    plt.plot([splitPosition, pend],[rearIMean, rearIMean], c='y')
    plt.plot([pmid, pmid],[allIMean-allIStdDev, allIMean+allIStdDev], c='y')
    pmid=(position[0]+splitPosition)/2
    plt.plot([pmid, pmid],[frontIMean-frontIStdDev, frontIMean+frontIStdDev], c='y')
    pmid=(pend+splitPosition)/2
    plt.plot([pmid, pmid],[rearIMean-rearIStdDev, rearIMean+rearIStdDev], c='y')

    # Plot the peak features
    plt.axvline(peakMean, c='b')
    plt.plot([peakMean-peakStd, peakMean+peakStd], [peakIntensity, peakIntensity], c="b")

    # Plot the front features
    plt.axvline(frontMean, c='g')
    plt.plot([frontMean-frontStd, frontMean+frontStd], [frontIntensity, frontIntensity], c="g")

    # Plot the rear features
    plt.axvline(rearMean, c='r')
    plt.plot([rearMean-rearStd, rearMean+rearStd], [rearIntensity, rearIntensity], c="r")

#    plt.show(block=False)
#    plt.figure()
#    for sm in record2.getGammaMeasurements():
#        position = record2.getVehicleMotion().getPositions(sm)
#        plt.plot(position, sm.getCountsRange(0, sm.size()))
    plt.show()


except jpype.JException as ex:
    print(ex.stacktrace())
    raise ex



#
# Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
# 
# OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
# 
# This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
# between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
# See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
#