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

/**
 *
 * @author cmattoon
 */
public class LaneId
{
  private long siteId;
  private long rpmId;

  public LaneId(long siteId, long rpmId)
  {
    this.siteId = siteId;
    this.rpmId = rpmId;
  }

  /**
   * @return the siteId
   */
  public long getSiteId()
  {
    return siteId;
  }

  /**
   * @param siteId the siteId to set
   */
  public void setSiteId(long siteId)
  {
    this.siteId = siteId;
  }

  /**
   * @return the rpmId
   */
  public long getRpmId()
  {
    return rpmId;
  }

  /**
   * @param rpmId the rpmId to set
   */
  public void setRpmId(long rpmId)
  {
    this.rpmId = rpmId;
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