import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PeerProcess {
    private static int peersWithCompleteFile = 0;
    private static int thisPeerId;
    private static int numberOfPieces;
    private static Peer thisPeer;
    private static Props props;
    private static LinkedHashMap<Integer, Peer> peers;
    private static ConcurrentHashMap<Integer, Socket> peerSockets;

    public static void main(String[] args) {
        try {
            thisPeerId = Integer.parseInt(args[0]);
            peerSockets = new ConcurrentHashMap<>();

            //Setup peer specific info
            peers = FetchPeerInfo.getAllPeersInfo();

            //Create directories for to save downloaded file if they don't exist
            for (int peerId : peers.keySet()) {
                Files.createDirectories(Paths.get("peer_" + peerId));
            }

            //Setup common info
            props = Setup.getProps();
            numberOfPieces = props.getNumberOfPieces();
            System.out.println("Number Of Preferred Neighbors : " + props.getNeighborsPreferred());
            System.out.println("Unchoking Interval : " + props.getInterval('u') + " seconds");
            System.out.println("Optimistic Unchoking Interval : " + props.getInterval('o') + " seconds");
            System.out.println("File Name : " + props.getFileName());
            System.out.println("File Size : " + props.getSize('f'));
            System.out.println("Piece Size : " + props.getSize('p'));
            System.out.println("\n");


            //Setup file specific info for this peer
            Setup.setCurrentPeerFileInfo(peers, thisPeerId, props);
            thisPeer = peers.get(thisPeerId);
            System.out.println("Peer Id : " + thisPeerId);
            System.out.println("Host Name : " + thisPeer.hostName);
            System.out.println("Port Number : " + thisPeer.port);
            System.out.println("Has Complete File : " + (thisPeer.hasFile ? "Yes" : "No"));
            System.out.println("\n");

            if (thisPeer.hasFile)
                peersWithCompleteFile += 1;

            //Create log file for this peer
            Logger.startLogger(thisPeerId);

            //Starting all threads to start the protocol
            ConnectToPeers connectToPeers = new ConnectToPeers();
            AcceptConnectionsFromPeers acceptConnectionsFromPeers = new AcceptConnectionsFromPeers();
            UnchokePeers unchokePeers = new UnchokePeers();
            OptimisticallyUnchokePeers optimisticallyUnchokePeers = new OptimisticallyUnchokePeers();

            connectToPeers.start();
            acceptConnectionsFromPeers.start();
            unchokePeers.start();
            optimisticallyUnchokePeers.start();
        } catch (Exception e) {
//            e.printStackTrace();
        }
    }

    static class ConnectToPeers extends Thread {
        @Override
        public void run() {
            byte[] inputData = new byte[32];
            try {
                byte[] handShakeMessage = Messages.getHandshakeMessage(thisPeerId);

                //Iterate through the peers hashmap
                for (int peerId : peers.keySet()) {

                    //We break here because we only want to connect to peers started before this peer. If this peer is
                    // 1003 we only want to connect to 1001 and 1002. When the loop reaches 1003 we break
                    if (peerId == thisPeerId)
                        break;

                    //Writing the handshake on the output stream
                    Peer peer = peers.get(peerId);
                    Socket socket = new Socket(peer.hostName, peer.port);
                    Messages.sendMessage(socket, handShakeMessage);
                    System.out.println(Logger.tcpConnectionMake(peerId));

                    //The other peer sends a handshake message which is retrieved here
                    DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
                    dataInputStream.readFully(inputData);
                    int receivedPeerId = ByteBuffer.wrap(Arrays.copyOfRange(inputData, 28, 32)).getInt();

                    //This is the check that is mentioned in the project description. If the peer id is different
                    //from the one we connected to , we close the socket
                    if (receivedPeerId != peerId)
                        socket.close();
                    else {
                        System.out.println(Logger.tcpConnectionMade(peerId));
                        StringBuilder handshakeMsg = new StringBuilder();
                        handshakeMsg.append(new String(Arrays.copyOfRange(inputData, 0, 28)));
                        handshakeMsg.append(receivedPeerId);
                        peerSockets.put(peerId, socket);
                        new ExchangeMessages(socket, peerId).start();
                    }
                }
            } catch (Exception e) {
//                e.printStackTrace();
            }
        }
    }

    static class AcceptConnectionsFromPeers extends Thread {
        @Override
        public void run() {
            byte[] data = new byte[32];
            try {
                byte[] handShakeMessage = Messages.getHandshakeMessage(thisPeerId);
                Peer peer = peers.get(thisPeerId);
                ServerSocket serverSocket = new ServerSocket(peer.port);
                //While loop runs peers.size() - 1 times because we want to connect to all other peers
                while (peerSockets.size() < peers.size() - 1) {
                    Socket socket = serverSocket.accept();
                    DataInputStream input = new DataInputStream(socket.getInputStream());
                    input.readFully(data);
                    StringBuilder handshakeMsg = new StringBuilder();
                    int peerId = ByteBuffer.wrap(Arrays.copyOfRange(data, 28, 32)).getInt();
                    handshakeMsg.append(new String(Arrays.copyOfRange(data, 0, 28)));
                    handshakeMsg.append(peerId);
                    System.out.println(Logger.tcpConnectionMade(peerId));
                    //Sending handshake message to connected peer
                    Messages.sendMessage(socket, handShakeMessage);
                    System.out.println(Logger.tcpConnectionMake(peerId));
                    new ExchangeMessages(socket, peerId).start();
                    peerSockets.put(peerId, socket);
                }
                serverSocket.close();
            } catch (Exception e) {
//                e.printStackTrace();
            }
        }
    }

    static class UnchokePeers extends Thread {
        @Override
        public void run() {
            try {
                while (peersWithCompleteFile < peers.size()) {
                    int preferredNeighbors = props.getNeighborsPreferred();
                    ArrayList<Integer> interestedPeers = new ArrayList<>();
                    ArrayList<Integer> preferredPeers = new ArrayList<>();

                    for (int peerId : peerSockets.keySet()) {
                        if (peers.get(peerId).isInterested)
                            interestedPeers.add(peerId);
                    }

                    if (interestedPeers.size() <= preferredNeighbors)
                        preferredPeers.addAll(interestedPeers);

                    else {
                        if (thisPeer.hasFile) {
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
                                    if (peers.get(interestedPeers.get(j)).downloadRate > peers.get(maxDownloadRatePeer).downloadRate) {
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
                        peers.get(peerId).isChoked = false;
                        Messages.sendMessage(peerSockets.get(peerId), Messages.getMessage(MessageTypes.UNCHOKE));
                    }

                    for (int peerId : interestedPeers) {
                        if (!peers.get(peerId).isChoked) {
                            peers.get(peerId).isChoked = true;
                            Messages.sendMessage(peerSockets.get(peerId), Messages.getMessage(MessageTypes.CHOKE));
                        }
                    }
                    if (!preferredPeers.isEmpty())
                        System.out.println(Logger.changePrefNeighbours(preferredPeers));

                    Thread.sleep(props.getInterval('u') * 1000L);
                }
            } catch (Exception e) {
//                e.printStackTrace();
            }
        }
    }

    static class OptimisticallyUnchokePeers extends Thread {
        @Override
        public void run() {
            try {
                while (peersWithCompleteFile < peers.size()) {
                    int randomPeerId = 0;
                    ArrayList<Integer> interestedPeers = new ArrayList<>();

                    for (int peerId : peerSockets.keySet()) {
                        if (peers.get(peerId).isInterested && peers.get(peerId).isChoked)
                            interestedPeers.add(peerId);
                    }

                    if (!interestedPeers.isEmpty()) {
                        Random random = new Random();
                        int randomPeerIndex = random.nextInt(interestedPeers.size());
                        randomPeerId = interestedPeers.get(randomPeerIndex);
                        Messages.sendMessage(peerSockets.get(randomPeerId), Messages.getMessage(MessageTypes.UNCHOKE));
                        peers.get(randomPeerId).isChoked = false;
                    }
                    if (randomPeerId != 0)
                        System.out.println(Logger.changeOptilyUnchokedNeighbour(randomPeerId));

                    Thread.sleep(props.getInterval('o') * 1000L);
                }
            } catch (Exception e) {
//                e.printStackTrace();
            }
        }
    }

    static class ExchangeMessages extends Thread {
        private Socket socket;
        private final int peerId;

        public ExchangeMessages(Socket socket, int peerId) {
            this.socket = socket;
            this.peerId = peerId;
        }

        public boolean checkIfInteresting(int[] thisPeerBitfield, int[] connectedPeerBitField, int length) {
            for (int i = 0; i < length; i++) {
                if (thisPeerBitfield[i] == 0 && connectedPeerBitField[i] == 1) {
                    return true;
                }
            }
            return false;
        }

        public void checkIfCompleteFileDownloaded() {
            int parts = 0;
            byte[] mergedFile = new byte[props.getSize('f')];
            for (int bit : thisPeer.bitField) {
                if (bit == 1)
                    parts += 1;
            }
            int index = 0;
            if (parts == thisPeer.bitField.length) {
                peersWithCompleteFile += 1;
                thisPeer.hasFile = true;
                for (int piece = 0; piece < numberOfPieces; piece++) {
                    for (int i = 0; i < thisPeer.filePieces[piece].length; i++) {
                        mergedFile[index] = thisPeer.filePieces[piece][i];
                        index += 1;
                    }
                }
                try {
                    FileOutputStream fileOutputStream = new FileOutputStream("peer_" + thisPeerId + File.separatorChar
                            + props.getFileName());
                    BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream);
                    bufferedOutputStream.write(mergedFile);
                    bufferedOutputStream.close();
                    fileOutputStream.close();
                    System.out.println(Logger.totalDownloadCompleted());
                } catch (Exception e) {
//                    e.printStackTrace();
                }
            }
        }

        public int getPieceIndex(int[] thisPeerBitfield, int[] connectedPeerBitfield, int len) {
            ArrayList<Integer> indices = new ArrayList<>();
            int i;
            for (i = 0; i < len; i++) {
                if (thisPeerBitfield[i] == 0 && connectedPeerBitfield[i] == 1) {
                    indices.add(i);
                }
            }
            Random r = new Random();
            if (indices.size() > 0) {
                return indices.get(Math.abs(r.nextInt() % indices.size()));
            }
            return -1;
        }

        @Override
        public synchronized void run() {
            synchronized (this) {
                try {
                    long startTime;
                    long endTime;
                    double elapsedTime;
                    DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
                    byte[] bitFieldMessage = Messages.getBitFieldMessage(thisPeer.bitField);
                    Messages.sendMessage(socket, bitFieldMessage);

                    while (peersWithCompleteFile < peers.size()) {
                        int messageLengthField = dataInputStream.readInt();
                        byte[] input = new byte[messageLengthField];
                        startTime = System.nanoTime();
                        dataInputStream.readFully(input);
                        endTime = System.nanoTime();
                        elapsedTime = (double) (endTime - startTime) / 1000000000;

                        char messageType = (char) input[0];
                        byte[] message = new byte[messageLengthField - 1];
                        int index = 0;
                        for (int i = 1; i < messageLengthField; i++) {
                            message[index] = input[i];
                            index++;
                        }
                        int pieceIndex;
                        int bits;
                        Peer peer = peers.get(peerId);
                        switch (messageType) {
                            case MessageTypes.CHOKE:
                                peer.isChoked = true;
                                System.out.println(Logger.choked(peerId));
                                break;
                            case MessageTypes.UNCHOKE:
                                peer.isChoked = false;
                                pieceIndex = getPieceIndex(thisPeer.bitField, peer.bitField, thisPeer.bitField.length);

                                if (pieceIndex != -1)
                                    Messages.sendMessage(socket, Messages.getRequestMessage(pieceIndex));
                                System.out.println(Logger.unchoked(peerId));
                                break;
                            case MessageTypes.INTERESTED:
                                peer.isInterested = true;
                                System.out.println(Logger.receivedInterestedMessage(peerId));
                                break;
                            case MessageTypes.NOTINTERESTED:
                                peer.isInterested = false;
                                if (!peer.isChoked) {
                                    peer.isChoked = true;
                                    Messages.sendMessage(socket, Messages.getMessage(MessageTypes.CHOKE));
                                }
                                System.out.println(Logger.receivedNotInterestedMessage(peerId));
                                break;
                            case MessageTypes.HAVE:
                                pieceIndex = ByteBuffer.wrap(message).getInt();
                                peer.bitField[pieceIndex] = 1;
                                bits = 0;
                                for (int bit : peer.bitField) {
                                    if (bit == 1)
                                        bits++;
                                }
                                if (bits == thisPeer.bitField.length) {
                                    peer.hasFile = true;
                                    peersWithCompleteFile++;
                                }
                                if (checkIfInteresting(thisPeer.bitField, peer.bitField, thisPeer.bitField.length))
                                    Messages.sendMessage(socket, Messages.getMessage(MessageTypes.INTERESTED));
                                else
                                    Messages.sendMessage(socket, Messages.getMessage(MessageTypes.NOTINTERESTED));

                                System.out.println(Logger.receivedHaveMessage(peerId, pieceIndex));
                                System.out.println((new Date()) + " : Bitfield for peer " + peerId + " updated to "
                                        + Arrays.toString(peer.bitField));
                                break;
                            case MessageTypes.BITFIELD:
                                int[] bitField = new int[message.length / 4];
                                index = 0;
                                for (int i = 0; i < message.length; i += 4) {
                                    bitField[index] = ByteBuffer.wrap(Arrays.copyOfRange(message, i, i + 4)).getInt();
                                    index++;
                                }
                                peer.bitField = bitField;
                                System.out.println((new Date()) + " : Bitfield for peer " + peerId + " updated to "
                                        + Arrays.toString(bitField));
                                bits = 0;
                                for (int x : peer.bitField) {
                                    if (x == 1)
                                        bits++;
                                }
                                if (bits == thisPeer.bitField.length) {
                                    peer.hasFile = true;
                                    peersWithCompleteFile++;
                                } else {
                                    peer.hasFile = false;
                                }

                                if (checkIfInteresting(thisPeer.bitField, peer.bitField
                                        , thisPeer.bitField.length))
                                    Messages.sendMessage(socket, Messages.getMessage(MessageTypes.INTERESTED));
                                else
                                    Messages.sendMessage(socket, Messages.getMessage(MessageTypes.NOTINTERESTED));
                                break;
                            case MessageTypes.REQUEST:
                                pieceIndex = ByteBuffer.wrap(message).getInt();
                                Messages.sendMessage(socket, Messages.getPieceMessage(pieceIndex
                                        , thisPeer.filePieces[pieceIndex]));
                                break;
                            case MessageTypes.PIECE:
                                pieceIndex = ByteBuffer.wrap(Arrays.copyOfRange(message, 0, 4)).getInt();
                                index = 0;
                                thisPeer.filePieces[pieceIndex] = new byte[message.length - 4];
                                for (int i = 4; i < message.length; i++) {
                                    thisPeer.filePieces[pieceIndex][index] = message[i];
                                    index++;
                                }
                                thisPeer.bitField[pieceIndex] = 1;
                                thisPeer.updateNumberOfPieces();
                                if (!peer.isChoked) {
                                    int requestPieceIndex = getPieceIndex(thisPeer.bitField, peer.bitField, thisPeer.bitField.length);

                                    if (requestPieceIndex != -1)
                                        Messages.sendMessage(socket, Messages.getRequestMessage(requestPieceIndex));
                                }
                                double rate = (double) (message.length + 5) / elapsedTime;
                                peer.downloadRate = rate;

                                System.out.println(Logger.downloadingPieceCompleted(peerId, pieceIndex));
                                System.out.println((new Date()) + " : Bitfield for peer " + thisPeerId + " updated to "
                                        + Arrays.toString(thisPeer.bitField));

                                checkIfCompleteFileDownloaded();
                                for (int peerId : peerSockets.keySet()) {
                                    Messages.sendMessage(peerSockets.get(peerId), Messages.getHaveMessage(pieceIndex));
                                }
                                break;
                        }
                    }
                    System.exit(0);
                } catch (Exception e) {
//                    e.printStackTrace();
                }
            }
        }
    }
}