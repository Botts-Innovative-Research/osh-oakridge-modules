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

import gov.llnl.ernie.analysis.VehicleClassifier;
import gov.llnl.math.matrix.MatrixColumnTable;
import java.io.Serializable;
import java.util.List;

/**
 * Class of vehicle stored in the vehicle classifier.
 *
 * This includes the vehicle info, the template for matching the vehicle, and
 * the background models.
 *
 * @author nelson85
 */
public interface VehicleClass extends Serializable
{
  VehicleInfo getInfo();

  default VehicleClassifier getClassifier()
  {
    return null;
  }
  
  /**
   * The template features for the vehicle.
   *
   * The features vary from portal type.  For RPM8 this will be numbers between
   * -0.5 and 0.5 depending on whether the portal is occupied or not.
   *
   * @return the template
   */
  List<VehicleRepresentative> getRepresentatives();

  List<BackgroundModel> getBackgroundModels();

  public interface BackgroundModel
  {
    int size();

    double[] get(int sensorId);

    MatrixColumnTable toMatrix();
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