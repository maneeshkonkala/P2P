class Peer {
    int pId;
    String hostName;
    int port;
    boolean hasFile;
    int[] bitField;
    int numberOfPieces = 0;
    boolean isChoked = true;
    boolean isInterested = false;
    double downloadRate = 0;
    byte[][] filePieces;

    public Peer(int pId, String hostName, int port, boolean hasFile) {
        this.pId = pId;
        this.hostName = hostName;
        this.port = port;
        this.hasFile = hasFile;
    }

    public void updateNumberOfPieces() {
        this.numberOfPieces++;
        this.hasFile = (this.numberOfPieces == bitField.length);
    }
}