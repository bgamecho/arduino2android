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
    
    errorFile = rootDir+"errors.txt"
    errorData = numpy.genfromtxt(errorFile, dtype='str')
    errorTimestamps = errorData[:,0].astype(numpy.long)
    errors = errorData[:,1]

    #t0 = min([pingTimestamps[0], batteryTimestamps[0], cpuTimestamps[0], eventTimestamps[0], errorTimestamps[0]]).astype(numpy.long)
    t0 = min([pingTimestamps[0]]).astype(numpy.long)
    tn = max([pingTimestamps[-1], batteryTimestamps[-1], cpuTimestamps[-1], eventTimestamps[-1], errorTimestamps[-1]]).astype(numpy.long)
    print "t: "+str(t0)+" tn: "+str(tn)
    

    #Calculate Throughput at each time interval
    #pairIter = pairwise(pingTimestamps)
    #throughput = [(12.0/(pair[1]-pair[0]).astype(numpy.float)*1000) for pair in pairIter]

    throughputData = []
    i = 1
    t = pingData[0][0].astype(numpy.long)
    s = 0
    while (i < len(pingData)):
        ti = pingData[i][0].astype(numpy.long)
        diff = (ti-t).astype(numpy.float)
        s += pingData[i][2].astype(numpy.long)
        if diff > 1000:
            th = s/diff*1000
            throughputData.append([ti, th])
            t = ti
            s = 0
        i += 1
    #throughputTimestamps = throughputData[:,0]
    throughputTimestamps = [row[0] for row in throughputData]
    rawThroughput = [row[1] for row in throughputData]
    throughput = interp1d(throughputTimestamps,rawThroughput)
    
    
    
    
    plt.figure(1)
    plt.xticks( rotation=25 )
    
    plt.subplot(311)
    plt.title("Ping signals")
    plt.plot(pingTimestamps, ping, color="green", linestyle="-", label="ping")
    plt.xlim(t0-5000, tn+5000)
    plt.ylim(0, 200)
    plt.ylabel('Ping time')
    plt.xlabel('time (miliseconds)')
    plt.legend(loc='best')
    
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
    plt.legend(loc='best')
    
    plt.subplot(313)
    plt.plot(throughputTimestamps, throughput, color="blue", linestyle="-", label="throughput")
    plt.xlim(t0-5000, tn+5000)
    plt.ylabel('Throughput')
    plt.xlabel('time (miliseconds)')
    plt.legend(loc='best')
    
    plt.show()

#    plt.annotate(event,
#         xy=(eventTimestamp,42), xycoords='data', rotation=90,
#         xytext=(-10, +10), textcoords='offset points', fontsize=10,
#         arrowprops=dict(arrowstyle="->", connectionstyle="arc3,rad=.2"))


def pairwise(iterable):
    "s -> (s0,s1), (s1,s2), (s2, s3), ..."
    a, b = itertools.tee(iterable)
    next(b, None)
    return itertools.izip(a, b)


if __name__ == '__main__':
    main()

#while 1:
#    print 'ENTER cmd:'
#    if sys.stdin.readline().strip() == 's':
#        print 'sending command S'
#    else:
#        print 'err'