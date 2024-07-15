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

import java.util.Iterator;
import static org.testng.Assert.*;
import org.testng.annotations.Test;

/**
 *
 * @author pham21
 */
public class LimitedSizeQueueNGTest
{
  
  public LimitedSizeQueueNGTest()
  {
  }

  /**
   * Test of enqueueAll method, of class LimitedSizeQueue.
   */
  @Test
  public void testEnqueueAll()
  {
    System.out.println("enqueueAll");
//    Iterable<T> in = null;
//    LimitedSizeQueue instance = null;
//    Iterable expResult = null;
//    Iterable result = instance.enqueueAll(in);
//    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of enqueue method, of class LimitedSizeQueue.
   */
  @Test
  public void testEnqueue()
  {
    System.out.println("enqueue");
    Object in = null;
    LimitedSizeQueue instance = null;
    Object expResult = null;
    Object result = instance.enqueue(in);
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of clear method, of class LimitedSizeQueue.
   */
  @Test
  public void testClear()
  {
    System.out.println("clear");
    LimitedSizeQueue instance = null;
    instance.clear();
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of get method, of class LimitedSizeQueue.
   */
  @Test
  public void testGet()
  {
    System.out.println("get");
    int i = 0;
    LimitedSizeQueue instance = null;
    Object expResult = null;
    Object result = instance.get(i);
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of size method, of class LimitedSizeQueue.
   */
  @Test
  public void testSize()
  {
    System.out.println("size");
    LimitedSizeQueue instance = null;
    int expResult = 0;
    int result = instance.size();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of isFull method, of class LimitedSizeQueue.
   */
  @Test
  public void testIsFull()
  {
    System.out.println("isFull");
    LimitedSizeQueue instance = null;
    boolean expResult = false;
    boolean result = instance.isFull();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of iterator method, of class LimitedSizeQueue.
   */
  @Test
  public void testIterator()
  {
    System.out.println("iterator");
    LimitedSizeQueue instance = null;
    Iterator expResult = null;
    Iterator result = instance.iterator();
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
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