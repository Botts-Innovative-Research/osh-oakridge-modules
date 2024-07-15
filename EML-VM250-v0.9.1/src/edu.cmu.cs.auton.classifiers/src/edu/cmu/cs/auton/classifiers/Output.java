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
 * Class containing the predictions and diagnostics made by the Random Forest classifier for a query/test point.
 * @author sray
 *
 */
public class Output {
	// Log likelihoods for each of the output classes (in alphabetical order of the class names)
	private double classLogLikelihoods[];
	
	// Diagnostics of the prediction
	private double meanEntropy, dotProductSum, inboundsScore, consistency;
	
	// Expected accuracy (with confidence bounds) if training metrics were provided
	private Accuracy accuracy;

	// Raw and weighted attribute usage
	private double rawFeatureCount[], weightedFeatureCount[];

	public Output() {	
		accuracy = null;
	}
	
	public void printOutput() {
		System.out.println("LogLikelihoods");
		for(int i=0; i<classLogLikelihoods.length; i++)
			System.out.println(classLogLikelihoods[i]);
		
		System.out.println("Mean entropy = " + meanEntropy);
		System.out.println("Dotproduct Sum = " + dotProductSum);
		System.out.println("inbounds = " + inboundsScore);
		
		if(accuracy != null)
			System.out.println("Accuracies = " + accuracy.getExpectedAccuracy() + " (" + accuracy.getExpectedAccuracyLower() +
					" - " + accuracy.getExpectedAccuracyUpper() + ")");
			
	}
	
	public double[] getClassLogLikelihoods() {
		return classLogLikelihoods;
	}

	public void setClassLogLikelihoods(double[] classLogLikelihoods) {
		this.classLogLikelihoods = classLogLikelihoods;
	}

	public double getMeanEntropy() {
		return meanEntropy;
	}

	public void setMeanEntropy(double meanEntropy) {
		this.meanEntropy = meanEntropy;
	}

	public double getDotProductSum() {
		return dotProductSum;
	}

	public void setDotProductSum(double dotProductSum) {
		this.dotProductSum = dotProductSum;
	}

	public double getInboundsScore() {
		return inboundsScore;
	}

	public void setInboundsScore(double inboundsScore) {
		this.inboundsScore = inboundsScore;
	}

	public double getConsistency() {
		return consistency;
	}

	public void setConsistency(double consistency) {
		this.consistency = consistency;
	}

	public double[] getRawFeatureCount() {
		return rawFeatureCount;
	}

	public void setRawFeatureCount(double[] rawFeatureCount) {
		this.rawFeatureCount = rawFeatureCount;
	}

	public double[] getWeightedFeatureCount() {
		return weightedFeatureCount;
	}

	public void setWeightedFeatureCount(double[] weightedFeatureCount) {
		this.weightedFeatureCount = weightedFeatureCount;
	}
	
	public Accuracy getAccuracy() {
		return accuracy;
	}

	public void setAccuracy(Accuracy accuracy) {
		this.accuracy = accuracy;
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