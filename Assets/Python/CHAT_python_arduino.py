# -*- coding: utf-8 -*-
"""
Created on Tue Jul 15 19:50:47 2014

@author: Xabi
"""

import serial
import sys
import threading
import Queue
import PyQt4
import numpy
#from __future__ import print_function
from serial.serialutil import SerialException
from time import sleep
#from serialutils import full_port_name, enumerate_serial_ports

import re

import _winreg as winreg
import itertools

def enumerate_serial_ports():
    """ Uses the Win32 registry to return an
        iterator of serial (COM) ports
        existing on this computer.
    """
    path = 'HARDWARE\\DEVICEMAP\\SERIALCOMM'
    try:
        key = winreg.OpenKey(winreg.HKEY_LOCAL_MACHINE, path)
    except WindowsError:
        raise IterationError

    for i in itertools.count():
        try:
            val = winreg.EnumValue(key, i)
            yield str(val[1])
        except EnvironmentError:
            break

def full_port_name(portname):
    """ Given a port-name (of the form COM7,
        COM12, CNCA0, etc.) returns a full
        name suitable for opening with the
        Serial class.
    """
    m = re.match('^COM(\d+)$', portname)
    if m and int(m.group(1)) < 10:
        return portname
    return '\\\\.\\' + portname




def console(q, lock):
    while 1:
        #raw_input()   # Afther pressing Enter you'll be in "input mode"
        with lock:
            print '\n\n>>Options menu:\n\t'+\
            '[1]Open a port \n\t'+\
            '[2]See available ports\n\t'+\
            '[3]Scan a log file\n\t'+\
            '\'quit\' to exit application '
            cmd = raw_input('> ')

        q.put(cmd)
        if cmd == 'quit':
            break
        sleep(0.05)
        
def open_port(lock):
    with lock:
        print 'Enter port number'
        raw_input('>')
        try:
            mSerial = serial.Serial(7, 57600)
            if mSerial and mSerial.isOpen:
                print 'Port successfully opened'
                i = 0
                while i < 10:
                    rcv = mSerial.readline().strip()
                    sys.stdout.write('\r%s' % rcv)
                    sys.stdout.flush()
                    i+=1
    #            sendMsg = sys.stdin.readline().strip()
    #            if sendMsg != 'end':
    #                mSerial.write(sendMsg)
    #                print mSerial.readline()
                mSerial.close()
            else:
                print 'Could not open port'
        except SerialException, e:
            print 'Exception happenned while opening port'
            print e
            if mSerial:
                mSerial.close()
            
def show_ports(lock):
    with lock:
        print '\n>>Showing available ports'
        for p in  enumerate_serial_ports():
            print p

def scan_log(lock):
    with lock:
        print 'This function is not yet done'
        logFile = gui_fname("C:/Documents and Settings/Sensores/Mis documentos/Dropbox/PFG/git/arduino2android/Assets/Python/")
        print 'You selected the file: '+logFile
        #data = numpy.loadtxt(logFile, 'float')
        sleep(2)
        data = numpy.genfromtxt(logFile,dtype='str')
        print data
        #noisy sequence:
        nSeq = data[0:30,1]
        print nSeq
        maxLen = len(nSeq)
        print maxLen

def gui_fname(dir=None):
  """Select a file via a dialog and return the file name.
  """
  if dir is None: dir ='./'
  fname = PyQt4.QtGui.QFileDialog.getOpenFileName(None, "Select data file...",dir, filter="All files (*);; SM Files (*.sm)")
  #return fname[0]
  return str(fname)


def invalid_input(lock):
    with lock:
        print('--> Unknown command')

def main():
    cmd_actions = {'1': open_port, '2': show_ports, '3': scan_log}
    cmd_queue = Queue.Queue()
    stdout_lock = threading.Lock()

    dj = threading.Thread(target=console, args=(cmd_queue, stdout_lock))
    dj.start()
    
    app = PyQt4.QtGui.QApplication(sys.argv)

    while 1:
        cmd = cmd_queue.get()
        print 'hi'
        if cmd == 'quit':
            break
        action = cmd_actions.get(cmd, invalid_input)
        action(stdout_lock)


        
    
if __name__ == '__main__':
    main()