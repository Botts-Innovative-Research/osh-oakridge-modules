/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.utility.xml.bind;

import gov.llnl.utility.TestSupport.TestReader;
import gov.llnl.utility.internal.xml.bind.DocumentReaderImpl;
import gov.llnl.utility.io.ReaderException;
import gov.llnl.utility.xml.bind.DocumentReader.Hook;
import gov.llnl.utility.xml.bind.DocumentReader.Hooks;
import static gov.llnl.utility.xml.bind.DocumentReader.SCHEMA_SOURCE;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Map;
import static org.testng.Assert.*;
import org.testng.annotations.Test;
import org.xml.sax.InputSource;

/**
 *
 * @author pham21
 */
public class DocumentReaderNGTest
{

  public DocumentReaderNGTest()
  {
  }

  /**
   * Test of create method, of class DocumentReader.
   */
  @Test
  public void testCreate_ObjectReader()
  {
    System.out.println("create");
    TestReader writer = TestReader.of(Double.class);
    DocumentReaderImpl result = (DocumentReaderImpl) DocumentReader.create(writer);
    assertSame(result.getObjectReader(), writer);
    assertEquals(result.getProperty(SCHEMA_SOURCE), "http://utility.llnl.gov/schema/utility.xsd");
  }

  /**
   * Test of create method, of class DocumentReader.
   */
  @Test
  public void testCreate_Class() throws Exception
  {
    System.out.println("create");
    DocumentReaderImpl result = (DocumentReaderImpl) DocumentReader.create(Double.class);
    assertNull(result.getObjectReader());
    assertNull(result.getProperty(SCHEMA_SOURCE));
  }

  /**
   * Test of clearContext method, of class DocumentReader.
   */
  @Test
  public void testClearContext()
  {
    System.out.println("clearContext");
    DocumentReader instance = new TestDocReaderImpl();
    instance.clearContext();
  }

  /**
   * Test of getContext method, of class DocumentReader.
   */
  @Test
  public void testGetContext()
  {
    System.out.println("getContext");
    DocumentReader instance = new TestDocReaderImpl();
    assertNull(instance.getContext());
  }

  /**
   * Test of loadFile method, of class DocumentReader.
   */
  @Test
  public void testLoadFile() throws Exception
  {
    System.out.println("loadFile");
    assertNull((new TestDocReaderImpl()).loadFile(null));
  }

  /**
   * Test of loadResource method, of class DocumentReader.
   */
  @Test
  public void testLoadResource() throws Exception
  {
    System.out.println("loadResource");
    assertNull((new TestDocReaderImpl()).loadResource(null));
  }

  /**
   * Test of loadStream method, of class DocumentReader.
   */
  @Test
  public void testLoadStream() throws Exception
  {
    System.out.println("loadStream");
    assertNull((new TestDocReaderImpl()).loadStream(null));
  }

  /**
   * Test of loadURL method, of class DocumentReader.
   */
  @Test
  public void testLoadURL() throws Exception
  {
    System.out.println("loadURL");
    assertNull((new TestDocReaderImpl()).loadURL(null));
  }

  /**
   * Test of loadSource method, of class DocumentReader.
   */
  @Test
  public void testLoadSource() throws Exception
  {
    System.out.println("loadSource");
    assertNull((new TestDocReaderImpl()).loadSource(null));
  }

  /**
   * Test of setErrorHandler method, of class DocumentReader.
   */
  @Test
  public void testSetErrorHandler()
  {
    System.out.println("setErrorHandler");
    DocumentReader instance = new TestDocReaderImpl();
    instance.setErrorHandler(null);
  }

  /**
   * Test of setPropertyHandler method, of class DocumentReader.
   */
  @Test
  public void testSetPropertyHandler()
  {
    System.out.println("setPropertyHandler");
    DocumentReader instance = new TestDocReaderImpl();
    instance.setPropertyHandler(null);
  }

  /**
   * Test of getObjectReader method, of class DocumentReader.
   */
  @Test
  public void testGetObjectReader()
  {
    System.out.println("getObjectReader");
    assertNull((new TestDocReaderImpl()).getObjectReader());
  }

  /**
   * Test of getProperties method, of class DocumentReader.
   */
  @Test
  public void testGetProperties()
  {
    System.out.println("getProperties");
    assertNull((new TestDocReaderImpl()).getProperties());
  }

  /**
   * Test of Hooks/Hook, of class DocumentReader.
   */
  @Test
  public void testHooks()
  {
    System.out.println("Hooks/Hook");
    Class<Hook> hookClass;
    Hook hook = null;
    Hooks annotation = (Hooks) TestHookInteface.class.getAnnotation(Hooks.class);
    if (annotation != null)
    {
      hookClass = (Class<Hook>) annotation.value();

      try
      {
        hook = hookClass.getConstructor().newInstance();
      }
      catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException ex)
      {
        // Reflection failed not use.
        System.out.println(ex);
      }
    }
    hook.startDocument(null);
    hook.endDocument(null);
  }

  // <editor-fold defaultstate="collapsed" desc="Support Classes">
  @DocumentReader.Hooks(TestHooks.class)
  public interface TestHookInteface
  {
  }

  public class TestDocReaderImpl implements DocumentReader
  {
    @Override
    public void clearContext()
    {
    }

    @Override
    public ReaderContext getContext()
    {
      return null;
    }

    @Override
    public Object loadFile(Path file) throws ReaderException, FileNotFoundException, IOException
    {
      return null;
    }

    @Override
    public Object loadResource(String resourceName) throws ReaderException, IOException
    {
      return null;
    }

    @Override
    public Object loadStream(InputStream stream) throws ReaderException
    {
      return null;
    }

    @Override
    public Object loadURL(URL url) throws IOException, ReaderException
    {
      return null;
    }

    @Override
    public Object loadSource(InputSource inputSource) throws ReaderException
    {
      return null;
    }

    @Override
    public void setErrorHandler(ReaderContext.ExceptionHandler exceptionHandler)
    {
    }

    @Override
    public void setPropertyHandler(PropertyMap handler)
    {
    }

    @Override
    public ObjectReader getObjectReader()
    {
      return null;
    }

    @Override
    public Map<String, Object> getProperties()
    {
      return null;
    }
  }
  // </editor-fold>

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