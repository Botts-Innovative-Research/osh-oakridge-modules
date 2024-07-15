/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.ernie.rtk;


import gov.llnl.ernie.data.EnergyScale;
import gov.llnl.utility.ExpandableObject;
import gov.llnl.utility.UUIDUtilities;
import java.time.Instant;

/**
 *
 * @author nelson85
 * @param <Type>
 */
public abstract class SpectrumBase<Type> extends ExpandableObject implements Spectrum<Type>
{
  private static final long serialVersionUID = UUIDUtilities.createLong("SpectrumBase-v1");
  /**
   * energy bins for this data. May be null if not available.
   */
  private EnergyScale energyScale = null;
  /**
   * real time in seconds.
   */
  private double realtime = 0;
  /**
   * live time in seconds.
   */
  private double livetime = 0;
  /**
   * minimum valid channel (inclusive)
   */
  private int minimumValidChannel = 0;
  /**
   * maximum valid channel (exclusive)
   */
  private int maximumValidChannel = 0;

  protected SpectrumBase()
  {
  }

  protected SpectrumBase(double livetime, double realtime)
  {
    this.livetime = livetime;
    this.realtime = realtime;
  }

  /**
   * Copy constructor. Copy the attributes, times and energy bins.
   *
   * @param obj
   */
  protected SpectrumBase(Spectrum obj)
  {

    super(obj);
    this.realtime = obj.getRealTime();
    this.livetime = obj.getLiveTime();
    this.energyScale = obj.getEnergyScale();   // FIXME deep or shallow copy
    this.setValidRange(obj.getMinimumValidChannel(),
            obj.getMaximumValidChannel());
  }

  @Override
  final public double getLiveTime()
  {
    return livetime;
  }

  @Override
  final public double getRealTime()
  {
    return realtime;
  }

  /**
   * Alters the size of the spectrum. The current counts in the spectrum are
   * cleared. Clears the valid range.
   *
   * @param size
   */
  abstract public void resize(int size);

//<editor-fold desc="loader">  
  /**
   * Set the real time for the spectrum. Real time should be in seconds.
   *
   * @param toDouble
   */
  final public void setRealTime(double toDouble)
  {
    this.realtime = toDouble;
  }

  /**
   * Set the live time for the spectrum. Live time should be in seconds.
   *
   * @param toDouble
   */
  final public void setLiveTime(double toDouble)
  {
    this.livetime = toDouble;
  }
//</editor-fold>
//<editor-fold desc="energy-bins">

  /**
   * Get the energy bins associated with the spectrum.
   *
   * @return the energy bins associated with the spectrum, or null if no scale
   * is set.
   */
  @Override
  final public EnergyScale getEnergyScale()
  {
    return energyScale;
  }

  /**
   * Set the energy bins associated with the spectrum.
   *
   * @param energyScale
   */
  final public void setEnergyScale(EnergyScale energyScale)
  {
    if (energyScale == null)
      throw new NullPointerException("Null Energy Scale");
    this.energyScale = energyScale;
    if (size() != this.energyScale.getChannels())
    {
      // FIXME Add error handling
    }
  }
//</editor-fold>
//<editor-fold desc="timestamp">

  @Override
  final public Instant getStartTime()
  {
    return this.getAttribute(SpectrumAttributes.TIMESTAMP, Instant.class);
  }

  @Override
  final public Instant getEndTime()
  {
    Instant date = this.getStartTime();
    if (date == null)
      return null;
    long ts = (long) (date.toEpochMilli() + this.realtime * 1000.0);
    return Instant.ofEpochMilli(ts);
  }

  /**
   * Set the timestamp for this spectrum.
   *
   * @param timestamp
   */
  final public void setStartTime(Instant timestamp)
  {
    this.setAttribute(SpectrumAttributes.TIMESTAMP, timestamp);
  }

//</editor-fold>
//<editor-fold desc="valid-range">
  /**
   * @return the minimumValidChannel
   */
  @Override
  public int getMinimumValidChannel()
  {
    return minimumValidChannel;
  }

  /**
   * @param minimumValidChannel the minimumValidChannel to set
   */
  public void setMinimumValidChannel(int minimumValidChannel)
  {
    if (minimumValidChannel < 0)
      minimumValidChannel = 0;
    this.minimumValidChannel = minimumValidChannel;
    clearCache();
  }

  /**
   * @return the maximumValidChannel
   */
  @Override
  public int getMaximumValidChannel()
  {
    return maximumValidChannel;
  }

  /**
   * @param maximumValidChannel the maximumValidChannel to set
   */
  public void setMaximumValidChannel(int maximumValidChannel)
  {
    if (maximumValidChannel > size())
      maximumValidChannel = size();
    this.maximumValidChannel = maximumValidChannel;
    clearCache();
  }
//</editor-fold>

  /**
   * Clear the spectrum data. This clears the realtime and livetime.
   */
  public void clear()
  {
    this.realtime = 0;
    this.livetime = 0;
    clearCache();
  }

  protected void assign(Spectrum obj)
  {
    // Copy any attributes
    super.copyAttributes(obj);

    // copy the energy bins
    EnergyScale eb = obj.getEnergyScale();
    if (eb == null)
      this.energyScale = null;
    else
      this.energyScale = EnergyScaleFactory.newScale(eb.getEdges());

    this.livetime = obj.getLiveTime();
    this.realtime = obj.getRealTime();
    this.minimumValidChannel = obj.getMinimumValidChannel();
    this.maximumValidChannel = obj.getMaximumValidChannel();
    clearCache();
  }

  protected <T> void addAssignBase(Spectrum<T> obj)
  {
    this.livetime += obj.getLiveTime();
    this.realtime += obj.getRealTime();
    this.minimumValidChannel = Math.max(minimumValidChannel, obj.getMinimumValidChannel());
    this.maximumValidChannel = Math.max(maximumValidChannel, obj.getMaximumValidChannel());
    clearCache();
  }

  protected <T> void subtractAssignBase(Spectrum<T> obj)
  {
    this.livetime -= obj.getLiveTime();
    this.realtime -= obj.getRealTime();
    this.minimumValidChannel = Math.max(minimumValidChannel, obj.getMinimumValidChannel());
    this.maximumValidChannel = Math.max(maximumValidChannel, obj.getMaximumValidChannel());
    clearCache();
  }

//<editor-fold desc="title">
  /**
   * Get the title of the spectrum.
   *
   * @return the title or null if not defined.
   */
  @Override
  public String getTitle()
  {
    return this.getAttribute(SpectrumAttributes.TITLE, String.class);
  }

  /**
   * Set the title of this spectrum. Title is stored as an attribute.
   *
   * @param title the title to set
   */
  public void setTitle(String title)
  {
    this.setAttribute(SpectrumAttributes.TITLE, title);
  }
//</editor-fold>

//<editor-fold desc="out-of-range">  
  abstract public void setUnderRange(double value);

  abstract public void setOverRange(double value);
//</editor-fold> 

  public void setValidRange(int start, int end)
  {
    this.minimumValidChannel = start;
    this.maximumValidChannel = end;
  }

  protected abstract void assignData(Object obj);

  /**
   * Access the underlying data in the collection.
   *
   * @return access the array.
   * @throws UnsupportedOperationException if the collection does not support
   * modification of the data.
   */
  public abstract Type toArray() throws UnsupportedOperationException;

  /**
   * Clear the cached valued. This must be called if the spectrum data is
   * altered.
   */
  public abstract void clearCache();
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