/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.ernie.io;

import gov.llnl.ernie.data.Record;
import gov.llnl.ernie.manipulator.Manipulation;
import java.io.File;
import java.util.List;

/**
 *
 * @author mattoon1
 */
public interface RecordWriter
{
  /**
   * Record will be written out to disk.
   *
   * @param record record to write
   * @param outputfile file where results will be written (inside outputDir)
   * @param manipulations list of manipulations applied to record
   * @throws RecordWriterException if an error occurred during writing.
   */
  void writeRecord(Record record, File outputfile, List<Manipulation> manipulations) throws RecordWriterException;
  
  /**
   * Set the directory where results will be saved
   * @param dir 
   */
  void setOutputDir(String dir);
  
  /**
   * Get directory where results will be saved
   * @return String
   */
  String getOutputDir();
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