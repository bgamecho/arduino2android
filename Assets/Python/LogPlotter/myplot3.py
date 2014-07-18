import numpy
import matplotlib.pyplot as plot
import scipy.signal
import sys
from PySide import *

path = "/Users/borja/Documents/ResearchStay/MobileBIT/Logs/"
#myPath = path+"2014-05-20_10-18-27.txt" ## 50 Hz
#myPath = path+"log_May_16.txt" ## 50Hz
#myPath = path+"myECGraw.txt" ## 100Hz
#myPath = path+"2014-05-30_18-01.txt" ## 100Hz
#myPath = path+"2014-05-30_18-27.txt" ## 100Hz

myPath = path+"2014-05-30_19-00.txt" ## 100Hz

#maxLen = 3000 # length of the data


#### LOW PASS FILTER #####
def low_pass_filter_p(data):
  a = [1, -2, 1]
  b = [1, 0, 0,  -2,   0, 0, 1] # 100HZ
  #b = [1, 0 , -2, 0, 1] #50Hz
  xVal_p = scipy.signal.lfilter(b,[1],data,-1,None)
  yVal_p = scipy.signal.lfilter(b,a,data,-1,None)
  return yVal_p

# Real time
def low_pass_filter_r(data):
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
def SlopeSumFunction(data):

  #increments of Delta values
  yDelta = numpy.diff(data)
  uDelta = []
  for x in range(0, maxLen-1):
    if yDelta[x] > 0:
      uDelta.insert(x, yDelta[x])
    else:
      uDelta.insert(x, 0)
  uDelta.insert(x, 0)

  #w is the time the slope is up 128 ms..
  w = 13
  zVal=[]
  for i in range(0, w+2):
    zVal.insert(i,0)

  #Sum of slopes
  for i in range(w+1, maxLen):
    sum_k = 0
    for k in range(i-w-1,i):
      sum_k += uDelta[k]
    zVal.insert(i,sum_k)

  return zVal

### Decission Rule ####
##### Adaptive thresholding #####
def HeartBeatDetector(data):
  heartBeats = 0
  num_frames = 200
  t_base = 3 * numpy.mean(data[0:num_frames])
  threshold = 0.6 * t_base
  print "Threshold base: "+ str(t_base)
  print "Threshold init: "+ str(0.6 * t_base)

  result_x=[]
  result_y=[]
  result_y_0=[]

  eyeclosed=0 #eyeclosed window is off
  wind_size = 15 # window size to search max and min

  for i in range(30, maxLen):
    if eyeclosed == 0 :
      if zVal[i] > threshold :
        heartBeats+=1

        ##### Local search #####

        maxVal = max(data[i-wind_size : i+wind_size])
        minVal = min(data[i-wind_size : i+wind_size])
        result_y.append(maxVal)

        #point of the max value
        maxPoint = zVal[i-wind_size:i+wind_size].index(maxVal)
        maxPoint = i -wind_size + maxPoint
        result_x.append(maxPoint)
        minPoint = max(i-wind_size, i+wind_size)
        t_base = maxVal
        threshold = 0.6 * t_base
        print "Beat detected - Pos: "+ str(i)+ " : "+ str(maxPoint)
        print "\tNew Threshold: "+ str(threshold)
        print "\tMax - Min = "+str(maxVal - minVal)
        eyeclosed = 30 #eyeclosed window is on
        #print str(i) + " heart beat detected at position: " + str(maxPoint) + " value: "+str(maxVal)
    else:
      eyeclosed-=1

  result_y_0=[]
  for i in range(0, len(result_x)):
    result_y_0.append(nSeq[result_x[i]])

  plot_heartPoints(nSeq, result_x, result_y_0, data, result_x, result_y)

  return heartBeats

##### PLOTTING ####
def plot_heartPoints(seq1, result_x, result_y, seq2, result_x2, result_y2):
  plot.figure()
  plot.subplot(2,1,1)
  plot.plot(seq1[0:maxLen])
  plot.plot(result_x, result_y, 'ro')
  plot.subplot(2,1,2)
  plot.plot(seq2[0:maxLen], color='g', label='SSF signal')
  plot.plot(result_x2, result_y2, 'ro', color='c', label='Heart Beats = {i}'.format(i=len(result_x2)))
  plot.legend(loc='best')
  plot.grid(True)
  plot.show()


def plotting(sequence):
  plot.figure()
  plot.plot(sequence)
  plot.show()

def plotting_2(seq1, seq2):
  plot.figure()
  plot.subplot(2,1,1)
  plot.plot(seq1[0:maxLen])
  #plot.plot(result_x,result_y_0, 'ro')
  plot.subplot(2,1,2)
  plot.plot(seq2[0:maxLen], color='g', label='SSF signal')
  #plot.plot(result_x,result_y,'ro', color='c', label='Heart Beats = {i}'.format(i=heartBeats))
  #plot.legend(loc='best')
  plot.grid(True)
  plot.show()

def gui_fname(dir=None):
  """Select a file via a dialog and return the file name.
  """
  if dir is None: dir ='./'
  fname = QtGui.QFileDialog.getOpenFileName(None, "Select data file...",dir, filter="All files (*);; SM Files (*.sm)")
  return fname[0]

#def main():


if __name__ == '__main__':
  #  main()
  app = QtGui.QApplication(sys.argv)
  var = gui_fname("/Users/borja/Documents/ResearchStay/MobileBIT/Logs/")

  data = numpy.loadtxt(var, 'float')

  #noisy sequence:
  nSeq = data[0:3000,1]
  maxLen = len(nSeq)
  print maxLen
  yVal_p = low_pass_filter_p(nSeq)
  zVal = SlopeSumFunction(yVal_p)
  #plotting_2(yVal_p, zVal)
  heartBeats = HeartBeatDetector(zVal)
  print 'Heartbeats: ' + str(heartBeats)

  #sys.exit(app.exec_())
