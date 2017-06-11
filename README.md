# Synopsis
This repository contains all the three machine problems that were part of CS425: Distributed Systems course at UIUC, Spring 2017.
The three machine problems are as following:
1. Totally-ordered fault-tolerant group chat application.
2. Fault-tolerant in-memory key value store.
3. Distributed transactions system with serial equivalence and deadlock detection.


# Directory structure
* Detailed problem statements and design documents could be found in directory "Problem Statements and Design documents."
* Rest of the directory structure is in accordance with gradle/maven defalt directory structure.


# Key algorithms implemented and other learnings
1. ISIS totally ordered multicast.
2. SWIM failure detector.
3. Cassandra/Chord based ring partitioned key-value store which tolerates certain number of failures by making those many extra copies. These copies were made when SWIM failure detector detected failures.
4. Two-phase locking for serial equivalence in distributed transactions.
5. Deadlock detection b constructing wait for graph between transactions.


# Authors
Bhopesh Bassi: bbassi2@illinois.edu

Zhongshen Zeng: zzeng9@illinois.edu


