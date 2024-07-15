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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author nelson85
 */
public class ApplicationProperties
{
  public static final String PROPERTY_FILE = "ernie.properties";
  public static final String MACHINE_LEARNING_HOME = "machine_learning.home";
  public static final String MACHINE_LEARNING_MODEL = "machine_learning.model";
  public static final String MACHINE_LEARNING_METRICS = "machine_learning.metrics";
  public static final String MACHINE_LEARNING_FALLBACK_MODEL = "machine_learning.fallbackmodel";
  public static final String MACHINE_LEARNING_FALLBACK_METRICS = "machine_learning.fallbackmetrics";
  public static final String MACHINE_LEARNING_SOURCE2_MODEL = "machine_learning.source2model";
  public static final String MACHINE_LEARNING_SOURCE2_METRICS = "machine_learning.source2metrics";

  public static final String DATABASE_QUERIES = "database.queries";
  public static final String VEHICLE_DATABASE = "vehicle.database";

  // Properties for teleforensics center
  public static final String TELEFORENSICS_MODE = "teleforensics.mode";

  static private Properties properties = null;
  public static final String RPM_TABLE_NAME = "rpm.tablename";

  // Used to instantiate the correct RecordFactory.
  public static final String RECORD_FACTORY_NAME = "factory.name";

  static public Properties getProperties()
  {
    if (properties == null)
    {
      try
      {
        properties = new Properties();
        properties.load(new FileInputStream(PROPERTY_FILE));
      }
      catch (IOException ex)
      {
        throw new RuntimeException(ex);
      }
    }
    return properties;
  }

  static public String getProperty(String key)
  {
    return getProperties().getProperty(key);
  }

  static void storeProperties()
  {
    FileOutputStream out = null;
    try
    {
      out = new FileOutputStream(PROPERTY_FILE);
      properties.store(out, "");
      out.close();
    }
    catch (FileNotFoundException ex)
    {
      throw new RuntimeException(ex);
    }
    catch (IOException ex)
    {
      throw new RuntimeException(ex);
    }
    finally
    {
      try
      {
        if (out != null)
        {
          out.close();
        }
      }
      catch (IOException ex)
      {
        throw new RuntimeException(ex);
      }
    }
  }

  public static void main(String[] args)
  {
    if (false)
    {
      try
      {
        Properties prop = new Properties();
        prop.setProperty(MACHINE_LEARNING_HOME, "cmu2");
        prop.setProperty(MACHINE_LEARNING_MODEL, "allmodel.txt");
        prop.setProperty(MACHINE_LEARNING_METRICS, "metric.fds");
        prop.setProperty(DATABASE_QUERIES, "queries.txt");
        prop.store(new FileOutputStream(PROPERTY_FILE), "");
      }
      catch (IOException ex)
      {
        Logger.getLogger(ApplicationProperties.class.getName()).log(Level.SEVERE, null, ex);
      }
    }

    Properties prop = ApplicationProperties.getProperties();
    System.out.println(prop.getProperty(DATABASE_QUERIES));

  }

  public static boolean hasProperty(String key)
  {
    Properties prop = ApplicationProperties.getProperties();
    return (prop.getProperty(key) != null);
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