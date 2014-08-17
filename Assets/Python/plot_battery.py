# -*- coding: utf-8 -*-
"""
Created on Fri Jul 18 15:41:18 2014

@author: Sensores
"""

import sys
import time
from time import sleep
import datetime as dt
import matplotlib.pyplot as plt
import matplotlib.dates as md
from scipy.interpolate import interp1d
from scipy import interpolate
import operator
import numpy
import serial
import itertools
import matplotlib.dates as mdates


def main():
    rootDir = "C:/Documents and Settings/Sensores/Mis documentos/Dropbox/PFG/git/arduino2android/Assets/Python/test_logs/test_log7/"
    
    
    batteryFile = rootDir+"battery.txt"
    batteryData = numpy.genfromtxt(batteryFile,dtype='str')
    batteryTimestamps = batteryData[:,0].astype(numpy.long)
    battery = batteryData[:,1].astype(numpy.float)
    
    cpuFile = rootDir+"cpu.txt"
    cpuData = numpy.genfromtxt(cpuFile,dtype='str')
    print cpuData[0]
    cpuTimestamps = cpuData[:,0].astype(numpy.long)
    cpu = cpuData[:,1].astype(numpy.float)
    
    t0 = min([cpuTimestamps[0], batteryTimestamps[0]]).astype(numpy.long)
    tn = max([cpuTimestamps[-1], batteryTimestamps[-1]]).astype(numpy.long)


    plt.figure(1)
    plt.subplot(211)    
    plt.title("Battery")
    plt.plot(batteryTimestamps, battery, color="blue", linestyle="-", marker="o", label="Battery %")
    
    plt.xlim(t0-5000, tn+5000)
    plt.ylabel('Battery %')
    plt.xlabel('time (miliseconds)')
    plt.legend(loc='upper left')
    plt.grid(True)
    
    
    plt.subplot(212)
    plt.title("CPU")
    plt.plot(cpuTimestamps,cpu, color="green", linestyle="-", label="CPU")
    plt.plot(smooth(cpuTimestamps),smooth(cpu), color="black", linestyle="-", label="smooth-CPU")
    
    plt.xlim(t0-5000, tn+5000)
    plt.ylabel('CPU')
    plt.xlabel('time (miliseconds)')
    plt.legend(loc='lower left')
    plt.grid(True)
    
    plt.show()




def sortByColumn(bigList, *args):
    bigList.sort(key=operator.itemgetter(*args)) # sorts the list in place

def smooth(x,window_len=11,window='hanning'):
    """smooth the data using a window with requested size.
    
    Taken from http://wiki.scipy.org/Cookbook/SignalSmooth
    
    This method is based on the convolution of a scaled window with the signal.
    The signal is prepared by introducing reflected copies of the signal 
    (with the window size) in both ends so that transient parts are minimized
    in the begining and end part of the output signal.
    
    input:
        x: the input signal 
        window_len: the dimension of the smoothing window; should be an odd integer
        window: the type of window from 'flat', 'hanning', 'hamming', 'bartlett', 'blackman'
            flat window will produce a moving average smoothing.

    output:
        the smoothed signal
        
    example:

    t=linspace(-2,2,0.1)
    x=sin(t)+randn(len(t))*0.1
    y=smooth(x)
    
    see also: 
    
    numpy.hanning, numpy.hamming, numpy.bartlett, numpy.blackman, numpy.convolve
    scipy.signal.lfilter
 
    TODO: the window parameter could be the window itself if an array instead of a string
    NOTE: length(output) != length(input), to correct this: return y[(window_len/2-1):-(window_len/2)] instead of just y.
    """ 
     
    if x.ndim != 1:
        raise ValueError, "smooth only accepts 1 dimension arrays."

    if x.size < window_len:
        raise ValueError, "Input vector needs to be bigger than window size."
        

    if window_len<3:
        return x
    
    
    if not window in ['flat', 'hanning', 'hamming', 'bartlett', 'blackman']:
        raise ValueError, "Window is on of 'flat', 'hanning', 'hamming', 'bartlett', 'blackman'"
    

    s=numpy.r_[x[window_len-1:0:-1],x,x[-1:-window_len:-1]]
    #print(len(s))
    if window == 'flat': #moving average
        w=numpy.ones(window_len,'d')
    else:
        w=eval('numpy.'+window+'(window_len)')
    
    y=numpy.convolve(w/w.sum(),s,mode='valid')
    return y    


if __name__ == '__main__':
    main()

#while 1:
#    print 'ENTER cmd:'
#    if sys.stdin.readline().strip() == 's':
#        print 'sending command S'
#    else:
#        print 'err'