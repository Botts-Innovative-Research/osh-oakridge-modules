/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.ernie.internal.io;

import gov.llnl.ernie.data.Lane;
import gov.llnl.ernie.io.LaneDatabase;
import gov.llnl.utility.xml.bind.ReaderInfo;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author nelson85
 */
@ReaderInfo(LaneDatabaseReader.class)
public class LaneDatabaseImpl implements LaneDatabase
{
  // List of lane definitions
  List<LaneDefinition> defaultLanes = new ArrayList<>();
  List<LaneDefinition> configuredLanes = new ArrayList<>();
  Map<Long, Integer> laneErrorsMap = new HashMap<>();
  
  @Override
  public void lookup(Lane lane2)
  {
    for ( LaneDefinition ld: this.configuredLanes)
    {
      if (ld.matches(lane2))
      {
        ld.apply(lane2);
        return;
      }
    }
    for ( LaneDefinition ld: this.defaultLanes)
    {
      if (ld.matches(lane2))
      {
        ld.apply(lane2);
        if (!configuredLanes.isEmpty())
        {
          Long locationHash = 1000 * lane2.getPortId() + lane2.getRpmId();
          if (!laneErrorsMap.containsKey(locationHash))
          {
            laneErrorsMap.put(locationHash, 0);
          }
          if (laneErrorsMap.get(locationHash) < 5)
          {
            Logger.getLogger(LaneDefinition.class.getName()).log(Level.WARNING,
              String.format("No match for siteId %d / rpmId %d found in lane database."
                      + " Using default lane.",
                      lane2.getPortId(), lane2.getRpmId()));
            laneErrorsMap.put(locationHash, laneErrorsMap.get(locationHash)+1);
          }
        }
        return;
      }
    }
    throw new RuntimeException("Unable to find lane definition");
  }

  public void add(LaneDefinition ld)
  {
    this.defaultLanes.add(ld);
  }
  
  /**
   * 
   * @param lanesFile
   * @throws FileNotFoundException
   * @throws IOException 
   */
  public void loadLanes(Path lanesFile) throws FileNotFoundException, IOException
  {
    BufferedReader in = new BufferedReader(new FileReader(lanesFile.toFile()));
    String line = in.readLine();  // skip header
    String[] values;
    while ((line = in.readLine()) != null)
    {
      values = line.split(",");
      String siteLabel = values[0];
      int siteId = Integer.parseInt(values[1]);
      int rpmId = Integer.parseInt(values[2]);
      double width = Double.parseDouble(values[3]);

      String collimated = values[4];
      LaneDefinition templateLane = null;
      for (LaneDefinition tmp : this.defaultLanes)
      {
        if (collimated.equals("y") && tmp.collimated)
        {
          templateLane = tmp;
        }
        // beware: some lane configurations claim collimation status is unknown
        else if ((collimated.equals("n") || collimated.equals("?")) && !tmp.collimated)
        {
          templateLane = tmp;
        }
      }
      if (templateLane == null)
      {
        throw new RuntimeException(String.format(
                "No appropriate template found for lane with collimated = '%s' in %s",
                collimated, lanesFile));
      }
      
      LaneDefinition newLane = new LaneDefinition(templateLane);
      newLane.laneId = new LaneId(siteId, rpmId);
      newLane.laneWidth = width;
      
      // put more specific lanes at beginning of the list
      this.configuredLanes.add(0, newLane);
    }
  }

  /**
   * @return the defaultLanes 
   */
  public List<LaneDefinition> getDefaultLane(){
    return defaultLanes;
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