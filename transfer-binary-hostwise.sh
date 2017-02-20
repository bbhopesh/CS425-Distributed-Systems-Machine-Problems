 #!/bin/bash 

remoteUser=$1
hostName=$2
remoteSsh="ssh ${remoteUser}@${hostName}"

# Create remote directory.
$remoteSsh << EOF
	cd # start from home.
	mkdir -p CS425MP1
	cd CS425MP1
	rm -rf *
	cd
EOF
# copy file.
scp build/distributions/CS425-MP1.tar $remoteSsh:"/home/$remoteUser/CS425MP1"

# Remote unpack tar
$remoteSsh << EOF
	cd ~/CS425MP1
	tar -xf CS425-MP1.tar
	
EOF
