import java.io.*;
import java.util.*;

public class Logger {
    static int currentPeerId;
    static BufferedWriter bufferedWriter;
    static int numberOfPieces = 0;

    public static void startLogger(int peerId) {
        try {
            currentPeerId = peerId;
            FileOutputStream fileOutputStream = new FileOutputStream("log_peer_" + currentPeerId + ".log");
            bufferedWriter = new BufferedWriter(new OutputStreamWriter(fileOutputStream));
        } catch (FileNotFoundException e) {}
    }

    public static String tcpConnectionMake(int peerId) {
        String s = "";
        try {
            List<String> str = new ArrayList<>();
            String date = (new Date()).toString();
            str.add(date);
            str.add(": Peer");
            str.add(String.valueOf(currentPeerId));
            str.add("makes a connection to Peer");
            str.add(String.valueOf(peerId));
            s = generateLogString(str);
            appendToLog(s);
        } catch (IOException e) {}
        return s;
    }

    public static String tcpConnectionMade(int peerId) {
        String s = "";
        try {
            List<String> str = new ArrayList<>();
            String date = (new Date()).toString();
            str.add(date);
            str.add(": Peer");
            str.add(String.valueOf(currentPeerId));
            str.add("is connected from Peer");
            str.add(String.valueOf(peerId));
            s = generateLogString(str);
            appendToLog(s);
        } catch (IOException e) {}
        return s;
    }

    public static String changePrefNeighbours(List<Integer> neighbors) {
        String s = "";
        StringBuffer sb = new StringBuffer();
        try {
            List<String> str = new ArrayList<>();
            String date = (new Date()).toString();
            str.add(date);
            str.add(": Peer");
            str.add(String.valueOf(currentPeerId));
            str.add("has the preferred neighbors");
            s = date + " : Peer " + currentPeerId + " has the preferred neighbors ";
            sb = new StringBuffer(s);
            for (int neighbor : neighbors) {
                sb.append(neighbor);
                sb.append(",");
            }
            sb.deleteCharAt(sb.length() - 1);
            str.add(sb.toString());
            s = generateLogString(str);
            appendToLog(s);
        } catch (IOException e) {}
        return s;
    }

    public static String changeOptilyUnchokedNeighbour(int peerId) {
        String s = "";
        try {
            List<String> str = new ArrayList<>();
            String date = (new Date()).toString();
            str.add(date);
            str.add(": Peer");
            str.add(String.valueOf(currentPeerId));
            str.add("has the optimistically unchoked neighbor");
            str.add(String.valueOf(peerId));
            s = generateLogString(str);
            appendToLog(s);
        } catch (IOException e) {}
        return s;
    }

    public static String unchoked(int peerId) {
        String s = "";
        try {
            List<String> str = new ArrayList<>();
            String date = (new Date()).toString();
            str.add(date);
            str.add(": Peer");
            str.add(String.valueOf(currentPeerId));
            str.add("is unchoked by");
            str.add(String.valueOf(peerId));
            s = generateLogString(str);
            appendToLog(s);
        } catch (IOException e) {}
        return s;
    }

    public static String choked(int peerId) {
        String s = "";
        try {
            List<String> str = new ArrayList<>();
            String date = (new Date()).toString();
            str.add(date);
            str.add(": Peer");
            str.add(String.valueOf(currentPeerId));
            str.add("is choked by");
            str.add(String.valueOf(peerId));
            s = generateLogString(str);
            appendToLog(s);
        } catch (IOException e) {}
        return s;
    }

    public static String receivedHaveMessage(int peerId, int pieceIndex) {
        String s = "";
        try {
            List<String> str = new ArrayList<>();
            String date = (new Date()).toString();
            str.add(date);
            str.add(": Peer");
            str.add(String.valueOf(currentPeerId));
            str.add("received the 'have' message from Peer");
            str.add(String.valueOf(peerId));
            str.add("for the piece");
            str.add(String.valueOf(pieceIndex));
            s = generateLogString(str);
            appendToLog(s);
        } catch (IOException e) {}
        return s;
    }

    public static String receivedInterestedMessage(int peerId) {
        String s = "";
        try {
            List<String> str = new ArrayList<>();
            String date = (new Date()).toString();
            str.add(date);
            str.add(": Peer");
            str.add(String.valueOf(currentPeerId));
            str.add("received the 'interested' message from Peer");
            str.add(String.valueOf(peerId));
            s = generateLogString(str);
            appendToLog(s);
        } catch (IOException e) {}
        return s;
    }

    public static String receivedNotInterestedMessage(int peerId) {
        String s = "";
        try {
            List<String> str = new ArrayList<>();
            String date = (new Date()).toString();
            str.add(date);
            str.add(": Peer");
            str.add(String.valueOf(currentPeerId));
            str.add("received the 'not interested' message from Peer");
            str.add(String.valueOf(peerId));
            s = generateLogString(str);
            appendToLog(s);
        } catch (IOException e) {}
        return s;
    }

    public static String downloadingPieceCompleted(int peerId, int pieceIndex) {
        String s = "";
        numberOfPieces += 1;
        try {
            List<String> str = new ArrayList<>();
            String date = (new Date()).toString();
            str.add(date);
            str.add(": Peer");
            str.add(String.valueOf(currentPeerId));
            str.add("has downloaded the piece");
            str.add(String.valueOf(pieceIndex));
            str.add("from Peer");
            str.add(String.valueOf(peerId));
            str.add("Now the number of pieces it has is");
            str.add(String.valueOf(numberOfPieces));
            s = generateLogString(str);
            appendToLog(s);
        } catch (IOException e) {}
        return s;
    }

    public static String totalDownloadCompleted() {
        String s = "";
        try {
            List<String> str = new ArrayList<>();
            String date = (new Date()).toString();
            str.add(date);
            str.add(": Peer");
            str.add(String.valueOf(currentPeerId));
            str.add("has downloaded the complete file");
            s = generateLogString(str);
            appendToLog(s);
        } catch (IOException e) {}
        return s;
    }

    public static void closeLogger() {
        try {
            bufferedWriter.close();
        } catch (IOException e) {}
    }

    public static String generateLogString(List<String> str) {
        StringBuilder sb = new StringBuilder();
        for(String s : str) {
            sb.append(s);
            sb.append(Constants.space);
        }
        sb.append(".");
        return sb.toString();
    }

    public static void appendToLog(String s) throws IOException {
        bufferedWriter.append(s);
        bufferedWriter.newLine();
        bufferedWriter.newLine();
        bufferedWriter.flush();
    }
}
