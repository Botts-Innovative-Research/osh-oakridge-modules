#
# Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
# 
# OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
# 
# This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
# between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
# See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
#

import datetime
import re

import numpy
import matplotlib
from matplotlib import pyplot

__doc__ = """
Support reading an VM250 data file into classes. One class represents a single occupancy,
and it in turn is stored inside a DailyActivity class that provides some extra utilities.

Update: now also supports dumping parsed results into an SQL server. See 'readVM250_dailies.py'
"""

gammaDelta = datetime.timedelta(0,0,200000)         # 200 ms
neutronDelta = datetime.timedelta(0,1,0)            # 1 s
minBuffer = datetime.timedelta(0,1,0)               # warn if records are closer together than 1 s
occupancyTimeout = datetime.timedelta(0,10,0)       # 10 s

gammaBackgroundInterval = datetime.timedelta(0,60,0)     # only save gamma background to database every 1 minute
neutronBackgroundInterval = datetime.timedelta(0,60,0)   # "" neutron background ""

backgroundTags = set(['NB','GB'])
errorTags = set(['GH','GL','NH','TC','TT'])
occTags = set(['SP','GS','NS','GA','NA','GX'])
spectraTags = set(['GS','NS','GA','NA'])
setupTags = set(['SG1','SG2','SG3','SN1','SN2'])
ignoredTags = set(['RW','AB','AR','AM','AA',
    'FP','TE','NNB',
    'IJ'])  # FIXME support reading IJ tags (with injected source details)?
allowedTags = set.union(backgroundTags, errorTags, occTags, setupTags, ignoredTags)

class occupancy:
    """
    Represents a single occupancy passing through VM250 portal
    """
    def __init__(self, occLines, continuation = False):
        self.starttime = occLines[0][-1]

        self.gammas = []
        self.neutrons = []
        self.velocities = []
        self.gammaAlarm = False
        self.neutronAlarm = False
        self.realOccupancy = False
        self.tamperFlag = False
        self.continuation = continuation
        self.lastGammaBackground = None
        self.lastNeutronBackground = None   # NB or NH record
        self.neutronBackground = -1         # from GX record

        gammaIndex = -5
        neutronIndex = 0
        for tag, vals, time in occLines:

            if tag == 'SP':
                if float(vals[0]) == 0: continue # bad velocity measurement, skip it and keep reading
                velocityMetersPerSecond = 0.3048/float(vals[0]) # listed value is in s/ft
                self.velocities.append( (time, velocityMetersPerSecond) )
            elif tag in ('GS','GA'):
                timeStamp = self.starttime + gammaIndex * gammaDelta
                gammaIndex += 1
                self.gammas.append( (timeStamp, list(map(int,vals)), tag=='GA') )
                if tag == 'GA': self.gammaAlarm = True
            elif tag in ('NS','NA'):
                timeStamp = self.starttime + neutronIndex * neutronDelta
                neutronIndex += 1
                self.neutrons.append( (timeStamp, list(map(int,vals)), tag=='NA') )
                if tag == 'NA': self.neutronAlarm = True
            elif tag == 'GX':
                self.index = int(vals[0])
                self.neutronBackground = float(vals[1])/1000
                self.railCarIndex = int(vals[2])
                self.endtime = time
                self.duration = self.endtime - self.starttime
                self.realOccupancy = True
            elif tag in ('TT','TC'):
                self.tamperFlag = True
            else:
                raise KeyError("Encountered unexpected tag '%s' in occupancy" % tag)

        if not self.realOccupancy:
            # GX flag not found
            self.endtime = time
            self.duration = self.endtime - self.starttime

    def __len__(self):
        return len(self.gammas)

    def plot(self, log=False):
        fig, ax = pyplot.subplots()
        colors = ['b','g','r','c']

        if log: pltcmd = ax.semilogy
        else: pltcmd = ax.plot

        times, counts, alarm = zip(*self.gammas)
        counts = numpy.array(counts)
        for i in range(4):
            pltcmd( times, counts[:,i], label="%d" % i, color=colors[i],
                    drawstyle='steps' )
        pltcmd( times, counts.sum(1), label="Sum", color='k',
                drawstyle='steps' )

        times, counts, alarm = zip(*self.neutrons)
        counts = numpy.array(counts)
        for i in range(4):
            pltcmd( times, counts[:,i], color=colors[i], linestyle='--',
                    drawstyle='steps' )
        pltcmd( times, counts.sum(1), color='k', linestyle='--',
                drawstyle='steps' )

        ax.legend()
        pyplot.gcf().autofmt_xdate()
        xfmt = matplotlib.dates.DateFormatter('%H:%M:%S')
        ax.xaxis.set_major_formatter(xfmt)
        title = "Occupancy #%d." % self.index
        if self.gammaAlarm: title += ' Gamma alarm!'
        if self.neutronAlarm: title += ' Neutron alarm!'
        fig.suptitle( title )


class VM250_dailyTraffic:
    """
    One day's worth of VM250 data
    """
    def __init__(self, VM250file, date, connection=None, occupancyId=1, siteId=1, laneId=1, verbose=True):
        self.version = None
        self.parameters = {}
        self.occupancyId = occupancyId
        self.siteId = siteId
        self.laneId = laneId

        self.neutronBackground = []
        self.gammaBackground = []
        self.tamperFlags = []

        self.occupancies = []
        self.neutronAlarms = []
        self.gammaAlarms = []

        # store SQL commits as list-of-lists, then commit multiple rows at a time using executemany()
        self.neutronBackgroundSQL = []
        self.gammaBackgroundSQL = []
        self.backgroundErrorsSQL = []
        self.tamperFlagsSQL = []
        self.occupanciesSQL = []
        self.gammasSQL = []
        self.neutronsSQL = []
        self.velocitiesSQL = []

        self.con = connection
        self.cur = None
        if connection is not None:
            self.cur = connection.cursor()

        with open(VM250file) as fin:
            datalines = fin.readlines()

        if len(datalines) == 0 or (len(datalines) == 1 and datalines[0].startswith('No data')):
            print("Warning: no data present!")
            return

        from collections import Counter
        (ncommas, count), = Counter( map(lambda f: f.count(','), datalines[:100]) ).most_common(1)
        if count < 95:
            if verbose:
                print("Warning: encountered many abnormal lines at start of file")

        nTimestamps = ncommas - 4
        if nTimestamps > 2:
            raise NotImplementedError("Found %d timestamps per line" % nTimestamps)
        if nTimestamps > 0:
            sampleTime = datalines[0].split(',')[-1]
            if ":" in sampleTime: dateFormat = "%H:%M:%S.%f"
            elif "-" in sampleTime: dateFormat = "%H-%M-%S.%f"
            else: raise ValueError("Unknown date format '%s'" % sampleTime)

        defaultTime = datetime.datetime.today()
        def makeTimeDelta( timestamp ):
            """
            Convert time to timedelta (latter can be added to another time)
            """
            dt = datetime.datetime.strptime( timestamp, dateFormat )
            return dt - datetime.datetime.strptime("0", "%H")

        setupLines = {}
        for line in datalines:
            if line[:3] in setupTags:
                tag = line[:3]
                if tag in setupLines:
                    #raise NotImplementedError("Warning: setup repeated!")
                    print(
                        "Warning: setup repeated!\n\t"
                        "Current unprocess setup: {}\n\t"
                        "Discarded setup data: {}".format(
                            setupLines,
                            line
                        )
                    )
                    continue
                if nTimestamps > 0:
                    time = date + makeTimeDelta( line.rstrip().split(',')[-1] )
                else:
                    time = defaultTime
                setupLines[tag] = line
                if len(setupLines) == 5:
                    self.parseSetup( setupLines, time )
                    setupLines = {}
        if self.version is None:
            print("Warning: no setup information found")
            self.parameters['Intervals'] = 5
            self.parameters['OccupancyHoldin'] = 10
            self.parameters['NSigma'] = 6

        inOccupancy = False
        lastOccupancyTime = -1
        occLines = []
        continuation = False

        lastGammaBG, lastNeutronBG = None, None
        time = defaultTime
        fiveSec = datetime.timedelta(seconds=5)
        pointTwoSec = datetime.timedelta(seconds=0.2)

        skipLine = False
        for idx in range(len(datalines)):
            if skipLine:
                skipLine = False
                continue

            line = datalines[idx]
            ncommas = line.count(',')

            if len(line) > 60 and not line.startswith('IJ'):
                print( "Warning: line #%d too long: %s" % (idx, line.rstrip()) )
                continue

            if ( (nTimestamps == 1 and ncommas != 5) or
                    (nTimestamps == 2 and ncommas != 6) or
                    (nTimestamps == 0 and ncommas != 4) ):
                if line[:3] not in setupTags:
                    # malformed line
                    if verbose:
                        print( "Warning: line #%d appears malformed: %s" % (idx,line.rstrip()) )
                    continue

            # use regex to check for invalid data (stars), non-numeric values
            # example: '*****' is invalid
            if re.search("\*+",line):
                print( "Warning: line #%d contains non-numeric values: %s" % (idx, line.rstrip()) )
                continue

            vals = line.rstrip().split(',')

            tag = vals[0]

            if not tag:     # sometimes comma and timestamp wrap over to start of the next line
                print("Warning: line #%d appears malformed: %s" % (idx, line.rstrip()) )
                continue

            if nTimestamps > 0:
                try:
                    tdelta = makeTimeDelta( vals[-1].strip() )
                    time = date + tdelta
                except ValueError:
                    print("Warning: couldn't extract date from string '%s'" % vals[-1])
                    # just use previous time stamp
            else:
                if tag=='GB':
                    time += fiveSec
                elif tag=='GS':
                    time += pointTwoSec

            if tag in occTags:
                inOccupancy = True
                occLines.append( [tag, vals[1:5], time] )
                lastOccupancyTime = time

                if tag == 'GX':     # normally signals end of occupancy

                    # check ahead one line for any more tags related to this occupancy:
                    nextValsIndex=idx+1
                    if nextValsIndex != len(datalines):
                        valsNext = datalines[nextValsIndex].rstrip().split(',')
                        if valsNext[0] in spectraTags:
                            if len(valsNext) > 5:   # need a timestamp for the next check
                                timeNext = date + makeTimeDelta( valsNext[-1] )
                                if time == timeNext:
                                    occLines.append( [valsNext[0], valsNext[1:5], timeNext] )
                                    skipLine = True

                    self.addOccupancy( occupancy(occLines, continuation) )
                    inOccupancy = False
                    occLines = []
                    continuation = False

                    if idx < len(datalines)-1:
                        nextvals = datalines[idx+1].split(',')
                        if nextvals[0] in occTags:
                            if nTimestamps > 0:
                                timestr = nextvals[-1].rstrip()
                                newtime = time + makeTimeDelta(timestr)
                                if newtime - time < minBuffer:
                                    continuation = True
                            else:
                                continuation = True # FIXME assumption

            else:
                if inOccupancy and time - lastOccupancyTime > occupancyTimeout:
                    # assume occupancy ended abnormally
                    self.addOccupancy( occupancy(occLines) )
                    inOccupancy = False
                    occLines = []
                    continuation = False

                if tag in ('NB','NH'):
                    vals = list(map(int,vals[1:5]))
                    self.neutronBackground.append( (time, vals, tag[1]) )

                    if any(vals) and self.con:   # otherwise this table is dominated by zeros
                        self.neutronBackgroundSQL.append( tuple([self.siteId, self.laneId, time] + vals) )
                        lastNeutronBG = time
                elif tag in ('GB','GH','GL'):
                    vals = list(map(int,vals[1:5]))
                    self.gammaBackground.append( (time, vals, tag[1]) )

                    if (lastGammaBG is None or
                            time - lastGammaBG > gammaBackgroundInterval) and self.con:
                        self.gammaBackgroundSQL.append( tuple([self.siteId, self.laneId, time] + vals) )
                        lastGammaBG = time
                elif tag in ('TC','TT'):
                    self.tamperFlags.append( (time, tag) )
                    if self.con:
                        self.tamperFlagsSQL.append( (self.siteId, self.laneId, time, tag) )
                    if inOccupancy: occLines.append( [tag, vals[1:-1], time] )
                elif tag in setupTags:
                    continue        # already dealt with in parseSetup
                elif tag in ignoredTags:
                    continue        # skipping for now
                else:
                    #raise NotImplementedError( "Encountered unexpected tag '%s'" % tag )
                    print(
                        "Warning: Encountered unexpected tag '{}'\n\t"
                        "line #{} contains unexpected tag : {}".format(
                            tag,
                            idx,
                            line.rstrip()
                        )
                    )
                    continue

        if self.cur:
            # commit all rows (except setup rows already done).
            def chunker(seq, size=1000):
                return (seq[pos:pos+size] for pos in range(0, len(seq), size))

            def batchInsert( table, rows ):
                if not rows: return
                nfields = len(rows[0])
                template = ','.join( ['?'] * nfields )
                header = "insert into %s values(%s) " % (table, template)
                for bidx, batch in enumerate( chunker(rows) ):
                    self.cur.executemany( header, batch )
                    self.con.commit()

            batchInsert( 'tamperFlags', self.tamperFlagsSQL )
            self.cur.fast_executemany = True
            batchInsert( 'neutronBackground', self.neutronBackgroundSQL )
            batchInsert( 'gammaBackground', self.gammaBackgroundSQL )
            batchInsert( 'occupancy', self.occupanciesSQL )
            batchInsert( 'gammas', self.gammasSQL )
            batchInsert( 'neutrons', self.neutronsSQL )
            batchInsert( 'velocities', self.velocitiesSQL )
            batchInsert( 'backgroundErrors', self.backgroundErrorsSQL )

            self.cur.close()

    def parseSetup( self, setupLines, time):
        """
        Parse information about software version and VM250 setup from dictionary.
        """
        assert len(setupLines) == 5

        # using regex to replace '***' with -1
        # in the raw data missing fields are mapped to '***'
        # our script mapped '***' to -1
        for key,value in setupLines.items():
            setupLines[key]=re.sub("\*+","-1",value)


        vtag = setupLines['SG2'].split(',')[6]
        version = setupLines['SG3'].split(',')[5]
        self.version = version + vtag
        vehicleVersions = ['1.10.1A', '1.20.1A', '1.20.2A']
        trainVersions = ['2.00.1T', '2.33.4A']
        if self.version not in vehicleVersions + trainVersions:
            raise NotImplementedError("Unrecognized version string '%s'" % self.version)

        vals = setupLines['SG1'].split(',')[1:]
        self.parameters['siteId'] = self.siteId
        self.parameters['laneId'] = self.laneId
        self.parameters['DateTime'] = time
        self.parameters['Version'] = self.version
        self.parameters['GammaBackgroundHighFault'] = int(vals[0])
        self.parameters['GammaBackgroundLowFault'] = int(vals[1])
        self.parameters['Intervals'] = int(vals[2])
        self.parameters['OccupancyHoldin'] = int(vals[3])
        self.parameters['NSigma'] = float(vals[4])

        vals = setupLines['SG2'].split(',')[1:]
        self.parameters['DetectorsOnLine'] = int(vals[0])
        self.parameters['MasterLowLevelDiscriminator'] = float(vals[1])
        self.parameters['MasterHighLevelDiscriminator'] = float(vals[2])
        self.parameters['RelayOutput'] = int(vals[3])
        self.parameters['Algorithm'] = vals[4]

        vals = setupLines['SG3'].split(',')[1:]
        self.parameters['SlaveLowLevelDiscriminator'] = float(vals[0])
        self.parameters['SlaveHighLevelDiscriminator'] = float(vals[1])
        self.parameters['BackgroundNSigma'] = float(vals[3])
        if self.version in vehicleVersions:
            self.parameters['SystemBackgroundTime'] = 0
            self.parameters['GammaBackgroundTime'] = int(vals[2])
        elif self.version in trainVersions:
            self.parameters['SystemBackgroundTime'] = int(vals[2])
            self.parameters['GammaBackgroundTime'] = 0

        vals = setupLines['SN1'].split(',')[1:]
        self.parameters['NeutronBackgroundHighFault'] = int(vals[0])
        self.parameters['NeutronMaxIntervals'] = int(vals[1])
        self.parameters['Alpha'] = int(vals[2])
        self.parameters['ZMax'] = int(vals[3])
        self.parameters['SequentialIntervals'] = int(vals[4])
        if self.version in vehicleVersions:
            self.parameters['NeutronBackgroundTime'] = int(vals[5])
        else:
            self.parameters['NeutronBackgroundTime'] = 0

        vals = setupLines['SN2'].split(',')[1:]
        self.parameters['NeutronMasterLowerLevelDiscriminator'] = float(vals[0])
        self.parameters['NeutronMasterUpperLevelDiscriminator'] = float(vals[1])
        self.parameters['NeutronSlaveLowerLevelDiscriminator'] = float(vals[2])
        self.parameters['NeutronSlaveUpperLevelDiscriminator'] = float(vals[3])

        sql = ''.join( ["insert into setup values({siteId}, {laneId}, '{DateTime}', '{Version}', ",
            "{GammaBackgroundHighFault}, {GammaBackgroundLowFault}, ",
            "{Intervals}, {OccupancyHoldin}, {NSigma}, {DetectorsOnLine}, ",
            "{MasterLowLevelDiscriminator}, {MasterHighLevelDiscriminator}, {RelayOutput}, '{Algorithm}', ",
            "{SlaveLowLevelDiscriminator}, {SlaveHighLevelDiscriminator}, {BackgroundNSigma}, {GammaBackgroundTime}, ",
            "{SystemBackgroundTime}, {NeutronBackgroundHighFault}, {NeutronMaxIntervals}, {Alpha}, {ZMax}, ",
            "{SequentialIntervals}, {NeutronBackgroundTime}, {NeutronMasterLowerLevelDiscriminator}, ",
            "{NeutronMasterUpperLevelDiscriminator}, {NeutronSlaveLowerLevelDiscriminator}, ",
            "{NeutronSlaveUpperLevelDiscriminator})"] )
        fullSql = sql.format(**self.parameters)
        #print(fullSql)

        if self.con:
            self.cur.execute( fullSql )
            self.con.commit()


    def addOccupancy( self, newOccupancy ):
        if len(self.gammaBackground) > 0:
            newOccupancy.lastGammaBackground = self.gammaBackground[-1]
        self.occupancies.append( newOccupancy )
        if newOccupancy.gammaAlarm:
            self.gammaAlarms.append( newOccupancy )
        if newOccupancy.neutronAlarm:
            self.neutronAlarms.append( newOccupancy )
        if not newOccupancy.realOccupancy:
            print( "Warning: Occupancy %d at %s likely not real" % (len(self.occupancies),
                    newOccupancy.starttime) )

        if self.cur:
            if newOccupancy.lastGammaBackground is not None:
                lastGB, GBflag = newOccupancy.lastGammaBackground[1:]
            else:
                lastGB = [-1] * 4
                GBflag = 'B'

            if newOccupancy.lastNeutronBackground is not None:
                lastNB, NBflag = newOccupancy.lastNeutronBackground[1:]
            else:
                NBflag = 'B'

            self.occupanciesSQL.append( tuple(
                    [self.occupancyId, self.siteId, self.laneId, newOccupancy.starttime, newOccupancy.continuation] + lastGB +
                    [newOccupancy.gammaAlarm, newOccupancy.neutronBackground, newOccupancy.neutronAlarm, newOccupancy.realOccupancy] ) )

            if GBflag != 'B' or NBflag != 'B':
                self.backgroundErrorsSQL.append( tuple([self.occupancyId, int(GBflag=='H'), int(GBflag=='L'), int(NBflag=='H')]) )

            for idx, gammaRecord in enumerate(newOccupancy.gammas):
                counts = gammaRecord[1]
                isAlarm = gammaRecord[2]
                self.gammasSQL.append( tuple([self.occupancyId, idx] + counts + [isAlarm]) )

            for idx, neutronRecord in enumerate(newOccupancy.neutrons):
                counts = neutronRecord[1]
                isAlarm = neutronRecord[2]
                self.neutronsSQL.append( tuple([self.occupancyId, idx] + counts + [isAlarm]) )

            for velocityRecord in newOccupancy.velocities:
                self.velocitiesSQL.append( tuple([self.occupancyId] + list(velocityRecord)) )

        self.occupancyId += 1


    def plotAlarms(self):
        for occ in self.occupancies:
            if occ.gammaAlarm or occ.neutronAlarm:
                occ.plot()

        pyplot.ion()
        pyplot.show()

    def plotGammaBackground(self):
        import numpy
        import matplotlib
        from matplotlib import pyplot
        times,vals,flags = zip(*self.gammaBackground)
        vals = numpy.array(vals)
        pyplot.figure()
        pyplot.plot(times, vals.sum(1))
        pyplot.ylabel("Gamma cps in all panels")


if __name__ == '__main__':
    import sys
    dailyFile = VM250_dailyTraffic( sys.argv[1], date=datetime.datetime.today() )


#
# Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
# 
# OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
# 
# This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
# between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
# See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
#