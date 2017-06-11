 #!/bin/bash 

remoteUser=$1
hostName=$2
remoteSsh="ssh ${remoteUser}@${hostName}"

# Create remote directory.
$remoteSsh << EOF
	cd # start from home.
	mkdir -p CS425-Distributed-Systems-Machine-Problems
	cd CS425-Distributed-Systems-Machine-Problems
	rm -rf *
	cd
EOF
# copy file.
scp build/distributions/CS425-Distributed-Systems-Machine-Problems.tar $remoteSsh:"/home/$remoteUser/CS425-Distributed-Systems-Machine-Problems"

# Remote unpack tar
$remoteSsh << EOF
	cd ~/CS425-Distributed-Systems-Machine-Problems
	tar -xf CS425-Distributed-Systems-Machine-Problems.tar
	
EOF
