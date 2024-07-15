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
package gov.llnl.ernie.api;

import java.io.File;
import java.nio.file.Files;


/**
 *
 * @author cmattoon
 */
public class Example
{
  public static void main(String[] args) throws Exception
  {    
    ERNIE_lane lane1 = new ERNIE_lane("SamplePort", 1, true, 4.82, 5, 10); // collimated, 4.82 m wide
    ERNIE_lane lane2 = new ERNIE_lane("SamplePort", 2, false, 5.02, 5, 10); // uncollimated, 5.02 m

    // process lane 1 scans:
    File lane1Dir = new File("lane1Files");
    File[] dailyFiles = lane1Dir.listFiles();

    for (File dailyFile : dailyFiles) {
        if (!dailyFile.getName().contains("daily") || dailyFile.getName().endsWith(".xml")) {
            continue;
        }
        Results res = lane1.process(Files.lines(dailyFile.toPath()));
        res.saveToXML(new File(dailyFile.getAbsolutePath().replace(".txt", ".xml")));
    }

    // lane 2:
    File lane2Dir = new File("lane2Files");
    dailyFiles = lane2Dir.listFiles();

    for (File dailyFile : dailyFiles) {
        if (!dailyFile.getName().contains("daily") || dailyFile.getName().endsWith(".xml")) {
            continue;
        }
        Results res = lane2.process(Files.lines(dailyFile.toPath()));
        res.saveToXML(new File(dailyFile.getAbsolutePath().replace(".txt", ".xml")));
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