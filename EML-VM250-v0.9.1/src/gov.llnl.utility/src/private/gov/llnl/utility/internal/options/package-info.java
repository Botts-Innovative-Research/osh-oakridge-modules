/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.utility.internal.options;

// We have most of the option framework here, but some things 
// were simplified from joptsimple. Most of the stuff we left out
// was badly thought out in that package.
//
// FIXME the method names in joptsimple are horrible.  
// refactor to better match naming conventions.  The overall structure
// was fine.
//
// FIXME support specifying an argument more that once.
// Need to hold a list of objects.
//
// FIXME support for getting the default for an unspecified option.
//
// FIXME should you be able to tell the difference between a default value
// and a user supplied one.
//
// FIXME some documentation would be nice.  Although we mostly
// followed joptsimple, there are some differences worth documenting.
//
// FIXME the help formatter using raw strings is a poor implementation.
// as it encourages merging large strings together.  Better to use a 
// Writter.  


/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */