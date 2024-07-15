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

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.BiConsumer;
import java.util.function.Function;
import gov.llnl.ernie.analysis.Features;
import gov.llnl.ernie.analysis.FeaturesDescription;
import gov.llnl.utility.StringUtilities;
import java.util.stream.Collectors;

/**
 * Feature Group description serves as a dictionary to make variables in
 * features into names.
 *
 * @author nelson85
 * @param <T>
 */
public class FeaturesDescriptionImpl<T extends Features> implements FeaturesDescription
{
  private static final long serialVersionUID = gov.llnl.utility.UUIDUtilities.createLong("FeaturesDescriptionImpl-v1");
  private final String name;
  private final List<FeatureDescription> features;
  private final Map<String, FeatureDescription> map = new TreeMap<>();

  public FeaturesDescriptionImpl(String prefix, List<FeatureDescription> features)
  {
    this.name = prefix;
    this.features = features;
    buildIndex();
  }

  @Override
  public String getPrefix()
  {
    return name;
  }

  @Override
  public Collection<FeatureDescription> getFeatureDescriptions()
  {
    return Collections.unmodifiableList(features);
  }

  @Override
  public FeatureDescription getFeatureDescription(String key)
  {
    FeatureDescription fd = map.get(key);
    if (fd == null)
    {
      String available = StringUtilities.join(map.keySet().stream().collect(Collectors.toList()), ", ");
      throw new RuntimeException("Unable to find feature " + key + " in " + this.getClass().getName() + " available " + available);
    }
    return fd;
  }

  /**
   * Get the value of a named feature from a Features.
   *
   * @param features
   * @param key
   * @return
   */
  public Object get(T features, String key)
  {
    FeatureDescription fd = map.get(key);
    return fd.get(features);
  }

  public void set(T features, String key, Object value)
  {
    FeatureDescription fd = map.get(key);
    fd.set(features, value);
  }

//<editor-fold desc="builder">
  /**
   * Called by the constructor to build a map of features for name lookups.
   */
  protected final void buildIndex()
  {
    this.features.sort((c1,c2)->c1.getName().compareTo(c2.getName()));
    map.clear();
    for (FeatureDescription f : this.features)
    {
      String lname = f.getName();
      if (map.containsKey(lname))
        throw new RuntimeException("Repeat key " + lname + " in " + this.getClass().getName());
      map.put(f.getName(), f);
    }
  }

  /**
   * Get the internal feature list.
   *
   * This is called in the constructor to access the hidden feature list when
   * features are defined dynamically.
   *
   * Typical use: * {@code
   * <pre>
   * super("MyFeatures", new ArrayList<>());
   * FeatureDescriptionBuilder builder=newBuilder(this.getInternal());
   * builder.add(...);
   * this.buildIndex(); // add the new features to the index.
   * </pre>
   * }
   *
   * @return
   */
  protected final List<FeatureDescription> getInternal()
  {
    return features;
  }

  /**
   * Create a new builder for the feature description list.
   *
   * This will be called either in a static initializer or in the constructor.
   *
   * @param features is the structure to hold the features.
   * @return a new builder.
   */
  public static FeatureDescriptionBuilder newBuilder(List<FeatureDescription> features)
  {
    return new FeatureDescriptionBuilder(features);
  }

  /**
   * Builder for constructing feature description tables.
   *
   * Called using newBuilder().
   */
  protected static class FeatureDescriptionBuilder
  {
    final List<FeatureDescription> features;

    FeatureDescriptionBuilder(List<FeatureDescription> features)
    {
      this.features = features;
    }

    /**
     * Defines a set of features from an array.
     *
     * @param <T> is the type of feature group we are accessing.
     * @param <F> is the array type (should be double[])
     * @param prefix is the prefix to append to the feature names.
     * @param accessor is a method to access the feature list. Must be readable
     * and writable.
     * @param length is the expected length of the array.
     * @param offset is added to the name of each feature
     */
    public <T, F> void defineArrayOrdinal(
            String prefix,
            Function<T, F> accessor, int length,
            int offset)
    {
      for (int i = 0; i < length; ++i)
      {
        final int i2 = i;
        this.add(prefix + (offset + i2),
                Double.class,
                (T t) -> ((double[]) accessor.apply(t))[i2],
                (T t, Double v) -> ((double[]) accessor.apply(t))[i2] = v
        );
      }
    }

    /**
     * Defines a set of features from an array.
     *
     * @param <T> is the type of feature group we are accessing.
     * @param <F> is the array type (should be double[])
     * @param prefix is the prefix to append to the feature names.
     * @param accessor is a method to access the feature list. Must be readable
     * and writable.
     * @param length is the expected length of the array.
     */
    public <T, F> void defineArrayOrdinal(
            String prefix,
            Function<T, F> accessor, int length)
    {
      defineArrayOrdinal(prefix, accessor, length, 0);
    }

    /**
     * Defines a set of features from an array with labeled columns.
     *
     * The length of the keys must match the length of the array.
     *
     * @param <T> is the type of feature group we are accessing.
     * @param <F> is the array type (should be double[])
     * @param prefix is the prefix to append to the feature names.
     * @param accessor is a method to access the feature list. Must be readable
     * and writable.
     * @param keys is a list of keys to label each column in order.
     */
    public <T, F> void defineArrayLabeled(
            String prefix,
            Function<T, F> accessor,
            String... keys)
    {
      for (int i = 0; i < keys.length; ++i)
      {
        final int i2 = i;
        this.add(prefix + keys[i2],
                Double.class,
                (T t) -> ((double[]) accessor.apply(t))[i2],
                (T t, Double v) -> ((double[]) accessor.apply(t))[i2] = v
        );
      }
    }

    /**
     * Include a group of features on a feature list.
     *
     * @param <T> is the type of feature group we are accessing.
     * @param <F> is the feature group type.
     * @param prefix is a string to prefix to the feature name.
     * @param description is the FeaturesDescription for this feature set.
     * @param accessor is a method to access the subfeature structure.
     */
    public <T, F> void includeFeatures(String prefix, FeaturesDescription description, Function<T, F> accessor)
    {
      for (FeatureDescription feature : description.getFeatureDescriptions())
      {
        final FeatureDescription feature2 = feature;
        this.add(prefix + feature.getName(),
                Double.class,
                (T f) -> (Double) feature2.get(accessor.apply(f)),
                (T f, Double d) -> feature2.set(accessor.apply(f), d)
        );
      }
    }

    /**
     * Add an individual feature to the feature list.
     *
     * @param <T> is the type of the feature group.
     * @param <R> is the type of the feature.
     * @param name is the name of the feature on the list.
     * @param type is the class of the feature. (typically Double.class)
     * @param getter is a method to get the value of the feature.
     * @param setter is a method to set the value of a feature.
     */
    public <T, R> void add(String name, Class<R> type, Function<T, R> getter, BiConsumer<T, R> setter)
    {
      features.add(new FeatureDescriptionImpl<>(name, type, getter, setter));
    }
  }
//</editor-fold>
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