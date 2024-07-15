/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.ernie;

import gov.llnl.utility.xml.bind.DocumentReader;
import java.util.Map;

/**
 *
 * @author pham21
 */
class AnalysisHooks implements DocumentReader.Hook
{
  @Override
  public void startDocument(DocumentReader dr)
  {
    dr.setProperty(DocumentReader.COMPUTE_MD5SUM, true);
  }

  @Override
  public void endDocument(DocumentReader dr)
  {
    Map<String, String> checksums = (Map<String, String>) dr.getProperty(DocumentReader.RESULT_MD5SUM);
    Analysis analysis = (Analysis) dr.getContext().getLastObject();
    for (Map.Entry<String, String> entry : checksums.entrySet())
    {
      analysis.addSetting(entry.getKey(), entry.getValue());
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