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

import gov.llnl.ernie.manipulator.ManipulationException;
import gov.llnl.ernie.vm250.data.VM250Record;
import gov.llnl.ernie.vm250.data.VM250RecordInternal;
import gov.llnl.ernie.vm250.data.VM250RecordInternal.PanelData;
import gov.llnl.ernie.vm250.data.VM250RecordInternal.VelocityReading;
import gov.llnl.math.Interpolator;
import gov.llnl.math.Smoothing;
import gov.llnl.math.matrix.Matrix;
import gov.llnl.math.matrix.MatrixColumnTable;
import gov.llnl.math.matrix.MatrixFactory;
import gov.llnl.math.matrix.MatrixViews;
import gov.llnl.math.random.PoissonRandom;

/**
 * Utilities for the record manipulator.
 *
 * Currently this just contains code related to altering the vehicle velocity.
 *
 * @author mattoon1
 */
public class VM250RecordManipulatorUtilities
{

  /**
   * (debug) Alters a record to change the speed during the encounter. This
   * method is not called directly, but rather through 
   * {@link #applyManipulation(gov.llnl.ernie.Record, gov.llnl.ernie.common.RecordManipulator.Manipulation) }
   *
   * @param record is the record to be altered.
   * @param vel1New is the new velocity at the start of the encounter
   * @param vel2New is the new velocity at the end of the encounter
   * @return a new record with the same meta data as the original.
   * @throws ManipulationException when the altered record has a bad motion
   * profile.
   */
  static public VM250Record alterRecordVelocity(VM250Record record, double vel1New, double vel2New) throws ManipulationException, ManipulationException
  {
    VM250RecordInternal internal = record.getInternal();
    if (vel1New != vel2New)
    {
      throw new UnsupportedOperationException("VM250 velocity manipulation with non-constant velocity");
    }
    double[] position;
    double[] positionNew;

    Matrix sampleOriginal;
    Matrix sampleSmoothed;
    Matrix sampleNew;

// We must seed the random number generator so that we produce the same numbers when we draw the same parameters
    PoissonRandom pr = new PoissonRandom();
    pr.setSeed(record.getContextualInfo().getScanID());

    int sz = internal.getLength();

    // FIXME hardcoded, should be read from lane configuration
    double vpsDistance = 0.2; // record.getLane().getVPSProperties().getBeamDistance();

// Step 1 extract current velocity profile
    double vehicleLength = record.getVehicleMotion().getVehicleLength();

    position = record.getVehicleMotion().getPosition();

// Step 2 compute new profile
    // Compute how many time segments we will need
    int frontPad = 0; // VM250 scans are inconsistent about whether they include pre-samples
    int rearPad = 0;
    double time1New = 0;
    double time2New = time1New+2*(vehicleLength+vpsDistance)/(vel1New+vel2New);
    int N = (int) (Math.ceil(time2New*5)+rearPad);
    if (N>1000)
    {
      throw new ManipulationException("Manipulated record would be too long");
    }
    positionNew = new double[N];

    VelocityPairs velocityProfileNew = new VelocityPairs();
    velocityProfileNew.add(time1New, vel1New);
    velocityProfileNew.add(time2New, vel2New);
    computeProfile(positionNew, velocityProfileNew);

// Step 3 Pack in new object
    VM250RecordInternal record2 = VM250RecordInternal.duplicate(internal);
    for (PanelData pd : record2.getPanelData())
    {
      pd.resize(N);
    }

// Step 4 Form new radiation data
    // Kludge for the position interplation so that the end points are
    // always covered.
    position[0] = -100;
    position[position.length-1] = 100;

    for (int i = 0; i<internal.getNPanels(); i++)
    {
      try
      {
        // Copy data into a matrix
        sampleOriginal = MatrixFactory.newMatrix(internal.getLength(), 1);
        for (int i2 = 0; i2<internal.getLength(); i2++)
        {
          sampleOriginal.set(i2, 0, internal.getPanelData()[i].gammaData[i2][0]);
        }

        // Apply smoothing
        sampleSmoothed = Smoothing.smooth(sampleOriginal, 2*2);
        sampleNew = interp1(position, sampleSmoothed, positionNew);

        for (int i1 = 0; i1<1; i1++)
        {
          for (int i2 = 0; i2<record2.getLength(); i2++)
          {
            record2.getPanelData()[i].gammaData[i2][i1] = pr.draw(sampleNew.get(i2, i1));
          }
        }
      } catch (Exception ex)
      {
        throw new ManipulationException(
                "Exception encountered when altering vehicle velocity: %s",
                ex);
      }
    }

    record2.getVelocities().clear();
    record2.getVelocities().add(record2.new VelocityReading(record2.getRpmDateTime(), vel1New));

    return new VM250Record(record2);
  }

  static Matrix interp1(double[] x, Matrix y, double[] x1) throws Exception
  {
    Interpolator interpolator = Interpolator.builder().clip().create();
    Matrix out = new MatrixColumnTable(x1.length, y.columns());
    double[] yv = new double[y.rows()];
    for (int i = 0; i<y.columns(); ++i)
    {
      y.copyColumnTo(yv, 0, i);
      double[] x2 = interpolator.interp(x, yv, x1);
      MatrixViews.selectColumn(out, i).assign(MatrixFactory.wrapColumnVector(x2));
    }
    return out;
  }

  static public void computeProfile(double[] position, VelocityPairs velocityProfile) throws ManipulationException
  {
    int N = position.length;
    if (velocityProfile.size()<2)
    {
      throw new ManipulationException("Velocity Profile irregular.");
    }

    double time1 = velocityProfile.get(0).time;
    double time2 = velocityProfile.get(1).time;
    double vel1 = velocityProfile.get(0).velocity;
    double vel2 = velocityProfile.get(1).velocity;

    double length = (vel1+vel2)/2*(time2-time1);
    for (int i = 0; i<N; ++i)
    {
      double time = (i+1)*0.2;
      if (time<time1)
      {
        position[i] = (time-time1)*vel1;
        //   velocity(i)=vel1;
      } else if (time>time2)
      {
        position[i] = (time-time2)*vel2+length;
        //   velocity(i)=vel2;
      } else
      {
        double delta = time-time1;
        double f = (time-time1)/(time2-time1);
        //   velocity(i)=vel1*(1-f)+f*vel2;
        position[i] = vel1*delta+(vel2-vel1)*delta*delta/2/(time2-time1);
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