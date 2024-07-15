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

import gov.llnl.utility.io.WriterException;
import gov.llnl.utility.xml.DomUtilities;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

// TODO this code needs some considerations in terms of changed reader
// capabilities.  We should at least try to apply the schema when producing 
// results.
/**
 * WriterBaseTyped is a base class for writer that converts an object into a
 * DOM..
 *
 * @param <T> is the object type that is handled by this writer
 */
public abstract class WriterTyped<T>
{
  /**
   * Save the object in a file. Convenience method.
   *
   * @param file
   * @param object
   * @throws FileNotFoundException
   * @throws WriterException
   */
  public void save(Path file, T object) throws FileNotFoundException, WriterException, IOException
  {
    try (OutputStream os = Files.newOutputStream(file))
    {
      DomUtilities.printXml(os, convertToDocument(object));
    }
  }

  /**
   * Convert the object into a document.
   *
   * @param object
   * @return the document hold the serialized object.
   * @throws WriterException
   */
  public Document convertToDocument(T object) throws WriterException
  {
    DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
    DocumentBuilder docBuilder;
    try
    {
      docBuilder = docFactory.newDocumentBuilder();
    }
    catch (ParserConfigurationException ex)
    {
      throw new RuntimeException(ex);
    }

    Document doc = docBuilder.newDocument();
    Element root = convertToElement(doc, object);
    doc.appendChild(root);
    return doc;
  }

  /**
   * Convert an object to an XML element.
   *
   * @param doc is the document to that will hold the element.
   * @param obj is the object to store in the element.
   * @return the resulting element
   * @throws WriterException if the conversion fails
   */
  abstract public Element convertToElement(Document doc, T obj) throws WriterException;
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