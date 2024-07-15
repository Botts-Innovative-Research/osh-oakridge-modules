/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.ernie.internal.data;

import gov.llnl.ernie.data.Lane;
import gov.llnl.ernie.data.SensorPosition;
import gov.llnl.ernie.data.SensorProperties;
import gov.llnl.ernie.data.VPSProperties;
import gov.llnl.math.euclidean.Vector3;
import gov.llnl.utility.UUIDUtilities;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author mattoon1
 */
public class LaneImpl implements Lane, Serializable
{
  private static final long serialVersionUID = UUIDUtilities.createLong("Lane-v1");

  String laneType;
  String laneVector;
  protected String laneConveyance;
  protected long portId;
  protected long rpmId;
  protected VPSProperties vpsProperties;
  public double laneWidth;
  public double laneHeight;
  private final List<SensorProperties> gammaSensorProperties = new ArrayList<>();
  private final List<SensorProperties> neutronSensorProperties = new ArrayList<>();

  public List<int[]> gammaGroup;
  public List<int[]> neutronGroup;
  public RatioConversion gammaConversion;
  public RatioConversion neutronConversion;
  public int panels;

  public class Vector3Impl implements Vector3
  {
    double x, y, z;

    public Vector3Impl(double x, double y, double z)
    {
      this.x = x;
      this.y = y;
      this.z = z;
    }

    @Override
    public double getX()
    {
      return x;
    }

    @Override
    public double getY()
    {
      double w = laneWidth / 2.0;
      return w * Math.signum(y);
    }

    @Override
    public double getZ()
    {
      return z;
    }

  }

  public LaneImpl()
  {
  }

  /**
   * @return TODO REFACTOR what is LaneVector?
   */
  @Override
  public String getLaneVector()
  {
    return laneVector;
  }

  /**
   * @return TODO REFACTOR what is LaneConveyance?
   */
  @Override
  public String getLaneConveyance()
  {
    return laneConveyance;
  }

  /**
   * @return TODO REFACTOR what does this represent? What kind of values are
   * possible?
   */
  @Override
  public String getLaneType()
  {
    return laneType;
  }

  /**
   * @return an identifier for the port or terminal.
   *
   * TODO REFACTOR old dataSourceId maps to portId
   */
  @Override
  public long getPortId()
  {
    return portId;
  }

  /**
   * @return an identifier for the rpm. This indicates which portal if there are
   * multiple portals in the terminal.
   */
  @Override
  public long getRpmId()
  {
    return rpmId;
  }

  /**
   * Get the width of the lane.
   *
   * @return the lane width in meters.
   */
  @Override
  public double getLaneWidth()
  {
    return laneWidth;
  }

  /**
   * @return laneHeight
   */
  @Override
  public double getLaneHeight()
  {
    return laneHeight;
  }

  /**
   * Get the properties of the vps. This will be a covariant return with the
   * specific vendor implementation.
   *
   * @return the properties of the vps.
   */
  @Override
  public VPSProperties getVPSProperties()
  {
    return vpsProperties;
  }

  //<editor-fold desc="Setters used by LaneBuilder" defaultstate="collapsed">
  public void setLaneType(String laneType)
  {
    this.laneType = laneType;
  }

  public void setLaneVector(String laneVector)
  {
    this.laneVector = laneVector;
  }

  public void setLaneConveyance(String laneConveyance)
  {
    this.laneConveyance = laneConveyance;
  }

  public void setPortId(long portId)
  {
    this.portId = portId;
  }

  public void setRpmId(long rpmId)
  {
    this.rpmId = rpmId;
  }

  public void setVpsProperties(VPSProperties vpsProperties)
  {
    this.vpsProperties = vpsProperties;
  }

  public void setLaneWidth(double laneWidth)
  {
    this.laneWidth = laneWidth;
  }

  public void setLaneHeight(double laneHeight)
  {
    this.laneHeight = laneHeight;
  }

  //</editor-fold>
  @Override
  public boolean isSecondary()
  {
    // FIXME determine this from the database.
    return false;
  }

  /**
   * Access the specifics about the gamma panels.
   *
   * @return the gamma panels associated with this panel.
   */
  @Override
  public List<SensorProperties> getGammaSensorProperties()
  {
    return Collections.unmodifiableList(gammaSensorProperties);
  }

  /**
   * Access the specifics about the neutron panels.
   *
   * @return the neutron panels associated with this panel.
   */
  @Override
  public List<SensorProperties> getNeutronSensorProperties()
  {
    return Collections.unmodifiableList(neutronSensorProperties);
  }

  public void addGammaPanelProperties(SensorProperties gammaProperties)
  {
    this.gammaSensorProperties.add(gammaProperties);
  }

  public void addNeutronPanelProperties(SensorProperties neutronProperties)
  {
    this.neutronSensorProperties.add(neutronProperties);
  }

  @Override
  public Vector3 convertGammaRatios(double ratio1, double ratio2)
  {
    if (gammaConversion != null)
      return gammaConversion.apply(ratio1, ratio2);
    return Vector3.of(0, 0, 0);
  }

  @Override
  public Vector3 convertNeutronRatios(double ratio1, double ratio2)
  {
    if (neutronConversion != null)
      return neutronConversion.apply(ratio1, ratio2);
    return Vector3.of(0, 0, 0);
  }

  public void setWidth(double laneWidth)
  {
    this.laneWidth = laneWidth;
  }

  @Override
  public int[] getGammaPanels(SensorPosition position)
  {
    if (this.gammaGroup == null)
      throw new NullPointerException("gammaGroup is null");
    return this.gammaGroup.get(position.getValue());
  }

  @Override
  public int[] getNeutronPanels(SensorPosition position)
  {
    if (this.neutronGroup == null)
      throw new NullPointerException("gammaGroup is null");
    return this.neutronGroup.get(position.getValue());
  }

  public void setPanels(int panels)
  {
    this.panels = panels;
  }

  public interface RatioConversion
  {
    Vector3 apply(double ratio1, double ratio2);
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