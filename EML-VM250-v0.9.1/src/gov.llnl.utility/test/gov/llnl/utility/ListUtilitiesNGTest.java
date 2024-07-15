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

import gov.llnl.utility.internal.ReverseCollection;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.function.ToDoubleFunction;
import static org.testng.Assert.*;
import org.testng.annotations.Test;

/**
 *
 * @author pham21
 */
public class ListUtilitiesNGTest
{

  public ListUtilitiesNGTest()
  {
  }

  /**
   * Test of newList method, of class ListUtilities.
   */
  @Test
  public void testNewList()
  {
    System.out.println("newList");

    Iterable<Integer> iterable = new ArrayList<>()
    {
      {
        add(1);
        add(2);
        add(3);
      }
    };
    List expResult = new ArrayList<Integer>()
    {
      {
        add(1);
        add(2);
        add(3);
      }
    };

    List result = ListUtilities.newList(iterable);
    assertEquals(result, expResult);
  }

  /**
   * Test of permute method, of class ListUtilities.
   */
  @Test
  public void testPermute()
  {
    System.out.println("permute");
    List<Integer> control = new ArrayList()
    {
      {
        add(1);
        add(2);
        add(3);
        add(4);
        add(5);
        add(6);
        add(7);
        add(8);
        add(9);
        add(10);
      }
    };

    List<Integer> list = new ArrayList<>()
    {
      {
        add(1);
        add(2);
        add(3);
        add(4);
        add(5);
        add(6);
        add(7);
        add(8);
        add(9);
        add(10);
      }
    };
    int range = list.size();
    ListUtilities.permute(list, range);
    // Do we have the same list object
    assertEquals(list, list);

    // We need at least one element not in its original place
    int swappedCounter = 0;
    for (int i = 0; i < control.size(); ++i)
    {
      if (control.get(i).intValue() != list.get(i).intValue())
      {
        ++swappedCounter;
      }
    }
    assertTrue(swappedCounter > 0);
  }

  /**
   * Test of unique method, of class ListUtilities.
   */
  @Test
  public void testUnique()
  {
    System.out.println("unique");
    List<Integer> list = new ArrayList<>()
    {
      {
        add(1);
        add(4);
        add(2);
        add(2);
        add(1);
        add(2);
        add(3);
        add(4);
        add(3);
        add(1);
        add(3);
        add(4);
      }
    };
    List expResult = new ArrayList<>()
    {
      {
        add(1);
        add(2);
        add(3);
        add(4);
      }
    };
    List<Integer> result = ListUtilities.unique(list);
    assertEquals(result, expResult);
  }

  /**
   * Test of getDoubleField method, of class ListUtilities.
   */
  @Test
  public void testGetDoubleField()
  {
    System.out.println("getDoubleField");
    List<Integer> list = new ArrayList<>()
    {
      {
        add(1);
        add(2);
        add(3);
        add(4);
      }
    };
    ToDoubleFunction<Integer> mapper = value -> value * 2.0;
    double[] expResult = new double[]
    {
      2.0, 4.0, 6.0, 8.0
    };
    double[] result = ListUtilities.getDoubleField(list, mapper);
    assertEquals(result, expResult);
  }

  /**
   * Test of reverse method, of class ListUtilities.
   */
  @Test
  public void testReverse_1args_1()
  {
    System.out.println("reverse");
    List<Integer> list = new ArrayList<>()
    {
      {
        add(1);
        add(2);
        add(3);
        add(4);
      }
    };
    Integer[] expResult = new Integer[]
    {
      4, 3, 2, 1
    };
    ReverseCollection<Integer> result = ListUtilities.reverse(list);

    Integer[] resultArray = new Integer[result.size()];
    result.toArray(resultArray);
    assertEquals(resultArray, expResult);
  }

  /**
   * Test of reverse method, of class ListUtilities.
   */
  @Test
  public void testReverse_1args_2()
  {
    System.out.println("reverse");
    Integer[] array = new Integer[]
    {
      1, 2, 3, 4
    };
    Integer[] expResult = new Integer[]
    {
      4, 3, 2, 1
    };
    ReverseCollection<Integer> result = ListUtilities.reverse(array);
    Integer[] resultArray = new Integer[result.size()];
    result.toArray(resultArray);
    assertEquals(resultArray, expResult);
  }

  /**
   * Test of findFirst method, of class ListUtilities.
   */
  @Test
  public void testFindFirst()
  {
    System.out.println("findFirst");
    List<Integer> list = new ArrayList<>()
    {
      {
        add(1);
        add(2);
        add(3);
        add(4);
      }
    };
    ListUtilities.FindMatcher<Integer> matcher = value -> value > 2;
    ListIterator result = ListUtilities.findFirst(list, matcher);
    assertEquals(result.next(), 3);
    assertEquals(result.next(), 4);
    assertTrue(result.hasNext() == false);
  }

  /**
   * Test of replaceFirst method, of class ListUtilities.
   */
  @Test
  public void testReplaceFirst()
  {
    System.out.println("replaceFirst");
    List<Integer> list = new ArrayList<>()
    {
      {
        add(1);
        add(2);
        add(3);
        add(4);
      }
    };
    Integer replacement = 21;
    ListUtilities.FindMatcher<Integer> matcher = value -> value == 2;
    Integer result = ListUtilities.replaceFirst(list, replacement, matcher);
    assertEquals(result.intValue(), 2);
    assertEquals(list.get(1), replacement);
    assertNull(ListUtilities.replaceFirst(list, replacement, matcher));
  }

  /**
   * Test of shrinkTo method, of class ListUtilities.
   */
  @Test(expectedExceptions =
  {
    IllegalArgumentException.class
  })
  public void testShrinkTo()
  {
    System.out.println("shrinkTo");
    List<Integer> list = new ArrayList<>()
    {
      {
        add(1);
        add(2);
        add(3);
        add(4);
      }
    };
    int size = 2;

    List<Integer> result = ListUtilities.shrinkTo(list, size);
    // result and list is the same object
    assertEquals(result, list);
    assertEquals(System.identityHashCode(result), System.identityHashCode(list));
    assertEquals(list.size(), size);
    assertEquals(result.size(), size);

    // Test IllegalArgumentException
    ListUtilities.shrinkTo(list, list.size() * 2);
  }

  /**
   * Test of ensureSize method, of class ListUtilities.
   */
  @Test
  public void testEnsureSize_ArrayList_int()
  {
    System.out.println("ensureSize");
    ArrayList<Integer> list = new ArrayList<>(0);
    int requiredSize = 10;
    ListUtilities.ensureSize(list, requiredSize);
    
    assertEquals(list.size(), requiredSize);
  }

  /**
   * Test of ensureSize method, of class ListUtilities.
   */
  @Test
  public void testEnsureSize_List_int()
  {
    System.out.println("ensureSize");
    List<Integer> list = new ArrayList<>(0);
    int requiredSize = 10;
    ListUtilities.ensureSize(list, requiredSize);
    assertEquals(list.size(), requiredSize);
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