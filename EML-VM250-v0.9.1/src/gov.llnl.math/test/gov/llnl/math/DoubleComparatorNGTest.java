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
public class DoubleComparatorNGTest
{

  @Test
  public void testComparePositive()
  {
    DoubleComparator comp = new DoubleComparator.Positive();
    Assert.assertTrue(comp.compare(-1, -1) == 0);
    Assert.assertTrue(comp.compare(-1, 0) < 0);
    Assert.assertTrue(comp.compare(-1, 1) < 0);
    Assert.assertTrue(comp.compare(0, -1) > 0);
    Assert.assertTrue(comp.compare(0, 0) == 0);
    Assert.assertTrue(comp.compare(0, 1) < 0);
    Assert.assertTrue(comp.compare(1, -1) > 0);
    Assert.assertTrue(comp.compare(1, 0) > 0);
    Assert.assertTrue(comp.compare(1, 1) == 0);
  }

  @Test
  public void testCompareNegative()
  {
    DoubleComparator comp = new DoubleComparator.Negative();
    Assert.assertTrue(comp.compare(-1, -1) == 0);
    Assert.assertTrue(comp.compare(-1, 0) > 0);
    Assert.assertTrue(comp.compare(-1, 1) > 0);
    Assert.assertTrue(comp.compare(0, -1) < 0);
    Assert.assertTrue(comp.compare(0, 0) == 0);
    Assert.assertTrue(comp.compare(0, 1) > 0);
    Assert.assertTrue(comp.compare(1, -1) < 0);
    Assert.assertTrue(comp.compare(1, 0) < 0);
    Assert.assertTrue(comp.compare(1, 1) == 0);
  }

  @Test
  public void testCompareAbsolute()
  {
    DoubleComparator comp = new DoubleComparator.Absolute();
    Assert.assertTrue(comp.compare(-1, -1) == 0, "-1 <=> -1");
    Assert.assertTrue(comp.compare(-1, 0) > 0, "-1 <=> 0");
    Assert.assertTrue(comp.compare(-1, 1) == 0, "-1 <=> 1");
    Assert.assertTrue(comp.compare(0, -1) < 0, "0 <=> -1");
    Assert.assertTrue(comp.compare(0, 0) == 0, "0 <=> 0");
    Assert.assertTrue(comp.compare(0, 1) < 0, "0 <=> 1");
    Assert.assertTrue(comp.compare(1, -1) == 0, "1 <=> -1");
    Assert.assertTrue(comp.compare(1, 0) > 0, "1 <=> 0");
    Assert.assertTrue(comp.compare(1, 1) == 0, "1 <=> 1");
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