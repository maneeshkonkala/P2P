import java.io.*;
import java.util.*;
import java.util.stream.*;

public class Setup {

    static void assignFilePieces(int fileSize, int pieceSize, byte[][] filePieces, int filePart, byte[] fileBytes, Peer peer) {
        for (int counter = 0; counter < fileSize; counter += pieceSize) {
            int remainingPieceSize = counter + pieceSize;
            int size = remainingPieceSize <= fileSize ? remainingPieceSize : fileSize;
            filePieces[filePart] = Arrays.copyOfRange(fileBytes, counter, size);
            filePart++;
            peer.updateNumberOfPieces();
        }
    }

    static int[] fillBitFiled(int[] bitField, int val) {
        for(int i=0;i<bitField.length;i++) {
            bitField[i] = val;
        }
        return bitField;
    }

    static void setCurrentPeerFileInfo(LinkedHashMap<Integer, Peer> peers, int thisPeerId
            , Props props) {
        try {
            Peer currentPeer = peers.get(thisPeerId);
            int fileSize = props.getSize(Constants.file);
            int pieceSize = props.getSize(Constants.piece);
            int numberOfPieces = props.getNumberOfPieces();
            int[] bitField = new int[numberOfPieces];
            byte[][] filePieces = new byte[numberOfPieces][];

            if (currentPeer.hasFile()) {
                bitField = fillBitFiled(bitField, 1);
                currentPeer.setBitField(bitField);
                String oz = Constants.peerFileNamePrefix + thisPeerId + File.separatorChar + props.getFileName();
                FileInputStream fischal = new FileInputStream(oz);
                BufferedInputStream file = new BufferedInputStream(fischal);
                byte[] fileBytes = new byte[fileSize];
                file.read(fileBytes);
                file.close();
                int filePart = 0;
                assignFilePieces(fileSize, pieceSize, filePieces, filePart, fileBytes, currentPeer);
            } 
            else {
                bitField = fillBitFiled(bitField, 0);
                currentPeer.setBitField(bitField);
            }
            currentPeer.setFilePieces(filePieces);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static Props getProps() {
        Props props = new Props();
        try {
            FileReader fr = new FileReader(Constants.fileNameOfCommonConfiguration);
            BufferedReader commonInfo = new BufferedReader(fr);
            Stream<String> common = commonInfo.lines();
            Object[] commonLines = common.toArray();
            int neighboursPreferred = Integer.parseInt(commonLines[0].toString().split(" ")[1]);
            props.setNeighborsPreferred(neighboursPreferred);
            int unchokingInterval = Integer.parseInt(commonLines[1].toString().split(" ")[1]);
            props.setInterval(Constants.unchoking, unchokingInterval);
            int optimisticUnchokingInterval = Integer.parseInt(commonLines[2].toString().split(" ")[1]);
            props.setInterval(Constants.optimisticUnchoking, optimisticUnchokingInterval);
            String fileName = commonLines[3].toString().split(" ")[1];
            props.setFileName(fileName);
            int fileSize = Integer.parseInt(commonLines[4].toString().split(" ")[1]);
            props.setSize(Constants.file, fileSize);
            int pieceSize = Integer.parseInt(commonLines[5].toString().split(" ")[1]);
            props.setSize(Constants.piece, pieceSize);
            commonInfo.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return props;
    }
}
