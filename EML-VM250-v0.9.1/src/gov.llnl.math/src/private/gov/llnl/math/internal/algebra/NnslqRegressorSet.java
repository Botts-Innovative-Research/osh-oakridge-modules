/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.math.internal.algebra;

import gov.llnl.math.DoubleArray;
import gov.llnl.math.algebra.Nnlsq;
import gov.llnl.utility.UUIDUtilities;
import gov.llnl.utility.annotation.Matlab;
import java.util.Iterator;

/**
 *
 * @author nelson85
 */
public class NnslqRegressorSet implements Nnlsq.Output<NnlsqRegressor>
{
  private static final long serialVersionUID = UUIDUtilities.createLong("NnslqRegressorSet-v1");
  NnlsqImpl parent;
  NnlsqRegressor head;
  NnlsqRegressor tail;
  int size = 0;

  NnslqRegressorSet(NnlsqImpl parent)
  {
    this.parent = parent;
  }

  public NnlsqRegressor[] toArray()
  {
    NnlsqRegressor[] out = new NnlsqRegressor[size()];
    int i = 0;
    for (NnlsqRegressor regressor : this)
    {
      out[i++] = regressor;
    }
    return out;
  }

  /**
   * Remove a regressor from the set.
   *
   * @param regressor
   */
  void remove(NnlsqRegressor regressor)
  {
    if (regressor.set != this)
      throw new RuntimeException("Attempt to remove regressor contained in another set");
    if (regressor.prev != null)
      regressor.prev.next = regressor.next;
    if (regressor.next != null)
      regressor.next.prev = regressor.prev;
    if (head == regressor)
      head = regressor.next;
    if (tail == regressor)
      tail = regressor.prev;
    // Clear all points on the removed item
    regressor.next = null;
    regressor.prev = null;
    regressor.set = null;
    size--;
  }

  /**
   * Inserts the regressor at the end of the list.
   *
   * @param regressor
   */
  void add(NnlsqRegressor regressor)
  {
    if (regressor.set != null)
      throw new RuntimeException("Attempt to add regressor which is part of another set");
    regressor.set = this;
    size++;
    regressor.prev = tail;
    regressor.next = null;
    if (head == null)
      head = regressor;
    else
      tail.next = regressor;
    tail = regressor;
  }

  /**
   * Inserts an element after a particular element in the set.
   *
   * @param insertion
   * @param add
   */
  public void insert(NnlsqRegressor insertion, NnlsqRegressor add)
  {
    if (add.set != null)
      throw new RuntimeException("Attempt to insert element from another set");
    add.set = this;
    size++;
    // head insertion
    if (insertion == null)
    {
      add.prev = null;
      add.next = head;
      head.prev = add;
      head = add;
      return;
    }
    if (insertion == tail)
      tail = add;
    add.prev = insertion;
    add.next = insertion.next;
    insertion.next = add;
    if (add.next != null)
      add.next.prev = add;
  }

  @Override
  public Iterator<NnlsqRegressor> iterator()
  {
    return new NnlsqRegressorSetIterator(head, true);
  }

  public Iterator<NnlsqRegressor> decendingIterator()
  {
    return new NnlsqRegressorSetIterator(tail, false);
  }

  public int size()
  {
    return this.size;
  }

  public boolean isEmpty()
  {
    return head == null;
  }

  @Matlab
  public NnlsqRegressor get(int index)
  {
    if (index >= size)
      throw new RuntimeException("Index error");
    NnlsqRegressor start = head;
    for (int i = 0; i < index; ++i)
      start = start.next;
    return start;
  }

  void transferAll(NnslqRegressorSet set)
  {
    // Empty set logic
    if (set.head == null)
      return;
    size += set.size;
    // Claim all the nodes in the set
    NnlsqRegressor claim = set.head;
    while (claim != null)
    {
      claim.set = this;
      claim = claim.next;
    }
    if (this.head == null)
    {
      // Currently empty list
      this.head = set.head;
      this.tail = set.tail;
    }
    else
    {
      // Splice the head of the set to the tail
      set.head.prev = this.tail;
      this.tail.next = set.head;
      this.tail = set.tail;
    }
    // Empty the transferred set
    set.clear();
  }

  public void swap(NnlsqRegressor element1, NnlsqRegressor element2)
  {
    if (element1 == null || element2 == null)
      throw new RuntimeException("Null elements in swap");
    if (element1.set != this || element2.set != this)
      throw new RuntimeException("Attempt to manipulate from a different set");
    if (element1 == element2)
      return;
    // Special cases
    if (element2.next == element1)
    {
      this.remove(element2);
      this.insert(element1, element2);
      return;
    }
    if (element1.next == element2)
    {
      this.remove(element1);
      this.insert(element2, element1);
      return;
    }
    // Two seperated elements
    NnlsqRegressor insertion1 = element1.prev;
    NnlsqRegressor insertion2 = element2.prev;
    this.remove(element1);
    this.remove(element2);
    this.insert(insertion2, element1);
    this.insert(insertion1, element2);
  }

  private void clear()
  {
    head = null;
    tail = null;
    size = 0;
  }

  int indexOf(NnlsqRegressor element)
  {
    int i = 0;
    for (NnlsqRegressor entry : this)
    {
      if (entry == element)
        return i;
    }
    return -1;
  }

  public NnlsqRegressor findById(int regressorId)
  {
    for (NnlsqRegressor entry : this)
      if (entry.id == regressorId)
        return entry;
    return null;
  }

//<editor-fold desc="public api">
  @Matlab
  @Override
  public double[] toCoefficients()
  {
    double[] out = new double[parent.input.getNumRegressors()];
    copyCoefficients(out);
    return out;
  }

  @Override
  public void copyCoefficients(double[] X)
  {
    DoubleArray.fill(X, 0);
    for (NnlsqRegressor entry : this)
      X[entry.id] = entry.coef;
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