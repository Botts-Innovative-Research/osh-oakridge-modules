/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.ernie.spectral;

import gov.llnl.ernie.analysis.Features;
import gov.llnl.ernie.analysis.FeaturesDescription;
import gov.llnl.ernie.analysis.FeaturesDescription.FeatureDescription;
import gov.llnl.math.euclidean.Vector3;
import gov.llnl.math.internal.euclidean.Vector3Impl;

// Note: the position.x and length may be in units of time or distance depending
// on how the analysis was called.
public class JointSource implements Features
{
  private static final long serialVersionUID = gov.llnl.utility.UUIDUtilities.createLong("JointSource-v1");

  JointSourceDescription description;

  // Features
  public Vector3 position = Vector3.ZERO; // where is the source located (m)
  double length; // how long does the source appear to be (m)
  public double intensity; // how bright is the source (intensity units)
  public double[] correlationEnergyFeatures; // what does the source look like
  double bkgHypothesis; // is the sample consistent with background?
  double[] pcaEnergyFeatures = new double[4];
  double[] ratioEnergyFeatures = new double[13];

  // Information for viewer
  public double x1;
  public double x2;
  // Audit
  double[] spectrum; // Spectra shape
  public double ratio1;
  public double ratio2;

  // debug
  public void dump()
  {
    System.out.println("  position: " + position.getX() + "," + position.getY() + "," + position.getZ());
    System.out.println("  length: " + length);
    System.out.println("  intensity: " + intensity);
    System.out.println("  bkg_chisqr: " + bkgHypothesis);
    System.out.print("  spectrum: ");
    for (double d : spectrum)
    {
      System.out.print(d + " ");
    }
    System.out.println();
  }

  public JointSource(FeaturesDescription description)
  {
    this.description = (JointSourceDescription) description;
  }

  public JointSource(
          FeaturesDescription description,
          Vector3 position,
          double length,
          double intensity,
          double bkgHypothesis,
          double[] pcaEnergyFeatures,
          double[] ratioEnergyFeatures,
          double[] correlationEnergyFeatures
  )
  {
    this.description = (JointSourceDescription) description;
    this.position = position;
    this.length = length;
    this.intensity = intensity;
    this.bkgHypothesis = bkgHypothesis;
    this.pcaEnergyFeatures = pcaEnergyFeatures;
    this.ratioEnergyFeatures = ratioEnergyFeatures;
    this.correlationEnergyFeatures = correlationEnergyFeatures;
  }

  // Added for proj-ernie4 python scripts support in JPype env
  public JointSource(
          FeaturesDescription description,
          Vector3Impl position,
          double length,
          double intensity,
          double bkgHypothesis,
          double[] pcaEnergyFeatures,
          double[] ratioEnergyFeatures,
          double[] correlationEnergyFeatures
  )
  {
    this.description = (JointSourceDescription) description;
    this.position = position;
    this.length = length;
    this.intensity = intensity;
    this.bkgHypothesis = bkgHypothesis;
    this.pcaEnergyFeatures = pcaEnergyFeatures;
    this.ratioEnergyFeatures = ratioEnergyFeatures;
    this.correlationEnergyFeatures = correlationEnergyFeatures;
  }

  @Override
  public FeaturesDescription getDescription()
  {
    return this.description;
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