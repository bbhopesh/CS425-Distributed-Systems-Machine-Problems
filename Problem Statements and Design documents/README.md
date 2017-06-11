# Transfer code to cluster and setup
* `./transfer-binary.sh $VM_NUMS $USER` e.g. `./transfer-binary.sh 10 bbassi2`
* `cd /home/$USER/CS425-Distributed-Systems-Machine-Problems`


# MP1
* Provide peer count as number of group chat members.
* Provide a unique name to each starting from A while running the following command.`java -cp "lib/*"  edu.illinois.uiuc.sp17.cs425.team4.MP1.CP2FailureDetector -peerCount 4 -myName A`


# MP2
* `java -cp "lib/*"  edu.illinois.uiuc.sp17.cs425.team4.MP2.MP2Main`
* Some sample batch files are kvbatchfile directory.


# MP3
* Run data servers on machines 1 to 5 with command: `java -cp "lib/*"  edu.illinois.uiuc.sp17.cs425.team4.MP3.Server`
* Run lock server on machine 9 with command: `java -cp "lib/*"  edu.illinois.uiuc.sp17.cs425.team4.MP3.LockService`
* Run clients on machines 6 to 8 with command: `java -cp "lib/*"  edu.illinois.uiuc.sp17.cs425.team4.MP3.Client`
 
## Load testing
* `git clone https://github.com/XueweiKent/428mp3demo.git`
* `cp 428mp3demo/* .`
* `python load_test1.py java -cp "lib/*"  edu.illinois.uiuc.sp17.cs425.team4.MP3.Client`
* `python load_test2.py java -cp "lib/*"  edu.illinois.uiuc.sp17.cs425.team4.MP3.Client`
* `python load_test3.py java -cp "lib/*"  edu.illinois.uiuc.sp17.cs425.team4.MP3.Client`
