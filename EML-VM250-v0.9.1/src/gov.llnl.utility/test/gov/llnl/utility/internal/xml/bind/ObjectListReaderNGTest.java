/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.utility.internal.xml.bind;

import gov.llnl.utility.TestSupport.TestPackage;
import gov.llnl.utility.TestSupport.TestPolyReader;
import gov.llnl.utility.TestSupport.TestReader;
import gov.llnl.utility.UtilityPackage;
import gov.llnl.utility.internal.xml.bind.ObjectListReader;
import gov.llnl.utility.xml.bind.AnyReader;
import gov.llnl.utility.xml.bind.Reader;
import java.lang.reflect.Field;
import java.util.List;
import static org.testng.Assert.*;
import org.testng.annotations.Test;

/**
 *
 * @author pham21
 */
public class ObjectListReaderNGTest
{

  public ObjectListReaderNGTest()
  {
  }

  /**
   * Test of ObjectListReader constructor, of class ObjectListReader.
   */
  @Test
  public void testConstructor() throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException
  {
    System.out.println("ObjectListReader constructor");
    TestReader testReader = TestReader.of(String.class);
    String name = "name";
    ObjectListReader instance = new ObjectListReader(testReader, name, UtilityPackage.getInstance());

    // Use reflection to get fields' information
    Field readerField = ObjectListReader.class.getDeclaredField("reader");
    Field nameField = ObjectListReader.class.getDeclaredField("name");
    Field schemaField = ObjectListReader.class.getDeclaredField("schema");
    readerField.setAccessible(true);
    nameField.setAccessible(true);
    schemaField.setAccessible(true);

    assertSame(readerField.get(instance), testReader);
    assertSame(nameField.get(instance), name);
    assertSame(schemaField.get(instance).getClass(), UtilityPackage.class);
  }

  /**
   * Test of getDeclaration method, of class ObjectListReader.
   */
  @Test
  public void testGetDeclaration()
  {
    System.out.println("getDeclaration");
    TestReader testReader = TestReader.of(String.class);
    String name = "name";
    ObjectListReader instance = new ObjectListReader(testReader, name, UtilityPackage.getInstance());

    Reader.Declaration dec = instance.getDeclaration();
    assertSame(dec.pkg(), UtilityPackage.class);
    assertSame(dec.name(), name);
  }

  /**
   * Test of start method, of class ObjectListReader.
   */
  @Test
  public void testStart() throws Exception
  {
    System.out.println("start");
    ObjectListReader instance = new ObjectListReader(null, null, null);
    List result = instance.start(null);
    assertEquals(result.size(), 0);
  }

  /**
   * Test of getHandlers method, of class ObjectListReader.
   */
  @Test
  public void testGetHandlers() throws Exception
  {
    System.out.println("getHandlers");
    // PolymorphicReader branch
    TestPolyReader tpr = new TestPolyReader();
    String name = "name";
    ObjectListReader instance = new ObjectListReader(tpr, name, TestPackage.getInstance());
    Reader.ElementHandlerMap result = instance.getHandlers();
    // No exception means it passed

    // AnyReader branch
    AnyReader anyReader = AnyReader.of(String.class);
    instance = new ObjectListReader(anyReader, name, TestPackage.getInstance());
    result = instance.getHandlers();
    // No exception means it passed

    // Other reader branch
    TestReader tr = TestReader.of(String.class);
    instance = new ObjectListReader(tr, name, TestPackage.getInstance());
    result = instance.getHandlers();
    // No exception means it passed
  }

  /**
   * Test of getObjectClass method, of class ObjectListReader.
   */
  @Test
  public void testGetObjectClass()
  {
    System.out.println("getObjectClass");
    ObjectListReader instance = new ObjectListReader(null, null, null);
    assertSame(instance.getObjectClass(), List.class);
  }

  /**
   * Test of getSchemaType method, of class ObjectListReader.
   */
  @Test
  public void testGetSchemaType()
  {
    System.out.println("getSchemaType");
    TestReader testReader = TestReader.of(String.class);
    ObjectListReader instance = new ObjectListReader(testReader, null, null);
    assertEquals(instance.getSchemaType(), "List-" + testReader.getSchemaType());
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