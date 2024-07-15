/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.ernie.rtk.view;

import gov.llnl.ernie.data.SensorView;
import gov.llnl.math.euclidean.Vector3;
import gov.llnl.math.euclidean.Vector3Ops;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author nelson85
 */
public class SensorViewEncounter
{
  public boolean dynamic = true;

  public static class Output
  {
    public SensorView sensor;
    public List<Instant> times;
    public double[] solidAngle;  // in solid angle
    public double[] sensorBearing; // direction to the sensor from prospective of source.
  }

  public boolean computeBearing = false;

  /**
   *
   * @param sensor is the sensor under simulation.
   * @param times is the boundaries for the times of the encounter
   * @param trace is the function of motion over the passby.
   * @return
   */
  public Output simulate(SensorView sensor, List<Instant> times, Trace trace)
  {
    int n = times.size();

    // Create an output structure
    Output output = new Output();
    output.sensor = sensor;
    output.solidAngle = new double[n - 1];
    output.times = times;
    if (computeBearing)
    {
      output.sensorBearing = new double[n - 1];
    }

    // Initialize state variables
    Vector3 pos0 = trace.get(times.get(0));
    Instant time0 = times.get(0);

    // For each interval
    for (int i = 1; i < n; i++)
    {
      Vector3 pos1 = trace.get(times.get(i));
      Instant time1 = times.get(i);

      // FIXME this should depend on the amount of angular change for the sensor
      double dt = Duration.between(time0, time1).toNanos() * 1e-9;
      if (dynamic)
      {
        Vector3 v0 = Vector3Ops.subtract(pos0, sensor.getOrigin());
        Vector3 v1 = Vector3Ops.subtract(pos1, sensor.getOrigin());
        double correlation = Vector3Ops.correlation(v0, v1);
        int parts2 = 2;
        if (correlation < 0.99995)
          parts2 = 3;
        if (correlation < 0.9995)
          parts2 = 7;
        if (correlation < 0.995)
          parts2 = 13;
        output.solidAngle[i - 1] = dt * integrate2(sensor, pos0, pos1, parts2);
//        System.out.println(correlation + "," + integrate2(sensor, pos0, pos1, parts2) + "," + integrate2(sensor, pos0, pos1, 50));
      }
      else
      {
        output.solidAngle[i - 1] = dt * integrate2(sensor, pos0, pos1, 9);
      }

      // Compute bearing if requested
      if (computeBearing)
      {
        Vector3 dv = Vector3Ops.subtract(pos1, pos0);
        output.sensorBearing[i - 1] = Bearing.compute(dv,
                Vector3Ops.subtract(sensor.getOrigin(), pos0));
      }

      // Update our position
      pos0 = pos1;
      time0 = time1;
    }
    return output;
  }

  double integrate(SensorView sensor, Vector3 pos0, Vector3 pos1, int parts)
  {
    if (parts == 0)
    {
      Vector3 mid = Vector3Ops.multiply(Vector3Ops.add(pos0, pos1), 0.5);
      return sensor.computeSolidAngle(mid);
    }

    parts *= 2;
    Vector3 dv = Vector3Ops.subtract(pos1, pos0);

    // Integrate with Simpson's rule
    double sum = sensor.computeSolidAngle(pos0);
    sum += sensor.computeSolidAngle(pos1);
    for (int j = 1; j < parts; j++)
    {
      double f = ((double) j) / (parts + 1);
      Vector3 v = Vector3Ops.add(pos0, Vector3Ops.multiply(dv, f));
      int coef = 2 << (j & 1);
      sum += coef * sensor.computeSolidAngle(v);
    }
    // (b-a)/3/n
    return sum / 3.0 / parts;
  }

  double integrate2(SensorView sensor, Vector3 pos0, Vector3 pos1, int parts)
  {
    Vector3 dv = Vector3Ops.subtract(pos1, pos0);

    // Integrate with Simpson's rule
    double sum = 0;
    for (int j = 0; j < parts; j++)
    {
      double f = (j + 0.5) / parts;
      Vector3 v = Vector3Ops.add(pos0, Vector3Ops.multiply(dv, f));
      sum += sensor.computeSolidAngle(v);
    }
    return sum / parts;
  }

  public List<Output> simulate(Iterable<SensorView> sensors, List<Instant> times, Trace trace)
  {
    ArrayList<Output> out = new ArrayList<>();
    for (SensorView sensor : sensors)
    {
      out.add(this.simulate(sensor, times, trace));
    }
    return out;
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