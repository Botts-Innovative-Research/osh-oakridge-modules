/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.math.algebra;

import gov.llnl.math.algebra.Constraint.ConstraintDatum;
import gov.llnl.utility.UUIDUtilities;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Constraints are augmentations to the Nnlsq algorithm to additional
 * dynamically computed quadratic constraints.
 *
 * @author yao2
 */
public class Constraint implements Iterable<ConstraintDatum>, Serializable
{

  private static final long serialVersionUID = UUIDUtilities.createLong("Constraint");
  
  public static class ConstraintDatum implements Serializable
  {

    public int regressorId;
    public double coefficent;

    private ConstraintDatum(int regressorId, double coefficient)
    {
      this.regressorId = regressorId;
      this.coefficent = coefficient;
    }

    public int getId()
    {
      return regressorId;
    }

    public double getCoef()
    {
      return coefficent;
    }
  }

  public ArrayList<ConstraintDatum> points = new ArrayList<>();
  public double rhs_;

  /**
   * Allocate a constraint. 
   *
   * @param rhs
   */
  public Constraint(double rhs)
  {
    this.rhs_ = rhs;
  }

  public void add(int regressorId, double coef)
  {
    points.add(new ConstraintDatum(regressorId, coef));
  }
  
  public void weight(double d)
  {
    this.rhs_*=d;
    for (ConstraintDatum p:this.points)
    {
      p.coefficent*=d;
    }
  }

  @Override
  public Iterator<ConstraintDatum> iterator()
  {
    return points.iterator();
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