/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.utility;

import static org.testng.Assert.*;
import org.testng.annotations.Test;
import java.util.logging.Logger;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;

/**
 *
 * @author pham21
 */
public class LogFormatterNGTest
{
  
  public LogFormatterNGTest()
  {
  }

  /**
   * Test of format method, of class LogFormatter.
   */
  @Test
  public void testFormat()
  {
    System.out.println("format");
    
    Logger logger = Logger.getLogger("TestLogFormatter");
    logger.setUseParentHandlers(false); 
    LogFormatter lf = new LogFormatter();
    ConsoleHandler ch = new ConsoleHandler();    
    ch.setFormatter(lf);
    ch.setLevel(Level.ALL);
    logger.addHandler(ch);
    
    
    logger.log(Level.SEVERE, "BottleCaps!", new Exception("FALLOUT!"));
    logger.log(Level.WARNING, "Warning");
    logger.log(Level.CONFIG, "CONFIG");
    logger.log(Level.INFO, "INFO");
    logger.log(Level.FINE, "FINE");
    logger.log(Level.FINER, "FINER");
    logger.log(Level.FINEST, "FINEST");
    
    ch.close();
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