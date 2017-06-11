# Synopsis
This repository contains all the three machine problems that were part of CS425: Distributed Systems course at UIUC, Spring 2017.
The three machine problems are as following:
1. Totally-ordered fault-tolerant group chat application.
2. Fault-tolerant in-memory key value store.
3. Distributed transactions system with serial equivalence and deadlock detection.


# Directory structure
* Detailed problem statements, design documents and instrunctions to run code are in directory "Problem Statements and Design documents."
* Rest of the directory structure is in accordance with gradle/maven defalt directory structure.


# Key algorithms implemented
All the algorithms listed below were built from scratch directly on top of TCP and without use of any of the distributed systems frameworks/abstractions like RPC etc. Even though you would use frameworks in practical systems, it made sense to not use when main aim was learning.

1. ISIS totally ordered multicast.
2. SWIM failure detector.
3. Cassandra/Chord based ring partitioned key-value store which tolerates certain number of failures by making those many extra copies. These copies were made when SWIM failure detector detected failures.
4. Two-phase locking for serial equivalence in distributed transactions.
5. Deadlock detection by constructing wait for graph between transactions.

