/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.ernie.impl;

import gov.llnl.ernie.data.AnalysisSource;
import gov.llnl.math.DoubleArray;

/**
 *
 * @author nelson85
 */
public abstract class AnalysisSourceImpl implements AnalysisSource
{
  protected double probabilityContamination;
  protected double probabilityFissile;
  protected double probabilityIndustrial;
  protected double probabilityMedical;
  protected double probabilityNORM;
  protected double probabilityNonEmitting;
  protected double probabilityRelease;
  protected double probabilityInvestigate;
  protected double x1;
  protected double x2;
  protected double y;
  protected double z;
  protected String classifierUsed;

  public AnalysisSourceImpl()
  {
  }

  /**
   * @return the probabilityContamination
   */
  final public double getProbabilityContamination()
  {
    return probabilityContamination;
  }

  /**
   * @return the probabilityFissile
   */
  final public double getProbabilityFissile()
  {
    return probabilityFissile;
  }

  /**
   * @return the probabilityIndustrial
   */
  final public double getProbabilityIndustrial()
  {
    return probabilityIndustrial;
  }

  /**
   * @return the probabilityMedical
   */
  final public double getProbabilityMedical()
  {
    return probabilityMedical;
  }

  /**
   * @return the probabilityNORM
   */
  final public double getProbabilityNORM()
  {
    return probabilityNORM;
  }

  /**
   * @return the probabilityNonEmitting
   */
  final public double getProbabilityNonEmitting()
  {
    return probabilityNonEmitting;
  }

  /**
   * Get the probability for each of the source classes.
   *
   * @return
   */
  final public double[] getSourceProbabilities()
  {
    double[] probabilities = new double[6];
    probabilities[0] = this.probabilityNonEmitting;
    probabilities[1] = this.probabilityNORM;
    probabilities[2] = this.probabilityMedical;
    probabilities[3] = this.probabilityIndustrial;
    probabilities[4] = this.probabilityFissile;
    probabilities[5] = this.probabilityContamination;
    return probabilities;
  }

  /**
   * Returns true if the source is most likely non-emitting class.
   *
   * @return
   */
  final public boolean isNonEmitting()
  {
    double[] prob = getSourceProbabilities();
    return prob[DoubleArray.findIndexOfMaximum(prob)] == this.probabilityNonEmitting;
  }

  /**
   * Returns true if the source classification indicates a compact sources.
   *
   * @return
   */
  final public boolean isCompact()
  {
    return Math.abs(this.x2 - this.x1) < 3;
  }

  @Override
  final public boolean isDistributed()
  {
    return !isCompact();
  }

  /**
   * @return the probabilityRelease
   */
  final public double getProbabilityRelease()
  {
    return probabilityRelease;
  }

  /**
   * @return the probabilityInvestigate
   */
  final public double getProbabilityInvestigate()
  {
    return probabilityInvestigate;
  }

  final public double getPositionX1()
  {
    return this.x1;
  }

  final public double getPositionX2()
  {
    return this.x2;
  }

  final public double getPositionY()
  {
    return this.y;
  }

  final public double getPositionZ()
  {
    return this.z;
  }

  final public String getClassifierUsed()
  {
    return this.classifierUsed;
  }

  final public void setClassifierUsed(String classifier)
  {
    this.classifierUsed = classifier;
  }

  /**
   * @param probabilityContamination the probabilityContamination to set
   */
  final public void setProbabilityContamination(double probabilityContamination)
  {
    this.probabilityContamination = probabilityContamination;
  }

  /**
   * @param probabilityFissile the probabilityFissile to set
   */
  final public void setProbabilityFissile(double probabilityFissile)
  {
    this.probabilityFissile = probabilityFissile;
  }

  /**
   * @param probabilityIndustrial the probabilityIndustrial to set
   */
  final public void setProbabilityIndustrial(double probabilityIndustrial)
  {
    this.probabilityIndustrial = probabilityIndustrial;
  }

  /**
   * @param probabilityMedical the probabilityMedical to set
   */
  final public void setProbabilityMedical(double probabilityMedical)
  {
    this.probabilityMedical = probabilityMedical;
  }

  /**
   * @param probabilityNORM the probabilityNORM to set
   */
  final public void setProbabilityNORM(double probabilityNORM)
  {
    this.probabilityNORM = probabilityNORM;
  }

  /**
   * @param probabilityNonEmitting the probabilityNonEmitting to set
   */
  final public void setProbabilityNonEmitting(double probabilityNonEmitting)
  {
    this.probabilityNonEmitting = probabilityNonEmitting;
  }

  /**
   * @param probabilityRelease the probabilityRelease to set
   */
  final public void setProbabilityRelease(double probabilityRelease)
  {
    this.probabilityRelease = probabilityRelease;
  }

  /**
   * @param probabilityInvestigate the probabilityInvestigate to set
   */
  final public void setProbabilityInvestigate(double probabilityInvestigate)
  {
    this.probabilityInvestigate = probabilityInvestigate;
  }

  /**
   * @return the x1
   */
  final public double getX1()
  {
    return x1;
  }

  /**
   * @param x1 the x1 to set
   */
  final public void setX1(double x1)
  {
    this.x1 = x1;
  }

  /**
   * @return the x2
   */
  final public double getX2()
  {
    return x2;
  }

  /**
   * @param x2 the x2 to set
   */
  final public void setX2(double x2)
  {
    this.x2 = x2;
  }

  /**
   * @return the y
   */
  final public double getY()
  {
    return y;
  }

  /**
   * @param y the y to set
   */
  final public void setY(double y)
  {
    this.y = y;
  }

  /**
   * @return the z
   */
  final public double getZ()
  {
    return z;
  }

  /**
   * @param z the z to set
   */
  final public void setZ(double z)
  {
    this.z = z;
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