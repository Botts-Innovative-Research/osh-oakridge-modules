/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.utility.options;

import java.util.List;

/**
 *
 * @author nelson85
 */
public interface OptionSet
{
  /**
   * Check to see if an option was specified in the arguments.
   *
   * If no dashes are specified in the option key, it assumes whether it is a
   * long or short option based on the length. Otherwise, it uses the dashes
   * directly to match the option.
   *
   * @param option is the key for the option.
   * @return true if the option is specified.
   */
  boolean has(String option);

  /**
   * Get the argument value supplied by the user.
   *
   * If no dashes are specified in the option key, it assumes whether it is a
   * long or short option based on the length. Otherwise, it uses the dashes
   * directly to match the option.
   *
   * If the user did not supply an argument, the default values will be
   * supplied.
   *
   * @param option is the option key.
   * @return the value or null.
   */
  Object valueOf(String option);

  /**
   * Check to see if an option was specified in the arguments.
   *
   * @param option is the OptionSpec defined by the parser.
   * @return true if the option is specified.
   */
  boolean has(OptionSpec option);

  /**
   * Get the argument value supplied by the user.
   *
   * If the user did not supply an argument, the default values will be
   * supplied.
   *
   * @param <T> is the type of the argument.
   * @param option is the option key.
   * @return the value or null.
   */
  <T> T valueOf(OptionSpec<T> option);

  /**
   * Get the arguments not given as options.
   *
   * @return a list of arguments.
   */
  List<String> nonOptionArguments();
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