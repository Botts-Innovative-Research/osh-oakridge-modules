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

import java.util.ArrayList;

/**
 * Class containing the lookup table of dotproduct sum to expected accuracy of prediction (with confidence bounds).
 * @author sray
 *
 */
public class AccuracyLookupTable {

	// List of dotproduct sums in descending order
	private ArrayList<Double> dotproductSumLookup;
	
	// List of accuracy objects mapped to dotproduct sums in the above list
	private ArrayList<Accuracy> lookupAccuracies;
	
	public AccuracyLookupTable(ArrayList<Double> dotproductSumLookup, ArrayList<Accuracy> lookupAccuracies) {
		this.dotproductSumLookup = dotproductSumLookup;
		this.lookupAccuracies = lookupAccuracies;
	}
	
	/*
	 * Compute expected accuracy of prediction for a dotproduct sum
	 * @param dotproductSum Dotproduct sum computed for the prediction made for a query
	 * @return Expected accuracy (with confidence bounds)
	 */
	public Accuracy computeExpectedAccuracy(double dotproductSum) {
		int M = findClosestIndex(dotproductSum);
		return lookupAccuracies.get(M);
	}
	
	/*
	 * Helper method to lookup the dotproduct sum closest to the input value
	 * Uses binary search to narrow down
	 */
	private int findClosestIndex(double dotproductSum) {
		int lo = 0, up = dotproductSumLookup.size()-1;
		while(lo <= up) {
			int M = (lo + up)/2;
			if(dotproductSumLookup.get(M) <= dotproductSum &&
					(M-1 >= 0 && dotproductSumLookup.get(M-1) > dotproductSum)) {
				return M;
			}
			if(dotproductSumLookup.get(M) <= dotproductSum) 
				up = M-1;
			else
				lo = M+1;
		}
		
		if(lo > up)
			lo = up;
		if(lo < 0)
			lo = 0;
		
		return lo;
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