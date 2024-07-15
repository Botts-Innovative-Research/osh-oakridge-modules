/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.ernie.analysis;

import java.io.Serializable;
import java.util.Collection;

/**
 * Description of the data produced by a FeatureExtractor.
 *
 * This is used to convert a FeatureGroup into serialized forms either to store
 * on disk or push back into a database.
 *
 * @author nelson85
 */
public interface FeaturesDescription extends Serializable
{
  /**
   * Name of the feature extractor that computed the features.
   *
   * @return
   */
  String getPrefix();

  /**
   * Get the description of each feature to be stored in the feature group.
   *
   * @return an unmodifiable collection of feature descriptions.
   */
  Collection<FeatureDescription> getFeatureDescriptions();

  FeatureDescription getFeatureDescription(String key);

  public interface FeatureDescription<T, R>
  {
    /**
     * Get the name of the feature used as a key in the feature group.
     *
     * @return
     */
    String getName();

    /**
     * Get the type of data held.
     *
     * @return
     */
    Class getType(); // Double, Long, Int, Boolean

    R get(T obj);

    void set(T obj, R value);

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