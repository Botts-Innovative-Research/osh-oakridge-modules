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

// PENDING Initialize should likely be called automatically by object reader on
// the completion of the document.  But how do we make sure things happen in 
// the correct order.   Consider the implications of this.
// PENDING consider truncating the stack trace like ReaderException
/**
 * Interface for objects that need to be initialized after loading.
 * Initialization should occur after the object is loaded and prior to being
 * used.
 */
public interface InitializeInterface
{
  default void assertInitialized(boolean isInitialized) throws IllegalStateException
  {
    if (!isInitialized)
      throw new IllegalStateException(this.getClass().getName() + " has not been initilized!");
  }

  /**
   * Called after object has been loaded. Initializes the memory and verifies
   * that all prerequisites are met.
   *
   * @throws InitializeException if a prerequisite has not been met.
   */
  public void initialize() throws InitializeException;
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