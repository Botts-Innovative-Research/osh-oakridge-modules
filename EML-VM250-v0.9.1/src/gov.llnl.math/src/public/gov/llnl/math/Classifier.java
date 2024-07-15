/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.math;

import java.io.Serializable;
import java.util.Collection;

/**
 * Generic interface for all classification algorithms. The job of a classifier
 * is to assign one or more labels to a sample. Classifiers usually produce a
 * list of classifications such that the probabilities add up to one. It is
 * assumed that each of the labels are mutually exclusive.
 * <p>
 * Alternative behaviors should be documented by the specific implementation.
 *
 * @author nelson85
 * @param <Input>
 */
public interface Classifier<Input> extends Serializable
{
  /**
   * Generic interface for labels created by the classifier. The classifier
   * implementation should extend this interface to produce it specific label
   * with any additional information associated with that label.
   */
  interface Classification extends Serializable
  {
    /**
     * Get the label associated with the classification.
     *
     * @return
     */
    String getLabel();

    /**
     * Get the probability that the label is associated with the input.
     *
     * @return
     */
    double getProbability();

    /**
     * Get the log likelihood for this sample.
     *
     * @return
     */
    default double getLikelihood()
    {
      return Math.log(getProbability());
    }
  }

  interface ClassificationSet<Type extends Classification>
          extends Collection<Type>, Serializable
  {

  }

  /**
   * Each classification should have an associated rule. This class if primarily
   * a place holder to enforce a naming convention.
   */
  interface Rule extends Serializable
  {
  }

  /**
   * Analyze an input and assign it a set of labels.
   *
   * @param input is the sample to classify.
   * @return a list of classifications that apply.
   */
  ClassificationSet classify(Input input);
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