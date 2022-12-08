import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class OptimisticPeerUnchoker extends Thread {
    List<String> info;
    public void run() {
        try {
            while (PeerProcess.peersWithCompleteFile < PeerProcess.peers.size()) {
                int randomPeerId = 0;
                List<Integer> interestedPeers = new ArrayList<>();
                for (int peerId : PeerProcess.peerSockets.keySet()) {
                    Peer peer = PeerProcess.peers.get(peerId);
                    if (peer.isInterested && peer.isChoked) interestedPeers.add(peerId);
                }
                if (!interestedPeers.isEmpty()) {
                    Random random = new Random();
                    int randomPeerIndex = random.nextInt(interestedPeers.size());
                    randomPeerId = interestedPeers.get(randomPeerIndex);
                    Socket s = PeerProcess.peerSockets.get(randomPeerId);
                    byte[] data = Messages.getMessage(MessageTypes.UNCHOKE);
                    Messages.sendMessage(s, data);
                    PeerProcess.peers.get(randomPeerId).isChoked = false;
                }
                if (randomPeerId != 0){
                    info = new ArrayList<>();
                    info.add(Logger.changeOptilyUnchokedNeighbour(randomPeerId));
                    Utils.printInfo(info);
                }
                int interval = PeerProcess.props.getInterval('o');
                long intervalInMillis = interval * 1000L;
                Thread.sleep(intervalInMillis);
            }
        } catch (Exception e) {}
    }
}
