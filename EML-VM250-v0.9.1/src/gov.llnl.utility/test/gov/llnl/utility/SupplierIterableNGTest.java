/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.utility;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Supplier;
import static org.testng.Assert.*;
import org.testng.annotations.Test;

/**
 *
 * @author pham21
 */
public class SupplierIterableNGTest
{

  public SupplierIterableNGTest()
  {
  }

  /**
   * Test of iterator method, of class SupplierIterable.
   */
  @Test
  public void testIterator()
  {
    System.out.println("iterator");
    TestSupplier ts = new TestSupplier();
    SupplierIterable instance = new SupplierIterable(ts);
    
    Iterator<Integer> iterator = instance.iterator();
    Integer value = 0;
    while(iterator.hasNext())
    {
      assertEquals(iterator.next(), value);
      ++value;
    }
    assertEquals(iterator.hasNext(), false);
    assertNull(iterator.next());
  }

  class TestSupplier implements Supplier<Integer>
  {
    List<Integer> intList = new ArrayList<>()
    {
      {
        add(2);
        add(1);
        add(0);
      }
    };

    @Override
    public Integer get()
    {
      if(intList.isEmpty())
        return null;
      
      return intList.remove(intList.size() - 1);
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