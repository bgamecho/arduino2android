# -*- coding: utf-8 -*-
"""
Created on Tue Jul 15 19:50:47 2014

@author: Xabi
"""

import serial


def main():
    mSerial = serial.Serial(7, 57600)
    
    while True:
        print mSerial.readline()
        
        mSerial.write(sys.stdin.read())
        
    
if __name__ == '__main__':
    main()