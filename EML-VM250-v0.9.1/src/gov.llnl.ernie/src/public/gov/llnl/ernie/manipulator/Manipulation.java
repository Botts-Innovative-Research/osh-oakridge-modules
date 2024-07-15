/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.ernie.manipulator;

import gov.llnl.ernie.analysis.Features;
import gov.llnl.ernie.analysis.FeaturesDescription;
import gov.llnl.ernie.rtk.DoubleSpectrum;

/**
 * Class to hold directions for how a record is to be manipulated for source
 * injection and speed changes.
 */
public class Manipulation implements Features
{
  /**
   * @return the vehicleClass
   */
  public int getVehicleClass()
  {
    return vehicleClass;
  }

  /**
   * @param vehicleClass the vehicleClass to set
   */
  public void setVehicleClass(int vehicleClass)
  {
    this.vehicleClass = vehicleClass;
  }
  public static final String EXTERNAL = "EXTERNAL";

  private static final long serialVersionUID
          = gov.llnl.utility.UUIDUtilities.createLong("Manipulation-v1");
  private FeaturesDescription description;

  boolean alterVelocity; // manipulations[1]
  private boolean alterVehicle;

  int where; // where injection was requested. 0: cabin, 1: payload, 2: driver, 3: anywhere

  boolean injectCompact; // manipulations[2]
  boolean injectDistributed; // manipulations[3]
  boolean injectExtracted;

  double speedStart; // [4]
  double speedEnd; // [5]
  double compactIntensity; // [6]
  double compactPx; // [7]
  double compactPy; // [8]
  double compactPz; // [9]
  String compactSourceId = null; // [10]

  double distributedIntensity1;
  double distributedIntensity2;
  double distributedPx1;
  double distributedPx2;
  double distributedPy;
  double distributedPz;
  String distributedSourceId = null; // [10]

  private int vehicleClass = 0;
  int injectedExtractedId; //

  int cargoModelId = 0;

//  double[] compactSourceSpectrum;
//  double[] distributedSourceSpectrum;
  // TODO added this for the refactor, see manipulationInfo.sourceType getting set in FeatureBuilder
  int sourceType;
  double gammaLeakage;
  double neutronLeakage;
  DoubleSpectrum compactSource;
  DoubleSpectrum distributedSource;

  /**
   * ManipulationDescription with prefix = "Manipulation."
   */
  public static ManipulationDescription MANIPULATION1_DESCRIPTION = new ManipulationDescription("Info.Manipulation.");
  /**
   * ManipulationDescription with prefix = "Manipulation2."
   */
  public static ManipulationDescription MANIPULATION2_DESCRIPTION = new ManipulationDescription("Info.Manipulation2.");

  /**
   * See {@link #MANIPULATION1_DESCRIPTION} and
   * {@link #MANIPULATION2_DESCRIPTION}
   *
   * @param description
   */
  public Manipulation(ManipulationDescription description)
  {
    this.description = description;
  }

  public Manipulation(Manipulation manip)
  {
    this.description = manip.description;
    this.alterVelocity = manip.alterVelocity; // manipulations[1]
    this.alterVehicle = manip.alterVehicle;

    this.where = manip.where; // where injection was requested. 0: cabin, 1: payload, 2: driver, 3: anywhere

    this.injectCompact = manip.injectCompact; // manipulations[2]
    this.injectDistributed = manip.injectDistributed; // manipulations[3]
    this.injectExtracted = manip.injectExtracted;

    this.speedStart = manip.speedStart; // [4]
    this.speedEnd = manip.speedEnd; // [5]
    this.compactIntensity = manip.compactIntensity; // [6]
    this.compactPx = manip.compactPx; // [7]
    this.compactPy = manip.compactPy; // [8]
    this.compactPz = manip.compactPz; // [9]
    this.compactSourceId = manip.compactSourceId; // [10]

    this.distributedIntensity1 = manip.distributedIntensity1;
    this.distributedIntensity2 = manip.distributedIntensity2;
    this.distributedPx1 = manip.distributedPx1;
    this.distributedPx2 = manip.distributedPx2;
    this.distributedPy = manip.distributedPy;
    this.distributedPz = manip.distributedPz;
    this.distributedSourceId = manip.distributedSourceId; // [10]
    if (manip.compactSource != null)
      this.compactSource = new DoubleSpectrum(manip.compactSource);
    if (manip.distributedSource != null)
      this.distributedSource = new DoubleSpectrum(manip.distributedSource);
  }

  @Override
  public FeaturesDescription getDescription()
  {
    return this.description;
  }

  public void setGroupDescription(FeaturesDescription description)
  {
    this.description = description;
  }

  /**
   * Directs the injection of a compact source with a defined spectrum defined
   * externally from ERNIE.
   *
   * @param source
   */
  public void setExternalCompactSource(DoubleSpectrum source)
  {
    this.injectCompact = true;
    this.compactIntensity = 1;
    this.compactSource = source;
    this.compactSourceId = null;
  }

  /**
   * Directs the injection of a distributed source with a defined spectrum
   * defined externally from ERNIE.
   *
   * @param source
   */
  public void setExternalDistributedSource(DoubleSpectrum source)
  {
    this.injectDistributed = true;
    this.distributedSource = source;
    this.distributedSourceId = null;
  }

  /**
   * Define the location of a compact source injection.
   *
   * @param x
   * @param y
   * @param z
   */
  public void setCompactLocation(double x, double y, double z)
  {
    this.compactPx = x;
    this.compactPy = y;
    this.compactPz = z;
  }

  public void dump()
  {
    System.out.println("Manip:");
    System.out.println("  alterVelocity " + alterVelocity); // manipulations[1]
    System.out.println("  injectCompact " + injectCompact); // manipulations[2]
    System.out.println("  injectDistributed " + injectDistributed); // manipulations[3]
    System.out.println("  speedStart " + speedStart); // [4]
    System.out.println("  speedEnd " + speedEnd); // [5]
    if (injectCompact)
    {
      System.out.println("  compactIntensity " + compactIntensity); // [6]
      System.out.println("  compactPx " + compactPx); // [7]
      System.out.println("  compactPy " + compactPy); // [8]
      System.out.println("  compactPz " + compactPz); // [9]
      System.out.println("  compactSourceId " + compactSourceId); // [10]
    }
    if (injectDistributed)
    {
      System.out.println("  distributedIntensity1 " + distributedIntensity1);
      System.out.println("  distributedIntensity2 " + distributedIntensity2);
      System.out.println("  distributedPx1 " + distributedPx1);
      System.out.println("  distributedPx2 " + distributedPx2);
      System.out.println("  distributedPy " + distributedPy);
      System.out.println("  distributedPz " + distributedPz);
      System.out.println("  distributedSourceId " + distributedSourceId); // [10]
      System.out.println("  distributedSource " + distributedSource); // [10]
    }
  }

  /**
   * @return the serialVersionUID
   */
  public static long getSerialVersionUID()
  {
    return serialVersionUID;
  }

  /**
   * @return the alterVelocity
   */
  public boolean isAlterVelocity()
  {
    return alterVelocity;
  }

  /**
   * @return the injectCompact
   */
  public boolean isInjectCompact()
  {
    return injectCompact;
  }

  /**
   * @return the injectDistributed
   */
  public boolean isInjectDistributed()
  {
    return injectDistributed;
  }

  /**
   * @return the speedStart
   */
  public double getSpeedStart()
  {
    return speedStart;
  }

  /**
   * @return the speedEnd
   */
  public double getSpeedEnd()
  {
    return speedEnd;
  }

  /**
   * @return the compactIntensity
   */
  public double getCompactIntensity()
  {
    return compactIntensity;
  }

  /**
   * @return the compactPx
   */
  public double getCompactPx()
  {
    return compactPx;
  }

  /**
   * @return the compactPy
   */
  public double getCompactPy()
  {
    return compactPy;
  }

  /**
   * @return the compactPz
   */
  public double getCompactPz()
  {
    return compactPz;
  }

  /**
   * @return the compactSourceId
   */
  public String getCompactSourceId()
  {
    return compactSourceId;
  }

  /**
   * @return the distributedIntensity1
   */
  public double getDistributedIntensity1()
  {
    return distributedIntensity1;
  }

  /**
   * @return the distributedIntensity2
   */
  public double getDistributedIntensity2()
  {
    return distributedIntensity2;
  }

  /**
   * @return the distributedPx1
   */
  public double getDistributedPx1()
  {
    return distributedPx1;
  }

  /**
   * @return the distributedPx2
   */
  public double getDistributedPx2()
  {
    return distributedPx2;
  }

  /**
   * @return the distributedPy
   */
  public double getDistributedPy()
  {
    return distributedPy;
  }

  /**
   * @return the distributedPz
   */
  public double getDistributedPz()
  {
    return distributedPz;
  }

  /**
   * @return the distributedSourceId
   */
  public String getDistributedSourceId()
  {
    return distributedSourceId;
  }

  /**
   * @return the cargoModelId
   */
  public int getCargoModelId()
  {
    return cargoModelId;
  }

  /**
   * @return the sourceType
   */
  public int getSourceType()
  {
    return sourceType;
  }

  /**
   * @return the gammaLeakage
   */
  public double getGammaLeakage()
  {
    return gammaLeakage;
  }

  /**
   * @return the neutronLeakage
   */
  public double getNeutronLeakage()
  {
    return neutronLeakage;
  }

  /**
   * @param alterVelocity the alterVelocity to set
   */
  public void setAlterVelocity(boolean alterVelocity)
  {
    this.alterVelocity = alterVelocity;
  }

  /**
   * @param injectCompact the injectCompact to set
   */
  public void setInjectCompact(boolean injectCompact)
  {
    this.injectCompact = injectCompact;
  }

  /**
   * @param injectDistributed the injectDistributed to set
   */
  public void setInjectDistributed(boolean injectDistributed)
  {
    this.injectDistributed = injectDistributed;
  }

  /**
   * @param speedStart the speedStart to set
   */
  public void setSpeedStart(double speedStart)
  {
    this.speedStart = speedStart;
  }

  /**
   * @param compactIntensity the compactIntensity to set
   */
  public void setCompactIntensity(double compactIntensity)
  {
    this.compactIntensity = compactIntensity;
  }

  /**
   * @param compactPx the compactPx to set
   */
  public void setCompactPx(double compactPx)
  {
    this.compactPx = compactPx;
  }

  /**
   * @param compactPy the compactPy to set
   */
  public void setCompactPy(double compactPy)
  {
    this.compactPy = compactPy;
  }

  /**
   * @param compactPz the compactPz to set
   */
  public void setCompactPz(double compactPz)
  {
    this.compactPz = compactPz;
  }

  /**
   * @param compactSourceId the compactSourceId to set
   */
  public void setCompactSourceId(String compactSourceId)
  {
    this.compactSourceId = compactSourceId;
  }

  /**
   * @param distributedIntensity1 the distributedIntensity1 to set
   */
  public void setDistributedIntensity1(double distributedIntensity1)
  {
    this.distributedIntensity1 = distributedIntensity1;
  }

  /**
   * @param distributedIntensity2 the distributedIntensity2 to set
   */
  public void setDistributedIntensity2(double distributedIntensity2)
  {
    this.distributedIntensity2 = distributedIntensity2;
  }

  /**
   * @param distributedPx1 the distributedPx1 to set
   */
  public void setDistributedPx1(double distributedPx1)
  {
    this.distributedPx1 = distributedPx1;
  }

  /**
   * @param distributedPx2 the distributedPx2 to set
   */
  public void setDistributedPx2(double distributedPx2)
  {
    this.distributedPx2 = distributedPx2;
  }

  /**
   * @param distributedPy the distributedPy to set
   */
  public void setDistributedPy(double distributedPy)
  {
    this.distributedPy = distributedPy;
  }

  /**
   * @param distributedPz the distributedPz to set
   */
  public void setDistributedPz(double distributedPz)
  {
    this.distributedPz = distributedPz;
  }

  /**
   * @param distributedSourceId the distributedSourceId to set
   */
  public void setDistributedSourceId(String distributedSourceId)
  {
    this.distributedSourceId = distributedSourceId;
  }

  /**
   * @param cargoModelId the cargoModelId to set
   */
  public void setCargoModelId(int cargoModelId)
  {
    this.cargoModelId = cargoModelId;
  }

  /**
   * @param sourceType the sourceType to set
   */
  public void setSourceType(int sourceType)
  {
    this.sourceType = sourceType;
  }

  /**
   * @param gammaLeakage the gammaLeakage to set
   */
  public void setGammaLeakage(double gammaLeakage)
  {
    this.gammaLeakage = gammaLeakage;
  }

  /**
   * @param neutronLeakage the neutronLeakage to set
   */
  public void setNeutronLeakage(double neutronLeakage)
  {
    this.neutronLeakage = neutronLeakage;
  }

  /**
   * @param description the description to set
   */
  public void setDescription(FeaturesDescription description)
  {
    this.description = description;
  }

  /**
   * @param speedEnd the speedEnd to set
   */
  public void setSpeedEnd(double speedEnd)
  {
    this.speedEnd = speedEnd;
  }

  /**
   * @return the compactSource
   */
  public DoubleSpectrum getCompactSource()
  {
    return compactSource;
  }

  /**
   * @param compactSource the compactSource to set
   */
  public void setCompactSource(DoubleSpectrum compactSource)
  {
    this.compactSource = compactSource;
  }

  /**
   * @return the distributedSource
   */
  public DoubleSpectrum getDistributedSource()
  {
    return distributedSource;
  }

  /**
   * @param distributedSource the distributedSource to set
   */
  public void setDistributedSource(DoubleSpectrum distributedSource)
  {
    this.distributedSource = distributedSource;
  }

  /**
   *
   * @return where injection made. 0=cabin, 1=payload, 2=driver, 3=anywhere
   */
  public int getWhere()
  {
    return this.where;
  }

  public void setWhere(int where)
  {
    this.where = where;
  }

  /**
   * @return the alterVehicle
   */
  public boolean isAlterVehicle()
  {
    return alterVehicle;
  }

  /**
   * @param alterVehicle the alterVehicle to set
   */
  public void setAlterVehicle(boolean alterVehicle)
  {
    this.alterVehicle = alterVehicle;
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