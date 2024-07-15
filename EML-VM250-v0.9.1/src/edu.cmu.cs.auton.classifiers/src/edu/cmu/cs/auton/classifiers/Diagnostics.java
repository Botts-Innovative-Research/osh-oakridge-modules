/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */

package edu.cmu.cs.auton.classifiers;

/**
 * Helper class containing attribute usages and inbounds scores.
 * This class object is used to collect the attribute usage and inbounds score metrics 
 * while prediction is made by the RF decision trees.
 * @author sray
 *
 */
public class Diagnostics {

	private double inbounds, total;
	private double rawCounts[], weightedCounts[];
	
	public Diagnostics(int num) {
		rawCounts = new double[num];
		weightedCounts = new double[num];
	}
	
	public double getInbounds() {
		return inbounds;
	}
	public void setInbounds(double inbounds) {
		this.inbounds = inbounds;
	}
	public double getTotal() {
		return total;
	}
	public void setTotal(double total) {
		this.total = total;
	}
	public double[] getRawCounts() {
		return rawCounts;
	}
	public void setRawCounts(double[] rawCounts) {
		this.rawCounts = rawCounts;
	}
	public double[] getWeightedCounts() {
		return weightedCounts;
	}
	public void setWeightedCounts(double[] weightedCounts) {
		this.weightedCounts = weightedCounts;
	}
	
	public void incrementRawCount(int attnum, double incr) {
		rawCounts[attnum] += incr;
	}
	
	public void incrementWeightedCount(int attnum, double incr) {
		weightedCounts[attnum] += incr;
	}
	
	public void incrementTotal(double incr) {
		total += incr;
	}
	
	public void incrementInBounds(double incr) {
		inbounds += incr;
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