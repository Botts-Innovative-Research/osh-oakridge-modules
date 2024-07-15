#
# Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
# 
# OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
# 
# This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
# between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
# See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
#

import numpy as np
import matplotlib.pyplot as plt
import scipy.optimize
from scipy.interpolate import interp1d

t=[-2, 0, 3, 8]
x=[-4, 0, 7, 16] 
d2t=interp1d(x, t, kind='cubic', fill_value="extrapolate")
bq1=[[0,1],[4,0],[5,1],[8,0],[9,1],[9.4,0],[13,1],[15,0]]
bq2=[[-1.2,1],[-1.0,0], [0,1],[4,0],[5,1],[8,0],[13,1],[15,0]]

b1 = np.array([ [d2t(i),j] for i,j in bq1 ])
b2 = np.array([ [d2t(i-0.85),j] for i,j in bq2 ])
#b2=np.array([[0,1],[3,0]])

def g(x):
    return 1/(1+np.exp(-x))

def compute(phi, b1, b2, d):
    x=np.arange(-30,200,2)
    u=0
    v0=0
    v1=0
    v2=0
    for i in range(b1.shape[0]):
        t0 = b1[i,0]
        d0 = 10*(phi[2]*t0**3+phi[1]*t0**2+phi[0]*t0)
        a = (2*b1[i,1]-1)
        e = g(x-d0)
        u += a*e
        v0 += a*e*(1-e)*(-10*t0)
        v1 += a*e*(1-e)*(-10*t0*t0)
        v2 += a*e*(1-e)*(-10*t0*t0*t0)
    for i in range(b2.shape[0]):
        t0 = b2[i,0]
        d0 = 10*(phi[2]*t0**3+phi[1]*t0**2+phi[0]*t0+d)
        a = (1-2*b2[i,1])
        e = g(x-d0)
        u += a*e
        v0 += a*e*(1-e)*(-10*t0)
        v1 += a*e*(1-e)*(-10*t0*t0)
        v2 += a*e*(1-e)*(-10*t0*t0*t0)
    J = np.dot(u,u)/len(x)+1*(phi[2]**2)
    g0 = 2*np.dot(u,v0)/len(x)
    g1 = 2*np.dot(u,v1)/len(x)
    g2 = 2*np.dot(u,v2)/len(x)+1*phi[2]
    grad = np.array((g0,g1,g2))
    return (J, grad)


def plotT(b1,b2):
    x=np.arange(-3,20,0.1)
    u0 = 0
    u1 = 0
    for i in range(b1.shape[0]):
        t0 = b1[i,0]
        u0 += (2*b1[i,1]-1)*(x>t0)
    for i in range(b2.shape[0]):
        t0 = b2[i,0]
        u1 += (2*b2[i,1]-1)*(x>t0)
    plt.plot(u0)
    plt.plot(u1+1.1)


def plotD(phi, b1,b2,d):
    x=np.arange(-30,200,0.1)
    u0 = 0
    u1 = 0
    for i in range(b1.shape[0]):
        t0 = b1[i,0]
        d0 = 10*(phi[2]*t0**3+phi[1]*t0*t0+phi[0]*t0)
        u0 += (2*b1[i,1]-1)*(x>d0)
    for i in range(b2.shape[0]):
        t0 = b2[i,0]
        d0 = 10*(phi[2]*t0**3+phi[1]*t0*t0+phi[0]*t0+d)
        u1 += (2*b2[i,1]-1)*(x>d0)
    plt.plot(u0)
    plt.plot(u1+1.1)

#plotT(b1,b2)

result = scipy.optimize.minimize(compute, np.array((2,0,0)), args=(b1,b2,0.85),method="TNC", jac=True)
m=result.x
print(result)
plt.figure()
plotD(result.x, b1,b2,0.85)
plt.figure()
t=np.arange(-2,10,0.1)
plt.plot(t,t*m[0]+t*t*m[1]+t*t*t*m[2],label='computed')
d=np.arange(-4,20,0.1)
plt.plot(d2t(d),d,label='truth')
plt.legend()
plt.show()

#   u1 = 0

print(compute((1,0,0),b1,b2,0.85))
#print((compute((0+0.01,1),b1,b2,0.85)-compute((0-0.01,1),b1,b2,0.85))/0.02)
#print((compute((0,1+0.01),b1,b2,0.85)-compute((0,1-0.01),b1,b2,0.85))/0.02)
#print(computeGrad((0,1),b1,b2,0.85))

#plotD((2,0,0), b1,b2,0.85)



#
# Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
# 
# OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
# 
# This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
# between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
# See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
#