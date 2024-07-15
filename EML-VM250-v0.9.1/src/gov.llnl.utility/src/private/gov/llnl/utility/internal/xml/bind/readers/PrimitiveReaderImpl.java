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

import gov.llnl.utility.ClassUtilities;
import gov.llnl.utility.UtilityPackage;
import gov.llnl.utility.io.ReaderException;
import gov.llnl.utility.xml.bind.Reader;
import java.lang.annotation.Annotation;

/**
 *
 * @author nelson85
 * @param <Obj>
 */
@Reader.Declaration(pkg = UtilityPackage.class, name = Reader.Declaration.NULL,
        contents = Reader.Contents.TEXT)
public class PrimitiveReaderImpl<Obj> extends ContentsReader<Obj>
{
  ClassUtilities.Primitive primitive;

  public PrimitiveReaderImpl(ClassUtilities.Primitive prim)
  {
    this.primitive = prim;
  }

  @Override
  @SuppressWarnings("unchecked")
  public Obj contents(String textContents) throws ReaderException
  {
    try
    {
      return (Obj) primitive.valueOf(textContents);
    }
    catch (NumberFormatException ex)
    {
      throw new ReaderException(ex);
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public Class<Obj> getObjectClass()
  {
    return primitive.getBoxedType();
  }
  
  public Reader.TextContents getTextContents()
  {
    Reader.TextContents tc = 
               this.getClass().getDeclaredAnnotation(Reader.TextContents.class);

    if (tc!=null)
      return tc;
    
    return new Reader.TextContents()
    {
      @Override
      public String base()
      {
        return "xs:"+ primitive.getPrimitiveType().getName();
      }

      @Override
      public Class<? extends Annotation> annotationType()
      {
         return Reader.TextContents.class;
      }
    };
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