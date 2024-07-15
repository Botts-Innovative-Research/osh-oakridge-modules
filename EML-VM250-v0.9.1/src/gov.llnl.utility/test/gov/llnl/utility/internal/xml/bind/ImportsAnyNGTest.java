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

/**
 *
 * @author pham21
 */
public class ImportsAnyNGTest
{

  public ImportsAnyNGTest()
  {
  }

  /**
   * Test of getObjectClass method, of class ImportsAny.
   */
  @Test
  public void testGetObjectClass()
  {
    System.out.println("getObjectClass");
    ImportsAny instance = new ImportsAny();
    assertSame(instance.getObjectClass(), Object.class);
  }

  /**
   * Test of getReader method, of class ImportsAny.
   */
  @Test
  public void testGetReader() throws Exception
  {
    System.out.println("getReader");
    String namespaceURI = "namespaceURI";
    String localName = "localName";
    String qName = "qName";

    // Use reflection to add information into the SchemaManagerImpl.classmap
    SchemaManagerImpl smi = (SchemaManagerImpl) SchemaManager.getInstance();
    Method registerClassMethod = smi.getClass().getDeclaredMethod("registerClass", String.class, String.class, String.class);
    registerClassMethod.setAccessible(true);
    registerClassMethod.invoke(smi, namespaceURI, localName, "long");

    ImportsAny instance = new ImportsAny();
    ObjectReader result = instance.getReader(namespaceURI, localName, qName, null);
    assertSame(result.getClass(), ImportsReader.class);

    // Cannot test ReaderException(namespaceURI + "#" + localName + " is not a document type.");
    // Because by default, Reader.Declaration.document() return true.
    // I have to either update SchemaManagerImpl.getObjectClass method to use a 
    // classloader in the class.forname method
    // or create a dummy class in the source directory. Both options are not good.    
    
    try
    { // Test ClassNotFoundException/ReaderException
      instance.getReader("", "", "", null);
    }
    catch (ReaderException re)
    {
      assertSame(re.getCause().getClass(), ClassNotFoundException.class);
    }

  }

  /**
   * Test of getOptions method, of class ImportsAny.
   */
  @Test
  public void testGetOptions()
  {
    System.out.println("getOptions");
    ImportsAny instance = new ImportsAny();
    EnumSet<Reader.Options> options = EnumSet.of(Reader.Options.ANY_ALL);
    assertTrue(options.equals(instance.getOptions()));
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