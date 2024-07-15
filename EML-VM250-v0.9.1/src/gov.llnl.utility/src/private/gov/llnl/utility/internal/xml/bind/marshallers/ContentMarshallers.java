/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.utility.internal.xml.bind.marshallers;

import gov.llnl.utility.annotation.Internal;
import gov.llnl.utility.xml.bind.WriterContext.Marshaller;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author nelson85
 */
@Internal
public class ContentMarshallers
{
  public static List<Marshaller> getDefault()
  {
    Marshaller[] marshallers = new Marshaller[]
    {
      new StringMarshaller(),
      new BooleanMarshaller(),
      new IntegerMarshaller(),
      new IntegerArrayMarshaller(),
      new LongMarshaller(),
      new LongArrayMarshaller(),
      new FloatMarshaller(),
      new FloatArrayMarshaller(),
      new FloatsArrayMarshaller(),
      new DoubleMarshaller(),
      new DoubleArrayMarshaller(),
      new DoublesArrayMarshaller(),
      new UUIDMarshaller(),
      new InstantMarshaller()
    };
    return Arrays.asList(marshallers);
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