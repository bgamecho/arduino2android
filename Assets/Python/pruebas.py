# -*- coding: utf-8 -*-
"""
Created on Fri Jul 18 15:41:18 2014

@author: Sensores
"""

import sys


import serial


def main():
    mSerial = serial.Serial(7, 57600)
    
    while True:
        print mSerial.readline()
        mSerial.close()
        mSerial.write(sys.stdin.read())
        
    
if __name__ == '__main__':
    main()

#while 1:
#    print 'ENTER cmd:'
#    if sys.stdin.readline().strip() == 's':
#        print 'sending command S'
#    else:
#        print 'err'