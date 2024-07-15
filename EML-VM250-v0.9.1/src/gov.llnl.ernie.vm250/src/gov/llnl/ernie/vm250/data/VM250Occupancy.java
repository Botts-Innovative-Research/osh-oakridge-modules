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
package gov.llnl.ernie.vm250.data;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.javatuples.Pair;
import org.javatuples.Triplet;

/**
 * Represent an Occupancy.
 *
 * @author pham21
 */
public class VM250Occupancy
{
// Fields obtained by reverse engineered VM250_daily.py script.
  // FIXME Remove python notes once DailyFileLoader is completed and working.
  private boolean continuation;         // continuation                           
  private boolean gammaAlarm;           // gammaAlarm
  private boolean neutronAlarm;         // neutronAlarm
  private boolean realOccupancy;        // realOccupancy
  private boolean tamper;               // tamperFlag
  private int index;                    // index
  private int railCarIndex;             // railCarIndex
  private double neutronBackground;     // neutronBackground

  private Duration duration;            // duration; Equivalent to Python's timedelta    
  private Instant startTime;            // starttime
  private Instant endTime;              // endtime

  private List<Triplet<Instant, List<Integer>, Boolean>> gammaDataList;     // gammas
  private List<Triplet<Instant, List<Integer>, Boolean>> neutronDataList;   // neutrons
  private List<Pair<Instant, Double>> velocityData;                         // velocities  
  private Triplet<Instant, List<Integer>, String> lastGammaBackgroundData;  // lastGammaBackground

  // lastNeutronBackground is an unknown type . In the Python script, called VM250_daily.py, 
  // lastNeutronBackground is used for NB or NH record and the script
  // doesn't use lastNeutronBackground
  
  private VM250Occupancy()
  {
  }

  public static VM250Occupancy of(List<Triplet<String, List<String>, Instant>> parsedOccupancyDataList) throws UnsupportedOperationException
  {
    return of(parsedOccupancyDataList, false);
  }

  public static VM250Occupancy of(List<Triplet<String, List<String>, Instant>> parsedOccupancyDataList, boolean continuation) throws UnsupportedOperationException
  {

    final long GAMMA_DELTA = 200L; // Represent 200 ms
    final long NEUTRON_DELTA = 1L; // Represent 1 s

    VM250Occupancy out = new VM250Occupancy();

    out.continuation = continuation;
    out.neutronBackground = -1;
    out.startTime = parsedOccupancyDataList.get(0).getValue2();
    out.gammaDataList = new ArrayList<>();
    out.neutronDataList = new ArrayList<>();
    out.velocityData = new ArrayList<>();

    long gammaIndex = -5L;
    int neutronIndex = 0;

    for (Triplet<String, List<String>, Instant> triplet : parsedOccupancyDataList)
    {
      String tag = triplet.getValue0();
      List<String> valueStrList = triplet.getValue1();
      Instant time = triplet.getValue2();

      if (tag.equals("SP"))
      {
        double doubleValue = Double.parseDouble(valueStrList.get(0));
        if (doubleValue == 0.0)
        {
          // Bad velocity measurement, skip it and keep reading
          continue;
        }
        double velocityMetersPerSecond = 0.3048 / doubleValue; // listed value is in s/ft
        out.velocityData.add(Pair.with(time, velocityMetersPerSecond));
      }
      else if (tag.equals("GS") || tag.equals("GA"))
      {
        boolean isGammaAlarm = tag.equals("GA");
        if (isGammaAlarm)
        {
          out.gammaAlarm = isGammaAlarm;        
        }
        Instant timeStamp = out.startTime.plus(gammaIndex * GAMMA_DELTA, ChronoUnit.MILLIS);
        ++gammaIndex;
        out.gammaDataList.add(Triplet.with(
                timeStamp,
                valueStrList.stream().map(s -> Integer.parseInt(s)).collect(Collectors.toList()),
                isGammaAlarm
        ));
      }
      else if (tag.equals("NS") || tag.equals("NA"))
      {
        boolean isNeutronAlarm = tag.equals("NA");
        if (isNeutronAlarm)
        {
          out.neutronAlarm = isNeutronAlarm;
        }
        Instant timeStamp = out.startTime.plus(neutronIndex * NEUTRON_DELTA, ChronoUnit.SECONDS);
        ++neutronIndex;
        out.neutronDataList.add(Triplet.with(
                timeStamp,
                valueStrList.stream().map(s -> Integer.parseInt(s)).collect(Collectors.toList()),
                isNeutronAlarm
        ));
      }
      else if (tag.equals("GX"))
      {
        out.index = Integer.parseInt(valueStrList.get(0));
        out.neutronBackground = Double.parseDouble(valueStrList.get(1)) / 1000.0;
        out.railCarIndex = Integer.parseInt(valueStrList.get(2));
        out.endTime = time;
        out.duration = Duration.between(out.startTime, out.endTime);
        out.realOccupancy = true;
      }
      else if (tag.equals("TT") || tag.equals("TC"))
      {
        out.tamper = true;
      }
      else
      {
        throw new UnsupportedOperationException(String.format("Encountered unexpected tag '%s' in occupancy", tag));
      }
    }

    if (!out.realOccupancy)
    {
      // GX flag not found
      // Get the last timestame
      out.endTime = parsedOccupancyDataList.get(parsedOccupancyDataList.size() - 1).getValue2();
      out.duration = Duration.between(out.startTime, out.endTime);
    }

    return out;
  }

  /**
   * @return the continuation
   */
  public boolean isContinuation()
  {
    return continuation;
  }

  /**
   * @return the gammaAlarm
   */
  public boolean isGammaAlarm()
  {
    return gammaAlarm;
  }

  /**
   * @return the neutronAlarm
   */
  public boolean isNeutronAlarm()
  {
    return neutronAlarm;
  }

  /**
   * @return the realOccupancy
   */
  public boolean isRealOccupancy()
  {
    return realOccupancy;
  }

  /**
   * @return the tamper
   */
  public boolean isTamper()
  {
    return tamper;
  }

  /**
   * @return the index
   */
  public int getIndex()
  {
    return index;
  }

  /**
   * @return the railCarIndex
   */
  public int getRailCarIndex()
  {
    return railCarIndex;
  }

  /**
   * @return the neutronBackground
   */
  public double getNeutronBackground()
  {
    return neutronBackground;
  }

  /**
   * @return the duration
   */
  public Duration getDuration()
  {
    return duration;
  }

  /**
   * @return the startTime
   */
  public Instant getStartTime()
  {
    return startTime;
  }

  /**
   * @return the endTime
   */
  public Instant getEndTime()
  {
    return endTime;
  }

  /**
   * @return the gammaDataList
   */
  public List<Triplet<Instant, List<Integer>, Boolean>> getGammaDataList()
  {
    return gammaDataList;
  }

  /**
   * @return the neutronDataList
   */
  public List<Triplet<Instant, List<Integer>, Boolean>> getNeutronDataList()
  {
    return neutronDataList;
  }

  /**
   * @return the lastGammaBackgroundData
   */
  public Triplet<Instant, List<Integer>, String> getLastGammaBackgroundData()
  {
    return lastGammaBackgroundData;
  }

  /**
   * @return the velocityData
   */
  public List<Pair<Instant, Double>> getVelocityData()
  {
    return velocityData;
  }

  /**
   * @return the size of the gamma data
   */
  public int size()
  {
    return gammaDataList.size();
  }

  /**
   * @param lastGammaBackgroundData the lastGammaBackgroundData to set
   */
  public void setLastGammaBackgroundData(Triplet<Instant, List<Integer>, String> lastGammaBackgroundData)
  {
    this.lastGammaBackgroundData = lastGammaBackgroundData;
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