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

import gov.llnl.ernie.internal.data.LaneImpl;
import gov.llnl.math.DoubleArray;
import gov.llnl.math.euclidean.Vector3;
import gov.llnl.utility.UUIDUtilities;
import java.io.Serializable;

/**
 *
 * @author nelson85
 */
public class LaneRatioConversion implements LaneImpl.RatioConversion, Serializable
{
   private static final long serialVersionUID = UUIDUtilities.createLong("LaneRatioConversion-v1");
  public final LaneImpl lane;
  public final double[] yFactors;
  public final double[] zFactors;

  public LaneRatioConversion(LaneImpl lane, double[] yFactors, double[] zFactors)
  {
    this.lane = lane;
    this.yFactors = yFactors;
    this.zFactors = zFactors;
  }

  @Override
  public Vector3 apply(double ratio1, double ratio2)
  {
    // These need to match the definition in the training script.
    double[] q =
    {
      ratio1 * ratio1 * ratio2, ratio1 * ratio2 * ratio2, ratio1 * ratio1 * ratio1, ratio1 * ratio1, ratio1, ratio2 * ratio2 * ratio2, ratio2 * ratio2, ratio2, 1
    };
    double y = 0;
    if (yFactors != null)
      y = DoubleArray.multiplyInner(yFactors, q);
    double z = 0;
    if (zFactors != null)
      z = DoubleArray.multiplyInner(zFactors, q);
    if (Math.abs(y) > lane.laneWidth / 2)
      y = Math.signum(y) * lane.laneWidth / 2;
    if (z < 0)
      z = 0;
    if (z > lane.laneHeight)
      z = lane.laneHeight;
    return Vector3.of(0, y, z);
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