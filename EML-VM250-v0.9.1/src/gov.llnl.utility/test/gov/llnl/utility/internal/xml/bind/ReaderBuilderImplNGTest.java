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

import static gov.llnl.utility.ClassUtilities.INTEGER_PRIMITIVE;
import gov.llnl.utility.TestSupport.TestPackage;
import gov.llnl.utility.TestSupport.TestReader;
import gov.llnl.utility.TestSupport.TestReaderMixed;
import gov.llnl.utility.TestSupport.TestSectionImpl;
import gov.llnl.utility.internal.xml.bind.ReaderBuilderImpl.Producer;
import gov.llnl.utility.internal.xml.bind.readers.PrimitiveReaderImpl;
import gov.llnl.utility.io.ReaderException;
import gov.llnl.utility.xml.bind.AnyReader;
import gov.llnl.utility.xml.bind.ObjectReader;
import gov.llnl.utility.xml.bind.Reader;
import java.util.EnumSet;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import static org.testng.Assert.*;
import org.testng.annotations.Test;
import org.xml.sax.Attributes;

/**
 *
 * @author pham21
 */
public class ReaderBuilderImplNGTest
{

  public ReaderBuilderImplNGTest()
  {
  }

  /**
   * Test of ReaderBuilderImpl constructor, of class ReaderBuilderImpl.
   */
  @Test
  public void testConstructor()
  {
    System.out.println("ReaderBuilderImpl constructor");

    // Referenceable is true
    TestReader tr = TestReader.of(String.class);
    ReaderBuilderImpl instance = new ReaderBuilderImpl(tr);
    assertEquals(instance.uriName, "#" + TestPackage.namespace);
    assertSame(instance.parentReader, tr);
    assertNotNull(instance.parentGroup);
    assertNotNull(instance.handlerList);
    assertNull(instance.target);
    assertSame(instance.baseClass, String.class);
    assertNull(instance.lastHandler);
    assertNull(instance.resultClass);
    assertNull(instance.elementName);
    assertNull(instance.producer);
    assertEquals(instance.parentGroup.flags, EnumSet.of(Reader.Options.OPTIONAL));

    // contentRequired is true
    TestReaderMixed trm = TestReaderMixed.of(String.class);
    instance = new ReaderBuilderImpl(trm);
    assertEquals(instance.uriName, "#" + TestPackage.namespace);
    assertSame(instance.parentReader, trm);
    assertNotNull(instance.parentGroup);
    assertNotNull(instance.handlerList);
    assertNull(instance.target);
    assertSame(instance.baseClass, String.class);
    assertNull(instance.lastHandler);
    assertNull(instance.resultClass);
    assertNull(instance.elementName);
    assertNull(instance.producer);
    assertEquals(instance.parentGroup.flags, EnumSet.of(Reader.Options.REQUIRED));
  }

  /**
   * Test of ReaderBuilderImpl copy-constructor, of class ReaderBuilderImpl.
   */
  @Test
  public void testCopyConstructor()
  {
    System.out.println("ReaderBuilderImpl copy-constructor");
    TestReader tr = TestReader.of(String.class);
    ReaderBuilderImpl rbi = new ReaderBuilderImpl(tr);

    ReaderBuilderImpl instance = new ReaderBuilderImpl(rbi);
    assertSame(instance.uriName, rbi.uriName);
    assertSame(instance.parentReader, rbi.parentReader);
    assertSame(instance.target, rbi.target);
    assertSame(instance.baseClass, rbi.baseClass);
    assertSame(instance.resultClass, rbi.resultClass);
    assertSame(instance.elementName, rbi.elementName);
    assertSame(instance.handlerList, rbi.handlerList);
    assertSame(instance.parentGroup, rbi.parentGroup);
  }

  /**
   * Test of getHandlers method, of class ReaderBuilderImpl.
   */
  @Test
  public void testGetHandlers() throws Exception
  {
    System.out.println("getHandlers");
    TestReader tr = TestReader.of(String.class);
    ReaderBuilderImpl instance = new ReaderBuilderImpl(tr);

    Reader.ElementHandlerMap ehm = instance.getHandlers();
    assertNotNull(ehm);
    assertSame(ehm.getClass(), ElementHandlerMapImpl.class);

    // test ReaderException
    TestProducer tp = new TestProducer(tr);
    instance.producer = tp;
    try
    {
      ehm = instance.getHandlers();
    }
    catch (ReaderException re)
    {
      assertEquals(re.getMessage(), "Incomplete reader on getHandlers " + tp);
    }
  }

  /**
   * Test of element method, of class ReaderBuilderImpl.
   */
  @Test
  public void testElement()
  {
    System.out.println("element");
    TestReader tr = TestReader.of(String.class);
    ReaderBuilderImpl instance = new ReaderBuilderImpl(tr);
    String name = "name";

    ReaderBuilderImpl result = instance.element(name);
    assertSame(result, instance);
    assertNull(result.lastHandler);
    assertEquals(result.elementName, name + instance.uriName);
  }

  /**
   * Test of contents method, of class ReaderBuilderImpl.
   */
  @Test
  public void testContents() throws Exception
  {
    System.out.println("contents");
    // contents called reader method so more extensive test in reader method
    TestReader tr = TestReader.of(String.class);
    ReaderBuilderImpl instance = new ReaderBuilderImpl(tr);
    ReaderBuilderImpl result = instance.contents(Integer.class);
    assertSame(result, instance);
  }

  /**
   * Test of any method, of class ReaderBuilderImpl.
   */
  @Test
  public void testAny() throws Exception
  {
    System.out.println("any");
    // any called reader method so more extensive test in reader method
    TestReader tr = TestReader.of(String.class);
    ReaderBuilderImpl instance = new ReaderBuilderImpl(tr);
    ReaderBuilderImpl result = instance.any(Integer.class);
    assertSame(result, instance);
  }

  /**
   * Test of reader method, of class ReaderBuilderImpl.
   */
  @Test
  public void testReader() throws Exception
  {
    System.out.println("reader");
    TestReader tr = TestReader.of(String.class);
    ReaderBuilderImpl instance = new ReaderBuilderImpl(tr);
    ReaderBuilderImpl result = instance.reader(ObjectReader.create(Integer.class));
    assertSame(result, instance);
    assertSame(result.resultClass, Integer.class);
    assertNull(result.lastHandler);
    assertNotNull(result.producer);
    assertEquals(result.producer.toString(), "reader for " + Integer.class.toString());

    // Test first producer null check
    TestReaderMixed trm = TestReaderMixed.of(Double.class);
    instance = new ReaderBuilderImpl(trm);
    instance.producer = new TestProducer(trm);
    AnyReader ar = AnyReader.of(Long.class);
    try
    {
      instance.reader(ar);
    }
    catch (ReaderException re)
    {
      assertEquals(re.getMessage(), "element contents redefined,\n    previous="
              + instance.producer.toString() + "\n    new=" + ar.toString());
    }

    // Test reader null check
    instance.producer = null;
    try
    {
      instance.reader(null);
    }
    catch (ReaderException re)
    {
      assertEquals(re.getMessage(), "reader is null");
    }

  }

  /**
   * Test of readers method, of class ReaderBuilderImpl.
   */
  @Test
  public void testReaders() throws Exception
  {
    System.out.println("readers");
    TestReader tr = TestReader.of(Double.class);
    TestReaderMixed trm = TestReaderMixed.of(Long.class);
    ReaderBuilderImpl instance = new ReaderBuilderImpl(tr);

    Reader.ReaderBuilderCall result = instance.readers(Integer.class, tr, trm);
    assertSame(result, instance);
    assertSame(((ReaderBuilderImpl) result).resultClass, Integer.class);
    // Can not access the array of ObjectReaders from the static private inner class
    // ReadersProducer. So we're just going to check if producer exists.
    assertNotNull(((ReaderBuilderImpl) result).producer);
    assertNull(((ReaderBuilderImpl) result).lastHandler);

    // Test producer not null check
    instance.producer = new TestProducer(trm);
    try
    {
      instance.readers(Integer.class, tr, trm);
    }
    catch (ReaderException re)
    {
      assertEquals(re.getMessage(), "element contents redefined");
    }

    // Test readers null check
    instance.producer = null;
    try
    {
      instance.readers(Integer.class, null);
    }
    catch (ReaderException re)
    {
      assertEquals(re.getMessage(), "reader is null");
    }

    // Test one reader
    result = instance.readers(Integer.class, tr);
  }

  /**
   * Test of using method, of class ReaderBuilderImpl.
   */
  @Test
  public void testUsing()
  {
    System.out.println("using");
    TestReader tr = TestReader.of(Double.class);
    ReaderBuilderImpl instance = new ReaderBuilderImpl(tr);
    instance.target = "target";
    instance.baseClass = String.class;

    Integer target = Integer.parseInt("0");
    Reader.ReaderBuilder result = instance.using(target);
    assertNotSame(result, instance);

    ReaderBuilderImpl resultImpl = (ReaderBuilderImpl) result;
    assertSame(resultImpl.target, target);
    assertSame(resultImpl.baseClass, target.getClass());
    assertNotSame(resultImpl.target, instance.target);
    assertNotSame(resultImpl.baseClass, instance.baseClass);
    assertSame(resultImpl.uriName, instance.uriName);
    assertSame(resultImpl.parentReader, instance.parentReader);
    assertSame(resultImpl.resultClass, instance.resultClass);
    assertSame(resultImpl.elementName, instance.elementName);
    assertSame(resultImpl.handlerList, instance.handlerList);
    assertSame(resultImpl.parentGroup, instance.parentGroup);
  }

  /**
   * Test of choice method, of class ReaderBuilderImpl.
   */
  @Test
  public void testChoice_ReaderOptionsArr()
  {
    System.out.println("choice");
    TestReader tr = TestReader.of(Double.class);
    ReaderBuilderImpl instance = new ReaderBuilderImpl(tr);

    ReaderBuilderImpl result = instance.choice(null);
    assertNotSame(result, instance);
    assertNotNull(result.parentGroup);
    assertSame(result.parentGroup.getClass(), ElementGroupImpl.ChoiceGroup.class);
    assertNotSame(result.parentGroup, instance.parentGroup);
    assertSame(result.parentGroup.getParent(), instance.parentGroup);
    assertNull(result.parentGroup.flags);

    result = instance.choice(Reader.Options.ANY_ALL);
    assertTrue(result.parentGroup.flags.equals(EnumSet.of(Reader.Options.ANY_ALL)));

    result = instance.choice(Reader.Options.ANY_ALL, Reader.Options.ANY_LAX, Reader.Options.ANY_OTHER);
    assertTrue(result.parentGroup.flags.equals(EnumSet.of(Reader.Options.ANY_ALL, Reader.Options.ANY_LAX, Reader.Options.ANY_OTHER)));
  }

  /**
   * Test of choice method, of class ReaderBuilderImpl.
   */
  @Test
  public void testChoice_0args()
  {
    System.out.println("choice");
    TestReader tr = TestReader.of(Double.class);
    ReaderBuilderImpl instance = new ReaderBuilderImpl(tr);

    ReaderBuilderImpl result = instance.choice();
    assertNotSame(result, instance);
    assertNotNull(result.parentGroup);
    assertSame(result.parentGroup.getClass(), ElementGroupImpl.ChoiceGroup.class);
    assertNotSame(result.parentGroup, instance.parentGroup);
    assertSame(result.parentGroup.getParent(), instance.parentGroup);
    assertNull(result.parentGroup.flags);
  }

  /**
   * Test of anyElement method, of class ReaderBuilderImpl.
   */
  @Test
  public void testAnyElement_Class() throws Exception
  {
    System.out.println("anyElement");
    // anyElement(Class cls) called anyElement(AnyFactory any) method 
    // so more extensive test in anyElement(AnyFactory any) method
    TestReader tr = TestReader.of(Double.class);
    ReaderBuilderImpl instance = new ReaderBuilderImpl(tr);

    ReaderBuilderImpl result = instance.anyElement(Integer.class);
    assertSame(result, instance);
  }

  /**
   * Test of anyElement method, of class ReaderBuilderImpl.
   */
  @Test
  public void testAnyElement_Class_ReaderOptionsArr() throws Exception
  {
    System.out.println("anyElement");
    // anyElement(Class cls, Options... options) called anyElement(AnyFactory any)
    // method so more extensive test in anyElement(AnyFactory any) method
    TestReader tr = TestReader.of(Double.class);
    ReaderBuilderImpl instance = new ReaderBuilderImpl(tr);

    ReaderBuilderImpl result = instance.anyElement(Integer.class, Reader.Options.NO_REFERENCE);
    assertSame(result, instance);
  }

  /**
   * Test of anyElement method, of class ReaderBuilderImpl.
   */
  @Test
  public void testAnyElement_ReaderAnyFactory() throws Exception
  {
    System.out.println("anyElement");
    Integer target = Integer.parseInt("1");

    TestReader tr = TestReader.of(Double.class);
    ReaderBuilderImpl instance = new ReaderBuilderImpl(tr);

    // Indirect test anyElement(Class cls)
    AnyContents ac = new AnyContents(target.getClass());
    ReaderBuilderImpl result = instance.anyElement(ac);
    assertSame(result, instance);
    assertSame(result.resultClass, target.getClass());
    // Indirect test producer
    assertNotNull(result.producer);
    assertEquals(result.producer.toString(), "any " + ac.getClass().toString());

    // Indirect test anyElement(Class cls, Options... options)
    instance = new ReaderBuilderImpl(tr);
    AnyContents ac2 = new AnyContents(target.getClass(), Reader.Options.ANY_ALL, Reader.Options.ANY_LAX);
    result = instance.anyElement(ac2);
    assertSame(result, instance);
    assertSame(result.resultClass, target.getClass());
    // Indirect test producer
    assertNotNull(result.producer);
    assertEquals(result.producer.toString(), "any " + ac2.getClass().toString());

    // Test ReaderException
    try
    {
      TestReaderAll tra = new TestReaderAll();
      instance = new ReaderBuilderImpl(tra);
      instance.anyElement(ac);
    }
    catch (ReaderException re)
    {
      assertEquals(re.getMessage(), "any not accepted in all or choice order.");
    }
    try
    {
      TestReaderChoice trc = new TestReaderChoice();
      instance = new ReaderBuilderImpl(trc);
      instance.anyElement(ac);
    }
    catch (ReaderException re)
    {
      assertEquals(re.getMessage(), "any not accepted in all or choice order.");
    }

    // Test producer not null check
    try
    {
      instance = new ReaderBuilderImpl(tr);
      instance.producer = new TestProducer(null);
      instance.anyElement(ac);
    }
    catch (ReaderException re)
    {
      assertEquals(re.getMessage(), "element contents redefined");
    }
  }

  /**
   * Test of reference method, of class ReaderBuilderImpl.
   */
  @Test
  public void testReference() throws Exception
  {
    System.out.println("reference");
    TestReaderMixed trm = TestReaderMixed.of(Double.class);
    ReaderBuilderImpl instance = new ReaderBuilderImpl(trm);
    ReaderBuilderImpl result = instance.reference(Long.class);
    assertSame(result, instance);
    assertSame(result.resultClass, Long.class);
    assertNotNull(result.producer);
    assertEquals(result.producer.toString(), "reference");

    // Test ReaderException
    try
    {
      instance.reference(Long.class);
    }
    catch (ReaderException re)
    {
      assertEquals(re.getMessage(), "element contents redefined");
    }
  }

  /**
   * Test of section method, of class ReaderBuilderImpl.
   */
  @Test
  public void testSection() throws Exception
  {
    System.out.println("section");
    TestSectionImpl section = new TestSectionImpl();
    
    TestReader tr = TestReader.of(Double.class);
    ReaderBuilderImpl instance = new ReaderBuilderImpl(tr);    
    ReaderBuilderImpl result = (ReaderBuilderImpl) instance.section(section);    
    assertSame(result, instance);
    assertNotNull(instance.lastHandler);
    assertTrue(instance.lastHandler.options.equals(EnumSet.of(Reader.Options.NO_REFERENCE)));
    assertEquals(instance.lastHandler.getReader(), section);    
  }

  /**
   * Test of setOptions method, of class ReaderBuilderImpl.
   */
  @Test
  public void testSetOptions()
  {
    System.out.println("setOptions");
    ElementHandlerImpl handler1 = new ElementHandlerImpl("", null, null, null, null);
    ElementHandlerImpl handler2 = new ElementHandlerImpl("", null, null, null, null);
    handler2.setNextHandler(handler1);
    assertNull(handler1.options);
    assertNull(handler2.options);
    EnumSet<Reader.Options> flags = EnumSet.of(Reader.Options.UNBOUNDED, Reader.Options.REQUIRED);
    TestReaderMixed trm = TestReaderMixed.of(Double.class);
    ReaderBuilderImpl instance = new ReaderBuilderImpl(trm);
    instance.lastHandler = handler2;
    instance.setOptions(flags);
    assertTrue(handler1.options.equals(flags));
    assertTrue(handler2.options.equals(flags));

    // Test RuntimeException
    try
    {
      instance.lastHandler = null;
      instance.setOptions(flags);
    }
    catch (RuntimeException re)
    {
      assertEquals(re.getMessage(), "null handler");
    }
  }

  /**
   * Test of getOptions method, of class ReaderBuilderImpl.
   */
  @Test
  public void testGetOptions()
  {
    System.out.println("getOptions");
    ElementHandlerImpl handler1 = new ElementHandlerImpl("", null, null, null, null);
    EnumSet<Reader.Options> flags = EnumSet.of(Reader.Options.UNBOUNDED, Reader.Options.REQUIRED);
    TestReaderMixed trm = TestReaderMixed.of(Double.class);
    ReaderBuilderImpl instance = new ReaderBuilderImpl(trm);
    instance.lastHandler = handler1;
    assertNull(instance.getOptions());
    handler1.options = flags;
    assertTrue(instance.getOptions().equals(flags));
  }

  /**
   * Test of deferrable method, of class ReaderBuilderImpl.
   */
  @Test
  public void testDeferrable()
  {
    System.out.println("deferrable");
    EnumSet<Reader.Options> flags = EnumSet.of(Reader.Options.DEFERRABLE);
    ElementHandlerImpl handler1 = new ElementHandlerImpl(null, null, null, null, null);
    TestReaderMixed trm = TestReaderMixed.of(Double.class);
    ReaderBuilderImpl instance = new ReaderBuilderImpl(trm);
    instance.lastHandler = handler1;
    ReaderBuilderImpl result = instance.deferrable();
    assertSame(result, instance);
    assertTrue(result.getOptions().equals(flags));
  }

  /**
   * Test of optional method, of class ReaderBuilderImpl.
   */
  @Test
  public void testOptional()
  {
    System.out.println("optional");
    EnumSet<Reader.Options> flags = EnumSet.of(Reader.Options.OPTIONAL);
    ElementHandlerImpl handler1 = new ElementHandlerImpl(null, null, null, null, null);
    TestReaderMixed trm = TestReaderMixed.of(Double.class);
    ReaderBuilderImpl instance = new ReaderBuilderImpl(trm);
    instance.lastHandler = handler1;
    ReaderBuilderImpl result = instance.optional();
    assertSame(result, instance);
    assertTrue(result.getOptions().equals(flags));
  }

  /**
   * Test of required method, of class ReaderBuilderImpl.
   */
  @Test
  public void testRequired()
  {
    System.out.println("required");
    EnumSet<Reader.Options> flags = EnumSet.of(Reader.Options.REQUIRED);
    ElementHandlerImpl handler1 = new ElementHandlerImpl(null, null, null, null, null);
    TestReaderMixed trm = TestReaderMixed.of(Double.class);
    ReaderBuilderImpl instance = new ReaderBuilderImpl(trm);
    instance.lastHandler = handler1;
    ReaderBuilderImpl result = instance.required();
    assertSame(result, instance);
    assertTrue(result.getOptions().equals(flags));
  }

  /**
   * Test of unbounded method, of class ReaderBuilderImpl.
   */
  @Test
  public void testUnbounded()
  {
    System.out.println("unbounded");
    EnumSet<Reader.Options> flags = EnumSet.of(Reader.Options.UNBOUNDED);
    ElementHandlerImpl handler1 = new ElementHandlerImpl(null, null, null, null, null);
    TestReaderMixed trm = TestReaderMixed.of(Double.class);
    ReaderBuilderImpl instance = new ReaderBuilderImpl(trm);
    instance.lastHandler = handler1;
    ReaderBuilderImpl result = instance.unbounded();
    assertSame(result, instance);
    assertTrue(result.getOptions().equals(flags));
  }

  /**
   * Test of define method, of class ReaderBuilderImpl.
   */
  @Test
  public void testDefine()
  {
    System.out.println("define");
    EnumSet<Reader.Options> flags = EnumSet.of(Reader.Options.NO_REFERENCE);
    ElementHandlerImpl handler1 = new ElementHandlerImpl(null, null, null, null, null);
    TestReaderMixed trm = TestReaderMixed.of(Double.class);
    ReaderBuilderImpl instance = new ReaderBuilderImpl(trm);
    instance.lastHandler = handler1;
    ReaderBuilderImpl result = instance.define();
    assertSame(result, instance);
    assertTrue(result.getOptions().equals(flags));
  }

  /**
   * Test of noid method, of class ReaderBuilderImpl.
   */
  @Test
  public void testNoid()
  {
    System.out.println("noid");
    EnumSet<Reader.Options> flags = EnumSet.of(Reader.Options.NO_ID);
    ElementHandlerImpl handler1 = new ElementHandlerImpl(null, null, null, null, null);
    TestReaderMixed trm = TestReaderMixed.of(Double.class);
    ReaderBuilderImpl instance = new ReaderBuilderImpl(trm);
    instance.lastHandler = handler1;
    ReaderBuilderImpl result = instance.noid();
    assertSame(result, instance);
    assertTrue(result.getOptions().equals(flags));
  }

  /**
   * Test of executeCall method, of class ReaderBuilderImpl.
   */
  @Test
  public void testExecuteCall() throws Exception
  {
    System.out.println("executeCall");
    BiConsumer<String, String> method = (a, b) -> System.out.print("...testExecuteCall...");

    TestReader tr = TestReader.of(String.class);
    ReaderBuilderImpl instance = (new ReaderBuilderImpl(tr)).reader(ObjectReader.create(Integer.class));
    instance.elementName = "elementName";
    instance.target = Integer.parseInt("1");
    ReaderBuilderImpl result = instance.executeCall(method);

    assertSame(result, instance);
    assertNull(result.producer);
    assertNull(result.elementName);
    assertNull(result.resultClass);
    assertNotNull(result.lastHandler);
    assertNotNull(result.target);
    assertEquals(result.lastHandler.getClass(), ReaderHandler.class);
  }

  /**
   * Test of call method, of class ReaderBuilderImpl.
   */
  @Test
  public void testCall_BiConsumer() throws Exception
  {
    System.out.println("call");
    // mehtod call(BiConsumer<T, T2> method) calls executeCall so going to do 
    // simple testing
    TestReader tr = TestReader.of(String.class);
    ReaderBuilderImpl instance = new ReaderBuilderImpl(tr);
    instance.producer = new TestProducer(null);
    assertNull(instance.lastHandler);
    ReaderBuilderImpl result = instance.call((a, b) -> System.out.print("...testExecuteCall..."));
    assertNotNull(instance.lastHandler);
    assertSame(result, instance);
  }

  /**
   * Test of call method, of class ReaderBuilderImpl.
   */
  @Test
  public void testCall_BiConsumer_Class() throws Exception
  {
    System.out.println("call");
    // mehtod call(BiConsumer<T, T3> method, Class<T3> resultClass)
    // evenutally calls executeCall so going to do simple testing

    BiConsumer<String, String> method = (a, b) -> System.out.print("testCall_BiConsumer_Class");

    // resultClass is null branch
    PrimitiveReaderImpl pri = new PrimitiveReaderImpl(INTEGER_PRIMITIVE);
    ReaderBuilderImpl instance = new ReaderBuilderImpl(pri);
    assertNull(instance.lastHandler);
    ReaderBuilderImpl result = instance.call(method, Integer.class);
    assertNotNull(instance.lastHandler);
    assertSame(result, instance);

    // ResultClass is not null branch, the else branch
    instance = new ReaderBuilderImpl(pri);
    instance.resultClass = Long.class;
    instance.producer = new TestProducer(null);
    assertNull(instance.lastHandler);
    result = instance.call(method, Long.class);
    assertNotNull(instance.lastHandler);
    assertSame(result, instance);

    // Test ReaderException
    instance.resultClass = String.class;
    try
    {
      instance.call(method, Long.class);
    }
    catch (ReaderException re)
    {
      assertEquals(re.getMessage(), "Can't assign " + instance.resultClass + " to " + Long.class + " for method " + method);
    }
  }

  /**
   * Test of call method, of class ReaderBuilderImpl.
   */
  @Test
  public void testCall_Consumer() throws Exception
  {
    System.out.println("call");
    // method call(Consumer<T> method) calls 
    // method  call(BiConsumer<T, T3> method, Class<T3> resultClass), which
    // evenutally calls executeCall so going to do simple testing
    Consumer<Integer> method = x -> System.out.println(x);

    PrimitiveReaderImpl pri = new PrimitiveReaderImpl(INTEGER_PRIMITIVE);
    ReaderBuilderImpl instance = new ReaderBuilderImpl(pri);

    assertNull(instance.lastHandler);
    ReaderBuilderImpl result = instance.call(method);
    assertNotNull(instance.lastHandler);
    assertSame(result, instance);
  }

  /**
   * Test of call method, of class ReaderBuilderImpl.
   */
  @Test
  public void testCall_ReaderAttributesStringConsumer() throws Exception
  {
    System.out.println("call");
    // method call(final Reader.AttributesStringConsumer method) calls 
    // method  call(BiConsumer<T, T3> method, Class<T3> resultClass), which
    // evenutally calls executeCall so going to do simple testing
    Reader.AttributesStringConsumer method = new Reader.AttributesStringConsumer()
    {
      @Override
      public void call(Object t, Attributes attr, String contents) throws ReaderException
      {
        System.out.println(t.getClass());
      }
    };
    AttributeContentsReader acr = new AttributeContentsReader();
    ReaderBuilderImpl instance = new ReaderBuilderImpl(acr);

    assertNull(instance.lastHandler);
    ReaderBuilderImpl result = instance.call(method);
    assertNotNull(instance.lastHandler);
    assertSame(result, instance);
  }

  /**
   * Test of callString method, of class ReaderBuilderImpl.
   */
  @Test
  public void testCallString() throws Exception
  {
    System.out.println("callString");
    BiConsumer<String, String> method = (a, b) -> System.out.print(a + ":" + b);

    AnyReader ar = AnyReader.of(String.class);
    ReaderBuilderImpl instance = new ReaderBuilderImpl(ar);

    assertNull(instance.lastHandler);
    ReaderBuilderImpl result = instance.callString(method);
    assertNotNull(instance.lastHandler);
    assertSame(result, instance);
    assertSame(result.lastHandler.getTargetClass(), String.class);
    assertSame(result.lastHandler.method, method);
  }

  /**
   * Test of callDouble method, of class ReaderBuilderImpl.
   */
  @Test
  public void testCallDouble() throws Exception
  {
    System.out.println("callDouble");
    BiConsumer<String, Double> method = (a, b) -> System.out.print(a + ":" + b);

    AnyReader ar = AnyReader.of(Double.class);
    ReaderBuilderImpl instance = new ReaderBuilderImpl(ar);

    assertNull(instance.lastHandler);
    ReaderBuilderImpl result = instance.callDouble(method);
    assertNotNull(instance.lastHandler);
    assertSame(result, instance);
    assertSame(result.lastHandler.getTargetClass(), Double.class);
    assertSame(result.lastHandler.method, method);
  }

  /**
   * Test of callInteger method, of class ReaderBuilderImpl.
   */
  @Test
  public void testCallInteger() throws Exception
  {
    System.out.println("callInteger");
    BiConsumer<String, Integer> method = (a, b) -> System.out.print(a + ":" + b);

    AnyReader ar = AnyReader.of(Integer.class);
    ReaderBuilderImpl instance = new ReaderBuilderImpl(ar);

    assertNull(instance.lastHandler);
    ReaderBuilderImpl result = instance.callInteger(method);
    assertNotNull(instance.lastHandler);
    assertSame(result, instance);
    assertSame(result.lastHandler.getTargetClass(), Integer.class);
    assertSame(result.lastHandler.method, method);
  }

  /**
   * Test of callLong method, of class ReaderBuilderImpl.
   */
  @Test
  public void testCallLong() throws Exception
  {
    System.out.println("callLong");
    BiConsumer<String, Long> method = (a, b) -> System.out.print(a + ":" + b);

    AnyReader ar = AnyReader.of(Long.class);
    ReaderBuilderImpl instance = new ReaderBuilderImpl(ar);

    assertNull(instance.lastHandler);
    ReaderBuilderImpl result = instance.callLong(method);
    assertNotNull(instance.lastHandler);
    assertSame(result, instance);
    assertSame(result.lastHandler.getTargetClass(), Long.class);
    assertSame(result.lastHandler.method, method);
  }

  /**
   * Test of callBoolean method, of class ReaderBuilderImpl.
   */
  @Test
  public void testCallBoolean() throws Exception
  {
    System.out.println("callBoolean");
    BiConsumer<String, Boolean> method = (a, b) -> System.out.print(a + ":" + b);

    AnyReader ar = AnyReader.of(Boolean.class);
    ReaderBuilderImpl instance = new ReaderBuilderImpl(ar);

    assertNull(instance.lastHandler);
    ReaderBuilderImpl result = instance.callBoolean(method);
    assertNotNull(instance.lastHandler);
    assertSame(result, instance);
    assertSame(result.lastHandler.getTargetClass(), Boolean.class);
    assertSame(result.lastHandler.method, method);
  }

  /**
   * Test of nop method, of class ReaderBuilderImpl.
   */
  @Test
  public void testNop() throws Exception
  {
    System.out.println("nop");
    // mehtod nop calls executeCall so going to do simple testing
    TestReader tr = TestReader.of(String.class);
    ReaderBuilderImpl instance = new ReaderBuilderImpl(tr);
    instance.producer = new TestProducer(null);
    assertNull(instance.lastHandler);
    instance.nop();
    assertNotNull(instance.lastHandler);
  }

  /**
   * Test of getKey method, of class ReaderBuilderImpl.
   */
  @Test
  public void testGetKey()
  {
    System.out.println("getKey");
    TestReader tr = TestReader.of(String.class);
    ReaderBuilderImpl instance = new ReaderBuilderImpl(tr);    
    String value = "value";
    assertEquals(instance.getKey(value), value+ "#" + TestPackage.namespace);
  }

  /**
   * Test of flag method, of class ReaderBuilderImpl.
   */
  @Test
  public void testFlag() throws Exception
  {
    System.out.println("flag");
    AnyReader ar = AnyReader.of(Boolean.class);
    ReaderBuilderImpl instance = new ReaderBuilderImpl(ar);
    ReaderBuilderImpl result = (ReaderBuilderImpl) instance.flag();
    assertSame(result, instance);
    assertSame(instance.resultClass, Boolean.class);
    assertEquals(instance.producer.toString(), "reader for " + Boolean.class);
  }

  /**
   * Test of list method, of class ReaderBuilderImpl.
   */
  @Test
  public void testList() throws Exception
  {
    System.out.println("list");
    TestReaderMixed trm = TestReaderMixed.of(String.class);
    ReaderBuilderImpl instance = new ReaderBuilderImpl(trm);    
    ReaderBuilderImpl result = (ReaderBuilderImpl) instance.list(TestReader.of(String.class));
    assertSame(result, instance);
    assertSame(instance.resultClass, List.class);
    assertEquals(result.producer.toString(), "reader for " + List.class);    
  }

  /**
   * Test of add method, of class ReaderBuilderImpl.
   */
  @Test
  public void testAdd()
  {
    System.out.println("add");
    TestReaderMixed trm = TestReaderMixed.of(String.class);
    ReaderBuilderImpl instance = new ReaderBuilderImpl(trm);
    
    assertNotNull(instance.handlerList);
    assertNull(instance.handlerList.firstHandler);
    assertNull(instance.handlerList.lastHandler);
    assertNull(instance.handlerList.trace);
    
    ElementHandlerImpl first = new ElementHandlerImpl(null,null,null,null,null);    
    instance.add(first);    
    assertEquals(instance.handlerList.firstHandler, first);
    assertEquals(instance.handlerList.lastHandler, first);
    
    ElementHandlerImpl second = new ElementHandlerImpl(null,null,null,null,null);    
    instance.add(second);    
    assertEquals(instance.handlerList.firstHandler, first);
    assertEquals(instance.handlerList.lastHandler, second);
    
    ElementHandlerImpl third = new ElementHandlerImpl(null,null,null,null,null);    
    instance.add(third);    
    assertEquals(instance.handlerList.firstHandler, first);
    assertEquals(instance.handlerList.lastHandler, third);
    
    ElementHandlerImpl iter = (ElementHandlerImpl) instance.handlerList.firstHandler;
    assertEquals(iter, first);
    
    iter = (ElementHandlerImpl) iter.getNextHandler();
    assertEquals(iter, second);
    
    iter = (ElementHandlerImpl) iter.getNextHandler();
    assertEquals(iter, third);
    
    iter = (ElementHandlerImpl) iter.getNextHandler();
    assertNull(iter);
  }

  class TestProducer<T, T2> implements Producer<T, T2>
  {
    public Reader<T2> reader;

    TestProducer(Reader<T2> reader)
    {
      this.reader = reader;
    }

    @Override
    public ElementHandlerImpl newInstance(ReaderBuilderImpl builder, String elementName, EnumSet<Reader.Options> flags, T target, Class<T2> resultClass, BiConsumer<T, T2> method) throws ReaderException
    {
      return new ElementHandlerImpl<>(elementName, flags, target, resultClass, method);
    }
  }

  @Reader.Declaration(pkg = TestPackage.class,
          name = "#TestReaderAll", order = Reader.Order.ALL)
  static public class TestReaderAll<T> extends ObjectReader<T>
  {
    Class<T> cls;
    public T obj;

    public TestReaderAll()
    {
      this.cls = null;
    }

    public TestReaderAll(Class<T> cls)
    {
      this.cls = cls;
    }

    @Override
    public Class getObjectClass()
    {
      return cls;
    }
  }

  @Reader.Declaration(pkg = TestPackage.class,
          name = "#TestReaderAll", order = Reader.Order.CHOICE)
  static public class TestReaderChoice<T> extends ObjectReader<T>
  {
    Class<T> cls;
    public T obj;

    public TestReaderChoice()
    {
      this.cls = null;
    }

    public TestReaderChoice(Class<T> cls)
    {
      this.cls = cls;
    }

    @Override
    public Class getObjectClass()
    {
      return cls;
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