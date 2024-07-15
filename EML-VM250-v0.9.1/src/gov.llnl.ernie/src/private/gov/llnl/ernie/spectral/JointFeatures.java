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

/**
 *
 * @author nelson85
 */
public class JointFeatures implements Features 
{
  private static final long serialVersionUID 
          = gov.llnl.utility.UUIDUtilities.createLong("JointFeatures-v1");
  
  JointFeaturesDescription description;
  // JointFeatures for each of the sources
  public JointSource[] sources = new JointSource[2];
  // audit information
  public JointSeparation.Results separationResults;

  public JointFeatures(JointFeaturesDescription description)
  {
    this.description = description;
    sources[0] = new JointSource(description.sourceDescription);
    sources[1] = new JointSource(description.sourceDescription);
  }

  //under certain conditions we may need to swap Source1 and Source2
  void swapSources()
  {
    JointSource temp = sources[0];
    sources[0] = sources[1];
    sources[1] = temp;
  }

  // debug
  public void dump()
  {
    System.out.println("Source 1:");
    sources[0].dump();
    System.out.println("Source 2:");
    sources[1].dump();
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