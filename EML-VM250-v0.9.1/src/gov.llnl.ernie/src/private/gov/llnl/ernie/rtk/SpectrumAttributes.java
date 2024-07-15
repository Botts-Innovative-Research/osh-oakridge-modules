/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.ernie.rtk;

import gov.llnl.ernie.ErniePackage;
import gov.llnl.utility.xml.bind.Reader;
import java.time.Instant;

/**
 * This is a holder class that will define all of the attributes supported by
 * the different file types.
 *
 * @author nelson85
 */
public class SpectrumAttributes
{
  public static final String URN = ErniePackage.getInstance().getNamespaceURI();
  // Common

  @Reader.ElementDeclaration(pkg = ErniePackage.class, name = "title", type = String.class)
  public static final String TITLE = URN + "#title"; // String

  @Reader.ElementDeclaration(pkg = ErniePackage.class, name = "timestamp", type = Instant.class)
  public static final String TIMESTAMP = URN + "#timestamp";  // Instant

  @Reader.ElementDeclaration(pkg = ErniePackage.class, name = "neutrons", type = double.class)
  public static final String NEUTRONS = URN + "#neutrons"; // Double

  @Reader.ElementDeclaration(pkg = ErniePackage.class, name = "energyPolynomial", type = double[].class)
  public static final String ENERGY_POLYNOMIAL = URN + "#energyPolynomial"; // EnergyPolynomial

  @Reader.ElementDeclaration(pkg = ErniePackage.class, name = "gammaFluxLines", type = double.class)
  public static final String GAMMA_FLUX_LINES = URN + "#gammaFluxLines"; // Double

  @Reader.ElementDeclaration(pkg = ErniePackage.class, name = "gammaFluxTotal", type = double.class)
  public static final String GAMMA_FLUX_TOTAL = URN + "#gammaFluxTotal"; // Double

  @Reader.ElementDeclaration(pkg = ErniePackage.class, name = "neutronFluxTotal", type = double.class)
  public static final String NEUTRON_FLUX_TOTAL = URN + "#neutronFluxTotal"; // Double

  @Reader.ElementDeclaration(pkg = ErniePackage.class, name = "distance", type = double.class)
  public static final String DISTANCE = URN + "#distance"; // Double

  @Reader.ElementDeclaration(pkg = ErniePackage.class, name = "gammaDose", type = double.class)
  public static final String GAMMA_DOSE = URN + "#gammaDose"; // Double

  @Reader.ElementDeclaration(pkg = ErniePackage.class, name = "neutronDose", type = double.class)
  public static final String NEUTRON_DOSE = URN + "#neutronDose"; // Double

  @Reader.ElementDeclaration(pkg = ErniePackage.class, name = "temperature", type = double.class)
  public static final String TEMPERATURE = URN + "#temperature"; // Double (C)

  @Reader.ElementDeclaration(pkg = ErniePackage.class, name = "highVoltage", type = double.class)
  public static final String HIGH_VOLTAGE = URN + "#highVoltage"; // Double (V)

  /**
   * Option to writer to prevent fields from being written
   */
  public static final String WRITER_EXCLUDE = URN + "#exclude"; // Predicate
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