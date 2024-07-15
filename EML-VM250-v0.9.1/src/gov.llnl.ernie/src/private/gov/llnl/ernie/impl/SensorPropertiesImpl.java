/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.ernie.impl;

import gov.llnl.utility.UUIDUtilities;
import gov.llnl.ernie.data.SensorProperties;
import gov.llnl.ernie.data.EnergyScale;
import gov.llnl.ernie.rtk.EnergyScaleFactory;
import gov.llnl.ernie.rtk.view.SensorFace;
import gov.llnl.ernie.rtk.view.SensorFaceRectangular;
import gov.llnl.ernie.data.SensorView;
import gov.llnl.ernie.rtk.view.SensorViewFactory;
import gov.llnl.math.euclidean.Vector3;
import gov.llnl.math.euclidean.Versor;
import java.time.Duration;
import java.util.Arrays;

/**
 *
 * @author guensche1
 */
public class SensorPropertiesImpl implements SensorProperties
{
  private static final long serialVersionUID = UUIDUtilities.createLong("SensorPropertiesImpl-v1");

  private final Vector3 origin; // center of the detector front face.

  private final double width;
  private final double height;
  private final double thickness;

  private final boolean isCombined;
  private final int numberChannels;
  private final EnergyScale energyEdges;
  private double[] energyFactors;
  private final Duration samplePeriod;
  public double sideCollimator;

  /**
   * @param origin
   * @param width
   * @param thickness
   * @param height detector height
   * @param samplePeriod
   * @param isCombined is this a composite panel
   * @param numberChannels channels per panel
   * @param energyEdges is the edges of the channels in keV.
   */
  public SensorPropertiesImpl(Vector3 origin,
          double width, double height, double thickness,
          boolean isCombined, int numberChannels, double[] energyEdges,
          Duration samplePeriod)
  {
    this.origin = origin;
    this.width = width;
    this.height = height;
    this.thickness = thickness;
    this.isCombined = isCombined;
    this.numberChannels = numberChannels;
    this.energyEdges = EnergyScaleFactory.newScale(energyEdges);
    this.samplePeriod = samplePeriod;

    // The channels and energy edges should match if energy bins were supplied.
    if (energyEdges != null && numberChannels != energyEdges.length - 1)
      throw new RuntimeException(
              String.format("Size mismatch (%d, %d)", numberChannels, energyEdges.length - 1));
  }

  /**
   * Get if this a composition panel. A composition panel will not have a valid
   * position.
   *
   * @return
   */
  @Override
  public boolean isCombined()
  {
    return isCombined;
  }

  /**
   * Get the number of channels in the panel.
   *
   * @return number of channels.
   */
  @Override
  public int getNumberOfChannels()
  {
    return numberChannels;
  }

  /**
   * Get the energy boundaries for the channels. There will be one more edge
   * than channel.
   *
   * @return the edges for the channels in keV.
   */
  @Override
  public EnergyScale getEnergyScale()
  {
    return energyEdges;
  }

  /**
   * Get the position of the panel relative to the center of the lane.
   *
   * @return the x position
   * @throws IllegalStateException if this is a combined panel
   */
  @Override
  public Vector3 getOrigin() throws IllegalStateException
  {
    if (isCombined())
    {
      throw new IllegalStateException("Combined panels do not have position.");
    }
    return origin;
  }

  @Override
  public double getWidth() throws IllegalStateException
  {
    if (isCombined())
    {
      throw new IllegalStateException("Combined panels do not have height.");
    }
    return width;
  }

  /**
   * Get the height of the panel (top - bottom)
   *
   * @return the height
   * @throws IllegalStateException if this is a combined panel
   */
  @Override
  public double getHeight() throws IllegalStateException
  {
    if (isCombined())
    {
      throw new IllegalStateException("Combined panels do not have height.");
    }
    return height;
  }

  @Override
  public double getThickness() throws IllegalStateException
  {
    if (isCombined())
    {
      throw new IllegalStateException("Combined panels do not have thickness.");
    }
    return thickness; // FROM GADRAS MODE
  }

  @Override
  public double[] getEnergyFactors()
  {
    return this.energyFactors;
  }

  /**
   * @param energyFactors the energyFactors to set
   */
  public void setEnergyFactors(double[] energyFactors)
  {
    this.energyFactors = energyFactors;
  }

  @Override
  public Duration getSamplePeriod()
  {
    return samplePeriod;
  }

  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    sb.append("SensorProperties{");
    if (origin != null)
      sb.append(String.format("x=%f,y=%f,z=%f,", origin.getX(), origin.getY(), origin.getZ()));
    sb.append(String.format("isCombined=%b,", isCombined));
    sb.append(String.format("numberChannels=%d,", numberChannels));
    sb.append(")");
    return sb.toString();
  }

  @Override
  public SensorView getSensorView()
  {
    Vector3 origin = this.getOrigin();

    // Assume the orientation of the panel based on which side of the lane we are on.
    Versor orientation = Versor.of(Vector3.AXIS_Z, (this.getOrigin().getY() < 0 ? 1 : -1) * Math.PI / 2);

    // Add a new volume detector
    if (this.sideCollimator > 0)
    {
      SensorView[] faces = new SensorFace[3];
      faces[0] = SensorViewFactory.createRectangularCollimated(
              this.getWidth(), this.getHeight(),
              Vector3.ZERO, Versor.ZERO, this.sideCollimator, 0);
      faces[1] = new SensorFaceRectangular(width, thickness,
              Vector3.of(0, 0, height / 2), Versor.of(Vector3.AXIS_Y, -Math.PI / 2));
      faces[2] = new SensorFaceRectangular(width, thickness,
              Vector3.of(0, 0, -height / 2), Versor.of(Vector3.AXIS_Y, Math.PI / 2));
      return SensorViewFactory.createComposite(Arrays.asList(faces), origin, orientation);
    }

    if (this.getThickness() == 0)
    {
      return SensorViewFactory.createRectangular(
              this.getWidth(), this.getHeight(), origin, orientation);
    }

    return SensorViewFactory.createCuboid(this.getWidth(), this.getHeight(), this.getThickness(),
            origin, orientation);
  }

  /**
   * @return the sideCollimator
   */
  @Override
  public double getSideCollimator()
  {
    return sideCollimator;
  }

  /**
   * @param sideCollimator the sideCollimator to set
   */
  public void setSideCollimator(double sideCollimator)
  {
    this.sideCollimator = sideCollimator;
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