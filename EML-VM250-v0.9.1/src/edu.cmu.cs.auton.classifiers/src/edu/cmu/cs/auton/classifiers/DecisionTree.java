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
import java.io.IOException;

/**
 * Class representing a decision tree loaded from a model file.
 * @author sray
 *
 */
public class DecisionTree {
	//-1 for leaf,otherwise attnum for splitting
	private int attnum;				
	
	//Threshold, Min-Max for numeric attribute range
	double cutoff, min, max;
	
	//Classification probability vector at leaf node
	double classCounts[];
	
	//All examples with x < cutoff
	DecisionTree left;
	
	//All examples with x >= cutoff
	DecisionTree right;
	
	/*
	 * Constructor
	 */
	public DecisionTree() {
		attnum = -1;
		cutoff = -1;
		min = -1;
		max = -1;
		
		classCounts = null;
		left = null;
		right = null;
	}
	
	/*
	 * Create a DecisionTree from model file
	 */
	public static DecisionTree createDecisionNode(BufferedReader br) throws IOException {
		DecisionTree node = new DecisionTree();
		
		InOut.inCheckBegin(br, "decision_node");
		
		boolean isLeaf = InOut.inBool(br);			// Leaf node?
		if(isLeaf == true) {
			boolean isClassification = InOut.inBool(br); //Classification?
			if(isClassification == true) {
				double counts[] = InOut.inDoubleArray(br);
				node.setClassCounts(counts);
			}
			else {
				throw new IllegalArgumentException("Regression not defined!");
			}
		}
		else {
			int attnum = InOut.inInteger(br);       // Attribute number
			node.setAttnum(attnum);
			
			boolean isSymbolic = InOut.inBool(br);  // Symbolic attribute?
			if(isSymbolic == false) {
				InOut.inBool(br); 					// Has missing values?
				InOut.inInteger(br); 				// Missing att decision path
				double cutoff = InOut.inDouble(br); // Split threshold
				double min = InOut.inDouble(br);	// Min of dataa points
				double max = InOut.inDouble(br);    // Max of data points
				
				node.setCutoff(cutoff);
				node.setMin(min);
				node.setMax(max);
			}
			else {
				throw new IllegalArgumentException("Symbolic attributes not defined!");
			}
			
			DecisionTree left = createDecisionNode(br); // Left child
			DecisionTree right = createDecisionNode(br);// Right child
			
			node.setLeft(left);
			node.setRight(right);
		}
		
		InOut.inCheckEnd(br, "decision_node");
		
		return node;
	}
	
	/*
	 * Method to make predictions for a query
	 * @param query Feature values for the query
	 * @param depth Depth of the current node in the decision tree
	 * @param incr Amount used for incrementing attribute usage counts, and inbounds score counts
	 * @diag Diagnostics object used for collecting attribute usage and inbounds scores   
	 */
	public double[] classifyQuery(double query[], int depth, double incr, Diagnostics diag) {
		if(attnum == -1) { // Leaf node?
			double sum = 0;
			for(int i=0; i<classCounts.length; i++) {
				sum += classCounts[i];
			}
			
			// Normalize
			double leafCounts[] = new double[classCounts.length];
			for(int i=0; i<classCounts.length && sum>0; i++) {
				leafCounts[i] = classCounts[i]/sum;
			}
			
			return leafCounts;
		}
		else { // Not a leaf node
			double weight = incr/(depth+1);
			diag.incrementWeightedCount(attnum, weight);
			diag.incrementRawCount(attnum, incr);
			
			double value = query[attnum];
			
			// In bounds?
			double valid = (value >= min && value <= max) ? incr : 0;
			
			diag.incrementInBounds(valid);
			diag.incrementTotal(incr);
			
			if(value < cutoff) {
				return left.classifyQuery(query, depth+1, incr, diag);	// Take left path
			}
			else {
				return right.classifyQuery(query, depth+1, incr, diag); // Take right path
			}
		}
	}
	
	public int getAttnum() {
		return attnum;
	}

	public void setAttnum(int attnum) {
		this.attnum = attnum;
	}

	public double getCutoff() {
		return cutoff;
	}

	public void setCutoff(double cutoff) {
		this.cutoff = cutoff;
	}

	public double getMin() {
		return min;
	}

	public void setMin(double min) {
		this.min = min;
	}

	public double getMax() {
		return max;
	}

	public void setMax(double max) {
		this.max = max;
	}

	public double[] getClassCounts() {
		return classCounts;
	}

	public void setClassCounts(double[] classCounts) {
		this.classCounts = classCounts;
	}

	public DecisionTree getLeft() {
		return left;
	}

	public void setLeft(DecisionTree left) {
		this.left = left;
	}

	public DecisionTree getRight() {
		return right;
	}

	public void setRight(DecisionTree right) {
		this.right = right;
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