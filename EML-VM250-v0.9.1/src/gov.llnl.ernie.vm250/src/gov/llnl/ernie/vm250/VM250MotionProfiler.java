/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.ernie.vm250;

import gov.llnl.ernie.Analysis;
import gov.llnl.ernie.analysis.AnalysisPreprocessor;
import gov.llnl.ernie.analysis.AnalysisException;
import gov.llnl.ernie.data.Record;
import gov.llnl.ernie.vm250.data.VM250RecordInternal;
import gov.llnl.utility.InitializeException;
import gov.llnl.ernie.data.VehicleMotion;
import gov.llnl.ernie.vm250.data.VM250Record;
import gov.llnl.ernie.vm250.data.VM250RecordInternal.VelocityReading;
import gov.llnl.ernie.vm250.data.VM250VehicleMotion;
import gov.llnl.utility.xml.bind.Reader;
import java.time.Instant;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author nelson85
 */
@Reader.Declaration(pkg = ErnieVM250Package.class, name = "motionProfiler",
        referenceable = true)
public class VM250MotionProfiler implements AnalysisPreprocessor
{
  private double DEFAULT_VELOCITY = 2.0;      // velocities all in m/s
  private double MIN_CREDIBLE_VELOCITY = 0.2;
  private double MAX_CREDIBLE_VELOCITY = 10;
  private double VELOCITY_FALLBACK_THRESHOLD = -1;

  @Override
  public void initialize(Analysis par0) throws InitializeException
  {
    //not used
  }

  @Override
  public void compute(Record record)
  {
    VM250Record recordImpl = (VM250Record) record;
    VM250RecordInternal recordInternal = (VM250RecordInternal) record.getInternal();

    // Last extract the vehicle motion
    VM250VehicleMotion motion = this.extract(record);
    recordImpl.setMotion(motion);

    // Verify it is successful.
    VehicleMotion mpr = record.getVehicleMotion();
    if (mpr == null || !mpr.isGood())
    {
      record.addFault(VM250Record.Fault.BAD_VEHICLE_MOTION);
    }
  }

  public VM250VehicleMotion extract(Record record)
  {
    if (record.getLane() == null)
    {
      return null;
    }
    VM250Record recordImpl = (VM250Record) record;
    VM250RecordInternal recordInternal = (VM250RecordInternal) record.getInternal();
    VM250VehicleMotion result = new VM250VehicleMotion();

    // Scan through velocity readings
    double bestVelocity;
    if (recordInternal.getVelocities().isEmpty())
    {
      bestVelocity = DEFAULT_VELOCITY; // approximate mean of velocity distribution
      Logger.getLogger(VM250MotionProfiler.class.getName()).log(
              Level.WARNING, String.format(
                      "No velocity reading for scan, assuming %f m/s.", DEFAULT_VELOCITY));
      // not currently sending to fallback:
      //record.addFault(VM250Record.Fault.MISSING_VELOCITY);
    }
    else
    {
      bestVelocity = 10000;
      for (VelocityReading vr : recordInternal.getVelocities())
      {
        if ((vr.velocity < bestVelocity) &
                (vr.velocity > MIN_CREDIBLE_VELOCITY) &
                (vr.velocity < MAX_CREDIBLE_VELOCITY))
        {
          bestVelocity = vr.velocity;
        }
      }
      if (bestVelocity == 10000)
      {
        bestVelocity = DEFAULT_VELOCITY;
        Logger.getLogger(VM250MotionProfiler.class.getName()).log(
                Level.WARNING, String.format(
                        "No reliable velocity reading for scan, assuming %f m/s.", DEFAULT_VELOCITY));
        // not currently sending to fallback:
        //record.addFault(VM250Record.Fault.MISSING_VELOCITY);
      }
    }
    recordImpl.bestVelocity = bestVelocity;

    if ((VELOCITY_FALLBACK_THRESHOLD > 0) & (bestVelocity > VELOCITY_FALLBACK_THRESHOLD))
    {
      record.addFault(VM250Record.Fault.BAD_VELOCITY);
    }

    double v0 = bestVelocity;
    if (v0 != -1)
    {
      int N = recordInternal.getLength();
      double dt = recordInternal.getLane().getGammaSensorProperties().get(0).getSamplePeriodSeconds();
      double startTime = dt * recordInternal.getSetupInfo().getIntervals();
      double endTime = dt * (N - recordInternal.getSetupInfo().getOccupancyHoldin());
      result.setStartTime(startTime);
      result.setEndTime(endTime);
      
      double[] position = new double[N];
      double[] velocity = new double[N];
      for (int i1 = 0; i1 < N; ++i1)
      {
        double time = (i1 + 1) * dt;
        double deltaT = time - result.getStartTime();

        position[i1] = v0 * deltaT;
        velocity[i1] = v0;
      }
      result.setPosition(position);
      result.setVelocity(velocity);
      result.setVelocityInitial(v0);
      result.setVelocityFinal(v0);
      Instant t0 = record.getGammaMeasurements().get(0).getTime(0);
      result.setReferenceTime(t0);

      result.setVehicleLength( v0 * (endTime - startTime) );
    }
    else
    {
      Logger.getLogger(VM250MotionProfiler.class.getName()).log(Level.WARNING,
              String.format("Can't extract motionProfile for record %d (no velocity info)",
                      recordInternal.getOccupancyId()));
    }
    return result;
  }

  /**
   * Set default velocity. This velocity will be assigned to any scans that
   * have no 'SP' readings within a credible range.
   * 
   * @param DEFAULT_VELOCITY the DEFAULT_VELOCITY to set
   */
  @Reader.Attribute(name = "defaultVelocity", type = double.class, required = false)
  public void set_DEFAULT_VELOCITY(double DEFAULT_VELOCITY)
  {
    this.DEFAULT_VELOCITY = DEFAULT_VELOCITY;
  }
  
  /**
   * Set minimum credible velocity. Velocity readings below this value are ignored.
   * 
   * @param value the MIN_CREDIBLE_VELOCITY to set
   */
  @Reader.Attribute(name = "minCredibleVelocity", type = double.class, required = false)
  public void set_MIN_CREDIBLE_VELOCITY(double value)
  {
    this.MIN_CREDIBLE_VELOCITY = value;
  }

  /**
   * Set maximum credible velocity. Velocity readings above this value are ignored.
   * 
   * @param value the MAX_CREDIBLE_VELOCITY to set
   */
  @Reader.Attribute(name = "maxCredibleVelocity", type = double.class, required = false)
  public void set_MAX_CREDIBLE_VELOCITY(double value)
  {
    this.MAX_CREDIBLE_VELOCITY = value;
  }

  /**
   * Set velocity fallback threshold. If the threshold is positive and
   * minimum credible velocity is above the threshold, send scan to fallback.
   * 
   * @param value the MAX_CREDIBLE_VELOCITY to set
   */
  @Reader.Attribute(name = "velocityFallbackThreshold", type = double.class, required = false)
  public void set_VELOCITY_FALLBACK_THRESHOLD(double value)
  {
    this.VELOCITY_FALLBACK_THRESHOLD = value;
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