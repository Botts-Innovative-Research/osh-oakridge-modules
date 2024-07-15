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

import gov.llnl.utility.io.ReaderException;
import gov.llnl.utility.xml.bind.ObjectReader;
import gov.llnl.utility.xml.bind.Reader;
import gov.llnl.utility.xml.bind.SchemaManager;
import java.lang.reflect.Method;
import java.util.EnumSet;
import static org.testng.Assert.*;
import org.testng.annotations.Test;
import org.xml.sax.Attributes;

/**
 *
 * @author pham21
 */
public class AnyContentsNGTest
{

  public AnyContentsNGTest()
  {
  }

  /**
   * Test of AnyContents constructor, of class AnyContents.
   */
  @Test
  public void testConstructor_1()
  {
    System.out.println("AnyContents constructor 1");
    AnyContents instance = new AnyContents(String.class);

    assertSame(instance.type, String.class);
    assertNull(instance.options);
  }

  /**
   * Test of AnyContents constructor, of class AnyContents.
   */
  @Test
  public void testConstructor_2()
  {
    System.out.println("AnyContents constructor 2");
    EnumSet<Reader.Options> opts = EnumSet.of(Reader.Options.NO_CACHE, Reader.Options.NO_ID);
    AnyContents instance = new AnyContents(String.class, Reader.Options.NO_CACHE, Reader.Options.NO_ID);
    assertSame(instance.type, String.class);
    assertTrue(opts.equals(instance.options));
  }

  /**
   * Test of getObjectClass method, of class AnyContents.
   */
  @Test
  public void testGetObjectClass()
  {
    System.out.println("getObjectClass");
    AnyContents instance = new AnyContents(String.class);
    assertSame(instance.getObjectClass(), String.class);
  }

  /**
   * Test of getReader method, of class AnyContents.
   */
  @Test(expectedExceptions =
  {
    RuntimeException.class
  })
  public void testGetReader() throws Exception
  {
    System.out.println("getReader");
    String namespaceURI = "uri";
    String localName = "localName";
    String qName = "qName";

    // Type that goes into AnyContents has to be the same with something inside
    // the classmap of SchemaManagerImpl
    // Use reflection to add information into the classmap
    SchemaManagerImpl smi = (SchemaManagerImpl) SchemaManager.getInstance();
    Method registerClassMethod = smi.getClass().getDeclaredMethod("registerClass", String.class, String.class, String.class);
    registerClassMethod.setAccessible(true);
    registerClassMethod.invoke(smi, namespaceURI, localName, "long");

    AnyContents instance = new AnyContents(Long.class);

    ObjectReader result = instance.getReader(namespaceURI, localName, "", null);
    assertSame(result.getObjectClass(), Long.class);

    // Test ReaderException
    try
    {
      instance.type = String.class;
      result = instance.getReader(namespaceURI, localName, "", null);
    }
    catch (ReaderException re)
    {
      assertEquals(re.getMessage(), Long.class + " is not castable to " + instance.getObjectClass());
    }

    // Test ClassNotFoundException/RuntimeException
    result = instance.getReader("", "", "", null);
  }

  /**
   * Test of getOptions method, of class AnyContents.
   */
  @Test
  public void testGetOptions()
  {
    System.out.println("getOptions");
    EnumSet<Reader.Options> opts = EnumSet.of(Reader.Options.NO_CACHE, Reader.Options.NO_ID);
    AnyContents instance = new AnyContents(String.class, Reader.Options.NO_CACHE, Reader.Options.NO_ID);
    assertSame(instance.type, String.class);
    assertTrue(opts.equals(instance.getOptions()));
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