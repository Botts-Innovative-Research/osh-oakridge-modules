/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.math.random;

import gov.llnl.utility.UUIDUtilities;
import java.io.Serializable;

/**
 *
 * @author nelson85
 */
public class UniformRandom extends RandomFactory implements Serializable
{
  private static final long serialVersionUID = UUIDUtilities.createLong("UniformRandom");

  public UniformRandom()
  {
    super(getDefaultGenerator());
  }

  public UniformRandom(RandomGenerator random)
  {
    super(random);
  }

  public RandomVariable newVariable(double min, double max)
  {
    return new UniformVariable(min, max);
  }

  class UniformVariable implements RandomVariable
  {
    double min, range;

    private UniformVariable(double min, double max)
    {
      this.range = max - min;
      this.min = min;
    }

    @Override
    public double next()
    {
      return getGenerator().nextDouble() * range + min;
    }
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