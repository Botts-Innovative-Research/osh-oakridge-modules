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

import gov.llnl.utility.TestSupport;
import gov.llnl.utility.TestSupport.TestElement;
import gov.llnl.utility.TestSupport.TestPackage;
import gov.llnl.utility.TestSupport.TestReaderContext;
import gov.llnl.utility.internal.xml.bind.ElementHandlerMapImpl;
import gov.llnl.utility.internal.xml.bind.HandlerContextImpl;
import gov.llnl.utility.internal.xml.bind.ReaderBuilderImpl;
import gov.llnl.utility.internal.xml.bind.ReaderDeclarationImpl;
import gov.llnl.utility.internal.xml.bind.readers.PrimitiveReaderImpl;
import gov.llnl.utility.io.ReaderException;
import gov.llnl.utility.xml.DomBuilder;
import gov.llnl.utility.xml.bind.ObjectReader.Section;
import java.lang.reflect.Field;
import static org.testng.Assert.*;
import org.testng.annotations.Test;
import org.xml.sax.Attributes;

/**
 *
 * @author pham21
 */
public class ObjectReaderNGTest
{

  public ObjectReaderNGTest()
  {
  }

  /**
   * Test of create method, of class ObjectReader.
   */
  @Test
  public void testCreate() throws Exception
  {
    System.out.println("create");
    ObjectReader<Long> result = ObjectReader.create(Long.class);
    PrimitiveReaderImpl pri = (PrimitiveReaderImpl) result;
    // Cast successful so test was successful but we'll call an assert
    assertSame(pri.getObjectClass(), Long.class);
  }

  /**
   * Test of start method, of class ObjectReader.
   */
  @Test
  public void testStart() throws Exception
  {
    System.out.println("start");
    ObjectReader instance = new ObjectReaderImpl();
    assertNull(instance.start(null));
  }

  /**
   * Test of getAttribute method, of class ObjectReader.
   */
  @Test
  public void testGetAttribute_4args()
  {
    System.out.println("getAttribute");
    org.xml.sax.helpers.AttributesImpl attr = new org.xml.sax.helpers.AttributesImpl();
    attr.addAttribute("uri", "localName", "one", "type", "1");

    assertEquals((int) ObjectReader.getAttribute(attr, "one", int.class, 0), 1);

    // Default value
    assertEquals((int) ObjectReader.getAttribute(attr, "two", int.class, 0), 0);
  }

  /**
   * Test of getAttribute method, of class ObjectReader.
   */
  @Test
  public void testGetAttribute_3args() throws Exception
  {
    System.out.println("getAttribute");
    org.xml.sax.helpers.AttributesImpl attr = new org.xml.sax.helpers.AttributesImpl();
    attr.addAttribute("uri", "localName", "one", "type", "1");

    assertEquals((int) ObjectReader.getAttribute(attr, "one", int.class), 1);

    try
    {
      ObjectReader.getAttribute(attr, "two", int.class);
    }
    catch (ReaderException re)
    {
      assertEquals(re.getMessage(), "Required attribute two was not found.");
    }
  }

  /**
   * Test of getHandlers method, of class ObjectReader.
   */
  @Test
  public void testGetHandlers() throws Exception
  {
    System.out.println("getHandlers");
    ObjectReader instance = new ObjectReaderImpl();
    assertNull(instance.getHandlers());
  }

  /**
   * Test of contents method, of class ObjectReader.
   */
  @Test
  public void testContents() throws Exception
  {
    System.out.println("contents");
    ObjectReader instance = new ObjectReaderImpl();
    assertNull(instance.contents(null));
  }

  /**
   * Test of end method, of class ObjectReader.
   */
  @Test
  public void testEnd() throws Exception
  {
    System.out.println("end");
    ObjectReader instance = new ObjectReaderImpl();
    assertNull(instance.end());
  }

  /**
   * Test of getHandlerKey method, of class ObjectReader.
   */
  @Test
  public void testGetHandlerKey()
  {
    // null check condiiton will not be true
    System.out.println("getHandlerKey");
    ObjectReader instance = new ObjectReaderImpl();
    assertEquals(instance.getHandlerKey(), "ObjectReaderImpl#TestPackage");
  }

  /**
   * Test of getSchemaType method, of class ObjectReader.
   */
  @Test
  public void testGetSchemaType()
  {
    System.out.println("getSchemaType");
    ObjectReader instance = new ObjectReaderImpl();
    assertEquals(instance.getSchemaType(), "ObjectReaderImpl");

    // Automatic naming
    instance = new TheOtherObjReader();
    assertEquals(instance.getSchemaType(), "ObjectReaderNGTest-TheOtherObjReader-type");
  }

  /**
   * Test of getObject method, of class ObjectReader.
   */
  @Test
  public void testGetObject()
  {
    System.out.println("getObject");
    TestReaderContext trc = new TestReaderContext();
    HandlerContextImpl hci = new HandlerContextImpl();
    Object obj = new Object();
    hci.targetObject = obj;
    trc.handlerContext = hci;
    ObjectReader instance = new ObjectReaderImpl();
    instance.setContext(trc);
    assertSame(instance.getObject(), obj);
  }

  /**
   * Test of getContext method, of class ObjectReader.
   */
  @Test
  public void testGetContext()
  {
    System.out.println("getContext");
    TestReaderContext trc = new TestReaderContext();
    ObjectReader instance = new ObjectReaderImpl();
    instance.setContext(trc);
    assertSame(instance.getContext(), trc);
  }

  /**
   * Test of setContext method, of class ObjectReader.
   */
  @Test
  public void testSetContext()
  {
    System.out.println("setContext");
    TestReaderContext context = new TestReaderContext();
    ObjectReader instance = new ObjectReaderImpl();
    instance.setContext(context);

    try
    {
      instance.setContext(new TestReaderContext());
    }
    catch (RuntimeException re)
    {
      assertEquals(re.getMessage(), "reentrant issue ");
    }
  }

  /**
   * Test of newBuilder method, of class ObjectReader.
   */
  @Test
  public void testNewBuilder_0args() throws IllegalArgumentException, NoSuchFieldException, IllegalAccessException
  {
    System.out.println("newBuilder");
    ObjectReader instance = new ObjectReaderImpl();
    ReaderBuilderImpl result = (ReaderBuilderImpl) instance.newBuilder();

    Field readerField = result.getClass().getDeclaredField("parentReader");
    readerField.setAccessible(true);
    assertSame(readerField.get(result), instance);
  }

  /**
   * Test of newBuilder method, of class ObjectReader.
   */
  @Test
  public void testNewBuilder_Class() throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException
  {
    System.out.println("newBuilder");
    ObjectReader instance = new ObjectReaderImpl();
    Reader.ReaderBuilder result = instance.newBuilder(String.class);

    Field readerField = result.getClass().getDeclaredField("parentReader");
    readerField.setAccessible(true);
    assertSame(readerField.get(result), instance);
  }

  /**
   * Test abstract class Section
   */
  @Test
  public void testSection() throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException, ReaderException
  {
    System.out.println("Section");
    ObjectReaderImpl instance = new ObjectReaderImpl();

    ObjectReaderImpl.ObjectSection os = instance.new ObjectSection();
    Integer targetObj = Integer.valueOf("0");
    TestReaderContext context = new TestReaderContext();
    HandlerContextImpl hci = new HandlerContextImpl();
    hci.targetObject = targetObj;
    context.handlerContext = hci;
    os.setContext(context);

    // Test ctor
    assertEquals(os.order, Reader.Order.FREE);
    assertEquals(os.name, "name");

    ReaderDeclarationImpl osDec = (ReaderDeclarationImpl) os.getDeclaration();
    assertEquals(osDec.pkg().getName(), "gov.llnl.utility.TestSupport$TestPackage");
    assertEquals(osDec.name(), "name");
    assertEquals(osDec.order(), Reader.Order.FREE);

    assertNull(os.start(null));
    assertNull(os.contents(null));
    assertNull(os.end());

    assertSame(os.getObject(), targetObj);
    assertSame(os.getObjectClass(), String.class);

    ReaderBuilderImpl rbi = (ReaderBuilderImpl) os.newBuilder();
    Field readerField = rbi.getClass().getDeclaredField("parentReader");
    readerField.setAccessible(true);
    assertSame(readerField.get(rbi), os);

    rbi = (ReaderBuilderImpl) os.newBuilder(String.class);
    readerField = rbi.getClass().getDeclaredField("parentReader");
    readerField.setAccessible(true);
    assertSame(readerField.get(rbi), os);

    os.setContext(null);
    SchemaBuilder builder = new SchemaBuilder();
    os.createSchemaType(builder);

    builder = new SchemaBuilder();
    TestElement te = new TestElement("TestElement");
    DomBuilder type = new DomBuilder(te);
    DomBuilder domOut = os.createSchemaElement(builder, "name", type, false);
  }

  /**
   * Test abstract class StringSection
   */
  @Test
  public void testStringSection() throws ReaderException
  {
    System.out.println("StringSection");
    ObjectReaderImpl instance = new ObjectReaderImpl();

    ObjectReaderImpl.StrObjectSection os = instance.new StrObjectSection();
    Integer targetObj = Integer.valueOf("0");
    TestReaderContext context = new TestReaderContext();
    HandlerContextImpl hci = new HandlerContextImpl();
    hci.targetObject = targetObj;
    context.handlerContext = hci;
    os.setContext(context);

    assertNull(os.start(null));
    assertNull(os.contents(null));
    assertNull(os.end());
    assertNull(os.getHandlers());

    assertSame(os.getObject(), targetObj);
    assertSame(os.getObjectClass(), String.class);
  }

  /**
   * Test nested class Imports
   */
  @Test
  public void testImports() throws ReaderException
  {
    System.out.println("Imports");
    ObjectReaderImpl instance = new ObjectReaderImpl();

    ObjectReaderImpl.Imports imports = instance.new Imports();

    assertEquals(imports.order, Reader.Order.FREE);
    assertEquals(imports.name, "imports");
    assertNotNull(imports.getHandlers());
    imports.createSchemaType(new SchemaBuilder());
  }

  /**
   * Test nested class Defines
   */
  @Test
  public void testDefines() throws ReaderException
  {
    System.out.println("Defines");
    ObjectReaderImpl instance = new ObjectReaderImpl();

    ObjectReaderImpl.Defines defines = instance.new Defines();

    assertEquals(defines.order, Reader.Order.FREE);
    assertEquals(defines.name, "defines");
    assertNotNull(defines.getHandlers());
  }

  // <editor-fold defaultstate="collapsed" desc="Support Classes">
  @Reader.Declaration(pkg = TestSupport.TestPackage.class, name = "ObjectReaderImpl", typeName = "ObjectReaderImpl")
  public class ObjectReaderImpl extends ObjectReader
  {
    @Override
    public Class getObjectClass()
    {
      return String.class;
    }

    public class ObjectSection extends Section
    {
      public ObjectSection()
      {
        super(Reader.Order.FREE, "name");
      }

      @Override
      public Reader.ElementHandlerMap getHandlers() throws ReaderException
      {
        return null;
      }
    }

    public class StrObjectSection extends StringSection
    {
      @Override
      public Object contents(String textContents) throws ReaderException
      {
        return null;
      }

    }
  }

  @Reader.Declaration(pkg = TestPackage.class, name = "TheOtherObjReader")
  public class TheOtherObjReader extends ObjectReader
  {
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