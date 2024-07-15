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

import static gov.llnl.utility.ClassUtilities.LONG_PRIMITIVE;
import gov.llnl.utility.TestSupport.TestElement;
import gov.llnl.utility.io.ReaderException;
import gov.llnl.utility.xml.DomBuilder;
import gov.llnl.utility.xml.bind.Reader;
import gov.llnl.utility.xml.bind.SchemaManager;
import java.lang.reflect.Method;
import java.util.EnumSet;
import java.util.function.BiConsumer;
import static org.testng.Assert.*;
import org.testng.annotations.Test;

/**
 *
 * @author pham21
 */
public class AnyHandlerImplNGTest
{

  public AnyHandlerImplNGTest()
  {
  }

  /**
   * Test of AnyHandlerImpl constructor, of class AnyHandlerImpl.
   */
  @Test
  public void testConstructor()
  {
    System.out.println("AnyHandlerImpl constructor");
    BiConsumer<Object, Object> doNothing = (a, b) -> System.out.print("");
    String target = "target";
    AnyContents anyContents = new AnyContents(String.class);
    EnumSet<Reader.Options> flags = EnumSet.of(Reader.Options.ANY_ALL, Reader.Options.NO_CACHE);
    AnyHandlerImpl instance = new AnyHandlerImpl(
            flags, target, target.getClass(), doNothing, anyContents
    );

    assertEquals(instance.getKey(), "##any");
    assertSame(instance.options, flags);
    assertSame(instance.target, target);
    assertEquals(instance.getTargetClass(), target.getClass());
    assertSame(instance.method, doNothing);
    assertSame(instance.any, anyContents);
  }

  /**
   * Test of getHandler method, of class AnyHandlerImpl.
   */
  @Test
  public void testGetHandler() throws Exception
  {
    System.out.println("getHandler");

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

    // Setup
    org.xml.sax.helpers.AttributesImpl attributes = new org.xml.sax.helpers.AttributesImpl();
    attributes.addAttribute(namespaceURI, localName, qName, "String", "value");

    BiConsumer<Object, Object> doNothing = (a, b) -> System.out.print("");
    String target = "target";
    AnyContents anyContents = new AnyContents(Long.class); // <- has an effect on the test
    EnumSet<Reader.Options> flags = EnumSet.of(Reader.Options.ANY_ALL, Reader.Options.NO_CACHE);
    AnyHandlerImpl instance = new AnyHandlerImpl(
            flags, target, target.getClass(), doNothing, anyContents
    );

    ReaderHandler rh = instance.getHandler(namespaceURI, localName, qName, attributes);
    assertEquals(rh.reader.getObjectClass(), LONG_PRIMITIVE.getBoxedType());
  }

  /**
   * Test of onStart method, of class AnyHandlerImpl.
   */
  @Test(expectedExceptions =
  {
    ReaderException.class
  })
  public void testOnStart() throws Exception
  {
    System.out.println("onStart");
    BiConsumer<Object, Object> doNothing = (a, b) -> System.out.print("");
    String target = "target";
    AnyContents anyContents = new AnyContents(String.class);
    EnumSet<Reader.Options> flags = EnumSet.of(Reader.Options.ANY_ALL, Reader.Options.NO_CACHE);
    AnyHandlerImpl instance = new AnyHandlerImpl(
            flags, target, target.getClass(), doNothing, anyContents
    );
    instance.onStart(null, null);
  }

  /**
   * Test of onEnd method, of class AnyHandlerImpl.
   */
  @Test(expectedExceptions =
  {
    ReaderException.class
  })
  public void testOnEnd() throws Exception
  {
    System.out.println("onEnd");
    BiConsumer<Object, Object> doNothing = (a, b) -> System.out.print("");
    String target = "target";
    AnyContents anyContents = new AnyContents(String.class);
    EnumSet<Reader.Options> flags = EnumSet.of(Reader.Options.ANY_ALL, Reader.Options.NO_CACHE);
    AnyHandlerImpl instance = new AnyHandlerImpl(
            flags, target, target.getClass(), doNothing, anyContents
    );
    instance.onEnd(null);
  }

  /**
   * Test of createSchemaElement method, of class AnyHandlerImpl.
   */
  @Test
  public void testCreateSchemaElement() throws Exception
  {
    System.out.println("createSchemaElement");
    
    TestElement testElement = new TestElement("TestElement");
    DomBuilder group = new DomBuilder(testElement);
    
    AnyContents anyContents = new AnyContents(String.class);
    EnumSet<Reader.Options> flags = EnumSet.of(Reader.Options.ANY_ALL, Reader.Options.NO_CACHE, Reader.Options.REQUIRED);
    
    AnyHandlerImpl instance = new AnyHandlerImpl(flags, null, null, null, anyContents);        
 
    instance.createSchemaElement(null, group);
   
    TestElement childElement = (TestElement) testElement.childrenList.get(0);  
    for(Reader.Options op : flags)
    {
      if(op.getKey() != null)
      {
        assertTrue(childElement.attrMap.containsKey(op.getKey()));
        assertEquals(childElement.attrMap.get(op.getKey()), op.getValue());
      }
    }
  }
  
  /**
   * Test of merge method, of class AnyHandlerImpl.
   */
  @Test
  public void testMerge() throws Exception
  {
    AnyContents anyContents = new AnyContents(String.class);
    EnumSet<Reader.Options> flags0 = EnumSet.of(Reader.Options.ANY_ALL);
    EnumSet<Reader.Options> flags1 = EnumSet.of(Reader.Options.ANY_SKIP, Reader.Options.UNBOUNDED);
    EnumSet<Reader.Options> flags3 = EnumSet.copyOf(flags0);
    flags3.addAll(flags1);
    
    AnyHandlerImpl instance = new AnyHandlerImpl(flags0, null, null, null, anyContents);     
    
    Method mergeMethod = instance.getClass().getDeclaredMethod("merge", EnumSet.class, EnumSet.class);
    mergeMethod.setAccessible(true);
    
    assertSame(mergeMethod.invoke(instance, flags0, null), flags0);
    assertSame(mergeMethod.invoke(instance, null, flags1), flags1);
    assertTrue(flags3.equals(mergeMethod.invoke(instance, flags0, flags1)));
    
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