/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.math.algebra;

import gov.llnl.math.IntegerArray;
import gov.llnl.utility.annotation.Matlab;

/**
 *
 * @author yao2
 */
public class IndexSet
{
  public IndexSet(int size)
  {
    resize(size);
  }

  public IndexSet(int[] set)
  {
    length_ = set.length;
    capacity_ = length_;
    set_ = set.clone();
  }

  public static IndexSet createFilled(int n)
  {
    IndexSet out = new IndexSet(n);
    out.fill(n);
    return out;
  }

  public final void resize(int size)
  {
    length_ = 0;
    capacity_ = size;
    set_ = new int[size];
  }

  public void clear()
  {
    length_ = 0;
  }

  public void set(int i, int value)
  {
    set_[i] = value;
  }

  public void fill(int n)
  {
    length_ = n;
    for (int i = 0; i < n; ++i)
    {
      //set_[i] = i;
      set(i, i);
    }
  }

  public void insert(int id)
  {
    set_[length_] = id;
    length_++;
  }

  public void erase(int index)
  {
    if (index > length_)
    {
      return;
    }
    length_--;
    set_[index] = set_[length_];
  }

  public void remove(int index)
  {

    length_--;
    for (int i = 0; i < length_; i++)
    {
      if (i >= index)
      {
        set(i, set_[i + 1]);
      }
    }
  }

  public int size()
  {
    return length_;
  }

  public int get(int i)
  {
    return set_[i];
  }

  public IndexSet clone()
  {
    IndexSet out = new IndexSet(this.size());
    for (int i = 0; i < size(); ++i)
    {
      out.set_[i] = this.set_[i];
    }
    out.length_ = this.length_;
    return out;
  }
  int capacity_;
  int length_;
  int[] set_;

  @Matlab
  public int[] toArray()
  {
    return IntegerArray.copyOfRange(set_, 0, length_);
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