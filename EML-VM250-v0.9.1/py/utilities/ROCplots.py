#
# Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
# 
# OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
# 
# This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
# between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
# See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
#

import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt
import pandas
import os

labels = {32: 'medical', 33: 'industrial', 34 : 'fissile', 
          42 : 'medical', 43 : 'industrial', 44 : 'fissile'}

def errorfill(roc, color=None, label=None, alpha_fill=0.1, ax=None):
    """
    Plot line with confidence bands
    """
    ax = ax if ax is not None else plt.gca()
    if color is None:
        color = ax._get_lines.color_cycle.next()
    ax.plot(roc[:,0], roc[:,1], color=color, label=label)
    ax.fill_between(roc[:,0], roc[:,3], roc[:,5], color=color, alpha=alpha_fill)
    ax.fill_betweenx(roc[:,1], roc[:,2], roc[:,4], color=color, alpha=alpha_fill)


def add_RPM_Alarms(ax, fold, alarms, drawCurves = False):
    """
    Draw points and optionally curves for ROS, SOR and nSigma
    """
    ROSplotted = False
    SORplotted = False
    nSigmaPlotted = False

    for i in range(len(alarms)):
    
        fname = fold + "_alarm_point" + str(i+4) + ".csv"

        filename = "A" + str(i+4) + "_roc_std.csv"
        roc = pandas.read_csv(filename).values
        name = alarms[i]
        color = 'Brown'
        #color = '#196f3d' # Dark green
        rslabel = ""
        if name == "SOR":
            continue    # only plotting ROS for now
            #color = '#e67e22' # Orange
            if SORplotted == False:
                rslabel = "SOR"
            SORplotted = True
        elif name == "ROS":
            if ROSplotted == False:
                rslabel = "ROS"
            ROSplotted = True
        elif name == "NSigma":
            if nSigmaPlotted == False:
                rslabel = "NSigma"
            nSigmaPlotted = True

        point = pandas.read_csv(fname).values
        if drawCurves:
            errorfill(roc, color, rslabel)
            ax.plot(point[:,1], point[:,0], color=color, marker='o')
        else:
            # ensure label appears in the legend
            ax.plot(point[:,1], point[:,0], color=color, marker='o', label=rslabel)


def plotROC1( fold, *alarms, mandatoryInspectionRate=None, outputFile="test.png", drawCurves=False ):
    """
    Simple ROC plot showing full ERNIE curve + old alarm points.
    If drawCurves = True, draw curves as well as points for old alarm algorithm.
    """
    label = labels[fold]
    fold = str(fold)
    num_settings = len(alarms)
    fig, ax = plt.subplots(figsize=(6.4,6.4))

    roc1 = pandas.read_csv('A1_roc_std.csv').values

    errorfill(roc1, 'Blue', 'ERNIE')

    add_RPM_Alarms(ax, fold, alarms, drawCurves = drawCurves)

    if mandatoryInspectionRate:
        plt.axvline(x=mandatoryInspectionRate, color='Red', label='Mandatory Inspections')

    ax.plot(roc1[:,6], roc1[:,7], color='Black', label='Chance') # Random line

    ax.set_xlabel('Fraction of vehicles alarming')
    ylabel = 'Fraction of ' + label + ' threats detected'

    ax.set_ylabel(ylabel)
    ax.set_xscale('log')

    leg = ax.legend(loc=4, fontsize='small', markerscale=0.)

    plt.grid(True)

    plt.xlim([1E-4,1])
    plt.ylim([0,1])
    plt.title(label.title() + " test set")

    plt.savefig(outputFile)


def plotROC2( fold, *alarms, mandatoryInspectionRate = None, outputFile = "test.png", secondSource = True, drawCurves = False ):
    """
    Detailed ROC plot showing ERNIE results before/after SSL, old alarm points and curves,
    impact of 2nd-source logic, etc.
    If drawCurves = True, draw curves as well as points for old alarm algorithm.
    """
    label = labels[fold]
    fold = str(fold)
    
    num_settings = len(alarms)
    fig, ax = plt.subplots(figsize=(6.4,6.4))

    roc1 = pandas.read_csv('A1_roc_std.csv').values
    roc2 = pandas.read_csv('A2_roc_std.csv').values
    roc3 = pandas.read_csv('A3_roc_std.csv').values

    # SSL scores
    SSLth = pandas.read_csv(fold + 'SSL_th.csv').values

    # Plot SSL line
    Scores = SSLth[:,2]
    for i in range(len(Scores)):
        plt.text(SSLth[i,1]*1.1, SSLth[i,0]-0.02, '%.3f' % Scores[i], color='Blue', fontsize='small')
    ax.plot(SSLth[:,1], SSLth[:,0], 'bo')

    errorfill(roc2, 'Magenta', 'ERNIE(no SSL)')
    if secondSource:
        errorfill(roc1, 'Blue', 'ERNIE(2-source)')
        errorfill(roc3, 'Brown', 'ERNIE(SSL)')
    else:
        errorfill(roc1, 'Blue', 'ERNIE(SSL)')

    add_RPM_Alarms(ax, fold, alarms, drawCurves=drawCurves)

    if mandatoryInspectionRate:
        plt.axvline(x=mandatoryInspectionRate, color='Red', label='Mandatory Inspections')

    ax.plot(roc1[:,6], roc1[:,7], color='Black', label='Chance') # Random line

    ax.set_xlabel('Fraction of vehicles alarming')
    ylabel = 'Fraction of ' + label + ' threats detected'

    ax.set_ylabel(ylabel)
    ax.set_xscale('log')

    leg = ax.legend(loc=4, fontsize='small')

    plt.grid(True)

    plt.xlim([1E-4,1])
    plt.ylim([0,1])
    plt.title(label.title() + " test set")

    plt.savefig(outputFile)



#
# Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
# 
# OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
# 
# This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
# between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
# See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
#