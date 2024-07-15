/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.math.internal.euclidean;

import gov.llnl.math.euclidean.Versor;

/**
 *
 * @author nelson85
 */
public final class VersorInv implements Versor
{
  Versor versor_;

  public VersorInv(Versor v)
  {
    this.versor_ = v;
  }

  @Override
  public double getI()
  {
    return -versor_.getI();
  }

  @Override
  public double getJ()
  {
    return -versor_.getJ();
  }

  @Override
  public double getK()
  {
    return -versor_.getK();
  }

  @Override
  public double getU()
  {
    return versor_.getU();
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