# Synopsis
This project implements a chat system that supports **totally ordered multicast** and **failure detection**. This chat system allows multiple clients and each client implements two functions: take messages as input and display messages from all participants in the chat room (including messages from oneself). The messages is displayed in a total ordering, i.e., all clients will see the same messages displayed in the same order. Any client in the chat room can leave or fail at any point of time and other clients in the chat room is able to detect these failures.

# Building
1. Git clone this repo into a local directory.

2. cd to project home directory and run sh transfer-binary.sh VM_NUMS USERNAME. This shell script will build the application and transfer the built tar file to VM_NUMS of virtual machines assigned to our group(gourp 4).It will also untar the files on the root directory of the VM. Note this script requires you to automate login process using ssh-copy-id id@server. If you can't automatically login your cluster, then go to step three. Otherwise go to step 6. 

3. run ./gradlew clean build

4. A build folder will be then created in the home directory. cd to /build/distribution. Use scp to transfer the CS425-MP1.tar file into the VMs you wish to be deployed on 

5. ssh into the virtual machines and untar the CS425-MP1.tar files. A lib folder will be created

6. cd to /lib and run java -cp "*"  edu.illinois.uiuc.sp17.cs425.team4.MP1.CP2 -peerCount <clientNumber> -outputFile <file.txt>. Replace <clientNumber> with the number of clients you wish to add. __The flag -outputFile <file.txt> is optional, if you do not input this option, the message will be print into the terminal__. If you input this option, all the message will be written into a text file you specified. Replace <file.txt> with a text file name 

7. Repeat step 4-6 on the same number of virtual machines you wish to deploy on and start chatting!



# Authors
Bhopesh Bassi: bbassi2@illinois.edu

Zhongshen Zeng: zzeng9@illinois.edu


