/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.ernie.rtk;

import gov.llnl.ernie.ErniePackage;
import gov.llnl.ernie.data.EnergyScale;
import gov.llnl.utility.annotation.Internal;
import gov.llnl.utility.io.WriterException;
import gov.llnl.utility.xml.bind.ObjectWriter;

/**
 *
 * @author nelson85
 */
@Internal
public class EnergyScaleWriter extends ObjectWriter<EnergyScale>
{
  public EnergyScaleWriter()
  {
    super(Options.REFERENCEABLE, "energyScale", ErniePackage.getInstance());
  }

  @Override
  public void attributes(WriterAttributes attributes, EnergyScale object) throws WriterException
  {
  }

  @Override
  public void contents(EnergyScale object) throws WriterException
  {
    WriterBuilder wb = newBuilder();
//    if (object instanceof EnergyPairsScale)
//    {
//      wb.element("pairs").writer(new EnergyPairsScaleWriter()).put((EnergyPairsScale) object);
//    }
//    else
//    {
      wb.element("values").putContents(object.getEdges());
//    }

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