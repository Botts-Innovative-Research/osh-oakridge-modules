/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gov.llnl.ernie.vm250.tools;

import gov.llnl.ernie.data.Lane;
import gov.llnl.ernie.internal.data.LaneImpl;
import gov.llnl.ernie.io.LaneDatabase;
import gov.llnl.ernie.vm250.ErnieVM250Package;
import gov.llnl.ernie.vm250.data.VM250Occupancy;
import gov.llnl.ernie.vm250.data.VM250Record;
import gov.llnl.ernie.vm250.data.VM250RecordInternal;
import gov.llnl.ernie.vm250.data.VM250RecordInternal.VelocityReading;
import gov.llnl.utility.io.ReaderException;
import gov.llnl.utility.xml.bind.DocumentReader;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import org.javatuples.Pair;
import org.javatuples.Triplet;

/**
 * This class help convert an occupancy data into a VM250Record.
 *
 * @author pham21
 */
public class VM250OccupancyConverter
{

  LaneDatabase lanes;
  boolean hasSetupInfo;
  int intervals;
  int occupancyHoldin;
  double nSigma;

  public VM250OccupancyConverter(Path vm250LaneDatabaseXmlPath, int intervals, int occupancyHoldin, double nSigma) throws ReaderException, IOException
  {
    this(vm250LaneDatabaseXmlPath);
    this.intervals = intervals;
    this.occupancyHoldin = occupancyHoldin;
    this.nSigma = nSigma;
    this.hasSetupInfo = true;
  }

  public VM250OccupancyConverter(Path vm250LaneDatabaseXmlPath) throws ReaderException, IOException
  {
    DocumentReader<LaneDatabase> docReader = DocumentReader.create(LaneDatabase.class);
    lanes = docReader.loadFile(vm250LaneDatabaseXmlPath);
  }

  public VM250Record toRecord(VM250Occupancy occupancy)
  {
    VM250RecordInternal record = new VM250RecordInternal();

    int[] gammaBackgroundArray;
    if (occupancy.getLastGammaBackgroundData() == null)
    {
      ErnieVM250Package.LOGGER.log(
              Level.WARNING, "Occupany is missing gamma background info!");
      gammaBackgroundArray = new int[]
      {
        -1, -1, -1, -1
      };
    }
    else
    {
      gammaBackgroundArray
              = occupancy.getLastGammaBackgroundData().getValue1().stream().mapToInt(i -> i).toArray();
    }

    record.setSegmentDescription(1, 1,
            occupancy.getStartTime(), occupancy.isContinuation(), gammaBackgroundArray,
            occupancy.getNeutronBackground(), occupancy.isGammaAlarm(),
            occupancy.isNeutronAlarm(), occupancy.isRealOccupancy());

    // Grabbing counts info
    // Each panel has an associated gamma and neutron data. 
    // For each row should have n data, with n equal to the number of panels.
    List<List<Integer>> gammaCountsList = new ArrayList<>(occupancy.getGammaDataList().size());
    List<List<Integer>> neutronCountsList = new ArrayList<>(occupancy.getNeutronDataList().size());

    for (Triplet<Instant, List<Integer>, Boolean> triplet : occupancy.getGammaDataList())
    {
      gammaCountsList.add(triplet.getValue1());
    }
    for (Triplet<Instant, List<Integer>, Boolean> triplet : occupancy.getNeutronDataList())
    {
      neutronCountsList.add(triplet.getValue1());
    }

    // Putting counts info into the panel objects
    for (int panelIndex = 0; panelIndex < record.getNPanels(); ++panelIndex)
    {
      VM250RecordInternal.PanelData panel = record.getPanelData()[panelIndex];
      panel.resize(gammaCountsList.size());
      panel.resize_neutron(neutronCountsList.size());

      // Grab gamma data associated with panel number equal to panelIndex
      for (int gammaIndex = 0; gammaIndex < gammaCountsList.size(); ++gammaIndex)
      {
        panel.gammaData[gammaIndex][0] = gammaCountsList.get(gammaIndex).get(panelIndex);
      }

      // Grab neutron data associated with panel number equal to panelIndex
      for (int neutronIndex = 0; neutronIndex < neutronCountsList.size(); ++neutronIndex)
      {
        panel.neutronData[neutronIndex][0] = neutronCountsList.get(neutronIndex).get(panelIndex);
      }
    }

    ArrayList<VelocityReading> velocities = new ArrayList<>();
    for (Pair<Instant, Double> pair : occupancy.getVelocityData())
    {
      velocities.add(record.new VelocityReading(pair.getValue0(), pair.getValue1()));
    }
    record.setVelocities(velocities);

    fetchLane(record);

    if (hasSetupInfo)
      record.setSetupInfo(intervals, occupancyHoldin, nSigma);   

    return new VM250Record(record);
  }

  private void fetchLane(VM250RecordInternal record)
  {
    // Mock-up Database, fetchLane:
    Lane lane = record.getLane();
    if (lane == null)
    {
      return;
    }

    LaneImpl laneImpl = (LaneImpl) lane;
    laneImpl.setLaneType("Primary");
    laneImpl.setLaneVector("Land Crossing");
    laneImpl.setLaneConveyance("Cargo");
    laneImpl.setLaneWidth(4.5);
    laneImpl.setRpmId(1L);
    laneImpl.setPortId(1L);
    laneImpl.setPanels(4);

    lanes.lookup(lane);
  }
}


/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */