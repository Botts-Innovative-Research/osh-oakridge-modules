/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.utility.io;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

/**
 *
 * @author nelson85
 * @param <T>
 */
public interface DataFileReader<T>
{
  /**
   * Load a file into a class.
   *
   * @param path is the file to be read
   * @return the object that was read
   * @throws FileNotFoundException if the file cannot be found
   * @throws IOException if any io errors occur while reading
   * @throws ReaderException if the file format is incorrect
   */
  T loadFile(Path path) throws FileNotFoundException, IOException, ReaderException;

  /**
   * Load a stream into a class. Must close the stream when complete.
   *
   * @param stream is the file to be read
   * @return the object that was read
   * @throws FileNotFoundException if the file cannot be found
   * @throws IOException if any io errors occur while reading
   * @throws ReaderException if the file format is incorrect
   */
  T loadStream(InputStream stream) throws IOException, ReaderException;

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