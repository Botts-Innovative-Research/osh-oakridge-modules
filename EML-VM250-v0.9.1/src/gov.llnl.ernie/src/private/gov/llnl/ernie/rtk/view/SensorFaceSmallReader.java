/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.ernie.rtk.view;

import gov.llnl.ernie.ErniePackage;
import gov.llnl.math.euclidean.Vector3;
import gov.llnl.math.euclidean.Versor;
import gov.llnl.utility.io.ReaderException;
import gov.llnl.utility.xml.bind.ObjectReader;
import gov.llnl.utility.xml.bind.Reader;
import org.xml.sax.Attributes;

/**
 *
 * @author nelson85
 */
@Reader.Declaration(pkg = ErniePackage.class, name = "smallFace", referenceable = true)
@Reader.Attribute(name = "width", type = double.class, required = true)
@Reader.Attribute(name = "area", type = double.class, required = true)
public class SensorFaceSmallReader extends ObjectReader<SensorFaceSmall>
{
  @Override
  public SensorFaceSmall start(Attributes attributes) throws ReaderException
  {
    SensorFaceSmall obj = new SensorFaceSmall();
    obj.area = Double.parseDouble(attributes.getValue("area"));
    return obj;
  }

  @Override
  public Reader.ElementHandlerMap getHandlers() throws ReaderException
  {
    Reader.ReaderBuilder<SensorFaceSmall> builder = this.newBuilder();
    builder.element("origin").call((p, o) -> p.origin = o, Vector3.class);
    builder.element("orientation").call((p, o) -> p.orientation = o, Versor.class);
    return builder.getHandlers();
  }

  @Override
  public Class<? extends SensorFaceSmall> getObjectClass()
  {
    return SensorFaceSmall.class;
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