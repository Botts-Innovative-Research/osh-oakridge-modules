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

import static org.testng.Assert.*;
import org.testng.annotations.Test;

/**
 *
 * @author pham21
 */
public class SingletonsNGTest
{

  static class TestSingleton implements Singletons.Singleton
  {
    private static TestSingleton instance;

    private TestSingleton()
    {
    }
    
    public static TestSingleton getInstance()
    {
      if(instance == null)
      {
        instance = new TestSingleton();
      }
      return instance;
    }
  }
  
  static class TestSingletonRequired implements Singletons.SingletonRequired
  {
    private static TestSingletonRequired instance;

    private TestSingletonRequired()
    {
    }
    
    public static TestSingletonRequired getInstance()
    {
      if(instance == null)
      {
        instance = new TestSingletonRequired();
      }
      return instance;
    }
  }
  
  static class MyTest1 
  {
    private static Integer getInstance()
    {
      return 0;
    }
  }
  
  static class MyTest2 
  {
    public static MyTest2 getInstance() throws Exception
    {
      throw new Exception("STOP! In the name of LOVE!");
    }    
  }

  public SingletonsNGTest()
  {
  }

  /**
   * Test of getSingleton method, of class Singletons.
   */
  @Test(expectedExceptions =
  {
    RuntimeException.class
  })
  public void testGetSingleton()
  {
    System.out.println("getSingleton");    
    assertEquals(Singletons.getSingleton(TestSingleton.class), TestSingleton.getInstance());
    assertEquals(Singletons.getSingleton(TestSingletonRequired.class), TestSingletonRequired.getInstance());
  
    // Test IllegalAccessException
    Singletons.getSingleton(MyTest1.class);
    
    // Test NoSuchMethodException
    Singletons.getSingleton(Integer.class);
    
    // Test InvocationTargetException
    Singletons.getSingleton(MyTest2.class);
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