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
import sys
import numpy
import pandas
from matplotlib import pyplot as plt

sys.path.append("py") #adds the py directory of proj-ernie4 to path
os.environ['JAVA_HOME'] = "/usr/workspace/rda/pkg/amazon-corretto-11.0.6.10.1-linux-x64"

from utilities import startJPype
import jpype

from utilities.utilities import projErnie4Dir

import java.lang
from java.nio.file import Paths,Path

from gov.llnl.utility.xml.bind import DocumentReader
from gov.llnl.ernie import Analysis
from gov.llnl.ernie.io import RecordDatabase
from gov.llnl.ernie.manipulator import RecordManipulator
from gov.llnl.ernie.common import ExtentFeatureExtractor


class plotOccupancy:
    def __init__(self, mode='DB'):
        "Defines the mode as either database (DB) or Feature Table (FT)"
        self.mode = mode
        
    def setup_config(self, erniepath=projErnie4Dir(), analysis_file="vm250Analysis.xml", 
                     database_file="vm250RecordDatabase.xml", ft_path="", ft_name=""):
        config_path = os.path.join(erniepath, "config")
        
        searchPaths = searchPaths=jpype.JArray(Path)([
        Paths.get(config_path)
        ])
        
        try:
            #Load analysis file
            analysisReader=DocumentReader.create(Analysis)
            analysisReader.setProperty(DocumentReader.SEARCH_PATHS, searchPaths)
            self.analysis=analysisReader.loadFile(Paths.get(
                os.path.join(config_path, analysis_file)
            ))

            #load database file if database mode is enabled
            databaseReader=DocumentReader.create(RecordDatabase)
            databaseReader.setProperty(DocumentReader.SEARCH_PATHS, searchPaths)
            self.database=databaseReader.loadFile(Paths.get(
                os.path.join(config_path, database_file)
            ))
            # load extent extractor
            self.efe = ExtentFeatureExtractor()
            if self.mode=='FT':
                self.features = pandas.read_pickle(
                    os.path.join(ft_path, "featureTable", ft_name)
                )
                manipulatorReader = DocumentReader.create(RecordManipulator)
                self.manipulator = manipulatorReader.loadFile(Paths.get(config_path,"vm250RecordManipulator.xml"))
        except jpype.JException as ex:
            print(ex.stacktrace())
            
    def select_fold(self, foldnum=20):
        self.fold = self.features.loc[ self.features['SegmentInfo.FoldID'] == foldnum]
        
    def peak_intensity_filter(self, cutoff=500):
        self.fold = self.fold.loc[self.fold['Features.Statistics.Peak.Intensity']>cutoff]
        
    def plot_gammas(self, record, title=None, includeNeutrons=False, processed=False, originalrecord=False, ):
        records = [record]
        if originalrecord:
            records.append(originalrecord)
        self.fig = plt.figure(figsize=(len(records)*5, 5))
        for num,rec in enumerate(records,1):
            if processed:
                r1 = rec
            else:
                r1 = self.prepare_record(rec)
            ax = plt.subplot(1,len(records),num, )
            gammas = numpy.array([measurement.getCountsRange(0, measurement.size())
                              for measurement in r1.getGammaMeasurements()])
            gammaBackground = numpy.array([measurement.getCountsRange(0, measurement.size())
                              for measurement in r1.getGammaBackgrounds()])
            gammaTimes = numpy.arange(gammas.shape[1]) * 0.2  # each gamma sample is 0.2 s

            if includeNeutrons:
                neutrons = numpy.array([measurement.getCountsRange(0, measurement.size())
                                    for measurement in r1.getNeutronMeasurements()])
                neutronBackground = numpy.array([measurement.getCountsRange(0, measurement.size())
                              for measurement in r1.getNeutronBackgrounds()])
                neutronTimes = numpy.arange(neutrons.shape[1])  # each neutron sample is 1.0 s

            for pidx,g in enumerate(gammas):
                plt.plot(gammaTimes, g, drawstyle='steps-post', label='Panel %d' % pidx)
            for gb in gammaBackground:
                plt.plot(gammaTimes, gb, '--', drawstyle='steps-post')
            # plot sums
            plt.plot(gammaTimes, sum(gammas), drawstyle='steps-post', label='Panel Sum', color='k')
            plt.plot(gammaTimes, sum(gammaBackground), 'k--', drawstyle='steps-post')
            # Set axis conditions
            if num == 1:
                y_lim = ax.get_ylim()
            plt.ylim(y_lim)
            plt.xlabel("Time (s)")
            plt.ylabel("Counts")
            plt.legend()
            if title:
                if num==2:
                    title='Un'+title.lower()
                plt.title(title)
                
    def overlay_records(self, record_dict, title=None, includeNeutrons=False,):
        self.fig = plt.figure(figsize=(10,8))
        linestyles=['-', '--', ':']
        colors = ['C0','C1','C2','C3']
        for num,(key,rec) in enumerate(record_dict.items(),1):    
            r1 = rec
            gammas = numpy.array([measurement.getCountsRange(0, measurement.size())
                              for measurement in r1.getGammaMeasurements()])
            gammaBackground = numpy.array([measurement.getCountsRange(0, measurement.size())
                              for measurement in r1.getGammaBackgrounds()])
            gammaTimes = numpy.arange(gammas.shape[1]) * 0.2  # each gamma sample is 0.2 s

            if includeNeutrons:
                neutrons = numpy.array([measurement.getCountsRange(0, measurement.size())
                                    for measurement in r1.getNeutronMeasurements()])
                neutronBackground = numpy.array([measurement.getCountsRange(0, measurement.size())
                              for measurement in r1.getNeutronBackgrounds()])
                neutronTimes = numpy.arange(neutrons.shape[1])  # each neutron sample is 1.0 s

            for pidx,g in enumerate(gammas):
                plt.plot(gammaTimes, g, drawstyle='steps-post', linestyle=linestyles[num-1], 
                         color=colors[pidx], label='%d m Panel %d ' % (key, pidx))
            plt.plot(gammaTimes, sum(gammas), drawstyle='steps-post', linestyle=linestyles[num-1],
                     label='%d m Panel Sum' % key, color='k')
        plt.xlabel("Time (s)")
        plt.ylabel("Counts")
        plt.legend()
        if title:
            plt.title(title)
        
    def save_fig(self, fig_dir, title):
        if not os.path.exists(fig_dir):
            os.makedirs(fig_dir)
        self.fig.savefig(os.path.join(fig_dir,str(title)+'.png'))
            
    def find_manipulations(self, ):
        "Filters scans in a fold into 3 categories: unmanipulated, manipulated, or twicemanipulated(manipulated2)"
        feature = self.fold
        self.manipulated_scans = feature[ (feature['Manipulation.DistSourceInjected'] == 1) | (
            feature['Manipulation.PointSourceInjected']==1) ]
        self.manip_indeces = self.manipulated_scans.index
        mscans = self.manipulated_scans
        self.unmanipulated_scans = feature[~feature.index.isin(self.manipulated_scans.index)]
        self.manipulated2_scans = mscans[(mscans['Manipulation2.DistSourceInjected'] == 1) | (
            mscans['Manipulation2.PointSourceInjected']==1)]
        self.manipulated_scans = mscans[~mscans.index.isin(self.manipulated2_scans.index)]
        self.manip_list = [s for s in list(mscans) if s.startswith('Manipulation.')]
        self.manip2_list = [s for s in list(mscans) if s.startswith('Manipulation2.')]
    
        
    def prepare_record(self,rid):
        r1 = self.database.getRecord(rid)
        self.analysis.prepare(r1)
        return r1

    def extract_manipulation(self, idx, plot='WithOriginal', lanewidth=None, title=None, ):
        "extracts the manipulation parameters and creates a manipulated record"
        if idx in self.manipulated_scans.index:    
            scan = self.manipulated_scans.loc[idx]
            manip_list = ['Manipulation.']
        elif idx in self.manipulated2_scans:
            scan = self.manipulated2_scans.loc[idx]
            manip_list = ['Manipulation.', 'Manipulation2.']
        record = self.database.getRecord(scan['SegmentInfo.CBPID'])
        self.analysis.processRecord(record)
        record2 = self.database.getRecord(scan['SegmentInfo.CBPID'])
        print('original: ', str(record2.getLane().getLaneWidth()))
        self.analysis.prepare(record2)
        if lanewidth:
            record2.getLane().setLaneWidth(lanewidth)
        for count,m in enumerate(manip_list):
            manip = self.manipulator.createManipulation("index:%d" % idx)
            manip.setCargoModelId(int(scan[m+'CargoModel']))
            manip.setDistributedIntensity1(scan[m+'DistIntensity1'])
            manip.setDistributedIntensity2(scan[m+'DistIntensity2'])
            manip.setDistributedSourceId(scan[m+'DistSNum'])
            manip.setDistributedPx1(scan[m+'DistX1'])
            manip.setDistributedPx1(scan[m+'DistX2'])
            manip.setDistributedPy(scan[m+'DistY'])
            manip.setDistributedPz(scan[m+'DistZ'])
            originalVelocity = record.getVehicleMotion().getVelocityInitial()
            manip.setAlterVelocity(scan[m+'velocityChanged'])
            manip.setSpeedStart( scan[m+'StartVelocity'])
            manip.setSpeedEnd(scan[m+'EndVelocity'])            
            manip.setInjectCompact(scan[m+'PointSourceInjected'])
            manip.setCompactSourceId(scan[m+'PointSNum'])
            manip.setCompactIntensity(scan[m+'PointIntensity'])
            x = scan[m+'PointX']; y=scan[m+'PointY']; z=scan[m+'PointZ']
            manip.setCompactLocation(x, y, z)
            if lanewidth and count==1:
                manip.set
            record2 = self.manipulator.applyManipulation(record2, manip).getRecord()
            self.analysis.prepare(record2)
            # print(record2.getLane().getLaneWidth())
            # print(self.efe.compute(record2).innerWidths[1])
        self.record2 = record2
        if plot:
            if not title:
                title='Injected: ' + str(idx)
            else:
                title='Injected: ' + title
            if plot == "WithOriginal":
                self.plot_gammas(record=record2, processed=True, title=title, originalrecord=record)
            else:
                self.plot_gammas(record=record2, processed=True, title=title, )
            plt.show() 
        

############ example ft_mode
ft_path = "/p/vast1/rda/ERNIE4_models/2020_08_20_VM250_SiteJ_collimated_varyLaneWidth"
ft_name = "test-vm250.pickle"
test_ft = plotOccupancy(mode='FT') 
test_ft.setup_config(ft_path=ft_path, ft_name=ft_name)
test_ft.select_fold(32)
test_ft.peak_intensity_filter()
test_ft.find_manipulations()
for idx in test_ft.manip_indeces[:10]:
    test_ft.extract_manipulation(idx, plot=True)
    title = "Injected_"+str(idx)
    test_ft.save_fig(fig_dir='Figures/occupancy_figs/test_figs', title=title)

#################example db_mode
test_dbmode = plotOccupancy()
test_dbmode.setup_config()
test_dbmode.plot_gammas(record=113321806, title='Example NORM from Database')
test_dbmode.save_fig(fig_dir='Figures/occupancy_figs/test_figs',title='ExampleNORMfromDB')

#
# Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
# 
# OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
# 
# This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
# between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
# See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
#