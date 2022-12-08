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
            OptimisticPeerUnchoker optimisticPeerUnchoker = new OptimisticPeerUnchoker();
            peerConnector.start();
            establishConnectionsWithPeers.start();
            peerUnchoking.start();
            optimisticPeerUnchoker.start();
        } catch (IOException e) {}
    }
}