#
# Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
# 
# OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
# 
# This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
# between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
# See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
#

#!/usr/bin/python
"""
preprocess a feature table (produced by featureBuilder.java) for use in model building and performance analysis.
 - strip out unused columns such as Manipulation.*, Alarm.*, etc.  (keep all Features.*)
 - convert multiple Classification.Source.* columns into a single source label column
"""
import sys

def preprocess( inputFile, outputFile ):
    print("going to process file '%s'" % inputFile)
    with open( inputFile, 'r') as fin,  open( outputFile, 'w' ) as fout:

        headers = fin.readline().strip().split(',')
        nHeaders = len(headers)
        print( "Number of header values = '%d'" % nHeaders )

        sourceClasses = {}
        indices = []
        newHeaders = []

        for idx, header in enumerate(headers):
            if header.startswith( 'Classification.Source' ):
                sourceClasses[idx] = header.replace('Classification.Source.','')
            else:
                if header.startswith('Features'):
                    indices.append(idx)
                    newHeaders.append(header)
                elif 'FoldID' in header:
                    indices.append(idx)
                    newHeaders.append(header)
                if header.startswith('Data'):
                    indices.append(idx)
                    newHeaders.append(header)

        newHeaders.append('LABEL\n')
        fout.write( ','.join( newHeaders ) )

        jdx = 1
        for line in fin:
            vals = line.strip().split(',')
            assert len(vals) == nHeaders, "Number of columns in row %d not consistent with headers!" % jdx

            valsToWrite = [vals[i] for i in indices]

            label = "Unlabeled"
            for column in sourceClasses:
                if vals[column] == '1':
                    label = sourceClasses[column]
            valsToWrite.append( label + '\n' )

            fout.write( ','.join(valsToWrite) )
            jdx += 1
            
if __name__ == '__main__':
    inputFile, outputFile = sys.argv[1:3]
    preprocess( inputFile, outputFile )



#
# Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
# 
# OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
# 
# This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
# between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
# See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
#