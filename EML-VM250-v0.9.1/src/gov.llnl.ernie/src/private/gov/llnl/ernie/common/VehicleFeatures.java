/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.ernie.common;

import gov.llnl.ernie.analysis.Features;
import gov.llnl.ernie.analysis.FeaturesDescription;

/**
 * Features related to the type of vehicle.
 *
 * This will be computed differently depending on the RPM type.
 *
 * @author nelson85
 */
public class VehicleFeatures implements Features
{
  private static final long serialVersionUID = gov.llnl.utility.UUIDUtilities.createLong("VehicleFeatures-v1");
  // This will be assigned based on the vehicle feature extractor so 
  // that we only exposed features that are calculated for a particular 
  // instrument.
  final private FeaturesDescription description;

  private double vehicleLength;
  private double laneWidth;
  private double cabinSplit;
  private double cabinLength;
  private double trailerLength;
  private double cabinFraction;
  private double trailerFraction;
  private double overhang;
  private double cargoLength; 
  private double cabinOffset;
  private double gapLength;
  
  // For plotting
  private double velocityStart; // Absolute (not a feature)
  private double velocityEnd; // Absolute (not a feature)
  private double cargoCenter; // Absolute (not a feature)
  private double gapCenter; // Absolute (not a feature)
  private double tractorLength;
  private double vehicleStart;
  private double vehicleEnd;

  public VehicleFeatures(FeaturesDescription description)
  {
    this.description = description;
  }

  @Override
  public FeaturesDescription getDescription()
  {
    return description;
  }

  /**
   * @return the vehicleLength
   */
  public double getVehicleLength()
  {
    return this.vehicleLength;
  }

  /**
   * @param vehicleLength the vehicleLength to set
   */
  public void setVehicleLength(double vehicleLength)
  {
    this.vehicleLength = vehicleLength;
  }

  /**
   * @return the laneWidth
   */
  public double getLaneWidth()
  {
    return laneWidth;
  }

  /**
   * @param laneWidth the laneWidth to set
   */
  public void setLaneWidth(double laneWidth)
  {
    this.laneWidth = laneWidth;
  }

  /**
   * @return the cabinSplit
   */
  public double getCabinSplit()
  {
    return this.cabinSplit;
  }

  /**
   * @param cabinSplit the cabinSplit to set
   */
  public void setCabinSplit(double cabinSplit)
  {
    this.cabinSplit = cabinSplit;
  }

  /**
   * @return the cabinLength
   */
  public double getCabinLength()
  {
    return this.cabinLength;
  }

  /**
   * @param cabinLength the cabinLength to set
   */
  public void setCabinLength(double cabinLength)
  {
    this.cabinLength = cabinLength;
  }

  /**
   * @return the trailerLength
   */
  public double getTrailerLength()
  {
    return this.trailerLength;
  }

  /**
   * @param trailerLength the trailerLength to set
   */
  public void setTrailerLength(double trailerLength)
  {
    this.trailerLength = trailerLength;
  }

  /**
   * @return the cabinFraction
   */
  public double getCabinFraction()
  {
    return this.cabinFraction;
  }

  /**
   * @param cabinFraction the cabinFraction to set
   */
  public void setCabinFraction(double cabinFraction)
  {
    this.cabinFraction = cabinFraction;
  }

  /**
   * @return the trailerFraction
   */
  public double getTrailerFraction()
  {
    return trailerFraction;
  }

  /**
   * @param trailerFraction the trailerFraction to set
   */
  public void setTrailerFraction(double trailerFraction)
  {
    this.trailerFraction = trailerFraction;
  }

  /**
   * @return the overhang
   */
  public double getOverhang()
  {
    return this.overhang;
  }

  /**
   * @param overhang the overhang to set
   */
  public void setOverhang(double overhang)
  {
    this.overhang = overhang;
  }

  /**
   * @return the velocityStart
   */
  public double getVelocityStart()
  {
    return this.velocityStart;
  }

  /**
   * @param velocityStart the velocityStart to set
   */
  public void setVelocityStart(double velocityStart)
  {
    this.velocityStart = velocityStart;
  }

  /**
   * @return the velocityEnd
   */
  public double getVelocityEnd()
  {
    return this.velocityEnd;
  }

  /**
   * @param velocityEnd the velocityEnd to set
   */
  public void setVelocityEnd(double velocityEnd)
  {
    this.velocityEnd = velocityEnd;
  }

  /**
   * @return the trailerStart
   */
  public double getCargoLength()
  {
    return this.cargoLength;
  }

  /**
   * @param cargoLength the trailerStart to set
   */
  public void setCargoLength(double cargoLength)
  {
    this.cargoLength = cargoLength;
  }

  /**
   * @return the cabinOffset
   */
  public double getCabinOffset()
  {
    return cabinOffset;
  }

  /**
   * @param cabinOffset the cabinOffset to set
   */
  public void setCabinOffset(double cabinOffset)
  {
    this.cabinOffset = cabinOffset;
  }

  public void setCargoCenter(double cargoMid)
  {
    this.cargoCenter = cargoMid;
  }

  public void setVehicleStart(double start)
  {
    this.vehicleStart = start;
  }

  public void setVehicleEnd(double end)
  {
    this.vehicleEnd = end;
  }

  public void setGapCenter(double gapMid)
  {
    this.gapCenter = gapMid;
  }

  public void setGapLength(double gapLength)
  {
    this.gapLength= gapLength;
  }

  /**
   * @return the cargoCenter
   */
  public double getCargoCenter()
  {
    return cargoCenter;
  }

  /**
   * @return the gapCenter
   */
  public double getGapCenter()
  {
    return gapCenter;
  }

  /**
   * @return the gapLength
   */
  public double getGapLength()
  {
    return gapLength;
  }

  public void setTractorLength(double d)
  {
    this.tractorLength =d;
  }

  /**
   * @return the tractorLength
   */
  public double getTractorLength()
  {
    return tractorLength;
  }

  /**
   * @return the vehicleStart
   */
  public double getVehicleStart()
  {
    return vehicleStart;
  }

  /**
   * @return the vehicleEnd
   */
  public double getVehicleEnd()
  {
    return vehicleEnd;
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