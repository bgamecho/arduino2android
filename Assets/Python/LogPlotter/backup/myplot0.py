import numpy
import matplotlib.pyplot as plot
import scipy.signal

path = "/Users/borja/Documents/ResearchStay/MobileBIT/Logs/"
# myPath=path+"2014-05-20_10-18-27.txt"
myPath = path+"myECGraw.txt"

data = numpy.loadtxt(myPath, 'float')

#noisy sequence:
nSeq = data[:,1]

#### LOW PASS FILTER #####

a = [1, -2, 1]
b = [1, 0, 0, 0, 0, -2, 0, 0, 0, 0, 1]
xVal_p = scipy.signal.lfilter(b,[1],data[:,1],-1,None)
yVal_p = scipy.signal.lfilter(b,a,data[:,1],-1,None)

# Real time

xSeq = data[0:500,1]
xVal_r = [xSeq[x] for x in range(0, 5, 1)]
xVal_r.extend([xSeq[x] - 2*xSeq[x-5] for x in range(5, 10, 1)])

#auxSeq5 = data[5:505,1]
#auxSeq10 = data[10:510,1]
#xVal_r.extend(auxSeq10 - (2*auxSeq5) + xSeq)

for x in range(10, 500):
    aux = (2*xSeq[x-5]) - xSeq[x-10]
    xVal_r.insert(x, xSeq[x] - aux)

yVal_r = [xVal_r[0]]
aux = (2*yVal_r[0]) + xVal_r[1]
yVal_r.insert(1, aux )

for x in range(2, 500):
    aux = (2*yVal_r[x-1]) - yVal_r[x-2]
    yVal_r.insert(x,(aux + xVal_r[x]))

##### PLOTTING ####
plot.figure()
plot.subplot(2,1,1)
plot.plot(xVal_p[0:500])
plot.subplot(2,1,1)
plot.plot(xVal_r[0:500])
plot.subplot(2,1,2)
plot.plot(yVal_p[0:500])
plot.subplot(2,1,2)
plot.plot(yVal_r[0:500])
plot.show()