/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gov.llnl.ernie.vm250.data;

import gov.llnl.ernie.data.VehicleClass;
import gov.llnl.ernie.internal.data.LaneImpl;
import gov.llnl.utility.UUIDUtilities;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 *
 * @author mattoon1
 */
public class VM250RecordInternal implements java.io.Serializable
{
  private static final long serialVersionUID = UUIDUtilities.createLong("VM250RecordInternal-v1");
  private static final int nPanels = 4;
  private static final int nGammaChannels = 1;

  private long occupancyId;
  private boolean isInjected = false;

  PanelData[] panelData;
  private CombinedPanelData combinedPanelData = new CombinedPanelData();

  private VehicleClass vehicleClass;

  private boolean gammaHighBackground = false;
  private boolean gammaLowBackground = false;
  private boolean neutronHighBackground = false;

  /**
   * Representation of the lane constructed from the lane description. This
   * holds all information about the lane including the panel location and lane
   * width.
   */
  private LaneImpl lane;

  public VM250RecordInternal()
  {
    this.lane = new LaneImpl();
    this.occupancyId = -1;

    panelData = new PanelData[nPanels];
    for (int i = 0; i < nPanels; i++)
    {
      panelData[i] = new PanelData();
    }
  }

  /**
   * SegmentDescription stores identifying information about the record, where
   * it was collected, etc.
   */
  public class SegmentDescription implements java.io.Serializable
  {

    private static final long serialVersionUID = 60000L;
    private int dataSourceId;
    private long rpmId;
    private Instant rpmDateTime;
    private boolean continuation;
    private double[] gammaBackground;
    private double neutronBackground;
    private List<Boolean> gammaAlarm;
    private List<Boolean> neutronAlarm;
    private boolean anyGammaAlarm;
    private boolean anyNeutronAlarm;

    private boolean realOccupancy;

    public SegmentDescription(int dataSourceId, long rpmId,
            Instant rpmDateTime, boolean continuation,
            int[] gammaBackground, double neutronBackground,
            boolean gammaAlarm, boolean neutronAlarm, boolean realOccupancy)
    {
      this.dataSourceId = dataSourceId;
      this.rpmId = rpmId;
      this.rpmDateTime = rpmDateTime;
      this.continuation = continuation;
      this.gammaBackground = new double[gammaBackground.length];
      for (int i = 0; i < gammaBackground.length; i++)
      {
        this.gammaBackground[i] = gammaBackground[i];
      }
      this.neutronBackground = neutronBackground;
      this.anyGammaAlarm = gammaAlarm;
      this.anyNeutronAlarm = neutronAlarm;
      this.realOccupancy = realOccupancy;
    }

    /**
     * @return the dataSourceId
     */
    public int getDataSourceId()
    {
      return dataSourceId;
    }

    /**
     * @return the rpmId
     */
    public long getRpmId()
    {
      return rpmId;
    }

    /**
     * @return the rpmDateTime
     */
    public Instant getRpmDateTime()
    {
      return rpmDateTime;
    }

    /**
     * @return the continuation
     */
    public boolean isContinuation()
    {
      return continuation;
    }

    /**
     * @return the gammaBackground
     */
    public double[] getGammaBackground()
    {
      return gammaBackground;
    }

    /**
     * @return the neutronBackground
     */
    public double getNeutronBackground()
    {
      return neutronBackground;
    }

    /**
     * @return the gammaAlarm
     */
    public List<Boolean> getGammaAlarm()
    {
      return gammaAlarm;
    }

    /**
     * @return the neutronAlarm
     */
    public List<Boolean> getNeutronAlarm()
    {
      return neutronAlarm;
    }

    /**
     * @return the anyGammaAlarm
     */
    public boolean isAnyGammaAlarm()
    {
      return anyGammaAlarm;
    }

    /**
     * @return the anyNeutronAlarm
     */
    public boolean isAnyNeutronAlarm()
    {
      return anyNeutronAlarm;
    }

    /**
     * @return the realOccupancy
     */
    public boolean isRealOccupancy()
    {
      return realOccupancy;
    }

    /**
     * @param gammaAlarm the gammaAlarm to set
     */
    public void setGammaAlarm(List<Boolean> gammaAlarm)
    {
      this.gammaAlarm = gammaAlarm;
    }

    /**
     * @param neutronAlarm the neutronAlarm to set
     */
    public void setNeutronAlarm(List<Boolean> neutronAlarm)
    {
      this.neutronAlarm = neutronAlarm;
    }
  }

  private SegmentDescription segmentDescription;

  public void setSegmentDescription(int dataSourceId, long rpmId, Instant rpmDateTime,
          boolean continuation, int[] gammaBackground, double neutronBackground,
          boolean gammaAlarm, boolean neutronAlarm, boolean realOccupancy)
  {
    segmentDescription = new SegmentDescription(dataSourceId, rpmId, rpmDateTime, continuation,
            gammaBackground, neutronBackground, gammaAlarm, neutronAlarm, realOccupancy);
  }

  public class SetupInfo implements java.io.Serializable
  {
    private static final long serialVersionUID = 60000L;
    private final int Intervals;
    private final int OccupancyHoldin;
    private final double NSigma;

    public SetupInfo(int intervals, int occupancyHoldin, double nSigma)
    {
      this.Intervals = intervals;
      this.OccupancyHoldin = occupancyHoldin;
      this.NSigma = nSigma;
    }

    /**
     * @return the Intervals
     */
    public int getIntervals()
    {
      return Intervals;
    }

    /**
     * @return the OccupancyHoldin
     */
    public int getOccupancyHoldin()
    {
      return OccupancyHoldin;
    }

    /**
     * @return the NSigma
     */
    public double getNSigma()
    {
      return NSigma;
    }

  }

  private SetupInfo setupInfo;

  public void setSetupInfo(int intervals, int occupancyHoldin, double nSigma)
  {
    this.setupInfo = new SetupInfo(intervals, occupancyHoldin, nSigma);
  }

  // FIXME: following code is nearly identical to stuff in RPM8RecordInternal.java. Refactor needed
  /**
   * Interface for PanelData and CombinedPanelData class.
   */
  public interface PanelDataInterface
  {
    public int[] gross();

    public int getGross(int index);

    public double[] getGrossGamma();

    public int[] getGammaSpectrum(int index);

    public int getGross(int begin, int end);

    public int[] getGammaSpectrum(int begin, int end);

    public int[] getNeutronCounts(int panel);

    public double[] getNeutronCounts();

    public double[] getTime();
  }

  /**
   * PanelData class holds the readings for the gamma and neutron detectors at
   * each point in time.
   */
  public class PanelData implements PanelDataInterface, java.io.Serializable
  {

    private static final long serialVersionUID = 40001L;

//    /**
//     * Timestamp for each sample in the record. This data is used only for
//     * debugging of problems and does not need to be populated.
//     */
//    public Timestamp[] sampleDateTime;
    /**
     * Sample id for each of the samples in the record. This is used to sort the
     * data when retrieving a record from the data base.
     */
    public long[] sampleId;

    /**
     * Gamma data for this panel. There are nGammaChannels of gamma data per
     * panel.
     */
    public int[][] gammaData; // nGammaChannels

    /**
     * Misc data supplied for each panel. This is used mainly for debugging as
     * none of the features consume it currently.
     */
    public int[][] miscData; // com_status gamma0_reject op_flags

    /**
     * Neutron data for this panel. There are 2 neutron channels per panel. See
     * the SAIC definitions of those channels.
     */
    public int[][] neutronData;  // This is the neutron data.

    /**
     * Number of time samples in this data. All gamma data must have this length
     * for the record to be valid.
     */
    int size_g_; // This is the number of gamma samples in the data.

    int size_n_; // Number of neutron samples

    // FIXME used in matlab should be removed. 
    public int[] gross()
    {
      int[] data = new int[size_g_];
      for (int j = 0; j < size_g_; ++j)
      {
        data[j] = 0;
        for (int i = 0; i < nGammaChannels; ++i)
        {
          data[j] += gammaData[j][i];
        }
      }
      return data;
    }

    public int size()
    {
      return size_g_;
    }

    public void resize(int sz)
    {
      size_g_ = sz;
      sampleId = new long[sz];
//      sampleDateTime = new Timestamp[sz];
      gammaData = new int[sz][nGammaChannels];
      miscData = new int[sz][3];
    }

    public int size_neutron()
    {
      return size_n_;
    }

    public void resize_neutron(int sz)
    {
      size_n_ = sz;
      neutronData = new int[sz][2];
    }

    /**
     * Copy the contents of another Panel data into this one. Called by
     * duplicate for the purposes of altering records for injection.
     *
     * @param in
     */
    public void assign(PanelData in)
    {
      resize(in.size());
      resize_neutron(in.size_neutron());
      for (int i = 0; i < size_g_; ++i)
      {
//        sampleDateTime[i] = in.sampleDateTime[i];
        sampleId[i] = in.sampleId[i];
        for (int j = 0; j < nGammaChannels; ++j)
        {
          gammaData[i][j] = in.gammaData[i][j];
        }
        for (int j = 0; j < 3; ++j)
        {
          miscData[i][j] = in.miscData[i][j];
        }
      }
      for (int i = 0; i < size_n_; ++i)
      {
        for (int j = 0; j < 2; ++j)
        {
          neutronData[i][j] = in.neutronData[i][j];
        }
      }
    }

    /**
     * Return the total gamma counts at a particular sample index.
     *
     * @param index
     * @return
     */
    public int getGross(int index)
    {
      int data = 0;

      for (int i = 0; i < nGammaChannels; ++i)
      {
        data += gammaData[index][i];
      }

      return data;
    }

    /**
     * Return the total gamma counts at a particular sample index.
     *
     * @return
     */
    public double[] getGrossGamma()
    {
      double[] data = new double[size_g_];

      for (int i = 0; i < size_g_; ++i)
      {
        for (int j = 0; j < nGammaChannels; j++)
        {
          data[i] += gammaData[i][j];
        }
      }

      return data;
    }

    /**
     * Returns to sum of the gross counts between two sample indices.
     *
     * @param begin is the first sample index (inclusive).
     * @param end is the last sample index (exclusive).
     * @return
     */
    public int getGross(int begin, int end)
    {
      int data = 0;

      for (int j = begin; j < end; ++j)
      {
        for (int i = 0; i < nGammaChannels; ++i)
        {
          data += gammaData[j][i];
        }
      }

      return data;
    }

    /**
     * Returns the count rate per unit sample time over an interval.
     *
     * @param begin is the first sample index (inclusive).
     * @param end is the last sample index (exclusive).
     * @return
     */
    public double getRate(int begin, int end)
    {
      return (double) getGross(begin, end) / (end - begin);
    }

    /**
     * Returns the gamma spectral data for a given sample index.
     *
     * @param index is the sample index to access.
     * @return the total counts in the spectrum.
     */
    public int[] getGammaSpectrum(int index)
    {
      int out[] = new int[nGammaChannels];
      for (int i = 0; i < nGammaChannels; ++i)
      {
        out[i] = this.gammaData[index][i];
      }
      return out;
    }

    /**
     * Returns the gamma spectral data for a range of sample indices.
     *
     * @param begin is the first sample index (inclusive).
     * @param end is the last sample index (exclusive).
     * @return the total counts in the spectrum.
     */
    public int[] getGammaSpectrum(int begin, int end)
    {
      int out[] = new int[nGammaChannels];
      for (int j = begin; j < end; ++j)
      {
        for (int i = 0; i < nGammaChannels; ++i)
        {
          out[i] += this.gammaData[j][i];
        }
      }
      return out;
    }

    public int[] getNeutronCounts(int panel)
    {
      int out[] = new int[size()];
      for (int i = 0; i < size(); i++)
      {
        out[i] = this.neutronData[i][panel];
      }
      return out;
    }

    public double[] getNeutronCounts()
    {
      double[] data = new double[size_g_];

      for (int i = 0; i < size_g_; ++i)
      {
        for (int j = 0; j < 2; j++)
        {
          data[i] += neutronData[i][j];
        }
      }

      return data;
    }

    //samples are taken at .1s interval
    public double[] getTime()
    {
      double[] tm = new double[size_g_];
      double ti = 0.;
      for (int i = 0; i < size_g_; i++)
      {
        tm[i] = ti;
        ti += .1;
      }
      return tm;
    }

    private void chopPresamples(int samples)
    {
      int sz = this.size_g_ - samples;
      long[] newSampleId = new long[sz];
//      Timestamp[] newSampleDateTime = new Timestamp[sz];
      int[][] newGammaData = new int[sz][nGammaChannels];
      int[][] newMiscData = new int[sz][3];
      int[][] newNeutronData = new int[sz][2];

      System.arraycopy(this.sampleId, samples, newSampleId, 0, sz);
//      System.arraycopy(this.sampleDateTime, samples, newSampleDateTime, 0, sz);
      System.arraycopy(this.gammaData, samples, newGammaData, 0, sz);
      System.arraycopy(this.miscData, samples, newMiscData, 0, sz);
      System.arraycopy(this.neutronData, samples, newNeutronData, 0, sz);

      this.size_g_ = sz;
      this.sampleId = newSampleId;
//      this.sampleDateTime = newSampleDateTime;
      this.gammaData = newGammaData;
      this.miscData = newMiscData;
      this.neutronData = newNeutronData;
    }

    private void chopPostsamples(int length)
    {
      int sz = length;
      long[] newSampleId = new long[sz];
//      Timestamp[] newSampleDateTime = new Timestamp[sz];
      int[][] newGammaData = new int[sz][nGammaChannels];
      int[][] newMiscData = new int[sz][3];
      int[][] newNeutronData = new int[sz][2];

      System.arraycopy(this.sampleId, 0, newSampleId, 0, sz);
//      System.arraycopy(this.sampleDateTime, 0, newSampleDateTime, 0, sz);
      System.arraycopy(this.gammaData, 0, newGammaData, 0, sz);
      System.arraycopy(this.miscData, 0, newMiscData, 0, sz);
      System.arraycopy(this.neutronData, 0, newNeutronData, 0, sz);

      this.size_g_ = sz;
      this.sampleId = newSampleId;
//      this.sampleDateTime = newSampleDateTime;
      this.gammaData = newGammaData;
      this.miscData = newMiscData;
      this.neutronData = newNeutronData;
    }
  };

  /**
   * CombinedPanelData is a class holding detector measurements for all panels
   * summed together. All methods in this class are computed from the raw panel
   * data.
   */
  public class CombinedPanelData implements PanelDataInterface, java.io.Serializable
  {

    private static final long serialVersionUID = 50000L;

    public int size()
    {
      return VM250RecordInternal.this.getPanelData()[0].size();
    }

    public int size_neutron()
    {
      return VM250RecordInternal.this.getPanelData()[0].size_neutron();
    }

    public int getGross(int index)
    {
      int sum = 0;
      for (int i = 0; i < nPanels; ++i)
      {
        sum += VM250RecordInternal.this.getPanelData()[i].getGross(index);
      }
      return sum;
    }

    public double[] getGrossGamma()
    {
      double[] sum = VM250RecordInternal.this.getPanelData()[0].getGrossGamma();
      for (int i = 1; i < nPanels; ++i)
      {
        double[] grossGamma = VM250RecordInternal.this.getPanelData()[i].getGrossGamma();
        for (int index = 0; index < size(); ++index)
        {
          sum[index] += grossGamma[index];
        }
      }
      return sum;
    }

    public double[] getNeutronCounts()
    {
      double[] sum = VM250RecordInternal.this.getPanelData()[0].getNeutronCounts();
      for (int i = 1; i < nPanels; ++i)
      {
        double[] neutCounts = VM250RecordInternal.this.getPanelData()[i].getNeutronCounts();
        for (int index = 0; index < size(); ++index)
        {
          sum[index] += neutCounts[index];
        }
      }
      return sum;
    }

    public double[] getTime()
    {
      return VM250RecordInternal.this.getPanelData()[0].getTime();
    }

    public int getGammaEnergyChannel(int time, int channel)
    {
      int out = 0;
      for (int k = 0; k < nPanels; ++k)
      {
        out += VM250RecordInternal.this.getPanelData()[k].gammaData[time][channel];
      }
      return out;
    }

    public int[] getGammaSpectrum(int j)
    {
      int out[] = new int[nGammaChannels];
      for (int i = 0; i < nGammaChannels; ++i)
      {
        int sum = 0;
        for (int k = 0; k < nPanels; ++k)
        {
          sum += VM250RecordInternal.this.getPanelData()[k].gammaData[j][i];
        }
        out[i] = sum;
      }
      return out;
    }

    public int getGross(int begin, int end)
    {
      int out = 0;
      for (int k = 0; k < nPanels; ++k)
      {
        out += VM250RecordInternal.this.getPanelData()[k].getGross(begin, end);
      }
      return out;
    }

    public double getRate(int begin, int end)
    {
      return (double) getGross(begin, end) / (end - begin);
    }

    public int[] getGammaSpectrum(int begin, int end)
    {
      int out[] = new int[nGammaChannels];
      for (int i = 0; i < nGammaChannels; ++i)
      {
        int sum = 0;
        for (int j = begin; j < end; ++j)
        {
          for (int k = 0; k < nPanels; ++k)
          {
            sum += VM250RecordInternal.this.getPanelData()[k].gammaData[j][i];
          }
        }
        out[i] = sum;
      }
      return out;
    }

    public double[] getGammaSpectrumMean(int begin, int end)
    {
      int raw[] = getGammaSpectrum(begin, end);
      double out[] = new double[nGammaChannels];
      for (int i = 0; i < nGammaChannels; i++)
      {
        out[i] = ((double) raw[i]) / (end - begin);
      }
      return out;
    }

    public int[] gross()
    {
      int size = VM250RecordInternal.this.getLength();
      int[] data = new int[size];
      for (int j = 0; j < size; ++j)
      {
        data[j] = this.getGross(j);
      }
      return data;
    }

    public int[] getNeutronCounts(int panel)
    {
      int out[] = new int[size_neutron()];
      for (int i = 0; i < size_neutron(); i++)
      {
        int sum = 0;
        for (int k = 0; k < nPanels; ++k)
        {
          sum += VM250RecordInternal.this.getPanelData()[k].neutronData[i][panel];
        }
        out[i] = sum;
      }
      return out;
    }
  }

  /**
   * Single velocity reading, with time stamp + estimated velocity in m/s
   */
  public class VelocityReading implements java.io.Serializable
  {
    public Instant timestamp;
    public double velocity;

    public VelocityReading(Instant timestamp, double velocity)
    {
      this.timestamp = timestamp;
      this.velocity = velocity;
    }

    public VelocityReading clone()
    {
      VelocityReading v = new VelocityReading(this.timestamp, this.velocity);
      return v;
    }
  }

  private ArrayList<VelocityReading> velocities;

  /**
   * Create a copy of a record. Used when applying manipulations to a record for
   * injection.
   *
   * @param record1
   * @return a copy of the record.
   */
  public static VM250RecordInternal duplicate(VM250RecordInternal record1)
  {
    VM250RecordInternal out = new VM250RecordInternal();
    out.setOccupancyId(record1.getOccupancyId());
    int[] gammaBackground = new int[record1.getSegmentDescription().getGammaBackground().length];
    for (int i = 0; i < gammaBackground.length; i++)
    {
      gammaBackground[i] = (int) record1.getSegmentDescription().getGammaBackground()[i];
    }
    out.setSegmentDescription(record1.getDataSourceId(), record1.getRpmId(),
            record1.getRpmDateTime(), record1.getSegmentDescription().isContinuation(),
            gammaBackground, record1.getSegmentDescription().getNeutronBackground(), record1.getSegmentDescription().isAnyGammaAlarm(), record1.getSegmentDescription().isAnyNeutronAlarm(), record1.getSegmentDescription().isRealOccupancy());
    for (int i = 0; i < nPanels; ++i)
    {
      out.getPanelData()[i].assign(record1.getPanelData()[i]);
    }
    out.setVelocities(
            record1.getVelocities().stream().map(p -> p.clone()).collect(Collectors.toCollection(ArrayList::new))
    );

    out.setLane(record1.getLane());
    out.setSetupInfo(record1.setupInfo.Intervals, record1.setupInfo.OccupancyHoldin,
            record1.setupInfo.NSigma);

    return out;
  }

  public boolean anyAlarm()
  {
    return (getSegmentDescription().isAnyGammaAlarm() || getSegmentDescription().isAnyNeutronAlarm());
  }

  public int getDataSourceId()
  {
    return this.getSegmentDescription().getDataSourceId();
  }

  public double[] getGammaBackgrounds()
  {
    return this.getSegmentDescription().getGammaBackground();
  }

  /**
   * Access the data associated with lane that recorded this
   * VM250RecordInternal.
   *
   * @return a lane structure for this lane.
   */
  public LaneImpl getLane()
  {
    return lane;
  }

  public int getLength()
  {
    return this.getPanelData()[0].gammaData.length;
  }

  public int getNPanels()
  {
    return nPanels;
  }

  public int getNumberOfChannels()
  {
    return nGammaChannels;
  }
//
//  public int getOccupancyEnd()
//  {
//    return this.getLength() - 1;
//  }
//
//  public int getOccupancyStart()
//  {
//    // TODO is this ok?
//    return 0;
//  }

  public Instant getRpmDateTime()
  {
    return this.getSegmentDescription().getRpmDateTime();
  }

  public long getRpmId()
  {
    return this.getSegmentDescription().getRpmId();
  }

  public long getSegmentDescriptorId()
  {
    return this.getOccupancyId();
  }

  /**
   * Determine if this is a secondary lane. This function is used by the feature
   * builder to exclude the secondary inspections. It is not used by the
   * analysis routines.
   *
   * @return true if the lane is a secondary at the time of the scan.
   */
  public boolean isSecondary()
  {
    return !this.lane.getLaneType().equals("Primary");
  }

  /**
   * Get the vehicle class this vehicle was labeled as by the vehicle
   * classifier.
   *
   * @return
   */
  public VehicleClass getVehicleClass()
  {
    return vehicleClass;
  }

  void setVehicleClass(VehicleClass vehicleClass)
  {
    this.vehicleClass = vehicleClass;
  }
//
//  /**
//   * Get a vehicle class that is rescaled to the length of this particular
//   * vehicle.
//   *
//   * @return the modified vehicle class
//   */
//  public VehicleClass getVehicleClassInstance()
//  {
//    VehicleClassImpl vc = new VehicleClassImpl(getVehicleClass());
//    vc.rescale(getMotion().vehicleLength);
//    return vc;
//  }

  /**
   * @return the isInjected
   */
  public boolean isInjected()
  {
    return isInjected;
  }

  /**
   * @param isInjected the isInjected to set
   */
  public void setIsInjected(boolean isInjected)
  {
    this.isInjected = isInjected;
  }

  /**
   * @return the occupancyId
   */
  public long getOccupancyId()
  {
    return occupancyId;
  }

  /**
   * @param occupancyId the occupancyId to set
   */
  public void setOccupancyId(long occupancyId)
  {
    this.occupancyId = occupancyId;
  }

  /**
   * @return the panelData
   */
  public PanelData[] getPanelData()
  {
    return panelData;
  }

  /**
   * @return the combinedPanelData
   */
  public CombinedPanelData getCombinedPanelData()
  {
    return combinedPanelData;
  }

  /**
   * @param lane the lane to set
   */
  public void setLane(LaneImpl lane)
  {
    this.lane = lane;
  }

  /**
   * @return the segmentDescription
   */
  public SegmentDescription getSegmentDescription()
  {
    return segmentDescription;
  }

  /**
   * @return the setupInfo
   */
  public SetupInfo getSetupInfo()
  {
    return setupInfo;
  }

  /**
   * @return the velocities
   */
  public ArrayList<VelocityReading> getVelocities()
  {
    return velocities;
  }

  /**
   * @param velocities the velocities to set
   */
  public void setVelocities(ArrayList<VelocityReading> velocities)
  {
    this.velocities = velocities;
  }

  /**
   * @param segmentDescription the segmentDescription to set
   */
  public void setSegmentDescription(SegmentDescription segmentDescription)
  {
    this.segmentDescription = segmentDescription;
  }

  /**
   * @return the gammaHighBackground
   */
  public boolean isGammaHighBackground()
  {
    return gammaHighBackground;
  }

  /**
   * @return the gammaLowBackground
   */
  public boolean isGammaLowBackground()
  {
    return gammaLowBackground;
  }

  /**
   * @return the neutronHighBackground
   */
  public boolean isNeutronHighBackground()
  {
    return neutronHighBackground;
  }

  /**
   * @param gammaHighBackground the gammaHighBackground to set
   */
  public void setGammaHighBackground(boolean gammaHighBackground)
  {
    this.gammaHighBackground = gammaHighBackground;
  }

  /**
   * @param gammaLowBackground the gammaLowBackground to set
   */
  public void setGammaLowBackground(boolean gammaLowBackground)
  {
    this.gammaLowBackground = gammaLowBackground;
  }

  /**
   * @param neutronHighBackground the neutronHighBackground to set
   */
  public void setNeutronHighBackground(boolean neutronHighBackground)
  {
    this.neutronHighBackground = neutronHighBackground;
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