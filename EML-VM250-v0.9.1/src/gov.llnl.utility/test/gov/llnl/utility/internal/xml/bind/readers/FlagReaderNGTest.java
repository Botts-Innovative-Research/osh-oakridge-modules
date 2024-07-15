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

import gov.llnl.utility.xml.bind.Reader;
import static org.testng.Assert.*;
import org.testng.annotations.Test;

/**
 *
 * @author pham21
 */
public class FlagReaderNGTest
{
  
  public FlagReaderNGTest()
  {
  }

  /**
   * Test of contents method, of class FlagReader.
   */
  @Test
  public void testContents() throws Exception
  {
    System.out.println("contents");
    FlagReader fr = new FlagReader();
    assertTrue(fr.contents(""));
    assertTrue(fr.contents("true"));
    assertTrue(fr.contents("Truth") == false);
  }

  /**
   * Test of getTextContents method, of class FlagReader.
   */
  @Test
  public void testGetTextContents()
  {
    System.out.println("getTextContents");
    FlagReader fr = new FlagReader();
    Reader.TextContents tc = fr.getTextContents();
    assertEquals(tc.base(), "util:boolean-optional");
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