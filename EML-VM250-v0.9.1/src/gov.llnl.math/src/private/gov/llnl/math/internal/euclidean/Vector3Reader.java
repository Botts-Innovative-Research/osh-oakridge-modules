/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.math.internal.euclidean;

import gov.llnl.math.MathPackage;
import gov.llnl.math.euclidean.Vector3;
import gov.llnl.utility.io.ReaderException;
import gov.llnl.utility.xml.bind.ObjectReader;
import gov.llnl.utility.xml.bind.Reader;
import org.xml.sax.Attributes;

/**
 *
 * @author nelson85
 */
@Reader.Declaration(pkg = MathPackage.class, name = "vector3",
        cls=Vector3.class, referenceable = true,
        contents = Reader.Contents.NONE)
@Reader.Attribute(name = "x", type = double.class)
@Reader.Attribute(name = "y", type = double.class)
@Reader.Attribute(name = "z", type = double.class)
public class Vector3Reader extends ObjectReader<Vector3>
{
  @Override
  public Vector3 start(Attributes attributes) throws ReaderException
  {
    double x = Double.parseDouble(attributes.getValue("x"));
    double y = Double.parseDouble(attributes.getValue("y"));
    double z = Double.parseDouble(attributes.getValue("z"));
    return Vector3.of(x, y, z);
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