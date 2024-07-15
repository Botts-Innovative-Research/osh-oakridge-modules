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
import numpy
import pandas
import inspect
parent_dir = os.path.dirname(
    os.path.dirname(os.path.abspath(inspect.getfile(inspect.currentframe())))
)
sys.path.append(parent_dir)

from utilities import startJPype
import jpype.dbapi2 as dbapi2

def loadFolds( database, foldIds=None, verbose=False ):
    """
    Load data from H2 database into pandas dataframe.
    If foldIds is None, load all folds. Otherwise load specified folds
    """
    # remove extension if supplied:
    database = database.rstrip('.mv.db').rstrip('.h2.db')
    dbname = "jdbc:h2:./%s;DATABASE_TO_UPPER=FALSE" % database
    cnx = dbapi2.connect(dbname)

    tables = [
        'SegmentInfo',
        'Classification',
        'Alarm',
        'Background',
        'Motion',
        'Info_Manipulation',
        'Info_Manipulation2',
        'Standard',
        'Vehicle',
        'Statistics',
        'NSigma',
        'Extent',
        'Transforms'
    ]

    dataframes = {}
    for table in tables:
        query = "select * from %s" % table
        df = pandas.read_sql_query( query, cnx )
        for column in df:
            if df[column].dtype == bool: # codes later in pipeline prefer uint to bool
                df[column] = df[column].astype( numpy.uint8 )
        if verbose:
            print("%s: %s" % (table, df.shape))
        dataframes[table] = df


    # merge results and rename features:
    df = dataframes['SegmentInfo']
    df = df.add_prefix('SegmentInfo_').set_index("SegmentInfo_ID")
    for table in tables[1:]:
        dfnow = dataframes[table]
        dfnow = dfnow.add_prefix(table + '_').set_index(table + "_ID")
        if not len(dfnow): continue
        how = 'inner'
        if 'Manipulation' in table: how = 'left'
        df = pandas.merge(df, dfnow, left_index=True, right_index=True, how=how)
        if 'Manipulation' in table:
            df = df.fillna(0)
    df.rename(lambda x: x.replace('_','.'), axis='columns', inplace=True)

    # additional renames for comparing to older feature tables:
    renames = {}
    for column in df.columns:
        if column.startswith('Info.'):
            renames[column] = column.replace('Info.','')
        if (column.startswith('Statistics') or column.startswith('Transforms') or column.startswith('Extent')
                or column.startswith('NSigma') or column.startswith('Standard') or column.startswith('Vehicle')):
            renames[column] = 'Features.' + column

    renames['SegementInfo.Location.IsSecondary'] = 'SegmentInfo.Location.Secondary'
    renames['Manipulation.velocityChanged'] = 'Manipulation.VelocityChanged'

    df.rename(columns=renames, inplace=True)

    if foldIds:
        df = df.loc[ df['SegmentInfo.FoldID'].isin( foldIds ) ]

    if verbose:
        print( df.groupby("SegmentInfo.FoldID")["SegmentInfo.FoldID"].count() )

    df = df.replace(
        ['Infinity','-Infinity','inf','-inf',numpy.inf,numpy.Inf,numpy.Infinity,numpy.NINF],
        numpy.nan
    )
    badRows = df.isnull().sum(axis=1) > 0
    df2 = df.loc[ badRows ]
    df = df.dropna()
    if any(badRows):
        print( "WARNING: detected %d rows with NaN or infinity in fold(s) %s!" %
                (badRows.sum(), sorted(df2['SegmentInfo.FoldID'].unique()) ) )

    cnx.close()

    return df, df2


if __name__ == '__main__':
    df, badRecs = loadFolds( sys.argv[1], sys.argv[2:], verbose=True )


#
# Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
# 
# OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
# 
# This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
# between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
# See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
#