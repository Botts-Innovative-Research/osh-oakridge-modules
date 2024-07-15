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

import gov.llnl.math.random.Random48;
import java.util.ArrayList;
import java.util.List;
import org.testng.Assert;
import org.testng.annotations.Test;
import gov.llnl.math.random.RandomGenerator;

/**
 *
 * @author nelson85
 */
public class LongArrayNGTest
{
  RandomGenerator rg=new Random48();
  
  public LongArrayNGTest()
  {
  }

  /**
   * Test of toPrimitives method, of class LongArray.
   */
  @Test
  public void testToPrimitives()
  {
    List<Long> input=new ArrayList<Long>();
    for (int i=0;i<100;i++)
      input.add(Double.doubleToLongBits(rg.nextDouble()));
    long[] out=LongArray.toPrimitives(input);
    for (int i=0;i<out.length;++i)
      Assert.assertEquals((long)input.get(i), out[i]);
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