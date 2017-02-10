# Synopsis
This project implements a chat system that supports **totally ordered multicast** and **failure detection**. This chat system allows multiple clients and each client implements two functions: take messages as input and display messages from all participants in the chat room (including messages from oneself). The messages is displayed in a total ordering, i.e., all clients will see the same messages displayed in the same order. Any client in the chat room can leave or fail at any point of time and other clients in the chat room is able to detect these failures.

# Building
1. Git clone this repo into a local directory.

2. cd to project home directory and run ./gradlew clean build

3. A build folder will be then created in the home directory. cd to /build/distribution. Use scp to transfer the CS425-MP1.tar file into the VMs you wish to be deployed on 

4. ssh into the virtual machines and untar the CS425-MP1.tar files. A lib folder will be created

5. cd to /lib and run java -cp "*"  edu.illinois.uiuc.sp17.cs425.team4.MP1.CP1 -peerCount <clientNumber> -myName <username> -outputFile <file.txt>. Replace <clientNumber> with the number of clients you wish to add and replace <username> with A-J, each corresponding to a VM from g04-01 to g04-10. ** The flag -outputFile <file.txt> is optional, if you do not input this option, the message will be print into the terminal. ** If you input this option, all the message will be written into a text file you specified. Replace <file.txt> with a text file name 

6. If you wish to see the messages in real time under a typical chat app user interface, replace the following commands with step 5. Otherwise, go to step 7

	I.type tmux to start a new tmux session

	II. run java -cp "*"  edu.illinois.uiuc.sp17.cs425.team4.MP1.CP1 -peerCount <clientNumber> -myName <username> -outputFile <file.txt>

	III. hit ctrl and b at the same time, followed by a double quotation makr ". This will split the terminal horizontally for you

	IV. type tmux swap-pane -U to move the new terminal up

	V. type tail -f <file.txt>, replace <file.txt> with the file you specified above to display real time chat messages 

7. Repeat step 4-6 on the same number of virtual machines you wish to deploy on and start chatting!



# Authors
Bhopesh Bassi: bbassi2@illinois.edu

Zhongshen Zeng: zzeng9@illinois.edu


