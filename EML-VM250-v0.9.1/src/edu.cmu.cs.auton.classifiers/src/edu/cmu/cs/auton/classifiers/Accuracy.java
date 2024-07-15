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
 * Class containing the expected accuracy of prediction with confidence bounds.
 * @author sray
 *
 */
public class Accuracy {
	
	private double expectedAccuracy, expectedAccuracyUpper, expectedAccuracyLower;
	
	public Accuracy() {
		expectedAccuracy = -1;
		expectedAccuracyUpper = -1;
		expectedAccuracyLower = -1;
	}
	
	public Accuracy(double expectedAccuracy, double expectedAccuracyUpper, double expectedAccuracyLower) {
		this.expectedAccuracy = expectedAccuracy;
		this.expectedAccuracyUpper = expectedAccuracyUpper;
		this.expectedAccuracyLower = expectedAccuracyLower;
	}
	
	public double getExpectedAccuracy() {
		return expectedAccuracy;
	}

	public void setExpectedAccuracy(double expectedAccuracy) {
		this.expectedAccuracy = expectedAccuracy;
	}

	public double getExpectedAccuracyUpper() {
		return expectedAccuracyUpper;
	}

	public void setExpectedAccuracyUpper(double expectedAccuracyUpper) {
		this.expectedAccuracyUpper = expectedAccuracyUpper;
	}

	public double getExpectedAccuracyLower() {
		return expectedAccuracyLower;
	}

	public void setExpectedAccuracyLower(double expectedAccuracyLower) {
		this.expectedAccuracyLower = expectedAccuracyLower;
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