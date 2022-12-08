import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class EstablishConnectionsWithPeers extends Thread {
    List<String> info;
    public void run() {
        byte[] data = new byte[32];
        try {
            byte[] handShakeMessage = Messages.getHandshakeMessage(PeerProcess.currentPeerId);
            Peer peer = PeerProcess.peers.get(PeerProcess.currentPeerId);
            ServerSocket serverSocket = new ServerSocket(peer.port);
            int limit = PeerProcess.peers.size() - 1;
            int size = PeerProcess.peerSockets.size();
            while (size < limit) {
                Socket socket = serverSocket.accept();
                InputStream inputStream = socket.getInputStream();
                DataInputStream input = new DataInputStream(inputStream);
                input.readFully(data);
                StringBuilder handshakeMsg = new StringBuilder();
                //scope for change - copyrange separate util function
                ByteBuffer bybuff = ByteBuffer.wrap(Arrays.copyOfRange(data, 28, 32));
                int peerId = bybuff.getInt();
                //scope for change
                handshakeMsg.append(new String(Arrays.copyOfRange(data, 0, 28)));
                handshakeMsg.append(peerId);
                info = new ArrayList<>();
                info.add(Logger.tcpConnectionMade(peerId));
                Utils.printInfo(info);
                Messages.sendMessage(socket, handShakeMessage);
                System.out.println(Logger.tcpConnectionMake(peerId));
                new PeerProcess.ExchangeMessages(socket, peerId).start();
                PeerProcess.peerSockets.put(peerId, socket);
            }
            serverSocket.close();
        } catch (IOException e) {}
    }
}
