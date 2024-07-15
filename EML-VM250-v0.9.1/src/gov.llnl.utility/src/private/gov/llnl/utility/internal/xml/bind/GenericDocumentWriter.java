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

import gov.llnl.utility.annotation.Internal;
import gov.llnl.utility.io.WriterException;
import gov.llnl.utility.xml.DomUtilities;
import gov.llnl.utility.xml.bind.DocumentWriter;
import gov.llnl.utility.xml.bind.ObjectWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 *
 * @author nelson85
 */
@Internal
public class GenericDocumentWriter
{

  @SuppressWarnings("unchecked")
  public static <T> String dumpXML(ObjectWriter<T> objectWriter, T object) throws WriterException
  {
    ObjectWriter<Generic> genericWriter = new GenericWriter(objectWriter);
    DocumentWriter<Generic> genericDocumentWriter = DocumentWriter.create(genericWriter);
    Document document = genericDocumentWriter.toDocument(new Generic(object));
    Element element = (Element) document.getDocumentElement().getFirstChild();
    try (ByteArrayOutputStream out = new ByteArrayOutputStream())
    {
      DomUtilities.printXml(out, element);
      out.flush();
      return new String(out.toByteArray());
    }
    catch (IOException ex)
    {
      throw new WriterException(ex);
    }
  }

  @Internal
  static public class Generic<T>
  {
    T object;

    private Generic(T object)
    {
      this.object = object;
    }
  }

  @Internal
  static public class GenericWriter<T> extends ObjectWriter<Generic<T>>
  {
    private final ObjectWriter<T> writer;

    public GenericWriter(ObjectWriter<T> writer)
    {
      super(ObjectWriter.Options.NONE, "generic", writer.getPackage());
      this.writer = writer;
    }

    @Override
    public void attributes(ObjectWriter.WriterAttributes attributes, Generic<T> object) throws WriterException
    {
    }

    @Override
    public void contents(Generic<T> object) throws WriterException
    {
      WriterBuilder wb = newBuilder();
      wb.writer(writer).put(object.object);
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