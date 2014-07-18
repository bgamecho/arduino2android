# -*- coding: utf-8 -*-
"""
Created on Fri Jul 18 15:29:13 2014

@author: Sensores
"""

import threading
import Queue

def console(q, lock):
    while 1:
        raw_input()   # Afther pressing Enter you'll be in "input mode"
        with lock:
            cmd = raw_input('> ')

        q.put(cmd)
        if cmd == 'quit':
            break

def action_foo(lock):
    with lock:
        print('--> action foo')
    # other actions

def action_bar(lock):
    with lock:
        print('--> action bar')

def invalid_input(lock):
    with lock:
        print('--> Unknown command')

def main():
    cmd_actions = {'foo': action_foo, 'bar': action_bar}
    cmd_queue = Queue.Queue()
    stdout_lock = threading.Lock()

    dj = threading.Thread(target=console, args=(cmd_queue, stdout_lock))
    dj.start()

    while 1:
        cmd = cmd_queue.get()
        if cmd == 'quit':
            break
        action = cmd_actions.get(cmd, invalid_input)
        action(stdout_lock)

main()