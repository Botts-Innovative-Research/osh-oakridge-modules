/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.ernie.vm250;

import gov.llnl.ernie.data.VehicleClass;
import gov.llnl.ernie.data.VehicleRepresentative;
import gov.llnl.ernie.vehicle.VehicleInfoImpl;
import gov.llnl.ernie.vehicle2.VehicleRepresentativeImpl;
import gov.llnl.math.DoubleArray;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.DoubleStream;

/**
 *
 * @author guensche1
 */
public class VM250VehicleClass implements VehicleClass
{
  private static final long serialVersionUID
          = gov.llnl.utility.UUIDUtilities.createLong("VehicleClassImpl-v1");
  public VehicleInfoImpl vehicleInfo = new VehicleInfoImpl();
  public double[] template;
  public double scale;
  public ArrayList<BackgroundModel> backgroundModels = new ArrayList<>();
  VehicleRepresentative representative = null;

  public double getScale()
  {
    return scale;
  }

  public VM250VehicleClass()
  {
  }

  @Override
  public VehicleInfoImpl getInfo()
  {
    return this.vehicleInfo;
  }

  public List<BackgroundModel> getBackgroundModels()
  {
    return Collections.unmodifiableList(this.backgroundModels);
  }

  public void addBackgroundModel(BackgroundModel bm)
  {
    this.backgroundModels.add(bm);
  }

  public void setTemplate(double[] template)
  {
    this.template = template;
  }
  
  public double[] getTemplate()
  {
    return this.template;
  }

  /**
   * Re-map template onto the range (-0.5,0.5) and recompute the scale factor.
   * Meant for VehicleClasses with full VPS beam data (e.g. RPM8)
   */
  public void renormalizeTemplate()
  {
    double min = DoubleArray.min(template);
    double max = DoubleArray.max(template);
    template = DoubleStream.of(template).map(p -> (p - min) / (max - min) - 0.5).toArray();
    scale = Math.sqrt(DoubleArray.sumSqr(template));
  }

  @Override
  public List<VehicleRepresentative> getRepresentatives()
  {
    if (this.representative == null)
    {
      representative = new VehicleRepresentativeImpl(this.template);
    }
    return List.of(this.representative);
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