import numpy
import matplotlib.pyplot as plot
import scipy.signal

path = "/Users/borja/Documents/ResearchStay/MobileBIT/Logs/"
# myPath=path+"2014-05-20_10-18-27.txt"
myPath = path+"myECGraw.txt"

data = numpy.loadtxt(myPath, 'float')

maxLen = 3000 # length of the data

#noisy sequence:
nSeq = data[:,1]

#### LOW PASS FILTER #####

a = [1, -2, 1]
b = [1, 0, 0,  -2,   0, 0, 1]
xVal_p = scipy.signal.lfilter(b,[1],data[:,1],-1,None)
yVal_p = scipy.signal.lfilter(b,a,data[:,1],-1,None)

# Real time

xSeq = data[0:maxLen,1]
xVal_r = [xSeq[x] for x in range(0, 5, 1)]
xVal_r.extend([xSeq[x] - 2*xSeq[x-5] for x in range(5, 10, 1)])

for x in range(10, maxLen):
    aux = (2*xSeq[x-5]) - xSeq[x-10]
    xVal_r.insert(x, xSeq[x] - aux)

yVal_r = [xVal_r[0]]
aux = (2*yVal_r[0]) + xVal_r[1]
yVal_r.insert(1, aux )

for x in range(2, maxLen):
    aux = (2*yVal_r[x-1]) - yVal_r[x-2]
    yVal_r.insert(x,(aux + xVal_r[x]))

##### SlopeSumFunction #####

yDelta = numpy.diff(yVal_p)
uDelta = []
for x in range(0, maxLen-1):
  if yDelta[x] > 0:
    uDelta.insert(x, yDelta[x])
  else:
    uDelta.insert(x, 0)
uDelta.insert(x, 0)
#print "uDelta len: " + str(len(uDelta))

w = 13
zVal=[]
for i in range(0, w+2):
  zVal.insert(i,0)

for i in range(w+1, maxLen):
  #print "i: "+ str(i)
  sum_k = 0
  for k in range(i-w-1,i):
    #print "k: "+ str(k)
    sum_k += uDelta[k]
    #print "sum: "+str(sum_k)
  zVal.insert(i,sum_k)

#print (zVal[maxLen])

### Decission Rule ####
##### Adaptive thresholding #####
num_frames = 200
t_base = 3 * numpy.mean(zVal[0:num_frames])
print "Threshold base: "+ str(t_base)
threshold = 0.6 * t_base
print "Threshold init: "+ str(0.6 * t_base)

result_x=[]
result_y=[]
result_y_0=[]
for i in range(50, maxLen):
  if zVal[i] > threshold :
    maxVal = max(zVal[i-40 : i+40])
    result_y.append(maxVal)
    #result_y_0.append(2)

    maxPoint = zVal[i-40:i+40].index(maxVal)
    print "i: "+str(i)+ " index: "+ str(zVal[i-40:i+40].index(maxVal))
    maxPoint = i -40 + maxPoint
    result_x.append(maxPoint)
    #minPoint = max(i-15, i+15)
    print str(i) + " heart beat detected at position: " + str(maxPoint) + " value: "+str(maxVal)

result_y_0=[]
for i in range(0, len(result_x)):
  result_y_0.append(nSeq[result_x[i]])



##### Local search #####

##### PLOTTING ####
plot.figure()
#plot.plot(nSeq[0:maxLen])
#plot.subplot(3,1,2)
#plot.plot(yVal_p[0:maxLen])
#plot.plot(zVal[0:maxLen])
plot.subplot(2,1,1)
plot.title('ECG Raw Signal')
plot.plot(nSeq[0:3000])
plot.plot(result_x,result_y_0, 'ro')
plot.subplot(2,1,2)
plot.title('LowFilter + Slope Sum Function ')
plot.xlabel('10ms')
plot.ylabel('Value')
plot.plot(zVal[0:3000], color='g', label='SSF signal')
plot.plot(result_x,result_y,'ro', color='c', label='Heart Beats')
plot.legend(loc='best')
plot.grid(True)
plot.show()
