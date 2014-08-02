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


def main():
    rootDir = "C:/Documents and Settings/Sensores/Mis documentos/Dropbox/PFG/git/arduino2android/Assets/Python/logsXabi/"
    
    pingFile = rootDir+"ping.txt"
    pingData = numpy.genfromtxt(pingFile,dtype='str')
    pingTimestamps = pingData[:,0].astype(numpy.long)
    ping = pingData[:,3].astype(numpy.long)
    
    batteryFile = rootDir+"battery.txt"
    batteryData = numpy.genfromtxt(batteryFile,dtype='str')
    batteryTimestamps = batteryData[:,0].astype(numpy.long)
    battery = batteryData[:,1].astype(numpy.float)
    
    cpuFile = rootDir+"cpu.txt"
    cpuData = numpy.genfromtxt(cpuFile,dtype='str')
    cpuTimestamps = cpuData[:,0].astype(numpy.long)
    cpu = cpuData[:,1].astype(numpy.float)
    
    eventFile = rootDir+"events.txt"
    eventData = numpy.genfromtxt(eventFile, dtype='str')
    eventTimestamps = eventData[:,0].astype(numpy.long)
    events = eventData[:,1]
    
    errorFile = rootDir+"error.txt"
    errorData = numpy.genfromtxt(errorFile, dtype='str')
    errorTimestamps = errorData[:,0].astype(numpy.long)
    errors = errorData[:,1]
    
    stressFile = rootDir+"data.txt"
    stressData = numpy.genfromtxt(stressFile, dtype='str')

    #t0 = min([pingTimestamps[0], batteryTimestamps[0], cpuTimestamps[0], eventTimestamps[0], errorTimestamps[0]]).astype(numpy.long)
    t0 = min([pingTimestamps[0]]).astype(numpy.long)
    tn = max([pingTimestamps[-1], batteryTimestamps[-1], cpuTimestamps[-1], eventTimestamps[-1], errorTimestamps[-1]]).astype(numpy.long)
    n = len(pingTimestamps)
   

    #Calculate Throughput at each time interval
    #pairIter = pairwise(pingTimestamps)
    #throughput = [(12.0/(pair[1]-pair[0]).astype(numpy.float)*1000) for pair in pairIter]
    pingSize = [[p[0],p[2]] for p in pingData]
    stressSize = [[p[0],p[2]] for p in stressData]
    
    allData = numpy.concatenate((pingSize, stressSize),0)
    throughputData = sorted(allData, key=operator.itemgetter(0), reverse=False)

    throughputChew = []
    i = 1
    t = throughputData[0][0].astype(numpy.long)
    s = 0
    while (i < len(throughputData)):
        ti = throughputData[i][0].astype(numpy.long)
        diff = (ti-t).astype(numpy.float)
        s += throughputData[i][1].astype(numpy.long)
        if diff >= 1500:
            th = s/diff*1000
            throughputChew.append([ti, th])
            t = ti
            s = 0
        i += 1
    throughputTimestamps = [row[0] for row in throughputChew]
    rawThroughput = [row[1] for row in throughputChew]
    
    tck = interpolate.splrep(throughputTimestamps, rawThroughput, s=0)
    xthroughput = numpy.arange(throughputTimestamps[0],throughputTimestamps[-1],len(throughputTimestamps))
    throughput_smooth = interpolate.splev(xthroughput,tck,der=0)
    
    
    plt.figure(1)
    plt.xticks( rotation=25 )
    
    plt.subplot(311)
    plt.title("Ping signals")
    plt.plot(pingTimestamps, ping, color="green", linestyle="-", label="ping")
    plt.xlim(t0-5000, tn+5000)
    plt.ylim(0, 200)
    plt.ylabel('Ping time')
    plt.xlabel('time (miliseconds)')
    plt.legend(loc='upper left')
    
    #Vertical line 1406908300916 ROBOTICA_9 12 42
    for line in eventData:
        eventTimestamp = line[0]
        event = line[1]
        plt.plot([eventTimestamp,eventTimestamp],[0,200], color ='red', linewidth=1.5, linestyle="--")
    
        plt.annotate(event,
             xy=(eventTimestamp,42), xycoords='data', rotation=90,
             xytext=(-10, +10), textcoords='offset points', fontsize=10)

    plt.subplot(312)    
    plt.title("Battery and CPU")
    plt.plot(batteryTimestamps, battery, color="blue", linestyle="-", label="battery")
    plt.plot(cpuTimestamps,cpu, color="black", linestyle="-", label="CPU")
    
    plt.xlim(t0-5000, tn+5000)
    plt.ylim(cpu.min()*0.9, cpu.max()*1.1)
    plt.ylabel('CPU')
    plt.xlabel('time (miliseconds)')
    plt.legend(loc='lower left')
    
    plt.subplot(313)
    plt.plot(xthroughput, throughput_smooth, color="blue", linestyle="-", label="throughput")
    plt.xlim(t0-5000, tn+5000)
    plt.ylabel('Throughput')
    plt.xlabel('time (miliseconds)')
    plt.legend(loc='upper left')
    
    plt.show()

#    plt.annotate(event,
#         xy=(eventTimestamp,42), xycoords='data', rotation=90,
#         xytext=(-10, +10), textcoords='offset points', fontsize=10,
#         arrowprops=dict(arrowstyle="->", connectionstyle="arc3,rad=.2"))


#def pairwise(iterable):
#    "s -> (s0,s1), (s1,s2), (s2, s3), ..."
#    a, b = itertools.tee(iterable)
#    next(b, None)
#    return itertools.izip(a, b)




def sortByColumn(bigList, *args):
    bigList.sort(key=operator.itemgetter(*args)) # sorts the list in place

            

if __name__ == '__main__':
    main()

#while 1:
#    print 'ENTER cmd:'
#    if sys.stdin.readline().strip() == 's':
#        print 'sending command S'
#    else:
#        print 'err'