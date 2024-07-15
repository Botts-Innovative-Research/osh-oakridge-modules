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

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

/**
 * This class represents a random forest classifier consisting of an array of decision trees.
 * This class object can be created from a prior saved model file and can be used to make predictions and diagnostics on 
 * query/test points.
 * @author sray
 *
 */
public class RandomForestClassifier {
	
	//Name of the model file
	private String modelName;
	
	// Input feature names
	private String featureNames[];
	
	// Output attribute's class names
	private String outputValueNames[];
	
	// Index of the output attribute (0-based)
	private int outputAttnum;
	
	// Array of decision trees
	private DecisionTree classifierArray[];
	
	// Lookup table mapping dotproduct sums to expected accuracies. May be NULL.
	private AccuracyLookupTable table;

	/*
	 * Default constructor
	 */
	public RandomForestClassifier() {
		featureNames = null;
		outputValueNames = null;
		outputAttnum = -1;
		classifierArray = null;
		table = null;
		modelName = null;
	}

	/*
	 * Method to make predictions on a query
	 * @param query Feature values for the query point
	 * @return Output Output class object containing the prediction scores and diagnostics. 
	 */
	public Output classifyQuery(double query[]) {
		int numFeatures = featureNames.length;
		int numLabels = outputValueNames.length;
		
		if(query.length != numFeatures-1) {
			throw new IllegalArgumentException("Invalid size of query data point!");
		}
		
		Diagnostics diag = new Diagnostics(numFeatures);
		double loglikelihoods[] = new double[numLabels];
		double entropySum = 0, dotproductSum = 0, sumOfSquares = 0;
		
		// Iterate through each of the decision trees making predictions
		for(int i=0; i<classifierArray.length; i++) {
			DecisionTree tree = classifierArray[i];
			double ll[] = tree.classifyQuery(query, 0, 1, diag);
			
			double entropy = getEntropy(ll);
			// Sum up entropy from each tree
			entropySum += entropy;
			
			// Sum up predictions from each tree
			for(int j=0; j<numLabels; j++) {
				loglikelihoods[j] += ll[j]; 
				sumOfSquares += (ll[j] * ll[j]);
			}
		}
		
		double meanEntropy = entropySum/classifierArray.length;
		double consistency = getConsistency(meanEntropy, numLabels);
		
		// Compute dotproduct sum
		for(int j=0; j<numLabels; j++) {
			dotproductSum += (loglikelihoods[j] * loglikelihoods[j]);
		}
		dotproductSum -= sumOfSquares;
		dotproductSum /= 2;
		
		// Take log for the summation of tree probabilities
		for(int j=0; j<numLabels; j++) {
			loglikelihoods[j] = blog(loglikelihoods[j]);
		}
		
		//Populate Output class object
		Output out = new Output();
		out.setClassLogLikelihoods(loglikelihoods);
		out.setDotProductSum(dotproductSum);
		out.setMeanEntropy(meanEntropy);
		out.setInboundsScore(diag.getInbounds()/diag.getTotal());
		out.setRawFeatureCount(diag.getRawCounts());
		out.setWeightedFeatureCount(diag.getWeightedCounts());
		out.setConsistency(consistency);
		
		// If dotproduct sum lookup table available, compute expected accuracy of prediction (with confidence bounds)
		if(table != null) {
			Accuracy accuracy = table.computeExpectedAccuracy(dotproductSum);
			out.setAccuracy(accuracy);
		}
		
		return out;
	}
	
	public String[] getFeatureNames() {
		return featureNames;
	}

	public void setFeatureNames(String[] featureNames) {
		this.featureNames = featureNames;
	}

	public String getModelName() {
		return modelName;
	}

	public void setModelName(String modelName) {
		this.modelName = modelName;
	}
	
	public String[] getOutputValueNames() {
		return outputValueNames;
	}

	public void setOutputValueNames(String[] outputValueNames) {
		this.outputValueNames = outputValueNames;
	}

	public int getOutputAttnum() {
		return outputAttnum;
	}

	public void setOutputAttnum(int outputAttnum) {
		this.outputAttnum = outputAttnum;
	}

	public DecisionTree[] getClassifierArray() {
		return classifierArray;
	}

	public void setClassifierArray(DecisionTree[] classifierArray) {
		this.classifierArray = classifierArray;
	}
	
	public AccuracyLookupTable getTable() {
		return table;
	}

	public void setTable(AccuracyLookupTable table) {
		this.table = table;
	}
	
	private double blog(double x) {
		if (x <= 5.1482e-131)
			return -300.0;
		return Math.log(x);
	}
	
	/*
	 * Compute entropy for the prediction vector
	 */
	private double getEntropy(double likelihoods[]) {
		double sum = 0;
		for(int i=0; i<likelihoods.length; i++) {
			double val = likelihoods[i];
			sum += val * blog(val);
		}
		
		sum *= -1.0;
		
		return sum;
	}
	
	/*
	 * Compute consistency
	 */
	private double getConsistency(double meanEntropy, int arity) {
		double maxEntropy = Math.log(arity);
		double consistency = 1.0 - (meanEntropy/maxEntropy);
		return consistency;
	}
	
	/*
	 * Sample main function
	 */
	public static void main(String[] args) {
		
		RandomForestClassifier cl = null;
		
		// Load a RF classifier model file
		try {
			cl = InOut.loadModelFile(args[0]);
		} catch (IOException exc) {
			System.out.println(exc.getMessage());
		}
		
		// Load a CSV file containing mappings of dotproduct sums to expected accuracies 
		// (OPTIONAL step)
		try { 
			AccuracyLookupTable table = InOut.loadMetricsFile(args[2]);
			cl.setTable(table);
		} catch (IOException exc) {
			System.out.println(exc.getMessage());
		}
		
		// Make predictions on a sample query point
		double query[] = RandomForestClassifier.populateDataPointFeatures(args[1]);
		try {
				Output out = cl.classifyQuery(query);
				out.printOutput();
		} catch (Exception exc) {
			System.out.println(exc.getMessage());
		}
	}
	
	private static double[] populateDataPointFeatures(String fileName) {

        ArrayList<Double> features = new ArrayList<Double>();
        FileInputStream fis = null;

        try {
           fis = new FileInputStream(fileName);
        } catch(FileNotFoundException ex) {
           System.out.println(ex.getMessage());
        }

        try {
          InputStreamReader isr = new InputStreamReader(fis);
          BufferedReader br = new BufferedReader(isr);
          String line;
          line = br.readLine(); // skip first line
          line = br.readLine();
          String vals[] = line.split(",");
          for (int idx=0; idx<vals.length-1; idx++) {
              double value = Double.valueOf( vals[idx] );
              features.add(value);
          }

          br.close();
          isr.close();
          fis.close();
        } catch(IOException ex) {
           System.out.println(ex.getMessage());
        }

        int size = features.size();
        double inputFeatures[] = new double[size];
        for(int i=0; i<size; i++) {
          inputFeatures[i] = features.get(i);
        }

        return inputFeatures;
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