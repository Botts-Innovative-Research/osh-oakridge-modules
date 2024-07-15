/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.ernie.vm250.data;

import java.io.Serializable;
import java.time.Instant;
import static java.util.Arrays.binarySearch;
import gov.llnl.ernie.data.VehicleMotion;
import java.time.Duration;

/**
 *
 * @author nelson85
 */
public class VM250VehicleMotion implements VehicleMotion, Serializable
{
  private static final long serialVersionUID
          = gov.llnl.utility.UUIDUtilities.createLong("VM250VehicleMotion-v1");

  // The location and velocity of the vehicle as a function of time
  private double[] position;
  double[] velocity;

  // The estimated vehicle length after conversions from time to distance
  private double vehicleLength;
  private double startTime;
  private double endTime;

  // The estimated motion parameters
  private double velocityInitial;
  private double velocityFinal;
  private Instant referenceTime;  

  public VM250VehicleMotion()
  {
  }
  
  public VM250VehicleMotion(VM250VehicleMotion orig)
  {
    if (orig.position != null)
    {
      position = orig.position.clone();
      velocity = orig.velocity.clone();
    }

    // The estimated vehicle length after conversions from time to distance
    vehicleLength = orig.vehicleLength;
    startTime = orig.startTime;
    endTime = orig.endTime;

    // The estimated motion parameters
    velocityInitial = orig.velocityInitial;
    velocityFinal = orig.velocityFinal;
    referenceTime = orig.referenceTime;  
  }
  
  @Override
  public boolean isGood()
  {
    for (double v : velocity)
    {
      if (v < 0)
      {
        return false;
      }
    }
    return true;
  }

  /**
   * Required by VehicleMotion interface, but always false for VM250
   * @return
   */
  @Override
  public boolean isReverse()
  {
    return false;
  }

  @Override
  public double getPosition(Instant time)
  {
    Duration delta = Duration.between(this.referenceTime, time);
    double d = delta.toNanos() * 1e-9 * 5; // units are fractional fifths of a second
    if (d < 0)
    {
      return position[0];
    }
    if (d >= position.length)
    {
      return position[position.length-1];
    }
    int i0 = (int) d;
    if (i0 + 1 == position.length)
    {
      i0--;
    }
    double f = d - i0;
    return position[i0] * (1 - f) + position[i0 + 1] * f;
  }

  @Override
  public double getVehicleLength()
  {
    return vehicleLength;
  }

  public double[] getVelocity()
  {
    return velocity;
  }
  
  @Override
  public double getVelocityInitial()
  {
    return this.velocityInitial;
  }

  @Override
  public double getVelocityFinal()
  {
    return this.velocityFinal;
  }

  /**
   * @return the position
   */
  public double[] getPosition()
  {
    return position;
  }

  public void setPosition(double[] position)
  {
    this.position = position;
  }

  public void setVelocity(double[] velocity)
  {
    this.velocity = velocity;
  }

  public void setVelocityInitial(double v0)
  {
    this.velocityInitial = v0;
  }

  public void setVelocityFinal(double v0)
  {
    this.velocityFinal = v0;
  }

  public void setStartTime(double startTime)
  {
    this.startTime = startTime;
  }

  public void setEndTime(double endTime)
  {
    this.endTime = endTime;
  }

  public double getStartTime()
  {
    return startTime;
  }

  public double getEndTime()
  {
    return endTime;
  }
  
  public void setReferenceTime(Instant t0)
  {
    this.referenceTime = t0;
  }

  /**
   * @param vehicleLength the vehicleLength to set
   */
  public void setVehicleLength(double vehicleLength)
  {
    this.vehicleLength = vehicleLength;
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