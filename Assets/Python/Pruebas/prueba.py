import sys
from PyQt4 import *

def gui_fname(dir=None):
    """Select a file via a dialog and return the file name.
    """
    if dir is None: dir ='./'
    fname = QtGui.QFileDialog.getOpenFileName(None, "Select data file...",
            dir, filter="All files (*);; SM Files (*.sm)")
    return fname[0]




def main():

    app = QtGui.QApplication(sys.argv)

    var = gui_fname("")
    print var

    #sys.exit(app.exec_())

if __name__ == '__main__':
    main()
