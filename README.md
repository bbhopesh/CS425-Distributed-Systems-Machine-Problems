# Synopsis
This project implements a Key-Value storage system that supports an interface with following operations: **SET, GET, OWNER, LIST_LOCAL, BATCH**. This system allows users to write to a key in any VM, then read the key from any VM. Machines is allowed to leave and join at anytime as long as there are three VMs up in the system at the same time. Keys will be rebalanced after a new VM has joined or left.

# Building
1. Git clone this repo into a local directory.

2. cd to project home directory and run sh transfer-binary.sh VM_NUMS USERNAME. This shell script will build the application and transfer the built tar file to VM_NUMS of virtual machines assigned to our group(gourp 4).It will also untar the files on the root directory of the VM.

3. SSH to the machines assigned to our group

4. cd to CS425-MP1/lib and run java -cp "*" edu.illinois.uiuc.sp17.cs425.team4.MP2.MP2Main. 

5. Repeat step 4 on at least three machines to start the system.

# Authors
Bhopesh Bassi: bbassi2@illinois.edu

Zhongshen Zeng: zzeng9@illinois.edu


