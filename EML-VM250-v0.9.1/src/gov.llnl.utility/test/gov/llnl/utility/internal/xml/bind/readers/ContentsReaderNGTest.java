/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.utility.internal.xml.bind.readers;

import gov.llnl.utility.UtilityPackage;
import gov.llnl.utility.TestSupport.TestElement;
import gov.llnl.utility.io.ReaderException;
import gov.llnl.utility.xml.DomBuilder;
import gov.llnl.utility.xml.bind.Reader;
import java.lang.annotation.Annotation;
import static org.testng.Assert.*;
import org.testng.annotations.Test;

/**
 *
 * @author pham21
 */
public class ContentsReaderNGTest
{

  /**
   * Test of start method, of class ContentsReader.
   */
  @Test
  public void testStart() throws Exception
  {
    System.out.println("start");
    assertNull(new TestContentsReaderImpl().start(null));
  }

  /**
   * Test of contents method, of class ContentsReader.
   */
  @Test
  public void testContents() throws ReaderException
  {
    System.out.println("contents");
    assertNull(new TestContentsReaderImpl().contents(""));
  }

  /**
   * Test of createSchemaType method, of class ContentsReader.
   */
  @Test
  public void testCreateSchemaType() throws Exception
  {
    System.out.println("createSchemaType");
    new TestContentsReaderImpl().createSchemaType(null);
  }

  /**
   * Test of createSchemaElement method, of class ContentsReader.
   */
  @Test
  public void testCreateSchemaElement()
  {
    System.out.println("createSchemaElement");

    TestElement first = new TestElement("First");
    DomBuilder domBuilder = new DomBuilder(first);

    ContentsReader instance = new TestContentsReaderImpl();
    DomBuilder tooDomTooBuildrious = instance.createSchemaElement(
            null, "Toretto&O'Conner", domBuilder, false);

    TestElement second = (TestElement) tooDomTooBuildrious.toElement();

    assertEquals(tooDomTooBuildrious.toElement(), domBuilder.toElement().getFirstChild());

    assertEquals(second.getTagName(), "xs:element");
    assertEquals(second.attrMap.get("name"), "Toretto&O'Conner");
    assertEquals(second.attrMap.get("type"), "xs:testcontentsreader");
  }

  @Reader.Declaration(
          pkg = UtilityPackage.class,
          name = "TestContentsReader",
          referenceable = true,
          contents = Reader.Contents.TEXT)
  public class TestContentsReaderImpl extends ContentsReader
  {
    @Override
    public Object contents(String textContents) throws ReaderException
    {
      return null;
    }

    public Reader.TextContents getTextContents()
    {
      Reader.TextContents tc
              = this.getClass().getDeclaredAnnotation(Reader.TextContents.class);

      if (tc != null)
        return tc;

      return new Reader.TextContents()
      {
        @Override
        public String base()
        {
          return "xs:testcontentsreader";
        }

        @Override
        public Class<? extends Annotation> annotationType()
        {
          return Reader.TextContents.class;
        }
      };
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