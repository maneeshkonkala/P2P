import java.util.*;

class CommonProperties {

    String fileName;
    Map<String, Integer> properties;

    public CommonProperties() {
        properties = new HashMap<>();
        properties.putIfAbsent(Constants.neighboursPreferred, 0);
        properties.putIfAbsent(Constants.intervalForUnchoking, 0);
        properties.putIfAbsent(Constants.intervalForOptimisticUnchoking, 0);
        properties.putIfAbsent(Constants.sizeOfFlie, 0);
        properties.putIfAbsent(Constants.sizeOfPiece, 0);
    }

    public int getNeighborsPreferred() {
        return this.properties.get(Constants.neighboursPreferred);
    }

    public void setNeighborsPreferred(int neighboursPreferred) {
        this.properties.put(Constants.neighboursPreferred, neighboursPreferred);
    }

    public int getInterval(char type) {
        String intervalType = type == 'u' ? Constants.intervalForUnchoking : Constants.intervalForOptimisticUnchoking;
        int interval = this.properties.get(intervalType);
        return interval; 
    }

    public void setInterval(char type, int interval) {
        String intervalType = type == 'u' ? Constants.intervalForUnchoking : Constants.intervalForOptimisticUnchoking;
        this.properties.put(intervalType, interval);
        return;
    }

    public String getFileName() {
        return this.fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public int getSize(char type) {
        String objectType = type == 'f' ? Constants.sizeOfFlie : Constants.sizeOfPiece;
        int size = this.properties.get(objectType);
        return size;
    }

    public void setSize(char type, int size) {
        String objectType = type == 'f' ? Constants.sizeOfFlie : Constants.sizeOfPiece;
        this.properties.put(objectType, size);
        return;
    }

    public int getNumberOfPieces() {
        return (int) Math.ceil((double) this.getSize('f') / this.getSize('p'));
    }
}