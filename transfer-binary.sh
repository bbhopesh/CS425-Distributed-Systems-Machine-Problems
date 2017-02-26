 #!/bin/bash 

# Do gradle build.
./gradlew clean build

peerCount=$1
remoteUser=$2
hostnameTemplate="sp17-cs425-g04-hostnumber.cs.illinois.edu"

# transfer to each host
hostNumber=1
while [  $hostNumber -le $peerCount ]; do
	printf -v hostNumberStr "%02d" $hostNumber
	hostName="${hostnameTemplate/hostnumber/$hostNumberStr}"
	./transfer-binary-hostwise.sh $remoteUser $hostName
	let hostNumber=hostNumber+1 
done

echo "######## Ignore errors like: <ssh: No such file or directory>. Files are being copied. ######"