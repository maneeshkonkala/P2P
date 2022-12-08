import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class PeerUnchoker extends Thread {
    List<String> info;
    public void run() {
        try {
            while (PeerProcess.peersWithCompleteFile < PeerProcess.peers.size()) {
                int preferredNeighbors = PeerProcess.props.getNeighborsPreferred();
                List<Integer> interestedPeers = new ArrayList<>();
                List<Integer> preferredPeers = new ArrayList<>();

                for (int peerId : PeerProcess.peerSockets.keySet()) {
                    Peer peer = PeerProcess.peers.get(peerId);
                    if(peer.isInterested) interestedPeers.add(peerId);
                }
                int size = interestedPeers.size();
                if (size <= preferredNeighbors) preferredPeers.addAll(interestedPeers);
                else {
                    if(PeerProcess.thisPeer.hasFile) {
                        Random random = new Random();
                        int randomPeerIndex;
                        for (int i = 0; i < preferredNeighbors; i++) {
                            randomPeerIndex = random.nextInt(interestedPeers.size());
                            if (!preferredPeers.contains(randomPeerIndex)) {
                                int randomPeer = interestedPeers.get(randomPeerIndex);
                                preferredPeers.add(randomPeer);
                                interestedPeers.remove(randomPeerIndex);
                            }
                        }
                    } else {
                        for (int i = 0; i < preferredNeighbors; i++) {
                            int maxDownloadRatePeer = interestedPeers.get(0);
                            int maxDownloadRatePeerIndex = 0;
                            for (int j = 1; j < interestedPeers.size(); j++) {
                                Peer interestedPeer = PeerProcess.peers.get(interestedPeers.get(j));
                                Peer maxDownloadRatedPeer = PeerProcess.peers.get(maxDownloadRatePeer);
                                if (interestedPeer.downloadRate > maxDownloadRatedPeer.downloadRate) {
                                    maxDownloadRatePeer = interestedPeers.get(j);
                                    maxDownloadRatePeerIndex = j;
                                }
                            }
                            preferredPeers.add(maxDownloadRatePeer);
                            interestedPeers.remove(maxDownloadRatePeerIndex);
                        }
                    }
                }
                for (int peerId : preferredPeers) {
                    PeerProcess.peers.get(peerId).isChoked = false;
                    Socket s = PeerProcess.peerSockets.get(peerId);
                    byte[] data = Messages.getMessage(MessageTypes.UNCHOKE);
                    Messages.sendMessage(s, data);
                }

                for (int peerId : interestedPeers) {
                    if (!PeerProcess.peers.get(peerId).isChoked) {
                        PeerProcess.peers.get(peerId).isChoked = true;
                        Socket s = PeerProcess.peerSockets.get(peerId);
                        byte[] data = Messages.getMessage(MessageTypes.CHOKE);
                        Messages.sendMessage(s, data);
                    }
                }
                if (!preferredPeers.isEmpty()) {
                    info = new ArrayList<>();
                    info.add(Logger.changePrefNeighbours(preferredPeers));
                    Utils.printInfo(info);
                }
                int interval = PeerProcess.props.getInterval('u');
                long intervalInMillis = interval * 1000L;
                Thread.sleep(intervalInMillis);
            }
        } catch (Exception e) {}
    }
}
