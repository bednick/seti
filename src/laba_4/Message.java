package laba_4;

import java.net.DatagramPacket;
import java.nio.ByteBuffer;
import java.util.Random;

/**
 * Класс реализующий сообщения
 */
public class Message {
    public static final int SIZEHEAD = (Long.BYTES + Integer.BYTES); //размер гарантированого заголовка long + int (guid + type)
    private long guid;
    private int type;
    private int countSent;
    private byte[] message;
    private Node address;

    private static Random random;
    static {
        random = new Random(System.currentTimeMillis());
    }

    public Message(int type, byte[] message) {
        this.guid = random.nextLong();
        this.countSent = 0;
        this.type = type;
        this.message = message.clone();
        this.address = null;
    }
    public Message(int type, byte[] message, Node address) {
        this.guid = random.nextLong();
        this.countSent = 0;
        this.type = type;
        this.message = message.clone();
        this.address = address;
    }
    public Message(DatagramPacket packet) {
        this.countSent = 0;
        ByteBuffer byteBuffer = ByteBuffer.allocate(packet.getData().length);
        byteBuffer.put(packet.getData().clone());
        byteBuffer.flip();
        this.guid = byteBuffer.getLong();
        this.type = byteBuffer.getInt();
        this.message = new byte[packet.getLength() - SIZEHEAD];
        byteBuffer.get(this.message);
        this.address = new Node(packet.getAddress(), packet.getPort());
    }
    public int getType(){
        return type;
    }
    public int getCountSent() {
        return countSent;
    }
    public void setCountSent(int countSent) {
        this.countSent = countSent;
    }
    public long getGuid() {
        return guid;
    }
    public Node getAddress() {
        return address;
    }
    public DatagramPacket getPacket() {
        ByteBuffer byteBuffer = ByteBuffer.allocate(message.length + SIZEHEAD);
        byteBuffer.putLong(guid);
        byteBuffer.putInt(type);
        byteBuffer.put(message);
        byteBuffer.flip();
        if (address != null) {
            return new DatagramPacket(byteBuffer.array(), message.length + SIZEHEAD, address.getInetAddress(), address.getPort());
        } else {
            return new DatagramPacket(byteBuffer.array(), message.length + SIZEHEAD);
        }
    }
    public byte[] getData() {
        return message;
    }
    public static class TYPE {
        public final static int MESSAGE = 1;
        public final static int ANSWER  = 2;
        public final static int DEAD    = 3;
        public final static int PARENT  = 4;
        public final static int CONNECT = 5;
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null) {
            return false;
        }
        if (o.getClass() != getClass()) {
            return false;
        }
        if (address != null) {
            return ((Message) o).guid == guid && address.equals(o);
        } else {
            if (((Message) o).address == null) {
                return ((Message) o).guid == guid;
            } else {
                return false;
            }
        }
    }
}
