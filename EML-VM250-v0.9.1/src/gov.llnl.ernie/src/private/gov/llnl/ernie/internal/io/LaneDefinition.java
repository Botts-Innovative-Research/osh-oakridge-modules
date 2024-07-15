/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.ernie.internal.io;

import gov.llnl.ernie.data.Lane;
import gov.llnl.ernie.data.VPSProperties;
import gov.llnl.ernie.internal.data.LaneImpl;
import gov.llnl.ernie.impl.SensorPropertiesImpl;
import gov.llnl.ernie.internal.data.LaneRatioConversion;
import gov.llnl.math.euclidean.Vector3;
import gov.llnl.utility.xml.bind.Reader;
import gov.llnl.utility.xml.bind.ReaderInfo;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Get the properties of the lane.
 *
 * @author nelson85
 */
@ReaderInfo(LaneDefinitionReader.class)
public class LaneDefinition implements Cloneable
{
  LaneId laneId = null;

  VPSProperties vpsProperties;
  
  double laneWidth = 6.0;
  double laneHeight = 6.0;

  double gammaPanelWidth;
  double gammaPanelHeight;
  double gammaPanelThickness;

  double neutronPanelWidth;
  double neutronPanelHeight;
  double neutronPanelThickness;

  boolean collimated;
  double gammaSideCollimator;

  Duration gammaSamplePeriod = null;
  Duration neutronSamplePeriod = null;

  List<Vector3> gammaPanelOrigin = new ArrayList<>();
  List<Vector3> neutronPanelOrigin = new ArrayList<>();

  double[] energyEdges = null;

  List<double[]> energyFactors = new ArrayList<>();
  int[][] gammaGroup = new int[5][];
  int[][] neutronGroup = new int[5][];

  double[] gammaYFactors = null;
  double[] gammaZFactors = null;

  double[] neutronYFactors = null;
  double[] neutronZFactors = null;

  // default constructor
  public LaneDefinition()
  {
    
  }

  // copy constructor
  public LaneDefinition(LaneDefinition other)
  {
    laneWidth = other.laneWidth;
    laneHeight = other.laneHeight;
    
    gammaPanelWidth = other.gammaPanelWidth;
    gammaPanelHeight = other.gammaPanelHeight;
    gammaPanelThickness = other.gammaPanelThickness;
    
    neutronPanelWidth = other.neutronPanelWidth;
    neutronPanelHeight = other.neutronPanelHeight;
    neutronPanelThickness = other.neutronPanelThickness;
    
    collimated = other.collimated;
    gammaSideCollimator = other.gammaSideCollimator;
    
    if (other.gammaSamplePeriod != null)
      gammaSamplePeriod = other.gammaSamplePeriod.plusNanos(0); // make a copy
    
    if (other.neutronSamplePeriod != null)
      neutronSamplePeriod = other.neutronSamplePeriod.plusNanos(0); // make a copy
    
    gammaPanelOrigin = new ArrayList<>(other.gammaPanelOrigin);
    neutronPanelOrigin = new ArrayList<>(other.neutronPanelOrigin);

    // Copy if available
    if (other.energyEdges != null)
      energyEdges = other.energyEdges.clone();

    // Copy if available
    if (other.energyFactors != null)
      energyFactors = new ArrayList<>(other.energyFactors);
    
    gammaGroup = other.gammaGroup.clone();
    neutronGroup = other.neutronGroup.clone();
 
    if (other.gammaYFactors != null)
      gammaYFactors = other.gammaYFactors.clone();
    if (other.gammaZFactors != null)
      gammaZFactors = other.gammaZFactors.clone();

    if (other.neutronYFactors != null)
      neutronYFactors = other.neutronYFactors.clone();
    if (other.neutronZFactors != null)
      neutronZFactors = other.neutronZFactors.clone();
  }
  
  public boolean matches(Lane lane2)
  {
    LaneImpl lane = (LaneImpl) lane2;

    if (lane.panels != gammaPanelOrigin.size() )
      return false;
    
    if (laneId != null)
    {
      return (laneId.getSiteId() == lane2.getPortId()
              && laneId.getRpmId() == lane2.getRpmId());
    }

    return true;
  }

  /**
   * toLane is used in python: proj-ernie4/py/rpm8/buildBackgroundModel.py   * 
   * 
   * @return a Lane
   */
  public Lane toLane()
  {
    return apply(new LaneImpl());
  }
  
  
  /**
   * Apply definition to this lane
   * 
   * @param lane2
   * @return 
   */
  public Lane apply(Lane lane2)
  {
    LaneImpl lane = (LaneImpl) lane2;
    lane.setLaneWidth(laneWidth);
    lane.setLaneHeight(laneHeight);
    lane.setVpsProperties(vpsProperties);
    
    boolean haveEnergyFactors = this.energyFactors.size()>0;
    
    if (haveEnergyFactors && (this.energyFactors.size()!=gammaPanelOrigin.size())) 
    {
      throw new RuntimeException("Panel size mismatch between energy factors and origin.");
    }

    for (int i = 0; i < gammaPanelOrigin.size(); ++i)
    {
      Vector3 origin = gammaPanelOrigin.get(i);

      SensorPropertiesImpl gammaProperties = new SensorPropertiesImpl(
              lane.new Vector3Impl(origin.getX(),origin.getY(),origin.getZ()),
              gammaPanelWidth, gammaPanelHeight, gammaPanelThickness,
              false,
              energyEdges.length - 1, energyEdges,
              gammaSamplePeriod
      );
      gammaProperties.sideCollimator = this.gammaSideCollimator;
      lane.addGammaPanelProperties(gammaProperties);
      if (haveEnergyFactors)
      {
        gammaProperties.setEnergyFactors(this.energyFactors.get(i));
      }
    }

    for (int i = 0; i < neutronPanelOrigin.size(); ++i)
    {
      Vector3 origin = neutronPanelOrigin.get(i);

      SensorPropertiesImpl neutronProperties = new SensorPropertiesImpl(
              lane.new Vector3Impl(origin.getX(), origin.getY(), origin.getZ()),
              0, gammaPanelHeight, 0,
              false,
              1, null,
              neutronSamplePeriod
      );
      lane.addNeutronPanelProperties(neutronProperties);
    }
    lane.gammaGroup = Arrays.asList(this.gammaGroup);
    lane.neutronGroup = Arrays.asList(this.neutronGroup);

    // Define the gammaConversion method to get Y,Z
    lane.gammaConversion = new LaneRatioConversion(lane, gammaYFactors, gammaZFactors);
    lane.neutronConversion = new LaneRatioConversion(lane, neutronYFactors, neutronZFactors);
    
    return lane;
    // FIXME Use the site info to get the beams disabled. This need to combine with 
    // the other lane definition.  Two lan
  }

  /**
   * @return the energyFactors
   */
  public List<double[]> getEnergyFactors()
  {
    return energyFactors;
  }

  /**
   * @return the gammaPanelWidth
   */
  public double getGammaPanelWidth()
  {
    return gammaPanelWidth;
  }

  /**
   * @return the gammaPanelHeight
   */
  public double getGammaPanelHeight()
  {
    return gammaPanelHeight;
  }

  /**
   * @return the gammaPanelThickness
   */
  public double getGammaPanelThickness()
  {
    return gammaPanelThickness;
  }

  /**
   * @return the laneHeight
   */
  public double getLaneHeight()
  {
    return laneHeight;
  }

  /**
   * @return the energyEdges
   */
  public double[] getEnergyEdges()
  {
    return energyEdges;
  }

  void addGammaPanel(Vector3 v)
  {
    this.gammaPanelOrigin.add(v);
  }

  void addNeutronPanel(Vector3 v)
  {
    this.neutronPanelOrigin.add(v);
  }

  void addEnergyFactors(double[] v)
  {
    this.energyFactors.add(v);
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