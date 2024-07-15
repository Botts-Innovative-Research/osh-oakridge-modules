/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.ernie.rtk.view;

import gov.llnl.ernie.data.SensorView;
import gov.llnl.math.euclidean.Vector3;
import gov.llnl.math.euclidean.Vector3Ops;
import gov.llnl.math.euclidean.Versor;
import gov.llnl.utility.xml.bind.ReaderInfo;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author nelson85
 */
@ReaderInfo(SensorViewCompositeReader.class)
public class SensorViewComposite implements SensorView
{
  List<SensorView> faces;
  Vector3 origin;
  Versor orientation;

  public SensorViewComposite(List<? extends SensorView> faces, Vector3 origin, Versor orientation)
  {
    this.faces = new ArrayList<>(faces);
    this.origin = origin;
    this.orientation = orientation;
  }

  SensorViewComposite()
  {
  }

  @Override
  public Vector3 getOrigin()
  {
    return origin;
  }

  @Override
  public Versor getOrientation()
  {
    return orientation;
  }

  @Override
  public double computeSolidAngle(Vector3 v)
  {
    double out = 0;
    Vector3 v2 = Vector3Ops.subtract(v, origin);
    Vector3 v3 = orientation.inv().rotate(v2);
    for (SensorView face : faces)
    {
      double q = face.computeSolidAngle(v3);
      out += q;
    }
    return out;
  }


    public String auditSolidAngle(Vector3 v)
  {
    StringBuilder sb=new StringBuilder();
    double out = 0;
    Vector3 v2 = Vector3Ops.subtract(v, origin);
    Vector3 v3 = orientation.inv().rotate(v2);
    sb.append("System ").append(v3.toString()).append("\n");
    for (SensorView face : faces)
    {
      double q = face.computeSolidAngle(v3);
      sb.append("Face ").append(face.toString())
              .append(" ").append(Vector3Ops.subtract(v3, face.getOrigin()).toString())
              .append(" ").append(q).append("\n");
      out += q;
    }
    return sb.toString();
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