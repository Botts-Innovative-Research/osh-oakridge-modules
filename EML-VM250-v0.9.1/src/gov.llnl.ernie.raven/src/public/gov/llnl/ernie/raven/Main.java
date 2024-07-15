/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gov.llnl.ernie.raven;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author nelson85
 */
public class Main
{
  static Logger LOGGER = Logger.getLogger(Main.class.getName());
  static Control control = null;
  static Log log = null;

  public static void main(String[] args) throws IOException
  {
    // No arguments currently
    System.out.println("Welcome.");

    // Launch the logger worker
    log = new Log("tcp://*");
    log.start();
    int logPort = log.getPortNum();
    
    // Launch the control worker
    control = new Control("tcp://localhost");
    control.setLogPort(logPort);
    control.start();
    int controlPort = control.getPortNum();
    
    // Lane workers get launched once we get a message on the control channel
    // Response back on std
    System.out.println("EML-VM250 Raven version 4.0.0");
    System.out.println("control-port: " + controlPort);
    System.out.println("log-port: " + logPort);
    System.out.println();

    // Create a copy of the port info on disk
    try ( BufferedWriter bw = Files.newBufferedWriter(Paths.get("ernie-raven.txt")))
    {
      bw.append("EML-VM250 Raven version 4.0.0");
      bw.newLine();
      bw.append("control-port: " + controlPort);
      bw.newLine();
      bw.append("log-port: " + logPort);
      bw.newLine();
      bw.flush();
    }

    // Wait for command worker to die
    try
    {
      // wait for the control thread to die before exiting
      control.thread.join();
    }
    catch (InterruptedException ex)
    {
      Main.LOGGER.log(Level.SEVERE, "Exception in Main", ex);
    }
    
    System.out.println("Thank you, Good-bye");
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