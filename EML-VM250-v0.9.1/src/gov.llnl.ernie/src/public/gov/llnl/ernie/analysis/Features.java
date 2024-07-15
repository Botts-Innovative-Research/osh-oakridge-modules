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

import gov.llnl.ernie.analysis.FeaturesDescription.FeatureDescription;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Features computed by a {@link FeatureExtractor}.
 *
 * A Features is a collection of string, double pairs storing values extracted
 * from a record. Not all feature groups are computed from FeatureExtractors.
 * Some are simply tables created by the feature table builder to annotate
 * records.
 *
 * The Features is intended as a view over the top of an existing structure
 * constructed by a feature extractor. There may be other auditing information
 * stored in the Features's primate implementation.
 *
 * @author guensche1
 */
public interface Features extends Serializable
{
  /**
   * Get the description of the features stored in this group.
   *
   * This is taken from the FeatureExtractor that produced the group.
   *
   * @return
   */
  FeaturesDescription getDescription();

  /**
   * Get the number of items in the feature group.
   *
   * @return
   */
  default int size()
  {
    return getDescription().getFeatureDescriptions().size();
  }

  /**
   * Convert the feature group to map.
   *
   * Does not include the prefix in the feature name.
   *
   * @return
   */
  default Map<String, Object> toMap()
  {
    FeaturesDescription featuresDescription = getDescription();
    String prefix = featuresDescription.getPrefix()+".";
    TreeMap<String, Object> out = new TreeMap<>();
    for (FeatureDescription description : featuresDescription.getFeatureDescriptions())
    {
      String key = description.getName();
      out.put(prefix + key, this.get(key, Object.class));
    }
    return out;
  }
  
  /**
   * Get the features that match a specific class.
   * 
   * @param <T>
   * @param cls
   * @return 
   */
    default <T> List<T> toList(Class<T> cls)
  {
    FeaturesDescription featuresDescription = getDescription();
    LinkedList<T> out = new LinkedList<>();
    for (FeatureDescription description : featuresDescription.getFeatureDescriptions())
    {
      String key = description.getName();
      Object obj = this.get(key, Object.class);
      // Skip anything that does not match
      if (cls.isInstance(obj))
        out.add(cls.cast(obj));
    }
    return out;
  }


  /**
   * Set the value of a feature by name.
   *
   * @param key is the name of the feature to set.
   * @param value is the value to assign.
   * @throws RuntimeException if the features does not contain this data member.
   */
  default void set(String key, Object value) throws RuntimeException
  {
    this.getDescription().getFeatureDescription(key).set(key, value);
  }

  /**
   * Get a value of a feature by name.
   *
   * @param <T> is the type of the feature.
   * @param key is the name of the feature to get.
   * @param cls is the class of the feature.
   * @return
   * @throws RuntimeException if the features does not contain this data member.
   * @throws ClassCastException if the feature type is incorrect.
   */
  default <T> T get(String key, Class<T> cls) throws RuntimeException, ClassCastException
  {
    Object value = this.getDescription().getFeatureDescription(key).get(this);
    if (cls != null && !cls.isAssignableFrom(value.getClass()))
    {
      throw new ClassCastException("Feature class incorrect for " + key);
    }
    return (T) value;
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