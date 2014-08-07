# -*- coding: utf-8 -*-
"""
Created on Fri Jul 18 15:41:18 2014

@author: Sensores
"""

import matplotlib.pyplot as plt
import matplotlib.dates as md
from scipy.interpolate import interp1d
from scipy import interpolate
import operator
import numpy
import numpy.ma as ma


def main():
    rootDir = "C:/Documents and Settings/Sensores/Mis documentos/Dropbox/PFG/git/arduino2android/Assets/Python/test_logs/test_log5/"
    
    pingFile = rootDir+"ping.txt"
    pingData = numpy.genfromtxt(pingFile,dtype='str')
    pingTimestamps = pingData[:,0].astype(numpy.long)
    ping = pingData[:,3].astype(numpy.long)
    
    pingById = dict()
    for line in pingData:
        robId = line[1]
        if robId in pingById:
            aux = pingById[robId]
            aux = numpy.vstack((aux,line))
            pingById[robId]=aux
        else:
            pingById[robId]=line
    pingById
    
    
    eventFile = rootDir+"events.txt"
    eventData = numpy.genfromtxt(eventFile, dtype='str')
    eventTimestamps = eventData[:,0].astype(numpy.long)
    events = eventData[:,1]

    #t0 = min([pingTimestamps[0], batteryTimestamps[0], cpuTimestamps[0], eventTimestamps[0], errorTimestamps[0]]).astype(numpy.long)
    t0 = min([pingTimestamps[0]]).astype(numpy.long)
    tn = max([pingTimestamps[-1], eventTimestamps[-1]]).astype(numpy.long)
    n = len(pingTimestamps)
   

    #Calculate Throughput at each time interval
    #pairIter = pairwise(pingTimestamps)
    #throughput = [(12.0/(pair[1]-pair[0]).astype(numpy.float)*1000) for pair in pairIter]
    pingSize = [[p[0],p[2]] for p in pingData]
    
    allData = pingSize
    throughputData = sorted(allData, key=operator.itemgetter(0), reverse=False)

    throughputChew = []
    i = 1
    t = throughputData[0][0].astype(numpy.long)
    s = 0
    while (i < len(throughputData)):
        ti = throughputData[i][0].astype(numpy.long)
        diff = (ti-t).astype(numpy.float)
        s += throughputData[i][1].astype(numpy.long)
        if diff >= 500:
            th = s/diff*1000
            throughputChew.append([ti, th])
            t = ti
            s = 0
        i += 1
    throughputTimestamps = [row[0] for row in throughputChew]
    rawThroughput = [row[1] for row in throughputChew]
    
#    tck = interpolate.splrep(throughputTimestamps, rawThroughput, s=0)
#    xthroughput = numpy.arange(throughputTimestamps[0],throughputTimestamps[-1],len(throughputTimestamps))
#    throughput_smooth = interpolate.splev(xthroughput,tck,der=0)
    
    
    plt.figure(1)
    plt.xticks( rotation=25 )
    
    plt.subplot(211)
    plt.title("Ping signals")

    for key in pingById.keys():
        pingTimestamps = pingById[key][:,0].astype(numpy.long)
        ping = pingById[key][:,3].astype(numpy.long)
        masked_ping = ma.array(ping)
        
        i=0
        while i<(len(pingTimestamps)-1):
            if (pingTimestamps[i+1]-pingTimestamps[i])>2000:
                masked_ping[i+1] = ma.masked
            i+=1
        
        #plt.plot(smooth(pingTimestamps), smooth(masked_ping), linestyle="-", label=key)
        plt.plot(pingTimestamps, masked_ping, linestyle="-", label=key)
        
    for line in eventData:
        eventTimestamp = line[0]
        event = line[1]
        
        if not "discovery" in event:
            plt.plot([eventTimestamp,eventTimestamp],[0,200], color ='red', linestyle="--")
            
            plt.annotate(event,
             xy=(eventTimestamp,42), xycoords='data', rotation=90,
             xytext=(-10, +10), textcoords='offset points', fontsize=10)
        else:
            plt.plot([eventTimestamp,eventTimestamp],[0,200], color ='blue', linestyle="--")

    plt.grid(True)
    plt.xlim(t0-5000, tn+5000)
    plt.ylabel('Ping time')
    plt.xlabel('time (miliseconds)')
    plt.legend(loc='upper left')

    
    plt.subplot(212)
    plt.grid(True)
    #plt.plot(xthroughput, throughput_smooth, color="blue", linestyle="-", label="throughput")
    #plt.plot(throughputTimestamps, rawThroughput, color="blue", linestyle="-", label="throughput")
    plt.plot(smooth(numpy.array(throughputTimestamps)), smooth(numpy.array(rawThroughput)), color="blue", linestyle="-", label="throughput")
    plt.xlim(t0-5000, tn+5000)
    plt.ylabel('Throughput')
    plt.xlabel('time (miliseconds)')
    plt.legend(loc='upper left')
    
    plt.show()


def smooth(x,window_len=30,window='hamming'):

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