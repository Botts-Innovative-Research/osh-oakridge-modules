/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.ernie.classifier;

import gov.llnl.ernie.ApplicationProperties;
import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 *
 * @author nelson85
 */
public class ClassifierFactory
{
  static public Classifier newClassifierFromProperties(
          String modelKey,
          String metricsKey,
          boolean verbose
  ) throws FileNotFoundException
  {
    Properties prop = ApplicationProperties.getProperties();
    Path machine_learning_home = Paths.get(prop.getProperty(ApplicationProperties.MACHINE_LEARNING_HOME));
    if (!Files.exists(machine_learning_home))
    {
      throw new FileNotFoundException("Unable to load classifier " + machine_learning_home.toAbsolutePath());
    }

    Path machine_learning_model = machine_learning_home.resolve(prop.getProperty(modelKey));
    String machine_learning_metrics = prop.getProperty(metricsKey, "");
    return new Classifier(machine_learning_model.toAbsolutePath().toString(), machine_learning_metrics, verbose);
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