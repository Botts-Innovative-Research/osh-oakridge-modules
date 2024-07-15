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

import gov.llnl.ernie.analysis.VehicleClassifier;
import gov.llnl.ernie.data.VehicleClass;
import gov.llnl.ernie.data.VehicleRepresentative;
import gov.llnl.ernie.vehicle.BackgroundModelImpl;
import gov.llnl.ernie.vehicle.VehicleInfoImpl;
import gov.llnl.math.matrix.Matrix;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author nelson85
 */
public class Vehicle2ClassImpl implements VehicleClass
{
  List<VehicleRepresentative> representatives = new ArrayList<>();
  
  private static final long serialVersionUID
          = gov.llnl.utility.UUIDUtilities.createLong("VehicleClassImpl-v1");
  public VehicleInfoImpl vehicleInfo = new VehicleInfoImpl();
  public ArrayList<BackgroundModel> backgroundModels = new ArrayList<>();
  transient public VehicleClassifier classifier;

  public Vehicle2ClassImpl()
  {
  }

  @Override
  public VehicleInfoImpl getInfo()
  {
    return this.vehicleInfo;
  }

  @Override
  public List<BackgroundModel> getBackgroundModels()
  {
    return Collections.unmodifiableList(this.backgroundModels);
  }

  public void clearBackgroundModels()
  {
    this.backgroundModels.clear();
  }
  
  public void addBackgroundModel(Matrix bm)
  {
    BackgroundModelImpl model = new BackgroundModelImpl();
    model.setMatrix(bm);
    this.backgroundModels.add(model);
  }
  
  public void addBackgroundModel(BackgroundModel bm)
  {
    this.backgroundModels.add(bm);
  }

  public void addRepresentative(VehicleRepresentative vr)
  {
    representatives.add(vr);
  }

  @Override
  public List<VehicleRepresentative> getRepresentatives()
  {
    return Collections.unmodifiableList(representatives);
  }

  /**
   * @return the id
   */
  public int getId()
  {
    return vehicleInfo.getId();
  }

  /**
   * @param id the id to set
   */
  public void setId(int id)
  {
    vehicleInfo.setId(id);
  }
  
  @Override
  public VehicleClassifier getClassifier()
  {
    return this.classifier;
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