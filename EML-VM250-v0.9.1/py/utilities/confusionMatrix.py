#
# Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
# 
# OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
# 
# This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
# between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
# See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
#

"""
Generate confusion matrix comparing labels to predictions.

Input: one or more 'final_predictions.csv' files produced during the model building step.
Output: (up to) 6-class confusion matrix, written to csv.
The matrix may contain fewer than 6 classes if fewer showed up in labels/predictions.
"""
import os
import sys
import pandas

def confusionMatrix(predictionFiles, outputfile="confusionMatrix.csv"):

    # merge all prediction files into single DataFrame:
    df = pandas.read_csv(predictionFiles[0])
    for filename in predictionFiles[1:]:
        df = pandas.concat([df, pandas.read_csv(filename)], ignore_index=True)

    # compare labels to predictions to get confusion matrix:
    groundTruth = df['true_output']
    groundTruth.name = ''
    raw_data = df.drop('true_output', axis=1)
    predictions = raw_data.idxmax(axis=1)
    
    confusion = pandas.crosstab(groundTruth, predictions)
    confusion.to_csv(outputfile)

if __name__ == '__main__':
    confusionMatrix(sys.argv[1:])



#
# Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
# 
# OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
# 
# This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
# between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
# See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
#