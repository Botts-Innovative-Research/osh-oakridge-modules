/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.math;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 *
 * @author nelson85
 */
public class DoubleConditionalNGTest
{
  static final double EPS = 1e-6;

  @Test
  public void testEvaluateGreaterThan()
  {
    DoubleConditional cond = new DoubleConditional.GreaterThan(5);
    Assert.assertTrue(!cond.test(5.0 - EPS));
    Assert.assertTrue(!cond.test(5.0));
    Assert.assertTrue(cond.test(5.0 + EPS));
  }

  @Test
  public void testEvaluateGreaterThanEqual()
  {
    DoubleConditional cond = new DoubleConditional.GreaterThanEqual(5);
    Assert.assertTrue(!cond.test(5.0 - EPS));
    Assert.assertTrue(cond.test(5.0));
    Assert.assertTrue(cond.test(5.0 + EPS));
  }

  @Test
  public void testEvaluateLessThan()
  {
    DoubleConditional cond = new DoubleConditional.LessThan(5);
    Assert.assertTrue(cond.test(5.0 - EPS));
    Assert.assertTrue(!cond.test(5.0));
    Assert.assertTrue(!cond.test(5.0 + EPS));
  }

  @Test
  public void testEvaluateLessThanEqual()
  {
    DoubleConditional cond = new DoubleConditional.LessThanEqual(5);
    Assert.assertTrue(cond.test(5.0 - EPS));
    Assert.assertTrue(cond.test(5.0));
    Assert.assertTrue(!cond.test(5.0 + EPS));
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