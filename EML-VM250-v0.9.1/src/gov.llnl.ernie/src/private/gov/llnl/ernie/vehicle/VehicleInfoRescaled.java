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

/**
 *
 * @author nelson85
 */
public class VehicleInfoRescaled implements VehicleInfo
{
  private static final long serialVersionUID = gov.llnl.utility.UUIDUtilities.createLong("VehicleInfoRescaled-v1");
  VehicleInfo base;
  double factor;
  transient final double[] features;

  public VehicleInfoRescaled(VehicleInfo vehicleClass, double vehicleLength, double[] features)
  {
    base = vehicleClass;
    factor = vehicleLength / vehicleClass.getVehicleLength();
    this.features = features;
  }

  @Override
  public int getId()
  {
    return base.getId();
  }

  @Override
  public int getType()
  {
    return base.getType();
  }

  @Override
  public double getVehicleLength()
  {
    return base.getVehicleLength() * factor;
  }

  @Override
  public double getCabLength()
  {
    return base.getCabLength() * factor;
  }

  @Override
  public int getNumTrailerAxels()
  {
    return base.getNumTrailerAxels();
  }

  @Override
  public double getPassengerAreaEnd()
  {
    return base.getPassengerAreaEnd() * factor;
  }

  @Override
  public double getPassengerAreaStart()
  {
    return base.getPassengerAreaStart() * factor;
  }

  @Override
  public double getPayloadAreaEnd()
  {
    return base.getPayloadAreaEnd() * factor;
  }

  @Override
  public double getPayloadAreaStart()
  {
    return base.getPayloadAreaStart() * factor;
  }

  @Override
  public double getTrailerEndFromRear()
  {
    return base.getTrailerEndFromRear() * factor;
  }

  @Override
  public double getTrailerStartFromRear()
  {
    return base.getTrailerStartFromRear() * factor;
  }

  @Override
  public boolean isBedOnly()
  {
    return base.isBedOnly();
  }

  @Override
  public boolean isWideAxel()
  {
    return base.isWideAxel();
  }

  @Override
  public boolean isHitchPresent()
  {
    return base.isHitchPresent();
  }

  @Override
  public boolean isLongHaul()
  {
    return base.isLongHaul();
  }

  @Override
  public boolean isTrailerPresent()
  {
    return base.isTrailerPresent();
  }

  @Override
  public double[] getFeatures()
  {
    return this.features;
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