import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.*;

public class MessageExchanger extends Thread {
    Socket socket;
    final int peerId;
    List<String> info;

    public MessageExchanger(Socket socket, int peerId) {
        this.socket = socket;
        this.peerId = peerId;
    }

    public boolean checkIfInteresting(int[] currentPeerBitfield, int[] connectedPeerBitField, int length) {
        for (int i = 0; i < length; i++) {
            if (currentPeerBitfield[i] == 0 && connectedPeerBitField[i] == 1) {
                return true;
            }
        }
        return false;
    }

    public void checkIfCompleteFileDownloaded() {
        int parts = 0;
        int size = PeerProcess.props.getSize('f');
        byte[] mergedFile = new byte[size];
        for (int bit : PeerProcess.thisPeer.bitField) {
            if (bit == 1)
                parts += 1;
        }
        int idx = 0;
        if (parts == PeerProcess.thisPeer.bitField.length) {
            PeerProcess.peersWithCompleteFile++;
            PeerProcess.thisPeer.hasFile = true;
            for (int piece = 0; piece < PeerProcess.numberOfPieces; piece++) {
                byte[][] fp = PeerProcess.thisPeer.filePieces;
                for (int i = 0; i < fp[piece].length; i++) {
                    mergedFile[idx] = PeerProcess.thisPeer.filePieces[piece][i];
                    idx++;
                }
            }
            try {
                String fileName = Constants.peerFileNamePrefix + PeerProcess.currentPeerId + File.separatorChar + PeerProcess.props.getFileName();
                FileOutputStream fileOutputStream = new FileOutputStream(fileName);
                BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream);
                bufferedOutputStream.write(mergedFile);
                bufferedOutputStream.close();
                fileOutputStream.close();
                info = new ArrayList<>();
                info.add(Logger.totalDownloadCompleted());
                Utils.printInfo(info);
            } catch (Exception e) {}
        }
    }

    public int getPieceIndex(int[] thisPeerBitfield, int[] connectedPeerBitfield, int len) {
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < len; i++) {
            if (thisPeerBitfield[i] == 0 && connectedPeerBitfield[i] == 1) {
                indices.add(i);
            }
        }
        Random r = new Random();
        if (indices.size() > 0) {
            int randomIndex = Math.abs(r.nextInt() % indices.size());
            return indices.get(randomIndex);
        }
        return -1;
    }

    public synchronized void run() {
        synchronized (this) {
            try {
                long startTime;
                long endTime;
                double elapsedTime;
                DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
                byte[] bitFieldMessage = Messages.getBitFieldMessage(PeerProcess.thisPeer.bitField);
                Messages.sendMessage(socket, bitFieldMessage);
                while (PeerProcess.peersWithCompleteFile < PeerProcess.peers.size()) {
                    int messageLengthField = dataInputStream.readInt();
                    byte[] input = new byte[messageLengthField];
                    startTime = System.nanoTime();
                    dataInputStream.readFully(input);
                    endTime = System.nanoTime();
                    elapsedTime = (double) (endTime - startTime) / 1e9;

                    char messageType = (char) input[0];
                    byte[] message = new byte[messageLengthField - 1];
                    int idx = 0;
                    for (int i = 1; i < messageLengthField; i++) {
                        message[idx++] = input[i];
                    }
                    int pieceIndex;
                    int bits;
                    Peer peer = PeerProcess.peers.get(peerId);
                    switch (messageType) {
                        case MessageTypes.CHOKE:
                            peer.isChoked = true;
                            info = new ArrayList<>();
                            info.add(Logger.choked(peerId));
                            Utils.printInfo(info);
                            break;
                        case MessageTypes.UNCHOKE:
                            peer.isChoked = false;
                            pieceIndex = getPieceIndex(PeerProcess.thisPeer.bitField, peer.bitField, PeerProcess.thisPeer.bitField.length);
                            if (pieceIndex != -1)Messages.sendMessage(socket, Messages.getRequestMessage(pieceIndex));
                            info = new ArrayList<>();
                            info.add(Logger.unchoked(peerId));
                            Utils.printInfo(info);
                            break;
                        case MessageTypes.INTERESTED:
                            peer.isInterested = true;
                            info = new ArrayList<>();
                            info.add(Logger.receivedInterestedMessage(peerId));
                            Utils.printInfo(info);
                            break;
                        case MessageTypes.NOTINTERESTED:
                            peer.isInterested = false;
                            if (!peer.isChoked) {
                                peer.isChoked = true;
                                Messages.sendMessage(socket, Messages.getMessage(MessageTypes.CHOKE));
                            }
                            info = new ArrayList<>();
                            info.add(Logger.receivedNotInterestedMessage(peerId));
                            Utils.printInfo(info);
                            break;
                        case MessageTypes.HAVE:
                            pieceIndex = ByteBuffer.wrap(message).getInt();
                            peer.bitField[pieceIndex] = 1;
                            bits = 0;
                            for (int bit : peer.bitField) {
                                if (bit == 1) bits++;
                            }
                            if (bits == PeerProcess.thisPeer.bitField.length) {
                                peer.hasFile = true;
                                PeerProcess.peersWithCompleteFile++;
                            }
                            if (checkIfInteresting(PeerProcess.thisPeer.bitField, peer.bitField, PeerProcess.thisPeer.bitField.length)) {
                                Messages.sendMessage(socket, Messages.getMessage(MessageTypes.INTERESTED));
                            }
                            else {
                                Messages.sendMessage(socket, Messages.getMessage(MessageTypes.NOTINTERESTED));
                            }
                            info = new ArrayList<>();
                            info.add(Logger.receivedHaveMessage(peerId, pieceIndex));
                            info.add((new Date()) + " : Bitfield for peer " + peerId + " updated to " + Arrays.toString(peer.bitField));
                            Utils.printInfo(info);
                            break;
                        case MessageTypes.BITFIELD:
                            int[] bitField = new int[message.length / 4];
                            idx = 0;
                            for (int i = 0; i < message.length; i += 4) {
                                bitField[idx++] = ByteBuffer.wrap(Arrays.copyOfRange(message, i, i + 4)).getInt();
                            }
                            peer.bitField = bitField;
                            info = new ArrayList<>();
                            info.add((new Date()) + " : Bitfield for peer " + peerId + " updated to " + Arrays.toString(bitField));
                            Utils.printInfo(info);
                            bits = 0;
                            for (int x : peer.bitField) {
                                if (x == 1) bits++;
                            }
                            peer.hasFile = (bits == PeerProcess.thisPeer.bitField.length);
                            if(peer.hasFile) PeerProcess.peersWithCompleteFile++;
                            if (checkIfInteresting(PeerProcess.thisPeer.bitField, peer.bitField, PeerProcess.thisPeer.bitField.length))
                                Messages.sendMessage(socket, Messages.getMessage(MessageTypes.INTERESTED));
                            else
                                Messages.sendMessage(socket, Messages.getMessage(MessageTypes.NOTINTERESTED));
                            break;
                        case MessageTypes.REQUEST:
                            pieceIndex = ByteBuffer.wrap(message).getInt();
                            Messages.sendMessage(socket, Messages.getPieceMessage(pieceIndex, PeerProcess.thisPeer.filePieces[pieceIndex]));
                            break;
                        case MessageTypes.PIECE:
                            pieceIndex = ByteBuffer.wrap(Arrays.copyOfRange(message, 0, 4)).getInt();
                            idx = 0;
                            PeerProcess.thisPeer.filePieces[pieceIndex] = new byte[message.length - 4];
                            for (int i = 4; i < message.length; i++) {
                                PeerProcess.thisPeer.filePieces[pieceIndex][idx++] = message[i];
                            }
                            PeerProcess.thisPeer.bitField[pieceIndex] = 1;
                            PeerProcess.thisPeer.updateNumberOfPieces();
                            if (!peer.isChoked) {
                                int requestPieceIndex = getPieceIndex(PeerProcess.thisPeer.bitField, peer.bitField, PeerProcess.thisPeer.bitField.length);
                                if (requestPieceIndex != -1) Messages.sendMessage(socket, Messages.getRequestMessage(requestPieceIndex));
                            }
                            double rate = (double) (message.length + 5) / elapsedTime;
                            peer.downloadRate = rate;
                            info = new ArrayList<>();
                            info.add(Logger.downloadingPieceCompleted(peerId, pieceIndex));
                            info.add((new Date()) + " : Bitfield for peer " + PeerProcess.currentPeerId + " updated to " + Arrays.toString(PeerProcess.thisPeer.bitField));
                            Utils.printInfo(info);
                            checkIfCompleteFileDownloaded();
                            for (int peerId : PeerProcess.peerSockets.keySet()) {
                                Messages.sendMessage(PeerProcess.peerSockets.get(peerId), Messages.getHaveMessage(pieceIndex));
                            }
                            break;
                    }
                }
                System.exit(0);
            } catch (Exception e) {}
        }
    }
}
