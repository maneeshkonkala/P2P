import java.io.DataInputStream;
import java.io.InputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ConnectToPeers extends Thread {
    List<String> info;
    public void run() {
        byte[] inputData = new byte[32];
        try {
            byte[] handShakeMessage = Messages.getHandshakeMessage(PeerProcess.currentPeerId);
            for (Integer peerId : PeerProcess.peers.keySet()) {
                if (peerId == PeerProcess.currentPeerId)
                    break;
                Peer peer = PeerProcess.peers.get(peerId);
                Socket socket = new Socket(peer.hostName, peer.port);
                Messages.sendMessage(socket, handShakeMessage);
                info = new ArrayList<>();
                info.add(Logger.tcpConnectionMake(peerId));
                Utils.printInfo(info);
                InputStream inputStream = socket.getInputStream();
                DataInputStream dataInputStream = new DataInputStream(inputStream);
                dataInputStream.readFully(inputData);
                ByteBuffer bybuff = ByteBuffer.wrap(Arrays.copyOfRange(inputData, 28, 32));
                int receivedPeerId = bybuff.getInt();
                if (receivedPeerId != peerId) socket.close();
                else {
                    info = new ArrayList<>();
                    info.add(Logger.tcpConnectionMade(peerId));
                    Utils.printInfo(info);
                    StringBuilder handshakeMsg = new StringBuilder();
                    String handShakeMessageFirstHalf = new String(Arrays.copyOfRange(inputData, 0, 28));
                    handshakeMsg.append(handShakeMessageFirstHalf);
                    handshakeMsg.append(receivedPeerId);
                    PeerProcess.peerSockets.put(peerId, socket);
                    new PeerProcess.ExchangeMessages(socket, peerId).start();
                }
            }
        } catch (Exception e) {}
    }
}