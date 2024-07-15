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
preprocess a feature table (produced by featureBuilder.java) for use in building 2nd-source classifier.
Similar to preprocess.py, except it also strips out all features other than Features.Joint.* and Features.Vehicle.*
"""

# operates on output of preprocess.py, tosses out all features except Joint and Vehicle
import sys

def preprocessJoint( inputFile, outputFile ):
    print("going to process file '%s'" % inputFile)
    with open( inputFile, 'r') as fin,  open( outputFile, 'w' ) as fout:

        headers = fin.readline().strip().split(',')
        nHeaders = len(headers)
        print( "Number of header values = '%d'" % nHeaders )

        sourceClasses = {}
        indices = []
        newHeaders = []

        for idx, header in enumerate(headers):
            if header in ('SegmentInfo.FoldID','LABEL'):
                indices.append(idx)
                newHeaders.append(header)
            elif header.startswith('Features.Joint') or header.startswith('Features.Vehicle'):
                indices.append(idx)
                newHeaders.append(header)

        fout.write( ','.join( newHeaders ) )
        fout.write('\n')

        jdx = 1
        for line in fin:
            vals = line.strip().split(',')
            assert len(vals) == nHeaders, "Number of columns in row %d not consistent with headers!" % jdx

            valsToWrite = [vals[i] for i in indices]

            fout.write( ','.join(valsToWrite) )
            fout.write('\n')
            jdx += 1
            
if __name__ == '__main__':
    inputFile, outputFile = sys.argv[1:3]
    preprocessJoint( inputFile, outputFile )



#
# Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
# 
# OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
# 
# This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
# between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
# See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
#