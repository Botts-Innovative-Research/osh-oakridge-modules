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
import java.util.List;
import java.util.ListIterator;
import org.testng.annotations.Test;

/**
 *
 * @author pham21
 */
public class UnmodifiableListNGTest
{

  static class TestUnmodifiableList extends UnmodifiableList<Integer>
  {
    @Override
    public int size()
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean isEmpty()
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean contains(Object o)
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Iterator<Integer> iterator()
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Object[] toArray()
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public <T> T[] toArray(T[] a)
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Integer get(int index)
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int indexOf(Object o)
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int lastIndexOf(Object o)
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ListIterator<Integer> listIterator()
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ListIterator<Integer> listIterator(int index)
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<Integer> subList(int fromIndex, int toIndex)
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
  }

  TestUnmodifiableList testUnmodifiableList;
  public UnmodifiableListNGTest()
  {
    testUnmodifiableList = new TestUnmodifiableList();
  }

  /**
   * Test of add method, of class UnmodifiableList.
   */
  @Test(expectedExceptions =
  {
    UnsupportedOperationException.class
  })
  public void testAdd_GenericType()
  {
    System.out.println("add");
    testUnmodifiableList.add(1);
  }

  /**
   * Test of remove method, of class UnmodifiableList.
   */
  @Test(expectedExceptions =
  {
    UnsupportedOperationException.class
  })
  public void testRemove_Object()
  {
    System.out.println("remove");
    testUnmodifiableList.remove(1);
  }

  /**
   * Test of containsAll method, of class UnmodifiableList.
   */
  @Test(expectedExceptions =
  {
    UnsupportedOperationException.class
  })
  public void testContainsAll()
  {
    System.out.println("containsAll");
    testUnmodifiableList.containsAll(new TestUnmodifiableList());
  }

  /**
   * Test of addAll method, of class UnmodifiableList.
   */
  @Test(expectedExceptions =
  {
    UnsupportedOperationException.class
  })
  public void testAddAll_Collection()
  {
    System.out.println("addAll");
    testUnmodifiableList.addAll(new TestUnmodifiableList());
  }

  /**
   * Test of addAll method, of class UnmodifiableList.
   */
  @Test(expectedExceptions =
  {
    UnsupportedOperationException.class
  })
  public void testAddAll_int_Collection()
  {
    System.out.println("addAll");
    testUnmodifiableList.addAll(0, new TestUnmodifiableList());
  }

  /**
   * Test of removeAll method, of class UnmodifiableList.
   */
  @Test(expectedExceptions =
  {
    UnsupportedOperationException.class
  })
  public void testRemoveAll()
  {
    System.out.println("removeAll");
    testUnmodifiableList.removeAll(new TestUnmodifiableList());
  }

  /**
   * Test of retainAll method, of class UnmodifiableList.
   */
  @Test(expectedExceptions =
  {
    UnsupportedOperationException.class
  })
  public void testRetainAll()
  {
    System.out.println("retainAll");
    testUnmodifiableList.retainAll(new TestUnmodifiableList());
  }

  /**
   * Test of clear method, of class UnmodifiableList.
   */
  @Test(expectedExceptions =
  {
    UnsupportedOperationException.class
  })
  public void testClear()
  {
    System.out.println("clear");
    testUnmodifiableList.clear();
  }

  /**
   * Test of set method, of class UnmodifiableList.
   */
  @Test(expectedExceptions =
  {
    UnsupportedOperationException.class
  })
  public void testSet()
  {
    System.out.println("set");
    testUnmodifiableList.set(0, 0);
  }

  /**
   * Test of add method, of class UnmodifiableList.
   */
  @Test(expectedExceptions =
  {
    UnsupportedOperationException.class
  })
  public void testAdd_int_GenericType()
  {
    System.out.println("add");
    testUnmodifiableList.add(0, 0);
  }

  /**
   * Test of remove method, of class UnmodifiableList.
   */
  @Test(expectedExceptions =
  {
    UnsupportedOperationException.class
  })
  public void testRemove_int()
  {
    System.out.println("remove");    
    testUnmodifiableList.remove(0);
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