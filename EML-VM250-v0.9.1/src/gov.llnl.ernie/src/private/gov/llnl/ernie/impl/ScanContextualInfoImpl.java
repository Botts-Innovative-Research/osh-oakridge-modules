/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.ernie.impl;

import gov.llnl.ernie.data.ScanContextualInfo;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

public class ScanContextualInfoImpl implements ScanContextualInfo
{
  long scanID; // segment descriptor 
  long portID;
  long siteID;
  long laneID; // RPM ID
  long vehicleID;
  int numSegments;

  // FIXME When injection ID is available in the internal record, assigned 
  // its value to injectionID
  long injectionID = 0;

  Instant timestmap;
  UUID segmentID;

  // Default ctor
  public ScanContextualInfoImpl()
  {
  }

  public ScanContextualInfoImpl(
          long scanID,
          long portID,
          long siteID,
          long laneID,
          long vehicleID,
          int numSegments,
          Instant dateTime,
          UUID segmentID)
  {
    this.scanID = scanID;
    this.portID = portID;
    this.siteID = siteID;
    this.laneID = laneID;
    this.vehicleID = vehicleID;
    this.numSegments = numSegments;
    this.timestmap = dateTime;
    this.segmentID = segmentID;
  }

  public ScanContextualInfoImpl(
          long scanID,
          long portID,
          long siteID,
          long laneID,
          long vehicleID,
          int numSegments,
          Instant dateTime)
  {
    this(
            scanID,
            portID,
            siteID,
            laneID,
            vehicleID,
            numSegments,
            dateTime,
            null);
  }

  @Override
  public long getScanID()
  {
    return this.scanID;
  }

  @Override
  public long getPortID()
  {
    return this.portID;
  }

  @Override
  public long getSiteID()
  {
    return this.siteID;
  }

  @Override
  public long getLaneID()
  {
    return this.laneID;
  }

  @Override
  public long getVehicleID()
  {
    return this.vehicleID;
  }

  @Override
  public Instant getTimestamp()
  {
    return this.timestmap;
  }

  /**
   * UUID Structure: AAAAAAAA-BBBB-CCCC-DDDD-EEEEEEEEEEEE
   *
   * AAAAAAAA: 
   *    - 8 digits long 
   *    - In the form of YYYYMMDD: 
   *        * Four digit year 
   *        * Two digit month 
   *        * Two digit day 
   *    - The year, month, and day comes from the RPM Date Time.
   *
   * BBBB: 
   *    - 4 digits long 
   *    - Is the RPM ID/lane ID
   *
   * CCCC: 
   *    - 4 digits long 
   *    - The vehicle ID mod 10000
   *
   * DDDD: 
   *    - 4 digits long 
   *    - Is the internal record's numSegments
   *
   * EEEEEEEEEEEE: 
   *    - 12 digits long 
   *    - First 9 digits come from the RPM Data Time:
   *        * Format: HHmmssSSS 
   *    - The last 3 digits come from injectionID value
   *
   * @return UUID
   */
  @Override
  public UUID getSegmentID()
  {
    if (segmentID != null)
    {
      return segmentID;
    }

    String UUIDstring = new SimpleDateFormat("yyyyMMdd").format(Date.from(timestmap));
    UUIDstring += String.format("-%04d", laneID);
    UUIDstring += String.format("-%04d", vehicleID % 10000);
    UUIDstring += String.format("-%04d", numSegments);
    UUIDstring += new SimpleDateFormat("-HHmmssSSS").format(Date.from(timestmap));
    UUIDstring += String.format("%03d", injectionID);
    return java.util.UUID.fromString(UUIDstring);
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