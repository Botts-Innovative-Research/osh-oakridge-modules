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
@Reader.Declaration(pkg = ErniePackage.class, name = "rectangularCollimatedFace", referenceable = true)
@Reader.Attribute(name = "width", type = double.class, required = true)
@Reader.Attribute(name = "height", type = double.class, required = true)
@Reader.Attribute(name = "size", type = double.class, required = true)
@Reader.Attribute(name = "top", type = double.class, required = true)
public class SensorFaceRectangularCollimatedReader extends ObjectReader<SensorFaceRectangularCollimated>
{

  @Override
  public SensorFaceRectangularCollimated start(Attributes attributes) throws ReaderException
  {
    SensorFaceRectangularCollimated obj = new SensorFaceRectangularCollimated();
    obj.height = Double.parseDouble(attributes.getValue("height"));
    obj.width = Double.parseDouble(attributes.getValue("width"));
    obj.extentSide = Double.parseDouble(attributes.getValue("side"));
    obj.extentTop = Double.parseDouble(attributes.getValue("top"));
    return obj;
  }

  @Override
  public ElementHandlerMap getHandlers() throws ReaderException
  {
    ReaderBuilder<SensorFaceRectangularCollimated> builder = this.newBuilder();
    builder.element("origin").call((p, o) -> p.origin = o, Vector3.class);
    builder.element("orientation").call((p, o) -> p.orientation = o, Versor.class);
    return builder.getHandlers();
  }

  @Override
  public Class<? extends SensorFaceRectangularCollimated> getObjectClass()
  {
    return SensorFaceRectangularCollimated.class;
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