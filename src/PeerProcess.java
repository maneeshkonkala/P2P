import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

public class PeerProcess {
    static int peersWithCompleteFile = 0;
    static int currentPeerId;
    static int numberOfPieces;
    static Peer thisPeer;
    static Props props;
    static LinkedHashMap<Integer, Peer> peers;
    static ConcurrentHashMap<Integer, Socket> peerSockets;

    public static void main(String[] args) {
        try {
            currentPeerId = Integer.parseInt(args[0]);
            peerSockets = new ConcurrentHashMap<>();
            peers = FetchPeerInfo.getAllPeersInfo();
            for (int peerId : peers.keySet()) {
                Files.createDirectories(Paths.get("peer_" + peerId));
            }
            props = Setup.getProps();
            numberOfPieces = props.getNumberOfPieces();
            List<String> info = new ArrayList<>();
            // scope for change
            info.add("Number Of Preferred Neighbors : " + props.getNeighborsPreferred());
            info.add("Unchoking Interval : " + props.getInterval('u') + " seconds");
            info.add("Optimistic Unchoking Interval : " + props.getInterval('o') + " seconds");
            info.add("File Name : " + props.getFileName());
            info.add("File Size : " + props.getSize('f'));
            info.add("Piece Size : " + props.getSize('p'));
            info.add("\n");
            Utils.printInfo(info);
            Setup.setCurrentPeerFileInfo(peers, currentPeerId, props);
            thisPeer = peers.get(currentPeerId);
            info = new ArrayList<>();
            // scope for change
            info.add("Peer Id : " + currentPeerId);
            info.add("Host Name : " + thisPeer.hostName);
            info.add("Port Number : " + thisPeer.port);
            info.add("Has Complete File : " + (thisPeer.hasFile ? "Yes" : "No"));
            info.add("\n");
            Utils.printInfo(info);
            peersWithCompleteFile += thisPeer.hasFile ? 1 : 0;
            Logger.startLogger(currentPeerId);
            PeerConnector peerConnector = new PeerConnector();
            EstablishConnectionsWithPeers establishConnectionsWithPeers = new EstablishConnectionsWithPeers();
            PeerUnchoker peerUnchoking = new PeerUnchoker();
            OptimisticallyUnchokePeers optimisticallyUnchokePeers = new OptimisticallyUnchokePeers();
            peerConnector.start();
            establishConnectionsWithPeers.start();
            peerUnchoking.start();
            optimisticallyUnchokePeers.start();
        } catch (IOException e) {}
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
            } catch (Exception e) {}
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
                    FileOutputStream fileOutputStream = new FileOutputStream("peer_" + currentPeerId + File.separatorChar
                            + props.getFileName());
                    BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream);
                    bufferedOutputStream.write(mergedFile);
                    bufferedOutputStream.close();
                    fileOutputStream.close();
                    System.out.println(Logger.totalDownloadCompleted());
                } catch (Exception e) {}
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
                                System.out.println((new Date()) + " : Bitfield for peer " + currentPeerId + " updated to "
                                        + Arrays.toString(thisPeer.bitField));

                                checkIfCompleteFileDownloaded();
                                for (int peerId : peerSockets.keySet()) {
                                    Messages.sendMessage(peerSockets.get(peerId), Messages.getHaveMessage(pieceIndex));
                                }
                                break;
                        }
                    }
                    System.exit(0);
                } catch (Exception e) {}
            }
        }
    }
}