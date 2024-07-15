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
import gov.llnl.math.euclidean.Versor;
import gov.llnl.utility.io.WriterException;
import gov.llnl.utility.xml.bind.ObjectWriter;

/**
 *
 * @author nelson85
 */
public class VersorWriter extends ObjectWriter<Versor>
{
  public VersorWriter()
  {
    super(Options.REFERENCEABLE, "versor", MathPackage.getInstance());
  }

  @Override
  public void attributes(WriterAttributes attributes, Versor object) throws WriterException
  {
    double i = object.getI();
    double j = object.getJ();
    double k = object.getK();
    double r = Math.sqrt(i * i + j * j + k * k);
    double angle = 2 * Math.atan2(r, object.getU());

    attributes.add("x", i / r);
    attributes.add("y", j / r);
    attributes.add("z", k / r);
    attributes.add("angle", angle);
  }

  @Override
  public void contents(Versor object) throws WriterException
  {
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