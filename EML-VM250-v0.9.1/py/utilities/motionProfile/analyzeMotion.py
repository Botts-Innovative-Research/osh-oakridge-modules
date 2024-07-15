#
# Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
# 
# OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
# 
# This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
# between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
# See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
#

import os
import sys
from subprocess import call
from ErnieAnalysis.utilities import javaclasspath

if len(sys.argv) != 3:
    print('Usage: python {} <path-to-csv-file-to-create> <path-to-pdf-plot-file-to-create>'.format(sys.argv[0]))
    sys.exit(1)

call( ['java','-cp',javaclasspath(),'gov.llnl.ernie.tools.MotionProfileHistogramGenerator'] + sys.argv[1] )
os.system('R --vanilla --slave --no-restore --args ' + sys.argv[1] + " " + sys.argv[2] + ' < motion-hist.R')


#
# Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
# 
# OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
# 
# This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
# between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
# See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
#