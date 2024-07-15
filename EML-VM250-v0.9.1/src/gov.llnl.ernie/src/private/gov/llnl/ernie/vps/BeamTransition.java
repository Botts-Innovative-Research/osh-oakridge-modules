/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.ernie.vps;

/**
 *
 * @author nelson85
 */
public class BeamTransition implements java.io.Serializable
{
  private static final long serialVersionUID = gov.llnl.utility.UUIDUtilities.createLong("BeamTransition-v1");
  public double transition; // may be in time or distance
  public int present;
  public int known;

  public BeamTransition(double transition, int present, int known)
  {
    this.transition = transition;
    this.present = present;
    this.known = known;
  }

  public BeamTransition(BeamTransition bt)
  {
    this.transition = bt.transition;
    this.present = bt.present;
    this.known = bt.known;
  }

  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    sb.append("transition(");
    sb.append("x=").append(transition).append(", ");
    sb.append("p=").append(present).append(")");
    return sb.toString();
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