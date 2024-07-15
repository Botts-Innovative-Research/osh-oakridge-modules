/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.utility.internal.xml.bind.readers;

import gov.llnl.utility.ClassUtilities;
import gov.llnl.utility.io.ReaderException;
import gov.llnl.utility.xml.bind.Reader;
import static org.testng.Assert.*;
import org.testng.annotations.Test;

/**
 *
 * @author pham21
 */
public class PrimitiveReaderImplNGTest
{
  
  public PrimitiveReaderImplNGTest()
  {
  }

  /**
   * Test of contents method, of class PrimitiveReaderImpl.
   */
  @Test(expectedExceptions =
  {
    ReaderException.class
  })
  public void testContents() throws Exception
  {
    System.out.println("contents");
    PrimitiveReaderImpl pri = new PrimitiveReaderImpl(ClassUtilities.INTEGER_PRIMITIVE);
    assertEquals((Integer)pri.contents("360"), Integer.valueOf(360));
   
    // Test ReaderException
    pri.contents("ccggaag");
  }

  /**
   * Test of getObjectClass method, of class PrimitiveReaderImpl.
   */
  @Test
  public void testGetObjectClass()
  {
    System.out.println("getObjectClass");
    PrimitiveReaderImpl pri = new PrimitiveReaderImpl(ClassUtilities.DOUBLE_PRIMITIVE);
    assertEquals(pri.getObjectClass(), Double.class);
  }

  /**
   * Test of getTextContents method, of class PrimitiveReaderImpl.
   */
  @Test
  public void testGetTextContents()
  {
    System.out.println("getTextContents");
    PrimitiveReaderImpl pri = new PrimitiveReaderImpl(ClassUtilities.LONG_PRIMITIVE);
    Reader.TextContents tc = pri.getTextContents();
    assertEquals(tc.base(), "xs:long");
    assertEquals(tc.annotationType(), Reader.TextContents.class);
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