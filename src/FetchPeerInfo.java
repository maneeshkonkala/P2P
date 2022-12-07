import java.util.*;
import java.util.stream.*;
import java.io.*;

public class FetchPeerInfo {

    static int getIntegerEquivalent(String str) {
        int integerEquivalent = Integer.parseInt(str);
        return integerEquivalent;
    }

    static String[] separateData(String line) {
        String[] contents = line.split(Constants.space);
        return contents;
    }

    static void parsePeerInfoFromLine(Object peerInfoLine, LinkedHashMap<Integer, Peer> peers) {
        String line = peerInfoLine.toString();
        String[] contents = separateData(line);
        String peerIdString = contents[0];
        int peerId = getIntegerEquivalent(peerIdString);
        String hostName = contents[1];
        String portString = contents[2];
        int portNumber = getIntegerEquivalent(portString);
        String fileInfo = contents[3];
        int fileInfoValue = getIntegerEquivalent(fileInfo);
        boolean hasFile = fileInfoValue == 1;
        Peer peer = new Peer(peerId, hostName, portNumber, hasFile);
        peers.put(peerId, peer);
    }

    static LinkedHashMap<Integer, Peer> getAllPeersInfo() {
        LinkedHashMap<Integer, Peer> peers = new LinkedHashMap<>();
        try {
            BufferedReader peerInfo = new BufferedReader(new FileReader(Constants.fileNameOfPeerInfoConfiguration));
            Stream<String> peerInfoLinesStream = peerInfo.lines();
            Object[] peerInfoLines = peerInfoLinesStream.toArray();
            for (Object peerInfoLine : peerInfoLines) {
                parsePeerInfoFromLine(peerInfoLine, peers);
            }
            peerInfo.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return peers;
    }
}
