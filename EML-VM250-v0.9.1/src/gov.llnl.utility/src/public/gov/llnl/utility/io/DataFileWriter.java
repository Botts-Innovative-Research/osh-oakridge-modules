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

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;

/**
 *
 * @author nelson85
 * @param <T>
 */
public interface DataFileWriter<T>
{
  /**
   * Save an object to a file.
   *
   * @param path
   * @param object
   * @throws IOException
   * @throws WriterException
   */
  void saveFile(Path path, T object) throws IOException, WriterException;

  /**
   * Write an object to a stream. The stream will be closed on completion.
   *
   * @param stream
   * @param object
   * @throws IOException
   * @throws WriterException
   */
  void saveStream(OutputStream stream, T object) throws IOException, WriterException;
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