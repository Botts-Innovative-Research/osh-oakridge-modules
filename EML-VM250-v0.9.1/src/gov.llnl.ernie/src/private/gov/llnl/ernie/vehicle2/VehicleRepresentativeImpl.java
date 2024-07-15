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
package gov.llnl.ernie.vehicle2;

import gov.llnl.ernie.data.VehicleRepresentative;

/**
 * This represents one of the possible feature vectors to associate with a
 * vehicle.
 *
 * @author nelson85
 */
public class VehicleRepresentativeImpl implements VehicleRepresentative
{
  public double[] features;
  public double[] template;
  private int id;

  public VehicleRepresentativeImpl()
  {
  }

  public VehicleRepresentativeImpl(double[] values)
  {
    features = values.clone();
  }

  @Override
  public double[] getFeatures()
  {
    return features;
  }

  @Override
  public double[] getTemplate()
  {
    return template;
  }

  @Override
  public double getDistance(double[] features)
  {
    double distance = 0;
    if (features.length != this.features.length)
    {
      System.out.println("Incompatible feature set passed in!");
      return 0;
    }

    for (int i = 0; i < this.features.length; i++)
    {
      double metric = this.features[i] - features[i];
      distance += metric * metric;
    }

    return Math.sqrt(distance);
  }

  public void setId(int repIndex)
  {
    this.id = repIndex;
  }

  /**
   * @return the id
   */
  public int getId()
  {
    return id;
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