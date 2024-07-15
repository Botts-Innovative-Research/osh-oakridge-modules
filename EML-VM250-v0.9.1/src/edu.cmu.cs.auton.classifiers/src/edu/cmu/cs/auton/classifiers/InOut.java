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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.zip.GZIPInputStream;

/**
 * Class for loading different data types from a model file
 * @author sray
 *
 */
public class InOut {

	/*
	 * Read in integer
	 */
	public static int inInteger(BufferedReader br) throws IOException {
		int num;
		String line = br.readLine();
		
		try {
			num = Integer.valueOf(line);
		} catch (NumberFormatException nfe) {
			throw new IllegalArgumentException(line + " is not an integer!");
		}
		return num;
	}
	
	/*
	 * Read in double
	 */
	public static double inDouble(BufferedReader br) throws IOException {
		double num;
		String line = br.readLine();
		
		try {
			num = Double.valueOf(line);
		} catch (NumberFormatException nfe) {
			throw new IllegalArgumentException(line + " is not a double!");
		}
		return num;
	}
	
	/*
	 * Read in boolean
	 */
	public static boolean inBool(BufferedReader br) throws IOException {
		boolean num;
		String line = br.readLine();
		
		try {
			num = Boolean.valueOf(line);
		} catch (NumberFormatException nfe) {
			throw new IllegalArgumentException(line + " is not a boolean!");
		}
		return num;
	}
	
	/*
	 * Read in string
	 */
	public static String inString(BufferedReader br) throws IOException {
		String line = br.readLine();
		return line;
	}
	
	/*
	 * Read in token followed by integer
	 */
	public static int inCheckInteger(BufferedReader br, String token) throws IOException {
		int num;
		String line = br.readLine();
		String[] tokens = line.split(" ");
		
		if(tokens.length != 2 || tokens[0].compareTo(token) != 0)
			throw new IllegalArgumentException(line + " is invalid!");
		
		try {
			num = Integer.valueOf(tokens[1]);
		} catch (NumberFormatException nfe) {
			throw new IllegalArgumentException(tokens[1] + " is not a number!");
		}
		
		return num;
	}
	
	/*
	 * Read in string and checks whether it matches <token>
	 * Signifies the start of the object
	 */
	public static boolean inCheckBegin(BufferedReader br, String token) throws IOException {
		String line = br.readLine();
		if(line.compareTo("<" + token + ">") == 0)
			return true;
		return false;
	}
	
	/*
	 * Read in string and checks whether it matches </token>
	 * Signifies the end of the object
	 */
	public static boolean inCheckEnd(BufferedReader br, String token) throws IOException {
		String line = br.readLine();
		if(line.compareTo("</" + token + ">") == 0)
			return true;
		return false;
	}
	
	/*
	 * Read in string array.
	 * String array will be enclosed in between <string_array> and </string_array> lines.
	 * After <string_array>, the next line will contain the size of the array followed by individual elements,
	 * one element on a line.
	 */
	public static String[] inStringArray(BufferedReader br) throws IOException {
		inCheckBegin(br, "string_array");
		
		int size = inCheckInteger(br, "size");
		
		String tokens[] = new String[size];
		for(int i=0; i<size; i++) {
			tokens[i] = inString(br);
		}
		
		inCheckEnd(br, "string_array");
		
		return tokens;
	}
	
	/*
	 * Read in double array.
	 * Double array will be enclosed in between <dyv> and </dyv> lines.
	 * After <dyv>, the next line will contain the size of the array followed by individual elements,
	 * one element on a line.
	 */
	public static double[] inDoubleArray(BufferedReader br) throws IOException {
		inCheckBegin(br, "dyv");
		
		int size = inCheckInteger(br, "size");
		
		double tokens[] = new double[size];
		for(int i=0; i<size; i++) {
			tokens[i] = inDouble(br);
		}
		
		inCheckEnd(br, "dyv");
		
		return tokens;
	}
	
	/**
	 * Load a RandomForestClassifier object from a model file
	 */
	public static RandomForestClassifier loadModelFile(String modelFileName) throws IOException {
		Path file = Paths.get(modelFileName);
		if (file.endsWith(".gz")) {
			// Handle zipped input file
			try (InputStream is = Files.newInputStream(file);
				GZIPInputStream gis = new GZIPInputStream(is);
				InputStreamReader isr = new InputStreamReader(gis, StandardCharsets.UTF_8);
				BufferedReader br = new BufferedReader(isr)) {
				return createClassifier(br);
			}
		} else {
			try (BufferedReader br = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
				return createClassifier(br);
			}
		}
	}

	private static AccuracyLookupTable readMetricsFile(BufferedReader br) throws IOException {
		String line;
		ArrayList<Double> dotproducts = new ArrayList<Double>();
		ArrayList<Accuracy> accuracies = new ArrayList<Accuracy>();

		while ((line = br.readLine()) != null) {
			String tokens[] = line.split(",");
			if (tokens.length != 4)
				throw new IllegalArgumentException("Invalid line " + line);

			double d0 = Double.valueOf(tokens[0]);
			double d1 = Double.valueOf(tokens[1]);
			double d2 = Double.valueOf(tokens[2]);
			double d3 = Double.valueOf(tokens[3]);

			Accuracy acc = new Accuracy(d0, d1, d2);

			dotproducts.add(d3);
			accuracies.add(acc);
		}
		return new AccuracyLookupTable(dotproducts, accuracies);
	}

	/**
	 * Load a table mapping dotproduct sums to expected accuracies from a
	 * CSV file.
	 */
	public static AccuracyLookupTable loadMetricsFile(String metricsFileName) throws IOException {
		Path file = Paths.get(metricsFileName);
		if (file.endsWith(".gz")) {
			// Handle zipped input file
			try (InputStream is = Files.newInputStream(file);
				GZIPInputStream gis = new GZIPInputStream(is);
				InputStreamReader isr = new InputStreamReader(gis, StandardCharsets.UTF_8);
				BufferedReader br = new BufferedReader(isr)) {
				return readMetricsFile(br);
			}
		} else {
			try (BufferedReader br = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
				return readMetricsFile(br);
			}
		}
	}

	/*
	 * Load a RandomForestClassifier object
	 */
	public static RandomForestClassifier createClassifier(BufferedReader br) throws IOException {
		RandomForestClassifier cl = new RandomForestClassifier();
		
		InOut.inCheckBegin(br, "bag_model");
		
		int version = InOut.inInteger(br);
		if(version != 1) {
			throw new IllegalArgumentException("bag_model version number mismatch, expected 1, got " + version);
		}
		
		// Input model name
		String modelName[] = InOut.inStringArray(br);
		cl.setModelName(modelName[0]);
		
		// Input feature names
		String attnames[] = InOut.inStringArray(br);
		cl.setFeatureNames(attnames);
		
		int numTrees = InOut.inInteger(br);
		int outputAttnum = InOut.inInteger(br);
		
		cl.setOutputAttnum(outputAttnum);
		
		// Output attribute's class names
		String labels[] = InOut.inStringArray(br);
		cl.setOutputValueNames(labels);
		
		// Create each of the individual decision trees
		DecisionTree array[] = new DecisionTree[numTrees];
		for(int i=0; i<numTrees; i++) {
			array[i] = createDecisionTree(br);
		}
		
		cl.setClassifierArray(array);
		
		InOut.inCheckEnd(br, "bag_model");
		
		return cl;
	}
	
	/*
	 * Load in a decision tree from the model file
	 */
	private static DecisionTree createDecisionTree(BufferedReader br) throws IOException {
		DecisionTree root = null;
		
		InOut.inCheckBegin(br, "decision_tree");
		
		root = DecisionTree.createDecisionNode(br);
		
		InOut.inCheckEnd(br, "decision_tree");
		
		return root;
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