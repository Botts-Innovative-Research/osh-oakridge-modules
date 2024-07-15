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

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
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
public class PathUtilitiesNGTest
{
  
  public PathUtilitiesNGTest()
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
   * Test of joinStrings method, of class PathUtilities.
   */
  @Test
  public void testJoinStrings()
  {
    System.out.println("joinStrings");
    Collection<Path> collection = null;
    String expResult = "";
    String result = PathUtilities.joinStrings(collection);
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getComponents method, of class PathUtilities.
   */
  @Test
  public void testGetComponents()
  {
    System.out.println("getComponents");
    Path p = null;
    List expResult = null;
    List result = PathUtilities.getComponents(p);
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of resolve method, of class PathUtilities.
   */
  @Test
  public void testResolve() throws Exception
  {
    System.out.println("resolve");
    Path file = null;
    Path[] search = null;
    Path expResult = null;
    Path result = PathUtilities.resolve(file, search);
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of isGzip method, of class PathUtilities.
   */
  @Test
  public void testIsGzip() throws Exception
  {
    System.out.println("isGzip");
    Path file = null;
    boolean expResult = false;
    boolean result = PathUtilities.isGzip(file);
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getFileExtensionPosition method, of class PathUtilities.
   */
  @Test
  public void testGetFileExtensionPosition()
  {
    System.out.println("getFileExtensionPosition");
    Path path = null;
    int expResult = 0;
    int result = PathUtilities.getFileExtensionPosition(path);
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getFileExtension method, of class PathUtilities.
   */
  @Test
  public void testGetFileExtension()
  {
    System.out.println("getFileExtension");
    Path path = null;
    String expResult = "";
    String result = PathUtilities.getFileExtension(path);
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of changeExtension method, of class PathUtilities.
   */
  @Test
  public void testChangeExtension()
  {
    System.out.println("changeExtension");
    Path path = null;
    String ext = "";
    Path expResult = null;
    Path result = PathUtilities.changeExtension(path, ext);
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of fileBasename method, of class PathUtilities.
   */
  @Test
  public void testFileBasename()
  {
    System.out.println("fileBasename");
    Path path = null;
    String expResult = "";
    String result = PathUtilities.fileBasename(path);
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of md5Checksum method, of class PathUtilities.
   */
  @Test
  public void testMd5Checksum() throws Exception
  {
    System.out.println("md5Checksum");
    Path file = null;
    String expResult = "";
    String result = PathUtilities.md5Checksum(file);
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of newPath method, of class PathUtilities.
   */
  @Test
  public void testNewPath_String()
  {
    System.out.println("newPath");
    String path = "";
    PathUtilities instance = new PathUtilities();
    Path expResult = null;
    Path result = instance.newPath(path);
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of newPath method, of class PathUtilities.
   */
  @Test
  public void testNewPath_String_String()
  {
    System.out.println("newPath");
    String directory = "";
    String filename = "";
    PathUtilities instance = new PathUtilities();
    Path expResult = null;
    Path result = instance.newPath(directory, filename);
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of chdir method, of class PathUtilities.
   */
  @Test
  public void testChdir()
  {
    System.out.println("chdir");
    String dir = "";
    PathUtilities.chdir(dir);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of findPaths method, of class PathUtilities.
   */
  @Test
  public void testFindPaths_Path_String() throws Exception
  {
    System.out.println("findPaths");
    Path directory = null;
    String pattern = "";
    Collection expResult = null;
    Collection result = PathUtilities.findPaths(directory, pattern);
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of findPaths method, of class PathUtilities.
   */
  @Test
  public void testFindPaths_3args() throws Exception
  {
    System.out.println("findPaths");
    Path directory = null;
    String pattern = "";
    boolean recursive = false;
    Collection expResult = null;
    Collection result = PathUtilities.findPaths(directory, pattern, recursive);
    assertEquals(result, expResult);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of findFileRecursive method, of class PathUtilities.
   */
  @Test
  public void testFindFileRecursive() throws Exception
  {
    System.out.println("findFileRecursive");
    Path directory = null;
    Path filename = null;
    Path expResult = null;
    Path result = PathUtilities.findFileRecursive(directory, filename);
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