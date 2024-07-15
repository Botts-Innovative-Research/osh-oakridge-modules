/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.ernie.data;

import gov.llnl.math.DoubleArray;
import gov.llnl.math.IntegerArray;
import java.time.Instant;

/**
 * Measurements taken from a sensor.
 *
 * It is assumed that all measurements for a given detector type will operate in
 * lock step and each device will produce the same number of samples during a
 * scan. Sensors for different radiation types can have different sampling
 * rates.
 *
 */
public interface SensorMeasurement
{
  /**
   * Get the properties of the sensor.
   *
   * @return the sensor properties.
   */
  SensorProperties getSensorProperties();

  /**
   * Get the total number of samples in this measurement.
   *
   * @return the number of sample periods recorded for this sensor.
   */
  int size();

  /**
   * Get the sample number for the start of the occupancy.
   *
   * @return the sample number during which the vehicle enters.
   */
  int getOccupancyStart();

  /**
   * Get the sample number for the end of the occupancy.
   *
   * @return the sample number in which the vehicle exits.
   */
  int getOccupancyEnd();

  /**
   * Get the time for each sample.
   *
   * Times are used to match between different sensor types and convert into
   * vehicle position.
   *
   * @param sampleIndex is the sample number from the start of the scan.
   * @return the measurement time.
   * @throws ArrayIndexOutOfBoundsException for an invalid sample index.
   */
  Instant getTime(int sampleIndex) throws ArrayIndexOutOfBoundsException;

  /**
   * Get the measured counts as a function of the sample index.
   *
   * @param sampleIndex is the sample number from the start of the scan.
   * @return the counts at that sample.
   * @throws ArrayIndexOutOfBoundsException for an invalid sample index.
   */
  int getCounts(int sampleIndex) throws ArrayIndexOutOfBoundsException;

  /**
   * Get a copy of the expected gross counts from specified time period.
   *
   * @param sampleIndexBegin is the start of the range in samples (inclusive).
   * @param sampleIndexEnd is the end of the range in samples (exclusive).
   * @return a vector of gross counts from [sampleIndexBegin, sampleIndexEnd)
   * @throws ArrayIndexOutOfBoundsException for an invalid sample index.
   */
  int[] getCountsRange(int sampleIndexBegin, int sampleIndexEnd) throws ArrayIndexOutOfBoundsException;

  /**
   * Get a copy of the expected gross counts from specified time period.
   *
   * @param sampleIndexBegin is the start of the range (inclusive).
   * @param sampleIndexEnd is the end of the range (exclusive).
   * @return a vector of gross counts from [sampleIndexBegin, sampleIndexEnd)
   * @throws ArrayIndexOutOfBoundsException for an invalid sample index.
   */
  default double getCountsSum(int sampleIndexBegin, int sampleIndexEnd) throws ArrayIndexOutOfBoundsException
  {
    return IntegerArray.sum(this.getCountsRange(sampleIndexBegin, sampleIndexEnd));
  }

  /**
   * Get the average rate over a period of time.
   *
   * @param sampleIndexBegin is the start of the range in samples (inclusive).
   * @param sampleIndexEnd is the end of the range in samples (exclusive).
   * @return the count rate in counts per second.
   * @throws ArrayIndexOutOfBoundsException for an invalid sample index.
   */
  default double getCountRate(int sampleIndexBegin, int sampleIndexEnd) throws ArrayIndexOutOfBoundsException
  {
    return getCountsSum(sampleIndexBegin, sampleIndexEnd)
            / (sampleIndexEnd - sampleIndexBegin)
            / this.getSensorProperties().getSamplePeriodSeconds();
  }

  /**
   * Get the expected spectrum for a particular time slice.
   *
   * @param sampleIndex
   * @return a spectrum for the timeslice or null if not a spectral portal.
   * @throws ArrayIndexOutOfBoundsException for an invalid sample index.
   */
  int[] getSpectrum(int sampleIndex) throws ArrayIndexOutOfBoundsException;

  /**
   * Get a copy of the spectral data from a specified time period.
   *
   * @param sampleIndexBegin is the start of the range in samples (inclusive).
   * @param sampleIndexEnd is the end of the range in samples (exclusive).
   * @return a vector of spectrum from [sampleIndexBegin, sampleIndexEnd)
   * @throws ArrayIndexOutOfBoundsException for an invalid sample index.
   */
  int[][] getSpectrumRange(int sampleIndexBegin, int sampleIndexEnd) throws ArrayIndexOutOfBoundsException;

  /**
   * Get the sum of the spectrum over a range of samples.
   *
   * @param sampleIndexBegin is the start of the range in samples (inclusive).
   * @param sampleIndexEnd is the end of the range in samples (exclusive).
   * @return summed spectrum from [sampleIndexBegin, sampleIndexEnd)
   * @throws ArrayIndexOutOfBoundsException for an invalid sample index.
   */
  int[] getSpectrumSum(int sampleIndexBegin, int sampleIndexEnd) throws ArrayIndexOutOfBoundsException;

  /**
   * Get the mean rate of the spectrum over a range of samples.
   *
   * @param sampleIndexBegin is the start of the range (inclusive).
   * @param sampleIndexEnd is the end of the range (exclusive).
   * @return mean spectrum from [sampleIndexBegin, sampleIndexEnd)
   * @throws ArrayIndexOutOfBoundsException for an invalid sample index.
   */
  public default double[] getSpectrumRate(int sampleIndexBegin, int sampleIndexEnd) throws ArrayIndexOutOfBoundsException
  {
    double[] sum = IntegerArray.promoteToDoubles(getSpectrumSum(sampleIndexBegin, sampleIndexEnd));
    return DoubleArray.divideAssign(sum, (sampleIndexEnd - sampleIndexBegin) * 
            this.getSensorProperties().getSamplePeriodSeconds());
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