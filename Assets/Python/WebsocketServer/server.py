import json
import pylab
import traceback
import datetime

#from sys import exit
from txws import WebSocketFactory
from twisted.internet import protocol, reactor

def tostring(data):
    dtype=type(data).__name__
    if dtype=='ndarray':
        if pylab.shape(data)!=(): data=list(data)
        else: data='"'+data.tostring()+'"'
    elif dtype=='dict' or dtype=='tuple':
        try: data=json.dumps(data)#'"'+unicode(data)+'"'
        except: pass
    elif dtype=='NoneType':
        data=''
    elif dtype=='str' or dtype=='unicode':
        data=json.dumps(data)#'"'+unicode(data)+'"'

    return str(data)

class VS(protocol.Protocol):

    global filename
    global target
    global flag


    def connectionMade(self):
        print "CONNECTED"
        self.transport.write('server.connected()')

    def send(self,data,prot):
        print 'send: ' + data
        prot.transport.write(data)

    def dataReceived(self, req):

        try:
            #if not req.find('read'):
                #print 'req:> ' + req
                #res = eval(req)

            if (req.find('shutdown')>=0):
                print 'shutdown'
                return

            if (req.find('START:[]')>=0):
                path = "/Users/borja/Documents/ResearchStay/MobileBIT/Logs/"
                now = datetime.datetime.now()
                VS.filename= path + now.strftime("%Y-%m-%d_%H-%M")+".txt"
                # VS.filename= path + "myECGraw.txt"
                # 'a' append | 'w' new file
                VS.target = open (VS.filename, 'w')
                print 'Ready to log data into '+VS.filename

            if (req.find('STORE_START')>=0):
                VS.flag = 1
                print 'Create log file: '+ VS.filename

            #When read command is received...
            if (req.find('STORE_LOG')>=0):
                if(VS.flag):
                    #replace from format read[(seq1,data1),...] to seq\tdata\n
                    myReq= req.replace("STORE_LOG:[","")
                    myReq= myReq.replace("\n","");
                    myReq= myReq.replace(")]","")
                    myReq= myReq.replace("(","")
                    myReq= myReq.replace(")","\n")
                    myReq= myReq.replace(",","\t")
                    VS.target.write(myReq)
                    print myReq
                else:
                    print "flag is 0"

            if (req.find('STORE_STOP')>=0):
                if(VS.flag):
                    VS.flag = 0
                    VS.target.close()
                    print 'Log file closed'
                else:
                    print "flag is 0"

            if (req.find('DISCONNECTED')>=0):
                print 'Disconnected from the client'

            #for test purpose
            if (req.find('myCommand')>=0):
                print 'command filtered'

        except Exception as e:
            print traceback.format_exc()
            res='sys.exception("'+str(e)+'")'
            print 'res:> ' + res

        #echo of the dataReceived
        #self.transport.write(req)

    def connectionLost(self, reason):
        server.shutdown()
        return

class server(object):
    @staticmethod
    def shutdown():
        connector.stopListening()
        try: reactor.stop()
        except: pass

        print "DISCONNECTED"


class VSFactory(protocol.Factory):
    def buildProtocol(self, addr):
        return VS()


if __name__=='__main__':
    try:
        #lab
        ip_addr, port = "localhost", 9001
        #resi
        #ip_addr, port = "10.24.40.202", 9001

        print "LISTENING AT %s:%s"%(ip_addr, port)

        connector = reactor.listenTCP(port, WebSocketFactory(VSFactory()))
        reactor.run()

    except Exception as e:
        print traceback.format_exc()
