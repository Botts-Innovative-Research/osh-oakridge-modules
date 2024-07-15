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
Run CMU executable to generate two random forest classifiers
 - one using all available features
 - second using only joint + vehicle features

Once models are built, use them to make predictions on the test set (for later use in ROC generation)
"""
import os
import sys
import time
import shutil
import argparse
import inspect
parent_dir = os.path.dirname(
    os.path.dirname(os.path.abspath(inspect.getfile(inspect.currentframe())))
)
sys.path.insert(0,parent_dir)

from utilities import preprocess, preprocessJoint, score, utilities

def print(*args, **kwargs):
    import builtins
    if 'flush' not in kwargs: kwargs['flush'] = True
    builtins.print(*args, **kwargs)

def model_builder( projectDir, modelName=None, skipSSL=False):
    projectDir = os.path.abspath( projectDir )
    workDir = os.path.join( projectDir, 'modelBuilding' )

    if not modelName:   # use project directory name as model name
        modelName = os.path.split( projectDir )[-1]

    binDir = os.path.join(utilities.projErnie4Dir(), "bin" )

    originalDir = os.path.abspath('.')
    os.chdir( workDir )

    cmuExePath = ""
    if "nt" in os.name:
        cmuExePath = os.path.abspath(binDir+'/llnl_canes_ml_core.exe')
    else:
        cmuExePath = os.path.abspath(binDir+'/llnl_canes_ml_core')

    # define some paths_dict:
    paths_dict = {
        'cmuExe': cmuExePath,
        'SSL': 'option SSL',
        'CV': os.path.abspath('CV/folds1-10_processed.csv'),
        'modelName': modelName,
        'allmodel': os.path.abspath('SSL_allmodel.txt'),
        }

    foldList = [11, 20, 32, 33, 34, 42, 43, 44, 50, 52, 53, 54]
    for fold in foldList:
        paths_dict[ 'fold%d' % fold ] = os.path.abspath('fold%d/fold%d_processed.csv' % (fold,fold))

    if skipSSL:
        paths_dict['SSL'] = ''
        paths_dict['allmodel'] = os.path.abspath('allmodel.txt')

    os.chdir( 'allSSL' )
    start = time.time()
    print("Building primary classifier with SSL\n")
    cmd = ("{cmuExe} ds {CV} testds {fold11} {SSL} valAttnum SegmentInfo.FoldID \
            valds {fold20} save {allmodel} model_name {modelName}".format(
                **paths_dict ) )
    status = os.system( cmd )
    if status != 0:
        print("  ERROR: failure while building primary classifier!")
        sys.exit(-1)

    print("\nDone building primary classifier. Elapsed time: %.2f s\n" % (time.time() - start))

    # Make predictions using classifiers both before and after SSL:
    missingFolds = []
    for foldId in foldList[1:]:
        predictionFile = 'iteration_1_%d_predictions.csv' % foldId
        if not os.path.exists(predictionFile):  # newer llnl_canes executable doesn't automatically generate
            paths_dict['foldFile'] = paths_dict['fold%d' % foldId]

            if not os.path.exists( paths_dict['foldFile'] ):
                print("  WARNING: could not find fold%s for scoring!" % foldId)
                missingFolds.append(foldId)
                continue

            os.system("{cmuExe} option predict testds {foldFile} load iteration1.txt \
                    output_prefix iteration_1_{foldId}_".format(foldId=foldId, **paths_dict))
        score.score(predictionFile)

    for missing in missingFolds:
        foldList.remove(missing)

    os.chdir('..')

    # Make predictions using SSL model:
    for fold in ['fold%d' % f for f in foldList[1:]] + ['CV']:
        paths_dict['prefix'] = os.path.join(fold, 'final_')
        paths_dict['foldFile'] = paths_dict[fold]
        os.system("{cmuExe} option predict testds {foldFile} load {allmodel} \
                output_prefix {prefix}".format(**paths_dict))

    for foldId in foldList[1:]:
        score.score('fold%d/final_predictions.csv' % foldId)


    os.chdir( os.path.join(projectDir,'finalModel') )
    shutil.copy( os.path.join(workDir,paths_dict['allmodel']), '.' )


    # Save md5 hash of each model
    def md5(filename):
        import hashlib
        hash_md5 = hashlib.md5()
        with open(filename, "rb") as f:
            for chunk in iter(lambda: f.read(4096), b""):
                hash_md5.update(chunk)
        return hash_md5.hexdigest()

    with open('models_md5.txt','w') as fout:
        fout.write( str(md5(os.path.split(paths_dict['allmodel'])[-1])) + '\n' )

    os.chdir(originalDir)


if __name__ == '__main__':
    parser = argparse.ArgumentParser(
        description="Run CMU tools to generate Random Forest classifiers",
        usage="python model_builder.py [-h] project_dir [-m MODELNAME] [--skip-ssl]"
    )
    parser = argparse.ArgumentParser()
    parser.add_argument("project", help="path to project directory")
    parser.add_argument("-m", "--modelName", help="Name to give the machine-learning model")
    parser.add_argument("--skipssl", dest="skipSSL", action="store_true", help="skip SSL training")

    args = parser.parse_args()
    model_builder( args.project, args.modelName, args.skipSSL)



#
# Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
# 
# OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
# 
# This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
# between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
# See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
#