import java.io.DataOutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class Messages {
    public static byte[] createFinalMessage(char type, byte[] payload) {
        byte[] finalMessage;
        byte messageType = (byte) type;
        int payloadLength = payload.length;
        int messageTypeLength = 1;
        int messageLength = payloadLength + messageTypeLength;

        int idx;
        switch (type) {
            case MessageTypes.CHOKE:
            case MessageTypes.UNCHOKE:
            case MessageTypes.INTERESTED:
            case MessageTypes.NOTINTERESTED:
                finalMessage = new byte[messageLength + 4];
                idx = 0;
                for (byte b : ByteBuffer.allocate(4).putInt(messageLength).array()) finalMessage[idx++] = b;
                finalMessage[idx] = messageType;
                break;
            case MessageTypes.HAVE:
            case MessageTypes.BITFIELD:
            case MessageTypes.REQUEST:
            case MessageTypes.PIECE:
                finalMessage = new byte[messageLength + 4];
                idx = 0;
                for (byte b : ByteBuffer.allocate(4).putInt(messageLength).array()) finalMessage[idx++] = b;
                finalMessage[idx++] = messageType;
                for (byte b : payload) finalMessage[idx++] = b;
                break;
            default:
                finalMessage = new byte[0];
                break;
        }
        return finalMessage;
    }

    public static byte[] getHandshakeMessage(int peerId) {
        byte[] handShakeMessage = new byte[32];
        byte[] header = Constants.handShakeHeader.getBytes(StandardCharsets.UTF_8);
        byte[] zeroBits = Constants.zeroBitsHeader.getBytes(StandardCharsets.UTF_8);
        byte[] thisPeerId = ByteBuffer.allocate(4).putInt(peerId).array();

        int idx = 0;
        for (var headerByte : header) {
            handShakeMessage[idx++] = headerByte;
        }

        for (var zeroByte : zeroBits) {
            handShakeMessage[idx++] = zeroByte;
        }

        for (var peerIdByte : thisPeerId) {
            handShakeMessage[idx++] = peerIdByte;
        }
        return handShakeMessage;
    }

    public static byte[] getBitFieldMessage(int[] bitField) {
        int payloadLength = 4 * bitField.length;
        byte[] payload = new byte[payloadLength];
        int idx = 0;
        for (int bit : bitField) {
            byte[] bitBytes = ByteBuffer.allocate(4).putInt(bit).array();
            for (int i=0;i<bitBytes.length;i++) {
                payload[idx++] = bitBytes[i];
            }
        }
        return createFinalMessage(MessageTypes.BITFIELD, payload);
    }

    public static byte[] getMessage(char messageType) {
        return createFinalMessage(messageType, new byte[0]);
    }

    public static byte[] getMessageWithPayload(char messageType, byte[] payload) {
        return createFinalMessage(messageType, payload);
    }

    public static byte[] getRequestMessage(int pieceIndex) {
        byte[] payload = ByteBuffer.allocate(4).putInt(pieceIndex).array();
        return getMessageWithPayload(MessageTypes.REQUEST, payload);
    }

    public static byte[] getPieceMessage(int pieceIndex, byte[] piece) {
        int pieceIndexLength = 4;
        byte[] payload = new byte[pieceIndexLength + piece.length];
        int idx = 0;
        byte[] pieceIndexBytes = ByteBuffer.allocate(4).putInt(pieceIndex).array();
        for (int i=0;i<pieceIndexBytes.length; i++) {
            payload[idx++] = pieceIndexBytes[i];
        }
        for (int i=0;i<piece.length; i++) {
            payload[idx++] = piece[i];
        }
        return getMessageWithPayload(MessageTypes.PIECE, payload);
    }

    public static byte[] getHaveMessage(int pieceIndex) {
        byte[] payload = ByteBuffer.allocate(4).putInt(pieceIndex).array();
        return getMessageWithPayload(MessageTypes.HAVE, payload);
    }

    public static void sendMessage(Socket socket, byte[] data) {
        try {
            DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
            dataOutputStream.flush();
            dataOutputStream.write(data);
            dataOutputStream.flush();
        } catch (Exception e) {}
    }
}