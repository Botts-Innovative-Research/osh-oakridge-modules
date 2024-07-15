/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.ernie.vm250.tools;

import gov.llnl.ernie.data.Record;
import gov.llnl.ernie.manipulator.RecordOperator;
import gov.llnl.ernie.vm250.data.VM250Record;
import gov.llnl.ernie.vm250.data.VM250RecordInternal;
import gov.llnl.ernie.vm250.data.VM250VehicleMotion;
import gov.llnl.math.DoubleArray;
import gov.llnl.math.spline.CubicHermiteSpline;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;


public class VM250PreInjectionManipulation implements RecordOperator
{
  private double min;
  private double max;

  public void setLimits(double min, double max)
  {
    this.min = min;
    this.max = max;
  }
  
  /**
   * Since VM250 velocity readings appear to be unreliable,
   * add some random spread to velocities before injecting sources.
   * Should help train RF classifier to better handle uncertainty in vehicle velocity and length.
   * 
   * @param original
   * @param options: map, expected to contain keys "source", "seed" and "logger"
   * @return
   * @throws Exception 
   */
  @Override
  public Record apply(Record original, Map<String, Object> options) throws Exception
  {
    if (options.get("source") == null)
    {
      return original;
    }
    {
      try
      {
        VM250RecordInternal internal = (VM250RecordInternal)original.getInternal();
        VM250Record result = new VM250Record(VM250RecordInternal.duplicate(internal));
        result.setMotion(new VM250VehicleMotion((VM250VehicleMotion)original.getVehicleMotion()));
        result.setVehicleClass(original.getVehicleClass());

        double initialVelocity = original.getVehicleMotion().getVelocityInitial();
        int occupancyStart = original.getVPSMeasurement().getGammaOccupancyStart();
        int occupancyEnd = original.getVPSMeasurement().getGammaOccupancyEnd();
        
        Random rand2 = new Random((int)options.get("seed"));
        double[] velocityPickPoints = new double[3];
        for (int idx = 0; idx < 3; idx++)
        {
          velocityPickPoints[idx] = initialVelocity * (this.min + rand2.nextDouble() * (this.max - this.min));
        }
        double[] times = new double [] {occupancyStart, 0.5 * (occupancyStart+occupancyEnd), occupancyEnd};
        
        CubicHermiteSpline velocities = gov.llnl.math.spline.CubicHermiteSplineFactory.createNatural(times, velocityPickPoints);
        // now get motion profile from new record and modify it in-place:
        internal = (VM250RecordInternal)result.getInternal();
        VM250VehicleMotion motion = result.getVehicleMotion();
        
        double[] origPosition = motion.getPosition();
        double[] origVelocity = motion.getVelocity();
        
        motion.setVelocityInitial(velocityPickPoints[0]);
        motion.setVelocityFinal(velocityPickPoints[2]);
        double delta = 0;
        double dt = result.getGammaMeasurements().get(0).getSensorProperties().getSamplePeriodSeconds();
        for (int idx=0; idx<origPosition.length; idx++)
        {
          origVelocity[idx] = velocities.applyAsDouble(idx);
          delta += origVelocity[idx] * dt;
          origPosition[idx] = delta;
        }
        DoubleArray.addAssign(origPosition, -origPosition[occupancyStart]);
        motion.setVehicleLength( origPosition[occupancyEnd-1] );
        return result;
      }
      catch (Exception ex)
      {
        Logger logger = (Logger)options.get("logger");
        logger.log(Level.SEVERE, "Pre-manipulation modifications failed for record " + (int)options.get("seed"), ex);
        throw new Exception("Pre-manipulation modifications failed");
      }
    }
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