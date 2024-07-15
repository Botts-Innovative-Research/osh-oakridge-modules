/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.ernie.vehicle;

import gov.llnl.ernie.data.VehicleInfo;

public class VehicleInfoImpl implements VehicleInfo
{
  private static final long serialVersionUID = gov.llnl.utility.UUIDUtilities.createLong("VehicleInfo-v1");
  private int id;
  private int type;
  private double vehicleLength = -1;
  private double passengerAreaStart; // passenger area, from front of vehicle
  private double passengerAreaEnd;
  private double payloadAreaStart; // payload area, from rear of vehicle
  private double payloadAreaEnd;
  private double trailerStartFromRear; // trailer area, from rear of vehicle
  private double trailerEndFromRear;
  private double cabLength;
  private boolean isLongHaul;
  private boolean trailerPresent = false;
  private boolean hitchPresent = false; // Low bed
  private boolean bedOnly = false;
  private int numTrailerAxels = -1;
  private boolean wideAxel = false;

  /**
   * @return the passengerAreaStart
   */
  @Override
  public double getPassengerAreaStart()
  {
    return passengerAreaStart;
  }

  /**
   * @return the passengerAreaEnd
   */
  @Override
  public double getPassengerAreaEnd()
  {
    return passengerAreaEnd;
  }

  /**
   * @return the payloadAreaStart
   */
  @Override
  public double getPayloadAreaStart()
  {
    return payloadAreaStart;
  }

  /**
   * @return the payloadAreaEnd
   */
  @Override
  public double getPayloadAreaEnd()
  {
    return payloadAreaEnd;
  }

  /**
   * @return the trailerStartFromRear
   */
  @Override
  public double getTrailerStartFromRear()
  {
    return trailerStartFromRear;
  }

  /**
   * @return the trailerEndFromRear
   */
  @Override
  public double getTrailerEndFromRear()
  {
    return trailerEndFromRear;
  }

  /**
   * @return the cabLength
   */
  @Override
  public double getCabLength()
  {
    return cabLength;
  }

  /**
   * @return the isLongHaul
   */
  @Override
  public boolean isLongHaul()
  {
    return isLongHaul;
  }

  /**
   * @return the trailerPresent
   */
  @Override
  public boolean isTrailerPresent()
  {
    return trailerPresent;
  }

  /**
   * @return the hitchPresent
   */
  @Override
  public boolean isHitchPresent()
  {
    return hitchPresent;
  }

  /**
   * @return the bedOnly
   */
  @Override
  public boolean isBedOnly()
  {
    return bedOnly;
  }

  /**
   * @return the numTrailerAxels
   */
  @Override
  public int getNumTrailerAxels()
  {
    return numTrailerAxels;
  }

  /**
   * @return the hasWideAxel
   */
  @Override
  public boolean isWideAxel()
  {
    return wideAxel;
  }

  @Override
  public int getId()
  {
    return id;
  }

  @Override
  public int getType()
  {
    return type;
  }

  @Override
  public double getVehicleLength()
  {
    return vehicleLength;
  }

  /**
   * @param id the id to set
   */
  public void setId(int id)
  {
    this.id = id;
  }

  /**
   * @param type the type to set
   */
  public void setType(int type)
  {
    this.type = type;
  }

  /**
   * @param vehicleLength the vehicleLength to set
   */
  public void setVehicleLength(double vehicleLength)
  {
    this.vehicleLength = vehicleLength;
  }

  /**
   * @param passengerAreaStart the passengerAreaStart to set
   */
  public void setPassengerAreaStart(double passengerAreaStart)
  {
    this.passengerAreaStart = passengerAreaStart;
  }

  /**
   * @param passengerAreaEnd the passengerAreaEnd to set
   */
  public void setPassengerAreaEnd(double passengerAreaEnd)
  {
    this.passengerAreaEnd = passengerAreaEnd;
  }

  /**
   * @param payloadAreaStart the payloadAreaStart to set
   */
  public void setPayloadAreaStart(double payloadAreaStart)
  {
    this.payloadAreaStart = payloadAreaStart;
  }

  /**
   * @param payloadAreaEnd the payloadAreaEnd to set
   */
  public void setPayloadAreaEnd(double payloadAreaEnd)
  {
    this.payloadAreaEnd = payloadAreaEnd;
  }

  /**
   * @param trailerStartFromRear the trailerStartFromRear to set
   */
  public void setTrailerStartFromRear(double trailerStartFromRear)
  {
    this.trailerStartFromRear = trailerStartFromRear;
  }

  /**
   * @param trailerEndFromRear the trailerEndFromRear to set
   */
  public void setTrailerEndFromRear(double trailerEndFromRear)
  {
    this.trailerEndFromRear = trailerEndFromRear;
  }

  /**
   * @param cabLength the cabLength to set
   */
  public void setCabLength(double cabLength)
  {
    this.cabLength = cabLength;
  }

  /**
   * @param isLongHaul the isLongHaul to set
   */
  public void setIsLongHaul(boolean isLongHaul)
  {
    this.isLongHaul = isLongHaul;
  }

  /**
   * @param trailerPresent the trailerPresent to set
   */
  public void setTrailerPresent(boolean trailerPresent)
  {
    this.trailerPresent = trailerPresent;
  }

  /**
   * @param hitchPresent the hitchPresent to set
   */
  public void setHitchPresent(boolean hitchPresent)
  {
    this.hitchPresent = hitchPresent;
  }

  /**
   * @param bedOnly the bedOnly to set
   */
  public void setBedOnly(boolean bedOnly)
  {
    this.bedOnly = bedOnly;
  }

  /**
   * @param numTrailerAxels the numTrailerAxels to set
   */
  public void setNumTrailerAxels(int numTrailerAxels)
  {
    this.numTrailerAxels = numTrailerAxels;
  }

  /**
   * @param wideAxel the hasWideAxel to set
   */
  public void setWideAxel(boolean wideAxel)
  {
    this.wideAxel = wideAxel;
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