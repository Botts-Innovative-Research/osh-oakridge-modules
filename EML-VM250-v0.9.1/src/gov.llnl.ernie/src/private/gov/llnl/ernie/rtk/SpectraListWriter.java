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
import gov.llnl.utility.io.WriterException;
import gov.llnl.utility.xml.bind.ObjectWriter;
import java.util.TreeMap;

/**
 *
 * @author nelson85
 * @param <T>
 */
public class SpectraListWriter<T extends SpectraList<? extends Spectrum>>
        extends ObjectWriter<T>
{
  TreeMap<Integer, EnergyScale> binMap = new TreeMap<>();

  public SpectraListWriter()
  {
    super(Options.NONE, "spectraList", ErniePackage.getInstance());
  }

  @Override
  public void attributes(WriterAttributes attributes, T object) throws WriterException
  {
  }

  @Override
  public void contents(T object) throws WriterException
  {
    WriterBuilder wb = newBuilder();
    WriteObject wos = wb.element("spectrum").writer(new SpectrumWriter());
    for (Spectrum spectrum : object)
    {
      wos.put(spectrum);
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