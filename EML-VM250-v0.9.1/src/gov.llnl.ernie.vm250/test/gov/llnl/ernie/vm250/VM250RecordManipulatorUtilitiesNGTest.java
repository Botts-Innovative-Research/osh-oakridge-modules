/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gov.llnl.ernie.vm250;

import gov.llnl.ernie.vm250.data.VM250Record;
import gov.llnl.math.matrix.Matrix;
import static org.testng.Assert.*;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 *
 * @author pham21
 */
public class VM250RecordManipulatorUtilitiesNGTest
{
  
  public VM250RecordManipulatorUtilitiesNGTest()
  {
  }

  @BeforeClass
  public static void setUpClass() throws Exception
  {
  }

  @AfterClass
  public static void tearDownClass() throws Exception
  {
  }

  @BeforeMethod
  public void setUpMethod() throws Exception
  {
  }

  @AfterMethod
  public void tearDownMethod() throws Exception
  {
  }

  /**
   * Test of alterRecordVelocity method, of class VM250RecordManipulatorUtilities.
   */
  @Test
  public void testAlterRecordVelocity() throws Exception
  {
    System.out.println("alterRecordVelocity");
    VM250Record record = null;
    double vel1New = 0.0;
    double vel2New = 0.0;
    VM250Record expResult = null;
    VM250Record result = VM250RecordManipulatorUtilities.alterRecordVelocity(record, vel1New, vel2New);
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of interp1 method, of class VM250RecordManipulatorUtilities.
   */
  @Test
  public void testInterp1() throws Exception
  {
    System.out.println("interp1");
    double[] x = null;
    Matrix y = null;
    double[] x1 = null;
    Matrix expResult = null;
    Matrix result = VM250RecordManipulatorUtilities.interp1(x, y, x1);
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of computeProfile method, of class VM250RecordManipulatorUtilities.
   */
  @Test
  public void testComputeProfile() throws Exception
  {
    System.out.println("computeProfile");
    double[] position = null;
    VelocityPairs velocityProfile = null;
    VM250RecordManipulatorUtilities.computeProfile(position, velocityProfile);
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