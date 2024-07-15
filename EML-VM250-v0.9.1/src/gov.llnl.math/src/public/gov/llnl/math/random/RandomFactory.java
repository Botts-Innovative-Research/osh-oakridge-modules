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

import java.io.Serializable;

/**
 *
 * @author nelson85
 */
public abstract class RandomFactory implements Serializable
{
  private static final RandomGenerator defaultGenerator = new Random48();
  private RandomGenerator generator = null;

  public RandomFactory(RandomGenerator random)
  {
    generator = random;
  }

  public void setSeed(long seed)
  {
    generator.setSeed(seed);
  }

  public RandomGenerator getGenerator()
  {
    return generator;
  }

  public static RandomGenerator getDefaultGenerator()
  {
    return defaultGenerator;
  }

  // This proved hard to manage when a random generator has multiple
  // dependent generators.  Thus the generator can only be set at
  protected void setGenerator(RandomGenerator random)
  {
    this.generator = random;
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