from numpy import *
from matplotlib.pyplot import *

path = "/Users/borja/Documents/ResearchStay/MobileBIT/Logs/"
# myPath=path+"2014-05-20_10-18-27.txt"
myPath = path+"myECGraw.txt"

data = loadtxt(myPath, 'float')

nSeq = data[:,1]

figure()
plot(nSeq)
show()
