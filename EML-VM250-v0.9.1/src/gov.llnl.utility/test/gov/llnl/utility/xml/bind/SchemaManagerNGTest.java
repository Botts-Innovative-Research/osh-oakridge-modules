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

import gov.llnl.utility.internal.xml.bind.SchemaManagerImpl;
import gov.llnl.utility.io.ReaderException;
import gov.llnl.utility.io.WriterException;
import java.net.URI;
import java.net.URL;
import static org.testng.Assert.*;
import org.testng.annotations.Test;
import org.xml.sax.EntityResolver;

/**
 *
 * @author pham21
 */
public class SchemaManagerNGTest
{

  public SchemaManagerNGTest()
  {
  }

  /**
   * Test of getInstance method, of class SchemaManager.
   */
  @Test
  public void testGetInstance()
  {
    System.out.println("getInstance");
    SchemaManager instance = new SchemaManagerImpl();
    SchemaManager result = SchemaManager.getInstance();
    assertNotNull(result);
    assertNotSame(result, instance);
    assertSame(result, SchemaManager.SELF);
  }

  /**
   * Test of alias method, of class SchemaManager.
   */
  @Test
  public void testAlias()
  {
    System.out.println("alias");
    SchemaManager instance = new TestSchemaManagerImpl();
    instance.alias(null, null);
  }

  /**
   * Test of getObjectClass method, of class SchemaManager.
   */
  @Test
  public void testGetObjectClass() throws Exception
  {
    System.out.println("getObjectClass");
    SchemaManager instance = new TestSchemaManagerImpl();
    assertNull(instance.getObjectClass("", ""));
  }

  /**
   * Test of findObjectReader method, of class SchemaManager.
   */
  @Test
  public void testFindObjectReader() throws Exception
  {
    System.out.println("findObjectReader");
    SchemaManager instance = new TestSchemaManagerImpl();
    assertNull(instance.findObjectReader(String.class));
  }

  /**
   * Test of findObjectWriter method, of class SchemaManager.
   */
  @Test
  public void testFindObjectWriter() throws Exception
  {
    System.out.println("findObjectWriter");
    SchemaManager instance = new TestSchemaManagerImpl();
    assertNull(instance.findObjectWriter(null, Double.class));
  }

  /**
   * Test of registerReaderFactory method, of class SchemaManager.
   */
  @Test
  public void testRegisterReaderFactory()
  {
    System.out.println("registerReaderFactory");
    SchemaManager instance = new TestSchemaManagerImpl();
    instance.registerReaderFactory(null);
  }

  /**
   * Test of registerReaderFor method, of class SchemaManager.
   */
  @Test
  public void testRegisterReaderFor()
  {
    System.out.println("registerReaderFor");
    SchemaManager instance = new TestSchemaManagerImpl();
    instance.registerReaderFor(null, null);
  }

  /**
   * Test of registerWriterFactory method, of class SchemaManager.
   */
  @Test
  public void testRegisterWriterFactory()
  {
    System.out.println("registerWriterFactory");
    SchemaManager instance = new TestSchemaManagerImpl();
    instance.registerWriterFactory(null);
  }

  /**
   * Test of registerWriterFor method, of class SchemaManager.
   */
  @Test
  public void testRegisterWriterFor()
  {
    System.out.println("registerWriterFor");
    SchemaManager instance = new TestSchemaManagerImpl();
    instance.registerWriterFor(null, null);
  }

  /**
   * Test of getEntityResolver method, of class SchemaManager.
   */
  @Test
  public void testGetEntityResolver()
  {
    System.out.println("getEntityResolver");
    SchemaManager instance = new TestSchemaManagerImpl();
    assertNull(instance.getEntityResolver());
  }
  
  /**
   * Test of ReaderFactory interface, of class SchemaManager.
   */
  @Test
  public void testReaderFactory()
  {
    System.out.println("ReaderFactory");
    TestSchemaManagerImpl sm = new TestSchemaManagerImpl();
    SchemaManager.ReaderFactory instance = sm.new TestReaderFactory();
    assertNull(instance.getReader(null));    
  }
  
  /**
   * Test of WriterFactory interface, of class SchemaManager.
   */
  @Test
  public void testWriterFactory()
  {
    System.out.println("getEntityResolver");
    TestSchemaManagerImpl sm = new TestSchemaManagerImpl();
    SchemaManager.WriterFactory instance = sm.new TestWriterFactory();
    assertNull(instance.getWriter(null));
  }
  

  public class TestSchemaManagerImpl implements SchemaManager
  {
    public void alias(URI systemId, URL location)
    {
    }

    public Class<?> getObjectClass(String namespaceURI, String name) throws ClassNotFoundException
    {
      return null;
    }

    public <T> ObjectReader<T> findObjectReader(Class<T> cls) throws ReaderException
    {
      return null;
    }

    public <T> ObjectWriter<T> findObjectWriter(WriterContext context, Class<T> cls) throws WriterException
    {
      return null;
    }

    public void registerReaderFactory(ReaderFactory factory)
    {
    }

    public void registerReaderFor(Class cls, Class<? extends ObjectReader> readerCls)
    {
    }

    public void registerWriterFactory(WriterFactory factory)
    {
    }

    public void registerWriterFor(Class cls, Class<? extends ObjectWriter> writerCls)
    {
    }

    public EntityResolver getEntityResolver()
    {
      return null;
    }

    public class TestReaderFactory implements SchemaManager.ReaderFactory
    {
      @Override
      public ObjectReader getReader(Class cls)
      {
        return null;
      }
    }

    public class TestWriterFactory implements SchemaManager.WriterFactory
    {
      @Override
      public ObjectWriter getWriter(Class cls)
      {
        return null;
      }
    }
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