JCC = javac
JAVA = java
JFLAGS = -g
REMOTE_PEER_OPTS = -cp .:jsch.jar

default: peerProcess.class

peerProcess.class: PeerProcess.java
	$(JCC) $(JFLAGS) PeerProcess.java

Peer.class: Peer.java
	$(JCC) $(JFLAGS) Peer.java

MessageTypes.class: MessageTypes.java
	$(JCC) $(JFLAGS) MessageTypes.java

Messages.class: Messages.java
	$(JCC) $(JFLAGS) Messages.java

LogWriter.class: LogWriter.java
	$(JCC) $(JFLAGS) LogWriter.java

Setup.class: Setup.java
	$(JCC) $(JFLAGS) Setup.java

Props.class: Props.java
	$(JCC) $(JFLAGS) Props.java

FetchPeerInfo.class: FetchPeerInfo.java
	$(JCC) $(JFLAGS) FetchPeerInfo.java

clean:
	$(RM) *.class
