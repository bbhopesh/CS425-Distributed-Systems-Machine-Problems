# H1 Synopsis
This project implements a chat system that supports **totally ordered multicast** and **failure detection**. This chat system allows multiple clients and each client implements two functions: take messages as input and display messages from all participants in the chat room (including messages from oneself). The messages is displayed in a total ordering, i.e., all clients will see the same messages displayed in the same order. Any client in the chat room can leave or fail at any point of time and other clients in the chat room is able to detect these failures.

# H1 Building
1. Git clone this repo into a local directory.

2. Modify the method initializeGroupMembers of class CP1 of package edu.illinois.uiuc.sp17.cs425.team4.MP1

3. In the method initializeGroupMembers, initialize as many process as you want. Each process corresponds to a single client. The constructor of each process takes *IP address, port number, user name, UUID* as input. Note that for every IP address, input the VMs' address that you are going to deploy on.

4. cd to project home directory and run ./gradlew clean build

5. A build folder will be then created in the home directory. cd to /build/distribution. Use scp to transfer the CS425-MP1.tar file into the VMs you defined in step 3

6. ssh into **every** virtual machine and untar the CS425-MP1.tar file. A lib folder will be created

7. cd to /lib and run java -cp "*"  edu.illinois.uiuc.sp17.cs425.team4.MP1.CP1 <name>. Note that <name> must be the user name that you bind with this IP address in step 3 

8. After the deployment on **every** VM, a chat can now start!


# H1 Authors
Bhopesh Bassi: bbassi2@illinois.edu
Zhongshen Zeng: zzeng9@illinois.edu


