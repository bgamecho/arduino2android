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
import matplotlib.pyplot as plt
import thread
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
            '[1]Open ports \n\t'+\
            '[2]See available ports\n\t'+\
            '[3]Test script\n\t'+\
            '\'quit\' to exit application '
            cmd = raw_input('> ')

        q.put(cmd)
        if cmd == 'quit':
            break
        sleep(0.05)

portDict = {}        
def open_ports(lock):
    with lock:
        print 'Opening all ports'

        for p in  enumerate_serial_ports():
            if p != 'COM1':
                mSerial = serial.Serial()
                try:
                    mSerial = serial.Serial(p, 57600)
                    sleep(2)
                    if mSerial and mSerial.isOpen:
                        print 'Port '+p+' successfully opened'
                        portDict[p]=mSerial
                    else:
                        print 'Could not open port'
                except SerialException, e:
                    print 'Exception happenned while opening port'
                    print e
                    if mSerial and mSerial.isOpen:
                        mSerial.close()

        thread.start_new_thread( progressive_power_off, (portDict, 0) )


#def test_script(lock):
#    open_ports(lock)
#    
#    print 'Starting command script'
#    thread.start_new_thread( selective, ('COM18', 'p') )
#    
#def selective(portName, command):
#    for (pName,port) in portDict.items():
#        if pName in portName:
#            print 'Sending \''+command+'\' to '+portName
#            port.write('p')

def test_script(lock):
    thread.start_new_thread( progressive_power_on, (portDict, 0) )
    
    sleep(5)
   
    for (pName,port) in portDict.items():
        port.write('p')
    
#    while True:
#        for (pName,port) in portDict.items():
#            port.write('s')
#            thread.start_new_thread( increment_payload, (pName,port,0.0099,100))
#        sleep(900)
    
#    while True:
#        # 1) Start all Arduinos progressively
#        thread.start_new_thread( progressive_power_on, (portDict, 30) )
#        
#        #        for (pName,port) in portDict.items():
#        #            port.write('s')
#        #            thread.start_new_thread( increment_payload, (pName,port,0.0099,600))
#        
#        #sleep(180) # wait for 3mins
#        sleep(600) # wait for 5mins
#        thread.start_new_thread( progressive_power_off, (portDict, 30) )
#        sleep(600) # wait for 5mins
#        thread.start_new_thread( progressive_power_on, (portDict, 30) )
#        sleep(600) # wait for 5mins
#        thread.start_new_thread( progressive_power_off, (portDict, 30) )
#        
#    close_ports(portDict)
            
def close_ports(ports):
    for (pName,port) in ports.items():
        print 'Closing port '+pName
        port.close()

def progressive_power_on(ports, delay):
    for (pName,port) in ports.items():
        print 'Powering-on '+pName
        port.write('p')
        sleep(delay)
    print 'Done powering-on all Arduinos'

def progressive_power_off(ports, delay):
    for (pName,port) in ports.items():
        print 'Powering-off '+pName
        port.write('f')
        sleep(delay)
    print 'Done powering-off all Arduinos'

def increment_payload(name,port,delay,n):
    i=0
    while i<n:
        port.write('i')
        sleep(delay)
        i+=1
            
def show_ports(lock):
    with lock:
        print '\n>>Showing available ports'
        for p in  enumerate_serial_ports():
            print p



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
    cmd_actions = {'1': open_ports, '2': show_ports, '3': test_script}
    cmd_queue = Queue.Queue()
    stdout_lock = threading.Lock()

    dj = threading.Thread(target=console, args=(cmd_queue, stdout_lock))
    dj.start()
    
    app = PyQt4.QtGui.QApplication(sys.argv)

    while 1:
        cmd = cmd_queue.get()
        if cmd == 'quit':
            break
        action = cmd_actions.get(cmd, invalid_input)
        action(stdout_lock)


        
    
if __name__ == '__main__':
    main()